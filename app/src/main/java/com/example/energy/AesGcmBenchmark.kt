package com.example.energy

import android.content.Context
import kotlin.system.measureNanoTime

object AesGcmBenchmark {
    private val KEY = ByteArray(32) { 0x11 } // fixed for reproducible runs

    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        rounds: Int = 15,
        fileName: String = "aesgcm_bench_2p${minPow}_2p${maxPow}.csv"
    ): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ctB64: String
                val encNs = measureNanoTime { ctB64 = AESGcm.encrypt(plain, KEY) } // GCM prefixes nonce
                val decNs = measureNanoTime { AESGcm.decrypt(ctB64, KEY) }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}
