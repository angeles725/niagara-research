# B700 — JACE_UMBRELLA SecurityHistory + AuditHistory content (HD2): ~58 login sessions and 30 config edits, almost all by admin — a low-use, single-operator trace with one recorded auth failure

> Focus: **jace-history-audit** · Gap **HD2** (AuditHistory + SecurityHistory content). Sources:
> `SecurityHistory.hdb` + `AuditHistory.hdb` (SD P2) via `tools/hdbread.py`. Evidence:
> `sources/probes/B699-jace-history-audit/security-audit-content.txt`. **SECRETS DISCIPLINE:** operation
> types + counts cited; user identities role-only (`admin`) or masked. Marker `[CERT-hw]` (SD artifact).

## 700.1 — SecurityHistory: the authentication trail

[CERT-hw] `SecurityHistory.hdb` (18288 B): record type `history:SecurityAuditRecord`, **4-field schema**
(`timestamp, operation, userName, message`). Operation tally over the unit's life:

| operation | count |
|---|---|
| Login | 58 |
| Logout | 59 |
| Session | 28 |
| **Fail** | **1** |

~58 login/logout cycles, 28 session events, and **exactly one recorded authentication failure**. The `admin`
role account dominates (118 references); no evidence of multi-operator activity (consistent with the single
login account, focus `jace-station-config` [Block 688] §688.1). This is a **low-use, single-operator** trace —
the login volume of a bench/commissioning unit, not a production controller with many technicians.

## 700.2 — AuditHistory: 30 commissioning edits (from HD1)

[CERT-hw] `AuditHistory.hdb` = `history:AuditRecord`, 7-field, **30 records** (18 Added · 5 Changed · 7 Removed),
all `admin` — the config-build edits documented in [Block 699] §699.3 (adding the NRIO network + points, the
COM2↔COM1 portName churn). Together the two trails say the same thing: the station was **configured once,
lightly, by one admin**, then largely idle.

## 700.3 — What the record counts do and don't reveal

[CERT-hw]+[INFER] The `timestamp` field is a **binary-encoded `baja:AbsTime`** (not an ASCII string), so a
time-span is not string-extractable from the raw file; the record COUNTS are the reliable metric (exact
per-record timestamp decode is a refinement, not needed for the low-use verdict). The single `Fail` is a real
datum — one failed auth over ~58 sessions — but without decoding its record we cannot attribute it (masked user
/ time); it is noted, not escalated. This is the honest boundary of a count-level read.

## 700.4 — Security angle (reinforces B699/B698)

[CERT-hw] Both trails are **cleartext** on the card (`.hdb` records unencrypted, [Block 699] §699.1). So the
complete record of *who logged in, when, and what they changed* is readable from the SD with no key — and (no
off-box replica, [Block 689]) it is also silently **rewritable**. For a security-audit trail that is the worst
property: on physical media possession it offers neither confidentiality nor tamper-evidence.

## Connections

- .hdb format + reader → [Block 699] (HD1). Single admin account → [Block 688] §688.1. No off-box archive /
  no tamper-evidence → [Block 689] / [Block 566]. Cleartext-at-rest verdict → [Block 698] (DAR6).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | SecurityHistory = SecurityAuditRecord 4-field; 58 Login/59 Logout/28 Session/1 Fail | [CERT-hw] | hdbread --strings tally | measured |
| 2 | admin-dominated, single-operator trace | [CERT-hw] | 118 admin refs, no other role | measured |
| 3 | AuditHistory 30 commissioning edits (remittance to HD1) | [CERT-hw] | [Block 699] | cited |
| 4 | timestamp binary AbsTime → counts are the metric, not time-span | [CERT-hw]+[INFER] | schema + strings | reasoned |
| 5 | trails cleartext + rewritable on card | [CERT-hw] | [Block 699]/[Block 689] | cited |

**Tally:** [CERT-hw] ×4 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE**. No secret value; identities
role-only/masked. Counts measured via the reader.

## Open gaps (this focus)

HD2 CLOSED. Next: **HD3** (LogHistory.hdb, 55 KB — the largest: severity distribution, log sources, what the
station logged). Then HD4 (alarm.adb), HD5 (provisioning .hdb + synthesis).
