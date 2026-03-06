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

    fun runRangeAndLog(context: Context, minPow: Int, maxPow: Int, rounds: Int,
                       fileName: String = "xoodyak_bench_2p${minPow}_2p${maxPow}.csv"): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ct: String
                val encNs = measureNanoTime { ct = BcLwcAead.encryptUtf8(XoodyakEngine(), SPEC, plain, KEY, NONCE) }
                val decNs = measureNanoTime { BcLwcAead.decryptUtf8(XoodyakEngine(), SPEC, ct, KEY) }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}