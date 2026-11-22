# C baseline

Native C counterpart of the Java benchmark harness (`desktop/src/main/kotlin/DesktopMain.kt`).
It runs the same algorithms with the same parameters, the same command line, the same protocol and the same CSV/manifest output, so the Java results can be compared with a native baseline row by row. On Windows, `scripts/run-desktop-energy-5.ps1 -Impl c` runs it with energy measurement.

## Algorithms and implementations

| Name | Algorithm and parameters (as in the Java harness) | C implementation |
|---|---|---|
| `aes` | AES-256-CBC, PKCS#7, zero IV | OpenSSL 3 EVP |
| `aesgcm` | AES-256-GCM, random 96-bit IV per call, 128-bit tag | OpenSSL 3 EVP |
| `chacha20` | ChaCha20 (RFC 7539, 96-bit nonce, counter 0, no MAC) | OpenSSL 3 EVP |
| `rsa` | AES-256-CBC (fresh key + IV per call) + RSA-2048 OAEP (SHA-256, MGF1-SHA-256) wrap of key‖IV | OpenSSL 3 |
| `elgamal` | AES-256-CBC (fresh key + IV per call) + ElGamal PKCS#1 v1.5 wrap of key‖IV, the `ElGamalLite` groups: **2048-bit** RFC 3526 group 14 by default (768/1024 RFC 2409, 3072/4096 RFC 3526 selectable), g = 2 | `src/elgamal.c` on OpenSSL BIGNUM, following Bouncy Castle's `ElGamalEngine` + `PKCS1Encoding` |
| `ascon` | Ascon-AEAD128 (NIST SP 800-232), 128-bit key/nonce/tag | [ascon/ascon-c](https://github.com/ascon/ascon-c) `asconaead128/opt64` (`opt32` on 32-bit CPUs), designers, CC0 |
| `elephant` | Elephant v2 `elephant200` (Delirium), 128-bit key, 96-bit nonce, 128-bit tag | [rweather/lwc-finalists](https://github.com/rweather/lwc-finalists) `src/combined`, third-party, MIT |
| `giftcofb` | GIFT-COFB v1.1, 128-bit key/nonce/tag | [aadomn/gift](https://github.com/aadomn/gift) `giftcofb128v1/opt32` (fixsliced; Adomnicai & Peyrin), CC0 |
| `grain128` | Grain-128AEADv2, 128-bit key, 96-bit nonce, 64-bit tag | [rweather/lwc-finalists](https://github.com/rweather/lwc-finalists) `src/combined`, third-party, MIT |
| `xoodyak` | Xoodyak (round 3), 128-bit key/nonce/tag | [XKCP](https://github.com/XKCP/XKCP) `lib/high/Xoodyak` + `lib/low/Xoodoo/plain`, designers |

The lightweight ciphers use **optimized portable C** (no assembly, no SIMD). For Elephant and Grain-128AEAD the designers published only reference C (Grain's optimized code is x86-only C++), so a well-tested third-party optimized implementation is used to keep all five at a similar optimization level.
OpenSSL selects hardware AES and assembly at run time, as in normal use. The exact third-party commits are pinned in `fetch-third-party.sh`.

## Modes

- **mirror** (default name, e.g. `ascon`): the same data steps as the Java harness inside every timed call. Encrypt copies the plaintext (Java `String.toByteArray`), encrypts, builds the envelope (IV/nonce‖ciphertext‖tag) and Base64-encodes it. Decrypt reverses this and copies the plaintext out. Use this mode for C vs Java comparisons.
- **raw** (suffix `_raw`, e.g. `ascon_raw`): only the cipher on byte buffers. This is the pure algorithm cost.

Both modes keep the per-call IV/nonce/key generation of the Java harness. Buffers are allocated once per run.

## Build

```bash
./fetch-third-party.sh      # pinned sources into third_party/
./build.sh                  # -> build/crypto-c   (-O2, generic CPU; flags recorded in each manifest)
```
Needs a C compiler and OpenSSL 3 headers:
- Debian / Raspberry Pi OS: `sudo apt-get install -y libssl-dev`
- Windows: MinGW-w64 gcc + OpenSSL, for example Strawberry Perl. In Git Bash: `PATH=/c/Strawberry/c/bin:$PATH ./build.sh` (or set `OPENSSL_DIR`).
- macOS: Xcode command line tools + `brew install openssl@3` (found automatically, or set `OPENSSL_DIR`).

## Run

```bash
build/crypto-c <algorithm> <power> <rounds> <inner> <out.csv> [target_batch_ms] [warmup_ms] [warmup_small_power]
build/crypto-c list
```
The arguments, CSV columns and warm-up logic match `DesktopMain.kt`. Key sizes go in the name: `aes128/192/256`, `aesgcm128/192/256`, `rsa1024`–`rsa4096`, `elgamal768`–`elgamal4096` (defaults: AES-256, RSA-2048, ElGamal-2048).

Energy runs on Windows (from the repository root):
```powershell
.\scripts\run-desktop-energy-5.ps1 -PowerJoular '<powerjoular.exe>' -ProjectRoot . -Impl c            # main comparison
.\scripts\run-desktop-energy-5.ps1 -PowerJoular '<powerjoular.exe>' -ProjectRoot . -Impl c -KeySizes  # key sizes
```

## Validation against Bouncy Castle

```bash
LIB=../desktop/build/install/crypto-desktop/lib       # built Java harness
java -cp "$LIB/*" tools/VectorGen.java gen tools/vectors.txt
build/crypto-c selftest tools/vectors.txt tools/c2java.txt
java -cp "$LIB/*" tools/VectorGen.java verify tools/c2java.txt
```
`VectorGen` uses the Java harness classes themselves (`AES`, `AESGcm`, `ChaCha20Cipher`, `RsaLite`, `ElGamalLite`, `BcLwcAead`/`LwcSpecs`). It covers 14 plaintext lengths from 0 to 4096 bytes.
- The C code must reproduce every deterministic output byte-for-byte and decrypt it.
- It must decrypt the Java RSA/ElGamal hybrids.
- Java must decrypt everything C produces.
