# C10 PR4 (S24 cwd-independent runner) second read — feat/c10-cwd-independent-reds 6abfc42

investigador1, 2026-09-07. Four checks + OBSERVED mutation. This is the PR that came out of my tasks-read finding
(Edit 1 dropped → one edit). `[ev: git 6abfc42; reproduced run-pure-test.bats runs]`

## Verdict: clean PASS. The one-edit fix is correct and bites exactly; all four checks pass.

## The fix — one line at :62
`run-pure-test.sh` diff `61fa1f5..6abfc42` is a single line:
`- java -cp "$tmp:$JU:$HC" … "$testfqcn"` → `+ ( cd "$rt" && java -cp "$tmp:$JU:$HC" … "$testfqcn" )`. Nothing else in
the script changed. `[ev: diff 6abfc42 run-pure-test.sh]`

## Check 1 — no absolutise line anywhere — PASS
Grep for `cd … && pwd` / `rt=$(cd` across the shipped script → NONE. The dropped Edit 1 is truly absent. `[ev: grep 6abfc42:run-pure-test.sh]`

## Check 2 — retro exit-propagation reasoning — PASS (accurate)
`retros/2026-09-06-campaign10-run-pure-test-cwd.md` states: (a) the cwd-sensitive step is `java`, not `javac`
(`-sourcepath` resolves regardless of caller cwd); (b) under `set -euo pipefail` a subshell's exit code propagates
naturally as the script exit (preserves the exit-1-on-JUnit-failure bite) — CORRECT: `( … )` is the final command, so
`set -e` propagates its non-zero; (c) the subshell changes JVM cwd without touching the parent cwd or the
`trap 'rm -rf "$tmp"' EXIT` handler — CORRECT (trap is on the parent EXIT). It also records the absolutise line as
would-flip prose (matches my finding). `[ev: 6abfc42:retro; run-pure-test.sh:35 trap]`

## Check 3 — build-verify.md usage matches the script signature — PASS (and it FIXED a pre-existing doc bug)
Old text documented a WRONG 4-arg form `run-pure-test.sh <rt-dir> <pkg> <PureClass> <TestClass>`. The PR corrects it to
the real 2-arg signature `run-pure-test.sh <module-rt-dir> <pkg.TestClass>` — matching the script's own
`die 2 "usage: run-pure-test.sh <module-rt-dir> <pkg.TestClass>"` — and adds the cwd-independence note. Net improvement,
not just a compatible edit. `[ev: 6abfc42:build-verify.md:108; run-pure-test.sh:26]`

## Check 4 — 0 attribution trailers — PASS
Three commits in range (RED cherry-pick a91fa0e, fix 5204458, ticks 6abfc42); grep for
`co-authored-by|generated with|claude|anthropic` in bodies = 0. `[ev: git log 61fa1f5..6abfc42]`

## OBSERVED mutation — the fix bites (verified, not asserted)
Ran `tests/run-pure-test.bats` (junit present in ~/.gradle):
- PRISTINE (subshell fix): **8/8 green**, S24-cwd passes.
- MUTATION (revert to bare `java` in caller cwd): **S24-cwd FAILs** (test 7, `[ "$status" -eq 0 ]` fails), S24-cwd-regression stays green.
Exactly one mutation flips exactly the S24-cwd pin → task 4.7's single OBSERVED mutation is real, and the one-edit fix is
sufficient. My tasks-read finding (Edit 1 inert, one edit suffices) is fully vindicated by the shipped PR. `[ev: bats pristine vs reverted]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | diff is one line at :62; no absolutise line anywhere | [CERT] | diff + grep 6abfc42 |
| 2 | retro exit-propagation/trap/javac-vs-java reasoning accurate | [CERT] | 6abfc42:retro + script |
| 3 | build-verify.md now the real 2-arg signature (fixed a 4-arg doc bug) | [CERT] | build-verify.md:108; run-pure-test.sh:26 |
| 4 | 0 attribution trailers in the PR range | [CERT] | git log bodies |
| 5 | revert subshell → S24-cwd FAILs, regression green; pristine 8/8 | [CERT] | bats mutation run |
Tally: 5 [CERT] · 0 [INFER] · 0 unmarked.
