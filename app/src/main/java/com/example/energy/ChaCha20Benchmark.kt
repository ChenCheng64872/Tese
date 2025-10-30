package com.example.energy

import com.example.energy.chacha20.ChaCha20Cipher
import kotlin.math.pow
import kotlin.system.measureNanoTime

object ChaCha20Benchmark {

    data class SingleResult(
        val sizeBytes: Int,
        val encMin: Long,
        val encMedian: Long,
        val encMax: Long,
        val decMin: Long,
        val decMedian: Long,
        val decMax: Long
    )

    private val BASE_SAMPLE: String = buildString(1024) {
        val pattern = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        while (length < 1024) append(pattern)
    }.substring(0, 1024)

    private val FIXED_KEY: ByteArray = ByteArray(32) { it.toByte() }           // 00 01 02 ... 1F
    private val FIXED_NONCE: ByteArray = ByteArray(12) { (it * 3).toByte() }   // simple pattern

    private fun buildPlain(size: Int): String {
        val builder = StringBuilder(size)
        while (builder.length < size) {
            val remaining = size - builder.length
            if (remaining >= BASE_SAMPLE.length) builder.append(BASE_SAMPLE)
            else builder.append(BASE_SAMPLE.substring(0, remaining))
        }
        return builder.toString()
    }

    fun csvHeader(): String =
        "size_bytes,enc_min_ns,enc_median_ns,enc_max_ns,dec_min_ns,dec_median_ns,dec_max_ns"

    fun toCsvLine(r: SingleResult): String = buildString {
        append(r.sizeBytes); append(',')
        append(r.encMin); append(',')
        append(r.encMedian); append(',')
        append(r.encMax); append(',')
        append(r.decMin); append(',')
        append(r.decMedian); append(',')
        append(r.decMax); append('\n')
    }

    private fun runSingleSize(sizePow: Int, rounds: Int = 15): SingleResult {
        val size = 2.0.pow(sizePow).toInt()
        val plain = buildPlain(size)

        val encTimes = LongArray(rounds)
        val decTimes = LongArray(rounds)

        repeat(rounds) { i ->
            var cipherText: String
            encTimes[i] = measureNanoTime {
                cipherText = ChaCha20Cipher.encrypt(plain, FIXED_KEY, FIXED_NONCE)
            }
            decTimes[i] = measureNanoTime {
                @Suppress("UNUSED_VARIABLE")
                val back = ChaCha20Cipher.decrypt(cipherText, FIXED_KEY, FIXED_NONCE)
            }
        }

        return SingleResult(
            sizeBytes = size,
            encMin = encTimes.minOrNull() ?: 0,
            encMedian = median(encTimes),
            encMax = encTimes.maxOrNull() ?: 0,
            decMin = decTimes.minOrNull() ?: 0,
            decMedian = median(decTimes),
            decMax = decTimes.maxOrNull() ?: 0
        )
    }

    fun runRangeAndLog(
        context: android.content.Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        fileName: String = "chacha_bench_2p10_2p20.csv"
    ): String {
        require(minPow <= maxPow) { "minPow must be <= maxPow" }
        val logger = CsvLogger(context, fileName, csvHeader())

        for (powVal in minPow..maxPow) {
            val result = runSingleSize(powVal, rounds = 15)
            logger.appendLine(toCsvLine(result))
        }
        return logger.file().absolutePath
    }

    private fun median(values: LongArray): Long =
        values.sorted().let { s ->
            val m = s.size / 2
            if (s.size % 2 == 0) (s[m - 1] + s[m]) / 2 else s[m]
        }
}
