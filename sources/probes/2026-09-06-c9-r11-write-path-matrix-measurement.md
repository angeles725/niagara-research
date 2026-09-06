# C9 R11 — write-path matrix: the MEASURED uncovered set at client tip `a109249` (correction input for tasks/spec)

Author: companero (Fable), 2026-09-06. Tool: kit `toolbelt/lint-write-path.sh` (niagara-tools main) + `tools/module-find.py
slots --flags OPERATOR`, run on a clean worktree of the client at **`a109249`** (origin/main). `[ev: kit lint-write-path.sh]` `[ev: client @ a109249]`

## 1. CORRECTION — the matrix EXISTS; R11 is EXTEND, not author-from-scratch
`<client-root>/docs/write-path-matrix.md` **exists at `a109249`** (113 lines, **20 data rows**, columns
`Writable Slot | Writer | Timing | Invariant | Test`, legend ✅ this-campaign / 🔶 earlier test / ❌ C10). It is
ColdRoomPan-rt-centric (setpoint, differentialUp/Down, hoaMode, inhibit, freeze*, defrostInterval, resistanceMode,
fanRunMode, sensor validity). The lint's walk-up resolution finds it from every module root (`<module>/docs` →
`<parent>/docs` → `<client-root>/docs`). My earlier "no matrix anywhere" came from the LOCAL checkout at `4f5f1c7`, which
predates the file — a stale-tree read, exactly the C8 lesson. So:
- **SC-9 pin is exit `1 → 0`** (FAIL rows → clean), **not `3 → 0`** — the ERROR path only fires with NO matrix.
- PR11 = add the missing rows to the EXISTING file (keep its columns/legend), not a new file.
`[ev: client docs/write-path-matrix.md @ a109249]` `[ev: lint-write-path.sh walk-up resolution (retro campaign8-write-path Δ1)]`

## 2. The measured set (real counts, `a109249`)
| Module root | OPERATOR slots (`module-find --flags OPERATOR`) | Covered by a matrix row | **UNCOVERED (lint FAIL rows)** | lint exit |
|---|---|---|---|---|
| `Paccadia/ColdRoomPan` (rt) | 10 | 4 | **6** | 1 |
| `Compresores/CompPan` (rt) | 20 | 5 | **15** | 1 |
| `Dashboard/DashboardPan` (rt) | 46 | 5 | **41** | 1 |
| `Dashboard/DashboardPan` (ux) | 0 | — | 0 | (no OPERATOR slots) |
| **Total** | **76** | **14** | **62** | |
**+ S20 adds 2 OPERATOR slots on CompPan-rt** (`rotationInterval`, `rotationMode`) → **64 rows** for PR11 to land clean
(or 62 if PR11 merges before R1 and S20 carries its own 2 rows — sequence it, do not double-count).

**Reconciliation with the C8 retro's "13 on ColdRoomPan-rt + 15 on CompPan-rt":** CompPan **15 — exact match**.
ColdRoomPan is **6** at `a109249`, not 13 — the w14–w16/w23 rows (resistanceMode ×7, fanRunMode ×3) were added to the
matrix after that measurement. Real counts over estimates (C7-close L1): use 6/15/41. `[ev: retro campaign8-write-path]`

## 3. The exact uncovered slots (the rows PR11 must author) — "W14–W22" becomes THIS list
**ColdRoomPan-rt (6):** `coolOnSensorFault` · `fanMode` · `freezeDiffStop` · `freezeProtect` · `powerOnDelay` · `valveMode`

**CompPan-rt (15):** `condenser1Mode` · `condenser2Mode` · `condenser3Mode` · `faultReset` · `floatingSuction` · `minOn` ·
`powerOnDelay` · `runningAmpsThreshold` · `stageDelay` · `stageDownDelay` · `stageUpDelay` · `startProveDelay` ·
`suctionBand` · `suctionLowLimit` · `suctionMismatchTol`
(+ S20: `rotationInterval` · `rotationMode`)

**DashboardPan-rt (41):** `comp1Mode` · `comp2Mode` · `coolOnSensorFault` · `defrostDuration` · `evap1FanMode` ·
`evap1FreezeDiffRestart` · `evap1FreezeDiffStop` · `evap1FreezeProtect` · `evap1FreezeSetpoint` · `evap1ValveMode` ·
`evap2FanMode` · `evap2FreezeDiffRestart` · `evap2FreezeDiffStop` · `evap2FreezeProtect` · `evap2FreezeSetpoint` ·
`evap2ValveMode` · `evap3FanMode` · `evap3FreezeDiffRestart` · `evap3FreezeDiffStop` · `evap3FreezeProtect` ·
`evap3FreezeSetpoint` · `evap3ValveMode` · `evapHighLimit` · `evapLowLimit` · `evapTemp1Label` · `evapTemp2Label` ·
`evapTemp3Label` · `fanMode` · `intercambiadorMode` · `resist1Mode` · `resist2Mode` · `resistHighLimit` ·
`resistTemp1Label` · `resistTemp2Label` · `resistanceTempThreshold` · `staggerDelay` · `startDelay` · `zoneHighLimit` ·
`zoneLowLimit` · `zoneTemp1Label` · `zoneTemp2Label`

## 4. Sizing PR11 honestly — three row classes (the Test column decides the work)
| Class | Slots | Row shape | Work |
|---|---|---|---|
| **A — pass-through facade slot** (DashboardPan-rt `*Mode`/`*Limit`/`*Setpoint`/`*Label` that only LINK to the logic) | ~35 of the 41 | Writer = Dashboard/write-server · Timing = next propagation · Invariant = "lands on the facade, propagates DOWN the link to `ColdRoom_N.<slot>`; never overwritten (facade is a link SOURCE, B823-G1)" · Test = the existing logic-side test for the target slot, or **`bog-nav links --from CuartoN --slot <s>`** as the structural pin | mostly cross-reference; ~1 line each |
| **B — logic config slot with a timing invariant** (ColdRoomPan `powerOnDelay`/`valveMode`/`fanMode`/`freezeProtect`; CompPan `minOn`/`stageDelay`/`stage{Up,Down}Delay`/`startProveDelay`/`suction*`/`floatingSuction`/`condenserNMode`) | ~20 | needs a mid-cycle invariant (what a write DURING a running timer/latch does) | a NEW pure-seam test each (the C8 w-series shape) — the real PR11 effort |
| **C — one-shot / label** (`faultReset`, `*Label`, `coolOnSensorFault`) | ~7 | Writer · Timing = any · Invariant = "momentary clear / display only" | 1 line each |
Estimate: ~20 new pure tests + ~44 cross-reference rows. Sequence PR11 AFTER R1 (S20) so its 2 rows are in scope, or have
S20 carry its own 2 rows and PR11 the other 62.

## 5. How to re-measure (the SC-9 command, exact)
```
git -C <client> worktree add /tmp/wt-a109249 a109249
for m in Paccadia/ColdRoomPan Compresores/CompPan Dashboard/DashboardPan; do
  bash toolbelt/lint-write-path.sh /tmp/wt-a109249/$m; echo "exit=$?"; done   # today: 6/15/41 FAIL rows, exit 1 each
```
Pin: after PR11, all three exit **0** with zero FAIL rows (and `lint-write-path.sh` on a root with NO matrix still exits 3 —
the ERROR path stays pinned by WP7).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | matrix exists at a109249 at the client root, 20 data rows | [CERT] | `git ls-tree a109249` + wc/grep |
| 2 | uncovered = 6 / 15 / 41 (exit 1 each); OPERATOR = 10 / 46 / 20 / 0 | [CERT] | lint-write-path.sh + module-find at a109249 |
| 3 | CompPan 15 matches the C8 retro; ColdRoomPan 6 ≠ 13 (matrix grew) | [CERT] | this run vs retro campaign8-write-path |
| 4 | SC-9 pin is exit 1→0, not 3→0 | [CERT] | lint exits 3 only when no matrix is found (WP7) |
| 5 | row-class sizing A/B/C | [INFER] | by slot name/kind; confirm per row at apply |
