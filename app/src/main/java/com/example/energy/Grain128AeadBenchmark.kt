package com.example.energy

import android.content.Context
import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import org.bouncycastle.crypto.engines.Grain128AEADEngine
import kotlin.system.measureNanoTime

object Grain128AeadBenchmark {
    private val SPEC = LwcSpecs.GRAIN128
    private val KEY = ByteArray(SPEC.keyBytes) { 0x71.toByte() }
    private val NONCE = ByteArray(SPEC.nonceBytes) { 0x72.toByte() }

    private fun engine(): Grain128AEADEngine = Grain128AEADEngine()

    fun runRangeAndLog(
        context: Context,
        minPow: Int,
        maxPow: Int,
        rounds: Int,
        innerIterations: Int = 1,
        warmupRounds: Int = 5,
        fileName: String = "grain128aead_bench_2p${minPow}_2p${maxPow}.csv",
        subDir: String? = null
    ): String {
        require(innerIterations > 0) { "innerIterations must be > 0" }
        require(warmupRounds >= 0) { "warmupRounds must be >= 0" }

        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)

            // Warmup
            repeat(warmupRounds) {
                val tmp = BcLwcAead.encryptUtf8(engine(), SPEC, plain, KEY, NONCE)
                BcLwcAead.decryptUtf8(engine(), SPEC, tmp, KEY)
            }

            RoundRunner {
                var ct = ""
                var encMem = 0L
                var decMem = 0L

                val encNsTotal = measureNanoTime {
                    encMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            ct = BcLwcAead.encryptUtf8(engine(), SPEC, plain, KEY, NONCE)
                        }
                    }
                }

                val decNsTotal = measureNanoTime {
                    decMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            BcLwcAead.decryptUtf8(engine(), SPEC, ct, KEY)
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