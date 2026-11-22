/* Standard Base64 (RFC 4648, '+/' alphabet, '=' padding), as Bouncy Castle's
 * org.bouncycastle.util.encoders.Base64 used by the Java harness. */
#ifndef CB_B64_H
#define CB_B64_H

#include <stddef.h>

/* Output length of encoding n bytes (without a terminating NUL). */
size_t cb_b64_encoded_len(size_t n);
/* Encode n bytes into out (needs cb_b64_encoded_len(n) + 1 bytes); returns the length. */
size_t cb_b64_encode(const unsigned char *in, size_t n, char *out);
/* Upper bound of the decoded size of len chars. */
size_t cb_b64_decoded_cap(size_t len);
/* Decode len chars into out (needs len / 4 * 3 bytes); returns the byte count or (size_t)-1. */
size_t cb_b64_decode(const char *in, size_t len, unsigned char *out);

#endif
