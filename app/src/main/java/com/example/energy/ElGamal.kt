package com.example.energy.elgamal

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Base64
import java.security.*
import javax.crypto.Cipher

object ElGamalCipher {
    private const val ALGORITHM = "ElGamal/None/NoPadding"

    init {
        if (Security.getProvider("BC") == null)
            Security.addProvider(BouncyCastleProvider())
    }

    fun generateKeyPair(keySize: Int = 2048): KeyPair {
        val generator = KeyPairGenerator.getInstance("ElGamal", "BC")
        generator.initialize(keySize, SecureRandom())
        return generator.generateKeyPair()
    }

    fun encrypt(plainText: ByteArray, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance(ALGORITHM, "BC")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val cipherBytes = cipher.doFinal(plainText)
        return Base64.toBase64String(cipherBytes)
    }

    fun decrypt(cipherB64: String, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM, "BC")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(Base64.decode(cipherB64))
    }
}
