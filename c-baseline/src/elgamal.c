#include "elgamal.h"
#include "platform.h"

#include <stdlib.h>
#include <string.h>
#include <openssl/bn.h>

struct cb_elgamal {
    int bits;
    size_t block_in; /* ElGamalEngine input block for encryption: (bits - 1) / 8 */
    size_t half;     /* (bits + 7) / 8 */
    BIGNUM *p, *g, *x, *y, *p_minus_2, *p_minus_1_minus_x;
    BN_CTX *ctx;
    BN_MONT_CTX *mont;
};

static BIGNUM *group_prime(int bits) {
    switch (bits) {
    case 768: return BN_get_rfc2409_prime_768(NULL);
    case 1024: return BN_get_rfc2409_prime_1024(NULL);
    case 2048: return BN_get_rfc3526_prime_2048(NULL);
    case 3072: return BN_get_rfc3526_prime_3072(NULL);
    case 4096: return BN_get_rfc3526_prime_4096(NULL);
    default: return NULL;
    }
}

int cb_elgamal_supported(int bits) {
    return bits == 768 || bits == 1024 || bits == 2048 || bits == 3072 || bits == 4096;
}

static cb_elgamal *alloc_group(int bits) {
    if (!cb_elgamal_supported(bits)) cb_die("ElGamal: unsupported group size %d", bits);
    cb_elgamal *k = cb_xmalloc(sizeof *k);
    memset(k, 0, sizeof *k);
    k->bits = bits;
    k->block_in = (size_t)(bits - 1) / 8;
    k->half = (size_t)(bits + 7) / 8;
    k->ctx = BN_CTX_new();
    k->p = group_prime(bits);
    if (!k->ctx || !k->p) cb_die("ElGamal: group setup failed");
    if (BN_num_bits(k->p) != bits) cb_die("ElGamal: unexpected group size %d", BN_num_bits(k->p));
    k->g = BN_new(); k->x = BN_new(); k->y = BN_new();
    k->p_minus_2 = BN_new(); k->p_minus_1_minus_x = BN_new();
    if (!k->g || !k->x || !k->y || !k->p_minus_2 || !k->p_minus_1_minus_x) cb_die("ElGamal: BN_new");
    BN_set_word(k->g, 2);
    BN_copy(k->p_minus_2, k->p);
    BN_sub_word(k->p_minus_2, 2);
    k->mont = BN_MONT_CTX_new();
    if (!k->mont || !BN_MONT_CTX_set(k->mont, k->p, k->ctx)) cb_die("ElGamal: Montgomery setup");
    return k;
}

static void derive(cb_elgamal *k) {
    /* y = g^x mod p; cache p - 1 - x for decryption (BC: gamma^(p-1-x) * phi mod p) */
    if (!BN_mod_exp_mont(k->y, k->g, k->x, k->p, k->ctx, k->mont)) cb_die("ElGamal: y");
    BN_copy(k->p_minus_1_minus_x, k->p);
    BN_sub_word(k->p_minus_1_minus_x, 1);
    BN_sub(k->p_minus_1_minus_x, k->p_minus_1_minus_x, k->x);
}

cb_elgamal *cb_elgamal_new(int bits) {
    cb_elgamal *k = alloc_group(bits);
    /* x uniform in [2, p-2]: random in [0, p-4] + 2 */
    BIGNUM *range = BN_dup(k->p);
    BN_sub_word(range, 3);
    if (!BN_rand_range(k->x, range)) cb_die("ElGamal: x");
    BN_add_word(k->x, 2);
    BN_free(range);
    derive(k);
    return k;
}

cb_elgamal *cb_elgamal_from_x_hex(int bits, const char *x_hex) {
    cb_elgamal *k = alloc_group(bits);
    BN_free(k->x);
    k->x = NULL;
    if (!BN_hex2bn(&k->x, x_hex)) cb_die("ElGamal: bad x");
    derive(k);
    return k;
}

void cb_elgamal_free(cb_elgamal *k) {
    if (!k) return;
    BN_free(k->p); BN_free(k->g); BN_clear_free(k->x); BN_free(k->y);
    BN_free(k->p_minus_2); BN_clear_free(k->p_minus_1_minus_x);
    BN_MONT_CTX_free(k->mont);
    BN_CTX_free(k->ctx);
    free(k);
}

size_t cb_elgamal_ct_bytes(const cb_elgamal *k) { return 2 * k->half; }
size_t cb_elgamal_max_data(const cb_elgamal *k) { return k->block_in - 10; }
char *cb_elgamal_p_hex(const cb_elgamal *k) { return BN_bn2hex(k->p); }

void cb_elgamal_encrypt(cb_elgamal *k, const unsigned char *in, size_t len, unsigned char *out) {
    if (len > cb_elgamal_max_data(k)) cb_die("ElGamal: input too long");
    /* PKCS#1 v1.5 type 2 block (BC PKCS1Encoding.encodeBlock): 0x02 || nonzero random || 0x00 || data */
    unsigned char block[512];
    size_t bl = k->block_in;
    cb_random(block, bl);
    block[0] = 0x02;
    for (size_t i = 1; i < bl - len - 1; i++)
        while (block[i] == 0) cb_random(&block[i], 1);
    block[bl - len - 1] = 0x00;
    memcpy(block + bl - len, in, len);

    BN_CTX_start(k->ctx);
    BIGNUM *m = BN_CTX_get(k->ctx), *kk = BN_CTX_get(k->ctx);
    BIGNUM *gamma = BN_CTX_get(k->ctx), *phi = BN_CTX_get(k->ctx), *t = BN_CTX_get(k->ctx);
    if (!t) cb_die("ElGamal: BN_CTX_get");
    BN_bin2bn(block, (int)bl, m);
    /* k: random of p's bit length, redrawn while k == 0 or k > p - 2 (BC ElGamalEngine) */
    do {
        if (!BN_rand(kk, k->bits, BN_RAND_TOP_ANY, BN_RAND_BOTTOM_ANY)) cb_die("ElGamal: k");
    } while (BN_is_zero(kk) || BN_cmp(kk, k->p_minus_2) > 0);
    if (!BN_mod_exp_mont(gamma, k->g, kk, k->p, k->ctx, k->mont)) cb_die("ElGamal: gamma");
    if (!BN_mod_exp_mont(t, k->y, kk, k->p, k->ctx, k->mont)) cb_die("ElGamal: y^k");
    if (!BN_mod_mul(phi, m, t, k->p, k->ctx)) cb_die("ElGamal: phi");
    if (BN_bn2binpad(gamma, out, (int)k->half) != (int)k->half ||
        BN_bn2binpad(phi, out + k->half, (int)k->half) != (int)k->half)
        cb_die("ElGamal: encode");
    BN_CTX_end(k->ctx);
    OPENSSL_cleanse(block, bl);
}

int cb_elgamal_decrypt(cb_elgamal *k, const unsigned char *in, unsigned char *out, size_t out_cap) {
    BN_CTX_start(k->ctx);
    BIGNUM *gamma = BN_CTX_get(k->ctx), *phi = BN_CTX_get(k->ctx), *m = BN_CTX_get(k->ctx);
    if (!m) cb_die("ElGamal: BN_CTX_get");
    BN_bin2bn(in, (int)k->half, gamma);
    BN_bin2bn(in + k->half, (int)k->half, phi);
    if (!BN_mod_exp_mont(m, gamma, k->p_minus_1_minus_x, k->p, k->ctx, k->mont)) cb_die("ElGamal: dec exp");
    if (!BN_mod_mul(m, m, phi, k->p, k->ctx)) cb_die("ElGamal: dec mul");
    unsigned char block[512];
    size_t bl = k->block_in;
    int ok = BN_bn2binpad(m, block, (int)bl) == (int)bl;
    BN_CTX_end(k->ctx);
    if (!ok || block[0] != 0x02) return -1;
    size_t i = 1;
    while (i < bl && block[i] != 0) i++;
    if (i < 9 || i >= bl) return -1; /* at least 8 padding bytes, separator present */
    size_t len = bl - i - 1;
    if (len > out_cap) return -1;
    memcpy(out, block + i + 1, len);
    OPENSSL_cleanse(block, bl);
    return (int)len;
}
