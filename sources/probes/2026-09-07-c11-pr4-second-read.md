# C11 PR4 (T4 lint-guard-pins meta-check) second read — feat/c11-lint-guard-pins 948afa8

investigator1, 2026-09-07. Grammar/ids + self-scan + mutations, verified in a worktree. `[ev: git 948afa8; reproduced runs]`

## Verdict: PASS on the tool (non-vacuous, 15 MATCH/0 WARN, all ids resolve). THREE dispositions: T4-smoke line-number brittleness (QA fixing), a pre-existing routing error (lint-timers "3 usage"), and the METHODOLOGY sub-bullet belongs in PR5 close.

## Tool correctness — PASS
- **Grammar** matches §D4c: `^# Mutation: [A-Za-z][A-Za-z0-9_-]* -- .+$` (ASCII `--`, no em dash); fixture lookup
  `@test "<id>[: ]` in `tests/*.bats`; scope = `toolbelt/lint-*.sh` only (D4b); D9b prune; exit 0/1(--strict)/3(usage). `[ev: lint-guard-pins.sh @ 948afa8]`
- **All 15 retrofit ids resolve** to a real `@test`: S21-neg, S21-misparse, S23-pos, S23-and, EW-s22-neg2, EW-s22-nondo,
  WP-stale-concept-decoy, WP-stale-perrow, **WP-drift-decoy** (added now PR2 is in), DS2, LD1, LSV1, LS7, WBT1c,
  T4-nomutation (guard-pins' own self-pin). `[ev: grep tests/*.bats]`
- **Real-kit self-scan** (`lint-guard-pins.sh <repo-root>`): **15 MATCH / 0 WARN / 10 distinct scripts / exit 0**. `[ev: run]`
- **Non-vacuous (validates my proposal Finding 2)**: I deleted lint-servlet's `# Mutation: LSV1` → self-scan emits
  **1 WARN on lint-servlet**, `--strict` → **exit 1**; restore → 0 WARN. So "0 WARN over the kit" means "found and
  matched", not "found nothing". `[ev: real-kit mutation run]`
- **K13 revert held**: `tests/lint-timers.bats` is byte-identical to main (the worker's trim of the RED to fit the smoke's
  hardcoded line was reverted). `[ev: diff origin/main 948afa8]`
- **guard-pins.bats**: T4-usage/match/nofixture/nomutation/scope/strict all green (these pin the bad-id, no-mutation, and
  scope behaviours). **0 trailers; no new conflict markers.** `[ev: bats]`

## Disposition 1 — T4-smoke hardcodes a fixture line number (QA fixing) — CONFIRMED
T4-smoke asserts the substring `mutation S21-misparse -> tests/lint-timers.bats:436`. After the K13 revert restored
lint-timers.bats, S21-misparse is no longer at `:436`, so **T4-smoke fails on the `:436` substring only** — its MATCH-count
assertions (≥10 MATCH, 10 distinct scripts, 0 WARN, exit 0) all pass. QA's re-pin (assert the id resolves, drop the line
number) is right. Broader lesson: a smoke that hardcodes a fixture LINE is brittle by construction — a K13 breach was even
attempted to keep the number stable. `[ev: guard-pins.bats:81-87; self-scan]`

## Disposition 2 — the BUILD-LOOP routing calls lint-timers "3 usage" (pre-existing, wrong) — minor
The pre-gate routing documents `lint-timers.sh … exit 0 clean / 1 any FAIL / 3 usage`. But lint-timers uses **exit 2 for
usage** (`:58`), 3 for env (`:63`) — `# Exit: 0 no FAIL · 1 any FAIL · 2 usage · 3 env` (`:44`). PRE-EXISTING (both sides
of the PR4 diff carry it; PR4 only appended the guard-pins entry to the same line), so NOT a PR4 regression — but it is
exactly the K20 per-lint nuance task 4.9 protects, and PR4 already edits this line via fragment-merge. Recommend fixing
`lint-timers … 3 usage` → `2 usage / 3 env` in the same edit (or at close). `[ev: BUILD-LOOP.md routing; lint-timers.sh:44/:58/:63]`

## Disposition 3 — the METHODOLOGY K24(7) sub-bullet belongs in PR5 close, not this feature PR
PR4 adds a numbered `K24 (7) guard-pin grammar` sub-bullet to METHODOLOGY.md. That is METHODOLOGY doctrine, and the C10
precedent is unambiguous: **K24 and all its sub-bullets landed in the C10 CLOSE PR (15239ff / dab0807), folded from
retros — never in a feature PR.** The sub-bullet also cites `[ev: retro campaign11-lint-guard-pins]`, a retro that is still
PENDING (it folds at close). Landing a K-lesson mid-feature-PR (a) breaks the "K-lessons fold at close" pattern, (b) cites
a not-yet-folded retro, and (c) re-edits METHODOLOGY.md, which PR5 close will edit again (fragment-merge risk). The tool's
own header (lines 3-19) already documents the grammar for feature-PR readers — that is the feature-appropriate doc.
**Recommend: move the K24(7) sub-bullet to PR5 close** (fold it from the campaign11-lint-guard-pins retro, as C10 did). `[ev: METHODOLOGY diff 948afa8; C10 K24 @ 15239ff]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | grammar §D4c; 15 ids resolve; self-scan 15 MATCH/0 WARN/10 scripts/exit 0 | [CERT] | tool + runs |
| 2 | non-vacuous: delete a lint's mutation → 1 WARN + --strict exit 1 | [CERT] | real-kit mutation |
| 3 | K13 revert held (lint-timers.bats == main); 0 trailers; no markers | [CERT] | diff + git grep |
| 4 | T4-smoke fails only on the :436 substring; count assertions pass | [CERT] | bats + self-scan |
| 5 | BUILD-LOOP says lint-timers "3 usage" (pre-existing); actual is 2 usage/3 env | [CERT] | routing + lint-timers.sh:44/58/63 |
| 6 | C10 K24 landed at close (15239ff), not a feature PR → sub-bullet belongs in PR5 | [CERT] | git log METHODOLOGY.md |
Tally: 6 [CERT] · 0 [INFER] · 0 unmarked.
