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
    private val sampleMs: Long = 100L
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
        return if (v == Long.MIN_VALUE) null else v // nWh（可能为负）
    }

    private fun readCurrentMicroA(): Int? {
        val v = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        return if (v == Int.MIN_VALUE) null else v // 可能为负表示放电
    }

    private fun readVoltageMilliV(): Int? {
        val sticky: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        return sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
    }

    fun measure(block: () -> Unit): Result {
        val startNWh = readEnergyCounterNWh()
        if (startNWh != null) {
            val t0 = SystemClock.elapsedRealtime()
            block()
            val t1 = SystemClock.elapsedRealtime()
            val endNWh = readEnergyCounterNWh()
            if (endNWh != null) {
                val deltaNWh = endNWh - startNWh
                val mWh = abs(deltaNWh).toDouble() / 1_000_000.0
                return Result(
                    durationMs = (t1 - t0),
                    samples = 2,
                    energyMilliWattHour = mWh,
                    method = "ENERGY_COUNTER"
                )
            }
        }


        val running = AtomicBoolean(true)
        var samples = 0
        var energyJ = 0.0

        val voltageMvInitial = readVoltageMilliV() ?: 4000 // 兜底 4.0V（尽量用真实值）
        var lastT = SystemClock.elapsedRealtimeNanos()
        var lastI = readCurrentMicroA() ?: 0

        val sampler = Thread {
            while (running.get()) {
                try {
                    Thread.sleep(sampleMs)
                } catch (_: InterruptedException) {
                    break
                }
                val nowT = SystemClock.elapsedRealtimeNanos()
                val nowI = readCurrentMicroA() ?: lastI
                val dt = (nowT - lastT) / 1e9 // 秒
                val iAvgA = ((abs(lastI) + abs(nowI)) / 2.0) / 1e6 // A
                val vV = (readVoltageMilliV() ?: voltageMvInitial) / 1000.0
                energyJ += vV * iAvgA * max(dt, 0.0)
                samples += 1
                lastT = nowT
                lastI = nowI
            }
        }

        val t0 = SystemClock.elapsedRealtime()
        sampler.start()
        try {
            block()
        } finally {
            running.set(false)
            sampler.join(2 * sampleMs)
        }
        val t1 = SystemClock.elapsedRealtime()

        val mWh = energyJ / 3.6
        return Result(
            durationMs = (t1 - t0),
            samples = samples + 1, // 含起点
            energyMilliWattHour = mWh,
            method = "INTEGRATION"
        )
    }
}