#include "platform.h"

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#include <openssl/err.h>
#include <openssl/rand.h>

#ifdef _WIN32
#include <windows.h>

uint64_t cb_now_ns(void) {
    static LARGE_INTEGER freq;
    LARGE_INTEGER now;
    if (freq.QuadPart == 0) QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&now);
    /* split to avoid overflow: seconds part + remainder */
    uint64_t sec = (uint64_t)(now.QuadPart / freq.QuadPart);
    uint64_t rem = (uint64_t)(now.QuadPart % freq.QuadPart);
    return sec * 1000000000ull + rem * 1000000000ull / (uint64_t)freq.QuadPart;
}

int64_t cb_epoch_ms(void) {
    FILETIME ft;
    GetSystemTimePreciseAsFileTime(&ft);
    uint64_t t = ((uint64_t)ft.dwHighDateTime << 32) | ft.dwLowDateTime; /* 100 ns since 1601 */
    return (int64_t)((t - 116444736000000000ull) / 10000ull);
}
#else
#include <time.h>

uint64_t cb_now_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000000000ull + (uint64_t)ts.tv_nsec;
}

int64_t cb_epoch_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}
#endif

void cb_die(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    fputs("error: ", stderr);
    vfprintf(stderr, fmt, ap);
    fputc('\n', stderr);
    va_end(ap);
    ERR_print_errors_fp(stderr);
    exit(1);
}

void cb_random(unsigned char *buf, size_t len) {
    if (len && RAND_bytes(buf, (int)len) != 1) cb_die("RAND_bytes failed");
}

void *cb_xmalloc(size_t len) {
    void *p = malloc(len ? len : 1);
    if (!p) cb_die("out of memory (%zu bytes)", len);
    return p;
}
