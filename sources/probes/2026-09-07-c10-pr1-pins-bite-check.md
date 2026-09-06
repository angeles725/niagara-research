# C10 PR1 pins verification (0b97920) — S21-neg2 drop CONFIRMED; S21-misparse as committed does NOT bite the guard (my-error correction)

investigador1, 2026-09-07. OBSERVED runs of the shipped fix (0b97920, lint-timers.sh byte-identical to b37aff7),
the fix with the depth guard dropped, and the OLD lint (f90b8d1). `[ev: reproduced lint runs]`

## 1. S21-neg2 drop — CONFIRMED CORRECT
A PLAIN cross-method FIELD case (field set in armA, Clock.schedule in armB, **no** misparse-trigger annotation):
| script | verdict |
|---|---|
| OLD lint (f90b8d1) | CLEAN |
| FIX (0b97920) | CLEAN |
| FIX, depth guard dropped | CLEAN |
Clean on all three → it is structurally same-method-scoped pre-fix, nothing flips it, so it can be neither a RED-first
pin nor a guard mutation target. QA is right to drop it. `anyNoHardware` (same-method LOCAL) is covered by S21-neg.

## 2. S21-misparse bite — REFUTED for the committed fixture (it does NOT pin the depth guard)
QA committed my BMisparse with a **single-line** annotation:
`@NiagaraProperty( name = "ackAlarm", type = "BAlarmRecord", defaultValue = "new BAlarmRecord()" )`. OBSERVED:
| annotation form | FIX (0b97920) | FIX, depth guard DROPPED |
|---|---|---|
| **single-line** (committed) | CLEAN | **CLEAN** ← does not bite the guard |
| **multi-line** (defaultValue on its own line) | CLEAN | **FAIL(companion-flag)** ← bites the guard |

**Root cause:** the Case-B backward scan (`lint-silent-protection` section-D, ported into lint-timers) breaks the moment
it hits a line that starts with `@`. In the single-line form the `defaultValue = "new BAlarmRecord()"` sits ON the
`@NiagaraProperty(` line, so the scan breaks there and never reaches `BAlarmRecord(` — the class body is never mis-named,
guard or no guard → CLEAN both ways. Only when `defaultValue = "new BAlarmRecord()"` is on its **own** line (multi-line
annotation) does the scan reach it before any `@`, mis-name the class body, and produce the FALSE-FAIL that the depth
guard exists to prevent.

So the committed S21-misparse is RED-pre-fix / GREEN-post-fix (a valid RED-first fixture, RED via the OLD Pass-1 path) but
it does **not** exercise the Phase-2 depth guard — dropping the guard leaves it CLEAN. **The depth guard is still
unpinned.**

**My error, owned:** my PR1 note's "Proposed pin" CODE BLOCK was single-line, even though the empirical proof in that same
note used the multi-line shape. QA faithfully copied the single-line code. The fix is to use the multi-line annotation.

### Corrected S21-misparse fixture (this one bites: drop the guard → FALSE-FAIL)
```java
@NiagaraType
@NiagaraProperty(
  name = "ackAlarm",
  type = "BAlarmRecord",
  defaultValue = "new BAlarmRecord()"
)
public final class BMisparse extends BComponent
{
  private boolean startingUp = false;
  private Clock.Ticket t;
  public void armA() { startingUp = true; }
  public void armB() { t = Clock.schedule(this, BRelTime.makeSeconds(5), exp, null); }
  public void stopped() throws Exception { super.stopped(); if (t != null) { t.cancel(); t = null; } }
}
// assert exit 0 + no companion-flag; RED pre-fix; and drop-guard -> FALSE-FAIL (this is the pin the single-line form lost)
```
Optionally add a bite-assertion note so a future guard-drop is caught, not just the pre-fix mis-parse.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | plain cross-method clean on OLD/FIX/FIX-noguard → S21-neg2 correctly dropped | [CERT] | 3 runs |
| 2 | committed single-line S21-misparse: drop-guard stays CLEAN (does not pin the guard) | [CERT] | run |
| 3 | multi-line form: drop-guard → FALSE-FAIL (bites the guard) | [CERT] | run |
| 4 | mechanism: Case-B scan breaks at the `@` line, so single-line never reaches BAlarmRecord( | [CERT] | lint-silent-protection.sh section-D scan; both-form runs |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
