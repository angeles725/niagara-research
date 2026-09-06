# C9 PR8 (R8, Pattern A — CR-3 freeze) and PR9 (R9, Pattern B — CP-1 low suction): apply packages (client)

Author: companero (Fable), 2026-09-06. Contracts VERBATIM from the two client REDs — `qa/c9-alarm-cr3` **`70a357b`**
(`ColdRoomPan-rt/srcTest/.../FreezeAlarmWiringTest.java`, CRA1s/CRA2s/CRA3s/CRA4/CRA6 + CRA5 mutation) and `qa/c9-alarm-cp1`
**`8b43488`** (`CompPan-rt/srcTest/.../CompressorAlarmEdgeTest.java` CPB1–CPB4 + per-trip, `CompressorAlarmWiringTest.java`
CPB-W1..W4 + SC13). Mechanics = B827 §827.3/§827.4/§827.6 + design D9/D10 (D10a: the RED's pure `AlarmEdge` supersedes
the inline sketch). Anchors counted at the **a109249** worktree. `[ev: REDs 70a357b, 8b43488]` `[ev: corpus B827]` `[ev: design D9/D10]`

## 0. Shared facts (both PRs)
- **Harness split is load-bearing:** `BAlarmSourceExt`/`AlarmSupport` only become real inside a station; the WSL REDs are
  STRUCTURAL (they read the source) + pure. The live-routing halves (CRA1/2/3 behaviour, **CPB5** `sourceState == offnormal`)
  are **HARNESS-ONLY** on the Windows `niagaraTest` — `skip`-gated in WSL and **a SKIP is never reported as green** (C7 D9).
- **What the DashboardPan alarm bql selects** is `sourceState = 'offnormal' or 'fault'` (`BDashboardServlet.java:502`); both
  patterns route a `BAlarmRecord` with `sourceState = offnormal` — low/high is a key in `alarmData`, never a sourceState value.
- Both are **schema-risk SAFE** (additive child point / additive action + transient support; no retype/remove — B795).
- **R3 ↔ R8 coupling (D9):** making `freezeTripped` visible via a point CLOSES the CR-3 WARN that `lint-silent-protection`
  (R3) raises; whichever merges second updates the other's smoke pin (the four-root count+subject+absence table).

---
## PR8 — Pattern A, `BEvaporatorUnit` (ColdRoomPan-rt), RED `70a357b`
### A.1 Contract (verbatim)
| Pin | Asserts (source text, `FreezeAlarmWiringTest` :68-109) |
|---|---|
| CRA1s | `BEvaporatorUnit` declares a child slot `freezeAlarmPt` whose declaration contains `BBooleanPoint` |
| CRA3s | the source contains `BAlarmSourceExt` AND `BBooleanChangeOfStateAlgorithm` AND the algorithm alarms on TRUE (`alarmValue = true`) |
| CRA2s | inside `void recomputeFreeze()`'s body, `freezeAlarmPt` and `freezeTripped` appear together (regex `freezeAlarmPt.*freezeTripped\|freezeTripped.*freezeAlarmPt`, DOTALL) — the drive line |
| CRA4 | **additive-only**: every one of the embedded **21 a109249 slots** is still declared with its type (§A.3 list) |
| CRA6 | `Paccadia/build.gradle.kts` `defaultModuleVersion("2.1.0")` (today `2.0.7`, `:33`) |
| CRA5 (mutation) | remove the ext / set `alarmValue=false` → CRA3s flips (OBSERVED) |
| CRA1/2/3 live | HARNESS-ONLY (routed record on freeze trip; clears on recovery; `sourceState==offnormal`) |
### A.2 File-level diff plan
| # | Anchor @ a109249 | Edit |
|---|---|---|
| A-F1 | `BEvaporatorUnit.java` slot block, beside `freezeProtect`/`freezeSetpoint` (the freeze family) | NEW frozen child: ```@NiagaraProperty(name = "freezeAlarmPt", type = "BBooleanPoint", defaultValue = "new BBooleanPoint()", flags = Flags.SUMMARY \| Flags.READONLY)``` + the ext as the point's child — either declared in the point's default (`new BBooleanPoint()` then `started()` adds `alarmSourceExt` once if absent) or, cleaner, a static factory building `BBooleanPoint` with a child `BAlarmSourceExt` whose `offnormalAlgorithm = new BBooleanChangeOfStateAlgorithm()` with `alarmValue = true` (`isGrandparentLegal` requires a `BBooleanPoint`, B827 §827.3 `:86-89`; offnormal when `out == alarmValue`, `:124-129`). Put the literal tokens `BAlarmSourceExt`, `BBooleanChangeOfStateAlgorithm` and `alarmValue` + `true` in THIS source file (CRA3s greps the file). |
| A-F2 | `recomputeFreeze()` `:1088-1100`, right after `freezeTripped = ColdRoomControl.freezeTrip(...)` (`:1092`) | ONE drive line: `getFreezeAlarmPt().getOut().setValue(freezeTripped);` (writes the point's `out` `BStatusBoolean` from the latch). The ext raises on the false→true edge and clears on true→false — **no edge state machine in the module** (the ext owns it). |
| A-F3 | `module-include.xml` | no new type (the point/ext are `control:`/`alarm:` types); dependency floors: ColdRoomPan-rt must depend on `alarm` (add `api(":alarm")` if absent — check `ColdRoomPan-rt.gradle.kts`, L7 3-part floor). |
| A-F4 | `Paccadia/build.gradle.kts:33` | `defaultModuleVersion("2.0.7")` → `("2.1.0")` (CRA6; proposal :115) |
| A-F5 | lexicon | `freezeAlarmPt=Alarma congelamiento` (per-slot lint) |
| A-F6 | tests | the RED file lands as-is; GREEN = A-F1/F2/F4. `ColdRoomControlTest` etc. (37) unchanged. Harness: record the Windows `niagaraTest` result for CRA1/2/3-live or mark SKIP explicitly. |
### A.3 The 21-slot baseline CRA4 embeds (from a109249 — nothing may be dropped or retyped)
`runCmd BStatusBoolean · startDelay BRelTime · stopDelay BRelTime · fanRunMode BFanMode · defrostFanOffDelay BRelTime ·
valveOut BStatusBoolean · evapOut BStatusBoolean · resistanceOut BStatusBoolean · coilTemp BStatusNumeric ·
resistanceTemp BStatusNumeric · evapHighAlarmLimit double · evapLowAlarmLimit double · hasDefrost boolean · valveMode double ·
fanMode double · resistanceMode double · freezeProtect boolean · freezeSetpoint double · freezeDiffStop double ·
freezeDiffRestart double · powerOnDelay BRelTime` (`module-find slots` @ a109249: 21). `freezeAlarmPt` is the 22nd, additive.
### A.4 Anchors
`recomputeFreeze()` `:1088`; latch assignment `:1092`; `valveInhibited()` `:1102-1106`; HOA apply of `valveOut` `:1238`;
`private boolean freezeTripped` `:1287`; group gradle `:33` = `2.0.7`. `[ev: client @ a109249]`

---
## PR9 REFRESH 2026-09-06 — anchors re-cut against PR1's REAL tree (e5bee1c)
PR1 (rotation) grew `BCompressorControl` and `CompressorControl`; every PR9 insertion point below is RE-ANCHORED at PR1's
tip `Cliente/Leon-Guanjuato-worktrees/pr1-s20` (e5bee1c). **Compresores `build.gradle.kts:33` = `2.1.0` now** → PR9 sets it
to `2.2.0` (fragment-merge on that one line; PR1 already moved 2.0.3→2.1.0). The RED `8b43488` references only
`CompressorControl.AlarmEdge` (+ `CompressorControl.AlarmEdge.FIRE/CLEAR/NONE`, `new CompressorControl.AlarmEdge(1)`, the
`LOW_SUCTION` trip index) and greps `class\s+BCompressorControl\s+extends\s+BComponent\s+implements\s+[^{]*BIAlarmSource`
— PR1 renamed NEITHER class nor package, so the RED still resolves and stays RED only because `AlarmEdge` is absent. `[ev: pr1-s20 @ e5bee1c, 2026-09-06]`

| Insertion | Anchor @ a109249 (old) | Anchor @ PR1 tip e5bee1c (USE THIS) |
|---|---|---|
| `BCompressorControl` class decl | :414 | **:433** (`public class BCompressorControl`) |
| `started()` | :1777 | **:1845** — `super.started()` :1847; the `if (!Sys.atSteadyState()) return;` guard at :1854; `ctl.seedRestart(Clock.millis()); execute();` at :1857 |
| `stopped()` | :1806 | **:1874** — `super.stopped()` :1883 |
| `execute()` | :1891 | **:1960** — Cfg map incl. `cfg.suctionLowLimit = getSuctionLowLimit()` :1973; `double suction = CompressorControl.selectSuction(...)` :2039; `boolean suctionValid = !Double.isNaN(suction)` :2040; `ctl.step(...)` :2042; condenser out writes :2044-2046; execute ends before :2082 |
| CP-1 LP shed (the condition the edge watches) | :215 | **CompressorControl :234** (`target = Math.min(target, onCount - 1)`; lpFloor also at :295/:333/:366) — the same predicate `suctionValid && cfg.suctionLowLimit > 0d && suction < cfg.suctionLowLimit` |
| `CompressorControl` class / Cfg | (pkg-visible) | `final class CompressorControl` :50; `MODE_*` :56-58; `static final class Cfg` :94 (`suctionLowLimit`, `minOffMs`) — `AlarmEdge` is added as a `static final class` INSIDE this final class (legal) |

**Placement (re-anchored):**
- `AlarmEdge` (B-F1): new `static final class AlarmEdge` inside `CompressorControl` (CC:50), beside `Cfg` (CC:94); trip index constant `static final int LOW_SUCTION = 0` (the RED's name) beside `MODE_*` (CC:56).
- `implements BIAlarmSource` (B-F2/CPB-W1): edit the class decl at BCC:**433** — `public class BCompressorControl extends BComponent implements BIAlarmSource` (the regex needs `extends BComponent implements … BIAlarmSource` on that line).
- `AlarmSupport` field + `alarm = new AlarmSupport(this, "defaultAlarmClass")` (B-F4/F5, CPB-W2): in `started()` at BCC:**1848**, right AFTER `super.started()` and BEFORE the `!Sys.atSteadyState()` early-return, so a commissioning mount still creates it (W2 greps `new AlarmSupport(` within 2000 chars of `started()`). `stopped()` BCC:**1883** after `super.stopped()`: `alarm = null`.
- `alarmEdge.reseed(new boolean[]{ lpConditionNow() })` (B-F5/CPB-W4): also in `started()` at BCC:**1848** (a small `lpConditionNow()` helper reading the same suction slots as execute()) — reseed from the CURRENT LP condition so a restart during an active low-suction never re-fires.
- the FIRE/CLEAR decision (B-F6/CPB-W3): in `execute()` right AFTER `ctl.step(...)` at BCC:**2042**, using `suction`/`suctionValid` already computed at :2039-2040: `boolean now = suctionValid && cfg.suctionLowLimit > 0d && suction < cfg.suctionLowLimit; boolean recovered = suctionValid && suction >= cfg.suctionLowLimit + LP_DEADBAND_PSI; int d = alarmEdge.decide(CompressorControl.LOW_SUCTION, now, recovered); if (d == CompressorControl.AlarmEdge.FIRE) alarm.newOffnormalAlarm(lowSuctionData(suction)); else if (d == CompressorControl.AlarmEdge.CLEAR) alarm.toNormal(BFacets.DEFAULT, null);` — `newOffnormalAlarm` sits INSIDE the `== AlarmEdge.FIRE` branch (CPB-W3 regex).
- `ackAlarm` action (B-F3, CPB-W1 support): the visible `@NiagaraAction ackAlarm` delegating to `alarm.ackAlarm` goes in the action block (the RED wants a VISIBLE action per B827 §827.4, NOT hidden — investigador1's PR12 fix).

## PR9 — Pattern B, `BCompressorControl` + pure `CompressorControl.AlarmEdge` (CompPan-rt), RED `8b43488`
### B.1 Contract (verbatim — the pure class, `CompressorAlarmEdgeTest` :16-25)
```java
// static nested class in CompressorControl, no Baja
static final class AlarmEdge {
  static final int FIRE = 1, CLEAR = 2, NONE = 0;              // int constants (compile contract)
  AlarmEdge(int trips)
  int decide(int trip, boolean nowOffnormal, boolean recoveredPastDeadband)
     // FIRE  iff nowOffnormal && !wasOffnormal[trip]                       (then wasOffnormal[trip] = true)
     // CLEAR iff !nowOffnormal && wasOffnormal[trip] && recoveredPastDeadband (then wasOffnormal[trip] = false)
     // NONE  otherwise — repeated offnormal executes NEVER re-fire (R9.5)
  void reseed(boolean[] current)   // started(): wasOffnormal := current; returns nothing, fires nothing (R9.4)
  boolean wasOffnormal(int trip)
}
```
| Pin | Asserts |
|---|---|
| CPB1 | first `decide(t, true, false)` → FIRE; `wasOffnormal(t)` true |
| CPB2 | 25 more `decide(t, true, false)` → **0** FIREs (exactly one record over N executes) |
| CPB3 | `decide(t, false, false)` → NONE (inside deadband); `decide(t, false, true)` → CLEAR; `wasOffnormal` false; a NEW trip FIREs again |
| CPB4 | `reseed(new boolean[]{true})` → `wasOffnormal` true; next `decide(t, true, false)` → NONE (no re-fire after restart) |
| per-trip | `new AlarmEdge(2)`: trips 0 and 1 have independent edges |
| CPB-W1 | regex `class\s+BCompressorControl\s+extends\s+BComponent\s+implements\s+[^{]*BIAlarmSource` |
| CPB-W2 | source declares an `AlarmSupport` field AND `new AlarmSupport(` appears within 2000 chars after `public void started()` |
| CPB-W3 | source contains `AlarmEdge`; `newOffnormalAlarm` is guarded by the FIRE decision (regex accepts `== AlarmEdge.FIRE` / `== CompressorControl.AlarmEdge.FIRE`); `toNormal` present |
| CPB-W4 | `started()` calls `reseed(` |
| SC13 | `Compresores/build.gradle.kts` `defaultModuleVersion("2.2.0")` (today `2.0.3`; **PR1 takes it to 2.1.0 first** — fragment-merge on the same `:33` line) |
| CPB5 | HARNESS-ONLY: `BAlarmRecord.sourceState == offnormal` on the routed record |
| mutations | drop `implements BIAlarmSource` → W1; call `newOffnormalAlarm` outside the FIRE branch → W3; never reset `wasOffnormal` on CLEAR → CPB3 |
### B.2 File-level diff plan
| # | Anchor @ a109249 | Edit |
|---|---|---|
| B-F1 | `CompressorControl.java` — new `static final class AlarmEdge` (nested, package-visible) | §B.1 verbatim: `private final boolean[] wasOffnormal;` sized by `trips`; `decide` returns the int; `reseed` copies `current` in. Trip indices as named constants beside it: `TRIP_LOW_SUCTION = 0` (CP-1) — extend later for the other CompPan trips. |
| B-F2 | `BCompressorControl.java:414` class declaration | `public class BCompressorControl extends BComponent implements BIAlarmSource` (the W1 regex needs `implements ... BIAlarmSource` on the declaration) |
| B-F3 | `@NiagaraAction` block | `@NiagaraAction(name = "ackAlarm", parameterType = "BAlarmRecord", defaultValue = "new BAlarmRecord()", returnType = "BBoolean", flags = Flags.HIDDEN)` + `public BBoolean doAckAlarm(BAlarmRecord r) { return BBoolean.make(alarm != null && alarm.ackAlarm(r)); }` (B827 §827.4 `BIAlarmSource.java:53`) |
| B-F4 | fields | `private transient AlarmSupport alarm;` + `private final CompressorControl.AlarmEdge alarmEdge = new CompressorControl.AlarmEdge(1);` (transient state; a restart re-seeds, never re-fires) |
| B-F5 | `started()` `:1777` | after `super.started()`: `alarm = new AlarmSupport(this, "defaultAlarmClass");` (W2, `AlarmSupport.java:36`) then `alarmEdge.reseed(new boolean[]{ currentLowSuction() });` (W4 — seed from the CURRENT condition, fire nothing). `stopped()` `:1806`: `alarm = null`. |
| B-F6 | `execute()` `:1891`, after `ctl.step(...)` `:1971` | `boolean now = suctionValid && cfg.suctionLowLimit > 0d && suction < cfg.suctionLowLimit;` `boolean recovered = suctionValid && suction >= cfg.suctionLowLimit + LP_DEADBAND_PSI;` (≈1 psi, anti-chatter) `int d = alarmEdge.decide(CompressorControl.TRIP_LOW_SUCTION, now, recovered); if (d == CompressorControl.AlarmEdge.FIRE) alarm.newOffnormalAlarm(lowSuctionData(suction)); else if (d == CompressorControl.AlarmEdge.CLEAR) alarm.toNormal(BFacets.DEFAULT, null);` — `lowSuctionData` = `BFacets.make(BAlarmRecord.MSG_TEXT, BString.make("Low suction " + suction + " < " + limit), BAlarmRecord.SOURCE_NAME, BString.make("CompPan suction"))` (B827 §827.6). The `newOffnormalAlarm` call sits INSIDE the `== AlarmEdge.FIRE` branch (W3 regex). |
| B-F7 | `Compresores/build.gradle.kts:33` | `("2.0.3")` → PR1 `("2.1.0")` → PR9 `("2.2.0")`; fragment-merge: PR9 lands on top of PR1's line |
| B-F8 | gradle deps | CompPan-rt must depend on `alarm` (`api(":alarm")`) for `AlarmSupport`/`BIAlarmSource`/`BAlarmRecord` — L7 3-part floor |
| B-F9 | tests | both RED files land as-is; GREEN = B-F1..F7. `CompressorControlTest` (37) + `CompressorRotationTest` (PR1, 17) unchanged. CPB5 → Windows harness or explicit SKIP. |
### B.3 Anchors
class decl `:414`; `started()` `:1777`; `stopped()` `:1806`; `execute()` `:1891`; `ctl.step(...)` `:1971`; the CP-1 shed itself
`CompressorControl.java:215` (the condition the edge machine watches); group gradle `:33` = `2.0.3`. `[ev: client @ a109249]`

## Sequencing + gates (both)
PR1 before PR9 (the `:33` version line). `schema-risk.sh` SAFE for both; `lint-silent-protection` re-smoke after PR8 (CR-3
no longer silent — update the R3 four-root table); `lint-structure.sh` L7 for the new `alarm` dependency; `slot-coverage.sh
per-slot` for `freezeAlarmPt`; pure JUnit green in WSL; the harness-only pins recorded as SKIP with the Windows run noted.
Retro slugs: `campaign9-alarm-cr3`, `campaign9-alarm-cp1` (client repos — INDEX per repo convention).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | PR8 pins + regexes; 21-slot baseline; 2.0.7→2.1.0 | [CERT] | FreezeAlarmWiringTest :68-109 @ 70a357b; module-find @ a109249; Paccadia gradle :33 |
| 2 | PR9 AlarmEdge contract + wiring regexes; 2.0.3→2.1.0→2.2.0 | [CERT] | CompressorAlarmEdgeTest :16-89, CompressorAlarmWiringTest :37-74 @ 8b43488; Compresores gradle :33 |
| 3 | anchors :1088/:1092/:1102/:1238/:1287 and :414/:1777/:1806/:1891/:1971/:215 | [CERT] | grep @ a109249 worktree |
| 4 | ext legality (grandparent BBooleanPoint), AlarmSupport API, alarmData keys | [CERT] | B827 §827.3/§827.4/§827.6 line cites |
| 5 | deadband ≈ 1 psi, `alarm` dep needed | [INFER] | D10 + gradle check at apply |
