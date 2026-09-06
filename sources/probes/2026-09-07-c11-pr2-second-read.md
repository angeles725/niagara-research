# C11 PR2 (T3 concept-row-drift) second read — feat/c11-concept-drift 48e2f6f

investigador1, 2026-09-07. Design-D2 checks + OBSERVED mutation attribution, verified in a worktree. `[ev: git 48e2f6f; reproduced bats + mutation runs]`

## Verdict: PASS on correctness. ONE finding: task 2.7's OBSERVED mutation is mis-attributed (names WP-drift-true-concept but flips WP-drift-decoy) — a K24(7) mapping fix. All three DRIFT code paths ARE pinned.

## Correctness — all PASS
- **Diff matches design D2**: `DRIFT=0` var; `:441` `[concept]` continue → `_is_concept` capture; covered branch emits DRIFT
  iff `_is_concept`; `[ "$_is_concept" -eq 1 ] && continue` true-concept skip; STALE emit byte-identical; exit
  `[FAILED]→1; --strict && (STALE||DRIFT)→1; else 0`. +23/−6, only the row pass + exit. `[ev: diff 48e2f6f]`
- **Covered set reused (check 1)**: the DRIFT branch reads the SAME `$_covered_flat` (built once at `:432`); no second
  harvest added. `[ev: diff — no new _covered_names]`
- **FAIL emit + exit-1 byte-identical (check 2)**: the uncovered-FAIL printf is untouched (not in the diff); `[ "$FAILED"
  -eq 1 ] && exit 1` unchanged. `[ev: diff]`
- **STALE unchanged (check 3)**: full `lint-write-path.bats` **22/22** green; real-tree ff1b659 CompPan-rt `--strict` =
  **STALE 5 / DRIFT 0** (5 unchanged from PR5); marked tree 00e7118 = **STALE 0 / DRIFT 0** (the 5 `[concept]` rows exempt
  STALE and are true-concept → no DRIFT). `[ev: bats + ff1b659/00e7118 runs]`
- **Drift suite**: `tests/write-path-drift.bats` **4/4** (WP-drift-neg covered→DRIFT exit0; WP-drift-strict →1;
  WP-drift-true-concept absent→silent; WP-drift-decoy commented-concept→silent). `[ev: bats]`
- **0 trailers**; RED d726148 cherry-picked as commit 1 (ancestor). `[ev: git log]`

## Decoy attribution (check 4) — the worker is CORRECT
OBSERVED mutation "drop the `:439` HTML-comment strip" → I ran it (python edit `_row=$_mline`): **WP-drift-decoy flips**
(the commented `[concept]` now sets `_is_concept=1` → a covered slot false-DRIFTs). Matches the worker's stated
attribution. `[ev: mutation run — drop strip → WP-drift-decoy]`

## Finding — task 2.7's OBSERVED mutation names the wrong fixture (K24(7))
Task 2.7: "delete the `if [ "$_is_concept" -eq 1 ]` **wrapper** so every covered row emits DRIFT → **WP-drift-true-concept**
negative case FALSE-DRIFTs." I ran the three edits:
| edit | flips |
|---|---|
| delete the covered-branch DRIFT guard (`if [_is_concept]; then DRIFT`) | **WP-drift-decoy** (not true-concept) |
| drop the `:439` HTML-comment strip | **WP-drift-decoy** |
| delete the true-concept skip (`[ "$_is_concept" -eq 1 ] && continue`) | **WP-drift-true-concept** |
So the wrapper deletion flips **WP-drift-decoy**, NOT WP-drift-true-concept. WP-drift-true-concept is pinned by a DIFFERENT
line — the true-concept skip (deleting it lets a concept+absent row fall through to a STALE emit). The good news: unlike
PR1's I2/I3, **every DRIFT path here IS pinned** (decoy by the covered-branch guard AND the strip; true-concept by the
skip) — nothing is unpinned. The only issue is the task's mutation→fixture MAPPING. Recommend splitting task 2.7 into two
correctly-attributed OBSERVED mutations: (a) delete the covered-branch DRIFT guard → **WP-drift-decoy** false-DRIFTs; (b)
delete the true-concept skip → **WP-drift-true-concept** emits STALE. `[ev: three mutation runs @ 48e2f6f; K24(7)]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | diff matches D2; covered set reused (no 2nd harvest); FAIL/exit byte-identical | [CERT] | diff 48e2f6f |
| 2 | 22/22 lint-write-path.bats + 4/4 drift; STALE 5 ff1b659 unchanged; DRIFT 0 at 00e7118 | [CERT-live] | bats + client runs |
| 3 | drop :439 strip → WP-drift-decoy flips (worker attribution correct) | [CERT] | mutation run |
| 4 | wrapper deletion flips WP-drift-DECOY not true-concept; true-concept pinned by the skip line | [CERT] | three mutation runs |
| 5 | 0 trailers; RED cherry-picked as commit 1 | [CERT] | git log |
Tally: 4 [CERT] · 1 [CERT-live] · 0 [INFER] · 0 unmarked.
