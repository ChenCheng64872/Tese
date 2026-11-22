package com.example.energy

import com.example.energy.chacha20.ChaCha20Cipher
import com.example.energy.elgamal.ElGamalLite
import com.example.energy.lwc.AeadSpec
import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import com.example.energy.rsa.RsaLite
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.engines.AsconAEAD128
import org.bouncycastle.crypto.engines.ElephantEngine
import org.bouncycastle.crypto.engines.GiftCofbEngine
import org.bouncycastle.crypto.engines.Grain128AEADEngine
import org.bouncycastle.crypto.engines.XoodyakEngine
import org.bouncycastle.crypto.modes.AEADCipher
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.util.encoders.Base64
import java.security.SecureRandom

/**
 * One benchmark case, shared by the desktop harness (DesktopMain.kt) and Android
 * (CryptoBenchmarkInstrumentedTest), so both platforms time exactly the same code.
 *
 * Algorithm names: aes, aesgcm, aesgcmbc, chacha20, rsa, elgamal, ascon, elephant, giftcofb,
 * grain128, xoodyak. Optional key size in the name: aes128/192/256, aesgcm128.., aesgcmbc128..,
 * rsa1024/2048/3072/4096, elgamal768/1024/2048/3072/4096. Without a number: AES-256,
 * RSA-2048, ElGamal-2048. The RSA/ElGamal hybrids always use AES-256-CBC for the bulk data.
 */
class CryptoCase(val algorithm: String, private val random: SecureRandom = SecureRandom()) {
    val baseAlgorithm: String
    val keyBits: Int

    init {
        val sized = Regex("^(aesgcmbc|aesgcm|aes|rsa|elgamal)(\\d+)$").matchEntire(algorithm)
        baseAlgorithm = sized?.groupValues?.get(1) ?: algorithm
        require(baseAlgorithm in BASE_ALGORITHMS) { "Unknown algorithm: $algorithm" }
        keyBits = sized?.groupValues?.get(2)?.toInt() ?: when (baseAlgorithm) {
            "aes", "aesgcm", "aesgcmbc" -> 256; "rsa" -> 2048; "elgamal" -> ElGamalLite.DEFAULT_BITS; else -> 0
        }
        when (baseAlgorithm) {
            "aes", "aesgcm", "aesgcmbc" -> require(keyBits in setOf(128, 192, 256)) { "AES key size must be 128, 192 or 256" }
            "rsa" -> require(keyBits in setOf(1024, 2048, 3072, 4096)) { "RSA size must be 1024, 2048, 3072 or 4096" }
            "elgamal" -> require(keyBits in ElGamalLite.SUPPORTED_BITS) { "ElGamal group must be one of ${ElGamalLite.SUPPORTED_BITS}" }
        }
    }

    // Key material for the whole case (as in the original desktop harness).
    private val key = ByteArray(32).also(random::nextBytes)
    private val iv = ByteArray(16)
    private val symKey = ByteArray(if (baseAlgorithm.startsWith("aes")) keyBits / 8 else 32).also(random::nextBytes)
    private val rsaPair by lazy { RsaLite.generateKeyPair(keyBits) }
    private val elGamalPair by lazy { ElGamalLite.generateKeyPair(keyBits) }

    /** Encrypt and decrypt closures for one plaintext. decrypt() returns the recovered text. */
    fun build(plain: String): Pair<() -> Unit, () -> String> {
        var ciphertext = ""; var wrapped = ByteArray(0)
        val encrypt: () -> Unit
        val decrypt: () -> String
        when (baseAlgorithm) {
            "aes" -> { encrypt = { ciphertext = AES.encrypt(plain, symKey, iv) }; decrypt = { AES.decrypt(ciphertext, symKey, iv) } }
            // JCA provider: SunJCE on desktop JVMs, Conscrypt (BoringSSL, native) on Android.
            "aesgcm" -> { encrypt = { ciphertext = AESGcm.encrypt(plain, symKey) }; decrypt = { AESGcm.decrypt(ciphertext, symKey) } }
            // AES-GCM through Bouncy Castle (pure Java/Kotlin, like every other algorithm here).
            // Same envelope as AESGcm: Base64(IV(12) || CIPHERTEXT || TAG(16)), random IV per encryption.
            "aesgcmbc" -> {
                fun gcm(forEncryption: Boolean, nonce: ByteArray) =
                    GCMBlockCipher.newInstance(AESEngine.newInstance()).also {
                        it.init(forEncryption, AEADParameters(KeyParameter(symKey), 128, nonce))
                    }
                encrypt = {
                    val nonce = ByteArray(12).also(random::nextBytes)
                    val pt = plain.toByteArray(Charsets.UTF_8)
                    val c = gcm(true, nonce)
                    val out = ByteArray(12 + c.getOutputSize(pt.size))
                    System.arraycopy(nonce, 0, out, 0, 12)
                    var n = c.processBytes(pt, 0, pt.size, out, 12)
                    n += c.doFinal(out, 12 + n)
                    ciphertext = Base64.toBase64String(out, 0, 12 + n)
                }
                decrypt = {
                    val all = Base64.decode(ciphertext)
                    val c = gcm(false, all.copyOfRange(0, 12))
                    val out = ByteArray(c.getOutputSize(all.size - 12))
                    var n = c.processBytes(all, 12, all.size - 12, out, 0)
                    n += c.doFinal(out, n)
                    String(out, 0, n, Charsets.UTF_8)
                }
            }
            "chacha20" -> {
                val nonce = ByteArray(12).also(random::nextBytes)
                encrypt = { ciphertext = ChaCha20Cipher.encrypt(plain, key, nonce) }
                decrypt = { ChaCha20Cipher.decrypt(ciphertext, key, nonce) }
            }
            "rsa" -> {
                val pair = rsaPair
                encrypt = { random.nextBytes(key); random.nextBytes(iv); ciphertext = AES.encrypt(plain, key, iv); wrapped = RsaLite.encryptSmall(key + iv, pair.public as RSAKeyParameters) }
                decrypt = { val b = RsaLite.decryptSmall(wrapped, pair.private as RSAKeyParameters); AES.decrypt(ciphertext, b.copyOfRange(0, 32), b.copyOfRange(32, 48)) }
            }
            "elgamal" -> {
                val pair = elGamalPair
                encrypt = { random.nextBytes(key); random.nextBytes(iv); ciphertext = AES.encrypt(plain, key, iv); wrapped = ElGamalLite.encryptSmall(key + iv, pair.public as ElGamalPublicKeyParameters) }
                decrypt = { val b = ElGamalLite.decryptSmall(wrapped, pair.private as ElGamalPrivateKeyParameters); AES.decrypt(ciphertext, b.copyOfRange(0, 32), b.copyOfRange(32, 48)) }
            }
            else -> {
                val (spec, engine) = lwc(baseAlgorithm)
                val aeadKey = ByteArray(spec.keyBytes).also(random::nextBytes)
                val nonce = ByteArray(spec.nonceBytes)
                encrypt = { random.nextBytes(nonce); ciphertext = BcLwcAead.encryptUtf8(engine(), spec, plain, aeadKey, nonce) }
                decrypt = { BcLwcAead.decryptUtf8(engine(), spec, ciphertext, aeadKey) }
            }
        }
        return encrypt to decrypt
    }

    companion object {
        val BASE_ALGORITHMS = setOf("aes", "aesgcm", "aesgcmbc", "chacha20", "rsa", "elgamal",
            "ascon", "elephant", "giftcofb", "grain128", "xoodyak")

        private fun lwc(name: String): Pair<AeadSpec, () -> AEADCipher> = when (name) {
            "ascon" -> LwcSpecs.ASCON_128 to { AsconAEAD128() }
            "elephant" -> LwcSpecs.ELEPHANT to { ElephantEngine(ElephantEngine.ElephantParameters.elephant200) }
            "giftcofb" -> LwcSpecs.GIFT_COFB to { GiftCofbEngine() }
            "grain128" -> LwcSpecs.GRAIN128 to { Grain128AEADEngine() }
            else -> LwcSpecs.XOODYAK to { XoodyakEngine() }
        }
    }
}

/** Result of the time-based warm-up (see [warmUp]). */
data class WarmupResult(val smallPairs: Int, val pairs: Int, val startEpochMs: Long, val endEpochMs: Long)

/**
 * Time-based warm-up before the first measured batch: warmupMs on a 2^smallPower-byte input
 * (skipped when smallPower == 0), then warmupMs on the measured plaintext. Small inputs reach the
 * JIT thresholds quickly (many calls); large inputs alone can leave OSR-compiled loops and, for
 * SunJCE AES-GCM, the intrinsics unused for 25+ s at 1 MiB. Every phase starts with 3 checked
 * round trips.
 */
fun CryptoCase.warmUp(plain: String, encrypt: () -> Unit, decrypt: () -> String, warmupMs: Long, smallPower: Int): WarmupResult {
    val startEpoch = System.currentTimeMillis()
    var smallPairs = 0
    if (smallPower > 0) {
        val smallPlain = "A".repeat(1 shl smallPower)
        val (enc, dec) = build(smallPlain)
        repeat(3) { enc(); check(dec() == smallPlain) }
        val t0 = System.nanoTime()
        while (System.nanoTime() - t0 < warmupMs * 1_000_000) { enc(); dec(); smallPairs++ }
    }
    repeat(3) { encrypt(); check(decrypt() == plain) }
    val t0 = System.nanoTime(); var pairs = 0
    while (System.nanoTime() - t0 < warmupMs * 1_000_000) { encrypt(); decrypt(); pairs++ }
    check(decrypt() == plain)
    return WarmupResult(smallPairs, pairs, startEpoch, System.currentTimeMillis())
}

/** One measured batch: repeat inner encryptions + inner decryptions until targetMs has passed. */
data class Batch(val pairs: Int, val encNs: Long, val decNs: Long, val startEpochMs: Long, val endEpochMs: Long)

fun runBatch(encrypt: () -> Unit, decrypt: () -> String, inner: Int, targetMs: Long): Batch {
    var enc = 0L; var dec = 0L; var iterations = 0
    val epochStart = System.currentTimeMillis(); val start = System.nanoTime()
    do {
        var t = System.nanoTime(); repeat(inner) { encrypt() }; enc += System.nanoTime() - t
        t = System.nanoTime(); repeat(inner) { decrypt() }; dec += System.nanoTime() - t
        iterations += inner
    } while (System.nanoTime() - start < targetMs * 1_000_000)
    return Batch(iterations, enc, dec, epochStart, System.currentTimeMillis())
}
