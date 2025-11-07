package com.example.energy

import android.content.Context
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.system.measureNanoTime

/** One round = encrypt once + decrypt once; return (encNs, decNs) */
fun interface RoundRunner {
    fun run(roundIndex: Int): Pair<Long, Long>
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

/** Stats */
data class TimingStats(
    val min: Long,
    val median: Long,
    val max: Long,
    val stddev: Double
)

private fun median(values: LongArray): Long =
    values.sorted().let { s ->
        val m = s.size / 2
        if (s.size % 2 == 0) (s[m - 1] + s[m]) / 2 else s[m]
    }

private fun stddev(values: LongArray): Double {
    if (values.size <= 1) return 0.0
    val mean = values.average()
    var acc = 0.0
    for (v in values) { val d = v - mean; acc += d * d }
    return sqrt(acc / (values.size - 1))
}

/** CSV header unified across all algorithms */
const val BENCH_CSV_HEADER =
    "size_bytes,enc_min_ns,enc_median_ns,enc_max_ns,enc_std_ns,dec_min_ns,dec_median_ns,dec_max_ns,dec_std_ns,energy_mWh"

/** Generic runner: handles sizes, rounds, energy, stats, and CSV writing */
object BenchmarkRunner {

    data class ResultRow(
        val sizeBytes: Int,
        val encStats: TimingStats,
        val decStats: TimingStats,
        val energyMWh: Double
    ) {
        fun toCsv(): String = listOf(
            sizeBytes,
            encStats.min, encStats.median, encStats.max, encStats.stddev.toLong(),
            decStats.min, decStats.median, decStats.max, decStats.stddev.toLong(),
            String.format("%.6f", energyMWh)
        ).joinToString(",")
    }

    fun runRangeAndLog(
        context: Context,
        minPow: Int,
        maxPow: Int,
        rounds: Int,
        factory: SizeRunnerFactory,
        fileName: String
    ): String {
        val logger = CsvLogger(context, fileName, BENCH_CSV_HEADER)
        logger.startFresh()

        val meter = EnergyMeter(context)

        for (p in minPow..maxPow) {
            val size = 2.0.pow(p).toInt()
            // make sure plaintext is prepared before measuring energy (consistent with earlier design)
            BenchmarkPlain.build(size)

            val encTimes = LongArray(rounds)
            val decTimes = LongArray(rounds)

            val sizeRunner = factory.prepare(size)
            val energy = meter.measure {
                repeat(rounds) { i ->
                    val (encNs, decNs) = sizeRunner.run(i)
                    encTimes[i] = encNs
                    decTimes[i] = decNs
                }
            }

            val encStats = TimingStats(
                min = encTimes.minOrNull() ?: 0L,
                median = median(encTimes),
                max = encTimes.maxOrNull() ?: 0L,
                stddev = stddev(encTimes)
            )
            val decStats = TimingStats(
                min = decTimes.minOrNull() ?: 0L,
                median = median(decTimes),
                max = decTimes.maxOrNull() ?: 0L,
                stddev = stddev(decTimes)
            )

            logger.appendLine(ResultRow(size, encStats, decStats, energy.energyMilliWattHour).toCsv())
        }
        return logger.path()
    }
}
