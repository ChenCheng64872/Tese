package com.example.energy.chacha20

import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.encoders.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

object ChaCha20Cipher {

    private const val KEY_SIZE_BYTES = 32   // 256-bit key
    private const val NONCE_SIZE_BYTES = 12 // IETF ChaCha20 uses 96-bit nonce

    fun generateKey(): ByteArray = ByteArray(KEY_SIZE_BYTES).apply {
        SecureRandom().nextBytes(this)
    }

    fun generateNonce(): ByteArray = ByteArray(NONCE_SIZE_BYTES).apply {
        SecureRandom().nextBytes(this)
    }

    fun encrypt(plainText: String, key: ByteArray, nonce: ByteArray): String {
        require(key.size == KEY_SIZE_BYTES) { "Key must be 32 bytes (256-bit)" }
        require(nonce.size == NONCE_SIZE_BYTES) { "Nonce must be 12 bytes (96-bit)" }

        val engine = ChaCha7539Engine() // ChaCha20 with 96-bit nonce (IETF)
        engine.init(true, ParametersWithIV(KeyParameter(key), nonce))

        val input = plainText.toByteArray(StandardCharsets.UTF_8)
        val out = ByteArray(input.size)
        engine.processBytes(input, 0, input.size, out, 0)
        return Base64.toBase64String(out)
    }

    fun decrypt(cipherTextB64: String, key: ByteArray, nonce: ByteArray): String {
        require(key.size == KEY_SIZE_BYTES) { "Key must be 32 bytes (256-bit)" }
        require(nonce.size == NONCE_SIZE_BYTES) { "Nonce must be 12 bytes (96-bit)" }

        val engine = ChaCha7539Engine()
        engine.init(false, ParametersWithIV(KeyParameter(key), nonce))

        val input = Base64.decode(cipherTextB64)
        val out = ByteArray(input.size)
        engine.processBytes(input, 0, input.size, out, 0)
        return String(out, StandardCharsets.UTF_8)
    }
}
