package com.example.energy

import android.content.Context
import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import org.bouncycastle.crypto.engines.XoodyakEngine
import kotlin.system.measureNanoTime

object XoodyakBenchmark {
    private val SPEC = LwcSpecs.XOODYAK
    private val KEY = ByteArray(SPEC.keyBytes) { 0x81.toByte() }
    private val NONCE = ByteArray(SPEC.nonceBytes) { 0x82.toByte() }

    fun runRangeAndLog(
        context: Context,
        minPow: Int,
        maxPow: Int,
        rounds: Int,
        innerIterations: Int = 1,
        warmupRounds: Int = 5,
        fileName: String = "xoodyak_bench_2p${minPow}_2p${maxPow}.csv",
        subDir: String? = null
    ): String {
        require(innerIterations > 0) { "innerIterations must be > 0" }
        require(warmupRounds >= 0) { "warmupRounds must be >= 0" }

        require(innerIterations > 0 && warmupRounds >= 0)
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)

            repeat(warmupRounds) {
                val tmp = BcLwcAead.encryptUtf8(XoodyakEngine(), SPEC, plain, KEY, NONCE)
                check(BcLwcAead.decryptUtf8(XoodyakEngine(), SPEC, tmp, KEY) == plain) { "Round-trip mismatch" }
            }

            RoundRunner {
                lateinit var ct: String

                val encNsTotal = measureNanoTime {
                    repeat(innerIterations) {
                        ct = BcLwcAead.encryptUtf8(XoodyakEngine(), SPEC, plain, KEY, NONCE)
                    }
                }

                val decNsTotal = measureNanoTime {
                    repeat(innerIterations) {
                        check(BcLwcAead.decryptUtf8(XoodyakEngine(), SPEC, ct, KEY) == plain) { "Round-trip mismatch" }
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