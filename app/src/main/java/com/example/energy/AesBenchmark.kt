package com.example.energy

import android.content.Context
import java.security.SecureRandom
import kotlin.math.pow
import kotlin.system.measureNanoTime

object AesBenchmark {

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

    // ---- Build plaintext by repeating the base ----
    private fun buildPlain(size: Int): String {
        val builder = StringBuilder(size)
        while (builder.length < size) {
            val remaining = size - builder.length
            if (remaining >= BASE_SAMPLE.length)
                builder.append(BASE_SAMPLE)
            else
                builder.append(BASE_SAMPLE.substring(0, remaining))
        }
        return builder.toString()
    }

    // ---- CSV helpers (no “rounds” column) ----
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

    // ---- Single-size benchmark (fixed 15 rounds) ----
    private fun runSingleSize(
        sizePow: Int,
        rounds: Int = 15,
        password: String = "test-password",
        salt: ByteArray = ByteArray(16).apply { SecureRandom().nextBytes(this) },
        keyBits: Int = AES.KEY_SIZE_256_BITS,
        iterations: Int = AES.KEY_GENERATION_ITERATIONS,
        useBlankIv: Boolean = true
    ): SingleResult {
        val keyParam = AES.createKey(password, salt, iterations, keyBits)
        val key = keyParam.key
        warmUp(key)

        val size = 2.0.pow(sizePow).toInt()
        val plain = buildPlain(size)

        val encTimes = LongArray(rounds)
        val decTimes = LongArray(rounds)

        repeat(rounds) { i ->
            val iv = if (useBlankIv) AES.IV_BLANK else ByteArray(16).also { SecureRandom().nextBytes(it) }
            val encNanos = measureNanoTime { AES.encrypt(plain, key, iv) }
            encTimes[i] = encNanos

            val cipher = AES.encrypt(plain, key, iv)
            val decNanos = measureNanoTime { AES.decrypt(cipher, key, iv) }
            decTimes[i] = decNanos
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

    // ---- Range benchmark (2^10 → 2^20, 15 rounds each) ----
    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        fileName: String = "aes_bench_2p10_2p20.csv"
    ): String {
        val logger = CsvLogger(context, fileName, csvHeader())

        for (powVal in minPow..maxPow) {
            val result = runSingleSize(powVal, rounds = 15)
            logger.appendLine(toCsvLine(result))
        }

        return logger.file().absolutePath
    }

    // ---- Helpers ----
    private fun warmUp(key: ByteArray) {
        val iv = AES.IV_BLANK
        val s = "warmup"
        repeat(3) {
            val c = AES.encrypt(s, key, iv)
            AES.decrypt(c, key, iv)
        }
    }

    private fun median(values: LongArray): Long {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0)
            ((sorted[mid - 1] + sorted[mid]) / 2)
        else
            sorted[mid]
    }
}
