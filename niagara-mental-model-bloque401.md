# Block 401 — SA-G3: the log-IOC harvester — a forensic mode that hunts signature-bypass evidence in the logs that survive `Sys.setAuditor(null)`; live logs are clean (0 IOCs)

> **Focus:** `security-audit` (gap **SA-G3**, the last read-only-investigable gap). Adds a forensic mode to
> `tools/niagara-security-audit.py` (`--scan-logs`) that harvests the [B112] IOCs — the signature/verification
> evidence that persists in console/station logs even after an attacker runs `Sys.setAuditor(null)` to erase
> the in-station audit. Complements the posture audit (prevention) with detection.
>
> **Sources `[CERT-live]`:** [B112] (the IOC set + why the daemon `console.log` survives station-side
> deletion), [B75] (the attack it detects); live scan of 103 console logs under the running install; tool
> `tools/niagara-security-audit.py` (`scan_logs` / `LOG_IOCS`).

---

## 401.1 — The IOC set (`[CERT]` from B112)

The harvester encodes the persistent indicators [B112 §112.2.4] leaves when the [B75] vector fires: `[CERT]`
- **crit** — `**** Module validation has been DISABLED ****` / `module validation is disabled`
  (skipModuleValidation active — the banner [B113 §113.1.1] flagged).
- **high** — `No code signers for entry …` (an unsigned module loaded under `moduleVerificationMode=low`;
  logger `loader`, `ModuleClassLoader.java:73` — a **one-shot** WARNING per classloader, easy to miss live).
- **high** — `program.notSigned` / `is not signed` (an unsigned BProgram executed, `BCode.java:214`).
- **med** — `failed signature validation` / `Invalid signature` / `Self signed signing certificate not
  permitted` / `CERT_PATH_VALIDATION_FAILURE` (rejected/attempted-bad signatures).

## 401.2 — Why logs, not the audit `[CERT]`/`[REMITTANCE]`

[B112 §112.2] is the anchor: `Sys.setAuditor(null)` (`Sys.java:178`) silences the in-station audit without a
permission check, but the **daemon `console.log`** (`{niagara_user_home}/logging/console.*`, on Windows
`C:\ProgramData\...\logging`) is written by the **native `niagarad` process at OS level** — no Baja API
erases it from the compromised JVM. The classloader WARNINGs (§401.1) likewise land in the station log, not
the audit. So these logs are the forensic primary source for a self-cleaning intrusion. The harvester walks a
log tree, matches the IOC set, and reports `severity · IOC · file:line · text`. `[CERT]`

## 401.3 — Live result: clean `[CERT-live]`

Scanning the live install's log tree (103 console/station logs under
`C:\Users\equipo\Niagara4.14\OptimizerSupervisor`, incl. per-station `console_backup_*.txt`):
**0 indicators** — no module-validation-disabled banner, no `No code signers`, no `program.notSigned`, no
signature-failure lines. The signature-bypass posture is **latent, not exercised**: [B398]/[B399] show the
preconditions are present (moduleVerificationMode=low, skipModuleValidation licensed), but the logs show the
vector has **not been used**. A sanity run against an injected `console.log` correctly flags the banner
(crit) and `No code signers` (high), confirming the matcher works. `[CERT-live]`

> Caveat `[INFER]`: the running daemon's live `console.log` was not directly readable during the scan (held
> by `niagarad` / rotated); the 103 files cover station-side and backup consoles. For a real incident
> response, pull the daemon buffer via `/systemlog` and `/getdaemonoutput` ([B112 §112.2]) and feed the
> exported files to `--scan-logs`. The clean result therefore covers the station logs with high confidence
> and the daemon log by extension pending its live export.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Harvester encodes the B112 IOC set with severities | [CERT] | `LOG_IOCS` in tool; [B112 §112.2.4] |
| 2 | IOCs persist in logs that Sys.setAuditor(null) does not touch | [CERT]/[REMITTANCE] | [B112 §112.2], Sys.java:178 |
| 3 | Live scan of 103 logs → 0 IOCs | [CERT-live] | `--scan-logs` run |
| 4 | Matcher verified against an injected IOC (banner+No code signers) | [CERT-live] | sanity run |
| 5 | Daemon live console.log not directly read; use /systemlog export | [INFER] | §401.3 caveat |

**Tally:** 2 [CERT-live], 1 [CERT], 1 mixed, 1 [INFER]. No unmarked central claims.

## Connections
- **Closes SA-G3 → `security-audit` read-only-investigable = 0** (only SA-G2 requires-execution remains).
- **Operationalizes [B112]:** turns its three-tool detection thesis into a runnable harvester.
- Pairs prevention (posture audit, [B398]) with detection (this) — the [B75]/[B112] "tools existed but were
  off/unmonitored" gap, now a one-command check.
- `tools/niagara-security-audit.py` final surface: 16 posture checks + a forensic `--scan-logs` mode.

## Open gaps (`security-audit` — read-only exhausted)
- **SA-G2** — consume the native SecurityDashboard JSON (`BSecurityService.getStationDashboardData`) as a
  live source. `[requires-execution]` — needs a Fox/HTTP session to the station.
- **investigable_open = 0** → focus STOP candidate; SA-G2 is the dynamic/live-session continuation.
