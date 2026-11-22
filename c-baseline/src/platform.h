/* Timing, randomness and small helpers shared by the C baseline harness. */
#ifndef CB_PLATFORM_H
#define CB_PLATFORM_H

#include <stddef.h>
#include <stdint.h>

/* Monotonic clock in nanoseconds (QueryPerformanceCounter / CLOCK_MONOTONIC). */
uint64_t cb_now_ns(void);
/* Wall-clock time in milliseconds since the Unix epoch (batch windows for energy). */
int64_t cb_epoch_ms(void);
/* Cryptographically secure random bytes (OpenSSL RAND_bytes); aborts on failure. */
void cb_random(unsigned char *buf, size_t len);
/* malloc that aborts on failure. */
void *cb_xmalloc(size_t len);
/* Print an error message and exit(1). */
void cb_die(const char *fmt, ...);

#endif
