# RESEARCH-STATE — focus `security-audit`

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 401
gaps_closed: 3
known_gaps: 4
investigable_open: 0
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: security-audit
status: stopped
bootstrapped_on: 2026-08-07 (user chose "focus de seguridad + herramienta de auditoría" after the signing-pki arc)
block_prefix: niagara-mental-model-bloqueN.md (global numbering; next free: B398)

## Angle (declared)

The security thread of the corpus is **dispersed** across ~34 blocks (B75 incident, B112 forensics,
B113 signing-hardening, B114 at-rest, B160 config-write-no-auth, B317 pentest, B384 nverify bypass,
B392–B397 signing-pki) with **no consolidated threat model and no operational tool**. This focus:
1. Consolidates the dispersed findings into one **attack-surface / threat model** with a hardening checklist.
2. Produces an **operational audit tool** (`tools/niagara-security-audit.py`) that inspects a `niagara_home`
   on disk + a live station's ports and reports posture (secure vs insecure default) per check.
3. Validates the tool against the **live production supervisor** (`C:\Honeywell\OptimizerSupervisor-N4.14.0.162`,
   running) → `[CERT-live]` posture evidence, client-relevant.

Deliverable emphasis: this is the "qué implementar" half of the user's original ask — a reusable auditor,
not just documentation.

## Live baseline already observed (`[CERT-live]`, 2026-08-07)
- `niagara.moduleVerificationMode=low` — set in `defaults/system.properties` of the running install (INSECURE default, B75).
- `niagara.commandLinePropertyBlacklist` — commented out / disabled (B113 §113.1.3 gap, confirmed live).
- `truststore.jks` opens with `changeit` (B397).
- Station HTTPS 443 + platform 5011 present the default self-signed `ForRecoveryPurposes` cert (B397/B156).
- Platform plaintext 3011 open (127.0.0.1); Fox is TLS-only (4911), no plain 1911.

## Gap backlog

| Priority | Gap | Type | Status |
|---|---|---|---|
| — | **SA-G4** — threat-model narrative (attack trees) | investigable | **CLOSED B399** (4 attack trees T1-T4; B75 kill-chain shown live-present; mitigation map) |
| — | **SA-G1** — automate SEC-15 (module.xml KeyRingPermission scan) | investigable | **CLOSED B400** (17 holders, 11 wildcard, ALL signed → PASS; SEC-15 = downstream amplifier of T1, FAIL gated on wildcard AND unsigned) |
| — | **SA-G3** — log-IOC harvester [B112] | investigable | **CLOSED B401** (--scan-logs mode; live 103 logs = 0 IOCs clean; matcher sanity-verified) |
| 4 | **SA-G2** — consume native SecurityDashboard JSON (BSecurityService.getStationDashboardData) as live source | requires-execution | open |

## Iteration history

| Iter | Block | Gap | Result |
|---|---|---|---|
| 1 | B398 | bootstrap + tool + live audit | DELIVERED tools/niagara-security-audit.py (SEC-01..18); live audit of PRODUCTION supervisor = 13 findings (5 crit/4 high/4 med); confirmed live B316(ACLs)/B18(Webs.license skipModuleValidation)/B397/B75. 4 gaps seeded |
| 2 | B399 | SA-G4 threat model | CLOSED: 4 attack trees (T1 run-code/B75, T2 destroy-evidence, T3 steal-secrets, T4 inject-trust); all B75 preconditions live-present; mitigation map (moduleVerificationMode/ACLs/TLS = kill-chain trunk) |
| 3 | B400 | SA-G1 SEC-15 automation | CLOSED: 17 KeyRingPermission holders (11 wildcard, all signed → PASS); amplifier of T1 not standalone; tool now 16 automated checks |
| 4 | B401 | SA-G3 log-IOC harvester | CLOSED: forensic --scan-logs mode (B112 IOC set); live logs clean (0 IOCs); investigable=0 -> focus STOPPED. Tool final: 16 posture checks + forensic mode |
