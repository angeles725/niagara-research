# Block 400 — SA-G1: SEC-15 automated — the 17 KeyRingPermission holders enumerated, 11 hold `name="*"`, ALL signed; the wildcard is a downstream amplifier of T1, not a standalone flaw

> **Focus:** `security-audit` (gap **SA-G1**). Automates SEC-15 (from [B398]): scan every module's
> `META-INF/module.xml` for `KeyRingPermission`, extract its `name`/target, and cross with the module's
> signature state. Grounds [B114 §114.3.4]'s "17 modules" count with the exact list + wildcard + signature
> breakdown, and places SEC-15 correctly in the [B399] threat model.
>
> **Sources `[CERT-live]`:** zipfile scan of the 973 jars in
> `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\modules`; tool `tools/niagara-security-audit.py`
> (`scan_keyring_perms`). **Remittance:** [B114 §114.3.4] (KeyRing model), [B399] (attack trees T1/T3).

---

## 400.1 — The enumeration (`[CERT-live]`)

Of 973 module jars, **17** declare a `KeyRingPermission` in `META-INF/module.xml`. All 17 are **signed**.
Split by target: `[CERT-live]`

- **`name="*"` (whole secret store) — 11, all signed:** `baja`, `backup-rt`, `file-rt`, `fox-rt`,
  `rdb-rt`, `azureUtils-rt`, `cloudIotHubDep-rt`, `abstractMqttDriver-rt`, `platDaemon-wb`,
  `portalApi-wb`, `provisioningNiagara-wb`. These are core/framework + cloud-connector + backup/provisioning
  modules that legitimately need broad keyring access (backup exports the KeyRing; cloud drivers store their
  own creds; rdb/fox hold connection secrets).
- **Scoped names — 6, all signed (good practice):** `awsUtils-rt` (`aws.*`), `jetty-rt` (`web`),
  `orion-rt` (`entsec`, `intrusionSmartKey`), `platform-rt` (`com.tridium.syslog.clientPassword`),
  `rdbMySQL-rt` (`rdbMySQL.trustStore`), `rdbOracle-rt` (`rdbOracle.trustStore`).

## 400.2 — Verdict: PASS, because the risk is conditional on T1 `[CERT-live]`/`[INFER]`

SEC-15's danger is a wildcard `KeyRingPermission` in an **unsigned / attacker-controlled** module — that
grants total read of the secret store. On this install **there are zero unsigned wildcard holders** → the
tool reports **PASS**. The 11 legitimate wildcard holders are all signed core modules. `[CERT-live]`

The correct placement in the threat model: SEC-15 is **not a standalone flaw here** — it is a **downstream
amplifier of attack tree T1** ([B399]). The exposure is: *if* an attacker can load an unsigned module
(SEC-01 `low` and/or SEC-06 license `skipModuleValidation`, both **live-present** per [B398]), then that
module simply declares `<permission class="…KeyRingPermission" name="*"/>` and reads every secret. So the
control that matters is still T1's root (module verification), not the permission itself. Auditing SEC-15 in
isolation would mislabel 11 legitimate signed modules as risk; the tool avoids that by gating the FAIL on
`wildcard AND unsigned`. `[INFER]`

## 400.3 — The automation (`[CERT]`)

`scan_keyring_perms(moddir)` (in `tools/niagara-security-audit.py`) reads each jar via `zipfile`, pulls
`META-INF/module.xml`, regex-extracts `KeyRingPermission … name/target="…"`, and marks a jar signed if it
carries a `META-INF/*.{RSA,DSA,EC}` block. It returns (all holders, wildcard holders, wildcard-AND-unsigned)
and the SEC-15 check FAILs only on the last set. The full scan of 973 jars completes in a few seconds. The
auditor now runs **16 automated checks**; SEC-15 is PASS on the live supervisor. `[CERT]`

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 17 module jars declare KeyRingPermission; all signed | [CERT-live] | zipfile scan of modules/ |
| 2 | 11 hold name="*" (listed); 6 scoped (listed) | [CERT-live] | scan output §400.1 |
| 3 | 0 unsigned wildcard holders → SEC-15 PASS live | [CERT-live] | tool run |
| 4 | SEC-15 is a downstream amplifier of T1, not standalone | [INFER] | §400.2 + [B399] |
| 5 | scan_keyring_perms gates FAIL on wildcard AND unsigned | [CERT] | tool source |

**Tally:** 3 [CERT-live], 1 [CERT], 1 [INFER]. No unmarked central claims.

## Connections
- **Closes SA-G1.** Grounds [B114 §114.3.4] with the exact 17-module list + wildcard/signature split.
- **Refines [B399]:** SEC-15 belongs under T1/T3 as an amplifier; the FAIL condition is wildcard∧unsigned.
- Extends `tools/niagara-security-audit.py` to 16 automated checks.

## Open gaps (`security-audit` backlog)
- **SA-G3** (NEXT) — log-IOC harvester (module-validation-disabled banner, `program.notSigned`, daemon `console.log`) [B112]. `[investigable]`
- **SA-G2** — consume SecurityDashboard JSON as a live source. `[requires-execution]`
