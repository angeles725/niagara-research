# Block 399 — SA-G4: the Niagara N4 threat model — four attack trees rooted in the live posture, with the B75 kill-chain shown present on the production supervisor

> **Focus:** `security-audit` (gap **SA-G4**). Turns the [B398] checklist (SEC-01..18) and its live results
> into a **threat model**: assets, adversary classes, four attack trees whose leaves are the SEC-xx
> conditions annotated with their **live status on the production supervisor**, and the mitigation map. This
> is the client-facing synthesis half of the focus.
>
> **Sources `[REMITTANCE]`/`[CERT-live]`:** [B75] (the canonical attack chain), [B398] (live posture, 13
> findings), [B114] (secrets at rest), [B392–B397] (signing/trust), [B160] (config-write). Live status =
> the [B398] run against `C:\Honeywell\OptimizerSupervisor-N4.14.0.162`.

---

## 399.1 — Assets and adversary classes `[INFER]`

**Assets:** (A1) the control logic + config (`.bog`); (A2) credentials & keys (KeyRing `.kr`/`.km`,
integration secrets); (A3) the audit/history record (the evidence); (A4) the **trusted-code perimeter**
(what the station will load and run); (A5) availability of the station itself.

**Adversary classes:** (X1) network-adjacent, unauthenticated on the management LAN; (X2) an authenticated
**read-level** station user; (X3) filesystem/daemon access (local or via the platform daemon); (X4)
supply-chain (a malicious module or firmware image). Each attack tree notes which class it needs.

## 399.2 — Attack tree T1 — run unauthorized code / open network access (the B75 goal) `[CERT-live]`

```
GOAL T1: load code the operator never authorized (e.g. open :443, exfiltrate)
├─ via UNSIGNED module requesting NETWORK_COMMUNICATION
│   ├─ SEC-01 moduleVerificationMode = low ............. LIVE: PRESENT (crit)
│   └─ deploy path: platform daemon creds (X3) or FS write (X3)
│       └─ SEC-03 security/ ACL = Authenticated Users:Modify  LIVE: PRESENT (crit)
├─ via DISABLING chain validation at launch
│   ├─ SEC-06 license developer{skipModuleValidation=true} .. LIVE: PRESENT (high)
│   └─ SEC-05 skipModuleValidation NOT in CLI blacklist ..... LIVE: PRESENT (high)
│       └─ -Dniagara.classLoader.skipModuleValidation=true → all chain validation off
└─ via superuser BProgram (X2→X-priv)
    └─ SEC-07 program.requireSigning = false ................ LIVE: PRESENT (high)
```
**All four enabling conditions of the [B75] incident are present together on the running supervisor.** T1 is
not hypothetical here — the config that made the original 443/malware incident possible is live. `[CERT-live]`

## 399.3 — Attack tree T2 — destroy the evidence `[CERT-live]`

```
GOAL T2: erase/forge the record of the intrusion (after T1 or X2/X3)
├─ audit is structurally unsigned/mutable ................. ARCHITECTURAL (SEC-16, B393/B396)
│   ├─ Sys.setAuditor(null) — no permission check .......... [B75]
│   └─ HistoryDatabaseConnection.deleteHistory — skips Baja check
└─ no off-box copy exists
    └─ SEC-10 syslog offload = disabled .................... LIVE: PRESENT (high)
```
Because the record is unsigned ([B393]) and not shipped off-box ([B396]), an attacker who reaches X2/X3 can
rewrite the audit with no cryptographic trace. The only control (syslog offload) is off live. `[CERT-live]`

## 399.4 — Attack tree T3 — steal secrets `[CERT-live]`

```
GOAL T3: recover credentials / keys
├─ config passwords reversible at rest
│   └─ SEC-12 .bog EncryptionKeySource = none ............. LIVE: PRESENT (med, PRUEBAS)
├─ read the KeyRing broadly
│   └─ SEC-15 KeyRingPermission name="*" in a module ....... (audit per-module, SA-G1)
└─ intercept on the wire
    ├─ SEC-04 default TLS cert → MITM :443/:5011 ........... LIVE: PRESENT (crit)
    └─ SEC-14 HTTP :80 / SEC-09 :3011 plaintext ........... LIVE: :80 open, :3011 loopback
```

## 399.5 — Attack tree T4 — inject trusted code / persist `[CERT-live]`

```
GOAL T4: make malicious code permanently trusted
├─ plant a trust anchor
│   ├─ SEC-02 truststore password = changeit ............... LIVE: PRESENT (crit)
│   └─ SEC-03 security/ writable by Authenticated Users .... LIVE: PRESENT (crit)
│       → add your self-signed cert → your modules validate (the SEJOFA/niagaramoduledev pattern, B392)
└─ supply-chain (X4)
    ├─ module: verify chain to Honeywell Product PKI (RSA) .. [B392]
    └─ firmware: HMI ECDSA-signed, but PanelBus IO UNSIGNED .. [B394] (weakest at the OT edge)
```

## 399.6 — The live kill-chain (what an assessor would write) `[CERT-live]`/`[INFER]`

On the production supervisor, a single actor with management-LAN access can chain: **SEC-04 (MITM the
default TLS) → platform-daemon or FS access → SEC-03 (write to `security/`) → SEC-02 (add a trust anchor) or
SEC-01/06/05 (load an unsigned/validation-disabled module) → T1 code exec → SEC-16/10 (erase the unsigned,
un-offloaded audit).** Every link is a live-PRESENT finding, not a theoretical one. The residual friction is
authentication to the daemon (SEC-18, not measured) — which [B75] flags as lacking lockout/IP-filtering.
`[CERT-live]` on the individual links; `[INFER]` on end-to-end chainability (not detonated — production).

## 399.7 — Mitigation map (severity × chain-proximity) `[INFER]`

| # | Action | Kills / breaks | Effort |
|---|---|---|---|
| 1 | `moduleVerificationMode=high` | T1 main branch | property + restart |
| 2 | Lock `security/` ACL to Admin/SYSTEM | T4 plant-anchor, T1 deploy | icacls |
| 3 | Replace default TLS cert (:443/:5011) | T3 MITM, kill-chain entry | cert install |
| 4 | Blacklist `skipModuleValidation`+`ignoreVerificationMode` | T1 disable-validation branch | property |
| 5 | Enable syslog offload (TLS) | T2 (gives off-box evidence) | station config |
| 6 | Close :80; set daemon `sslOnly` | T3 plaintext | config |
| 7 | Rotate daemon/station creds; `.bog` keyring encryption | T3, SEC-18 | ops |

Items 1–5 each break a currently-live branch; 1–3 are the highest ratio (they sit on the kill-chain trunk).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Four attack trees (T1-T4) map every SEC-xx to an asset/goal | [INFER] | §399.2-5, derived from [B398] checklist |
| 2 | All 4 B75-incident enabling conditions are live-present (SEC-01/03/05/06/07) | [CERT-live] | [B398] §398.3 run |
| 3 | T2: audit unsigned + syslog off → traceless tamper | [CERT-live]/[REMITTANCE] | SEC-10 live off; [B393][B75] |
| 4 | T3: default TLS (SEC-04) + .bog none (SEC-12) + :80 live | [CERT-live] | [B398] |
| 5 | T4: changeit + writable security/ = trust-anchor injection | [CERT-live] | SEC-02+03 live |
| 6 | Mitigations 1-3 sit on the kill-chain trunk | [INFER] | §399.6-7 |

**Tally:** 3 [CERT-live], 1 mixed, 2 [INFER]. Live claims trace to [B398]'s measured run.

## Connections
- **Closes SA-G4.** Consolidates [B398]'s findings into attack trees + a mitigation map.
- **Grounds [B75]** as a live kill-chain, not a past incident — its preconditions co-occur now.
- **Remits** each leaf to its SEC-xx / source block.
- Feeds a client-facing report (offer: render as an Artifact/document from this block).

## Open gaps (`security-audit` backlog)
- **SA-G1** (NEXT) — automate SEC-15 (module.xml KeyRingPermission scan). `[investigable]`
- **SA-G3** — log-IOC harvester [B112]. `[investigable]`
- **SA-G2** — SecurityDashboard JSON as live source. `[requires-execution]`
