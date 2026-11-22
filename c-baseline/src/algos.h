/* Algorithm adapters of the C baseline. Every algorithm mirrors the Java harness
 * (desktop/src/main/kotlin/DesktopMain.kt) in parameters and, in "mirror" mode, in the
 * data transformations inside each timed call:
 *   encrypt: copy the plaintext (String -> UTF-8 bytes), encrypt, build the envelope,
 *            Base64-encode it
 *   decrypt: Base64-decode, split the envelope, decrypt, copy the plaintext out (bytes -> String)
 * "raw" mode (algorithm name suffix "_raw") times only the cipher on byte buffers; per-call
 * IV/nonce/key generation stays in both modes, because the Java harness does it too.
 * Buffers are allocated once per setup (efficient C), not per call as in Java. */
#ifndef CB_ALGOS_H
#define CB_ALGOS_H

#include <stddef.h>

typedef struct cb_algo cb_algo;

typedef struct {
    const char *name;        /* CLI name, e.g. "ascon" */
    const char *description; /* parameters and implementation, written to the manifest */
} cb_algo_info;

/* NULL-terminated table of supported base algorithm names. */
const cb_algo_info *cb_algo_list(void);

/* Set up an algorithm for a plaintext. name may carry the "_raw" suffix. Returns NULL for an
 * unknown name. The plaintext must stay valid for the lifetime of the object. */
cb_algo *cb_algo_new(const char *name, const unsigned char *plain, size_t len);
void cb_algo_free(cb_algo *a);
void cb_algo_encrypt(cb_algo *a);
void cb_algo_decrypt(cb_algo *a);
/* 1 if the last decryption reproduced the plaintext. */
int cb_algo_check(const cb_algo *a);
const char *cb_algo_description(const cb_algo *a);
int cb_algo_is_raw(const cb_algo *a);

/* ---- deterministic hooks for cross-checking against Bouncy Castle (selftest) ---- */
/* Fix key, IV/nonce for the next encryptions (only for aes, aesgcm, chacha20 and the LWC
 * ciphers; sizes as the algorithm expects). */
void cb_algo_fix(cb_algo *a, const unsigned char *key, size_t keylen,
                 const unsigned char *iv, size_t ivlen);
/* Mirror-mode Base64 output of the last encryption. */
const char *cb_algo_last_b64(const cb_algo *a);
/* Decrypt a given Base64 envelope (mirror mode); 1 if it equals the plaintext. */
int cb_algo_decrypt_b64(cb_algo *a, const char *b64);

#endif
