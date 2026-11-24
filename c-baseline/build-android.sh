#!/usr/bin/env bash
# Cross-compile the C baseline for Android arm64 -> build-android/crypto-c (a plain executable,
# run from adb shell; the same file can be linked into the app through JNI later).
# OpenSSL 3.5.9 (the Pi 5's version) is built from source and linked statically, so the binary
# needs only Android's libc. Run on Linux or WSL (make, perl, curl):
#
#   ANDROID_NDK_ROOT=$HOME/android-ndk-r30 ./build-android.sh
#
# The thesis build used NDK r30 (30.0.16248370), API 35, the harness at -O2 and OpenSSL with
# its default flags (-O3, assembly on) plus -fPIC, so libcrypto.a also links into the app (JNI).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
: "${ANDROID_NDK_ROOT:?set ANDROID_NDK_ROOT to the NDK directory}"
API=${API:-35}
OSSL=openssl-3.5.9
OSSL_SHA256=603f5602e2eef00d77fbd429d34dcd5822bb301757a1bc9cdb24c670f1eb859a
W=$PWD/build-android/openssl
export PATH=$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH

if [[ ! -f $W/install/lib/libcrypto.a ]]; then
  mkdir -p "$W"
  curl -fsSL -o "$W/$OSSL.tar.gz" "https://github.com/openssl/openssl/releases/download/$OSSL/$OSSL.tar.gz"
  echo "$OSSL_SHA256  $W/$OSSL.tar.gz" | sha256sum -c --quiet
  tar -xzf "$W/$OSSL.tar.gz" -C "$W"
  (cd "$W/$OSSL" &&
   ./Configure android-arm64 -D__ANDROID_API__=$API -fPIC no-shared no-tests no-docs --prefix="$W/install" --libdir=lib >/dev/null &&
   make -j"$(nproc)" build_libs >/dev/null && make install_dev >/dev/null)
fi

CC="aarch64-linux-android$API-clang -I$W/install/include -L$W/install/lib -Qunused-arguments" OUT=build-android ./build.sh
