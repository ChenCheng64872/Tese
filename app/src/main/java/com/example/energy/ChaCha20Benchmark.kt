package com.example.energy

import android.content.Context
import com.example.energy.chacha20.ChaCha20Cipher
import kotlin.system.measureNanoTime

object ChaCha20Benchmark {
    private val KEY = ByteArray(32) { 0x22 }   // fixed for reproducibility
    private val NONCE = ByteArray(12) { 0x33 } // fixed 96-bit nonce (benchmark only)

    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        rounds: Int = 15,
        fileName: String = "chacha20_bench_2p${minPow}_2p${maxPow}.csv"
    ): String {
        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            RoundRunner {
                lateinit var ctB64: String
                val encNs = measureNanoTime { ctB64 = ChaCha20Cipher.encrypt(plain, KEY, NONCE) }
                val decNs = measureNanoTime { ChaCha20Cipher.decrypt(ctB64, KEY, NONCE) }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}
