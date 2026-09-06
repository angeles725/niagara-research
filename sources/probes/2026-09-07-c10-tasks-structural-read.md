# C10 tasks.md structural read — niagara-tools be5b588 (7 PRs / 88 items)

investigador1, 2026-09-07. Four armed checks + a RED-backed finding. Read-only. `[ev: git show be5b588; RED a792d7a; runner f90b8d1]`

## Verdict: PASS with ONE finding to fold before PR4's lead gate.

## Check 1 — every PR cites the FINAL RED tip — PASS
S21 `52ebd11` (PR1) · S22 `954ebd7` (PR2) · S23 `f981754` (PR3) · S24 `a792d7a` (PR4) · S25 `db130a7` (PR5, and the
row even carries the coordinator correction "tip is db130a7, not a56a72e") · S26 none/chore (PR6) · close `41bca42`
BASE `1fb63d6` (PR7). All match the tips investigador confirmed did not advance. `[ev: tasks.md :40/:64/:88/:111/:131/:173]`

## Check 2 — PR1 → PR2/PR3 parser dependency — PASS (parallel is correct)
Each PR independently ports the section-D method-boundary parser + the D1b `brace_depth >= 2` guard into its OWN file
from the C9 `lint-silent-protection.sh:250-320` baseline: PR1 into lint-timers.sh (1.5), PR2 into
lint-ext-writable-shape.sh (2.7), PR3 applies it in-place to lint-silent-protection.sh (3.4). No hard file dependency
(different files) → parallel-after-PR1 is right. Cross-refs consistent: PR1 1.5 says "the guard lands [in
lint-silent-protection.sh] in PR3", and PR3 3.4 does exactly that. Note (not a blocker): three independent ports of the
same parser risk drift; each is fenced by its own OBSERVED mutations, so acceptable. `[ev: tasks.md :48/:75/:96]`

## Check 3 — PR6 after PR5 — PASS
PR6 line :156 "Depends on: PR5 merged (the `[concept]` marks produce their OBSERVED evidence only once the STALE rule
exists)"; 6.7 flip uses "PR5's kit". PR7 depends PR1-PR5; :176 "PR6 is client and does not block PR7". Consistent. `[ev: tasks.md :156/:164/:176]`

## Check 4 — no task inherits the refuted `-sourcepath` claim — PASS (text is correct) …
PR4 rationale (:114) and tasks 4.4/4.5 all state it correctly: "the cwd-sensitive part is the RUNTIME test read —
WiringTests read Paths.get(src/…) relative to JVM cwd; `-sourcepath` is NOT broken; fix is runner-side." No task carries
the old "-sourcepath breaks at :59" failure mode. `[ev: tasks.md :114/:119/:120]`

## … BUT: FINDING — PR4 Edit 1 is provably inert and its OBSERVED gate (4.7-b) cannot produce a RED
Reading the S24 RED at **a792d7a** (`tests/run-pure-test.bats`), BOTH S24 tests invoke the runner with an **absolute**
`$RT = "$BATS_TEST_TMPDIR/mod-rt"` — `S24-cwd` from cwd=`$BATS_TEST_TMPDIR/elsewhere`, `S24-cwd-regression` from
cwd=`$RT`. Neither ever passes `$1 = .` or any relative arg.
- **Edit 2 alone** (`( cd "$rt" && java … )`, task 4.5) fixes both: `$tmp/$JU/$HC` in `-cp` are already absolute (mktemp
  -d / gradle-cache find, runner :34/:18-19 @ f90b8d1), so `-cp` survives the cd, and cwd=`$rt` makes the test's
  `Paths.get("src/…")` resolve. Mutation 4.7-a (revert the subshell) → `S24-cwd` FAILs. Real flip. ✓
- **Edit 1** (`rt=$(cd "$rt" && pwd)` absolutise, task 4.4) is **dead code given Edit 2's structure**: the only cd that
  happens is Edit 2's own `cd "$rt"` inside a subshell that starts at the caller cwd, so a relative `$rt` resolves
  correctly there anyway; and javac (`-sourcepath "$rt/src:$testroot"`, runner :39-41) runs OUTSIDE the subshell from the
  caller cwd, where a relative `$rt` also resolves. There is no pre-`cd` that a relative `$rt` must "survive". So
  reverting Edit 1 leaves BOTH S24 RED tests GREEN → **mutation 4.7-b ("revert Edit 1 → S24-cwd-regression FAIL when
  $1=.") cannot produce its required RED**. PR4's lead gate (:125) demands "2 OBSERVED mutation flips"; only one flips.
- **This is an SC-7 evidence defect, not a correctness bug** (Edit 1 is harmless). Under RED-first, an edit whose
  mandated OBSERVED mutation can't RED is the same failure class as "would-flip" prose.

### Recommendation
Make PR4 a **ONE-edit fix** (Edit 2, the java subshell, only). Drop Edit 1, drop mutation 4.7-b, and drop the "TWO edits"
framing in the :114 rationale and the Unit-4 table (:31). This is exactly what a792d7a pins. (If the team insists on the
defensive absolutise, QA would have to add a RED that actually FAILs on revert — but I could not construct one, because
Edit 1 is provably inert under Edit 2, so option-drop is the clean path.)

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 5 lint RED tips + close tip all final in tasks.md | [CERT] | tasks.md :40/:64/:88/:111/:131/:173 |
| 2 | PR2/PR3 parallel; each ports D1b independently; cross-refs consistent | [CERT] | tasks.md :48/:75/:96 |
| 3 | PR6 depends PR5; PR7 depends PR1-PR5; PR6 doesn't block PR7 | [CERT] | tasks.md :156/:176 |
| 4 | PR4 text states -sourcepath not broken (no refuted claim inherited) | [CERT] | tasks.md :114/:119/:120 |
| 5 | Both S24 RED tests use absolute $RT; Edit 1 inert; 4.7-b cannot RED → one-edit fix | [CERT] | a792d7a run-pure-test.bats; runner :34/:39-43 @ f90b8d1 |
Tally: 5 [CERT] · 0 [INFER] · 0 unmarked.
