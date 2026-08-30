# B677 — JACE-8000 ARM crypto engine `libdsfspi.so`: the on-controller Niagara "DSF" JCE provider is a thin JNI facade (`com.tridium.dsf.provider.*`) over a STATICALLY-linked Mocana NanoCrypto (only `libc.so.4` needed) — AES-256-CBC cipher SPI, NIST CTR-DRBG/EC-DRBG SecureRandom, SHA-1/256, DSA/RSA keygen, SHA1WithDSA/SHAWithRSA — the ARM twin of the Windows `dsfspi.dll` ([Block 425]) (focus jace8000-qnx-native, QN1; §19 [CERT])

> **Focus:** `jace8000-qnx-native` (§16, NEW). **Gap closed:** QN1 (the ARM crypto SPI behind the JACE's
> at-rest keyring/config.bog). **Phase:** static RE, READ-ONLY. **Marker:** `[CERT]` — from the binary's own
> symbol table + string constants (the ARM ELFs are NOT stripped), corroborated by `readelf`/`nm`/`strings`.
> **Sources:** `sources/probes/B672-jace8000-sd/qn1-libdsfspi-symbols.txt` (inventory) · the binary
> `local-sd-image/bin-arm/libdsfspi.so` (ELF32 ARM LE, sha256 `bd3d6cc1f4964378e1ea3f9c92b4921a0224e3001832009a7cee4e7787aec74d`; gitignored, extracted read-only from the SD P2 `/opt/niagara/bin/`) ·
> `[CERT]` [Block 425] (Windows `dsfspi.dll`), [Block 466] (key domains), [Block 380] (FIPS provider).
>
> **Bottom line:** the JACE-8000's crypto engine is the **exact ARM twin of the Windows DSF stack** ([Block 425]).
> `libdsfspi.so` is a JNI shim exposing the Niagara `DsfSecurityProvider` JCE provider, and it **statically
> links Mocana NanoCrypto** (its only dynamic dependency is `libc.so.4` — the QNX C library). It supplies
> AES-256-CBC (the `DsfAes256Cipher`), a NIST **CTR-DRBG** SecureRandom, SHA-1/SHA-256 digests, DSA/RSA
> keypair generation, and SHA1WithDSA / SHAWithRSA signatures — i.e. the engine that encrypts the `.km`/`.kr`
> keyring and the station data at rest ([Block 466]/[Block 674]).

---

## §677.1 — It is a JNI provider facade, not a from-scratch crypto lib `[CERT]`

`nm -D` exports the JNI entry points of the Niagara **DSF** ("Data Security Framework") JCE provider
`com.tridium.dsf.provider` `[CERT qn1-libdsfspi-symbols.txt]`:

| SPI class (`com.tridium.dsf.provider.spi.*`) | JNI methods | JCE role |
|---|---|---|
| `DsfAes256CipherSpi` | `engineInit0`, `engineUpdate0`, `engineDoFinal0`, `deleteContext0` | symmetric cipher (AES-256) |
| `DsfSecureRandomSpi` | `engineSetSeed0`, `engineNextBytes0`, `engineGenerateSeed0` | RNG |
| `DsfSha1MessageDigestSpi` / `DsfSha256…` | `engineUpdate0`, `engineDigest0`, `engineReset0` | hashes |
| `DsfSha1WithDsaSignatureSpi` | `engineInitSign0/Verify0`, `engineSign0`, `engineVerify0` | DSA signature |
| `DsfShaWithRsaSignatureSpi` | (sign/verify) | RSA signature |
| `DsfRsaKeyPairGeneratorSpi` / `DsfDsaKeyPairGeneratorSpi` | `generateRsaKeyPair0` / `generateDsaKeyPair0` | keygen |
| `DsfSecurityProvider` | `initRandomContext0` | provider bootstrap |

C++ source names survive in the symbols (`DsfAes256Cipher.cpp`, `DsfAes256CipherSpi.cpp`), confirming a
thin C++ SPI layer. This is the **same provider architecture** as the Windows `dsfspi.dll` in [Block 425].

## §677.2 — Mocana NanoCrypto, statically linked `[CERT]`

The heavy lifting is **Mocana NanoCrypto**, embedded statically — `readelf -d` shows the ONLY `NEEDED`
library is **`libc.so.4`** (the QNX C runtime), i.e. no external crypto `.so` `[CERT]`. The Mocana surface is
unmistakable in the symbol/string table `[CERT]`:
- Lifecycle: `MOCANA_initMocana`, `MOCANA_initMocanaStaticMemory`, `MOCANA_freeMocana`, `MOCANA_addEntropy32Bits`,
  `MOCANA_addEntropyBit`, `ERR_MOCANA_NOT_INITIALIZED`.
- Symmetric: `AESALGO_blockEncrypt/Decrypt(Ex)`, `AESALGO_makeAesKey`, `AESCCM_*`, `AESCMAC_*`, `AESXTS*`,
  `AESKWRAP_encrypt/decrypt` (RFC-3394 `…3394` + RFC-5649 `…5649` key wrap), `AES_EAX_encryptMessage`.
- Cipher advertised: `AES-128-CBC`, `AES-192-CBC`, **`AES-256-CBC`** (the DSF default).
- Asymmetric/PKI: `CA_MGMT_convertKey*`, `ASN1CERT_Sign`, `ASN1CERT_generateSelfSignedCertificate`,
  `ALG_ID_rsaEncryptionCreate`, `ALG_ID_ecPublicKeyCreate`.

This confirms [Block 425]'s Windows finding ("DSF delegates everything to statically-linked Mocana NanoCrypto")
holds on the ARM/QNX controller unchanged.

## §677.3 — RNG = NIST CTR-DRBG (FIPS), with a hardware-random path `[CERT]`

The SecureRandom is Mocana's **NIST DRBG** `[CERT]`: error strings `ERR_FIPS_CTRDRBG_FAIL`,
`ERR_FIPS_ECDRBG_FAIL`, `ERR_NIST_RNG_CTR_*`, `ERR_NIST_RNG_DBRG_RESEED_NEEDED`, plus a **NIST KDF**
(`ERR_NIST_KDF`, `ERR_NIST_KDF_COUNTER_KEY_SIZES` = SP800-108 counter-mode KDF) and a **hardware-accel random**
path (`ERR_HARDWARE_ACCEL_RANDOM_NO_INIT/DEINIT`, `ERR_NANOTAP_GET_RANDOM_NUM_FAILED`). Entropy is fed via
`MOCANA_addEntropy32Bits`/`addEntropyBit`.
- **§14 refinement of [Block 425]:** the Windows twin was noted as "CTR-DRBG seeded only by timers" — the ARM
  build additionally exposes a **hardware-accelerated RNG** path (the AM335x has an on-SoC RNG) and a NIST KDF,
  so on the JACE the DRBG can be seeded from hardware, not only timers. `[INFER — the HW path is present in
  symbols; whether it is actually selected at runtime is a live check (QN1-G1).]`

## §677.4 — Where this sits in the JACE at-rest security story `[CERT]/[INFER]`

`libdsfspi.so` is the engine under the keyring/at-rest material located on the card in [Block 674]:
`/etc/km/.km`, `/home/niagara/security/.kr`, `keystore.jceks`, and the encrypted `config.bog`. The DSF
provider's **AES-256-CBC** + the machine-key domain ([Block 466]) is what makes the live `config.bog`
decryptable only on the device. This block identifies the ENGINE and its algorithms; the exact key-derivation
wiring from the machine key to the AES key (which Mocana KDF call, salt/iteration parameters) is a decompile
follow-up (QN1-G2) — the symbols name the primitives, not the parameter values.

## §677.5 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | libdsfspi.so = JNI facade for the DSF JCE provider com.tridium.dsf.provider | [CERT] | qn1-libdsfspi-symbols.txt (nm -D Java_*) |
| 2 | SPIs: AES256Cipher, SecureRandom, SHA1/256, DSA/RSA keygen, SHA1WithDSA/SHAWithRSA | [CERT] | qn1 symbols |
| 3 | Mocana NanoCrypto statically linked; only NEEDED = libc.so.4 | [CERT] | readelf -d; MOCANA_*/AESALGO_* symbols |
| 4 | Cipher = AES-256-CBC (+128/192, CCM, KWRAP 3394/5649, CMAC, XTS, EAX) | [CERT] | strings |
| 5 | RNG = NIST CTR-DRBG/EC-DRBG (FIPS) + NIST KDF + HW-accel random path | [CERT] | ERR_FIPS_*DRBG / ERR_NIST_* strings |
| 6 | ARM twin of Windows dsfspi.dll (B425); §14 adds HW-RNG path vs "timers only" | [CERT] + [INFER] | §677.3; [Block 425] |

**Tally:** 6 claims — 6 [CERT] (one [INFER] on the HW-RNG runtime selection → QN1-G1; one decompile follow-up
QN1-G2 for KDF parameters). 0 unmarked. Symbols/strings are direct evidence (not decompiled offsets), so no
twin-binary corroboration needed for identity; parameter-level claims are deferred, not asserted.

## §677.6 — Connections

- **[Block 425]** — the Windows `dsfspi.dll` (Mocana NanoCrypto facade); this is its ARM twin, confirmed.
- **[Block 466]** — the machine-key vs passphrase at-rest domains this engine implements.
- **[Block 674]** — the on-card keyring files (`.km`/`.kr`/keystore.jceks/config.bog) this engine protects.
- **[Block 380]** — the FIPS-gated provider selection (Windows); the FIPS DRBG here is the ARM crypto side.
- **[Block 392]/[Block 676]** — signing (DSA/RSA + ASN1CERT here are the provider's signature/keygen side).
