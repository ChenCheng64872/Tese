package com.example.energy

import com.example.energy.elgamal.ElGamalLite
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters
import java.security.SecureRandom
import kotlin.math.pow
import kotlin.system.measureNanoTime

object ElGamalBenchmark {

    data class SingleResult(
        val sizeBytes: Int,
        val encMin: Long,
        val encMedian: Long,
        val encMax: Long,
        val decMin: Long,
        val decMedian: Long,
        val decMax: Long
    )

    private val BASE_SAMPLE: String = buildString(1024) {
        val pattern = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        while (length < 1024) append(pattern)
    }.substring(0, 1024)

    private fun buildPlain(size: Int): String {
        val b = StringBuilder(size)
        while (b.length < size) {
            val remain = size - b.length
            if (remain >= BASE_SAMPLE.length) b.append(BASE_SAMPLE)
            else b.append(BASE_SAMPLE.substring(0, remain))
        }
        return b.toString()
    }

    fun csvHeader(): String =
        "size_bytes,enc_min_ns,enc_median_ns,enc_max_ns,dec_min_ns,dec_median_ns,dec_max_ns"

    fun toCsvLine(r: SingleResult): String = buildString {
        append(r.sizeBytes); append(',')
        append(r.encMin); append(',')
        append(r.encMedian); append(',')
        append(r.encMax); append(',')
        append(r.decMin); append(',')
        append(r.decMedian); append(',')
        append(r.decMax); append('\n')
    }

    private fun runSingleSize(sizePow: Int, rounds: Int = 15): SingleResult {
        val rnd = SecureRandom()

        // One ElGamal keypair reused across rounds for this size
        val kp = ElGamalLite.generateKeyPair()
        val pub = kp.public as ElGamalPublicKeyParameters
        val priv = kp.private as ElGamalPrivateKeyParameters

        val size = 2.0.pow(sizePow).toInt()
        val plain = buildPlain(size)

        val encTimes = LongArray(rounds)
        val decTimes = LongArray(rounds)

        repeat(rounds) {
            // fresh AES key + IV each round
            val aesKey = ByteArray(32).also { rnd.nextBytes(it) }
            val iv = ByteArray(16).also { rnd.nextBytes(it) }
            val keyBlob = aesKey + iv

            var cipherTextB64: String
            var wrappedKey: ByteArray

            encTimes[it] = measureNanoTime {
                cipherTextB64 = AES.encrypt(plain, aesKey, iv)
                wrappedKey = ElGamalLite.encryptSmall(keyBlob, pub)
            }

            decTimes[it] = measureNanoTime {
                val unwrapped = ElGamalLite.decryptSmall(wrappedKey, priv)
                val recKey = unwrapped.copyOfRange(0, 32)
                val recIv  = unwrapped.copyOfRange(32, 48)
                @Suppress("UNUSED_VARIABLE")
                val back = AES.decrypt(cipherTextB64, recKey, recIv)
            }
        }

        return SingleResult(
            sizeBytes = size,
            encMin = encTimes.minOrNull() ?: 0,
            encMedian = median(encTimes),
            encMax = encTimes.maxOrNull() ?: 0,
            decMin = decTimes.minOrNull() ?: 0,
            decMedian = median(decTimes),
            decMax = decTimes.maxOrNull() ?: 0
        )
    }

    fun runRangeAndLog(
        context: android.content.Context,
        minPow: Int = 10,
        maxPow: Int = 20,
        fileName: String = "elgamal_bench_2p10_2p20.csv"
    ): String {
        require(minPow <= maxPow)
        val logger = CsvLogger(context, fileName, csvHeader())
        for (p in minPow..maxPow) {
            val r = runSingleSize(p, rounds = 15)
            logger.appendLine(toCsvLine(r))
        }
        return logger.file().absolutePath
    }

    private fun median(values: LongArray): Long =
        values.sorted().let { s ->
            val m = s.size / 2
            if (s.size % 2 == 0) (s[m - 1] + s[m]) / 2 else s[m]
        }
}
