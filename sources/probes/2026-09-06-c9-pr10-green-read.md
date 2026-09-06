# C9 PR10 (S19 ext-writable-shape lint) GREEN read — c9-pr10 942b3db vs kit main 9830ce7

investigador1, 2026-09-06. Read-only in `niagara-tools-worktrees/c9-pr10` @ 942b3db. bats + four-root smoke run.
`[ev: bats + module-find @ a109249]`

## Verdict
Functionally correct and GREEN (bats 11/11, smoke exactly 1/0/0/0). The lead's judgment call is confirmed with evidence:
the class-level "any @NiagaraAction" exemption is a COARSE proxy for "writing action" and produces a CONFIRMED false
negative on `BCompressorControl.faultReset`. Acceptable as the C9 approximation (parity with module-find.py; catches the
high-confidence case), but the retro/doctrine must name the gap and C10 should refine it. No code drift.

## Invariants — all PASS
| Invariant | Result | Cite |
|---|---|---|
| B823 rule: complex OPERATOR `BStatusNumeric/Boolean/Enum` w/o a writing action → WARN; plain double/boolean/BRelTime/enum clean | PASS | `lint-ext-writable-shape.sh:5-18` |
| multi-line `@NiagaraProperty` joined by paren balance (handles nested `@Facet`) | PASS | Pass 2 `:105-118` |
| CLI `[--strict] <src-dir>`; exits 0/1/3; ERROR row on no sources (K20/EW11); D9b prune; row grammar | PASS | `:35-54`, EW11 green |
| bats EW1-EW11 | PASS | 11/11 |
| four-root smoke @ a109249 = **1/0/0/0** (DashboardPan-rt 1 WARN BRoomPanel.setpoint; CompPan-rt/ColdRoomPan-rt/DashboardPan-ux 0) | PASS | run this session |
| K19 routing: BUILD-LOOP §5 (1) + both SKILL lists (2) | PASS | grep |
| report-module §5.6 beside §5b/§5.5 | PASS | `report-module.sh:330-365` |
| retro triple (retro file + INDEX row pending/3 + CHANGELOG) | PASS | `retros/2026-09-06-campaign9-ext-writable-shape.md`, INDEX `:90` |

## The exemption proxy — CONFIRMED false negative (lead's call), C10 refinement
The exemption is class-level: "a class that exposes ANY @NiagaraAction is exempt" (`:14-18`, Pass 1 `:71-73`), parity with
`tools/module-find.py ext-writable`. Evidence it is too coarse (module-find @ a109249):
- `BCompressorControl.faultReset` is `BStatusBoolean`, `Flags.SUMMARY|OPERATOR`, **COMPLEX** — squarely in the B823 hazard
  class (an external oBIX write to a complex slot → "Cannot translate"/silent-zero).
- It has **no action that writes faultReset**. The class's ONLY `@NiagaraAction` is `powerOnExpired()` (HIDDEN, unrelated).
- So the lint exempts faultReset **because an unrelated hidden action exists** → CompPan-rt scores 0 WARN, MISSING faultReset.
This is a false negative by the lint's own contract (a complex OPERATOR slot with no writing action should WARN). It is
ACCEPTABLE for C9 — the lint correctly catches the high-confidence case (BRoomPanel.setpoint, in a class with zero actions)
and matches the module-find proxy — but two things should follow:
1. **Doctrine/retro honesty**: state that the exemption is class-level and coarse; a complex OPERATOR slot with no
   slot-writing action, in a class that has ANY other action, is a KNOWN blind spot. The EW10 test comment "CompPan-rt 0
   (faultReset has an action)" is imprecise — faultReset has NO action; the CLASS has `powerOnExpired`. Reword so the
   exemption's coarseness is not hidden behind a per-slot phrasing.
2. **C10 refinement**: require an action whose BODY writes the specific slot (`setFaultReset(...)` / `.set(faultReset,…)`),
   i.e. the same field/slot→writer follow the S18 silent-protection lint already does — that would flag faultReset correctly.
`[ev: module-find slots --name faultReset @ a109249: BStatusBoolean SUMMARY|OPERATOR COMPLEX; actions = powerOnExpired (h)]` `[ev: lint-ext-writable-shape.sh:14-18,:71-73]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | bats 11/11; smoke 1/0/0/0; K19/report §5.6/retro triple present | [CERT] | run + grep |
| 2 | faultReset is complex BStatusBoolean OPERATOR, no writing action; class has only powerOnExpired (hidden) → exempted = false negative | [CERT] | module-find @ a109249 |
| 3 | exemption is class-level "any @NiagaraAction", parity with module-find ext-writable | [CERT] | script :14-18 |
Tally: 3 [CERT] · 0 [INFER] · 0 unmarked.
