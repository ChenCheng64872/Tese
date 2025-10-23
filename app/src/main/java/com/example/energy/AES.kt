package com.example.energy

import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.encoders.Base64
import java.nio.charset.StandardCharsets
import java.security.Security

object AES {
    const val KEY_SIZE_128_BITS = 128
    const val KEY_SIZE_192_BITS = 192
    const val KEY_SIZE_256_BITS = 256

    val IV_BLANK = ByteArray(16) { 0x00 }

    const val KEY_GENERATION_ITERATIONS = 1000

    init {
        // Add provider if not already present
        if (Security.getProvider("BC") == null) {
            Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        }
    }

    fun createKey(password: String, salt: ByteArray, iterations: Int, keySizeInBits: Int): KeyParameter {
        val generator = PKCS5S2ParametersGenerator(SHA256Digest())
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toCharArray()), salt, iterations)
        return generator.generateDerivedMacParameters(keySizeInBits) as KeyParameter
    }

    fun encrypt(plain: String, key: ByteArray, iv: ByteArray): String {
        val cipher = createCipher(key, iv, true)
        val cipherBytes = cipherData(cipher, plain.toByteArray(StandardCharsets.UTF_8))
        return Base64.toBase64String(cipherBytes)
    }

    fun decrypt(cipherB64: String, key: ByteArray, iv: ByteArray): String {
        val cipher = createCipher(key, iv, false)
        val plainBytes = cipherData(cipher, Base64.decode(cipherB64))
        return String(plainBytes, StandardCharsets.UTF_8)
    }

    private fun cipherData(cipher: PaddedBufferedBlockCipher, data: ByteArray): ByteArray {
        val outBuf = ByteArray(cipher.getOutputSize(data.size))
        val len1 = cipher.processBytes(data, 0, data.size, outBuf, 0)
        val len2 = cipher.doFinal(outBuf, len1)
        return outBuf.copyOf(len1 + len2)
    }

    private fun createCipher(key: ByteArray, iv: ByteArray, forEncryption: Boolean): PaddedBufferedBlockCipher {
        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()))
        cipher.init(forEncryption, ParametersWithIV(KeyParameter(key), iv))
        return cipher
    }
}
