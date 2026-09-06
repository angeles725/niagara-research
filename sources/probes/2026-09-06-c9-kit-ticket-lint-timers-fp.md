# Kit ticket + S21 seed — lint-timers `companion-flag` false positive

Author: companero (Fable), 2026-09-06. Routes the Part-B finding of `2026-09-06-c9-issue-repo-hygiene-timer-lint.md` (a KIT
defect, not a client bug) as (1) a kit ticket and (2) a C9 research-candidate seed for C10. `gh` IS present on this machine
(2.45.0), so `kit-ticket.sh` would OPEN a real GitHub issue — leave the filing to the lead/Cristian (same as every issue
draft). No code lands in C9 for this. `[ev: lint-timers.sh header lines 14-15]` `[ev: BDefrostController.java @ a109249]`

## 1. Kit ticket (ready to file)
**Title:** `lint-timers companion-flag false positive: pairs a method-LOCAL boolean with a Clock.schedule in a different method`

**Body:**
```
## Defect
`toolbelt/lint-timers.sh` companion-flag FAILs on a per-call METHOD-LOCAL boolean and pairs it with a Clock.schedule* that
lives in a DIFFERENT method. Two independent false-positive causes.

## Evidence (client Leon-Guanjuato @ a109249, pre-existing v2.0.7 / 14443c2 — reproduces the shape)
Paccadia/ColdRoomPan/ColdRoomPan-rt/src/com/angeles/ColdRoomPan/BDefrostController.java
  FAIL  companion-flag  … flag 'anyNoHardware' set beside Clock.schedule* not cleared in stopped()/started()
- `anyNoHardware` is a method-LOCAL: `boolean anyNoHardware = false;` at :718, ONLY declaration, inside
  `private void requestDefrostCycle()` (:713); set true :726, read :740. A per-call local resets each call — nothing to
  clear in stopped()/started().
- `requestDefrostCycle()` (:713-~750) contains NO Clock.schedule. The schedules are at :808/:810/:850 in OTHER methods —
  the rule paired the flag across a method boundary.
- The same file PASSes timer-ticket (its real Clock.Ticket fields ARE cancelled in stopped()), so the timer discipline is
  correct; only the heuristic misfires.

## Rule refinement
companion-flag must fire only when BOTH hold:
  (a) the flag is a CLASS FIELD (declared at class scope), NOT a `type name = …;` inside a method body; and
  (b) the paired Clock.schedule* is in the SAME method body (brace-scoped) as the flag assignment, not file-wide.
Either guard alone fixes this case; implement both.

## RED fixture (tests/lint-timers.bats — add)
- NEG: a class with a method-local `boolean armed=false;` assigned true, and a Clock.schedule*() in a DIFFERENT method,
  Clock.Ticket cancelled in stopped() → companion-flag must NOT FAIL (exit 0 / PASS).
- POS (regression guard): a class FIELD `private boolean armed;` set true in the SAME method as a Clock.schedule*() and never
  reset in stopped()/started() → companion-flag STILL FAILs (the true defect the rule exists for).

## Acceptance
`lint-timers.sh` on ColdRoomPan-rt/src @ a109249 → exit 0 (companion-flag no longer fires on anyNoHardware); both bats
cases green; TL1-TL4 + the C8 companion/jdk-thread/changed-sched cases not regressed.
```

**Invocation (the lead runs when filing):**
```bash
export PATH=/usr/bin:/bin:$PATH
KIT=/home/cristian/modulos_niagara_n4/niagara-tools/build-n4-module-kit \
  "$KIT/toolbelt/kit-ticket.sh" "lint-timers companion-flag false positive: pairs a method-LOCAL boolean with a Clock.schedule in a different method"
# gh present → gh issue create --repo <kit-remote> --label kit,from-run,campaign-9 (paste the Body above)
# gh absent → writes retros/tickets/<date>-<slug>.md (the offline fallback), exit 0
```

## 2. C9 research-candidate seed (S21) — for `campaign9-research-candidates.md` (investigador1's file)
Table row (header `| # | Item | Class | Value | Tract. | Seed | Note |`):
```
| S21 | lint-timers companion-flag false positive (require class-FIELD + same-method schedule) | KIT | Low-noise: unblocks a clean report-module on ColdRoomPan-rt; removes a spurious FAIL | High (bats + awk scope fix) | C10 | Evidence BDefrostController.java :718 local vs :808/:810/:850; not a client bug; kit ticket drafted 2026-09-06 |
```
KIT-section prose entry (if the file prefers prose under `## KIT`):
> **S21 — lint-timers companion-flag false positive.** The `companion-flag` heuristic FAILs on a method-local boolean and
> pairs it with a Clock.schedule in another method (BDefrostController.java @ a109249: local `anyNoHardware` :718 vs
> schedules :808/:810/:850). Fix = require a class FIELD + a same-method-body schedule. KIT, C10; no C9 code. `[ev: kit ticket 2026-09-06]`

C9 disposition: **doc-note only** (no C9 code) — mention in PR12/PR13 close notes that the FP is filed and seeded to C10.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | kit-ticket.sh signature + gh/fallback behaviour | [CERT] | kit-ticket.sh header @ kit main |
| 2 | companion-flag FP: local :718, no schedule in requestDefrostCycle, schedules elsewhere | [CERT] | grep/awk @ a109249 (issue draft §B) |
| 3 | candidates table header columns | [CERT] | campaign9-research-candidates.md:9 |
| 4 | gh present (so filing is a deliberate step) | [CERT] | `gh --version` 2.45.0 |
