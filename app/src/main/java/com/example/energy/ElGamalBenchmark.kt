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
        rounds: Int = 20,
        innerIterations: Int = 10,
        warmupRounds: Int = 1,
        fileName: String = "elgamal_bench_2p${minPow}_2p${maxPow}.csv",
        subDir: String? = null
    ): String {
        val rnd = SecureRandom()

        val factory = SizeRunnerFactory { sizeBytes ->
            val plain = BenchmarkPlain.build(sizeBytes)
            val kp = ElGamalLite.generateKeyPair()
            val pub = kp.public as ElGamalPublicKeyParameters
            val priv = kp.private as ElGamalPrivateKeyParameters

            repeat(warmupRounds) {
                val aesKey = ByteArray(32).apply { rnd.nextBytes(this) }
                val iv = ByteArray(16).apply { rnd.nextBytes(this) }
                val ct = AES.encrypt(plain, aesKey, iv)
                val wrapped = ElGamalLite.encryptSmall(aesKey + iv, pub)
                val unwrapped = ElGamalLite.decryptSmall(wrapped, priv)
                val recKey = unwrapped.copyOfRange(0, 32)
                val recIv = unwrapped.copyOfRange(32, 48)
                AES.decrypt(ct, recKey, recIv)
            }

            RoundRunner {
                lateinit var lastCtB64: String
                lateinit var lastWrapped: ByteArray
                var encMem = 0L
                var decMem = 0L

                val encNsTotal = measureNanoTime {
                    encMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            val aesKey = ByteArray(32).apply { rnd.nextBytes(this) }
                            val iv = ByteArray(16).apply { rnd.nextBytes(this) }
                            lastCtB64 = AES.encrypt(plain, aesKey, iv)
                            lastWrapped = ElGamalLite.encryptSmall(aesKey + iv, pub)
                        }
                    }
                }

                val decNsTotal = measureNanoTime {
                    decMem = BenchmarkRunner.measureMemory {
                        repeat(innerIterations) {
                            val unwrapped = ElGamalLite.decryptSmall(lastWrapped, priv)
                            val recKey = unwrapped.copyOfRange(0, 32)
                            val recIv = unwrapped.copyOfRange(32, 48)
                            AES.decrypt(lastCtB64, recKey, recIv)
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