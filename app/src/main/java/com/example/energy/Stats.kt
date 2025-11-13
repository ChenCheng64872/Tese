package com.example.energy

import kotlin.math.sqrt

fun computeStatsNs(samples: List<Long>): TimingStats {
    require(samples.isNotEmpty())
    val sorted = samples.sorted()
    val min = sorted.first()
    val max = sorted.last()

    val median = if (sorted.size % 2 == 1) {
        sorted[sorted.size / 2]
    } else {
        val a = sorted[sorted.size / 2 - 1]
        val b = sorted[sorted.size / 2]
        (a + b) / 2
    }

    val mean = sorted.average()
    val variance = if (sorted.size > 1) {
        val acc = sorted.sumOf { (it - mean) * (it - mean) }
        acc / (sorted.size - 1)
    } else 0.0
    val std = sqrt(variance)
    val stdPercent = if (mean != 0.0) std / mean * 100.0 else 0.0

    return TimingStats(
        min = min,
        median = median,
        max = max,
        mean = mean,
        stddev = std,
        stdPercent = stdPercent
    )
}
