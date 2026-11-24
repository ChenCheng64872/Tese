package com.example.energy

/**
 * The C baseline (c-baseline/) through JNI: the same algorithms, parameters and timed loops as
 * crypto-c on the Pi and the Mac, run inside the app process (foreground, same measurement
 * pipeline as the Java cases). Names as in crypto-c: aes, aesgcm, chacha20, rsa, elgamal, ascon,
 * elephant, giftcofb, grain128, xoodyak, the key-size variants (aes128, rsa3072, ...) and each with
 * the "_raw" suffix (cipher only). Needs an APK built with ./gradlew -PnativeBench.
 */
class NativeCase(val algorithm: String, power: Int) : AutoCloseable {
    private var h: Long = nativeNew(algorithm, power)

    init {
        check(h != 0L) { "native setup failed for $algorithm (unknown name or round-trip mismatch)" }
    }

    /** {small_pairs, pairs, start_epoch_ms, end_epoch_ms} (main.c's warm-up). */
    fun warmUp(warmupMs: Long, smallPower: Int): WarmupResult {
        val r = checkNotNull(nativeWarmup(h, warmupMs, smallPower)) { "round trip mismatch in warm-up" }
        return WarmupResult(r[0].toInt(), r[1].toInt(), r[2], r[3])
    }

    /** One measured batch (main.c's loop); the round trip is checked after it. */
    fun batch(inner: Int, targetMs: Long): Batch {
        val r = checkNotNull(nativeBatch(h, inner, targetMs)) { "round trip mismatch after batch" }
        return Batch(r[0].toInt(), r[1], r[2], r[3], r[4])
    }

    /** implementation | mode | OpenSSL version | compiler | flags */
    fun info(): String = nativeInfo(h)

    override fun close() { nativeFree(h); h = 0 }

    companion object {
        init { System.loadLibrary("cryptobench") }
        @JvmStatic private external fun nativeNew(name: String, power: Int): Long
        @JvmStatic private external fun nativeWarmup(h: Long, warmupMs: Long, smallPower: Int): LongArray?
        @JvmStatic private external fun nativeBatch(h: Long, inner: Int, targetMs: Long): LongArray?
        @JvmStatic private external fun nativeInfo(h: Long): String
        @JvmStatic private external fun nativeFree(h: Long)
    }
}
