package com.example.energy.elgamal

import org.bouncycastle.crypto.AsymmetricBlockCipher
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.encodings.PKCS1Encoding
import org.bouncycastle.crypto.engines.ElGamalEngine
import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator
import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters
import org.bouncycastle.crypto.params.ElGamalParameters
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters
import java.math.BigInteger
import java.security.SecureRandom


object ElGamalLite {

    private val P = BigInteger((
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                    "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                    "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                    "E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF"
            ).lowercase(), 16)

    private val G = BigInteger.valueOf(2L)

    private val params = ElGamalParameters(P, G)
    private val rnd = SecureRandom()

    fun generateKeyPair(): AsymmetricCipherKeyPair {
        val gen = ElGamalKeyPairGenerator()
        gen.init(ElGamalKeyGenerationParameters(rnd, params))
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
