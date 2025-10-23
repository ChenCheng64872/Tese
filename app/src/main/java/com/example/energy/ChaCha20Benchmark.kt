package com.example.energy.chacha20

import kotlin.math.pow
import kotlin.system.measureNanoTime

object ChaChaBenchmark {

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

    fun run(
        minPow: Int = 4,
        maxPow: Int = 16,
        rounds: Int = 5,
        useFixedNonce: Boolean = true, // true => deterministic timing; false => new nonce each round
        key: ByteArray = ChaCha20Cipher.generateKey(),
        fixedNonce: ByteArray = ByteArray(12) { 0 } // only used if useFixedNonce=true
    ): List<Result> {
        require(minPow <= maxPow) { "minPow must be <= maxPow" }
        require(rounds >= 1) { "rounds must be >= 1" }

        // Warm-up to avoid first-use bias/JIT effects
        warmUp(key)

        val results = mutableListOf<Result>()
        for (powVal in minPow..maxPow) {
            val size = 2.0.pow(powVal).toInt()
            val plain = buildString(size) { repeat(size) { append('A') } }

            val encTimes = LongArray(rounds)
            val decTimes = LongArray(rounds)

            repeat(rounds) { r ->
                val nonce = if (useFixedNonce) fixedNonce else ChaCha20Cipher.generateNonce()

                val encNs = measureNanoTime {
                    ChaCha20Cipher.encrypt(plain, key, nonce)
                }
                encTimes[r] = encNs

                val cipher = ChaCha20Cipher.encrypt(plain, key, nonce)
                val decNs = measureNanoTime {
                    ChaCha20Cipher.decrypt(cipher, key, nonce)
                }
                decTimes[r] = decNs
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
        val nonce = ByteArray(12) { 0 }
        val s = "warmup"
        repeat(3) {
            val c = ChaCha20Cipher.encrypt(s, key, nonce)
            ChaCha20Cipher.decrypt(c, key, nonce)
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
