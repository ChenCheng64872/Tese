package com.example.energy.lwc

import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.modes.AEADCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.util.encoders.Base64
import java.nio.ByteBuffer

data class AeadSpec(
    val name: String,
    val keyBytes: Int,
    val nonceBytes: Int,
    val tagBits: Int = 128
)

object BcLwcAead {

    /**
     * Output format (Base64): NONCE || (CIPHERTEXT || TAG)
     */
    fun encryptUtf8(
        engine: AEADCipher,
        spec: AeadSpec,
        plainUtf8: String,
        key: ByteArray,
        nonce: ByteArray,
        aadUtf8: String? = null
    ): String {
        require(key.size == spec.keyBytes) { "${spec.name} key must be ${spec.keyBytes} bytes" }
        require(nonce.size == spec.nonceBytes) { "${spec.name} nonce must be ${spec.nonceBytes} bytes" }

        val pt = plainUtf8.toByteArray(Charsets.UTF_8)
        val aad = aadUtf8?.toByteArray(Charsets.UTF_8)

        engine.init(true, AEADParameters(KeyParameter(key), spec.tagBits, nonce, aad))

        val out = ByteArray(engine.getOutputSize(pt.size))
        var off = engine.processBytes(pt, 0, pt.size, out, 0)
        off += engine.doFinal(out, off)

        val packed = ByteBuffer.allocate(spec.nonceBytes + off).apply {
            put(nonce)
            put(out, 0, off)
        }.array()

        return Base64.toBase64String(packed)
    }

    fun decryptUtf8(
        engine: AEADCipher,
        spec: AeadSpec,
        ctB64: String,
        key: ByteArray,
        aadUtf8: String? = null
    ): String {
        require(key.size == spec.keyBytes) { "${spec.name} key must be ${spec.keyBytes} bytes" }

        val all = Base64.decode(ctB64)
        require(all.size > spec.nonceBytes) { "cipher too small" }

        val nonce = all.copyOfRange(0, spec.nonceBytes)
        val body = all.copyOfRange(spec.nonceBytes, all.size)
        val aad = aadUtf8?.toByteArray(Charsets.UTF_8)

        engine.init(false, AEADParameters(KeyParameter(key), spec.tagBits, nonce, aad))

        val out = ByteArray(engine.getOutputSize(body.size))
        return try {
            var off = engine.processBytes(body, 0, body.size, out, 0)
            off += engine.doFinal(out, off)
            String(out, 0, off, Charsets.UTF_8)
        } catch (e: InvalidCipherTextException) {
            throw IllegalStateException("${spec.name} tag verification failed", e)
        }
    }
}