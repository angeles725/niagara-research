# B827 · Raising an OPERATOR ALARM for a protection trip in an -rt module — the concrete fix for [B821]'s zero-alarm finding (seed S18): `BAlarmSourceExt` needs a `BControlPoint` parent, so a custom `BComponent` uses EITHER a child point + the ext (declarative) OR `BIAlarmSource` + `AlarmSupport.newOffnormalAlarm` (programmatic); both route a `BAlarmRecord sourceState=offnormal` that the console and DashboardPan's `bql` query show; adding it is schema-SAFE `[CERT]`

> **Scope**: [B821] §821.4 found our RT modules raise ZERO alarm-console events — every protection surface tops out at a
> plain SUMMARY slot. This block gives the MINIMAL CORRECT Niagara 4.14 pattern to make a trip raise a real operator
> alarm (the S18 fix), for our CUSTOM `BComponent`s (ColdRoomPan/CompPan are NOT `BControlPoint`s). Covers the two legal
> patterns, alarm-class routing + ack, offnormal/normal transitions, what the alarm console + DashboardPan alarm query
> show, copy-ready sketches for CP-1 (low-suction) and CR-3 (freeze), and the schema-risk. REMITTANCE to [B8] (the alarm
> architecture) — this block is the AUTHORING FIX, not a re-derivation; it also corrects two [B8] details.
>
> **Sources**: FUENTE 3 (`[CERT]`, Tridium docSource, crux cites confirmed at the enclosing method) —
> `organized/docSource/docSource-doc/extracted/alarm-rt/javax/baja/alarm/{AlarmSupport,BAlarmService,BAlarmRecord,BSourceState,BAckState,BIAlarmSource,BAlarmClass,ext/BAlarmSourceExt,ext/offnormal/{BOutOfRangeAlgorithm,BBooleanChangeOfStateAlgorithm}}.java`,
> `control-rt/vineflower/javax/baja/control/BPointExtension.java`. FUENTE 2 (`[CERT-doc]`) — niagara-help `class BSourceState`
> (offnormal/fault/normal/alert). FUENTE 1 — [B8] (alarm architecture, REMITTANCE + 2 corrections), [B821] (the gap +
> the 22-trip taxonomy: CP-1/CR-3), [B795] (schema-risk), our `DashboardPan-ux/BDashboardServlet` (the alarm query).

---

## 827.1 — The gap and the target `[CERT]`
[B821] §821.4: no protection in ColdRoomPan-rt/CompPan-rt raises a `BAlarmRecord`; the DashboardPan alarm panel queries
`station:|alarm:|bql:select * where sourceState = 'offnormal' or sourceState = 'fault' order by timestamp desc`
(`BDashboardServlet.java:502`). **So the fix must route a `BAlarmRecord` whose `sourceState = offnormal` (or `fault`)** —
then it appears in that query AND in the standard Workbench alarm console.

## 827.2 — Why the ext cannot mount on our component: it needs a `BControlPoint` parent `[CERT]`
`BAlarmSourceExt extends BPointExtension` (`BAlarmSourceExt.java:294`), and `BPointExtension.isParentLegal` is a hard
`return parent instanceof BControlPoint` (`BPointExtension.java:64-66`); `BAlarmSourceExt.isParentLegal` narrows it
further to `parent instanceof BControlPoint && offnormalAlgorithm.isGrandparentLegal(parent)` (`BAlarmSourceExt.java:1073-1078`).
Our `ColdRoomPan`/`CompPan` extend `BComponent`, NOT `BControlPoint`, so **the ext cannot mount on them directly**. Two
legal patterns follow. (Correction to [B8] §8.1.4: there are NO `BBooleanChangeOfStateAlarmExt`/`BOutOfRangeAlarmExt`
classes — `[CERT-absent]`, a clean grep; there is ONE `BAlarmSourceExt` whose `offnormalAlgorithm` PROPERTY is a
`BOffnormalAlgorithm` subclass, §827.3.)

## 827.3 — Pattern A: a child `BControlPoint` + `BAlarmSourceExt` (declarative) `[CERT]`
Add a child point to the custom component, drive it from the trip, and attach the ext with the right algorithm:
- **CR-3 freeze (boolean)**: a child `BBooleanPoint freezeAlarmPt` + a `BAlarmSourceExt` whose `offnormalAlgorithm` is a
  **`BBooleanChangeOfStateAlgorithm`** (`isGrandparentLegal` requires a `BBooleanPoint`, `:86-89`; `alarmValue` default
  `true`, offnormal when `out.getValue() == alarmValue`, `:124-129`). In `BEvaporatorUnit.changed()`, `set(freezeAlarmPt.out ← freezeTripped)`.
- **CP-1 low-suction (numeric)**: a child `BNumericPoint suctionAlarmPt` + a `BAlarmSourceExt` whose `offnormalAlgorithm`
  is a **`BOutOfRangeAlgorithm`** (`isGrandparentLegal` requires a `BNumericPoint`, `:291-294`; config `lowLimit`,
  `highLimit`, `deadband`, `limitEnable`; low trip when `presentValue < lowLimit`, `:501`). Drive `suctionAlarmPt.out ←`
  the live suction; set `lowLimit = suctionLowLimit`, enable low only, `deadband` ≈ 1 psi (anti-chatter).
**Pros**: fully declarative — operator-configurable delays/inhibit, the standard Workbench alarm-state widget works, no
alarm Java. **Cons**: one child point PER trip (CompPan has ~5 trips → 5 points), and the point must be kept in sync
(a `set()` in `changed()`). `[CERT]`

## 827.4 — Pattern B: `BIAlarmSource` + `AlarmSupport` (programmatic) `[CERT]`
The component ITSELF is the source — no child point. `[CERT]`:
- Implement `BIAlarmSource` (one method, declared as an action): `@NiagaraAction public BBoolean ackAlarm(BAlarmRecord ackRequest)` (`BIAlarmSource.java:53`).
- In `started()`: `support = new AlarmSupport(this, "defaultAlarmClass")` (`AlarmSupport.java:36`).
- On the OFFNORMAL edge of a trip: `support.newOffnormalAlarm(alarmData)` → `newAlarm(BSourceState.offnormal, data)`
  which `setAlarmTransition(offnormal)` + `setSourceState(offnormal)` (`AlarmSupport.java:109,135,160-161`) then
  `getAlarmService().routeAlarm(alarm)` (`:181-183`). `alarmData` = `BFacets` of `MSG_TEXT`, `SOURCE_NAME`, `presentValue`, …
- On the NORMAL edge: `support.toNormal(BFacets.DEFAULT, null)` (routes the matching return-to-normal).
- `doAckAlarm(BAlarmRecord r){ return BBoolean.make(support.ackAlarm(r)); }` (sets `ackState=acked`, re-routes).
- Source ORD = `BOrdList.make(comp.getNavOrd())` (`AlarmSupport.java:64`).
**Pros**: no child point; one `BIAlarmSource` per component fires MANY trips (different `alarmData`) — scales for CompPan.
**Cons**: Java + rebuild; must manage `toNormal` on every recovery + on restart; no auto Workbench delay/inhibit widget.

## 827.5 — The record, the routing, and the ack `[CERT]`
- **`BAlarmRecord`** (`BAlarmRecord.java`): `sourceState` (`BSourceState`, default `offnormal`, `:249`), `ackState`
  (`BAckState`, default `unacked`, `:275`), `ackRequired` (`:301`), `source` (BOrdList), `alarmClass` (String, default
  `"defaultAlarmClass"`), `priority`, `timestamp`, `uuid`, `normalTime`, `ackTime`, `user`, `alarmData` (BFacets —
  msgText/presentValue/lowLimit/highLimit/count/…), `alarmTransition` (the fixed initial state). `[CERT]`
- **`BSourceState`** = `{normal, offnormal, fault, alert}` ONLY (`BSourceState.java:44-60`; `[CERT-doc]` niagara-help:
  offnormal = out of the normal range, fault = invalid value/condition). **Correction to [B8] §8.1.5**: `LowLimit`/`HighLimit`
  are NOT `sourceState` values — the low/high detail is a key in `alarmData`, the `sourceState` is `offnormal`. This is
  why the DashboardPan `where sourceState='offnormal'` query DOES catch an out-of-range low-suction alarm (§827.1). `[CERT]`
- **`BAckState`** = `{acked, unacked, ackPending}` (`BAckState.java:42-52`).
- **Routing**: `BAlarmService.routeAlarm → doRouteToRecipient` (`:1027-1057`) → `lookupAlarmClass` (falls back to
  `defaultAlarmClass`, `:1272-1291`) → `BAlarmClass.doRouteAlarm` stores in the AlarmDatabase + fires the `alarm` topic
  (`:669-738`). **Ack**: `BAlarmService.doAckAlarm` → if `ackRequired`, `doRouteToSource → source.ackAlarm(uuid)`
  (`:989-1020,1108-1114`). `BAlarmClass.ackRequired` default = `TO_OFFNORMAL | TO_FAULT | TO_NORMAL` (`:176`) — so by
  default the operator acks BOTH the offnormal AND the return-to-normal event; uncheck `TO_NORMAL` to ack once. `[CERT]`
- **What shows**: the record lands in the AlarmDatabase → the DashboardPan `bql` query (§827.1) returns it, AND a
  `BConsoleRecipient` linked to the `defaultAlarmClass.alarm` topic shows it in the Workbench alarm console. The DB query
  finds it regardless of a recipient; a recipient is what makes a live console pop. `[CERT]`

## 827.6 — Copy-ready sketch (CP-1 + CR-3) `[INFER, grounded in §827.3-5]`
**Pattern B (programmatic — recommended for CompPan's several trips):**
```java
// BCompressorControl implements BIAlarmSource
private transient AlarmSupport alarm;          // built in started()
public void started(){ super.started(); alarm = new AlarmSupport(this, "defaultAlarmClass"); }
// in execute(), on the LP-trip EDGE (was-normal -> now suction < suctionLowLimit && valid):
BFacets d = BFacets.make(BAlarmRecord.MSG_TEXT, BString.make("Low suction "+suction+" < "+suctionLowLimit),
                         BAlarmRecord.SOURCE_NAME, BString.make("CompPan suction"));
alarm.newOffnormalAlarm(d);                    // sourceState=offnormal -> routed
// on the recovery EDGE (suction >= suctionLowLimit + deadband): alarm.toNormal(BFacets.DEFAULT, null);
public BBoolean doAckAlarm(BAlarmRecord r){ return BBoolean.make(alarm.ackAlarm(r)); } // @NiagaraAction
```
**Pattern A (declarative — CR-3 freeze, one point):** add a frozen `BBooleanPoint freezeAlarmPt` with a child
`BAlarmSourceExt{ offnormalAlgorithm = new BBooleanChangeOfStateAlgorithm{ alarmValue=true } }`; in
`BEvaporatorUnit.changed()` set `freezeAlarmPt.out = freezeTripped` (a `BStatusBoolean`). The ext raises/clears the
alarm automatically on the point's edge; nothing else. Guard the EDGE (only alarm on a transition, not every cycle).

## 827.7 — Schema-risk + station config `[CERT-doc + CERT]`
- **Schema-risk = SAFE (additive)**: adding a child point + ext (Pattern A) or a `BIAlarmSource` action + a transient
  `AlarmSupport` field (Pattern B) is `add_slot (frozen prop/action)` = **SAFE** ([B795] §795 r1 — old `.bog` has no
  entry → new default); NO retype of an existing slot, so no [B800] §800.8 outage risk. `[CERT-doc via B795]`
- **Station config**: `BAlarmService` + its `defaultAlarmClass` (`BAlarmClass`) exist by default. For a live console pop,
  add a `BConsoleRecipient` under `defaultAlarmClass` linked to its `alarm` topic (one-time station wiring). For the
  DashboardPan panel, nothing more — the `bql` DB query already reads the alarms. Optionally uncheck the class's
  `TO_NORMAL` ack bit so recovery doesn't require a second ack. `[CERT]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The fix must route a `BAlarmRecord sourceState=offnormal`/`fault` to hit the DashboardPan query + console | `[CERT]` | `BDashboardServlet.java:502` bql |
| 2 | `BAlarmSourceExt` needs a `BControlPoint` parent (`isParentLegal instanceof BControlPoint`), so a plain `BComponent` cannot host it | `[CERT]` | `BPointExtension.java:64-66`; `BAlarmSourceExt.java:294,1073-1078` |
| 3 | Programmatic path: `BIAlarmSource` + `AlarmSupport.newOffnormalAlarm` sets `sourceState=offnormal` and `routeAlarm`s | `[CERT]` | `BIAlarmSource.java:53`; `AlarmSupport.java:36,109,135,161,181-183` |
| 4 | `BSourceState` = normal/offnormal/fault/alert ONLY (low/high are `alarmData`, not sourceState) — corrects [B8] §8.1.5 | `[CERT]`+`[CERT-doc]` | `BSourceState.java:44-60`; niagara-help |
| 5 | CP-1 → `BOutOfRangeAlgorithm` (needs `BNumericPoint`, low trip `<lowLimit`); CR-3 → `BBooleanChangeOfStateAlgorithm` (needs `BBooleanPoint`, `alarmValue`) | `[CERT]` | `BOutOfRangeAlgorithm.java:291-294,501`; `BBooleanChangeOfStateAlgorithm.java:86-89,124-129` |
| 6 | Adding the ext/point or the `BIAlarmSource` action is schema-SAFE (additive) | `[CERT-doc]` | [B795] §795 r1 add_slot=SAFE |
| 7 | No `*AlarmExt` subclasses exist; one `BAlarmSourceExt` + a pluggable `offnormalAlgorithm` — corrects [B8] §8.1.4 | `[CERT-absent]` | clean grep for the ext-subclass names |

**Tally**: 5 `[CERT]` (2 with `[CERT-doc]`) · 1 `[CERT-absent]`. The two load-bearing cites (the `BControlPoint` parent
constraint; the `AlarmSupport` offnormal→route path) were confirmed at the enclosing method this session. §827.6 sketch is
`[INFER]` grounded in the [CERT] APIs. Dedupe: the alarm ARCHITECTURE (source/service/class/record/routing) is REMITTANCE
([B8]); this block adds the NON-POINT authoring patterns, the CP-1/CR-3 application, the schema-risk, the query-match
proof, and two [B8] corrections.

## Connections
- **[B821]** §821.4/§821.6 (the zero-alarm gap this fixes = seed S18; CP-1/CR-3 from its taxonomy), **[B8]** (alarm
  architecture — REMITTANCE + the §827.2/§827.5 corrections), **[B795]** (schema-risk = SAFE additive), **[B824]** (the
  silent-protection lint that WARNs the trips this block surfaces — B827 is the FIX B824 flags), **[B808]** (who-watches —
  the tier-1 alarm surface [B821] §821.4 lacked), **[B776]** (OPERATOR flags — the ack action). Kit: a `types/logic.md`
  §"Protection anatomy" line — a SAFETY trip raises a `BAlarmRecord sourceState=offnormal` via Pattern A (child point +
  ext) or Pattern B (`BIAlarmSource`+`AlarmSupport`); both hit the console + the bql query. CLIENT: add to CompPan
  (LP/discharge/stuck/proof-of-run) + ColdRoomPan (freeze) as the S13 health-surface tier-1.

## Open gaps
- **B827-G1** (bounded): the offnormal EDGE detection — Pattern B must fire `newOffnormalAlarm` only on the normal→offnormal
  transition (not every `execute`), and `toNormal` on recovery + on `started()` re-seed; the edge-tracking state (a
  `wasOffnormal[]` per trip) is a design detail to encode (pairs with the [B816] overlap discipline for the alarm write).
- **B827-G2** (requires-execution): confirm on a live station that a routed `defaultAlarmClass` alarm reaches the
  REFLOW/PANCCADIA console + the DashboardPan panel end-to-end (the [B821]-G2 tier-1 live confirm).
