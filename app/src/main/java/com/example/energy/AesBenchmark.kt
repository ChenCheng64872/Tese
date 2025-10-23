package com.example.energy

import kotlin.math.pow
import kotlin.system.measureNanoTime
import java.security.SecureRandom

object AesBenchmark {

    data class Result(
        val sizeBytes: Int,
        val rounds: Int,
        val encryptNanosMedian: Long,
        val decryptNanosMedian: Long
    ) {
        fun asLine(): String {
            val pow = Integer.numberOfTrailingZeros(sizeBytes)
            return "2^$pow (${sizeBytes}B)  enc=${encryptNanosMedian} ns  dec=${decryptNanosMedian} ns"
        }
    }

    /**
     * Benchmarks AES/CBC/PKCS7 using the AES helper (BouncyCastle).
     *
     * @param password  password for PBKDF2 (test only)
     * @param salt      salt for PBKDF2 (random if not provided)
     * @param keyBits   128/192/256 (default 256)
     * @param iterations PBKDF2 iterations (default AES.KEY_GENERATION_ITERATIONS)
     * @param minPow    smallest size as 2^minPow bytes (default 4 => 16 bytes)
     * @param maxPow    largest size as 2^maxPow bytes (default 16 => 65536 bytes)
     * @param rounds    number of repetitions per size (default 5, median reported)
     * @param useBlankIv if true, uses AES.IV_BLANK (deterministic); otherwise random 16B IV per op
     */
    fun run(
        password: String = "test-password",
        salt: ByteArray = ByteArray(16).apply { SecureRandom().nextBytes(this) },
        keyBits: Int = AES.KEY_SIZE_256_BITS,
        iterations: Int = AES.KEY_GENERATION_ITERATIONS,
        minPow: Int = 4,
        maxPow: Int = 16,
        rounds: Int = 5,
        useBlankIv: Boolean = true
    ): List<Result> {

        require(minPow <= maxPow) { "minPow must be <= maxPow" }
        require(rounds >= 1) { "rounds must be >= 1" }

        // Derive key once for the whole run (benchmarking payload size, not PBKDF2)
        val keyParam = AES.createKey(password, salt, iterations, keyBits)
        val key = keyParam.key

        // Warm-up (avoid first-run bias)
        warmUp(key)

        val results = mutableListOf<Result>()
        for (powVal in minPow..maxPow) {
            val size = 2.0.pow(powVal).toInt()
            val plain = buildString(size) { repeat(size) { append('A') } }

            val encTimes = LongArray(rounds)
            val decTimes = LongArray(rounds)

            repeat(rounds) { r ->
                val iv = if (useBlankIv) AES.IV_BLANK else ByteArray(16).also { SecureRandom().nextBytes(it) }

                // measure encrypt
                val encNanos = measureNanoTime {
                    AES.encrypt(plain, key, iv)
                }
                encTimes[r] = encNanos

                // measure decrypt (encrypt once, then decrypt)
                val cipher = AES.encrypt(plain, key, iv)
                val decNanos = measureNanoTime {
                    AES.decrypt(cipher, key, iv)
                }
                decTimes[r] = decNanos
            }

            results += Result(
                sizeBytes = size,
                rounds = rounds,
                encryptNanosMedian = median(encTimes),
                decryptNanosMedian = median(decTimes)
            )
        }
        return results
    }

    // --- helpers ---

    private fun warmUp(key: ByteArray) {
        val iv = AES.IV_BLANK
        val s = "warmup"
        repeat(3) {
            val c = AES.encrypt(s, key, iv)
            AES.decrypt(c, key, iv)
        }
    }

    private fun median(values: LongArray): Long {
        val copy = values.copyOf()
        copy.sort()
        val mid = copy.size / 2
        return if (copy.size % 2 == 0) {
            ((copy[mid - 1] + copy[mid]) / 2.0).toLong()
        } else {
            copy[mid]
        }
    }
}
