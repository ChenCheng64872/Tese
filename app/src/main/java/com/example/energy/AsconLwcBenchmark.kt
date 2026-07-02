package com.example.energy

import android.content.Context
import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import org.bouncycastle.crypto.engines.AsconAEAD128
import kotlin.system.measureNanoTime

object AsconLwcBenchmark {
    private val SPEC = LwcSpecs.ASCON_128
    private val KEY = ByteArray(SPEC.keyBytes) { 0x41 }
    private val NONCE = ByteArray(SPEC.nonceBytes) { 0x42 }

    fun runRangeAndLog(
        context: Context,
        minPow: Int,
        maxPow: Int,
        rounds: Int,
        innerIterations: Int = 1,
        warmupRounds: Int = 5,
        fileName: String = "ascon_bench_2p${minPow}_2p${maxPow}.csv",
        subDir: String? = null
    ): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)

            repeat(warmupRounds) {
                val tmp = BcLwcAead.encryptUtf8(AsconAEAD128(), SPEC, plain, KEY, NONCE)
                BcLwcAead.decryptUtf8(AsconAEAD128(), SPEC, tmp, KEY)
            }

            RoundRunner {
                lateinit var ct: String
                var encMem = 0L
                var decMem = 0L

                val encNsTotal = measureNanoTime {
                    encMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            ct = BcLwcAead.encryptUtf8(AsconAEAD128(), SPEC, plain, KEY, NONCE)
                        }
                    }
                }

                val decNsTotal = measureNanoTime {
                    decMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            BcLwcAead.decryptUtf8(AsconAEAD128(), SPEC, ct, KEY)
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