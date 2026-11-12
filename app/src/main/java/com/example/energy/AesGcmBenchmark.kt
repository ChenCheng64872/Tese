package com.example.energy

import android.content.Context
import kotlin.system.measureNanoTime

object AesGcmBenchmark {
    private val KEY = ByteArray(32) { 0x11 } // fixed for reproducible runs

    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        rounds: Int = 30,
        innerIterations: Int = 10,
        warmupRounds: Int = 1,
        fileName: String = "aesgcm_bench_2p${minPow}_2p${maxPow}.csv",
        subDir: String? = null
    ): String {
        require(innerIterations > 0 && warmupRounds >= 0)
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            repeat(warmupRounds) {
                val tmp = AESGcm.encrypt(plain, KEY)
                check(AESGcm.decrypt(tmp, KEY) == plain) { "Round-trip mismatch" }
            }

            RoundRunner {
                lateinit var ctB64: String

                val encNsTotal = measureNanoTime {
                    repeat(innerIterations) {
                        ctB64 = AESGcm.encrypt(plain, KEY)
                    }
                }

                val decNsTotal = measureNanoTime {
                    repeat(innerIterations) {
                        AESGcm.decrypt(ctB64, KEY)
                    }
                }

                RoundResult(
                    encNs = encNsTotal / innerIterations,
                    decNs = decNsTotal / innerIterations,
                    encMemBytes = -1,
                    decMemBytes = -1,
                    innerIterations = innerIterations
                )
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName, subDir)
    }
}
