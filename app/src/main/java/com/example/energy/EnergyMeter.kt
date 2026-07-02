package com.example.energy

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

class EnergyMeter(
    private val context: Context,
    private val sampleMs: Long = 10L
) {
    data class BatterySnapshot(
        val timestamp: String,        // ISO-8601 local date-time
        val levelPercent: Int,        // 0-100, -1 if unavailable
        val status: String,           // e.g. "DISCHARGING", "CHARGING", "FULL", "UNKNOWN"
        val temperatureCelsius: Float,// degrees C (tenths-of-degree / 10), -1 if unavailable
        val voltageMilliV: Int        // mV, -1 if unavailable
    )

    data class Result(
        val durationMs: Long,
        val samples: Int,
        val energyMilliWattHour: Double,
        val method: String,
        val batteryBefore: BatterySnapshot,
        val batteryAfter: BatterySnapshot
    )

    private val bm = context.getSystemService(BatteryManager::class.java)

    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun readBatterySnapshot(): BatterySnapshot {
        val sticky: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val rawLevel = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val rawScale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val levelPercent = if (rawLevel >= 0 && rawScale > 0) (rawLevel * 100 / rawScale) else -1

        val rawStatus = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val status = when (rawStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING    -> "CHARGING"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
            BatteryManager.BATTERY_STATUS_FULL        -> "FULL"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
            else -> "UNKNOWN"
        }

        val rawTemp = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -10) ?: -10
        val tempC = if (rawTemp != -10) rawTemp / 10f else -1f

        val voltageMilliV = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1

        return BatterySnapshot(
            timestamp = LocalDateTime.now().format(isoFmt),
            levelPercent = levelPercent,
            status = status,
            temperatureCelsius = tempC,
            voltageMilliV = voltageMilliV
        )
    }

    private fun readEnergyCounterNWh(): Long? {
        val v = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        return if (v == Long.MIN_VALUE || v == 0L) null else v 
    }

    private fun readCurrentMicroA(): Int? {
        val v = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        return if (v == Int.MIN_VALUE) null else v 
    }

    private fun readVoltageMilliV(): Int? {
        val sticky: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        return sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
    }

    fun measure(block: () -> Unit): Result {
        android.util.Log.d("EnergyMeter", "Starting measurement...")
        val batteryBefore = readBatterySnapshot()

        val startNWh = readEnergyCounterNWh()
        val startV = readVoltageMilliV()
        val startI = readCurrentMicroA()
        
        val running = AtomicBoolean(true)
        var samples = 0
        var energyJ = 0.0

        val voltageMvInitial = startV ?: 4000
        var lastT = SystemClock.elapsedRealtimeNanos()
        var lastI = startI ?: 0

        val sampler = Thread {
            while (running.get()) {
                val nowT = SystemClock.elapsedRealtimeNanos()
                val rawI = readCurrentMicroA() ?: lastI
                val nowV = readVoltageMilliV() ?: voltageMvInitial
                
                // Detection: If rawI is small (e.g. < 5000), it's likely mA, not uA.
                // Standard phones with screen on consume > 100mA (100,000 uA).
                // If it's mA, rawI would be ~200. If it's uA, rawI would be ~200,000.
                val iAmps = if (abs(rawI) > 0 && abs(rawI) < 5000) {
                    abs(rawI).toDouble() / 1000.0 // mA -> A
                } else {
                    abs(rawI).toDouble() / 1000000.0 // uA -> A
                }

                val dt = (nowT - lastT) / 1e9 // seconds
                val vV = nowV / 1000.0
                val deltaJ = vV * iAmps * max(dt, 0.0)
                energyJ += deltaJ
                samples += 1
                
                lastT = nowT
                lastI = rawI
                try {
                    Thread.sleep(sampleMs)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        val t0 = SystemClock.elapsedRealtime()
        sampler.start()
        try {
            block()
        } finally {
            running.set(false)
            sampler.interrupt()
            sampler.join(500)
        }
        val t1 = SystemClock.elapsedRealtime()

        val endNWh = readEnergyCounterNWh()
        val batteryAfter = readBatterySnapshot()

        if (startNWh != null && endNWh != null && endNWh != startNWh) {
            val deltaNWh = endNWh - startNWh
            val mWh = abs(deltaNWh).toDouble() / 1_000_000.0
            android.util.Log.d("EnergyMeter", "Method: ENERGY_COUNTER, Energy: $mWh mWh")
            return Result((t1 - t0), 2, mWh, "ENERGY_COUNTER", batteryBefore, batteryAfter)
        }

        val mWh = energyJ / 3.6
        android.util.Log.d("EnergyMeter", "Method: INTEGRATION, Energy: $mWh mWh, Samples: $samples")
        return Result(
            durationMs = (t1 - t0),
            samples = samples,
            energyMilliWattHour = mWh,
            method = "INTEGRATION",
            batteryBefore = batteryBefore,
            batteryAfter = batteryAfter
        )
    }
}