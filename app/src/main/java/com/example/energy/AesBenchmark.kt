package com.example.energy

import android.content.Context
import kotlin.system.measureNanoTime

object AesBenchmark {
    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        rounds: Int = 15,
        fileName: String = "aes_bench_2p${minPow}_2p${maxPow}.csv"
    ): String {
        // Fixed, repeatable key for benchmarking (PBKDF2 from password+salt).
        val password = "test-password"
        val salt = ByteArray(16) { 0x33 }
        val keyBits = AES.KEY_SIZE_256_BITS
        val iterations = AES.KEY_GENERATION_ITERATIONS
        val key = AES.createKey(password, salt, iterations, keyBits).key
        val blankIv = AES.IV_BLANK

        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ctB64: String
                val encNs = measureNanoTime { ctB64 = AES.encrypt(plain, key, blankIv) }
                val decNs = measureNanoTime { AES.decrypt(ctB64, key, blankIv) }
                encNs to decNs
            }
        }

        return BenchmarkRunner.runRangeAndLog(
            context = context,
            minPow = minPow,
            maxPow = maxPow,
            rounds = rounds,
            factory = factory,
            fileName = fileName
        )
    }
}
