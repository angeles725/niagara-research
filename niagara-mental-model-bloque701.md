# B701 — JACE_UMBRELLA LogHistory content (HD3): the log is Fox client sessions plus NRIO discovery churn against the down IO-34 — corroborating the module-down state from the config

> Focus: **jace-history-audit** · Gap **HD3** (LogHistory.hdb — the largest history, 55 KB). Source:
> `LogHistory.hdb` (SD P2) via `tools/hdbread.py`. Evidence:
> `sources/probes/B699-jace-history-audit/loghistory-content.txt`. **SECRETS DISCIPLINE:** logName/severity/
> source counts cited; message values masked. Marker `[CERT-hw]` (SD artifact).

## 701.1 — Schema + top log sources

[CERT-hw] `LogHistory.hdb` (55260 B): record type `history:LogRecord`, **5-field schema** (`timestamp, logName,
severity, message, exception`). The dominant `logName` sources:

| logName | count | what it is |
|---|---|---|
| fox | 37 | Fox client sessions (Workbench / tools connecting over foxs:4911) |
| rioNetwork.discovery | 30 | NRIO field-bus device discovery |
| sys.mixin | 20 | framework mixin lifecycle |
| rioNetwork.unsolicited | 12 | NRIO unsolicited-message handling |
| web | 10 | web server |
| backup | 9 | BackupService |
| rioNetwork.wrIo | 3 | NRIO write-IO |

## 701.2 — The NRIO churn corroborates the down module

[CERT-hw]+[INFER] The three `rioNetwork.*` sources (discovery 30 + unsolicited 12 + wrIo 3 = **45 combined
records**) are the single largest theme after Fox. This is the NRIO driver **repeatedly trying to discover and
poll the IO-34 module that [Block 687] §687.2 found "device is down."** The config's static "down" status and
the log's live discovery/poll churn are two independent views of the same fact — the one field module on this
station was not communicating. (Independent corroboration, RE-MEASURE satisfied.)

## 701.3 — Errors: transient Fox connection drops

[CERT-hw] 12 `error` + 8 `Exception` records, the identifiable ones being **`java.io.IOException` on
`com.tridium.fox.sys.BFoxConnection` / `FoxMessage`** — dropped Fox client connections (a client disconnecting
mid-session). These are transient connection errors, not station faults — consistent with a bench unit where
Workbench/tools connect and disconnect repeatedly.

## 701.4 — Verdict: a bench/commissioning log profile

[INFER] The LogHistory profile — Fox client sessions + NRIO retry churn against a down module + a handful of
transient connection IOExceptions, over ~a few hundred records in 55 KB — is that of a **bench / commissioning
unit**, not a controller running a live building. It matches every other axis (seed station, one down IO point,
low-use auth trail HD2). Nothing in the log indicates production field activity (no sustained point I/O, no
alarm storms, no scheduled control).

## Connections

- .hdb format + reader → [Block 699]. IO-34 down (config side) → [Block 687] §687.2. Fox transport → focus
  `jace8000` [Block 471]/[Block 474]. Low-use auth trail → [Block 700] (HD2). Seed-station verdict → [Block 692].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | LogHistory = LogRecord 5-field; sources fox/rioNetwork/sys.mixin/web/backup | [CERT-hw] | hdbread --strings tally | measured |
| 2 | rioNetwork.* churn (45) = driver retrying the down IO-34 | [CERT-hw]+[INFER] | tally + [Block 687] | corroborated |
| 3 | errors = transient Fox IOExceptions (BFoxConnection) | [CERT-hw] | strings | measured |
| 4 | bench/commissioning log profile, not production | [INFER] | 701.1-701.3 + focus | reasoned |

**Tally:** [CERT-hw] ×3 · [INFER] ×1. Ratio 0.33. Block TYPE = **EVIDENCE**. No secret value; message bodies
masked. Source counts measured via the reader.

## Open gaps (this focus)

HD3 CLOSED. Next: **HD4** (alarm.adb — the alarm-database format + what alarms fired). Then HD5 (the 3
provisioning .hdb + focus synthesis).
