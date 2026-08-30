# RESEARCH-STATE — focus: jace-history-audit (the operational trace of the JACE-8000 seed station: what its history/audit/alarm stores actually recorded, read from the SD)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** (operator's 3rd SD-track pick). Fuente
> = the `.hdb` history + `.adb` alarm stores of station `JACE_UMBRELLA`, extracted READ-ONLY from SD P2 QNX6 via
> `tools/qnx6read.py`. Artefacto `live-install` → **SECRETS DISCIPLINE**: audit/log records carry operator
> activity (usernames, operations). Cite STRUCTURE — record schema, counts, time ranges, operation TYPES —
> mask specific user identities and payload values.
>
> **Ángulo:** NOT the .hdb FORMAT (already covered by focus `database`) but the CONTENT this unit recorded — the
> operational/security trace of a seed station. Also builds a lightweight on-disk .hdb reader (§19). Sibling of
> `jace-station-config`/`jace-data-at-rest` on the same SD copy.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 696
gaps_closed: 2
known_gaps: 5
investigable_open: 3
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: jace-history-audit
status: active (bootstrapped 2026-08-30; backlog seeded from P2 history/alarm inventory)
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B699)

## Coverage

- **Covered blocks**: 694 corpus-wide (this focus: B699-) (shared-global)
- **Source (out of git)**: `/home/niagara/stations/JACE_UMBRELLA/` — `alarm/alarm.adb` (17408B),
  `history/station/seg0/SecurityHistory.hdb` (18288B), `seg4/LogHistory.hdb` (55260B),
  `seg7/AuditHistory.hdb` (10072B), + 3 provisioning `.hdb` (DeviceStep/NetworkStep/DeviceNetworkJob, 1856B each).
- **Coverage metric**: 2 / 5 gaps closed (HD1-2)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | HD1 the .hdb on-disk record format on THIS unit + a lightweight reader (record framing, schema, timestamps) — REMITTANCE format model to `database`, NEW = parse these files | §19 build | closed (B699 — .hdb=magic A106F11E+len-prefixed HistoryConfig XML+cleartext records; built tools/hdbread.py; AuditHistory=30 recs commissioning trail) |
| high | HD2 AuditHistory + SecurityHistory CONTENT — what config-audit + auth/security events were recorded (counts, time range, operation types; identities MASKED) | hdb content | closed (B700 — SecurityHistory 58 Login/59 Logout/28 Session/1 Fail admin-dominated; low-use single-operator trace) |
| medium | HD3 LogHistory CONTENT (55KB, largest) — severity distribution, log sources, what the station logged | hdb content | pending |
| medium | HD4 alarm.adb — the alarm-database format + what alarms actually fired on this unit | adb content | pending |
| low | HD5 the 3 provisioning .hdb + SYNTHESIS — DeviceStep/NetworkStep/DeviceNetworkJob records + the operational-trace verdict for a seed station | hdb + synthesis | pending |

`tried:` (none blocked — all files present on SD P2 and extractable via qnx6read.py; SOURCE-BEFORE-AGENT passes; HD1 builds the reader).

## Remittance (ya cubiertos — NO son gaps)

- The .hdb history persistence MODEL (BHistoryService, record types, the save/rollup cycle, HDB structure) → focus `database` [Block 402]–[Block 413].
- The DEPLOYED history/alarm CONFIG (which histories are collected, schemas, no off-box archive) → focus `jace-station-config` [Block 689].
- Alarm routing/recipients framework → [Block 34] + focus `alarm-webhook`.
- Provisioning job-record generation → focus `provisioning` [Block 567].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap — P2 history/alarm inventory) | — | no · inline | HD1–HD5 seeded |
| 1 | 2026-08-30 | HD1 .hdb format + reader | B699 | no · inline (§19 built tools/hdbread.py) | 0 new |
| 2 | 2026-08-30 | HD2 Security/Audit content | B700 | no · inline (hdbread --strings --mask) | 0 new |

## Blocked gaps (each tagged with what it needs)

(none — all read-only investigable from on-disk artifacts.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 3
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass.)
