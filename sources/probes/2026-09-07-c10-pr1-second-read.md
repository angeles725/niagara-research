# C10 PR1 (S21 lint-timers) second read — feat/c10-lint-timers-scope db97d76

investigador1, 2026-09-07. Diff vs design/RED at 52ebd11 + independent real-tree smoke + OBSERVED guard-attribution
runs. `[ev: git db97d76; reproduced lint runs @ ff1b659 + synthetic fixtures]`

## Verdict: fix is CORRECT and PASSES its smoke — but the depth guard is UNPINNED (durability gap). One pin to add.

## The fix (diff read) — matches the design
`lint-timers.sh` companion-flag check rewritten in 3 phases (replacing the old `:143-186` Pass 1):
- **Phase 0** comment strip (`//` + `/* */`, line numbers preserved).
- **Phase 1** class-scope FIELD collection — a `boolean|int|long` declared at `prev_depth == 1` is a FIELD; the same at depth ≥ 2 is a LOCAL and is NOT a candidate. `[ev: D1c]`
- **Phase 2** method-boundary parser ported from lint-silent-protection.sh:250-320, WITH the `brace_depth >= 2` guard (accept a method-open only when post-open depth ≥ 2, so the class body at depth 1 is never named a method). `[ev: D1b]`
- **Phase 3** same-method binding — FAIL only when a `Clock.schedule*` and an `X = true` share one `[meth_start,meth_end]` and `X in fields`. `[ev: D1d]`
- Pass 2 (stopped/started clear) unchanged. 15/15 bats green pristine.

## Independent real-tree smoke @ ff1b659 — VERIFIED (not just reported)
| Module | OLD (f90b8d1) | NEW (db97d76) | |
|---|---|---|---|
| ColdRoomPan-rt | FAIL companion-flag `anyNoHardware` (BDefrostController.java) exit 1 | **exit 0, anyNoHardware ABSENT** | flip ✓ |
| CompPan-rt | exit 0, 0 companion rows | exit 0, 0 companion rows | UNCHANGED ✓ |
| DashboardPan-rt | exit 0, 0 companion rows | exit 0, 0 companion rows | UNCHANGED ✓ |
| DashboardPan-ux | exit 0, 0 companion rows | exit 0, 0 companion rows | UNCHANGED ✓ |

## FINDING — the `brace_depth >= 2` depth guard is UNPINNED, and task 1.10 mis-attributes the S21-neg flip
The worker's claim is CORRECT and I confirmed it by OBSERVED mutation of the shipped script:
- **Mutation A — drop the depth guard** (`brace_depth > old_d && brace_depth >= 2` → `> old_d`): **all 15 lint-timers.bats stay green**; S21-neg stays CLEAN. The depth guard is caught by ZERO fixtures in 52ebd11.
- **Mutation B — locals-as-fields** (Phase 1 `prev_depth == 1` → `>= 1`): **S21-neg flips to FAIL** (`anyNoHardware`). This is the guard that actually cleans S21-neg.
So task 1.10's OBSERVED mutation — "drop brace_depth ≥ 2 guard → anyNoHardware re-FAILs (S21-neg flips)" — is **mis-attributed**: that flip is produced by the FIELD-scope drop, not the depth-guard drop. As written it cannot be produced as claimed.

## …but the depth guard is REAL / load-bearing (NOT inert like PR4 Edit 1) — so it needs a PIN, not removal
Synthetic fixture in the client's actual shape (annotation `defaultValue = "new BAlarmRecord()"` before a brace-only class
open — BCompressorControl.java:442 before class `{` :449), with the field set in one method and the schedule in a
DIFFERENT method (correct answer = CLEAN):
- WITH depth guard: **exit 0 CLEAN** (correct).
- WITHOUT depth guard: **FALSE FAIL** — the class body is mis-named one giant method ("BAlarmRecord" via the Case-B
  backward scan), collapsing armA/armB, so `startingUp=true` and `Clock.schedule` bind across methods.
So a future refactor could silently drop the guard and reintroduce the client-shape FP with ZERO test failure.

### Proposed pin (drop into the lint-timers RED — PR1 follow-up; PR3's silent-protection RED does NOT cover lint-timers.sh's own copy)
```java
// S21-misparse: annotation with an identifier( before a brace-only class open must NOT
// let the class body be named a method; field(armA) + schedule(armB) in DIFFERENT methods = CLEAN.
@NiagaraProperty( name = "ackAlarm", type = "BAlarmRecord", defaultValue = "new BAlarmRecord()" )
public final class BMisparse extends BComponent
{
  private boolean startingUp = false;
  private Clock.Ticket t;
  public void armA() { startingUp = true; }
  public void armB() { t = Clock.schedule(this, BRelTime.makeSeconds(5), exp, null); }
  public void stopped() throws Exception { super.stopped(); if (t != null) { t.cancel(); t = null; } }
}
// assert exit 0 + no companion-flag; FAILs iff the depth guard is dropped.
```
And re-label task 1.10 as TWO mutations: (a) drop field-scope guard → S21-neg re-FAILs (confirmed real); (b) drop depth
guard → S21-misparse false-FAILs.

## Does this block PR1? No — the fix is correct and the smoke passes. It is a DURABILITY gap (unpinned load-bearing guard).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 3-phase rewrite matches D1b/D1c/D1d; 15/15 green pristine | [CERT] | diff db97d76; bats run |
| 2 | ColdRoomPan-rt 1→0 anyNoHardware absent; CompPan/Dashboard unchanged | [CERT-live] | OLD vs NEW runs @ ff1b659 |
| 3 | drop depth guard → all 15 bats stay green (unpinned); S21-neg stays clean | [CERT] | Mutation A full-suite run |
| 4 | field-scope drop → S21-neg FAILs (this is the guard that bites it) | [CERT] | Mutation B run |
| 5 | depth guard IS load-bearing: BMisparse clean w/ guard, false-FAIL without | [CERT] | synthetic misparse-shape run |
Tally: 4 [CERT] · 1 [CERT-live] · 0 [INFER] · 0 unmarked.
