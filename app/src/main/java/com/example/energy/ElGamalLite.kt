package com.example.energy.elgamal

import org.bouncycastle.crypto.AsymmetricBlockCipher
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.agreement.DHStandardGroups
import org.bouncycastle.crypto.encodings.PKCS1Encoding
import org.bouncycastle.crypto.engines.ElGamalEngine
import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator
import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters
import org.bouncycastle.crypto.params.ElGamalParameters
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters
import java.security.SecureRandom


object ElGamalLite {

    /** Default group size: 2048 bits (~112-bit security, same level as RSA-2048). */
    const val DEFAULT_BITS = 2048

    /**
     * Standard MODP groups (g = 2): RFC 2409 groups 1 and 2 (768, 1024 bits) and RFC 3526
     * groups 14-16 (2048, 3072, 4096 bits). The original code used the 768-bit group.
     */
    private val GROUPS = mapOf(
        768 to DHStandardGroups.rfc2409_768,
        1024 to DHStandardGroups.rfc2409_1024,
        2048 to DHStandardGroups.rfc3526_2048,
        3072 to DHStandardGroups.rfc3526_3072,
        4096 to DHStandardGroups.rfc3526_4096,
    )

    val SUPPORTED_BITS: Set<Int> = GROUPS.keys

    fun params(bits: Int = DEFAULT_BITS): ElGamalParameters {
        val dh = GROUPS[bits] ?: throw IllegalArgumentException("Unsupported ElGamal group size: $bits")
        return ElGamalParameters(dh.p, dh.g)
    }

    private val rnd = SecureRandom()

    fun generateKeyPair(bits: Int = DEFAULT_BITS): AsymmetricCipherKeyPair {
        val gen = ElGamalKeyPairGenerator()
        gen.init(ElGamalKeyGenerationParameters(rnd, params(bits)))
        return gen.generateKeyPair()
    }

    private fun engine(forEncryption: Boolean, key: CipherParameters): AsymmetricBlockCipher {
        val enc = PKCS1Encoding(ElGamalEngine())
        enc.init(forEncryption, key)
        return enc
    }

    fun encryptSmall(data: ByteArray, pub: ElGamalPublicKeyParameters): ByteArray {
        val e = engine(true, pub)
        return e.processBlock(data, 0, data.size)
    }

    fun decryptSmall(cipher: ByteArray, priv: ElGamalPrivateKeyParameters): ByteArray {
        val e = engine(false, priv)
        return e.processBlock(cipher, 0, cipher.size)
    }
}
