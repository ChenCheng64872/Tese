#include "b64.h"

static const char ENC[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

size_t cb_b64_encoded_len(size_t n) { return (n + 2) / 3 * 4; }
size_t cb_b64_decoded_cap(size_t len) { return len / 4 * 3; }

size_t cb_b64_encode(const unsigned char *in, size_t n, char *out) {
    size_t i = 0, o = 0;
    for (; i + 3 <= n; i += 3) {
        unsigned v = (unsigned)in[i] << 16 | (unsigned)in[i + 1] << 8 | in[i + 2];
        out[o++] = ENC[v >> 18];
        out[o++] = ENC[(v >> 12) & 63];
        out[o++] = ENC[(v >> 6) & 63];
        out[o++] = ENC[v & 63];
    }
    if (n - i == 1) {
        unsigned v = (unsigned)in[i] << 16;
        out[o++] = ENC[v >> 18];
        out[o++] = ENC[(v >> 12) & 63];
        out[o++] = '=';
        out[o++] = '=';
    } else if (n - i == 2) {
        unsigned v = (unsigned)in[i] << 16 | (unsigned)in[i + 1] << 8;
        out[o++] = ENC[v >> 18];
        out[o++] = ENC[(v >> 12) & 63];
        out[o++] = ENC[(v >> 6) & 63];
        out[o++] = '=';
    }
    out[o] = '\0';
    return o;
}

/* 256-entry decode table (-1 = invalid), as table-driven decoders such as Bouncy Castle's. */
static signed char DEC[256];
static int dec_ready;

static void dec_init(void) {
    for (int i = 0; i < 256; i++) DEC[i] = -1;
    for (int i = 0; i < 64; i++) DEC[(unsigned char)ENC[i]] = (signed char)i;
    dec_ready = 1;
}

size_t cb_b64_decode(const char *in, size_t len, unsigned char *out) {
    if (!dec_ready) dec_init();
    if (len % 4) return (size_t)-1;
    if (len == 0) return 0;
    const unsigned char *s = (const unsigned char *)in;
    size_t o = 0, full = len - 4; /* all groups but the last have no padding */
    for (size_t i = 0; i < full; i += 4) {
        int a = DEC[s[i]], b = DEC[s[i + 1]], c = DEC[s[i + 2]], d = DEC[s[i + 3]];
        if ((a | b | c | d) < 0) return (size_t)-1;
        unsigned v = (unsigned)a << 18 | (unsigned)b << 12 | (unsigned)c << 6 | (unsigned)d;
        out[o++] = (unsigned char)(v >> 16);
        out[o++] = (unsigned char)(v >> 8);
        out[o++] = (unsigned char)v;
    }
    /* last group: may end in "=" or "==" */
    int a = DEC[s[full]], b = DEC[s[full + 1]];
    if ((a | b) < 0) return (size_t)-1;
    out[o++] = (unsigned char)(a << 2 | b >> 4);
    if (s[full + 2] == '=') return s[full + 3] == '=' ? o : (size_t)-1;
    int c = DEC[s[full + 2]];
    if (c < 0) return (size_t)-1;
    out[o++] = (unsigned char)((b & 15) << 4 | c >> 2);
    if (s[full + 3] == '=') return o;
    int d = DEC[s[full + 3]];
    if (d < 0) return (size_t)-1;
    out[o++] = (unsigned char)((c & 3) << 6 | d);
    return o;
}
