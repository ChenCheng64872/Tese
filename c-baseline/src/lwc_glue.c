/* Glue to the third-party lightweight AEAD code. The NIST-API implementations are compiled
 * with crypto_aead_encrypt/decrypt renamed per family (see the Makefile) so they can be
 * linked into one binary. Parameters match the Java harness (LwcSpecs.kt + DesktopMain.kt). */
#include "lwc_glue.h"

#include <string.h>

/* NIST LWC API, renamed per family */
#define NIST_AEAD(prefix)                                                                     \
    int prefix##_crypto_aead_encrypt(unsigned char *c, unsigned long long *clen,              \
                                     const unsigned char *m, unsigned long long mlen,         \
                                     const unsigned char *ad, unsigned long long adlen,       \
                                     const unsigned char *nsec, const unsigned char *npub,    \
                                     const unsigned char *k);                                 \
    int prefix##_crypto_aead_decrypt(unsigned char *m, unsigned long long *mlen,              \
                                     unsigned char *nsec, const unsigned char *c,             \
                                     unsigned long long clen, const unsigned char *ad,        \
                                     unsigned long long adlen, const unsigned char *npub,     \
                                     const unsigned char *k);                                 \
    static int prefix##_enc(unsigned char *c, size_t *clen, const unsigned char *m,           \
                            size_t mlen, const unsigned char *npub, const unsigned char *k) { \
        unsigned long long l = 0;                                                             \
        int r = prefix##_crypto_aead_encrypt(c, &l, m, mlen, NULL, 0, NULL, npub, k);         \
        *clen = (size_t)l;                                                                    \
        return r;                                                                             \
    }                                                                                         \
    static int prefix##_dec(unsigned char *m, size_t *mlen, const unsigned char *c,           \
                            size_t clen, const unsigned char *npub, const unsigned char *k) { \
        unsigned long long l = 0;                                                             \
        int r = prefix##_crypto_aead_decrypt(m, &l, NULL, c, clen, NULL, 0, npub, k);         \
        *mlen = (size_t)l;                                                                    \
        return r;                                                                             \
    }

NIST_AEAD(ascon)
NIST_AEAD(gift)
NIST_AEAD(xoodyak)

/* rweather/lwc-finalists ("combined" API: size_t lengths, no nsec) */
int delirium_aead_encrypt(unsigned char *c, size_t *clen, const unsigned char *m, size_t mlen,
                          const unsigned char *ad, size_t adlen, const unsigned char *npub,
                          const unsigned char *k);
int delirium_aead_decrypt(unsigned char *m, size_t *mlen, const unsigned char *c, size_t clen,
                          const unsigned char *ad, size_t adlen, const unsigned char *npub,
                          const unsigned char *k);
int grain128_aead_encrypt(unsigned char *c, size_t *clen, const unsigned char *m, size_t mlen,
                          const unsigned char *ad, size_t adlen, const unsigned char *npub,
                          const unsigned char *k);
int grain128_aead_decrypt(unsigned char *m, size_t *mlen, const unsigned char *c, size_t clen,
                          const unsigned char *ad, size_t adlen, const unsigned char *npub,
                          const unsigned char *k);

static int elephant_enc(unsigned char *c, size_t *clen, const unsigned char *m, size_t mlen,
                        const unsigned char *npub, const unsigned char *k) {
    return delirium_aead_encrypt(c, clen, m, mlen, NULL, 0, npub, k);
}
static int elephant_dec(unsigned char *m, size_t *mlen, const unsigned char *c, size_t clen,
                        const unsigned char *npub, const unsigned char *k) {
    return delirium_aead_decrypt(m, mlen, c, clen, NULL, 0, npub, k);
}
static int grain_enc(unsigned char *c, size_t *clen, const unsigned char *m, size_t mlen,
                     const unsigned char *npub, const unsigned char *k) {
    return grain128_aead_encrypt(c, clen, m, mlen, NULL, 0, npub, k);
}
static int grain_dec(unsigned char *m, size_t *mlen, const unsigned char *c, size_t clen,
                     const unsigned char *npub, const unsigned char *k) {
    return grain128_aead_decrypt(m, mlen, c, clen, NULL, 0, npub, k);
}

#ifndef CB_ASCON_VARIANT
#define CB_ASCON_VARIANT "opt64"
#endif

static const cb_lwc_impl IMPLS[] = {
    {"ascon", 16, 16, 16, ascon_enc, ascon_dec,
     "Ascon-AEAD128 (NIST SP 800-232), ascon/ascon-c crypto_aead/asconaead128/" CB_ASCON_VARIANT
     " (designers, CC0)"},
    {"elephant", 16, 12, 16, elephant_enc, elephant_dec,
     "Elephant v2 elephant200 (Delirium, Keccak-f[200]), rweather/lwc-finalists src/combined "
     "(third-party optimized portable C, MIT)"},
    {"giftcofb", 16, 16, 16, gift_enc, gift_dec,
     "GIFT-COFB v1.1, aadomn/gift crypto_aead/giftcofb128v1/opt32 (fixsliced, Adomnicai & Peyrin, CC0)"},
    {"grain128", 16, 12, 8, grain_enc, grain_dec,
     "Grain-128AEADv2, rweather/lwc-finalists src/combined (third-party optimized portable C, MIT)"},
    {"xoodyak", 16, 16, 16, xoodyak_enc, xoodyak_dec,
     "Xoodyak (round 3), XKCP lib/high/Xoodyak + lib/low/Xoodoo/plain (designers)"},
};

const cb_lwc_impl *cb_lwc_find(const char *name) {
    for (size_t i = 0; i < sizeof IMPLS / sizeof IMPLS[0]; i++)
        if (strcmp(IMPLS[i].name, name) == 0) return &IMPLS[i];
    return NULL;
}
