#include "algos.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <openssl/evp.h>
#include <openssl/rsa.h>

#include "b64.h"
#include "elgamal.h"
#include "lwc_glue.h"
#include "platform.h"

typedef enum { K_AES, K_AESGCM, K_CHACHA, K_RSA, K_ELGAMAL, K_LWC } kind_t;

/* Names may carry a key size: aes128/192/256, aesgcm128/192/256, rsa1024/2048/3072/4096,
 * elgamal768/1024/2048/3072/4096. Defaults: AES-256, RSA-2048, ElGamal-2048 (as DesktopMain). */
static const cb_algo_info INFOS[] = {
    {"aes", "AES-%d-CBC, PKCS#7 padding, zero IV (as DesktopMain 'aes'); OpenSSL EVP"},
    {"aesgcm", "AES-%d-GCM, random 96-bit IV per encryption, 128-bit tag; envelope IV||CT||TAG; OpenSSL EVP"},
    {"chacha20", "ChaCha20 (RFC 7539, 96-bit nonce fixed per run, counter 0, no MAC) as BC ChaCha7539Engine; OpenSSL EVP"},
    {"rsa", "hybrid: AES-256-CBC (random key+IV per call) + RSA-%d OAEP SHA-256/MGF1-SHA-256 wrap of key||IV; OpenSSL"},
    {"elgamal", "hybrid: AES-256-CBC (random key+IV per call) + ElGamal %d-bit (RFC 2409/3526 MODP group, g=2) PKCS#1 v1.5 wrap; OpenSSL BIGNUM"},
    {"ascon", NULL}, {"elephant", NULL}, {"giftcofb", NULL}, {"grain128", NULL}, {"xoodyak", NULL},
    {NULL, NULL},
};

const cb_algo_info *cb_algo_list(void) { return INFOS; }

struct cb_algo {
    kind_t kind;
    const cb_lwc_impl *lwc;
    int raw, fixed;
    int bits;      /* AES key bits, RSA modulus bits or ElGamal group bits (0 otherwise) */
    size_t keylen; /* symmetric key length in bytes */
    const unsigned char *plain;
    size_t len;
    unsigned char key[32], iv[16];
    char desc[400];

    /* buffers, allocated once */
    size_t cap;              /* plaintext + envelope overhead */
    unsigned char *pt_copy;  /* mirror: String -> bytes */
    unsigned char *env;      /* envelope (IV || CT || TAG, or CT) */
    size_t env_len;
    char *b64;               /* mirror: Base64 of the envelope */
    size_t b64_len;
    unsigned char *denv;     /* mirror: decoded envelope */
    unsigned char *out;      /* decrypted plaintext */
    size_t out_len;
    unsigned char *out_copy; /* mirror: bytes -> String */

    EVP_CIPHER *cbc;     /* AES-256-CBC: bulk cipher of the RSA/ElGamal hybrids */
    EVP_CIPHER *sym_cbc; /* AES-<bits>-CBC for "aes" */
    EVP_CIPHER *gcm;     /* AES-<bits>-GCM for "aesgcm" */
    EVP_CIPHER *chacha;
    EVP_CIPHER_CTX *cctx;
    EVP_PKEY *rsa;
    EVP_PKEY_CTX *rsa_enc, *rsa_dec;
    cb_elgamal *eg;
    unsigned char hk[48];      /* hybrid: AES key (32) || IV (16) of the current call */
    unsigned char wrapped[CB_ELGAMAL_MAX_CT_BYTES]; /* >= 512 bytes for RSA-4096 too */
    size_t wrapped_len;
};

/* ------------------------------------------------------------------ OpenSSL helpers */
static size_t cipher_run(EVP_CIPHER_CTX *ctx, const EVP_CIPHER *c, int enc, const unsigned char *key,
                         const unsigned char *iv, const unsigned char *in, size_t n, unsigned char *out) {
    int l1 = 0, l2 = 0;
    if (!EVP_CipherInit_ex2(ctx, c, key, iv, enc, NULL)) cb_die("cipher init");
    if (!EVP_CipherUpdate(ctx, out, &l1, in, (int)n)) cb_die("cipher update");
    if (!EVP_CipherFinal_ex(ctx, out + l1, &l2)) cb_die("cipher final (bad padding?)");
    return (size_t)(l1 + l2);
}

static size_t gcm_encrypt(cb_algo *a, const unsigned char *iv, const unsigned char *in, size_t n,
                          unsigned char *ct_tag) {
    int l1 = 0, l2 = 0;
    if (!EVP_EncryptInit_ex2(a->cctx, a->gcm, a->key, iv, NULL)) cb_die("gcm init");
    if (!EVP_EncryptUpdate(a->cctx, ct_tag, &l1, in, (int)n)) cb_die("gcm update");
    if (!EVP_EncryptFinal_ex(a->cctx, ct_tag + l1, &l2)) cb_die("gcm final");
    if (!EVP_CIPHER_CTX_ctrl(a->cctx, EVP_CTRL_GCM_GET_TAG, 16, ct_tag + l1 + l2)) cb_die("gcm tag");
    return (size_t)(l1 + l2) + 16;
}

static size_t gcm_decrypt(cb_algo *a, const unsigned char *iv, const unsigned char *ct_tag, size_t n,
                          unsigned char *out) {
    int l1 = 0, l2 = 0;
    if (n < 16) cb_die("gcm: ciphertext too short");
    if (!EVP_DecryptInit_ex2(a->cctx, a->gcm, a->key, iv, NULL)) cb_die("gcm init");
    if (!EVP_DecryptUpdate(a->cctx, out, &l1, ct_tag, (int)(n - 16))) cb_die("gcm update");
    if (!EVP_CIPHER_CTX_ctrl(a->cctx, EVP_CTRL_GCM_SET_TAG, 16, (void *)(ct_tag + n - 16))) cb_die("gcm set tag");
    if (EVP_DecryptFinal_ex(a->cctx, out + l1, &l2) <= 0) cb_die("gcm: tag verification failed");
    return (size_t)(l1 + l2);
}

static void chacha_iv(const unsigned char nonce[12], unsigned char iv[16]) {
    memset(iv, 0, 4); /* 32-bit block counter = 0, little endian (BC ChaCha7539Engine) */
    memcpy(iv + 4, nonce, 12);
}

static size_t rsa_wrap(cb_algo *a, const unsigned char *in, size_t n, unsigned char *out) {
    size_t outlen = sizeof a->wrapped;
    if (EVP_PKEY_encrypt(a->rsa_enc, out, &outlen, in, n) <= 0) cb_die("RSA encrypt");
    return outlen;
}

static size_t rsa_unwrap(cb_algo *a, const unsigned char *in, size_t n, unsigned char *out, size_t cap) {
    size_t outlen = cap;
    if (EVP_PKEY_decrypt(a->rsa_dec, out, &outlen, in, n) <= 0) cb_die("RSA decrypt");
    return outlen;
}

static EVP_PKEY_CTX *rsa_ctx(EVP_PKEY *k, int enc) {
    EVP_PKEY_CTX *c = EVP_PKEY_CTX_new_from_pkey(NULL, k, NULL);
    if (!c || (enc ? EVP_PKEY_encrypt_init(c) : EVP_PKEY_decrypt_init(c)) <= 0 ||
        EVP_PKEY_CTX_set_rsa_padding(c, RSA_PKCS1_OAEP_PADDING) <= 0 ||
        EVP_PKEY_CTX_set_rsa_oaep_md(c, EVP_sha256()) <= 0 ||
        EVP_PKEY_CTX_set_rsa_mgf1_md(c, EVP_sha256()) <= 0)
        cb_die("RSA OAEP context");
    return c;
}

/* ------------------------------------------------------------------ setup */
cb_algo *cb_algo_new(const char *name, const unsigned char *plain, size_t len) {
    char base[64];
    size_t nl = strlen(name);
    int raw = nl > 4 && strcmp(name + nl - 4, "_raw") == 0;
    if (nl >= sizeof base) return NULL;
    memcpy(base, name, nl + 1);
    if (raw) base[nl - 4] = '\0';
    /* optional key size suffix (digits) for the sized families; "chacha20" keeps its digits */
    int bits = 0;
    size_t bl = strlen(base), d = bl;
    while (d > 0 && base[d - 1] >= '0' && base[d - 1] <= '9') d--;
    if (d < bl && (strncmp(base, "aes", d) == 0 || strncmp(base, "aesgcm", d) == 0 ||
                   strncmp(base, "rsa", d) == 0 || strncmp(base, "elgamal", d) == 0) &&
        (d == 3 || d == 6 || d == 7)) {
        bits = atoi(base + d);
        base[d] = '\0';
    }

    cb_algo *a = cb_xmalloc(sizeof *a);
    memset(a, 0, sizeof *a);
    a->raw = raw;
    a->plain = plain;
    a->len = len;
    const char *desc = NULL;
    if (strcmp(base, "aes") == 0) a->kind = K_AES;
    else if (strcmp(base, "aesgcm") == 0) a->kind = K_AESGCM;
    else if (strcmp(base, "chacha20") == 0) a->kind = K_CHACHA;
    else if (strcmp(base, "rsa") == 0) a->kind = K_RSA;
    else if (strcmp(base, "elgamal") == 0) a->kind = K_ELGAMAL;
    else if ((a->lwc = cb_lwc_find(base)) != NULL) { a->kind = K_LWC; desc = a->lwc->impl; }
    else { free(a); return NULL; }
    switch (a->kind) {
    case K_AES: case K_AESGCM:
        if (!bits) bits = 256;
        if (bits != 128 && bits != 192 && bits != 256) cb_die("AES key size must be 128, 192 or 256");
        break;
    case K_RSA:
        if (!bits) bits = 2048;
        if (bits != 1024 && bits != 2048 && bits != 3072 && bits != 4096) cb_die("RSA size must be 1024, 2048, 3072 or 4096");
        break;
    case K_ELGAMAL:
        if (!bits) bits = CB_ELGAMAL_DEFAULT_BITS;
        if (!cb_elgamal_supported(bits)) cb_die("ElGamal group must be 768, 1024, 2048, 3072 or 4096");
        break;
    default:
        break;
    }
    a->bits = bits;
    a->keylen = (a->kind == K_AES || a->kind == K_AESGCM) ? (size_t)bits / 8 : 32;
    for (const cb_algo_info *i = INFOS; i->name && !desc; i++)
        if (strcmp(i->name, base) == 0) desc = i->description;
    char d1[300];
    snprintf(d1, sizeof d1, desc, bits); /* descriptions without %d ignore the argument */
    snprintf(a->desc, sizeof a->desc, "%s [%s mode]", d1, raw ? "raw" : "mirror");

    a->cap = len + 64;
    a->pt_copy = cb_xmalloc(a->cap);
    a->env = cb_xmalloc(a->cap);
    a->b64 = cb_xmalloc(cb_b64_encoded_len(a->cap) + 1);
    a->denv = cb_xmalloc(a->cap);
    a->out = cb_xmalloc(a->cap);
    a->out_copy = cb_xmalloc(a->cap);

    /* explicit fetch once: OpenSSL 3 implicit fetches on every init are slow */
    if (!(a->cctx = EVP_CIPHER_CTX_new())) cb_die("EVP_CIPHER_CTX_new");
    char cname[32];
    int symbits = (a->kind == K_AES || a->kind == K_AESGCM) ? bits : 256;
    if (!(a->cbc = EVP_CIPHER_fetch(NULL, "AES-256-CBC", NULL))) cb_die("fetch AES-256-CBC");
    snprintf(cname, sizeof cname, "AES-%d-CBC", symbits);
    if (!(a->sym_cbc = EVP_CIPHER_fetch(NULL, cname, NULL))) cb_die("fetch %s", cname);
    snprintf(cname, sizeof cname, "AES-%d-GCM", symbits);
    if (!(a->gcm = EVP_CIPHER_fetch(NULL, cname, NULL))) cb_die("fetch %s", cname);
    if (!(a->chacha = EVP_CIPHER_fetch(NULL, "ChaCha20", NULL))) cb_die("fetch ChaCha20");

    /* keys as in DesktopMain: random key, zero 16-byte IV, random ChaCha nonce */
    memset(a->key, 0, sizeof a->key);
    cb_random(a->key, a->keylen);
    memset(a->iv, 0, sizeof a->iv);
    if (a->kind == K_CHACHA) cb_random(a->iv, 12);
    if (a->kind == K_RSA) {
        if (!(a->rsa = EVP_PKEY_Q_keygen(NULL, NULL, "RSA", (size_t)bits))) cb_die("RSA keygen");
        a->rsa_enc = rsa_ctx(a->rsa, 1);
        a->rsa_dec = rsa_ctx(a->rsa, 0);
    }
    if (a->kind == K_ELGAMAL) a->eg = cb_elgamal_new(bits);
    return a;
}

void cb_algo_free(cb_algo *a) {
    if (!a) return;
    free(a->pt_copy); free(a->env); free(a->b64); free(a->denv); free(a->out); free(a->out_copy);
    EVP_CIPHER_CTX_free(a->cctx);
    EVP_CIPHER_free(a->cbc); EVP_CIPHER_free(a->sym_cbc); EVP_CIPHER_free(a->gcm); EVP_CIPHER_free(a->chacha);
    EVP_PKEY_CTX_free(a->rsa_enc); EVP_PKEY_CTX_free(a->rsa_dec); EVP_PKEY_free(a->rsa);
    cb_elgamal_free(a->eg);
    free(a);
}

const char *cb_algo_description(const cb_algo *a) { return a->desc; }
int cb_algo_is_raw(const cb_algo *a) { return a->raw; }

void cb_algo_fix(cb_algo *a, const unsigned char *key, size_t keylen, const unsigned char *iv, size_t ivlen) {
    memset(a->key, 0, sizeof a->key);
    memcpy(a->key, key, keylen < sizeof a->key ? keylen : sizeof a->key);
    memset(a->iv, 0, sizeof a->iv);
    memcpy(a->iv, iv, ivlen < sizeof a->iv ? ivlen : sizeof a->iv);
    a->fixed = 1;
}

/* ------------------------------------------------------------------ encrypt */
void cb_algo_encrypt(cb_algo *a) {
    /* mirror: the plaintext String is turned into a fresh byte array (toByteArray) */
    const unsigned char *pt = a->plain;
    if (!a->raw) { memcpy(a->pt_copy, a->plain, a->len); pt = a->pt_copy; }

    switch (a->kind) {
    case K_AES:
        a->env_len = cipher_run(a->cctx, a->sym_cbc, 1, a->key, a->iv, pt, a->len, a->env);
        break;
    case K_AESGCM: {
        if (!a->fixed) cb_random(a->iv, 12);
        memcpy(a->env, a->iv, 12);
        a->env_len = 12 + gcm_encrypt(a, a->iv, pt, a->len, a->env + 12);
        break;
    }
    case K_CHACHA: {
        unsigned char iv[16];
        chacha_iv(a->iv, iv);
        a->env_len = cipher_run(a->cctx, a->chacha, 1, a->key, iv, pt, a->len, a->env);
        break;
    }
    case K_RSA:
    case K_ELGAMAL:
        cb_random(a->hk, sizeof a->hk); /* fresh AES key || IV per call, as DesktopMain */
        a->env_len = cipher_run(a->cctx, a->cbc, 1, a->hk, a->hk + 32, pt, a->len, a->env);
        if (a->kind == K_RSA) {
            /* Java: AES.encrypt (Base64 inside) first, then wrap; order kept */
            if (!a->raw) a->b64_len = cb_b64_encode(a->env, a->env_len, a->b64);
            a->wrapped_len = rsa_wrap(a, a->hk, sizeof a->hk, a->wrapped);
        } else {
            if (!a->raw) a->b64_len = cb_b64_encode(a->env, a->env_len, a->b64);
            cb_elgamal_encrypt(a->eg, a->hk, sizeof a->hk, a->wrapped);
            a->wrapped_len = cb_elgamal_ct_bytes(a->eg);
        }
        return; /* the hybrid ciphertext Base64 is done above */
    case K_LWC: {
        const cb_lwc_impl *l = a->lwc;
        if (!a->fixed) cb_random(a->iv, l->nonce_bytes);
        memcpy(a->env, a->iv, l->nonce_bytes);
        size_t clen = 0;
        if (l->enc(a->env + l->nonce_bytes, &clen, pt, a->len, a->iv, a->key) != 0) cb_die("%s encrypt", l->name);
        a->env_len = l->nonce_bytes + clen;
        break;
    }
    }
    if (!a->raw) a->b64_len = cb_b64_encode(a->env, a->env_len, a->b64);
}

/* ------------------------------------------------------------------ decrypt */
static void decrypt_env(cb_algo *a, const unsigned char *env, size_t env_len) {
    switch (a->kind) {
    case K_AES:
        a->out_len = cipher_run(a->cctx, a->sym_cbc, 0, a->key, a->iv, env, env_len, a->out);
        break;
    case K_AESGCM:
        if (env_len < 12) cb_die("aesgcm: ciphertext too small");
        a->out_len = gcm_decrypt(a, env, env + 12, env_len - 12, a->out);
        break;
    case K_CHACHA: {
        unsigned char iv[16];
        chacha_iv(a->iv, iv);
        a->out_len = cipher_run(a->cctx, a->chacha, 0, a->key, iv, env, env_len, a->out);
        break;
    }
    case K_RSA:
    case K_ELGAMAL: {
        unsigned char hk[512]; /* OpenSSL RSA decrypt needs room for a full modulus */
        size_t n;
        if (a->kind == K_RSA) n = rsa_unwrap(a, a->wrapped, a->wrapped_len, hk, sizeof hk);
        else {
            int r = cb_elgamal_decrypt(a->eg, a->wrapped, hk, sizeof hk);
            if (r < 0) cb_die("ElGamal: bad padding");
            n = (size_t)r;
        }
        if (n != 48) cb_die("hybrid: unwrapped key has %zu bytes", n);
        a->out_len = cipher_run(a->cctx, a->cbc, 0, hk, hk + 32, env, env_len, a->out);
        break;
    }
    case K_LWC: {
        const cb_lwc_impl *l = a->lwc;
        if (env_len < l->nonce_bytes + l->tag_bytes) cb_die("%s: ciphertext too small", l->name);
        size_t mlen = 0;
        if (l->dec(a->out, &mlen, env + l->nonce_bytes, env_len - l->nonce_bytes, env, a->key) != 0)
            cb_die("%s: tag verification failed", l->name);
        a->out_len = mlen;
        break;
    }
    }
}

void cb_algo_decrypt(cb_algo *a) {
    if (a->raw) { decrypt_env(a, a->env, a->env_len); return; }
    size_t n = cb_b64_decode(a->b64, a->b64_len, a->denv);
    if (n == (size_t)-1) cb_die("Base64 decode");
    decrypt_env(a, a->denv, n);
    memcpy(a->out_copy, a->out, a->out_len); /* bytes -> String */
}

int cb_algo_check(const cb_algo *a) {
    const unsigned char *o = a->raw ? a->out : a->out_copy;
    return a->out_len == a->len && memcmp(o, a->plain, a->len) == 0;
}

const char *cb_algo_last_b64(const cb_algo *a) { return a->b64; }

int cb_algo_decrypt_b64(cb_algo *a, const char *b64) {
    size_t len = strlen(b64);
    if (cb_b64_decoded_cap(len) > a->cap) return 0;
    size_t n = cb_b64_decode(b64, len, a->denv);
    if (n == (size_t)-1) return 0;
    decrypt_env(a, a->denv, n);
    return a->out_len == a->len && memcmp(a->out, a->plain, a->len) == 0;
}
