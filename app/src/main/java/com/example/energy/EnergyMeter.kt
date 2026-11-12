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
        val batteryAfter: BatterySnapshot,
        val status: String = "VALID"
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
        return if (v <= 0L) null else v
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
        val before = readBatterySnapshot()
        val startCounter = readEnergyCounterNWh()
        val running = AtomicBoolean(true)
        val ready = java.util.concurrent.CountDownLatch(1)
        var samples = 0
        var energyJ = 0.0
        var invalid = false
        var started = 0L
        var ended = 0L
        val sampler = Thread {
            var previousT = SystemClock.elapsedRealtimeNanos()
            var previousI = readCurrentMicroA()
            var previousV = readVoltageMilliV()
            started = SystemClock.elapsedRealtimeNanos()
            previousT = started
            ready.countDown()
            while (running.get()) {
                try { Thread.sleep(sampleMs) } catch (_: InterruptedException) { }
                val t = if (running.get()) SystemClock.elapsedRealtimeNanos() else ended
                val current = readCurrentMicroA()
                val voltage = readVoltageMilliV()
                // Android API contract: CURRENT_NOW is microamperes; negative means discharge.
                val oldI=previousI; val oldV=previousV
                if (oldI == null || current == null || oldV == null || voltage == null || oldI >= 0 || current >= 0) {
                    invalid = true
                } else {
                    val oldPower = -oldI.toDouble()*oldV/1e9
                    val power = -current.toDouble()*voltage/1e9
                    energyJ += (oldPower+power)*0.5*(t-previousT).coerceAtLeast(0)/1e9
                    samples++
                }
                previousT=t; previousI=current; previousV=voltage
            }
        }
        sampler.isDaemon = true
        sampler.start()
        if (!ready.await(2, java.util.concurrent.TimeUnit.SECONDS)) {
            running.set(false)
            sampler.interrupt()
            val fallbackStart = SystemClock.elapsedRealtimeNanos()
            block()
            return Result((SystemClock.elapsedRealtimeNanos() - fallbackStart) / 1_000_000,
                0, Double.NaN, "CURRENT_INTEGRATION", before, before, "SAMPLER_START_TIMEOUT")
        }
        try { block() } finally {
            ended=SystemClock.elapsedRealtimeNanos()
            running.set(false)
            sampler.interrupt()
            sampler.join(1000)
        }
        if (sampler.isAlive) {
            return Result((ended - started) / 1_000_000, 0, Double.NaN,
                "CURRENT_INTEGRATION", before, before, "SAMPLER_STOP_TIMEOUT")
        }
        val endCounter=readEnergyCounterNWh()
        val after=readBatterySnapshot()
        val duration=(ended-started)/1_000_000
        val discharging=before.status == "DISCHARGING" && after.status == "DISCHARGING"
        if (discharging && startCounter != null && endCounter != null && startCounter > endCounter) {
            return Result(duration, 2, (startCounter-endCounter)/1_000_000.0,
                "ENERGY_COUNTER", before, after)
        }
        val valid=discharging && !invalid && samples >= 3 && duration >= sampleMs*3
        return Result(duration, samples, if(valid) energyJ/3.6 else Double.NaN,
            "CURRENT_INTEGRATION", before, after,
            if(valid) "VALID_DEVICE_ESTIMATE" else if(!discharging) "NOT_DISCHARGING" else "INSUFFICIENT_OR_INVALID_SAMPLES")
    }
}
