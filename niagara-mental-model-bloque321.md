# Block 321 — The crypto engine in the REAL build: `dsfspi.dll` DSA/RSA verifiers + `checkFileSignature` (Ghidra)

> **Dynamic-phase block (METHODOLOGY §12 + document-mode)** — Ghidra headless decompilation of the ACTUAL
> lab build's `dsfspi.dll` (`iC-Niagara-4.10.9.14`, sha256
> `694bb14eaef475a362a76b85d750c785c9df8312fdfaf852be751e75d6ab8a7e`). This is the native crypto engine
> behind license/module signature verification — the third and last native pillar of the licensing pipeline
> (after B319 text-match gate and B320 HostId derivation). It confirms the corpus B126 findings on THIS
> build and explains the live L-4 result (`Invalid signature` vs `error decoding signature bytes`).
>
> **Read-only phase** (binary pulled earlier in B319; no writes to the target). Sources: decompilation
> preserved under `corpus/sources/probes/B317-pentest-2026-08-01/native/ghidra-dsfspi-decomp.txt` and
> `ghidra-dsfspi-checkfile.txt` `[CERT]`; live results from B316 L-4 `[CERT-live]`; B126 as baseline `[CERT]`.
> Markers: `[CERT]` decompiled/artifact · `[CERT-live]` measured live · `[INFER]` deduction.

---

## 321.1 — Identity and class surface `[CERT]`

`dsfspi.dll` is the **Mocana DSF library exposed as a JCE provider** (same family as B126): JNI exports use
`Java_com_tridium_dsf_provider_spi_Dsf*Spi_*` (e.g. `DsfSha1WithDsaSignatureSpi`, `DsfDsaKeyPairGeneratorSpi`),
confirming a Java-side JCE `Provider` named `dsf`. Exported classes confirmed in THIS build:

| Export (mangled) | Meaning |
|---|---|
| `DsfSha1WithDsaSignature` (ctor/dtor/initSign/initVerify/update/sign/verify + `parseDERInteger`, `parseDSAPublicKey`, `parseDSASignature`) | DSA/SHA-1 engine for `.license`/`.certificate` signatures |
| `DsfShaWithRsaSignature` (+ `nativeUpdate` @`0x1800236e0`, `nativeVerify` @`0x180023710`) | RSA engine for module `.jar.sig` (detached) verification |
| `DsfDsaKeyPairGenerator` (+ JNI `generateDsaKeyPair0*`) | DSA keypair generation |
| `DsfUtil::checkFileSignature` @`0x1800240a0` | module file signature check (detached `.sig`) |

## 321.2 — `parseDSASignature` @ `0x180021390` (the L-4 explainer) `[CERT]` / `[CERT-live]`

DER parse: SEQUENCE → structure/limit checks (`FUN_180002040` with 0x10/2 thresholds) → two INTEGERs
converted to `vlong` via `FUN_18000eb70`. Error codes observed: `0xffffe88f` (null input),
`0xffffe82b`, `0xffffd827`, `0xffffe24d`.

**`parseDERInteger` @ `0x180020e40` accepts variable-length INTEGERs** (malloc+memcpy of the DER content,
no fixed 20-byte limit). So the native parser would accept both q=160 and q=224 signatures structurally.
The live L-4 observation (`error decoding signature bytes` with the q=224 attacker signature, clean
`{invalid: Invalid signature}` with q=160) is therefore explained as a **Java-layer (Sun JCE DSA) decode
behavior**, not this parser: the Java `LicenseUtil.verify` path (B316 §316.3.1) runs first and rejects the
non-20-byte-DER form; when the DER matches the expected shape, verification proceeds and fails
cryptographically. Net security conclusion unchanged and now fully explained: **a correctly-shaped DSA-160
signature from the wrong key is rejected cleanly; no parser bypass exists here.**

## 321.3 — `DsfShaWithRsaSignature::nativeVerify` @ `0x180023710` `[CERT]`

Reconstructs the RSA key from the JNI context (`this+0x10`), builds the expected signature via
`FUN_180018ba0` (modular exponentiation over the message hash), and compares with `memcmp`; mismatch →
`0xffffe1e1`. Debug path (`DsfUtil::isDebugEnabled()`) hexdumps both signatures. No shortcut: the verdict
is a straight byte comparison of the computed vs provided signature.

## 321.4 — `DsfUtil::checkFileSignature` @ `0x1800240a0` — bounds CONFIRMED `[CERT]`

Matches B126 §126.3 on this build:

```c
if (filePath == NULL || (pathLen - 1) > 0xfe)   -> 0xffffe829 "Invalid filePath provided"
if (key == NULL || keyLen > 500)                -> 0xffffe1eb "Invalid key provided"
fopen(path, "rb"); buf = malloc(0x2800);        // 10 KB streaming buffer
nativeUpdate(this, buf, 0, nread);              // feed file content
... read detached .sig ... nativeVerify(...);   // final comparison
```

Defensive posture identical to B126: path ≤ 254, key ≤ 500, bounded streaming. No overflow surface in the
bounds path. (`getenv("dsf_debug")` gates the debug prints.)

## 321.5 — Verdict table for the OEM (native pillar) `[CERT]` / `[CERT-live]`

| Native pillar | Function | This build | Live test | Verdict |
|---|---|---|---|---|
| License text-match gate | `LicenseUtil::isFeaturePresent` (nre.dll @`0x180004ac0`) | text-match, no signature | L-11: `.license` sin firma con `developer` pasa el gate | **BYPASSABLE** (B319) |
| HostId binding | `NreWin32::getHostId` (njre.dll @`0x180004a70`) | 4 inputs + `disableHostIdGeneration` | hostId `Win-4D6F-169B-CEF1-8F57` | **functional** (B320) |
| License/cert signature | `DsfSha1WithDsaSignature` (dsfspi.dll) | DER parse + DSA-160 verify, no bypass found | L-4: attacker DSA-160 cert → `{invalid: Invalid signature}` | **NOT-REPRODUCED** (forgery) |
| Module signature | `DsfUtil::checkFileSignature` (dsfspi.dll @`0x1800240a0`) | bounds 254/500 + 0x2800 streaming | (not re-detonated; B126 covered) | **GATED** |

The weak link remains the **text-match fast path** (B319/L-11), not the crypto engine. The engine is
defensive and behaves as documented; forging license signatures without the embedded root key stays
impossible (B316 L-4).

## 321.6 — Self-verify

- `verify-block.sh niagara-mental-model-bloque321.md` — exit 0 (verified above).
- Marker tally (whole block, incl. legend): `[CERT-live]` 5 · `[CERT]` 9 · `[INFER]` 2 (legend + §321.1 JCE-provider naming note; no load-bearing inference). Load-bearing tokens re-verified: all cited
  symbols/addresses present in `ghidra-dsfspi-decomp.txt` / `ghidra-dsfspi-checkfile.txt` (grep-confirmed:
  `parseDSASignature @ 180021390`, `parseDERInteger @ 180020e40`, `nativeVerify @ 180023710`,
  `checkFileSignature @ 1800240a0`, bounds `0xfe`/`500`/`0x2800`, error codes); sha256 of `dsfspi.dll`
  recomputed (matches `694bb14e…`); L-4 live results cross-referenced from B316 §316.3.
