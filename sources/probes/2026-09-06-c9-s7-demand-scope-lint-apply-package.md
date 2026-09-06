# C9 S7 — `lint-demand-scope.sh` (demand-in-scope lint, B820): apply package for PR2 / R2

Author: companero (Fable), 2026-09-06. Contract extracted from the QA RED `qa/c9-demand-in-scope` **`2916954` → re-issued `d0f5942`** (QA aligned the setup path to `lint-demand-scope.sh`; row token `demand-in-scope` unchanged; file still `tests/demand-in-scope.bats`)
(`tests/demand-in-scope.bats`, DS1–DS7 + DS-smoke); rule from B820 §820.2–§820.4; skeleton = `lint-delays.sh` (the
src-dir single-profile shape the RED itself cites). Real-tree anchors re-read at client `a109249`. Neither script name
exists on niagara-tools main today (RED state confirmed). `[ev: qa/c9-demand-in-scope d0f5942 (was 2916954) tests/demand-in-scope.bats]`
`[ev: corpus B820 §820.2-820.4]` `[ev: kit toolbelt/lint-delays.sh]`

## 0. TWO DECISIONS FOR THE LEAD before apply (both are RED-vs-proposal conflicts; K13 says the RED wins, but the
## proposal/spec text must then be corrected, and one needs a QA re-issue)
| # | Conflict | RED (2916954) | Proposal (ba3432c) | Recommendation |
|---|---|---|---|---|
| D-a | **script filename** — **RESOLVED both sides** (2nd read, investigador1) | RED re-issued `d0f5942`: bats :25 `DIS="$KIT/toolbelt/lint-demand-scope.sh"` | proposal already `lint-demand-scope.sh` | Script = `toolbelt/lint-demand-scope.sh`; row token `demand-in-scope` (DS2) unchanged. Nothing left to decide. |
| D-b | **severity** — **RESOLVED** (proposal/spec corrected in niagara-tools d9855f8) | WARN-ONLY: DS2 asserts `WARN` + `status 0`; DS5 = `--strict` → 1; bats header "WARN-only (--strict -> FAIL)"; B820 §820.3 "never a hard FAIL" | capability :75 says "**FAIL** a step/staging body that computes a target with no zero-demand short-circuit; rows `FAIL\|WARN`" | RED + B820 are right (a pure-modulator block fed by an upstream demand gate would false-positive on FAIL). **Correct the proposal/spec capability text to WARN-only, `--strict` promotes to exit 1.** No RED change. |
`[ev: proposal.md :34, :75, :158]` `[ev: bats DS2/DS5]` `[ev: corpus B820 §820.3]`

## 1. The contract (verbatim from the RED)
- **CLI:** `lint-demand-scope.sh [--strict] <java-src-dir>` — ONE src dir, scanned recursively (the `lint-delays.sh`
  shape). **Not** a module-root: the RED never passes a root and DS-smoke copies ONE file into a flat dir. Profile
  discovery (the `lint-write-path.sh` mode) is OUT of this RED — it can only be an additive later flag, never a pin dependency.
- **Row:** `WARN  demand-in-scope  <file>:<line>  <method> reads <pv> with no demand-shaped input in scope`
- **Exits (K20 disjoint):** `0` no WARN, or WARN without `--strict` · `1` any WARN under `--strict` · `3` usage/env
  (no arg, or not a directory).
- **Dot-dir prune (D9b):** `find "$SRC" -type d -name '.*' -prune -o -name '*.java' -print` — DS7 also asserts the
  output never mentions `Stale` (the pruned file's class name).
- Fixtures: ALL INLINE in the bats (heredoc Java written to `$BATS_TEST_TMPDIR/src/com/x/`); there is NO committed
  `tests/fixtures/` dir for this lint — do not add one for the RED pins (a real-tree fixture set is a separate item, §5).

## 2. Pin-by-pin (what each RED test fixes)
| Pin | Fixture shape (inline) | Expect | It pins |
|---|---|---|---|
| DS1 | `step(long now, int demandCount, double suction, double suctionLowLimit)` with `if (demandCount <= 0) target = 0;` and a suction compare | exit 0, no `WARN` | demand as a PARAMETER + a gate → clean |
| DS2 (NAMED MUTATION §820.4) | same class, `step(long now, double suction, double suctionLowLimit)` — demand param removed, `if (suction < suctionLowLimit) target--` | exit 0, output contains `WARN` AND `demand-in-scope` AND `step` | the flag fires on the decidable ABSENCE; row names the method |
| DS3 | no demand param but `private int demandCount = 0;` class field, suction compare | exit 0, no WARN | scope = params **plus enclosing-class fields** |
| DS4 | `ModeSelect.pick(long now, int mode)` — no process variable read at all | exit 0, no WARN | the rule keys on a PV READ; no pv → never flag (over-trigger guard) |
| DS5 | DS2's mutant | plain run exit 0; `--strict` run exit **1** | `--strict` promotion |
| DS6 | no argument | exit **3** | usage guard |
| DS7 | `Clean.java` (demand+gate) under `src/com/x/` + `Stale.java` (pressure-only) under `src/.deploy-baseline/com/x/` | exit 0, no WARN, output has no `Stale` | D9b prune |
| DS-smoke | the REAL `CompPan-rt/…/CompressorControl.java` copied ALONE into a temp dir (skip if absent) | exit 0; `grep -c 'WARN.*step'` == 0 | the real `step(long now, int demandCount, …)` is clean (B820 §820.4 PASS) |

## 3. The detection algorithm (B820 §820.2 static key → implementable rules; same-method-body discipline from lint-timers-ext D4)
For every `*.java` under the pruned walk, for every METHOD (brace-balanced body — extract by counting `{`/`}` from the
signature line, never a ±N-line window):
1. **Control-decision method?** — the body assigns a control target/command: any of `target =`, `target -=`/`--`,
   `cmd[`, `setBool(`, `set<Cap>(…)`, `get<Cap>().setValue(`, or returns an `int`/`boolean` computed from a comparison.
   If not → skip the method.
2. **Reads a PROCESS VARIABLE?** — an identifier matching, case-insensitively, `suction|pressure|discharge|temp|cv|coil|head`
   used as an operand of `<`, `>`, `<=`, `>=` (a comparison, not a mere mention). If none → skip (DS4).
3. **Demand-shaped input IN SCOPE?** — the method's PARAMETER names/types **or the enclosing class's FIELD names/types**
   (DS3) contain one matching `demand|call|enable|loopEnable|count` (case-insensitive; B820's set
   `{demand*, *call*, enable, *count, loopEnable}`) **or** a parameter typed `BStatusBoolean` named `in…`. If present → clean.
4. Else → emit `WARN  demand-in-scope  <file>:<line-of-signature>  <method> reads <pv> with no demand-shaped input in scope`
   (one row per method, `<pv>` = the first matched pv identifier). Set `FAILED=1` only when `--strict`.
**Advisory caveat to record in the script header (not a pin):** the bare `*count` name (e.g. `onCount`) is the weakest
demand signal and can false-clean a method whose only "count" is a running-count — the RED does not exercise it; keep it
(B820's set) but order the match `demand* > *call* > enable/loopEnable > *count` and name the matched token in the row so
a reviewer sees which one cleared it. `[ev: corpus B820 §820.2-820.3]` `[ev: retro campaign8-lint-timers-ext D4]`

## 4. Skeleton to mirror (`lint-delays.sh`, verbatim shape)
```
#!/usr/bin/env bash
# lint-demand-scope.sh — demand-in-scope lint (Campaign 9 PR2, B820). [ev: retro campaign9-demand-scope]
# Usage: lint-demand-scope.sh [--strict] <java-src-dir>
# Row:   WARN  demand-in-scope  <file>:<line>  <method> reads <pv> with no demand-shaped input in scope
# Exits: 0 no WARN (or WARN without --strict) · 1 any WARN under --strict · 3 usage/env
# Dot-directories pruned (D9b). VCS-free by design (kit-links L2).
set -u
STRICT=0; [ "${1:-}" = "--strict" ] && { STRICT=1; shift; }
[ $# -ge 1 ] || { printf 'usage: lint-demand-scope.sh [--strict] <java-src-dir>\n' >&2; exit 3; }
SRC="$1"; [ -d "$SRC" ] || { printf 'lint-demand-scope: not a directory: %s\n' "$SRC" >&2; exit 3; }
WARNED=0
while IFS= read -r f; do
  # per-file: awk pass = method-body extraction + rules 1-4 → prints WARN rows
  out=$(awk -f <(…rules…) "$f"); [ -n "$out" ] && { printf '%s\n' "$out"; WARNED=1; }
done < <(find "$SRC" -type d -name '.*' -prune -o -name '*.java' -print | sort)
[ "$STRICT" -eq 1 ] && [ "$WARNED" -eq 1 ] && exit 1
exit 0
```
`shellcheck` 0; no `git` invocation anywhere (kit-links L2). `[ev: kit toolbelt/lint-delays.sh :31-45, :430-435]`

## 5. Real-tree smoke — TWO levels (lesson 11: presence-only pins are not a smoke)
- **RED pin (DS-smoke):** the single real `CompressorControl.java` → exit 0, zero `WARN.*step`. This is the minimum.
- **PR2 acceptance (proposal PR2 row: "real-tree smoke on ALL four client module roots with exact counts + subjects +
  absence"):** run on `ColdRoomPan-rt/src`, `CompPan-rt/src`, `DashboardPan-rt/src`, `DashboardPan-ux/src` at
  `a109249` and RECORD in the PR2 retro: the exact WARN count per root, the exact `<file>:<method>` subjects, and the
  ABSENCE pin `no WARN whose subject is CompressorControl.step` (the B820 PASS anchor: `step(long now, int demandCount,…)`
  gates on `demandCount` at the FASE-2 tail and the FASE-1 fallback `target = demandCount`). The other roots' counts are
  NOT known today (no tool yet) — measure at GREEN, do not predict; a count of 0 on a root is a legitimate result only
  with the raw scan attached. `[ev: corpus B820 §820.4]` `[ev: close-retro lesson 11]`

## 6. K19 routing + retro (same PR)
- `BUILD-LOOP.md §5` pre-gate list, beside `lint-wb-threading.sh` (the WARN-only-with-`--strict` precedent): ``` `toolbelt/lint-demand-scope.sh [--strict] <src>` (demand-in-scope: a control/staging method that reads a process variable with NO demand-shaped input in scope → WARN "pressure without demand"; exit 0 WARN-only / 1 under --strict / 3 usage) [ev: retro campaign9-demand-scope] ```
- `skill/SKILL.md` toolbelt list: same one-liner. `kit-links.bats` L1/L8 must resolve the new script name.
- Retro `retros/2026-09-0X-campaign9-demand-scope.md` via `new-retro.sh kit` (slug `campaign9-demand-scope`, ≥6 chars ✓);
  record the OBSERVED DS2 flip (mutation) and the §5 four-root counts. `[ev: retro campaign8-retro-loop]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | RED invokes `demand-in-scope.sh`; proposal names `lint-demand-scope.sh` | [CERT] | bats setup() :24; proposal :34/:75/:158 |
| 2 | RED is WARN-only (DS2 exit 0 + WARN; DS5 strict → 1); proposal :75 says FAIL | [CERT] | bats; proposal :75; B820 §820.3 |
| 3 | CLI is `<java-src-dir>`, fixtures inline, D9b + `Stale` absence | [CERT] | bats DS1-DS7 |
| 4 | real `step` has `demandCount` param + gate → clean | [CERT] | B820 §820.4; CompressorControl.java @ a109249 |
| 5 | four-root counts | [INFER] | to be measured at GREEN (§5) |
