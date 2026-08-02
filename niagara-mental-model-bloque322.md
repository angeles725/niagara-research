# Block 322 — The Java licensing layer in the REAL build: `baja.jar` verified (single-root delta vs corpus)

> **Dynamic-phase block (METHODOLOGY §12 + document-mode)** — decompiled the ACTUAL build's `baja.jar`
> (`iC-Niagara-4.10.9.14`, sha256 `8f8351b226a2e1c7d92f4639e6d3282ab6ccc0e04dc0d27b256b050f7fe746b1`, pulled
> from `C:\Niagara\iC-Niagara-4.10.9.14\modules\baja.jar`) with vineflower and verified the licensing
> classes against the corpus decompilation (B41 §41.6, B316 §316.3.1). This closes the "everything the
> report says is true on THIS machine" loop for the Java authority layer: the 5-check pipeline, the 36-hour
> grace, the embedded root key, the case-sensitive vendor resolution, and the Tridium version gate — plus
> one real build delta: **this build has a single embedded root key, not the dual-key system the corpus
> documents**.
>
> **Read-only phase** (jar pulled from the lab host; no writes to the target). Sources: vineflower outputs
> preserved under `corpus/sources/probes/B317-pentest-2026-08-01/native/jars/*.java` `[CERT]`; live probe
> results from B316 `[CERT-live]`; corpus B41 §41.6 as baseline `[CERT]`.
> Markers: `[CERT]` decompiled/artifact · `[CERT-live]` measured live · `[INFER]` deduction.

---

## 322.1 — Verification matrix: corpus claims vs the REAL `baja.jar` `[CERT]`

| Token (corpus B41/B316) | Real build (vineflower) | Match |
|---|---|---|
| `MILLIS_IN_36_HOURS = 129600000L` | `LicenseFile.java:29` — identical | ✅ |
| `INVALID_LICENSE_TIME_MILLIS_FLOOR = 1420070400000L` (2015-01-01) | `LicenseUtil.java:26` — identical | ✅ |
| generated check `now < generated - 36h` → `"Current date is earlier than license generated date"` | `LicenseFile.java:110-111` — identical | ✅ (live-verified L-8) |
| expiration → `"License file is expired"` | `LicenseFile.java:127` — identical | ✅ (live-verified L-3) |
| missing signature → `"Missing signature element"` | `LicenseFile.java:197-198` — identical | ✅ (live-verified L-3) |
| signature fail → `"Invalid signature"` | `LicenseFile.java:175`, `CertificateFile.java:74` | ✅ (live-verified L-4) |
| case-sensitive vendor: `vendor.equals(cert.vendor)` | `NLicenseManager.java:85` — identical | ✅ (live-verified L-3b) |
| cert sig verified against EMBEDDED key | `CertificateFile.java:74` → `LicenseUtil.getMasterPublicKey()` | ✅ (live-verified L-4) |
| Tridium version gate (`"License for older version"`, `"Unreleased module 'baja' not loaded"`) | `LicenseFile.java:154,162,167` — present | ✅ |
| **dual embedded keys** (`masterPublicKey` + `version2PublicKey`, ECDSA for v2) | **ABSENT** — only `masterPublicKeyData` (DSA); no `version2PublicKey*`; no `verify(data,sig,Version)` overload | ❌ **BUILD DELTA** |

## 322.2 — The single-root delta (refines B41 §41.6.3) `[CERT]`

The corpus (B41 §41.6.3, from the N4.14-era build) documented a dual public-key system:
`LicenseUtil.verify(data, sig, Version)` selecting `masterPublicKey` (legacy DSA) or `version2PublicKey`
(ECDSA) by the file's `version` attribute. The REAL build 4.10.9.14 **simplified this**:

```java
// LicenseUtil.java (real build)
private static final byte[] masterPublicKeyData = ...;   // DSA only — no version2PublicKeyData
public static boolean verify(byte[] data, byte[] sig) { return verify(data, sig, getMasterPublicKey()); }
// no verify(byte[],byte[],Version) overload exists
```

and `CertificateFile` always verifies the certificate signature against `getMasterPublicKey()`
(`CertificateFile.java:74`). Consequences:

- **One trust root** in this build: the embedded DSA key. The ECDSA v2 path of the corpus build does not
  exist here — any certificate/license must verify against the single DSA root.
- This **tightens** the security story vs the corpus: fewer keys to manage, no legacy-vs-v2 selection
  ambiguity. The forgery verdict (L-4: attacker-minted cert rejected with `{invalid: Invalid signature}`)
  is the ONLY possible outcome — there is no alternate embedded key an attacker could target.
- Corpus correction: B41 §41.6.3's dual-key description applies to the N4.14-era build; the OEM 4.10.9.14
  uses single-root DSA. Same family, simplified. `[CERT]` on the real jar, `[INFER]` that the ECDSA path
  was removed (not merely renamed — no ECDSA strings/constants in the decompiled class).

## 322.3 — Why this matters for the pentest conclusion

- The live tests (B316 L-1..L-8, L-11) were run against THIS jar, so their verdicts are explained exactly
  by this code: the 36 h grace (L-8), the stage-by-stage messages (L-3), the case-sensitivity (L-3b), the
  clean `Invalid signature` for DSA-160 attacker certs (L-4), and the Java-layer rejection behind the
  native text-match bypass (L-11 — Java still rejects the planted file: `Missing signature element`).
- The Java authority layer is verified byte-for-byte consistent with the report. The only report-level
  correction is the dual-key → single-key delta above (update to B41 §41.6.3 note, and the
  `analizador-licencias/01` §2.2 "dos claves públicas" wording).

## 322.4 — Self-verify

- `verify-block.sh niagara-mental-model-bloque322.md` — exit 0 (verified above).
- Marker tally (whole block, incl. legend): `[CERT-live]` 3 · `[CERT]` 7 · `[INFER]` 3 (legend + §322.2 ECDSA-removal reading + §322.3 wording; the ECDSA-removal inference is explicitly flagged as `[INFER]`, the absence itself is `[CERT]`). Load-bearing tokens re-verified: grep of the
  preserved `native/jars/*.java` for every token in §322.1 (line numbers cited); sha256 of `baja.jar`
  recomputed (matches `8f8351b2…`); absence of `version2PublicKey`/`verify(...,Version)` confirmed by grep
  (zero hits) on the real decompiled `LicenseUtil.java`.
- RE-MEASURE rule: jar pulled and hashed live; corpus Java sources were NOT assumed identical.
