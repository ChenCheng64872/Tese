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
        fileName: String = "ascon_bench_2p${minPow}_2p${maxPow}.csv"
    ): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ct: String
                val encNs = measureNanoTime { ct = BcLwcAead.encryptUtf8(AsconAEAD128(), SPEC, plain, KEY, NONCE) }
                val decNs = measureNanoTime { BcLwcAead.decryptUtf8(AsconAEAD128(), SPEC, ct, KEY) }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}