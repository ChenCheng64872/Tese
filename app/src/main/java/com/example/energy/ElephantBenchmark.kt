package com.example.energy

import android.content.Context
import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import org.bouncycastle.crypto.engines.ElephantEngine
import kotlin.system.measureNanoTime

object ElephantBenchmark {
    private val SPEC = LwcSpecs.ELEPHANT
    private val KEY = ByteArray(SPEC.keyBytes) { 0x51 }
    private val NONCE = ByteArray(SPEC.nonceBytes) { 0x52 }

    private fun engine(): ElephantEngine =
        ElephantEngine(ElephantEngine.ElephantParameters.elephant200)

    fun runRangeAndLog(context: Context, minPow: Int, maxPow: Int, rounds: Int,
                       fileName: String = "elephant_bench_2p${minPow}_2p${maxPow}.csv"): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ct: String
                val encNs = measureNanoTime { ct = BcLwcAead.encryptUtf8(engine(), SPEC, plain, KEY, NONCE) }
                val decNs = measureNanoTime { BcLwcAead.decryptUtf8(engine(), SPEC, ct, KEY) }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}