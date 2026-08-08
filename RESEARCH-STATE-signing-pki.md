# RESEARCH-STATE — focus `signing-pki`

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 397
gaps_closed: 4
known_gaps: 8
investigable_open: 0
requires_execution_open: 3
blocked_open: 1
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: signing-pki
status: stopped
bootstrapped_on: 2026-08-07 (B392 capstone — reconciliation of the module trust anchor against the live install)
block_prefix: niagara-mental-model-bloqueN.md (global numbering; next free: B393)

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

## Gap backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| — | **SP-G2** — Data-integrity asymmetry: is the `.dist` backup signed/checksummed? audit/history/`.bog`. | investigable | **CLOSED B393** (thesis confirmed: signs code/delivery, not data) |
| — | **SP-G1** — Firmware supply-chain byte-level. | investigable | **CLOSED B394** (3 postures: HMI ECDSA-signed→Honeywell Product PKI; PanelBus raw unsigned; standalone AES-encrypted) |
| — | **SP-G5** — Vendor-cert chaining. | investigable | **CLOSED B395** (all 3 vendor certs incl. Tridium verify against a hidden embedded DSA root in baja.jar; NOT self-signed; corrects B392) |
| — | **SP-G7** — Optional integrity channel for local record. | investigable | **CLOSED B396** (only syslog offload: TLS transport but plaintext record, no per-message sig; resistance not evidence; nothing signs local record) |
| 3 | **SP-G8** — Does the PanelBus/HMI OTA receive path ([B242] `honIrmConfig`) enforce the ECDSA chain, or trust the jar-unpacked image? | requires-execution | open |
| 4 | **SP-G3** — Native `LicenseUtil::isFeaturePresent` is a text match, not a DSA verify [B126 §126.6]; confirm Java `LicenseManager` rejects a bad DSA signature. | requires-execution | **PARTIAL (2026-08-07)** — executed offline: the real verifier (LicenseUtil.verify replica, validated B323/B395) returns VALID on intact Honeywell.license, INVALID on 1-byte signature flip, INVALID on payload change (feature attr). Java-side DSA verify is a real crypto check [CERT], contrasting the native text-match gate [B126 §126.6]. PENDING [CERT-live]: running-station fail-closed on boot with a tampered license (needs the Windows station started + a throwaway station). |
| 5 | **SP-G6** — CRL/revocation enforcement for BACnet/SC + TLS (`BIssuerCertAndCrl` [B287]) — modelled, enforcement [INFER]. | requires-execution | open |
| 6 | **SP-G4** — Reproduce a Tridium-rooted (non-OEM) `baja.jar` chain to settle §392.7 empirically. | blocked (requires-artifact: a stock non-OEM install) | open |

## Iteration history

| Iter | Block | Gap | Result |
|---|---|---|---|
| 1 | B392 | bootstrap + trust-anchor reconciliation | 3 trust domains separated; real RSA chain decoded; B113 corrected; 6 gaps seeded |
| 2 | B393 | SP-G2 data-integrity asymmetry | CLOSED: signs code/delivery (dist RSA-4096/ECDSA, SignedDistFilter only OS/NRE/VM), NOT data (backup/audit/history/.bog unsigned; only per-field GCM password). SP-G7 spawned |
| 3 | B394 | SP-G1 firmware byte-level | CLOSED: 3 integrity postures (HMI ECDSA-signed chain to Honeywell Product PKI; PanelBus raw unsigned flash; standalone AES-encrypted). Asymmetry reaches OT edge. SP-G8 spawned |
| 4 | B395 | SP-G5 vendor-cert chaining | CLOSED (independent crypto verify): all 3 vendor certs signed by hidden embedded DSA-1024 root in baja.jar; Tridium.certificate NOT self-signed; corrects B392 §392.4; dual embedded roots DSA+ECDSA(v2) |
| 5 | B396 | SP-G7 optional integrity channel | CLOSED: syslog offload is the only channel (UDP/TCP/TLS, default TCP), record is plaintext string, no HMAC/RFC5848; tamper-resistance not evidence. investigable=0 → focus STOPPED |
| 6 (§12) | B397 | dynamic-phase live validation | [CERT-live]: changeit+SEJOFA confirmed on RUNNING platform (upgrades B392/B395); SP-G3 verifier proven to reject sig+payload tampering (executed [CERT]); live-boot fail-closed DECLINED on production supervisor (needs throwaway); TLS default cert remitted to B156/158/162 |
