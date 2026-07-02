package com.example.energy

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

class EnergyMeter(
    private val context: Context,
    private val sampleMs: Long = 10L
) {
    data class Result(
        val durationMs: Long,
        val samples: Int,
        val energyMilliWattHour: Double,
        val method: String
    )

    private val bm = context.getSystemService(BatteryManager::class.java)

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
        if (startNWh != null && endNWh != null && endNWh != startNWh) {
            val deltaNWh = endNWh - startNWh
            val mWh = abs(deltaNWh).toDouble() / 1_000_000.0
            android.util.Log.d("EnergyMeter", "Method: ENERGY_COUNTER, Energy: $mWh mWh")
            return Result((t1 - t0), 2, mWh, "ENERGY_COUNTER")
        }

        val mWh = energyJ / 3.6
        android.util.Log.d("EnergyMeter", "Method: INTEGRATION, Energy: $mWh mWh, Samples: $samples")
        return Result(
            durationMs = (t1 - t0),
            samples = samples,
            energyMilliWattHour = mWh,
            method = "INTEGRATION"
        )
    }
}