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
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            repeat(warmupRounds) {
                val tmp = AESGcm.encrypt(plain, KEY)
                AESGcm.decrypt(tmp, KEY)
            }

            RoundRunner {
                lateinit var ctB64: String
                var encMem = 0L
                var decMem = 0L

                val encNsTotal = measureNanoTime {
                    encMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            ctB64 = AESGcm.encrypt(plain, KEY)
                        }
                    }
                }

                val decNsTotal = measureNanoTime {
                    decMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            AESGcm.decrypt(ctB64, KEY)
                        }
                    }
                }

                RoundResult(
                    encNs = encNsTotal / innerIterations,
                    decNs = decNsTotal / innerIterations,
                    encMemBytes = encMem / innerIterations,
                    decMemBytes = decMem / innerIterations
                )
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName, subDir)
    }
}
