# C9 PR1 re-green read — pr1-s20 6d13d84 (RED 664dfbd byte-identical) vs a109249

investigador1, 2026-09-06. Read-only in `Leon-Guanjuato-worktrees/pr1-s20` @ 6d13d84. 116-line diff. `[ev: git diff a109249..6d13d84]`

## Verdict
All four corrections from my first read (963907e5d) landed; the ROT16b stale-clock defect (F3) is fixed. One precision
note on the "changed lines outside the rotation blocks" claim (it is four LINES / two KINDS, all semantically additive).
No behavior-altering change outside the rotation feature; SC-1 byte-identical holds.

## Four corrections — all CONFIRMED
| # | Correction | Result | Cite |
|---|---|---|---|
| F1 | `pickLeastHoursOff` byte-identical (the `cmdSince[k] != 0` guard reverted) | PASS | `git diff` of the method = EMPTY |
| F2 | staging on total `onCount` (`onCount < target` / `onCount > target`); `autoOnCount` ABSENT | PASS | `:265/:277`; grep autoOnCount = 0 |
| F3 | `rotSinceMs` stamped at the stage-up write AND the arm write; enable edge stamps ON units only; no lazy feedback stamp; zeroed on every shed + resetTransient | PASS | stage-up `+rotSinceMs[k]=now`, arm `+rotSinceMs[in]=now`; enable `for(k) if(cmd[k])`; shed `=0`; resetTransient `=0L` |
| F4 | `rotationInterval` facets MIN 0 / MAX 24 h, default 0 IN range | PASS | `newProperty(SUMMARY\|OPERATOR, BRelTime.make(0), BFacets.make(MIN, BRelTime.make(0), MAX, BRelTime.makeHours(24)))` `BCompressorControl.java:1723` |

## "Changed lines outside the rotation blocks" — four LINES / two KINDS, all additive (precision note)
The worker's "exactly two" is a change-KIND count, not a line count. The pre-existing (a109249) lines that were MODIFIED:
| Site | a109249 | 6d13d84 | Kind |
|---|---|---|---|
| stage-up write | `cmd[k]=true; cmdSince[k]=now; lastStageMs=now;` | `+ rotSinceMs[k]=now;` | stamp-on-ON |
| stage-down write | `cmd[k]=false; cmdSince[k]=now; lastStageMs=now;` | `+ rotSinceMs[k]=0;` | zero-on-OFF |
| HOA-OFF edge | `if (cmd[k]) cmdSince[k]=now;` | `if (cmd[k]) { cmdSince[k]=now; rotSinceMs[k]=0; }` | zero-on-OFF |
| LP-floor HAND shed | `{ cmdSince[k]=now; cmd[k]=false; }` | `+ rotSinceMs[k]=0;` | zero-on-OFF |
So it is FOUR modified lines (one stamp-on-ON + three zero-on-OFF shed sites), two KINDS. **Every one is semantically
additive**: it adds a write to the NEW `rotSinceMs` field only; `cmd`/`cmdSince`/`hours`/`lastStageMs` and all HOA/LP/staging
logic are unchanged. So there are ZERO behavior-altering changes outside the rotation blocks — the real target. `rotSinceMs`
is not in the golden trace (`now|cmd[]|stagesOn|lastStageMs`), so SC-1 byte-identical-at-0 holds (RED 664dfbd confirms).
The three shed-site zeroes are consistency belt-and-braces (the stage-up re-stamp on the NEXT turn-on is the real guarantee),
harmless. Recommend the retro say "two change KINDS across four command-write sites" so the count is not misread. `[ev: :265-277 diff]`

## D1 / gate 8 / pickLongestRotOn vs ROT11/13/16b — PASS
- **Completion drops rotOut BEFORE the stage move (ROT11)**: the 2b block `if (rotOut >= 0)` `:239` runs before the staging
  block; ROT12 cancel (`target >= onCount` `:243`), ROT13 drop-first (`target < onCount - 1` `:246`), ROT14 high-head drop
  (`dischargeHigh` `:247`); MAKE-before-break completion drops `rotOut` explicitly and sets `lastStageMs=now`. ✓
- **Gate 8** `(now - rotSinceMs[out]) >= c.rotationIntervalMs && (now - cmdSince[out]) >= c.minOnMs` `:299-300` — interval
  on the rotation clock, min-on on the command clock. ✓
- **`pickLongestRotOn`** `:469` — running AUTO, largest `now - rotSinceMs`, tie→hours. ✓
- **ROT16b (post-enable stage-up)**: the enable edge stamps ON units only (`if (cmd[k])`), OFF units keep 0, and the
  stage-up write stamps `rotSinceMs[k]=now` on turn-on — so an OFF-at-enable unit that stages on gets a FRESH clock and
  cannot rotate at ~0 runtime. The exact defect from my first read is closed. ✓

## Minor
`seedRestart` seeds ALL units (`for(k) rotSinceMs[k]=now`), not ON-only as the spec phrasing suggested — immaterial: at
restart `cmd[]` is cleared, so no OFF unit's seeded value is ever read (pickLongestRotOn reads only `cmd[k]`), and stage-up
re-stamps on turn-on. It mirrors how `cmdSince` is already seeded for all. `[ev: seedRestart body]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | F1-F4 all landed | [CERT] | diffs + facets line :1723 |
| 2 | 4 modified pre-existing lines, all additive rotSinceMs writes; zero behavior-altering | [CERT] | :265-277 + shed sites |
| 3 | completion-before-stage-move, gate 8, pickLongestRotOn, ROT16b fixed | [CERT] | :239/:299-300/:469 |
| 4 | seedRestart seeds all (immaterial) | [CERT] | seedRestart body |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
