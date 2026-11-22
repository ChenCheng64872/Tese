/* ElGamal encryption with PKCS#1 v1.5 (type 2) padding, matching Bouncy Castle's
 * PKCS1Encoding(ElGamalEngine()) as used by ElGamalLite in the Java harness.
 * Standard MODP groups, g = 2, primes from OpenSSL's built-in tables:
 *   768, 1024 bits: RFC 2409 groups 1, 2      2048, 3072, 4096 bits: RFC 3526 groups 14-16
 * Ciphertext = gamma || phi, each ceil(bits/8) bytes big-endian. */
#ifndef CB_ELGAMAL_H
#define CB_ELGAMAL_H

#include <stddef.h>

typedef struct cb_elgamal cb_elgamal;

#define CB_ELGAMAL_DEFAULT_BITS 2048
#define CB_ELGAMAL_MAX_CT_BYTES 1024 /* 4096-bit group */

/* 1 if bits is a supported group size. */
int cb_elgamal_supported(int bits);
/* New key pair: x uniform in [2, p-2], y = g^x mod p (as BC DHKeyGeneratorHelper). */
cb_elgamal *cb_elgamal_new(int bits);
/* Key pair from a given private key x (hex), for cross-checks with Bouncy Castle. */
cb_elgamal *cb_elgamal_from_x_hex(int bits, const char *x_hex);
void cb_elgamal_free(cb_elgamal *k);
size_t cb_elgamal_ct_bytes(const cb_elgamal *k);
/* Largest message: (bits - 1) / 8 - 10 bytes of PKCS#1 overhead. */
size_t cb_elgamal_max_data(const cb_elgamal *k);
/* Encrypt len bytes into out[cb_elgamal_ct_bytes(k)]. */
void cb_elgamal_encrypt(cb_elgamal *k, const unsigned char *in, size_t len, unsigned char *out);
/* Decrypt a cb_elgamal_ct_bytes(k)-byte ciphertext; returns the data length or -1. */
int cb_elgamal_decrypt(cb_elgamal *k, const unsigned char *in, unsigned char *out, size_t out_cap);
/* Hex of p (for checks), caller frees with OPENSSL_free. */
char *cb_elgamal_p_hex(const cb_elgamal *k);

#endif
