# B702 — JACE_UMBRELLA alarm.adb (HD4): the only alarms the station ever raised were NRIO ping-fail/success on the IO-34, routed to nobody

> Focus: **jace-history-audit** · Gap **HD4** (alarm.adb — the alarm database). Source: `alarm.adb` (SD P2) via
> `tools/qnx6read.py`. Evidence: `sources/probes/B699-jace-history-audit/alarm-adb.txt`. **SECRETS DISCIPLINE:**
> slot paths + lexicon keys are non-secret. Marker `[CERT-hw]` (SD artifact). Alarm-routing framework =
> REMITTANCE [Block 34] / focus `alarm-webhook`; HD4 reads THIS unit's alarm DB.

## 702.1 — Format: a distinct alarm-database container

[CERT-hw] `alarm.adb` (17408 B) is NOT a `.hdb`: magic **`60 0D F0 0D`**, version 1 — the Niagara alarm-database
format (`FileAlarmDbConfig`, the deployed AlarmService store, [Block 689] §689.1). Its records are cleartext
BOG-style key/value tuples (`msgText=…|sourceName=…|TimeZone=…|escalated=…`), same cleartext-at-rest property as
the histories ([Block 699]).

## 702.2 — Content: NRIO reachability, and nothing else

[CERT-hw] All ~15 alarm records share `source = /Drivers/NrioNetwork/io34_*` under `defaultAlarmClass`. The
`msgText` lexicon keys are exactly two:

- `driver:pingFail` — the NRIO module stopped responding (went down);
- `driver:pingSuccess` — it came back (ping restored).

`sourceName` values: `NrioNetwork io34_1_2`, `io34_1_2 Io34 Sec`, and **`NrioNetwork io34_2_1`**. The
`io34_2_1` source is a **different bus address** than the live config's `io34_1_2` ([Block 687]) — so the alarm
DB preserves an earlier commissioning attempt at another NRIO address that was later removed, matching the 7
`Removed` audit records ([Block 699] §699.3). **No non-NRIO alarm source exists.** The station's entire alarm
life is the one field module flapping between reachable and unreachable during commissioning.

## 702.3 — The alarms fired to nobody

[CERT-hw]+[INFER] `defaultAlarmClass` has **zero recipients** ([Block 689] §689.1), so every one of these
ping-fail/success alarms was recorded locally and **routed nowhere** — no console, no email, no webhook. On a
production controller a repeatedly-failing field module is exactly the alarm you want escalated; here it sat in
the local DB. This is the alarm-side of the "no egress" finding (B689) and reinforces the operator's
Telegram-egress need (focus `alarm-webhook`): the routing must be BUILT — the alarms already exist, they just
have nowhere to go.

## Connections

- Deployed AlarmService (defaultAlarmClass, 0 recipients, FileAlarmDbConfig) → [Block 689] §689.1. IO-34 down →
  [Block 687]. Commissioning Removed edits → [Block 699]. Alarm routing / webhook egress → focus `alarm-webhook`
  [Block 666]. Cleartext-at-rest → [Block 698].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | alarm.adb = magic 600DF00D v1, cleartext BOG-style records | [CERT-hw] | header + strings | measured |
| 2 | all ~15 alarms are NRIO io34_* ping-fail/success (2 lexicon keys) | [CERT-hw] | strings tally | measured |
| 3 | io34_2_1 = a removed earlier commissioning address (matches HD1 Removed) | [CERT-hw]+[INFER] | strings + [Block 699] | corroborated |
| 4 | alarms fired but routed nowhere (0 recipients) | [CERT-hw] | [Block 689] | cited |

**Tally:** [CERT-hw] ×3 · [INFER] ×1. Ratio 0.33. Block TYPE = **EVIDENCE**. Non-secret content. Record themes
measured.

## Open gaps (this focus)

HD4 CLOSED. Next: **HD5** (the 3 provisioning .hdb — DeviceStep/NetworkStep/DeviceNetworkJob — + the focus
synthesis). After HD5, investigable → 0 → focus STOP.
