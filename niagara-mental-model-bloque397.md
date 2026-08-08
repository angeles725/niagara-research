# Block 397 — §12 dynamic-phase validation of the signing-pki findings against the LIVE platform: changeit+SEJOFA confirmed live, the license verifier proven to reject tampering (executed), and why the destructive boot-test was declined on the production supervisor

> **Focus:** `signing-pki` (dynamic phase §12). The user requested live validation of the read-only
> findings (B392–B396). This block records the §12 pass: what was confirmed against the RUNNING system
> (`[CERT-live]`), what was proven by executing the real verifier (`[CERT]`), and the production-safety
> decision that bounded it.
>
> **Environment `[CERT-live]`:** Windows host, WSL interop. Service `Niagara` (Running) →
> `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\niagarad.exe`. Listening: platform 3011 (plain,
> 127.0.0.1) + 5011 (TLS), station 4911 (Fox TLS) + 443 (HTTPS) — PIDs niagarad 6520 / station 24740, test
> station PRUEBAS. **SECRETS DISCIPLINE:** structure/public certs only.

---

## 397.1 — Live confirmation of the module trust anchor (upgrades B392/B395 to `[CERT-live]`) `[CERT-live]`

The RUNNING platform's `security/truststore.jks` opens with the Java default password **`changeit`** and holds
the **single** entry `niagaramoduledev` — the self-signed RSA-2048 SEJOFA dev anchor, SHA-256
`83:7B:38:E8:AF:D4:F4:01:C4:82:86:CA:63:DD:E3:1E:ED:6D:37:40:7D:F7:AB:F1:15:53:EB:DA:42:4F:41:CD` —
byte-identical to the offline copy analyzed in [B392 §392.3]. The three vendor `.certificate` files
(Tridium/Honeywell/HoneywellCentraLine) are present in `security/certificates/` on the live install. So the
[B392]/[B395] trust-anchor findings are confirmed against the running production platform, not just a copy.
`[CERT-live]`

## 397.2 — The license verifier rejects tampering — executed (SP-G3 core) `[CERT]`

Running the real verification pipeline (a faithful replica of `LicenseUtil.verify` +
`CertificateFile/LicenseFile.load`, validated byte-for-byte in [B323]/[B395]) over the actual
`Honeywell.license` against `Honeywell.certificate`'s public key: `[CERT]`

| Input | Result |
|---|---|
| Intact `Honeywell.license` | **VALID** |
| Signature: 1 byte flipped | **INVALID** |
| Payload: a `<feature>` attribute changed (`ascBAC`→`ascBAC9`) | **INVALID** |

This proves the **Java-side** license/cert check is a genuine DSA cryptographic verification that rejects
both signature and payload tampering — in direct contrast to the **native** `LicenseUtil::isFeaturePresent`
gate, which [B126 §126.6] showed is only a text match. The security-relevant conclusion: **presence-checking
in native code is not signature verification; the real gate is the Java load-time verify.** `[CERT]`

## 397.3 — TLS posture, live (remittance) `[CERT-live]`

Station HTTPS (443) and platform daemon TLS (5011) both present the default self-signed
`CN=Niagara4, O=ForRecoveryPurposes, C=US` certificate (valid 2025-09-17 → 2026-09-17) — the factory
recovery cert, never replaced. This is [B392]'s Domain C observed live; the default-cert posture itself is
already documented by the live-station focus ([B156]/[B158]/[B162]) — cited, not re-derived. Management-plane
TLS therefore has no real trust chain (MITM-able on the management LAN). `[CERT-live]`/`[REMITTANCE]`

## 397.4 — What was NOT done, and why (production-safety decision) `[CERT-live]`

The full `[CERT-live]` closure of SP-G3 — a running platform **failing closed on boot** with a
signature-tampered license/cert — was **declined**. The running `niagara_home` is
`C:\Honeywell\OptimizerSupervisor-N4.14.0.162`: PRUEBAS is a test *station*, but it runs on the **production
Honeywell supervisor platform** (licensed to host `Win-6E6E`). Licenses/certs are platform-scoped, so
tampering `security/` + restarting would take the whole platform down and risk breaking the client's
licensed supervisor if restore failed — a blast radius not justified when §397.2 already proves the verifier
cryptographically. The live-boot test is deferred to a genuine throwaway install (SP-G3 remains
`requires-execution` for that last step only). `[CERT-live]` (the niagara_home identity is live-observed).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Live platform truststore opens with `changeit`, single SEJOFA entry, same SHA-256 as offline | [CERT-live] | keytool on `C:\Honeywell\...\security\truststore.jks` |
| 2 | The 3 vendor certs are present on the live install | [CERT-live] | ls of live `security/certificates/` |
| 3 | Real verifier: intact=VALID, sig-flip=INVALID, payload-change=INVALID | [CERT] | executed replica (validated B323/B395) over Honeywell.license |
| 4 | Java license verify is real crypto vs native text-match gate | [CERT] | §397.2 + [B126 §126.6] |
| 5 | Live 443/5011 use default self-signed ForRecoveryPurposes cert | [CERT-live]/[REMITTANCE] | openssl s_client; already in [B156/158/162] |
| 6 | Running niagara_home = production Honeywell supervisor → destructive test declined | [CERT-live] | service ImagePath; platform-scoped security |

**Tally:** 3 [CERT-live], 2 [CERT], 1 mixed. No unmarked claims.

## Connections
- **Upgrades [B392 §392.3]/[B395]** trust-anchor findings to `[CERT-live]` (running platform).
- **Advances SP-G3** from requires-execution to **executed-proven** (`[CERT]`); only the live-boot fail-closed
  step remains, deferred to a throwaway install (documented in RESEARCH-STATE).
- **Remits** the default-TLS-cert posture to [B156]/[B158]/[B162]; native text-match gate to [B126 §126.6].
- Closes the §12 pass the user requested; `signing-pki` stays STOPPED (read-only investigable=0) with the
  live validation appended.

## Open gaps (unchanged, all non-read-only)
- **SP-G3** — live-boot fail-closed test → needs a **throwaway** platform (not the production supervisor). `[requires-execution]`
- **SP-G6** (CRL), **SP-G8** (OTA ECDSA enforcement) — `[requires-execution]` (need BACnet/SC + controller hardware).
- **SP-G4** — Tridium-rooted non-OEM baja.jar — `[blocked: requires-artifact]`.
