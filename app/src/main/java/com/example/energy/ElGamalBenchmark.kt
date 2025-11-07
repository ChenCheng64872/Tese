package com.example.energy

import android.content.Context
import com.example.energy.elgamal.ElGamalLite
import java.security.SecureRandom
import kotlin.system.measureNanoTime
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters

object ElGamalBenchmark {
    fun runRangeAndLog(
        context: Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        rounds: Int = 15,
        fileName: String = "elgamal_bench_2p${minPow}_2p${maxPow}.csv"
    ): String {
        val rnd = SecureRandom()

        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            val kp = ElGamalLite.generateKeyPair()
            val pub = kp.public as ElGamalPublicKeyParameters
            val priv = kp.private as ElGamalPrivateKeyParameters

            RoundRunner {
                val aesKey = ByteArray(32).apply { rnd.nextBytes(this) }
                val iv     = ByteArray(16).apply { rnd.nextBytes(this) }

                lateinit var ctB64: String
                lateinit var wrapped: ByteArray

                val encNs = measureNanoTime {
                    ctB64 = AES.encrypt(plain, aesKey, iv)                 // bulk
                    wrapped = ElGamalLite.encryptSmall(aesKey + iv, pub)   // wrap key+iv
                }
                val decNs = measureNanoTime {
                    val unwrapped = ElGamalLite.decryptSmall(wrapped, priv)
                    val recKey = unwrapped.copyOfRange(0, 32)
                    val recIv  = unwrapped.copyOfRange(32, 48)
                    @Suppress("UNUSED_VARIABLE")
                    val back = AES.decrypt(ctB64, recKey, recIv)
                }
                encNs to decNs
            }
        }
        return BenchmarkRunner.runRangeAndLog(context, minPow, maxPow, rounds, factory, fileName)
    }
}
