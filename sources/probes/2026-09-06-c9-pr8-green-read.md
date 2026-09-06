# C9 PR8 (CR-3 alarm, Pattern A) GREEN read — pr8-alarm-cr3 9b68462 vs 4d07cad

investigador1, 2026-09-06. Read-only in `Leon-Guanjuato-worktrees/pr8-alarm-cr3` @ 9b68462 (rebased on 4d07cad). Client
Java + docSource for risk-1. `[ev: git diff 4d07cad..9b68462 + docSource]`

## Verdict
Structurally correct and matches D9 / B827 §827.2-3 on every invariant; the ext's PARENT is the BBooleanPoint (my PR12
fix). The worker's risk-1 (driving the point's `out` directly) is LEGITIMATE and correctly bounded to the B827-G2 live
gate — the WSL structural pins prove the authoring, never the firing. No code drift.

## Invariants — all PASS
| Invariant | Result | Cite |
|---|---|---|
| `freezeAlarmPt` = child `BBooleanPoint`, built by static `makeFreezeAlarmPt()` | PASS | `BEvaporatorUnit.java:193/:714/:1320` |
| ext = `BAlarmSourceExt` with `BBooleanChangeOfStateAlgorithm`, `alarmValue=true`; ext's PARENT is the point (`pt.add("alarmSourceExt", ext)`) | PASS | factory body; ext parent = BBooleanPoint (B827 §827.2) |
| exactly ONE drive line in `recomputeFreeze` | PASS | `getFreezeAlarmPt().setOut(new BStatusBoolean(freezeTripped))` `:1133` (only setOut) |
| additive: exactly ONE slot ADDED (`freezeAlarmPt`), none removed/retyped | PASS | @NiagaraProperty 25→26, only `freezeAlarmPt` added; schema-risk SAFE |
| gradle `api(":alarm-rt")` + `api(":control-rt")` | PASS | ColdRoomPan-rt.gradle.kts diff |
| `module-include.xml` unchanged (framework types) | PASS | 0 changes |
| `Paccadia/build.gradle.kts:33` 2.0.7 → **2.1.0** | PASS | gradle diff |
| lexicon key | PASS | `freezeAlarmPt=Alarma congelamiento` |

## Risk-1 judged against docSource — LEGITIMATE, bounded to B827-G2 (not a code defect)
The worker flags "driving a BBooleanPoint's `out` directly vs a writable/priority array." docSource findings:
1. **The priority-array alternative does not apply.** `BBooleanPoint extends BDiscretePoint` is a READ-ONLY point with no
   priority array — priority arrays belong to `BBooleanWritable`. So the choice is not "out vs priority array"; it is
   "drive `out` directly" vs "switch to `BBooleanWritable`" (heavier, unwarranted for an internal alarm-driver).
2. **`out` is `Flags.OPERATOR|READONLY|TRANSIENT|SUMMARY`** (`BBooleanPoint.java:75`). READONLY blocks operator/link writes
   but not the slotomatic-generated `setOut(BStatusBoolean)` from module code; TRANSIENT means it is recomputed, not
   persisted — fine, `recomputeFreeze` drives it every cycle.
3. **The point keeps the default `BNullProxyExt`** (`makeFreezeAlarmPt` sets no proxyExt; `BControlPoint.proxyExt` default
   `:153`). A NullProxyExt does not drive `out` from hardware, so a directly-set `out` is not overwritten by a proxy read
   — the reason the pattern can work at all.
4. **The residual, real risk is live-only.** `BControlPoint` normally writes `out` via `out.copyFrom(working, setOutContext)`
   inside its execute chain (`:309`, an internal `setOutContext`), and each `PointExtension.onExecute` runs on the point's
   execute (`:68-73`). Calling `setOut(...)` from the parent bypasses that path. Whether the point's own execute cycle
   (NullProxyExt onExecute + the alarm ext) PRESERVES the manually-set `out` or resets it, and whether the
   `BAlarmSourceExt` actually evaluates the offnormal edge on a code-set `out`, cannot be proven in WSL — it is exactly the
   **B827-G2 / CRA1-live** harness-only confirm. The structural pins (CRA1s point declared, CRA2s drive line, CRA3s ext +
   algorithm) correctly assert the AUTHORING, and the RED declares the live-routing halves harness-only — so PR8 does not
   over-claim. `[ev: docSource BBooleanPoint.java:75, BControlPoint.java:153,:309,:68-73]`

**Recommendation** (for the B827-G2 live test, not a PR8 change): assert on a station that (a) `setOut(freezeTripped)` →
the `BAlarmSourceExt` raises a `BAlarmRecord` (`sourceState=offnormal`) reaching the console + DashboardPan bql; (b) `out`
survives the point's execute cycle between drives (not reset to a null/stale status by NullProxyExt onExecute); (c) it
clears + `toNormal` on recovery. If (b) fails live, the fallback is a link into the point or `BBooleanWritable` — not
needed unless the live test shows clobbering. Keep the current lighter Pattern A pending that confirm.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | freezeAlarmPt child BBooleanPoint + ext(algo alarmValue=true), ext parent=point, one drive line | [CERT] | factory + `:1133` |
| 2 | only freezeAlarmPt added (25→26), additive/SAFE; gradle api alarm-rt/control-rt; 2.1.0; lexicon | [CERT] | diffs |
| 3 | BBooleanPoint out READONLY|TRANSIENT; no priority array (read-only point); NullProxyExt default | [CERT] | docSource :75/:153 |
| 4 | out normally written via copyFrom(working,setOutContext) in the execute chain; setOut bypasses it → live-only risk | [CERT] | docSource :309/:68-73 |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
