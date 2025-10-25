package com.example.energy.rsa

import org.bouncycastle.crypto.AsymmetricBlockCipher
import org.bouncycastle.crypto.encodings.OAEPEncoding
import org.bouncycastle.crypto.engines.RSAEngine
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
import org.bouncycastle.crypto.params.RSAKeyParameters
import java.math.BigInteger
import java.security.SecureRandom

object RsaLite {

    fun generateKeyPair(bits: Int = 2048): org.bouncycastle.crypto.AsymmetricCipherKeyPair {
        val gen = RSAKeyPairGenerator()
        val e = BigInteger.valueOf(65537L)
        gen.init(RSAKeyGenerationParameters(e, SecureRandom(), bits, 64))
        return gen.generateKeyPair()
    }

    // OAEP w/ SHA-256
    private fun engine(forEncryption: Boolean, key: RSAKeyParameters): AsymmetricBlockCipher {
        val enc = OAEPEncoding(RSAEngine(), org.bouncycastle.crypto.digests.SHA256Digest(), org.bouncycastle.crypto.digests.SHA256Digest(), null)
        enc.init(forEncryption, key)
        return enc
    }

    /** Encrypt a SMALL blob (e.g., AES key+IV). */
    fun encryptSmall(data: ByteArray, pub: RSAKeyParameters): ByteArray {
        val e = engine(true, pub)
        return e.processBlock(data, 0, data.size)
    }

    /** Decrypt a SMALL blob encrypted with encryptSmall. */
    fun decryptSmall(cipher: ByteArray, priv: RSAKeyParameters): ByteArray {
        val e = engine(false, priv)
        return e.processBlock(cipher, 0, cipher.size)
    }
}
