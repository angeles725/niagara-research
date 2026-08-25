# Block 484 — The native crypto core (`dsfspi.dll`): Mocana-backed PKCS#1 RSA verify/sign, the DSA-1024/SHA-1 license/cert verifier, the DER parsers, and the reconciliation that ECDSA P-256 v2.0 is NOT native (it runs through JCA)

> **Focus:** `licensing` — the NATIVE half of the parallel deep-dive. **Native RE by sibling session `Segundo`
> (2026-08-24, Ghidra `ExportDecompiledC` + radare2 corroboration); consolidated into this block by `Primero`.**
> This is the byte-level counterpart to [B482] (Java verifiers) and [B483] (license anatomy). READ-ONLY: only
> Ghidra/r2/objdump over `dsfspi.dll`; no binary executed. Markers §3.
>
> **Source:** `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/dsfspi.dll` (sha256 `82e8c7f0…`); evidence
> `sources/native-corroboration/oem-honeywell-licensing-2026-08-24/dsfspi.crypto-funcs.ghidra.c` (18 crypto
> functions, Ghidra + r2). Identity `[CERT .rsrc @0x180059058]` `Tridium.Niagara.DsfSpiLib 4.14.0.22`,
> "Mocana DSF library as a Java security provider".

## §484.1 — What Java can reach: the JCE Spi exports `[CERT export table]`

The only JNI `Spi` classes `dsfspi.dll` exposes to Java (`Java_com_tridium…`):
- **`DsfSha1WithDsaSignatureSpi`** → DSA-1024 / SHA-1 — verifies `.license` / `.certificate`.
- **`DsfSha256WithRsaSignatureSpi`** + **`DsfSha1WithRsaSignatureSpi`** → RSA — verifies modules (`checkFileSignature`, [B477-G3/B126]).
- `DsfAes256Cipher`, `Dsa/RsaKeyPairGenerator`, `SecureRandom`, `Sha1/Sha256MessageDigest`.
- **NO ECDSA/EC Spi export exists** `[CERT]` — see §484.5.

## §484.2 — RSA `nativeVerify` @0x18002b390 — PKCS#1 v1.5 `[CERT Ghidra + r2 corroborated]`

State gate `this+0x28==2`; load+validate the public key (`FUN_18001f310`); `k=(modbits+7)/8`; the RSA public-op
is dispatched through a **Mocana function pointer** `call qword[0x18002e218] = 0x18002d6b0`; `FUN_18001f6b0` does
the public-op + strips PKCS#1 padding → recovered hash; then `memcmp(expected, recovered)`.
- **Doctrine case (decompile ≠ evidence):** Ghidra rendered the `memcmp` with its return DISCARDED (looked like it
  always returns 0 = a forgery-accepting verifier). **FALSE** — r2 corroboration: `0x18002d616 = jmp memcmp`,
  `0x18002b505 mov ebx,eax` **captures** the return, `test ebx`→`je`→`xor eax,eax` (0 = OK) else `0xffffe1e1`.
  The verify is **sound**. Textbook example of why an un-corroborated decompile is not evidence.

## §484.3 — RSA `nativeSign` @0x18002b180 — PKCS#1 v1.5 `[CERT Ghidra]`

State gate `this+0x28==1`; padding rule ≥ 11 (`hashlen + DigestInfo prefix + 0x0B ≤ k`); block = `00 01 FF..FF 00
‖ DigestInfo ‖ hash`; RSA private-exp via Mocana thunk `FUN_18001e2b0`.

## §484.4 — DSA verify (license/cert) 5-arg @0x180029720 `[CERT Ghidra + r2 corroborated]`

State gate `this+0x28==2`; **signature-length restriction `uVar2-0x2c < 5` → sig ∈ [44,48] bytes = DSA-1024/SHA-1**
(q=160-bit, r/s ~20B, DER `SEQ`); the digest is SHA-1 (0x14=20B) → `vlong`; `parseDSASignature` extracts r,s;
`FUN_180013dc0` = the Mocana DSA verify → `local_60[0]==0` throws "error verifying signature", else OK. The 3-arg
`@0x1800296b0` just delegates with the `GetArrayLength` length. This is the function that validates every
`.license`/`.certificate` at the native layer.

## §484.5 — The DER layout of `<signature>` and `<publicKey>` (native "how it's composed") `[CERT Ghidra]`

- **`parseDSASignature` @0x180028e30:** `SEQUENCE (tag 0x10) { INTEGER (tag 2) r, INTEGER (tag 2) s }`; r_len/s_len
  from field +0x28; `FUN_18000ed90` = bytes→`vlong`. (This is the byte structure of the Base64 in `<signature>`,
  [B483 §483.3].)
- **`parseDSAPublicKey` @0x180028a50:** X.509 SPKI = `SEQ{ SEQ{ OID(tag 6) id-dsa, SEQ{ INT p, INT q, INT g } },
  BITSTRING(tag 3){ INT y } }`; builds an AsymmetricKey via `FUN_180013cd0(p,q,g,y)`. NB: it reads the tag-6 OID
  but does NOT compare its value to id-dsa here — the structure is DSA-specific (an ECDSA SPKI would fail on the
  `INT p/q/g` shape). (This is the byte structure of the vendor `.certificate` `<publicKey>`, [B483 §483.4].)
- **`parseDERInteger` @0x1800288e0:** expects tag 2 (else `0xffffe24d`), then malloc+memcpy of the integer bytes.

## §484.6 — FIPS / Mocana (static) `[CERT strings]`

Mocana FIPS module statically linked: `ERR_FIPS_RSA_KAT_FAILED @0x18003a028`, `ERR_FIPS_DSA_SIGN_VERIFY_FAIL`,
`ERR_MOCANA_NOT_INITIALIZED @0x18002f210`, `ERR_HARDWARE_ACCEL_DSA_1024_160 / RSA / SHA`. `vlong` = Mocana bignum
("converting message to vlong" @0x180042658). Corroborates the `fips140-2` license gate ([B481 §481.6]): the FIPS
crypto is present in the native provider, license-gated at the Java boot layer.

## §484.7 — RECONCILIATION: ECDSA P-256 v2.0 is NOT native `[CERT]` (refines B395/B482/B483)

`dsfspi.dll` exposes **no ECDSA Spi** (§484.1). The `prime256v1` (OID `2A8648CE3D030107`) / `ecPublicKey` OIDs and
the `EC_P256 @0x180041758` symbol are **static Mocana tables used for ECDH/SSH** (e.g. `ERR_SSH_EXPECTED_DSA_KEY`,
ECDH), not a license-verify path. Therefore the `version="2.0"` ECDSA P-256 path documented in Java
([B482 §482.4], [B483 §483.4], [B395]) — `LicenseUtil.getVersion2PublicKey()`→`toPublicKey(data,"ECDSA")`,
`Signature.getInstance(algorithm)` — is resolved by a **JCA provider (SunEC/BC), NOT by `dsfspi`**. Practically
moot for shipped artifacts: **every shipped `.license`/`.certificate` is `version="1.0"` → the native DSA-1024
path (§484.4)**; the ECDSA path is present in Java but unused by current material and, if used, would run in the
JVM's JCA, not the Mocana native lib. This refines the earlier "dual embedded root" framing: the DSA root is
native+Java; the ECDSA v2 root is **Java-only**.

## §484.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Identity `Tridium.Niagara.DsfSpiLib 4.14.0.22`, Mocana provider | `[CERT]` | `.rsrc @0x180059058` | PASS |
| 2 | Spi exports: DSA-1024/SHA-1 (license/cert) + RSA SHA-256/SHA-1 (modules); NO ECDSA Spi | `[CERT]` | export table | PASS |
| 3 | RSA nativeVerify @0x18002b390 PKCS#1 v1.5, Mocana ptr; memcmp verdict CAPTURED (r2-corroborated) | `[CERT+corrob]` | Ghidra + r2 `0x18002b505` | PASS |
| 4 | RSA nativeSign @0x18002b180 PKCS#1 v1.5 (00 01 FF..FF 00 ‖ DigestInfo) | `[CERT]` | Ghidra | PASS |
| 5 | DSA verify 5-arg @0x180029720 sig∈[44,48]=DSA-1024/SHA-1; Mocana DSA verify | `[CERT+corrob]` | Ghidra + r2 | PASS |
| 6 | DER: parseDSASignature SEQ{INT r,INT s}; parseDSAPublicKey SPKI DSA; parseDERInteger tag2 | `[CERT]` | @0x180028e30/0x180028a50/0x1800288e0 | PASS |
| 7 | Mocana FIPS static (KAT strings); corroborates fips140-2 gate | `[CERT]` | strings @0x18003a028/0x18002f210 | PASS |
| 8 | ECDSA P-256 v2.0 NOT native (no Spi; EC symbols = Mocana ECDH/SSH tables); Java-only via JCA | `[CERT]` | export table; EC_P256@0x180041758 | PASS (refines B395/B482/B483) |

**Tally:** 8 claims, all `[CERT]` (2 explicitly r2-corroborated). Native RE credit: sibling session `Segundo`.

## §484.9 — Connections & open gaps

- Native counterpart of [B482]/[B483]; refines [B395] (the v2 ECDSA root is Java-only, not native).
- [B477-G3]/[B126] `checkFileSignature` (RSA module verify) was done separately by Primero — not re-done here.
- **B482-G1** (module trust store: `truststore.jks` [B392] vs `cacerts`+TPK [B482]) → to be answered by Segundo's
  `nverify.exe` pass (which store/anchor the X.509 chain uses). Pending → block B485.
- **B485** (pending) native `nverify.exe` X.509 chain + `nre/njre` gate bodies (Segundo, in-flight).
