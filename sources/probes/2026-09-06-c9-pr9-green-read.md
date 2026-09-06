# C9 PR9 (CP-1 alarm, Pattern B) GREEN read — pr9-alarm-cp1 4f77dd1 vs cf19a1c

investigador1, 2026-09-06. Read-only in `Leon-Guanjuato-worktrees/pr9-alarm-cp1` @ 4f77dd1. Pure RED run + source read.
`[ev: run-pure-test + git diff]`

## Verdict
CLEAN. Pure AlarmEdge RED runs GREEN — **OK (5 tests)**. All D10/B827 §827.4/§827.6 invariants hold; my PR12 ackAlarm-
visible fix is reflected (ackAlarm is NOT `Flags.HIDDEN`); PR1's rotation blocks are untouched. No drift.

## Invariants — all PASS
| Invariant | Result | Cite |
|---|---|---|
| `AlarmEdge` static nested in `CompressorControl`; `LOW_SUCTION=0` beside `MODE_*`; `int decide(...)` → `static final int FIRE/CLEAR/NONE`; per-trip `state[]` (wasOffnormal); `reseed` | PASS | `CompressorControl.java:61,:123-157` |
| `AlarmEdge.decide` FIRE on normal→offnormal, CLEAR on offnormal→normal past deadband, NONE else; N executes = one FIRE | PASS | `:141-153`; RED CPB1-4 green |
| `BCompressorControl implements BIAlarmSource`; transient `AlarmSupport = new AlarmSupport(this, "defaultAlarmClass")` in `started()` | PASS | `:447`; started `:7` |
| **`reseed(currentLowSuction())` BEFORE the `atSteadyState()` early-return** (restart re-seeds without firing — B827-G1) | PASS | started: `reseed` `:8` precedes `if (!Sys.atSteadyState()) return` `:15` |
| FIRE/CLEAR computed right after `ctl.step` from the SAME `suction/suctionValid`; `newOffnormalAlarm` ONLY under FIRE; `toNormal` on CLEAR; checked exceptions wrapped | PASS | execute: `ctl.step :83`, `decide :93`, `newOffnormalAlarm :97` inside FIRE, `toNormal :99`, `catch :101` |
| `ackAlarm` is a VISIBLE `@NiagaraAction` + `doAckAlarm` (not HIDDEN — the console must invoke it; my PR12 fix) | PASS | `:439-440` (no `Flags.HIDDEN`; contrast tick/powerOnExpired `:435/:437` which ARE hidden) |
| `api(":alarm-rt")`; Compresores `:33` 2.1.0 → **2.2.0**; lexicon | PASS | gradle diffs |
| additive only: the ONLY schema change is `+ackAlarm` ACTION (add_slot); no `@NiagaraProperty` added/removed → schema-risk SAFE | PASS | slot diff: added `ackAlarm` (action), removed none |
| no change to PR1's rotation blocks (2b/3b/pickers/rotSinceMs) | PASS | rotation-token diff = empty |

## S23 seed — validated evidence (added to campaign9-research-candidates.md)
Ran `lint-silent-protection.sh` on the PR9 CompPan-rt tree: exit 0, **1 WARN on `CompressorControl.java`** (the CP-1
low-suction shed) — STILL flagged even though PR9 now raises the CP-1 alarm via `BIAlarmSource`/`newOffnormalAlarm` in
`BCompressorControl`. The lint's surface-follow recognises a named `*Alarm` slot and (post-fix) a child `BAlarmSourceExt`
on a driven point (CR-3), but NOT the programmatic `BIAlarmSource`/`AlarmSupport.newOffnormalAlarm` adapter (CP-1). So
Pattern B produces a false positive. C10 refinement — same coarse-heuristic family as S21/S22. `[ev: lint-silent-protection run @ 4f77dd1; CompressorControl.java CP-1 shed]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | pure AlarmEdge RED 5/5; decide/reseed/per-trip contract | [CERT] | run-pure-test; :123-157 |
| 2 | started() reseeds before the atSteadyState return; execute FIRE/CLEAR after ctl.step, newOffnormalAlarm under FIRE | [CERT] | started :8/:15; execute :83-101 |
| 3 | ackAlarm VISIBLE; additive (+ackAlarm action only); rotation untouched; 2.2.0 | [CERT] | :439; slot diff; gradle |
| 4 | S23: lint-silent-protection still flags CP-1 post-PR9 (Pattern B not a recognised surface) | [CERT] | lint run = 1 WARN |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
