# Block 490 — security-audit consolidation: the licensing/trust internals (B478/B481/B482/B487/B489) folded into the SEC checklist — four new items (SEC-19 revocation-off, SEC-20 TPK-pin relaxation, SEC-21 no license-change audit, SEC-22 non-recoverable license failure) and a client-facing threat re-cut

> **Focus:** `security-audit` (consolidation layer — NOT reopening SA-G4, which closed at B399 as attack-trees
> T1–T4). This block re-cuts the client/OEM-facing threat model enriched with this session's licensing-internals
> deep-dive, and proposes four new SEC items. READ-ONLY corpus synthesis; the live audit tool was NOT run.
> Markers §3, cite block§/file:line.

## §490.1 — Delta table: new licensing findings → SEC items `[CERT]`

| Finding (source) | SEC | New/Upd | Sev | Risk |
|---|---|---|---|---|
| **Revocation DISABLED** — `CertificateChainValidator` `setRevocationEnabled(false)`, no CRL/OCSP; native `nverify` same | **SEC-19** | NEW | high | a revoked-but-unexpired signer/CA validates modules & licenses forever; no config enables revocation ([B482 §482.1] `CertificateChainValidator.java:146`; [B489 §489.2]) |
| **TPK-pin relaxation levers** — RSA-2048 TPK memcmp pin relaxable via `--trusted-certificates` + a verify-flag (`byte[0x14007c03d]` default 0=active); `moduleVerificationMode=low` relaxes the chain | **SEC-20** | NEW | high | dropping the Tridium-specific signer pin to stock-CA-only (amplifier of SEC-01) ([B489 §489.2]; [B482 §482.1]) |
| **No tamper-evident audit of license/trust changes + node-locked has NO runtime watcher** | **SEC-21** | NEW | high | a silent license swap or trust-anchor edit is undetected — caught only at next load, never logged/signed/alarmed ([B481 §481.1,§481.5]; [B393]) |
| **License-failure exit `-3`/`-6` NON-recoverable** — station left FAILED, no auto-restart | **SEC-22** | NEW | med | a tampered/expired license or one unsigned required module bricks the station — availability/DoS lever ([B478 §478.2]) |
| **Module store = cacerts P12 (`changeit`, 99 roots) + RSA-2048 TPK pin**, NOT `truststore.jks`; system cacerts pw `jiUz!rHw2d8&i3kI`/`changeit`; `cacerts.sig` TPK-signed (`SHA256withRSA`) | **SEC-02** | UPDATE | crit | corrects the audited target: the `changeit`/`truststore.jks` check is not what gates modules; real store = cacerts + TPK pin; system-cacerts tamper is caught by the TPK-signed `cacerts.sig` (attacker can't re-sign without the TPK private key) ([B482 §482.2]; [B489 §489.4]) |
| KeyRing / AES-256-GCM at-rest envelope | **SEC-12** | UPDATE | med | at-rest crypto is strong when keyring used; failing case narrowed to `EncryptionKeySource=none` (plaintext) ([B482 §482.3]) |
| Niagara signs code, not data → extends to license/trust-config changes | **SEC-16** | UPDATE | med | broadens the unsigned-data gap to license/trust mutations (SEC-16→SEC-21); only syslog offload (SEC-10) gives resistance |
| No covert watcher (Java+native) | — | negative | info | no hidden exfil; reinforces SEC-21; embedded `http://` are DigiCert OCSP/CRL data, not callouts ([B487]) |

**NEW: SEC-19, SEC-20, SEC-21, SEC-22.** SEC-02/12/16 pre-existing (B398 §398.2), corrected/scoped.

## §490.2 — Client-facing threat re-cut (ranked by exploitability) `[CERT/INFER]`

Trust actually gating code = **cacerts P12 (changeit/99 roots) + RSA-2048 TPK pin**, with **revocation off**,
**no license-change audit**, and (live supervisor) **loose `security/` ACLs + a demo `developer{skipModuleValidation}`
license**.

1. **Developer/skipModuleValidation demo-license surface — HIGH (live half-satisfied).** Precondition: license
   `developer{skipModuleValidation}`+`smDeveloperMode` (LIVE `Webs.license`) + sysprop
   `niagara.classLoader.skipModuleValidation=true` + SEC-05 blacklist gap (LIVE). Impact: "Module validation has
   been DISABLED" → any unsigned module loads (B75 kill-chain). Mitigation: blacklist the skip levers (SEC-05);
   remove the developer license; `moduleVerificationMode=high`.
2. **moduleVerificationMode=low / TPK-pin relaxation — HIGH (live present).** `low` (LIVE) or `--trusted-certificates`
   or the verify-flag flipped → unsigned/non-TPK module loads. Mitigation: `high`; never pass `--trusted-certificates`
   in prod; keep the TPK pin default-active; lock CLI (SEC-05).
3. **Silent license swap — HIGH.** Local write to `security/licenses` (loose ACL SEC-03, LIVE) + no watcher/no audit
   (SEC-21) → drop a `developer{skipModuleValidation}` license to satisfy #1; unlogged/unalarmed until next load.
   Mitigation: lock `security/` ACLs; syslog offload (SEC-10); external file-integrity monitoring.
4. **Trust-store tamper — MED-HIGH.** `security/` Modify ACL + known cacerts pw. Brakes: system cacerts is
   TPK-signed (`cacerts.sig`) — cannot re-sign without the TPK private key; user cacerts pw is keyring-bound. So
   the injection path is narrower than a plain `changeit` store implies. Mitigation: lock ACLs; monitor
   `cacerts.sig`; keep FIPS/keyring.
5. **Revocation-disabled → compromised-but-unexpired signer stays trusted — LOW-MED.** `setRevocationEnabled(false)`
   is hardcoded; can't be enabled. Compensate: `moduleVerificationMode=high` + TPK pin active (don't relax) narrows
   acceptance to exactly the TPK signer; monitor cacerts.

## §490.3 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Revocation off (Java+native) → SEC-19 | `[CERT]` | `CertificateChainValidator.java:146`; [B489 §489.2] | PASS |
| 2 | TPK pin relaxable (--trusted-certificates / verify-flag / mode=low) → SEC-20 | `[CERT]`/`[INFER flag]` | [B489 §489.2]; [B482 §482.1] | PASS |
| 3 | No license-change audit + node-locked no watcher → SEC-21 | `[CERT]`/`[CERT neg]` | [B481 §481.1,§481.5] | PASS |
| 4 | Exit -3/-6 non-recoverable → SEC-22 (availability) | `[CERT]` | [B478 §478.2] | PASS |
| 5 | SEC-02 corrected: store = cacerts+TPK, cacerts.sig TPK-signed | `[CERT]` | [B482 §482.2]; [B489 §489.4] | PASS |
| 6 | #1 kill-chain (developer/skip + low + loose ACL) live half-satisfied | `[CERT-live]` | [B398 §398.3] | PASS |

**Tally:** 6 claims, `[CERT]`/`[CERT-live]`/`[CERT neg]`, 1 `[INFER]` sub-part (verify-flag semantics).

## §490.4 — Connections & note

- Consolidation layer over [B398] (SEC-01..18) + SA-G4/B399 (attack-trees); enriched with [B478]/[B481]/[B482]/
  [B487]/[B489]. Feeds `docs/niagara-licensing.md` §9.
- SEC-19..22 are proposed checklist additions; encoding them into `tools/niagara-security-audit.py` is a future
  step (SEC-19/22 are static/architectural; SEC-20/21 are launch-arg/ACL-adjacent). The tool was NOT run here.
- The B75 kill-chain trunk (SEC-01+03+06) is unchanged and remains live-present.
