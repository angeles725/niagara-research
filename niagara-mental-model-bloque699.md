# B699 — Niagara .hdb format on the JACE-8000 SD + a read-only reader (HD1): history records are cleartext; AuditHistory holds the seed station's commissioning trail

> Focus: **jace-history-audit** · Gap **HD1** (the .hdb format on this unit + a lightweight reader). Sources:
> `AuditHistory.hdb` extracted READ-ONLY from SD P2 + `tools/hdbread.py` (built this iteration, §19). Evidence:
> `sources/probes/B699-jace-history-audit/hdb-format-audit.txt`. **SECRETS DISCIPLINE:** operation types +
> counts cited; specific values/identities masked (the reader has a `--mask` mode). Marker `[CERT-hw]` (SD
> artifact). The history-persistence MODEL (BHistoryService, record types) = REMITTANCE focus `database`
> [Block 402]–[Block 413]; HD1 documents the ON-DISK bytes of THESE files + parses them.

## 699.1 — The .hdb on-disk format (this unit)

[CERT-hw] A Niagara `.hdb` history file on the JACE is:

| offset | bytes | meaning |
|---|---|---|
| 0 | `A1 06 F1 1E` | magic |
| 4 | uint32 BE | version (observed **2**) |
| 8 | uint32 BE | config-XML length |
| 12 | `<config len>` | embedded **HistoryConfig XML** (the schema) |
| 12+len | rest | packed **cleartext records** + index metadata |

The embedded XML is a standard BOG `h:HistoryConfig` with **`reversibleEncodingKeySource="none"`** — so **history
records are NOT encrypted**. The schema string (`n="schema"`) lists the record fields as `name,type;…`.

## 699.2 — tools/hdbread.py (§19 build)

[CERT-hw] Built a minimal READ-ONLY reader `tools/hdbread.py`: parses the header, extracts the schema
(history id, record type, fields), and does a best-effort cleartext record walk (`--ops` tallies audit
operations, `--strings --mask` dumps redacted field runs). It never mutates the file. Exact per-record binary
framing (the index table at the region head) is a refinement left to HD2/HD5; the schema + cleartext field walk
is sufficient to answer the content gaps. Registered in `tools/README.md`.

## 699.3 — AuditHistory content: the commissioning trail

[CERT-hw] `AuditHistory.hdb` (10072 B): record type `history:AuditRecord`, **7-field schema** — `timestamp,
operation, target, slotName, oldValue, value, userName`. It holds **30 audit records**: **18 Added · 5 Changed ·
7 Removed** (0 Renamed). These are the config edits made while the station was built: adding `/Drivers`,
`NrioNetwork`, points, and changing/removing slots — all by `admin` (role account).

Concrete cross-check: the audited `portName` slot appears as both **COM2** and **COM1** (4 portName edits),
while the LIVE config settled on **COM1** ([Block 687] §687.1). So the audit trail captured the back-and-forth
of commissioning the one NRIO network — direct evidence that `JACE_UMBRELLA` is a station that was *configured*
(not factory-blank) but only lightly, consistent with the seed-station verdict (focus `jace-station-config`
[Block 692]).

## 699.4 — Security note (reinforces B698)

[CERT-hw]+[INFER] Because `reversibleEncodingKeySource="none"`, the entire audit/history trail is **cleartext on
the card** — no key needed at all (unlike the config.bog reversible fields, which at least need `.km`). This
sharpens the DAR6 verdict ([Block 698]): the audit record of *who changed what, when* is the most trivially
recoverable data on the SD, and (per [Block 689]) there is no off-box replica to detect its tampering or
deletion. Whoever holds the card can read — or silently rewrite — the controller's entire audit history.

## Connections

- .hdb persistence model → focus `database` [Block 402]. Deployed history config (schemas, no off-box archive)
  → [Block 689]. NRIO portName=COM1 live → [Block 687]. Seed-station verdict → [Block 692]. Cleartext-at-rest
  data exposure → [Block 698] (DAR6). Reader tool → `tools/hdbread.py`.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | .hdb = magic A106F11E + BE version + len-prefixed HistoryConfig XML + cleartext records | [CERT-hw] | header parse | measured |
| 2 | records unencrypted (reversibleEncodingKeySource=none) | [CERT-hw] | embedded XML | grep-confirmed |
| 3 | AuditHistory = AuditRecord 7-field schema, 30 records (18A/5C/7R) | [CERT-hw] | hdbread --ops | measured |
| 4 | commissioning trail: portName COM2↔COM1, live=COM1 | [CERT-hw] | hdbread --strings + [Block 687] | cross-confirmed |
| 5 | full audit trail cleartext on card (reinforces B698) | [CERT-hw]+[INFER] | 699.1 + [Block 698] | reasoned |

**Tally:** [CERT-hw] ×4 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE** (+§19 tool). No secret value in block
or evidence file (reader `--mask` used). 4/4 structural claims measured.

## Open gaps (this focus)

HD1 CLOSED (format + reader). Next: **HD2** (AuditHistory + SecurityHistory content — the operations/logins
recorded, structure; identities masked), then HD3 (LogHistory 55KB), HD4 (alarm.adb), HD5 (provisioning .hdb +
synthesis).
