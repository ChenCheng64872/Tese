/* JNI entry points of the C baseline inside the app (com.example.energy.NativeCase).
 * The warm-up and the measured batch are the loops of c-baseline/src/main.c, unchanged, so the timed
 * code is the same as in crypto-c on the Pi and the Mac. The instrumented test keeps the protocol
 * around them (idle window, batch windows on elapsedRealtime, CSV), as for the Java cases. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <openssl/crypto.h>

#include "algos.h"
#include "platform.h"

#define CB_CFLAGS "-O2 (NDK clang, aarch64 android, JNI via app/src/main/cpp/CMakeLists.txt)"

typedef struct {
    char name[64];
    unsigned char *plain;
    size_t size;
    cb_algo *a;
} handle;

static unsigned char *make_plain(size_t n) {
    unsigned char *p = cb_xmalloc(n);
    memset(p, 'A', n); /* "A".repeat(1 shl power), as DesktopMain */
    return p;
}

static int round_trip_check(cb_algo *a, int n) {
    for (int i = 0; i < n; i++) {
        cb_algo_encrypt(a);
        cb_algo_decrypt(a);
        if (!cb_algo_check(a)) return 0;
    }
    return 1;
}

static jlongArray longs(JNIEnv *env, const jlong *v, int n) {
    jlongArray r = (*env)->NewLongArray(env, n);
    (*env)->SetLongArrayRegion(env, r, 0, n, v);
    return r;
}

JNIEXPORT jlong JNICALL Java_com_example_energy_NativeCase_nativeNew(JNIEnv *env, jclass cls, jstring jname, jint power) {
    const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
    handle *h = calloc(1, sizeof *h);
    strncpy(h->name, name, sizeof h->name - 1);
    (*env)->ReleaseStringUTFChars(env, jname, name);
    h->size = (size_t)1 << power;
    h->plain = make_plain(h->size);
    h->a = cb_algo_new(h->name, h->plain, h->size);
    if (!h->a || !round_trip_check(h->a, 3)) {
        if (h->a) cb_algo_free(h->a);
        free(h->plain); free(h);
        return 0;
    }
    return (jlong)(intptr_t)h;
}

/* main.c warm-up: optional small-input phase, then the measured size.
 * Returns {small_pairs, pairs, start_epoch_ms, end_epoch_ms}, or null on a round-trip mismatch. */
JNIEXPORT jlongArray JNICALL Java_com_example_energy_NativeCase_nativeWarmup(JNIEnv *env, jclass cls, jlong jh, jlong warmup_ms, jint small_power) {
    handle *h = (handle *)(intptr_t)jh;
    int64_t warm_start_epoch = cb_epoch_ms();
    long small_pairs = 0, warm_pairs = 0;
    if (small_power > 0) {
        size_t ssize = (size_t)1 << small_power;
        unsigned char *splain = make_plain(ssize);
        cb_algo *s = cb_algo_new(h->name, splain, ssize);
        if (!s || !round_trip_check(s, 3)) return NULL;
        uint64_t t0 = cb_now_ns();
        while (cb_now_ns() - t0 < (uint64_t)warmup_ms * 1000000ull) {
            cb_algo_encrypt(s); cb_algo_decrypt(s); small_pairs++;
        }
        cb_algo_free(s);
        free(splain);
    }
    if (!round_trip_check(h->a, 3)) return NULL;
    uint64_t w0 = cb_now_ns();
    while (cb_now_ns() - w0 < (uint64_t)warmup_ms * 1000000ull) {
        cb_algo_encrypt(h->a); cb_algo_decrypt(h->a); warm_pairs++;
    }
    int64_t warm_end_epoch = cb_epoch_ms();
    cb_algo_decrypt(h->a);
    if (!cb_algo_check(h->a)) return NULL;
    jlong v[4] = {small_pairs, warm_pairs, warm_start_epoch, warm_end_epoch};
    return longs(env, v, 4);
}

/* One measured batch (main.c's loop). Returns {pairs, enc_total_ns, dec_total_ns, start_epoch_ms,
 * end_epoch_ms}, or null if the decryption after the batch does not reproduce the plaintext. */
JNIEXPORT jlongArray JNICALL Java_com_example_energy_NativeCase_nativeBatch(JNIEnv *env, jclass cls, jlong jh, jint inner, jlong target_ms) {
    handle *h = (handle *)(intptr_t)jh;
    cb_algo *a = h->a;
    uint64_t enc = 0, dec = 0;
    long iterations = 0;
    int64_t epoch_start = cb_epoch_ms();
    uint64_t start = cb_now_ns();
    do {
        uint64_t t = cb_now_ns();
        for (long i = 0; i < inner; i++) cb_algo_encrypt(a);
        enc += cb_now_ns() - t;
        t = cb_now_ns();
        for (long i = 0; i < inner; i++) cb_algo_decrypt(a);
        dec += cb_now_ns() - t;
        iterations += inner;
    } while (cb_now_ns() - start < (uint64_t)target_ms * 1000000ull);
    int64_t epoch_end = cb_epoch_ms();
    cb_algo_decrypt(a);
    if (!cb_algo_check(a)) return NULL;
    jlong v[5] = {iterations, (jlong)enc, (jlong)dec, epoch_start, epoch_end};
    return longs(env, v, 5);
}

/* Description for the manifest: implementation | mode | OpenSSL version | compiler | flags. */
JNIEXPORT jstring JNICALL Java_com_example_energy_NativeCase_nativeInfo(JNIEnv *env, jclass cls, jlong jh) {
    handle *h = (handle *)(intptr_t)jh;
    char buf[1024];
    snprintf(buf, sizeof buf, "%s|%s|%s|%s|%s", cb_algo_description(h->a), cb_algo_is_raw(h->a) ? "raw" : "mirror",
             OpenSSL_version(OPENSSL_VERSION), __VERSION__, CB_CFLAGS);
    return (*env)->NewStringUTF(env, buf);
}

JNIEXPORT void JNICALL Java_com_example_energy_NativeCase_nativeFree(JNIEnv *env, jclass cls, jlong jh) {
    handle *h = (handle *)(intptr_t)jh;
    if (!h) return;
    cb_algo_free(h->a);
    free(h->plain);
    free(h);
}
