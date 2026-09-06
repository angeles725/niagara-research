# C10 close PR7 second read — kit chore/c10-close 15239ff (v0.21.0)

investigador1, 2026-09-07. Diff vs kit main 2f3300f. Every close-checklist item + two doc-accuracy findings. `[ev: git 15239ff]`

## Verdict: PASS to merge (fixes/version/folds all correct). TWO documentation-accuracy findings to fix before the release CHANGELOG/K24 are final — neither blocks the code.

## Finding 1 (CHANGELOG factual error) — the S26 bullet mis-states the client .gitignore patterns
CHANGELOG [v0.21.0] S26 bullet: "`.gitignore` extended with `**/*.class` and `**/tmp/` patterns". The ACTUAL PR6
patterns (git show 00e7118 vs ff1b659, and my PR6 read fec2ad064) are **`**/build/classes/`** and **`**/build/tmp/`**.
The stated patterns are BROADER than reality — `**/*.class` ignores every `.class` anywhere, `**/tmp/` ignores any `tmp/`
dir — so the CHANGELOG describes a more aggressive ignore than what shipped. Fix the bullet to the two actual
`**/build/…` patterns. `[ev: client .gitignore @ 00e7118; CHANGELOG.md S26 bullet @ 15239ff]`

## Finding 2 (K24 / close retro omit the Case-B `@`-stop) — matches the retro but not your description
K24 wording MATCHES the close-process retro's 7 recorded lessons exactly (verified line-by-line). But neither K24 nor the
retro records "the parser Case-B scan stops at an `@` line" that you named as expected. K24 item 6 covers the OLD bug
(forward brace-walk fires false on `@NiagaraProperty(`) + the section-D parser + `brace_depth>=2` guard — NOT the NEW
Case-B BACKWARD-scan `@`-stop (the exact mechanism that made the single-vs-multi-line S21-misparse pin distinction, my
PR1-pins finding + B832 §4.2). It is captured in B832, so not lost — but if you want it in K24, item 6 needs one clause.
`[ev: METHODOLOGY.md K24 @ 15239ff; close-process retro; B832 §4.2]`

## Everything else — PASS
- **VERSION** `0.21.0`. ✓
- **CHANGELOG six bullets, each names its tool**: `lint-timers.sh` (S21) / `lint-ext-writable-shape.sh` (S22) /
  `lint-silent-protection.sh` (S23) / `run-pure-test.sh` (S24) / `lint-write-path.sh` (S25) / client `.gitignore`+matrix
  (S26). `c10-close.bats` CLOSE-changelog ENFORCES all five kit tool names (grep per S21-S25). ✓
- **K24 wording ↔ retro**: 7 lessons, byte-consistent between K24 and the close retro table/list; pin-attribution rule is
  item 7 ("every OBSERVED mutation names the fixture it flips; QA confirms; a fixture in a note must match the shape of
  its proof"). ✓
- **No doctrine duplication for S24/S25**: the only doctrine files touched are `types/logic.md` (S23 Pattern B) and
  `types/logic-authoring.md` (S22 per-slot). S24/S25 doctrine folded in PR4/PR5; the close only flips their retro status.
  K24 items 2/3 (per-row, matrix-root) are META process lessons, not a re-fold of the S25 doctrine. ✓
- **Doctrine fold [ev:] count**: `logic.md` and `logic-authoring.md` each carry exactly one `[ev: retro campaign10-…]`
  (alongside the pre-existing campaign9 ev). ✓
- **Retro flips + agreed notes**: 5 retros pending→folded; ext-writable carries the "depth guard redundant given the
  do_methods gate; kept defensive" note (my PR2 finding); silent-protection carries the "SP-smoke 1→0 module-wide; S23-and
  pins the AND" attribution. Each note has one campaign10 ev. ✓
- **Promotion trailer**: six bare retro ids (lint-timers-scope, ext-writable-per-slot, silent-protection-pattern-b,
  run-pure-test-cwd, write-path-stale, close-process-meta-lessons). ✓
- **0 attribution trailers** across the range. ✓
- **Three default retargets**: `c9-close.bats`, `demand-in-scope.bats`, `ext-writable-shape.bats` each a109249→
  main-ff1b659 (the blessed read tree). ✓
- **BUILD-STATE flip**: `retro_pending: true→false` (correct 6 retro names); the PR2 session line is PRESENT (the
  fragment-merge drop + pre-push-hook stamp fix held); last_session = C10 CLOSE v0.21.0. ✓
- **c10-close.bats freeze**: VERSION_TARGET 0.21.0, TAG v0.21.0, tool-pins = C9 set + run-pure-test; 12/12 skip until
  C10_CLOSE set at freeze (as designed). ✓

## Note (not a finding)
The close commit body records `bats tests/ 417/418` with WP-stale-smoke skipping ("main-ff1b659 advanced to 00e7118
post-PR6"). The three retargets point at ff1b659 (STALE=5), not 00e7118 (STALE=0, marked); lint-write-path.bats's own
smoke default was not retargeted. Both real-tree facts hold (5 at ff1b659, 0 at 00e7118 — verified in PR5/PR6 reads); the
skip is pre-existing and outside the three retargeted files. Worth a one-line confirmation from QA at freeze that the skip
is intended, not a lost pin.

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | CHANGELOG S26 says `**/*.class`+`**/tmp/`; actual is `**/build/classes/`+`**/build/tmp/` | [CERT] | CHANGELOG @ 15239ff vs client .gitignore @ 00e7118 |
| 2 | K24 matches the retro's 7 lessons but neither records the Case-B @-stop | [CERT] | K24 + close retro @ 15239ff |
| 3 | VERSION 0.21.0; 6 CHANGELOG bullets each name their tool; bats enforces 5 | [CERT] | VERSION, CHANGELOG, c10-close.bats |
| 4 | doctrine folds one campaign10-ev each; S24/S25 not re-folded; 6-id trailer; 0 trailers | [CERT] | logic*.md, trailer, git log |
| 5 | three retargets a109249→ff1b659; BUILD-STATE retro_pending false + PR2 line present | [CERT] | test diffs, BUILD-STATE |
Tally: 5 [CERT] · 0 [INFER] · 0 unmarked.
