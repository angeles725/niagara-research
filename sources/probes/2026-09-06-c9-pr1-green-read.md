# C9 PR1 (S20 rotation) GREEN-diff read — feat/c9-comppan-rotation 57a15d2 vs client a109249

investigador1, 2026-09-06. Read-only in worktree `Cliente/Leon-Guanjuato-worktrees/pr1-s20` @ 57a15d2; diff base a109249.
Scope matches D1 (8 files: BCompressorControl, CompressorControl, BRotationMode, module-include.xml, module.lexicon,
build.gradle.kts, docs/write-path-matrix.md, CompressorRotationTest). Findings ranked; file:line pairs. `[ev: git diff a109249..57a15d2]`

## Verdict
The rotation FEATURE (2b/3b, gates, pickers, lifecycle, enum, adapter) is correctly built and matches D1. But **two ungated
production-path changes** ride along, and the rotation-clock stamping deviates from D1 in a way that has a **real correctness
defect**. Recommend: do not merge as-is; three fixes below.

## F1 — `pickLeastHoursOff` is NOT byte-identical (lead already flagged; CONFIRMED, + the reason it must revert)
`CompressorControl.java:439` changed `if ((now - cmdSince[k]) < minOffMs) continue;` → `if (cmdSince[k] != 0 && (now -
cmdSince[k]) < minOffMs) continue;`. This is the AUTO stage-up lead picker — a **production path, ungated by
rotationInterval**. It was added to satisfy ROT3 (fixture starts at now=0, cmdSince=0, no seedRestart, minOff>0 → the
never-commanded unit is wrongly blocked). The lead's rejection is right: the fix belongs in the FIXTURE (call
`seedRestart` or use minOff=0), not in production semantics. Note the worker LEFT `pickLeastHoursOffAuto` with the
un-guarded minOff (`:471`), so the two pickers now disagree on the never-commanded case — a second reason to revert F1
rather than propagate the guard. Verdict: QA re-pins ROT3, worker reverts `:439` to the a109249 form.

## F2 — a SECOND ungated production change: staging switched `onCount` → `autoOnCount` (NEW finding — judge like F1)
`CompressorControl.java:288` `if (autoOnCount < target)` and `:290` `else if (autoOnCount > target)` replace the a109249
`onCount` comparisons; `autoOnCount` (`:204-205`, AUTO-only commanded count) is computed unconditionally and used by
staging **whether or not rotation is enabled**. This CHANGES production staging when a HAND (`MODE_ON`) unit is on:
- **Failure scenario**: one unit in HAND, forced ON; suction/demand target = 1. a109249: `onCount==1==target` → HOLD.
  57a15d2: `autoOnCount==0 < 1` → stage up an AUTO unit → **2 compressors on for a demand of 1** (the HAND unit's
  capacity is ignored). This is live at `rotationInterval=0`.
- **Why ROT5 does not catch it**: the golden `demandSeq()` (`CompressorRotationTest.java:94`, blocks `{1,20,2,15,3,10,
  2,10,1,25,0,5,2,15,1,20}`) is pure demand with **all-AUTO modes — no HAND**, so `onCount==autoOnCount` throughout and
  the byte-identical trace passes. SC-1 therefore does NOT prove byte-identity for the HAND case; the change is unproven.
- The comment claims `autoOnCount` is "used by staging and the rotation arm gate 3", but **gate 3 uses `onCount`**
  (`:263` `&& onCount == target`), so `autoOnCount` is a staging-only change with no rotation-gate consumer.
Verdict: same class as F1. Either "HAND is invisible to AUTO staging" is an INTENDED, specified control change — then it
needs its own spec line + a HAND-trace pin, and ROT5's byte-identity must be scoped "all-AUTO only" — or revert `:288/:290`
to `onCount`. As shipped it is an unspecified, unproven production-semantics change. `[ev: CompressorControl.java:204-205,:263,:288,:290]` `[ev: CompressorRotationTest.java:94]`

## F3 — rotSinceMs lazy stamping (finding 2): CONSERVATIVE lag is fine, but there is a real STALE-CLOCK defect
The worker did NOT stamp `rotSinceMs` at the stage-up write (D1 said stamp at `:229`); instead:
- enable edge (`:155-158`): `rotationIntervalMs` 0→non-zero → `rotSinceMs[k]=now` for **all k** (incl. OFF units);
- lazy feedback stamp (`:179-181`): `if (cmd[k] && rotSinceMs[k]==0 && (now-cmdSince[k])>=stageDelayMs) rotSinceMs[k]=now;`
- arm (`:270`) stamps the incoming; stage-down (`:290`), HOA-OFF (`:337`), LP-shed (`:381`) all reset `rotSinceMs=0`.

Answering the lead's three questions:
1. **Hours ledger holds** — `hours[k]+=dtH` (`:176`) is untouched by rotSinceMs. ✓
2. **Stamp CAN lag the command by more than one stageDelay** — YES. The lazy stamp fires on the first step where
   `now-cmdSince>=stageDelay`; if steps are irregular/sparse (engine backpressure, or a long-idle first step) it fires late
   and sets `rotSinceMs=now` at that later step, so the clock under-counts. But the lag is always in the SAFE direction
   (rotation happens later, never earlier), so this alone does not break ROT16.
3. **ROT16 does NOT fully hold — the enable-edge stamp of OFF units is a defect.** At enable, an OFF unit gets
   `rotSinceMs=now (≠0)`; when it later stages ON, the stage-up write (`:281-283`) does NOT reset `rotSinceMs` and the lazy
   guard (`rotSinceMs==0`) is false, so `rotSinceMs` **stays at the enable time**.
   - **Failure scenario**: enable rotation at t0 while unit B is OFF. Demand rises, B stages ON at t1. `rotSinceMs[B]` is
     still t0, so its rotation clock reads `now-t0` and counts the time B was OFF. If `t1-t0 >= rotationInterval`, B is
     **immediately eligible to rotate out at ~0 runtime** — the opposite of ROT16's "a full interval before rotating".
     Reachable in production (enabling rotation while part of the rack is cycled off is normal). ROT1/ROT4 miss it because
     they seed `rotSinceMs` via `seedRestart` rather than via a fresh post-enable stage-up.
Fix (restores D1 and kills both the lag and the stale-clock): stamp `rotSinceMs[k]=now` AT the stage-up write (`:281-283`,
matching D1's `:229`), and at the enable edge stamp only `cmd[k]` units (set OFF units to 0). Then the lazy feedback stamp
(`:179-181`) is unnecessary. Also the `:178` comment says "start 2 stageDelays after" but the guard is ONE stageDelay —
fix the comment. `[ev: CompressorControl.java:155-158,:178-181,:281-283,:426-427]`

## Checklist — the rest (all PASS)
| Item | Result | Cite |
|---|---|---|
| Completion (2b) drops `rotOut` explicitly BEFORE the stage move; sets `lastStageMs=now` so same-cycle staging is blocked (ROT11) | PASS | `:249-268` (`cmd[rotOut]=false … rotOut=-1; swaps++`) |
| Gate 8 = interval on `rotSinceMs[out]` AND minOn on `cmdSince[out]` | PASS | `:274-275` |
| `pickLeastHoursOffAuto` (skip `cmd[k] || modes[k]!=MODE_AUTO`) used ONLY by rotation (2b break-before-make MAKE, 3b arm) | PASS | def `:465`; callers `:261,:279` only |
| `pickLongestRotOn` = running AUTO, largest `now-rotSinceMs`, tie→hours | PASS | `:481-495` |
| `resetTransient` clears rotOut/rotArmedMs/rotSinceMs[]/swaps/lastRotIntervalMs; `seedRestart` re-seeds rotSinceMs | PASS | `:411-414`, `:428` |
| `BRotationMode` = `@NiagaraType` `extends BFrozenEnum`, `@Range makeBefore/breakBefore`, NEW + non-linked (B828 §828.7-safe) | PASS | `BRotationMode.java:24-47` |
| Adapter: `rotationMode` `BRotationMode` `SUMMARY\|OPERATOR`; cfg wiring `getMillis()`/`getOrdinal()`; no change to `condenserNMode` (only adjacent `changed()`-guard context) | PASS | `BCompressorControl.java` +slots, cfg lines; condenserNMode unchanged |
| `Compresores/build.gradle.kts:33` 2.0.3 → **2.1.0** | PASS | gradle diff |
| `docs/write-path-matrix.md` +2 rows | PASS (count; rows not rendered here) | diff --stat |

## F4 — facet inconsistency on `rotationInterval` (minor but a likely verify-gate FAIL)
`rotationInterval` facets = `BFacets.make(BFacets.MIN, BRelTime.makeSeconds(1))` (`BCompressorControl.java` slot) but
`defaultValue = BRelTime.make(0)`. **The default (0) is below the facet MIN (1 s)**, and 0 is the disabled sentinel the
whole feature relies on — an operator cannot set it back to 0 from Workbench. D1 specified **MIN 0 / MAX 24 h** precisely
so 0 stays reachable and `verify-module.sh --src facets-req` (C8 D5) does not FAIL a numeric OPERATOR slot missing MAX.
As shipped: MIN=1s (excludes the sentinel) and **no MAX** (likely a facets-req FAIL). Fix: MIN `BRelTime.make(0)` + MAX
`BRelTime.makeHours(24)`. Confirm against `verify-module.sh --src` before merge. `[ev: design D1 slot block]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | F1 pickLeastHoursOff guard added; pickLeastHoursOffAuto left unguarded | [CERT] | `:439` vs `:471` |
| 2 | F2 staging onCount→autoOnCount, ungated, HAND-invisible; golden all-AUTO | [CERT] | `:288/:290/:204-205`; `CompressorRotationTest.java:94` |
| 3 | F3 enable-edge stamps OFF units, stage-up does not reset → stale rotation clock | [CERT] | `:155-158`, `:281-283` (no rotSinceMs), `:179-181` guard |
| 4 | F3 lag can exceed one stageDelay under irregular ticks; hours ledger intact | [CERT] | `:179-181`, `:176` |
| 5 | Feature core (2b/3b/gates/pickers/enum/adapter/version) matches D1 | [CERT] | checklist table |
| 6 | F4 default 0 below facet MIN 1s; no MAX | [CERT] | adapter slot decl |
Tally: 6 [CERT] · 0 [INFER] · 0 unmarked.
