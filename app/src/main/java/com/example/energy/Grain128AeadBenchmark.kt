package com.example.energy

import android.content.Context
import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import org.bouncycastle.crypto.engines.Grain128AEADEngine
import kotlin.system.measureNanoTime

object Grain128AeadBenchmark {
    private val SPEC = LwcSpecs.GRAIN128
    private val KEY = ByteArray(SPEC.keyBytes) { 0x71 }
    private val NONCE = ByteArray(SPEC.nonceBytes) { 0x72 }

    fun runRangeAndLog(context: Context, minPow: Int, maxPow: Int, rounds: Int,
                       fileName: String = "grain128aead_bench_2p${minPow}_2p${maxPow}.csv"): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ct: String
                val encNs = measureNanoTime { ct = BcLwcAead.encryptUtf8(Grain128AEADEngine(), SPEC, plain, KEY, NONCE) }
                val decNs = measureNanoTime { BcLwcAead.decryptUtf8(Grain128AEADEngine(), SPEC, ct, KEY) }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}