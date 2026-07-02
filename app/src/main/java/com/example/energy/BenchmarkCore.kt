package com.example.energy

import android.content.Context
import kotlin.math.pow
import kotlin.math.sqrt

data class RoundResult(
    val encNs: Long,
    val decNs: Long,
    val encMemBytes: Long,
    val decMemBytes: Long
)

/** One round = encrypt once + decrypt once; return RoundResult */
fun interface RoundRunner {
    fun run(roundIndex: Int): RoundResult
}

/** Hook to prepare for a given plaintext size; returns a per-round runner */
fun interface SizeRunnerFactory {
    fun prepare(sizeBytes: Int): RoundRunner
}

/** Shared 1024B base → repeat to size (constant, non-random, benchmark-friendly) */
object BenchmarkPlain {
    private val BASE: String = buildString(1024) {
        val pattern = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        while (length < 1024) append(pattern)
    }.substring(0, 1024)

    fun build(size: Int): String {
        val sb = StringBuilder(size)
        while (sb.length < size) {
            val r = size - sb.length
            if (r >= BASE.length) sb.append(BASE) else sb.append(BASE, 0, r)
        }
        return sb.toString()
    }
}

/** Stats (nanoseconds) */
data class TimingStats(
    val min: Long,
    val median: Long,
    val max: Long,
    val mean: Double,
    val stddev: Double,
    val stdPercent: Double
)

/** Median of LongArray */
private fun median(values: LongArray): Long =
    values.sorted().let { s ->
        val m = s.size / 2
        if (s.size % 2 == 0) (s[m - 1] + s[m]) / 2 else s[m]
    }

/** Full stats helper (no longer used for CSV, but kept if you want stats in code) */
private fun computeStats(values: LongArray): TimingStats {
    val sorted = values.sorted()
    val min = sorted.first()
    val max = sorted.last()
    val median = if (sorted.size % 2 == 0)
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
    else
        sorted[sorted.size / 2]

    val mean = sorted.average()
    val std = if (sorted.size > 1) {
        var acc = 0.0
        for (v in sorted) {
            val d = v - mean
            acc += d * d
        }
        sqrt(acc / (sorted.size - 1))
    } else 0.0

    val stdPercent = if (mean != 0.0) std / mean * 100.0 else 0.0
    return TimingStats(min, median, max, mean, std, stdPercent)
}

/** CSV header for summary per size (kept for reference, not used now) */
const val BENCH_CSV_HEADER =
    "size_bytes," +
            "enc_min_ns,enc_median_ns,enc_max_ns,enc_mean_ns,enc_std_ns,enc_std_percent," +
            "dec_min_ns,dec_median_ns,dec_max_ns,dec_mean_ns,dec_std_ns,dec_std_percent," +
            "energy_mWh"

/** CSV header for each execution (per round) */
const val BENCH_EXEC_CSV_HEADER =
    "size_bytes,round_index,enc_ns,dec_ns,enc_mem_bytes,dec_mem_bytes,energy_mWh,method," +
    "timestamp_before,battery_level_before,battery_status_before,battery_temp_c_before,battery_mv_before," +
    "timestamp_after,battery_level_after,battery_status_after,battery_temp_c_after,battery_mv_after"

/** Generic runner: handles sizes, rounds, energy, and CSV writing (per execution only) */
object BenchmarkRunner {

    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    fun measureMemory(block: () -> Unit): Long {
        System.gc()
        Thread.sleep(100) // Give GC some time
        val before = getUsedMemory()
        block()
        val after = getUsedMemory()
        return (after - before).coerceAtLeast(0L)
    }

    fun runRangeAndLog(
        context: Context,
        minPow: Int,
        maxPow: Int,
        rounds: Int,
        factory: SizeRunnerFactory,
        fileName: String,
        subDir: String? = null
    ): String {
        // Single CSV (one row per execution/round), as you requested
        val logger = CsvLogger(context, fileName, BENCH_EXEC_CSV_HEADER, subDir)
        logger.startFresh()

        val meter = EnergyMeter(context)

        // Loop over plaintext sizes 2^minPow .. 2^maxPow
        for (p in minPow..maxPow) {
            val size = 2.0.pow(p).toInt()

            // Prepare plaintext once (for consistency; not timed as part of energy)
            BenchmarkPlain.build(size)

            val encTimes = LongArray(rounds)
            val decTimes = LongArray(rounds)
            val encMem = LongArray(rounds)
            val decMem = LongArray(rounds)

            val sizeRunner = factory.prepare(size)

            // Measure energy for ALL rounds of this size together
            val result = meter.measure {
                repeat(rounds) { i ->
                    val r = sizeRunner.run(i)
                    encTimes[i] = r.encNs
                    decTimes[i] = r.decNs
                    encMem[i] = r.encMemBytes
                    decMem[i] = r.decMemBytes
                }
            }

            // Approximate per-round energy as total/rounds
            val energyPerRound = result.energyMilliWattHour / rounds.toDouble()

            // Write one row per execution
            val bb = result.batteryBefore
            val ba = result.batteryAfter
            for (i in 0 until rounds) {
                val line = listOf(
                    size,
                    i + 1, // round_index starting at 1
                    encTimes[i],
                    decTimes[i],
                    encMem[i],
                    decMem[i],
                    String.format("%.6e", energyPerRound),
                    result.method,
                    // battery snapshot before
                    bb.timestamp,
                    bb.levelPercent,
                    bb.status,
                    String.format("%.1f", bb.temperatureCelsius),
                    bb.voltageMilliV,
                    // battery snapshot after
                    ba.timestamp,
                    ba.levelPercent,
                    ba.status,
                    String.format("%.1f", ba.temperatureCelsius),
                    ba.voltageMilliV
                ).joinToString(",")
                logger.appendLine(line)
            }
        }

        // We now only have one CSV: the per-execution one.
        return logger.path()
    }
}
