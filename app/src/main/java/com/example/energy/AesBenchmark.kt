package com.example.energy

import android.content.Context
import kotlin.system.measureNanoTime

object AesBenchmark {
    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        rounds: Int = 10,
        innerIterations: Int = 1,
        warmupRounds: Int = 1,
        fileName: String = "aes_bench_2p${minPow}_2p${maxPow}.csv",
        subDir: String? = null
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

            repeat(warmupRounds) {
                val tmp = AES.encrypt(plain, key, blankIv)
                AES.decrypt(tmp, key, blankIv)
            }

            RoundRunner {
                lateinit var ctB64: String
                var encMem = 0L
                var decMem = 0L

                val encNsTotal = measureNanoTime {
                    encMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            ctB64 = AES.encrypt(plain, key, blankIv)
                        }
                    }
                }

                val decNsTotal = measureNanoTime {
                    decMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            AES.decrypt(ctB64, key, blankIv)
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

        return BenchmarkRunner.runRangeAndLog(
            context = context,
            minPow = minPow,
            maxPow = maxPow,
            rounds = rounds,
            factory = factory,
            fileName = fileName,
            subDir = subDir
        )
    }
}
