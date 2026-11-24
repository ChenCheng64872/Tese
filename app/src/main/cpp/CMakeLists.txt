# The C baseline (c-baseline/) as a JNI library for the app: the same sources and flags as
# c-baseline/build.sh, plus native_bench.c (the JNI entry points). Built only with
# ./gradlew -PnativeBench ..., after c-baseline/fetch-third-party.sh and c-baseline/build-android.sh
# (which builds OpenSSL 3.5.9 as a static, position-independent libcrypto.a).
cmake_minimum_required(VERSION 3.22)
project(cryptobench C)

set(CB ${CMAKE_CURRENT_SOURCE_DIR}/../../../../c-baseline)
set(TP ${CB}/third_party)
set(OSSL ${CB}/build-android/openssl/install)
foreach(f ${TP}/ascon-c ${TP}/gift ${TP}/XKCP ${TP}/lwc-finalists ${OSSL}/lib/libcrypto.a)
  if(NOT EXISTS ${f})
    message(FATAL_ERROR "missing ${f}: run c-baseline/fetch-third-party.sh and c-baseline/build-android.sh first")
  endif()
endforeach()

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_EXTENSIONS ON)                                   # -std=gnu11, as build.sh
set(CB_OPT -O2 -Wall -Wextra -Wno-unused-parameter -Wno-sign-compare)   # last -O wins over the build type's

# Ascon-AEAD128 (designers' opt64)
set(A ${TP}/ascon-c/crypto_aead/asconaead128/opt64)
add_library(cb_ascon OBJECT ${A}/aead.c ${A}/permutations.c ${A}/printstate.c)
target_include_directories(cb_ascon PRIVATE ${A} ${TP}/ascon-c/tests)
target_compile_definitions(cb_ascon PRIVATE crypto_aead_encrypt=ascon_crypto_aead_encrypt crypto_aead_decrypt=ascon_crypto_aead_decrypt)

# GIFT-COFB (fixsliced opt32)
set(G ${TP}/gift/crypto_aead/giftcofb128v1/opt32)
add_library(cb_gift OBJECT ${G}/encrypt.c ${G}/giftb128.c)
target_include_directories(cb_gift PRIVATE ${G})
target_compile_definitions(cb_gift PRIVATE crypto_aead_encrypt=gift_crypto_aead_encrypt crypto_aead_decrypt=gift_crypto_aead_decrypt)

# Xoodyak (XKCP plain)
set(X ${TP}/XKCP)
add_library(cb_xoodyak OBJECT ${X}/lib/high/Xoodyak/Xoodyak.c ${X}/lib/low/Xoodoo/plain/Xoodoo-plain.c
            ${X}/lib/low/Xoodoo/plain/Xoodyak-plain.c ${X}/tests/SUPERCOP/Xoodyak_aead_round3/encrypt.c)
target_include_directories(cb_xoodyak PRIVATE ${CB}/src/xkcp-config ${X}/lib/common ${X}/lib/high/Xoodyak ${X}/lib/low/Xoodoo
            ${X}/lib/low/Xoodoo/plain ${X}/lib/low/Xoodoo/plain/SnP ${X}/lib/low/common ${X}/tests/SUPERCOP/Xoodyak_aead_round3)
target_compile_definitions(cb_xoodyak PRIVATE crypto_aead_encrypt=xoodyak_crypto_aead_encrypt crypto_aead_decrypt=xoodyak_crypto_aead_decrypt)

# Elephant-Delirium and Grain-128AEADv2 (rweather/lwc-finalists)
set(R ${TP}/lwc-finalists/src/combined)
add_library(cb_rw OBJECT ${R}/elephant-delirium.c ${R}/internal-keccakp-200.c ${R}/grain-aead.c ${R}/internal-grain128.c ${R}/internal-util.c)
target_include_directories(cb_rw PRIVATE ${R})

# Harness (without main.c) + JNI
add_library(cryptobench SHARED native_bench.c ${CB}/src/algos.c ${CB}/src/lwc_glue.c ${CB}/src/elgamal.c
            ${CB}/src/b64.c ${CB}/src/platform.c
            $<TARGET_OBJECTS:cb_ascon> $<TARGET_OBJECTS:cb_gift> $<TARGET_OBJECTS:cb_xoodyak> $<TARGET_OBJECTS:cb_rw>)
target_include_directories(cryptobench PRIVATE ${CB}/src ${OSSL}/include)
target_compile_definitions(cryptobench PRIVATE CB_ASCON_VARIANT="opt64")
foreach(t cb_ascon cb_gift cb_xoodyak cb_rw cryptobench)
  target_compile_options(${t} PRIVATE ${CB_OPT})
endforeach()
target_link_libraries(cryptobench ${OSSL}/lib/libcrypto.a log)
