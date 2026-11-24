#!/usr/bin/env bash
# Build the C baseline harness (build/crypto-c, or build/crypto-c.exe on Windows).
#
#   ./build.sh                 generic -O2 build (the configuration chosen for the thesis)
#   CC=clang ./build.sh        another compiler
#   OPT="-O3 -march=native" ./build.sh   other optimisation flags (recorded in every manifest)
#
# Needs a C compiler and OpenSSL 3 (headers + libcrypto):
#   Raspberry Pi / Debian:  sudo apt-get install -y libssl-dev
#   Windows: MinGW-w64 gcc + OpenSSL, e.g. Strawberry Perl (C:/Strawberry or set OPENSSL_DIR)
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

CC=${CC:-gcc}
OPT=${OPT:-"-O2"}
TP=third_party
OUT=${OUT:-build}
OBJ=$OUT/obj
WARN="-Wall -Wextra -Wno-unused-parameter -Wno-sign-compare"

# Ascon: the designers' optimized portable C for the target word size
case "$($CC -dumpmachine)" in
  x86_64*|aarch64*|arm64*|powerpc64*|riscv64*) ASCON_VARIANT=opt64 ;;
  *) ASCON_VARIANT=opt32 ;;
esac

# OpenSSL flags
if [[ "${OS:-}" == Windows_NT ]]; then
  EXE=.exe
  OPENSSL_DIR=${OPENSSL_DIR:-$(for d in /c/Strawberry/c /c/Pearl/Strawberry/c; do [[ -f $d/include/openssl/evp.h ]] && echo $d && break; done)}
  [[ -n "$OPENSSL_DIR" ]] || { echo "OpenSSL not found: set OPENSSL_DIR" >&2; exit 1; }
  # C99 printf (%zu, %lld) on MinGW
  OSSL_CFLAGS="-I$OPENSSL_DIR/include -D__USE_MINGW_ANSI_STDIO=1"
  OSSL_LIBS="$OPENSSL_DIR/lib/libcrypto.a -lws2_32 -lcrypt32 -ladvapi32 -luser32 -lbcrypt"
elif [[ "$(uname -s)" == Darwin ]]; then
  # macOS: Homebrew OpenSSL 3 (brew install openssl@3), or set OPENSSL_DIR
  EXE=
  OPENSSL_DIR=${OPENSSL_DIR:-$(brew --prefix openssl@3 2>/dev/null || true)}
  [[ -f "$OPENSSL_DIR/include/openssl/evp.h" ]] || { echo "OpenSSL 3 not found: brew install openssl@3 (or set OPENSSL_DIR)" >&2; exit 1; }
  OSSL_CFLAGS="-I$OPENSSL_DIR/include"
  OSSL_LIBS="-L$OPENSSL_DIR/lib -lcrypto"
else
  EXE=
  OSSL_CFLAGS=$(pkg-config --cflags libcrypto 2>/dev/null || true)
  OSSL_LIBS=$(pkg-config --libs libcrypto 2>/dev/null || echo -lcrypto)
fi

rm -rf "$OBJ"; mkdir -p "$OBJ"
objs=()
cc() {  # cc <object name> <source> [extra flags...]
  local o=$OBJ/$1.o src=$2; shift 2
  $CC -std=gnu11 $OPT $WARN "$@" -c "$src" -o "$o"
  objs+=("$o")
}

# --- Ascon-AEAD128 (ascon/ascon-c) ---
A=$TP/ascon-c/crypto_aead/asconaead128/$ASCON_VARIANT
ASCON_FLAGS="-I$A -I$TP/ascon-c/tests -Dcrypto_aead_encrypt=ascon_crypto_aead_encrypt -Dcrypto_aead_decrypt=ascon_crypto_aead_decrypt"
for f in aead permutations printstate; do [[ -f $A/$f.c ]] && cc ascon_$f $A/$f.c $ASCON_FLAGS; done

# --- GIFT-COFB (aadomn/gift opt32, fixsliced) ---
G=$TP/gift/crypto_aead/giftcofb128v1/opt32
GIFT_FLAGS="-I$G -Dcrypto_aead_encrypt=gift_crypto_aead_encrypt -Dcrypto_aead_decrypt=gift_crypto_aead_decrypt"
for f in encrypt giftb128; do cc gift_$f $G/$f.c $GIFT_FLAGS; done

# --- Xoodyak (XKCP: high-level Xoodyak + Xoodoo plain) ---
X=$TP/XKCP
XOO_FLAGS="-Isrc/xkcp-config -I$X/lib/common -I$X/lib/high/Xoodyak -I$X/lib/low/Xoodoo -I$X/lib/low/Xoodoo/plain -I$X/lib/low/Xoodoo/plain/SnP -I$X/lib/low/common -I$X/tests/SUPERCOP/Xoodyak_aead_round3 -Dcrypto_aead_encrypt=xoodyak_crypto_aead_encrypt -Dcrypto_aead_decrypt=xoodyak_crypto_aead_decrypt"
cc xoodyak_Xoodyak $X/lib/high/Xoodyak/Xoodyak.c $XOO_FLAGS
cc xoodyak_Xoodoo-plain $X/lib/low/Xoodoo/plain/Xoodoo-plain.c $XOO_FLAGS
cc xoodyak_Xoodyak-plain $X/lib/low/Xoodoo/plain/Xoodyak-plain.c $XOO_FLAGS
cc xoodyak_encrypt $X/tests/SUPERCOP/Xoodyak_aead_round3/encrypt.c $XOO_FLAGS

# --- Elephant-Delirium and Grain-128AEADv2 (rweather/lwc-finalists, plain C) ---
R=$TP/lwc-finalists/src/combined
for f in elephant-delirium internal-keccakp-200 grain-aead internal-grain128 internal-util; do
  [[ -f $R/$f.c ]] && cc rw_$f $R/$f.c -I$R
done

# --- harness ---
DEFS=(-DCB_ASCON_VARIANT="\"$ASCON_VARIANT\"" -DCB_CFLAGS="\"$OPT (CC $($CC -dumpversion), $($CC -dumpmachine))\"")
for f in main algos lwc_glue elgamal b64 platform selftest; do cc h_$f src/$f.c $OSSL_CFLAGS "${DEFS[@]}"; done

$CC $OPT "${objs[@]}" $OSSL_LIBS -o "$OUT/crypto-c$EXE"
echo "built $OUT/crypto-c$EXE  (ascon $ASCON_VARIANT, $OPT, $($CC -dumpmachine))"
