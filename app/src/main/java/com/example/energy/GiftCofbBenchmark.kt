package com.example.energy

import android.content.Context
import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import org.bouncycastle.crypto.engines.GiftCofbEngine
import kotlin.system.measureNanoTime

object GiftCofbBenchmark {
    private val SPEC = LwcSpecs.GIFT_COFB
    private val KEY = ByteArray(SPEC.keyBytes) { 0x61 }
    private val NONCE = ByteArray(SPEC.nonceBytes) { 0x62 }

    fun runRangeAndLog(context: Context, minPow: Int, maxPow: Int, rounds: Int,
                       fileName: String = "giftcofb_bench_2p${minPow}_2p${maxPow}.csv"): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ct: String
                val encNs = measureNanoTime { ct = BcLwcAead.encryptUtf8(GiftCofbEngine(), SPEC, plain, KEY, NONCE) }
                val decNs = measureNanoTime { BcLwcAead.decryptUtf8(GiftCofbEngine(), SPEC, ct, KEY) }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}