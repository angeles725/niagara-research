# Block 492 — signing-pki reopen capstone: the reconciled module-signing trust model after the 2026-08-24 native+Java deep-dive — RSA-2048 detached `.sig` verified against cacerts (P12, `changeit`, 99 roots) + an embedded RSA-2048 TPK memcmp pin, NOT a dedicated `truststore.jks`

> **Focus:** `signing-pki` (reopen/capstone). This block consolidates the module-signing surface after this
> session corrected its trust anchor, tying together the Java verifiers [B482], the native crypto core [B484],
> the launcher gates [B485], and the native module verifier [B489]. READ-ONLY synthesis of already-cited blocks;
> no new RE. Markers §3.

## §492.1 — The reconciled module-signing model `[CERT]`

1. **Scheme:** a module JAR carries a detached `<name>.sig` = **RSA-2048** (256 raw bytes) plus standard
   `java.util.jar` X.509 signing (`META-INF/NIAGARA4.{SF,RSA}`) — [B126 §126.1], [B482 §482.1].
2. **Verifier (Java):** `JarSignatureRegistry` → `JarVerifier.verify()` → `CertificateChainValidator`
   PKIX (`CertPathValidator("PKIX")`), **anchors = system + user `cacerts`**, **`setRevocationEnabled(false)`**,
   then the **RSA-2048 TPK leaf-pin** if `SecurityConstants.canCheckTpk()` — [B482 §482.1-2].
3. **Verifier (native, nverify.exe):** same model in C — SHA-256 manifest+per-file + detached PKCS#7, trust
   store = an **embedded cacerts PKCS#12 (`changeit`, 99 stock roots)** + the **RSA-2048 TPK memcmp pin** on the
   signer's public key ("Signed by TPK"/"Not signed by TPK") — [B489 §489.2].
4. **Native crypto primitives:** `dsfspi.dll` Mocana-backed RSA `nativeVerify`/`nativeSign` (PKCS#1 v1.5) —
   [B484 §484.2-3]. `checkFileSignature` streams 10 KiB vs the detached `.sig` — [B477-G3/B126].
5. **Launcher gates:** `createVM` refuses `-javaagent`/`-agentpath` without the `developer` feature, and
   FIPS is a `fips140-2`-gated boolean — [B485].

## §492.2 — The corrected trust anchor (vs [B392]) `[CERT]`

[B392] (the previous signing-pki capstone) named the module trust store `security/truststore.jks` (`changeit`).
**Two independent code reads correct this** (Java `validateCertChain` [B482] + native `nverify` [B489]): the
module-verify store is **cacerts (P12/JKS) with ~99 stock CA roots**, and the real Tridium-specific control is an
**embedded RSA-2048 TPK** the verifier byte-compares against the signer's public key — NOT a dedicated
`truststore.jks`. The `changeit` password observation was right; the store identity and the "pin" mechanism are
corrected (§14 pointers on [B392], [B126 §126.4], [B482-G1 closed by B489]).
- Integrity brake: the system `cacerts` is TPK-signed via `cacerts.sig` (`SHA256withRSA`) — an attacker cannot
  re-sign it without the TPK **private** key (only the public pin is embedded) — [B482 §482.2].
- The signer CHAIN from [B392] still holds: `Niagara4Modules Code Signing → Honeywell CodeSign RSA CA →
  Honeywell Product PKI RSA`. What changed is the ANCHOR/pin identity, not the leaf chain.

## §492.3 — Security posture (ties [B490]) `[CERT]`

- **Revocation is OFF** (hardcoded `setRevocationEnabled(false)`) — a revoked-but-unexpired signer stays trusted
  (SEC-19, [B490]).
- **The TPK pin is relaxable** via `--trusted-certificates` / a verify-flag / `moduleVerificationMode=low`
  (SEC-20). With `low` (live) unsigned/non-TPK modules load; with `developer{skipModuleValidation}` (live demo
  license) chain validation is disabled entirely (the B75 kill-chain).
- Data is not signed (only code/delivery) — no signed audit of a signing-config change ([B393], SEC-21).

## §492.4 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Module sig = RSA-2048 detached `.sig` + X.509 JAR signing | `[CERT]` | [B126 §126.1]; [B482 §482.1] | PASS |
| 2 | Java verifier: PKIX vs cacerts (system+user) + TPK leaf-pin, revocation off | `[CERT]` | [B482 §482.1-2] | PASS |
| 3 | Native nverify: cacerts P12 (changeit, 99 roots) + TPK memcmp | `[CERT]` | [B489 §489.2] | PASS |
| 4 | Anchor corrected: NOT truststore.jks; cacerts + TPK (two independent reads) | `[CERT]` | [B482]+[B489]; corrects [B392] | PASS |
| 5 | dsfspi RSA nativeVerify PKCS#1 (Mocana); launcher developer/fips gates | `[CERT]` | [B484 §484.2]; [B485] | PASS |
| 6 | Posture: revocation off, TPK relaxable, code-not-data signing | `[CERT]` | [B490] SEC-19/20/21 | PASS |

**Tally:** 6 claims, all `[CERT]` (synthesis of corroborated blocks). No new RE.

## §492.5 — Connections & remaining gaps

- Reopen capstone over [B392]; consolidates [B482]/[B484]/[B485]/[B489]/[B490]. Corrects [B392]/[B126 §126.4].
- **Remaining (requires-execution/blocked, not opened here):** live-station tamper test of a re-signed/foreign
  module against a stock-Tridium anchor ([B392]'s conditional-universality thesis); a genuine non-OEM Tridium
  `baja.jar` to compare the anchor (SP-G4/SP-G6, still blocked — need a stock Tridium install).
