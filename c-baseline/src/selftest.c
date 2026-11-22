/* Cross-check of the C implementations against Bouncy Castle / the Java harness classes.
 *
 * vectors.txt is produced by tools/VectorGen.java ("gen") from the Java classes. Lines:
 *   vec|<algo>|<len>|<key hex>|<iv/nonce hex>|<base64 envelope>   deterministic algorithms
 *   rsa|<len>|<PKCS#8 DER hex>|<wrapped key||IV hex>|<base64 AES-CBC ciphertext>
 *   elgamal|<group bits>|<len>|<x hex>|<wrapped key||IV hex>|<base64 AES-CBC ciphertext>
 * The plaintext of length len is the ASCII pattern "ABC...XYZ0123456789" repeated.
 * Checks: C reproduces every deterministic envelope byte-for-byte and decrypts it; C decrypts
 * the Java RSA/ElGamal hybrid ciphertexts. If c2java.txt is given, C writes its own encryptions
 * in the same format (fresh randomness, same keys) for "VectorGen verify" on the Java side. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <openssl/evp.h>
#include <openssl/rsa.h>
#include <openssl/x509.h>

#include "algos.h"
#include "b64.h"
#include "elgamal.h"
#include "platform.h"

static const char PATTERN[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

static unsigned char *pattern(size_t n) {
    unsigned char *p = cb_xmalloc(n);
    for (size_t i = 0; i < n; i++) p[i] = (unsigned char)PATTERN[i % 36];
    return p;
}

static size_t unhex(const char *h, unsigned char *out, size_t cap) {
    size_t n = strlen(h) / 2;
    if (n > cap) cb_die("hex field too long");
    for (size_t i = 0; i < n; i++) {
        unsigned v;
        if (sscanf(h + 2 * i, "%2x", &v) != 1) cb_die("bad hex");
        out[i] = (unsigned char)v;
    }
    return n;
}

static void hexout(FILE *f, const unsigned char *b, size_t n) {
    for (size_t i = 0; i < n; i++) fprintf(f, "%02x", b[i]);
}

static size_t aes_cbc(int enc, const unsigned char *key, const unsigned char *iv, const unsigned char *in,
                      size_t n, unsigned char *out) {
    EVP_CIPHER_CTX *c = EVP_CIPHER_CTX_new();
    int l1 = 0, l2 = 0;
    if (!c || !EVP_CipherInit_ex2(c, EVP_aes_256_cbc(), key, iv, enc, NULL) ||
        !EVP_CipherUpdate(c, out, &l1, in, (int)n) || !EVP_CipherFinal_ex(c, out + l1, &l2))
        cb_die("AES-CBC");
    EVP_CIPHER_CTX_free(c);
    return (size_t)(l1 + l2);
}

static EVP_PKEY_CTX *oaep(EVP_PKEY *k, int enc) {
    EVP_PKEY_CTX *c = EVP_PKEY_CTX_new_from_pkey(NULL, k, NULL);
    if (!c || (enc ? EVP_PKEY_encrypt_init(c) : EVP_PKEY_decrypt_init(c)) <= 0 ||
        EVP_PKEY_CTX_set_rsa_padding(c, RSA_PKCS1_OAEP_PADDING) <= 0 ||
        EVP_PKEY_CTX_set_rsa_oaep_md(c, EVP_sha256()) <= 0 || EVP_PKEY_CTX_set_rsa_mgf1_md(c, EVP_sha256()) <= 0)
        cb_die("OAEP ctx");
    return c;
}

/* Decrypt a hybrid AES-CBC Base64 ciphertext with an unwrapped key||IV; 1 if == plain. */
static int hybrid_check(const unsigned char *hk, size_t hklen, const char *ct_b64, const unsigned char *plain,
                        size_t len) {
    if (hklen != 48) return 0;
    size_t bl = strlen(ct_b64);
    unsigned char *ct = cb_xmalloc(cb_b64_decoded_cap(bl) + 1), *pt = cb_xmalloc(bl + 32);
    size_t n = cb_b64_decode(ct_b64, bl, ct);
    int ok = n != (size_t)-1;
    if (ok) {
        size_t m = aes_cbc(0, hk, hk + 32, ct, n, pt);
        ok = m == len && memcmp(pt, plain, len) == 0;
    }
    free(ct); free(pt);
    return ok;
}

/* Fresh C hybrid encryption: returns Base64 ciphertext (malloc) and fills hk. */
static char *hybrid_encrypt(const unsigned char *plain, size_t len, unsigned char hk[48]) {
    cb_random(hk, 48);
    unsigned char *ct = cb_xmalloc(len + 32);
    size_t n = aes_cbc(1, hk, hk + 32, plain, len, ct);
    char *b = cb_xmalloc(cb_b64_encoded_len(n) + 1);
    cb_b64_encode(ct, n, b);
    free(ct);
    return b;
}

int cb_selftest(const char *vectors, const char *c2java) {
    FILE *f = fopen(vectors, "r");
    if (!f) cb_die("cannot open %s", vectors);
    FILE *o = c2java ? fopen(c2java, "w") : NULL;
    if (c2java && !o) cb_die("cannot write %s", c2java);
    static char line[1 << 22];
    int pass = 0, fail = 0;
    while (fgets(line, sizeof line, f)) {
        line[strcspn(line, "\r\n")] = '\0';
        if (!line[0] || line[0] == '#') continue;
        /* split on '|' keeping empty fields (e.g. ChaCha20 of an empty input is "") */
        char *fld[8] = {0};
        int nf = 0;
        for (char *s = line; nf < 8;) {
            fld[nf++] = s;
            char *bar = strchr(s, '|');
            if (!bar) break;
            *bar = '\0';
            s = bar + 1;
        }
        int ok = 0;
        char what[96];

        if (strcmp(fld[0], "vec") == 0 && nf == 6) {
            size_t len = strtoul(fld[2], NULL, 10);
            unsigned char *plain = pattern(len), key[64], iv[32];
            size_t kl = unhex(fld[3], key, sizeof key), il = unhex(fld[4], iv, sizeof iv);
            cb_algo *a = cb_algo_new(fld[1], plain, len);
            if (!a) cb_die("unknown algorithm %s", fld[1]);
            cb_algo_fix(a, key, kl, iv, il);
            cb_algo_encrypt(a);
            int same = strcmp(cb_algo_last_b64(a), fld[5]) == 0;
            int dec = cb_algo_decrypt_b64(a, fld[5]);
            ok = same && dec;
            snprintf(what, sizeof what, "%-9s len %5zu: %s, %s", fld[1], len,
                     same ? "same bytes as Java" : "DIFFERENT bytes", dec ? "decrypts Java" : "CANNOT decrypt Java");
            if (o) { /* C encryption with fresh randomness where the algorithm uses it */
                cb_algo *c = cb_algo_new(fld[1], plain, len);
                cb_algo_fix(c, key, kl, iv, il);
                cb_algo_encrypt(c);
                fprintf(o, "vec|%s|%zu|%s|%s|%s\n", fld[1], len, fld[3], fld[4], cb_algo_last_b64(c));
                cb_algo_free(c);
            }
            cb_algo_free(a);
            free(plain);
        } else if (strcmp(fld[0], "rsa") == 0 && nf == 5) {
            size_t len = strtoul(fld[1], NULL, 10);
            unsigned char *plain = pattern(len), der[4096], wr[512], hk[512];
            size_t dl = unhex(fld[2], der, sizeof der), wl = unhex(fld[3], wr, sizeof wr), hl = sizeof hk;
            const unsigned char *p = der;
            EVP_PKEY *k = d2i_AutoPrivateKey(NULL, &p, (long)dl);
            if (!k) cb_die("cannot parse the Java RSA key");
            EVP_PKEY_CTX *d = oaep(k, 0);
            ok = EVP_PKEY_decrypt(d, hk, &hl, wr, wl) > 0 && hybrid_check(hk, hl, fld[4], plain, len);
            snprintf(what, sizeof what, "rsa       len %5zu: %s", len, ok ? "decrypts Java OAEP+CBC" : "CANNOT decrypt Java");
            if (o) {
                unsigned char nhk[48], nwr[512];
                size_t nwl = sizeof nwr;
                char *b = hybrid_encrypt(plain, len, nhk);
                EVP_PKEY_CTX *e = oaep(k, 1);
                if (EVP_PKEY_encrypt(e, nwr, &nwl, nhk, 48) <= 0) cb_die("RSA encrypt");
                fprintf(o, "rsa|%zu|%s|", len, fld[2]);
                hexout(o, nwr, nwl);
                fprintf(o, "|%s\n", b);
                free(b);
                EVP_PKEY_CTX_free(e);
            }
            EVP_PKEY_CTX_free(d);
            EVP_PKEY_free(k);
            free(plain);
        } else if (strcmp(fld[0], "elgamal") == 0 && nf == 6) {
            int bits = atoi(fld[1]);
            size_t len = strtoul(fld[2], NULL, 10);
            unsigned char *plain = pattern(len), wr[CB_ELGAMAL_MAX_CT_BYTES], hk[512];
            cb_elgamal *k = cb_elgamal_from_x_hex(bits, fld[3]);
            if (unhex(fld[4], wr, sizeof wr) != cb_elgamal_ct_bytes(k)) cb_die("ElGamal ciphertext size");
            int r = cb_elgamal_decrypt(k, wr, hk, sizeof hk);
            ok = r >= 0 && hybrid_check(hk, (size_t)r, fld[5], plain, len);
            snprintf(what, sizeof what, "elgamal%-4d len %5zu: %s", bits, len, ok ? "decrypts Java ElGamal+CBC" : "CANNOT decrypt Java");
            if (o) {
                unsigned char nhk[48], nwr[CB_ELGAMAL_MAX_CT_BYTES];
                char *b = hybrid_encrypt(plain, len, nhk);
                cb_elgamal_encrypt(k, nhk, 48, nwr);
                fprintf(o, "elgamal|%d|%zu|%s|", bits, len, fld[3]);
                hexout(o, nwr, cb_elgamal_ct_bytes(k));
                fprintf(o, "|%s\n", b);
                free(b);
            }
            cb_elgamal_free(k);
            free(plain);
        } else {
            cb_die("bad vector line starting with '%s'", fld[0] ? fld[0] : "");
        }
        printf("%s  %s\n", ok ? "PASS" : "FAIL", what);
        ok ? pass++ : fail++;
    }
    fclose(f);
    if (o) fclose(o);
    printf("selftest: %d passed, %d failed\n", pass, fail);
    return fail ? 1 : 0;
}
