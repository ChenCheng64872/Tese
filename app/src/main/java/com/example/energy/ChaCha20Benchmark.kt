package com.example.energy

import android.content.Context
import com.example.energy.chacha20.ChaCha20Cipher
import kotlin.system.measureNanoTime

object ChaCha20Benchmark {
    private val KEY = ByteArray(32) { 0x22 }
    private val NONCE = ByteArray(12) { 0x33 }

    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        rounds: Int = 30,
        innerIterations: Int = 10,
        warmupRounds: Int = 1,
        fileName: String = "chacha20_bench_2p${minPow}_2p${maxPow}.csv",
        subDir: String? = null
    ): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)

            repeat(warmupRounds) {
                val tmp = ChaCha20Cipher.encrypt(plain, KEY, NONCE)
                ChaCha20Cipher.decrypt(tmp, KEY, NONCE)
            }

            RoundRunner {
                lateinit var ctB64: String
                var encMem = 0L
                var decMem = 0L

                val encNsTotal = measureNanoTime {
                    encMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            ctB64 = ChaCha20Cipher.encrypt(plain, KEY, NONCE)
                        }
                    }
                }

                val decNsTotal = measureNanoTime {
                    decMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            ChaCha20Cipher.decrypt(ctB64, KEY, NONCE)
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