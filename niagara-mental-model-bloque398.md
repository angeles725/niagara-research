# Block 398 — `security-audit` focus (bootstrap): the consolidated Niagara N4 hardening checklist (SEC-01..SEC-18) + an operational auditor (`tools/niagara-security-audit.py`), validated live against the production supervisor — 13 findings (5 critical)

> **Focus:** `security-audit` (BOOTSTRAP + capstone). The corpus security thread was dispersed across ~34
> blocks with no consolidated threat model and no operational tool. This block delivers both: the checklist
> and a read-only auditor, run against the **live production supervisor** for `[CERT-live]` posture evidence.
>
> **Angle:** consolidate B75/B112/B113/B114/B160/B316-B317/B379-B384/B392-B397 into one actionable checklist,
> encode the disk- and port-checkable items into a tool, and report the real posture of a running install.
>
> **Sources:** consolidation of the blocks above `[REMITTANCE]`; live measurement of
> `C:\Honeywell\OptimizerSupervisor-N4.14.0.162` (running service `Niagara`, station PRUEBAS)
> via `keytool`/`openssl`/`icacls`/port probes `[CERT-live]`; tool `tools/niagara-security-audit.py`.

---

## 398.1 — The tool

`tools/niagara-security-audit.py <niagara_home> [--station config.bog] [--host H] [--json]` — READ-ONLY.
It never writes to or restarts the target and reports secret STRUCTURE, not values (SECRETS DISCIPLINE).
Each check maps to a corpus block; output is severity-sorted with observed-vs-secure and a source citation.
Detection strategy per the enumeration: **disk-only** (system.properties, truststore password, licenses,
ACLs, keystore key sizes, `.bog` header) + **live ports** (`s_client` :443/:5011, TCP :3011/:1911/:80).
Registered in `tools/` alongside `niagara-license-tool.py`. `[CERT]`

## 398.2 — The consolidated checklist (SEC-01..SEC-18)

| id | sev | check | secure vs insecure default | block |
|---|---|---|---|---|
| SEC-01 | crit | `moduleVerificationMode` | `high` vs **`low`** (unsigned NETWORK_COMMUNICATION module loads) | B75 |
| SEC-02 | crit | truststore.jks password | custom vs **`changeit`** (anchor injection) | B392/B397 |
| SEC-03 | crit | `security/` filesystem ACLs | Admin/SYSTEM vs **Authenticated Users:Modify** | B316/B113 |
| SEC-04 | crit | TLS cert on :443/:5011 | CA-issued vs **default `ForRecoveryPurposes`** self-signed | B397/B156 |
| SEC-05 | high | `commandLinePropertyBlacklist` covers skip levers | includes skipModuleValidation+ignoreVerificationMode vs **absent** | B113 |
| SEC-06 | high | license attrs relaxing signature | none vs **`developer{skipModuleValidation}` / smDeveloperMode / unreleasedSoftware** | B75/B113 |
| SEC-07 | high | `program.requireSigning` | `true` vs **`false`** (BProgram arbitrary bytecode) | B75 |
| SEC-08 | high | `allowProgramRuntimeExec` | `false` vs `true` (Runtime.exec from programs) | B75 |
| SEC-09 | high | platform plaintext :3011 | sslOnly / loopback vs open on reachable iface | B75/B397 |
| SEC-10 | high | syslog offload to SIEM | enabled+TLS vs **disabled** (only tamper-resistance for unsigned records) | B75/B393/B396 |
| SEC-11 | med | weak keys / FIPS | ≥2048 / FIPS vs **RSA-1024 allowed** | B113/B392 |
| SEC-12 | med | `.bog` at-rest encryption | keyring vs **none** (reversible passwords in clear) | B114 |
| SEC-13 | med | backup passphrase = ZIP master key; backup unsigned | strong+rotated vs weak/reused | B114/B393 |
| SEC-14 | med | Fox/HTTP plaintext :1911/:80 | TLS-only vs open | B397/B134 |
| SEC-15 | med | `KeyRingPermission name="*"` in module.xml | scoped+signed vs wildcard in unsigned module | B114 |
| SEC-16 | med | local data record integrity | (architectural) unsigned — audit via SEC-10 | B393/B396 |
| SEC-17 | med | custom REST config-write without auth (module-specific) | permission-gated vs open (e.g. nmodsreflow) | B160 |
| SEC-18 | med | default daemon/station credentials | changed vs factory (`honeywell`/`webs`, `niagara`/`niagara`) | B75 |

SEC-13/15/17/18 are policy/module-specific or need deeper parsing → reported MANUAL by the tool; the rest are
automated. `[CERT]`/`[INFER]`

## 398.3 — Live posture of the PRODUCTION supervisor (`[CERT-live]`, 2026-08-07)

Running `niagara-security-audit.py` against `C:\Honeywell\OptimizerSupervisor-N4.14.0.162` (+ PRUEBAS
config.bog, host 127.0.0.1): **13 findings — 5 critical, 4 high, 4 med.** `[CERT-live]`

- **CRIT SEC-01** `niagara.moduleVerificationMode=low` (explicit in `defaults/system.properties`).
- **CRIT SEC-02** truststore.jks opens with `changeit`.
- **CRIT SEC-03** `security/` ACL = `NT AUTHORITY\Authenticated Users:(I)(M)` (Modify) — any authenticated
  user can plant a trust anchor or license. Confirms [B316 L-6] live.
- **CRIT SEC-04** both :443 and :5011 present the default `CN=Niagara4, O=ForRecoveryPurposes` cert.
- **HIGH SEC-05** `commandLinePropertyBlacklist` disabled/absent.
- **HIGH SEC-06** `Webs.license` carries `<feature name="developer" … skipModuleValidation="true"/>` **and**
  `<feature name="smDeveloperMode"/>` (expire 2027-03-31). The license half of the skipModuleValidation
  AND-gate is **already satisfied** — with SEC-05 open, `-Dniagara.classLoader.skipModuleValidation=true` at
  launch would disable all module chain validation. Confirms [B18 §18.3.2] live.
- **HIGH SEC-07** `program.requireSigning` false/default.
- **HIGH SEC-10** syslog offload disabled → the (unsigned) audit/history has no off-box copy.
- **MED SEC-09** :3011 open but loopback-only (mitigated). **MED SEC-12** PRUEBAS config.bog at-rest =
  none (plaintext). **MED SEC-14** HTTP :80 open. **MED SEC-16** data unsigned (architectural).
- **PASS:** SEC-08 (allowProgramRuntimeExec off), SEC-11 (keys ≥2048, no RSA-1024).

The single most dangerous combination live: **SEC-01 (low) + SEC-03 (loose ACLs) + SEC-06 (license enables
skipModuleValidation)** — exactly the preconditions of the [B75] unsigned-module-opens-443 incident, present
together on the running supervisor. `[CERT-live]`

## 398.4 — Hardening priority (from the live result) `[INFER]`

1. `moduleVerificationMode=high` (SEC-01) — closes the fail-open module load.
2. Lock `security/` ACLs to Admin/SYSTEM (SEC-03) — removes the plant-a-file path.
3. Replace the default TLS cert on :443/:5011 (SEC-04).
4. Add skipModuleValidation + ignoreVerificationMode to the CLI blacklist (SEC-05); treat the license's
   developer feature (SEC-06) as a standing risk that only SEC-05 can contain.
5. Enable syslog offload with TLS (SEC-10) — the only tamper-resistance for the unsigned record.
`[INFER]` (priority is derived from severity × exploit-chain proximity, not a vendor ranking).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Read-only auditor encodes SEC-01..18; disk + live-port checks | [CERT] | `tools/niagara-security-audit.py` |
| 2 | Live supervisor: 13 findings (5 crit/4 high/4 med) | [CERT-live] | tool run vs C:\Honeywell\... |
| 3 | moduleVerificationMode=low set explicitly | [CERT-live] | defaults/system.properties |
| 4 | security/ ACL grants Authenticated Users Modify | [CERT-live] | `icacls` output `(I)(M)` |
| 5 | Webs.license has developer{skipModuleValidation=true} + smDeveloperMode | [CERT-live] | rg of Webs.license (confirms B18 §18.3.2) |
| 6 | :443/:5011 default ForRecoveryPurposes cert; :80 open; :3011 loopback | [CERT-live] | openssl s_client + port probes |
| 7 | truststore opens with changeit | [CERT-live] | keytool -storepass changeit |
| 8 | The B75 incident preconditions (SEC-01+03+06) co-occur live | [CERT-live]/[INFER] | §398.3 |

**Tally:** 6 [CERT-live], 1 [CERT], 1 mixed. No unmarked central claims.

## Connections
- **Bootstraps `security-audit`**; consolidates the dispersed thread and turns it into a tool.
- **Confirms live:** [B316 L-6] (ACLs), [B18 §18.3.2] (Webs.license skipModuleValidation), [B397] (changeit,
  default cert), [B75] (moduleVerificationMode=low + incident preconditions).
- **Remits** each check's derivation to its source block (table §398.2).
- The tool is the "implementable" deliverable of the user's original ask.

## Open gaps (`security-audit` backlog)
- **SA-G1** — automate SEC-15 (scan `modules/*.jar` → `module.xml` for `KeyRingPermission name="*"`, cross with signature state). `[investigable]`
- **SA-G2** — consume the native **SecurityDashboard** JSON (`BSecurityService.getStationDashboardData`) as a live data source instead of recomputing SEC-01/06. `[requires-execution]`
- **SA-G3** — log-IOC harvester (module-validation-disabled banner, `program.notSigned`, daemon `console.log`) for post-incident use [B112]. `[investigable]`
- **SA-G4** — the full threat-model narrative block (attack trees from the checklist) for a client-facing report. `[investigable]`
