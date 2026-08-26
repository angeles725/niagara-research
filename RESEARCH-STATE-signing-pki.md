# RESEARCH-STATE — focus `signing-pki`

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 530
gaps_closed: 11
known_gaps: 14
investigable_open: 0
requires_execution_open: 0
blocked_open: 3
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: signing-pki
status: stopped
bootstrapped_on: 2026-08-07 (B392 capstone — reconciliation of the module trust anchor against the live install)
block_prefix: niagara-mental-model-bloqueN.md (global numbering; next free: B441)

## Scope

The signature / PKI surface of Niagara N4 as a subsystem. The signature thread was previously **dispersed**
across the corpus with no dedicated focus: module code-signing [B18][B75][B113][B126][B321][B384], native
verifier [B321][B384], licensing [B2][B126], Part 11 e-signature [B350–B356], jsonToolkit inbound-trust
[B335–B349], BACnet/SC certs [B287], firmware [B243]. B392 bootstraps the focus and delivers the capstone:
the three trust domains, the real Honeywell-rooted RSA module chain, and the corrected universality answer.

## Coverage

- **B392** (capstone/bootstrap) — three distinct trust domains (module-RSA-X.509 / license-DSA / TLS-PKI);
  decoded module chain `Niagara4Modules Code Signing → Honeywell CodeSign RSA CA → Honeywell Product PKI RSA`;
  `truststore.jks` password `changeit` + single self-signed SEJOFA dev anchor; `.certificate` = DSA-1024 XML
  wrapper (Tridium root 2003, Sun default params, never-expires); `System.exit(-6)` on failed required
  verification; corrects [B113] SEJOFA/".bks"; corrected answer to "any N4 accepts Tridium modules".
- **B441** (SP-G9 settled + [B440] §14 correction) — the provider-registration mechanism is neither a
  runtime `insertProviderAt(1)` nor a trailing `addProvider()`: `njre` launches the JVM with
  `-Djava.security.properties==bin/policy/java.security` (**double-`==` full override**, semantics per the
  install's own master file), and that effective file lists `provider.1=BCFKSWrap`,
  `provider.2=BouncyCastleFipsProvider`, `provider.3=Sun` — **BC ahead of Sun by static config**. Corrects
  [B440] claims 6/7/8 (it read the overridden stock JRE file). Verdict split: provider-priority is ENFORCED
  (shipped/declarative); approved-only STRICT mode is NOT enabled (`-Dorg.bouncycastle.fips.approved_only`
  absent everywhere) → FIPS-certified provider is primary but runs in general mode. Reconciles [B26] (shipped
  baseline, already had this) vs [B30] (strict-FIPS migration delta). Spawns SP-G9a/SP-G9b.
- **B440** (crypto-provider reconciliation) — BouncyCastle (general JCE/JSSE) vs Mocana/DSF (narrow
  native, [B425]) are two axes, not rivals. `njre` swaps `bcstd`↔`bcfips` by the `fips140-2` license
  feature ([B380] §380.2): PRESENT→bcstd, ABSENT→bcfips (`bcfips` is the forced default). This install
  has NO `fips140-2` feature in any of the 3 licenses → **runs `bcfips`** (corrects an in-session
  inference that read it as `bcstd`). JRE `java.security` is stock (BC registered dynamically at boot,
  confirms [B17]); B26/B30 FIPS provider files are a hardening template, not the shipped default. Spawns
  SP-G9 (registration priority: `insertProviderAt(1)` vs trailing `addProvider`).

## Gap backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| — | **SP-G2** — Data-integrity asymmetry: is the `.dist` backup signed/checksummed? audit/history/`.bog`. | investigable | **CLOSED B393** (thesis confirmed: signs code/delivery, not data) |
| — | **SP-G1** — Firmware supply-chain byte-level. | investigable | **CLOSED B394** (3 postures: HMI ECDSA-signed→Honeywell Product PKI; PanelBus raw unsigned; standalone AES-encrypted) |
| — | **SP-G5** — Vendor-cert chaining. | investigable | **CLOSED B395** (all 3 vendor certs incl. Tridium verify against a hidden embedded DSA root in baja.jar; NOT self-signed; corrects B392) |
| — | **SP-G7** — Optional integrity channel for local record. | investigable | **CLOSED B396** (only syslog offload: TLS transport but plaintext record, no per-message sig; resistance not evidence; nothing signs local record) |
| 3 | **SP-G8** — Does the PanelBus/HMI OTA receive path ([B242] `honIrmConfig`) enforce the ECDSA chain, or trust the jar-unpacked image? | investigable | **CLOSED B530** (DISK-FIRST static): the OTA receive path is `honeywellDeviceManager` (not honIrmConfig) — `.fw` ZIP unpacked, header `CRC` computed but NEVER validated (`crcValid=true` hardcoded, 3 decompilers agree), zero signature/gate. Receiver trusts the unpacked image. |
| — | **SP-G3** — Native `LicenseUtil::isFeaturePresent` is a text match, not a DSA verify [B126 §126.6]; confirm Java `LicenseManager` rejects a bad DSA signature. | requires-execution | **CLOSED B518** (§12 live, isolated test host, reversible byte-identical): real `nre` runtime rejects a 1-byte signature flip fail-closed `{invalid: Invalid signature}` [CERT-live]; HostId gate isolated (valid-sig/wrong-host license → `moved file`, features withheld) [CERT-live]; native half re-anchored read-only by peer session — `isFeaturePresent` text-scan invariant to any signature-region tamper → **asymmetry confirmed both sides on the same file**. Spawns SP-G3a. |
| 3 | **SP-G3a** — Does a *required-but-missing* feature force station `System.exit(-3/-6)` at full boot ([B488]) vs graceful feature-withholding? SP-G3 proved verifier rejection, not the process-exit path. | blocked (requires-artifact: isolated station/VM) | **RE-TYPED B519** — read-first showed the live host is the operator's WORKING supervisor (11 station configs incl. customer-named + a live station on :443); a blind second-station boot risks port collision/collateral. Needs a truly isolated station/VM, not this shared host. |
| 4 | **SP-G10** — Live in-process interposition ("mirror") PoC: can a shim / rogue JCE provider make the license or module verifier return "valid"? Feasibility ∝ `moduleVerificationMode` (=low live, B519). | requires-execution | **CLOSED B524** (operator-authorized Frida run): license DSA verify is **BC-FIPS Java-side** (§14-corrects B520 §1 — dsfspi DSA unused on this bcfips install); module verify **flipped in-process either way** (`checkFileSignature` force-valid → 0 FATAL; force-invalid → `FATAL failed signature check` abort). Java-layer license mirror **blocked-on-tool** (no Java bridge on this host's bare-bone agent) → spawned **SP-G10a**. |
| 5 | **SP-G6** — CRL/revocation enforcement for BACnet/SC + TLS (`BIssuerCertAndCrl` [B287]) — modelled, enforcement [INFER]. | investigable | **CLOSED B531** — BACnet/SC peer-cert path builds PKIX with `setRevocationEnabled(false)` (all 3 decompilers); CRL infra exists but revocation NOT enforced. TLS/module path parity ([B482]). Upgrades B287 [INFER]→[CERT]. |
| 6 | **SP-G4** — Reproduce a Tridium-rooted (non-OEM) `baja.jar` chain to settle §392.7 empirically. | blocked (requires-artifact: a stock non-OEM install) | open |
| — | **SP-G9** — Does daemon boot `insertProviderAt(1)` or trailing `addProvider()`? enforced vs shipped. | requires-execution / code-read | **CLOSED B441** (neither: static override `-Djava.security.properties==bin/policy/java.security`, BC at provider.1/2 ahead of Sun; priority ENFORCED, approved-only strict NOT enabled; corrects B440 6/7/8) |
| 4 | **SP-G9a** — Live `Security.getProviders()` on the running station to confirm the effective order matches `bin/policy/java.security` (upgrade §441.4 to [CERT-live]). | requires-execution | **CLOSED B529** — live order: `BC 1.7801` (bcstd general) first, then SUN…; `DSA`/`SHA1withDSA`/`SHA256withDSA` → `org.bouncycastle.jce.provider.BouncyCastleProvider`, NOT FIPS. §14-refines B441/B524 (declared policy ≠ effective order). |
| 6 | **SP-G9b** — Licensed-`bcstd` branch names `provider.2=BouncyCastleFipsProvider`, a class absent from standard BC. Second policy variant, or daemon rewrites the line when `fips140-2` present? | blocked (requires-artifact: a `fips140-2`-licensed install) | open (B441) |
| 4 | **SP-G10a** — License-side mirror runtime confirm: re-run the Java-layer hook (`LicenseUtil.verify`/`Signature.verify` → force true) with a FULL frida agent (Java bridge) on a disposable `nre.exe`. Static Java path is pinned (B524 F1); the runtime forcing is the remaining unproven half. | requires-execution | **CLOSED B528** (Frida-independent: `nre -@javaagent` + ASM rewrote the 6 `LicenseUtil.verify` overloads to `return true`; tampered `Webs.license` flipped `{invalid}` → `{valid}`; reversible, byte-identical restore, PIDs unchanged). Retracts the blocked-on-tool verdict — the wall was tool-choice, not capability. |

## Blocked gaps

- SP-G3a — needs: a truly isolated station/VM (live host is the operator's working supervisor); tried: read-first on the live host → 11 station configs + live station on :443, so a blind second-station boot risks port collision/collateral (B519) — see B519
- SP-G4 — needs: a stock non-OEM (Tridium-rooted) install to reproduce the chain empirically; tried: on-disk chain decode (B392/B395) proves the OEM anchor but not the non-OEM ancestor — see B395
- SP-G9b — needs: a `fips140-2`-licensed install (bcstd branch policy variant); tried: grep of this install's 3 licenses → no `fips140-2` feature string → bcfips only — see B441/B440

## Iteration history

| Iter | Block | Gap | Result |
|---|---|---|---|
| 1 | B392 | bootstrap + trust-anchor reconciliation | 3 trust domains separated; real RSA chain decoded; B113 corrected; 6 gaps seeded |
| 2 | B393 | SP-G2 data-integrity asymmetry | CLOSED: signs code/delivery (dist RSA-4096/ECDSA, SignedDistFilter only OS/NRE/VM), NOT data (backup/audit/history/.bog unsigned; only per-field GCM password). SP-G7 spawned |
| 3 | B394 | SP-G1 firmware byte-level | CLOSED: 3 integrity postures (HMI ECDSA-signed chain to Honeywell Product PKI; PanelBus raw unsigned flash; standalone AES-encrypted). Asymmetry reaches OT edge. SP-G8 spawned |
| 4 | B395 | SP-G5 vendor-cert chaining | CLOSED (independent crypto verify): all 3 vendor certs signed by hidden embedded DSA-1024 root in baja.jar; Tridium.certificate NOT self-signed; corrects B392 §392.4; dual embedded roots DSA+ECDSA(v2) |
| 5 | B396 | SP-G7 optional integrity channel | CLOSED: syslog offload is the only channel (UDP/TCP/TLS, default TCP), record is plaintext string, no HMAC/RFC5848; tamper-resistance not evidence. investigable=0 → focus STOPPED |
| 6 (§12) | B397 | dynamic-phase live validation | [CERT-live]: changeit+SEJOFA confirmed on RUNNING platform (upgrades B392/B395); SP-G3 verifier proven to reject sig+payload tampering (executed [CERT]); live-boot fail-closed DECLINED on production supervisor (needs throwaway); TLS default cert remitted to B156/158/162 |
| 7 | B440 | crypto-provider reconciliation | BC (JCE/JSSE) vs Mocana/DSF (native) = two axes not rivals; `njre` swaps bcstd↔bcfips by `fips140-2` feature; install has NO fips140-2 → runs **bcfips** (corrects in-session bcstd inference); java.security stock ⇒ dynamic registration; spawns SP-G9 |
| 8 | B441 | SP-G9 provider-registration | CLOSED: neither runtime call — `njre` `-Djava.security.properties==bin/policy/java.security` (double-`==` full override) puts BC at provider.1/2 ahead of Sun by static config. Priority ENFORCED; approved-only strict NOT enabled (flag absent). §14-corrects B440 6/7/8 (read overridden stock JRE file); reconciles B26 (baseline) vs B30 (strict migration). investigable=0 again → focus STAYS STOPPED. Spawns SP-G9a (live getProviders), SP-G9b (bcstd policy variant, blocked) |
| 9 | B524 | SP-G10 mirror (operator-authorized Frida run) | CLOSED: license DSA verify = BC-FIPS Java-side (dsfspi DSA unused live — §14-corrects B520 §1); module verify flipped in-process either way (`checkFileSignature` force-valid→0 FATAL / force-invalid→`FATAL failed signature check` abort). Zero install mutation; sha256+PID invariants hold. Java bridge absent on this host (blocked-on-tool) → spawns SP-G10a |
| 10 | B525 | document mode §20 — dynamic hardening runbook | CAPTURED: `docs/niagara-signing-hardening-guide.md` consolidates B518–B524's live findings into operator procedures (re-verify commands + H1–H7 actions + toolchain map). No new gap |
| 11 | B526 | dynamic-vs-static consistency audit | VERIFIED: B524 license/module findings agree with static anchors (LicenseUtil.java:172-181 JCE + provider.2=BCFips + bcfips branch); §14 backlink restored on B520; 3 SOURCES.md fabricated-cites fixed (verify-sources exit 0); verify-block B518–B525 clean |
| 12 | B527 | SP-G10 session ledger | RECORDED: 10 instruments (8 ran / 1 partial / 1 blocked-on-tool), verdict provenance (target process FATAL line + static code + sha256/PID invariants), unfinished license-side half = SP-G10a |
| 13 | B528 | SP-G10a license mirror (Frida-independent) | CLOSED: `nre -@javaagent` + ASM (COMPUTE_FRAMES) rewrote the 6 `LicenseUtil.verify` → `return true`; tampered `Webs.license` flipped `{invalid}`→`{valid}`; reversible, PIDs unchanged. Retracts the blocked-on-tool verdict |
| 14 | B529 | SP-G9a live provider order | CLOSED: `BC 1.7801` (bcstd general) first; `DSA`/`SHA1withDSA` → BouncyCastleProvider, NOT FIPS. §14-refines B441/B524 |
| 15 | B530 | SP-G8 OTA receive path | CLOSED (DISK-FIRST): `.fw` ZIP trusted as-unpacked; header CRC never validated (`crcValid=true` hardcoded, 3 decompilers) |
| 16 | B531 | SP-G6 CRL/revocation | CLOSED: BACnet/SC PKIX `setRevocationEnabled(false)`; CRL infra modelled, revocation NOT enforced (upgrades B287 [INFER]→[CERT]); TLS parity [B482] |
| 17 | B532 | licensing watch-map (synthesis) | MAP: 2 long-lived processes + boot-only gates (no node-locked watcher); `Nre.postInit` call chain; 3 interposition points (license ✓B528, module ✓B524, HostId mapped); defensive read H4/H6 + HostId inputs |
| 18 | B533 | persistent mirror | MAP: `station.java.options` (nre.properties:46) = always-on vector; agent = B528 + `isLicenseHostIdValid()`; verify via dual oracle + invariants; 2 dynamic follow-ups gated on SP-G3a isolate |
