/* Uniform interface to the lightweight AEAD implementations (no associated data, as in
 * BcLwcAead with aad = null). Output c = ciphertext || tag. Return 0 on success. */
#ifndef CB_LWC_GLUE_H
#define CB_LWC_GLUE_H

#include <stddef.h>

typedef int (*cb_lwc_enc_fn)(unsigned char *c, size_t *clen, const unsigned char *m, size_t mlen,
                             const unsigned char *npub, const unsigned char *k);
typedef int (*cb_lwc_dec_fn)(unsigned char *m, size_t *mlen, const unsigned char *c, size_t clen,
                             const unsigned char *npub, const unsigned char *k);

typedef struct {
    const char *name;
    size_t key_bytes, nonce_bytes, tag_bytes;
    cb_lwc_enc_fn enc;
    cb_lwc_dec_fn dec;
    const char *impl; /* provenance, written to the manifest */
} cb_lwc_impl;

/* name: ascon | elephant | giftcofb | grain128 | xoodyak; NULL if unknown. */
const cb_lwc_impl *cb_lwc_find(const char *name);

#endif
