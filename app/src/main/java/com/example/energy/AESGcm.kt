package com.example.energy

import org.bouncycastle.util.encoders.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESGcm {
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LEN = 12          // 96-bit nonce (recommended)
    private const val TAG_BITS = 128       // 16-byte tag

    fun encrypt(plainUtf8: String, key: ByteArray, iv: ByteArray? = null): String {
        require(key.size == 16 || key.size == 24 || key.size == 32) { "AES key must be 16/24/32 bytes" }
        val ivBytes = iv ?: ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, ivBytes))
        val ct = cipher.doFinal(plainUtf8.toByteArray(Charsets.UTF_8))

        // Store as: IV || CIPHERTEXT+TAG  (and return Base64)
        val out = ByteBuffer.allocate(IV_LEN + ct.size).apply {
            put(ivBytes); put(ct)
        }.array()
        return Base64.toBase64String(out)
    }

    fun decrypt(ctB64: String, key: ByteArray): String {
        val all = org.bouncycastle.util.encoders.Base64.decode(ctB64)
        require(all.size > IV_LEN) { "cipher too small" }
        val iv = all.copyOfRange(0, IV_LEN)
        val body = all.copyOfRange(IV_LEN, all.size)

        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        val pt = cipher.doFinal(body)
        return String(pt, Charsets.UTF_8)
    }
}
