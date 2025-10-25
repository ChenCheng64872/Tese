package com.example.energy

import java.security.SecureRandom
import kotlin.math.pow
import kotlin.system.measureNanoTime

object AesGcmBenchmark {

    data class SingleResult(
        val sizeBytes: Int,
        val encMin: Long,
        val encMedian: Long,
        val encMax: Long,
        val decMin: Long,
        val decMedian: Long,
        val decMax: Long
    )

    // ---- Constant 1024-byte base sample (deterministic) ----
    private val BASE_SAMPLE: String = buildString(1024) {
        val pattern = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        while (length < 1024) append(pattern)
    }.substring(0, 1024)

    private fun buildPlain(size: Int): String {
        val b = StringBuilder(size)
        while (b.length < size) {
            val r = size - b.length
            if (r >= BASE_SAMPLE.length) b.append(BASE_SAMPLE) else b.append(BASE_SAMPLE.substring(0, r))
        }
        return b.toString()
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

    private fun median(values: LongArray): Long {
        val s = values.sorted()
        val m = s.size / 2
        return if (s.size % 2 == 0) ((s[m - 1] + s[m]) / 2) else s[m]
    }

    private fun runSingleSize(sizePow: Int, rounds: Int = 15): SingleResult {
        val rnd = SecureRandom()
        val key = ByteArray(32).also { rnd.nextBytes(it) } // AES-256

        val size = 2.0.pow(sizePow).toInt()
        val plain = buildPlain(size)

        val enc = LongArray(rounds)
        val dec = LongArray(rounds)

        repeat(rounds) {
            var ct: String
            enc[it] = measureNanoTime {
                ct = AESGcm.encrypt(plain, key) // fresh IV inside
            }
            val ctRound = AESGcm.encrypt(plain, key)
            dec[it] = measureNanoTime {
                @Suppress("UNUSED_VARIABLE")
                val back = AESGcm.decrypt(ctRound, key)
            }
        }

        return SingleResult(
            sizeBytes = size,
            encMin = enc.minOrNull() ?: 0,
            encMedian = median(enc),
            encMax = enc.maxOrNull() ?: 0,
            decMin = dec.minOrNull() ?: 0,
            decMedian = median(dec),
            decMax = dec.maxOrNull() ?: 0
        )
    }

    fun runRangeAndLog(
        context: android.content.Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        fileName: String = "aesgcm_bench_2p10_2p20.csv"
    ): String {
        val logger = CsvLogger(context, fileName, csvHeader())
        for (p in minPow..maxPow) {
            val r = runSingleSize(p, 15)
            logger.appendLine(toCsvLine(r))
        }
        return logger.file().absolutePath
    }
}
