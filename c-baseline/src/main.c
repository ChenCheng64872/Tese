/* C baseline harness: the C counterpart of desktop/src/main/kotlin/DesktopMain.kt.
 *
 * Same command line, same protocol, same CSV and manifest, so scripts/pi/run-pi-energy-5.sh
 * and pi_integrate_energy.py work unchanged:
 *   crypto-c <algorithm> <power> <rounds> <inner> <output.csv> [target_batch_ms] [warmup_ms] [warmup_small_power]
 *   crypto-c list
 *   crypto-c selftest <vectors.txt> [c2java.txt]     (cross-check against Bouncy Castle)
 * Algorithms: aes aesgcm chacha20 rsa elgamal ascon elephant giftcofb grain128 xoodyak,
 * each also as <name>_raw (cipher only, no String/Base64 steps). */
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <openssl/crypto.h>

#include "algos.h"
#include "platform.h"

int cb_selftest(const char *vectors, const char *c2java);

#ifndef CB_CFLAGS
#define CB_CFLAGS "unknown"
#endif
#if defined(_WIN32)
#define CB_OS "Windows"
#elif defined(__linux__)
#define CB_OS "Linux"
#elif defined(__APPLE__)
#define CB_OS "macOS"
#else
#define CB_OS "unknown"
#endif
#if defined(__aarch64__) || defined(_M_ARM64)
#define CB_ARCH "aarch64"
#elif defined(__arm__)
#define CB_ARCH "arm"
#elif defined(__x86_64__) || defined(_M_X64)
#define CB_ARCH "x86_64"
#else
#define CB_ARCH "unknown"
#endif

static long parse_long(const char *s, const char *what, long lo, long hi) {
    char *end;
    errno = 0;
    long v = strtol(s, &end, 10);
    if (errno || *end || v < lo || v > hi) cb_die("%s must be an integer in %ld..%ld (got '%s')", what, lo, hi, s);
    return v;
}

static void json_escape(FILE *f, const char *s) {
    for (; *s; s++) {
        if (*s == '"' || *s == '\\') fputc('\\', f);
        fputc(*s, f);
    }
}

static void round_trip_check(cb_algo *a, int n) {
    for (int i = 0; i < n; i++) {
        cb_algo_encrypt(a);
        cb_algo_decrypt(a);
        if (!cb_algo_check(a)) cb_die("round trip mismatch");
    }
}

static unsigned char *make_plain(size_t n) {
    unsigned char *p = cb_xmalloc(n);
    memset(p, 'A', n); /* "A".repeat(1 shl power), as DesktopMain */
    return p;
}

static int run(int argc, char **argv) {
    const char *algorithm = argv[1];
    int power = (int)parse_long(argv[2], "power", 10, 20);
    int rounds = (int)parse_long(argv[3], "rounds", 1, 100);
    long inner = parse_long(argv[4], "inner", 1, 100000);
    const char *outpath = argv[5];
    long target_ms = argc > 6 ? parse_long(argv[6], "target_batch_ms", 0, 600000) : 0;
    long warmup_ms = argc > 7 ? parse_long(argv[7], "warmup_ms", 0, 600000) : 0;
    int small_power = argc > 8 ? (int)parse_long(argv[8], "warmup_small_power", 0, 20) : 0;
    if (small_power != 0 && small_power < 6) cb_die("warmup_small_power must be 0 or 6..20");

    FILE *probe = fopen(outpath, "r");
    if (probe) { fclose(probe); cb_die("Output exists; choose a new filename"); }

    size_t size = (size_t)1 << power;
    unsigned char *plain = make_plain(size);

    /* warm-up: optional small-input phase, then the measured size (see DesktopMain) */
    int64_t warm_start_epoch = cb_epoch_ms();
    long small_pairs = 0, warm_pairs = 0;
    if (small_power > 0) {
        size_t ssize = (size_t)1 << small_power;
        unsigned char *splain = make_plain(ssize);
        cb_algo *s = cb_algo_new(algorithm, splain, ssize);
        if (!s) cb_die("Unknown algorithm '%s'", algorithm);
        round_trip_check(s, 3);
        uint64_t t0 = cb_now_ns();
        while (cb_now_ns() - t0 < (uint64_t)warmup_ms * 1000000ull) {
            cb_algo_encrypt(s); cb_algo_decrypt(s); small_pairs++;
        }
        cb_algo_free(s);
        free(splain);
    }
    cb_algo *a = cb_algo_new(algorithm, plain, size);
    if (!a) cb_die("Unknown algorithm '%s'", algorithm);
    round_trip_check(a, 3);
    uint64_t w0 = cb_now_ns();
    while (cb_now_ns() - w0 < (uint64_t)warmup_ms * 1000000ull) {
        cb_algo_encrypt(a); cb_algo_decrypt(a); warm_pairs++;
    }
    int64_t warm_end_epoch = cb_epoch_ms();
    cb_algo_decrypt(a);
    if (!cb_algo_check(a)) cb_die("round trip mismatch after warm-up");

    FILE *out = fopen(outpath, "w");
    if (!out) cb_die("cannot write %s", outpath);
    fputs("algorithm,size_bytes,round,inner_iterations,batch_start_epoch_ms,batch_end_epoch_ms,"
          "enc_total_ns,dec_total_ns,enc_mean_ns,dec_mean_ns,energy_j,energy_status\n", out);

    char mpath[4096];
    snprintf(mpath, sizeof mpath, "%s.json", outpath);
    FILE *m = fopen(mpath, "w");
    if (!m) cb_die("cannot write %s", mpath);
    fprintf(m, "{\"language\":\"c\",\"os\":\"%s\",\"arch\":\"%s\",\"compiler\":\"", CB_OS, CB_ARCH);
    json_escape(m, __VERSION__);
    fputs("\",\"cflags\":\"", m);
    json_escape(m, CB_CFLAGS);
    fputs("\",\"openssl_version\":\"", m);
    json_escape(m, OpenSSL_version(OPENSSL_VERSION));
    fputs("\",\"implementation\":\"", m);
    json_escape(m, cb_algo_description(a));
    fprintf(m, "\",\"mode\":\"%s\",\"warmup_rounds\":3,\"warmup_ms\":%ld,\"warmup_pairs\":%ld,"
               "\"warmup_small_power\":%d,\"warmup_small_pairs\":%ld,\"warmup_start_epoch_ms\":%lld,"
               "\"warmup_end_epoch_ms\":%lld,\"energy_source\":\"external\"}",
            cb_algo_is_raw(a) ? "raw" : "mirror", warmup_ms, warm_pairs, small_power, small_pairs,
            (long long)warm_start_epoch, (long long)warm_end_epoch);
    fclose(m);

    for (int r = 0; r < rounds; r++) {
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
        if (!cb_algo_check(a)) cb_die("round trip mismatch in batch %d", r + 1);
        fprintf(out, "%s,%zu,%d,%ld,%lld,%lld,%llu,%llu,%.3f,%.3f,,NOT_MEASURED\n", algorithm, size, r + 1,
                iterations, (long long)epoch_start, (long long)epoch_end, (unsigned long long)enc,
                (unsigned long long)dec, (double)enc / iterations, (double)dec / iterations);
        fflush(out);
    }
    fclose(out);
    printf("CSV: %s\n", outpath);
    cb_algo_free(a);
    free(plain);
    return 0;
}

int main(int argc, char **argv) {
    if (argc >= 2 && strcmp(argv[1], "list") == 0) {
        for (const cb_algo_info *i = cb_algo_list(); i->name; i++) printf("%s\n%s_raw\n", i->name, i->name);
        return 0;
    }
    if (argc >= 3 && strcmp(argv[1], "selftest") == 0)
        return cb_selftest(argv[2], argc > 3 ? argv[3] : NULL);
    if (argc < 6 || argc > 9) {
        fprintf(stderr,
                "Usage: %s algorithm power rounds inner output.csv [target_batch_ms] [warmup_ms] [warmup_small_power]\n"
                "       %s list\n       %s selftest vectors.txt [c2java.txt]\n",
                argv[0], argv[0], argv[0]);
        return 2;
    }
    return run(argc, argv);
}
