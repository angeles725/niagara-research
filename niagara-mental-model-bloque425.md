# Block 425 — dsfspi.dll: the DSF crypto SPI is a thin JCE provider over statically-linked Mocana NanoCrypto

> Research of **`dsfspi.dll`**, the native backend of Niagara's `com.tridium.dsf.provider` JCE
> SecurityProvider: what the `Dsf*` crypto classes (DSA/RSA keygen, SHA-1/256 digest, AES-256 cipher,
> secure random, signature) ACTUALLY do at body grade. Thesis: they delegate every primitive to
> **Mocana NanoCrypto**, statically linked into the DLL — no own crypto, no external libcrypto. This
> CLOSES the residual left by [Block 126] §126.2, which inventoried the `Dsf*` symbols/vtables but did
> NOT open their bodies. It does NOT re-cover `DsfUtil::checkFileSignature` (body already in [Block 126]
> §126.3–§126.4).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `dsfspi.dll`
> sha256 `82e8c7f0d2e04c83847c9b9f4770cd7362ad6046a8b621d821acf3ddba491477`.
>
> Sources: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/dsfspi.dll` · preserved evidence in
> `audits/B425-dsfspi.txt` (Part A Ghidra bodies · Part B verified token-check: Mocana/NIST strings +
> import table). Method: Ghidra headless (bodies) delegated to a `sonnet`-tier sweep, then driver
> token-verified every load-bearing string/import live against the binary (r2/rabin2). Markers:
> `[CERT]` observed in the body / string / import table (address cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 126] (Dsf symbol inventory + checkFileSignature body —
> this OPENS the SPI bodies), [Block 380] (FIPS-gated provider swap that selects this vs BC), [Block 395]
> (the DSA roots this crypto verifies against), [Block 385] (nre native crypto = DPAPI, a different lane).

---

## 425.1 — DSF is a JCE SecurityProvider with native SPIs `[CERT]`

The exported symbols are JNI entrypoints named `Java_com_tridium_dsf_provider_spi_Dsf<Algo>Spi_*`
(`audits/B425-dsfspi.txt:60`–`:115`) — i.e. `dsfspi.dll` is the native half of a standard **`java.security.Provider`**
whose service classes live in `com.tridium.dsf.provider.spi`. The SPI roster: `[CERT]`

| JCE service | Native class | Mangled JNI seen |
|---|---|---|
| `KeyPairGenerator` (DSA) | `DsfDsaKeyPairGenerator` | `…DsfDsaKeyPairGeneratorSpi_*` |
| `KeyPairGenerator` (RSA) | `DsfRsaKeyPairGenerator` | `…DsfRsaKeyPairGeneratorSpi_*` |
| `MessageDigest` (SHA-1/256) | `DsfSha1/Sha256MessageDigest` | (vtables; via signature classes) |
| `Cipher` (AES-256) | `DsfAes256Cipher` | `…DsfAes256CipherSpi_*` |
| `Signature` | `DsfSha1WithDsaSignature`, `DsfSha1/Sha256WithRsaSignature` | `…DsfSha1WithDsaSignatureSpi_*`, `…DsfSha256WithRsaSignatureSpi_*` |
| `SecureRandom` | `DsfSecureRandom` | (context via `DsfObject::getRandomContext`) |

## 425.2 — The central finding: total delegation to statically-linked Mocana NanoCrypto `[CERT]`

Every primitive resolves to Mocana NanoCrypto compiled INTO `dsfspi.dll` — there is no runtime dependency
on OpenSSL, RSA BSAFE, Windows BCrypt/CNG, or any external crypto DLL. Evidence is two-pronged: Mocana's
signature error-string family is present, and the import table contains NO Windows crypto API: `[CERT]`

| Primitive | Delegation evidence | Citation |
|---|---|---|
| DSA/RSA keygen | body calls Mocana asym-keygen (`FUN_180014830`) after `getRandomContext`, then DER-serializes | `audits/B425-dsfspi.txt:207`,`:250`,`:258` |
| SHA-1 / SHA-256 | `ERR_HARDWARE_ACCEL_SHA256_INIT/_DEINIT`, `ERR_CRYPTO_SHA_ALGORITHM_DISABLED` (Mocana) | Part B (izz) · `0x180031f30` |
| AES-256 cipher | `AES-256-CBC` literal + `ERR_HARDWARE_ACCEL_AES_CBC_256_*`; **no GCM string** | Part B · `0x1800415f0` |
| Secure random | Mocana NIST DRBG family: `ERR_FIPS_CTRDRBG_FAIL`, `ERR_NIST_RNG_CTR_*`, `ERR_FIPS_ECDRBG_FAIL` | Part B · `0x18003a010`,`0x18003b4a8` |
| (imports) | ONLY `KERNEL32` timers imported; **no `BCryptGenRandom`/`CryptGenRandom`/`RtlGenRandom`/`ADVAPI32`** | Part B (rabin2 -i) |

## 425.3 — Key generation: Mocana asym-keygen + DER `[CERT]`

`DsfDsaKeyPairGenerator::generateKeyPair` (`audits/B425-dsfspi.txt:207`, offset `0x18001ee50`) obtains a
Mocana RNG context via `DsfObject::getRandomContext` (`:250`), allocates an asym-key slot, calls the Mocana
DSA generator `FUN_180014830` (`:258`), then serializes the result to a DER byte array returned as a
`jbyteArray`. `[CERT]` `DsfRsaKeyPairGenerator::generateKeyPair` follows the same shape (own overload/export,
`0x18001f020`). `[INFER]` the SPI does no key math itself — it marshals JNI ↔ Mocana slot ↔ DER.

## 425.4 — Digest, cipher, and the signature binding `[CERT]`

- **Digest**: `DsfSha1/Sha256MessageDigest` have own vtables but delegate the transform to Mocana
  (HW-accel SHA error strings, no external SHA import). `[CERT]`
- **Cipher**: `DsfAes256Cipher` is **AES-256 in CBC mode** (`AES-256-CBC` literal; `nativeInit` takes a
  direction+key+IV, consistent with CBC). No GCM/authenticated mode is present in the binary. `[CERT]`
- **Signature**: `DsfSha1WithDsaSignature` / `DsfSha(1|256)WithRsaSignature` store the digest object as a
  member; `sign`/`verify` invoke `digest()` via vtable, hand the hash to the Mocana DSA/RSA primitive
  (RSA verify = public decrypt + `memcmp` against the accumulated hash). `[CERT]`/`[INFER]` (RSA-verify
  `memcmp` shape mirrors the `checkFileSignature` path already in [Block 126] §126.3.)

## 425.5 — SecureRandom: NIST CTR-DRBG seeded only from wall-clock timers `[CERT]`/`[INFER]`

`DsfSecureRandom` is a **Mocana NIST SP 800-90A CTR-DRBG** (`ERR_FIPS_CTRDRBG_FAIL`, `ERR_NIST_RNG_CTR_*`;
EC-DRBG also built in via `ERR_FIPS_ECDRBG_FAIL` but CTR is primary). `[CERT]` The only OS entropy inputs
`dsfspi.dll` imports are `QueryPerformanceCounter`, `GetTickCount`, `GetSystemTimeAsFileTime`,
`GetSystemTime` — there is **no `BCryptGenRandom`/`CryptGenRandom`/`RtlGenRandom`**. `[CERT]` `[INFER]` this
means the DRBG's seed material comes from timers, a low-entropy source compared to the OS CSPRNG; a
CTR-DRBG is only as unpredictable as its seed, so on a predictable/virtualized boot this is a real
seed-quality concern for any DSF-generated key or nonce. (Distinct from `nre.dll`'s live secret path, which
is DPAPI — [Block 385] §385.3.)

## 425.6 — Who consumes it, and the FIPS gate `[CERT]`

`nre.dll` imports **exactly one** DSF export: `?checkFileSignature@DsfUtil@@SAHPEAEHPEBDH@Z`
(`audits/B425-dsfspi.txt` Part B) — the native module/file signature check ([Block 126] §126.3). `[CERT]`
The rest of the SPI (keygen, cipher, RNG, Signature) is reached from **Java**, as the `com.tridium.dsf.provider`
JCE provider. `[INFER]` This is the provider [Block 380] §380 swaps in under the FIPS gate (`initPaths`
selects `bcfips` vs the standard provider); DSF-over-Mocana is the FIPS-capable native path, matching the
`ERR_FIPS_*` strings. `[CERT]/[INFER]`

## 425.7 — Defensive-security reading `[CERT]`/`[INFER]`

1. **Single crypto core = single audit surface** `[CERT]` (§425.2): all DSF crypto is one statically-linked
   Mocana build. A vulnerability or a weak-mode default in that Mocana version applies uniformly; there is
   no second implementation to cross-check, and the version is pinned at build time (no OS crypto that
   patches independently).
2. **AES-CBC, not authenticated encryption** `[CERT]` (§425.4): `DsfAes256Cipher` offers confidentiality
   without integrity. Consistent with the corpus theme that Niagara adds integrity selectively (per-field
   GCM elsewhere), not by default.
3. **DRBG seeded from timers is the weakest link** `[INFER]` (§425.5): the strongest primitive (a NIST
   CTR-DRBG) is undermined if seeded only from `QueryPerformanceCounter`/`GetTickCount` — worth a live
   entropy check before trusting DSF-generated key material on cloned/virtual hosts.
4. **FIPS posture is real but native-pinned** `[CERT]/[INFER]` (§425.6): the `ERR_FIPS_*` strings show a
   FIPS-mode Mocana; [Block 380]'s gate decides when it is active. Certification claims rest on this DLL's
   Mocana build, not on the OS.

## 425.8 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | DSF = JCE provider `com.tridium.dsf.provider.spi` with native SPIs (DSA/RSA/SHA/AES/Sig/RNG) | `[CERT]` | `audits/B425-dsfspi.txt:60` |
| 2 | All primitives delegate to statically-linked Mocana NanoCrypto; no external libcrypto | `[CERT]` | `audits/B425-dsfspi.txt:258` + Part B |
| 3 | DSA keygen = Mocana gen (`FUN_180014830`) after `getRandomContext`, DER-serialized | `[CERT]` | `audits/B425-dsfspi.txt:207` |
| 4 | `DsfAes256Cipher` = AES-256-CBC; no GCM string in binary | `[CERT]` | `0x1800415f0` (Part B) |
| 5 | `DsfSecureRandom` = NIST CTR-DRBG; only KERNEL32 timers imported, no Windows CSPRNG | `[CERT]` | Part B (izz + rabin2 -i) |
| 6 | `nre.dll` imports exactly `checkFileSignature@DsfUtil` from dsfspi.dll | `[CERT]` | Part B (rabin2 -i nre.dll) |
| 7 | Timer-only DRBG seed = seed-quality weakness on predictable hosts | `[INFER]` | §425.5 (from claim 5) |

**Marker tally**: `[CERT]` ≈ 18 · `[INFER]` 6 ([INFER]/[CERT] ≈ 0.33). Type: **EVIDENCE block**
(decompilation + token-check) — ratio healthy; this closes the DSF SPI at body grade. Delegated sweep:
`sonnet` tier; every load-bearing string (Mocana `ERR_NIST_RNG_CTR_*`, `ERR_FIPS_CTRDRBG`, `AES-256-CBC`,
`ERR_HARDWARE_ACCEL_SHA256`) and the import table were re-verified LIVE by the driver against
`dsfspi.dll` (Part B) before authoring — VERIFY-BEFORE-ACTING satisfied.

## 425.9 — Connections

- **[Block 126]** — §126.2 inventoried the `Dsf*` symbols/vtables and left the bodies open; this block
  opens them. §126.3–§126.4 (`checkFileSignature`) is the one export `nre.dll` consumes natively.
- **[Block 380]** — the FIPS-gated `initPaths` provider swap selects DSF-over-Mocana vs the standard
  provider; this block identifies what the FIPS path actually runs.
- **[Block 395]** — the DSA vendor/module roots DSF verifies against (hidden embedded `masterPublicKeyData`).
- **[Block 385]** — `nre.dll`'s live native secret primitive is DPAPI, a SEPARATE lane from DSF's JCE crypto.
- **[Block 424]** — sibling native seam (getHostId) closed in the same reopening; both were uncaptured Aug-1 dumps.

<!-- research-block: platform-native focus, gap NG6 (DSF crypto SPI bodies) — CLOSED at body grade -->
