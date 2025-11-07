package com.example.energy

import kotlin.math.pow
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
    // 样本标准差（n-1）
    val mean = sorted.average()
    val variance = if (sorted.size > 1) {
        sorted.sumOf { (it - mean).pow2() } / (sorted.size - 1)
    } else 0.0
    val std = sqrt(variance)
    return TimingStats(min, median, max, std)
}

private fun Double.pow2() = (this * this)
private fun Long.pow2() = (this.toDouble() * this.toDouble())
