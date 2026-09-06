# C11 tasks.md structural read — kit 062f559 (54 items, PR1-PR5)

investigador1, 2026-09-07. Five armed checks. `[ev: git @ kit main 062f559; tests/*.bats existence]`

## Verdict: PASS. All five checks hold. Two minor accuracy notes (neither blocks PR1).

## Check 1 — final RED tips per PR — PASS (one note)
PR1 `qa/c11-parser-oneliner` **d88af78** + `qa/c11-golden-parser` **ed2088f**; PR2/T3 `qa/c11-concept-drift` **77352a7**;
PR3/T2 `qa/c11-client-root` **54078f6**; PR4/T4 `qa/c11-guard-pins` **ebc15e8**. All cited, re-read at apply (K13).
**Note**: PR5/close cites `qa/c11-close-checklist` as "skeleton" with NO tip SHA — the lead named **97624cb**. Per K13 a
skeleton still has a current tip to re-read; cite 97624cb (the TODO(freeze) pins are filled at close, but the skeleton tip
should be pinned now). `[ev: tasks.md PR1-PR5 RED lines]`

## Check 2 — PR order T1 → (T3 ∥ T2) → T4 → close — PASS
Chain "PR1 → PR2/PR3 (parallel after PR1) → PR4 → PR5"; PR2=T3 "touches toolbelt/lint-write-path.sh only, no tests/lib
overlap", PR3=T2 "touches tests/ only, no toolbelt overlap" → the parallelism is justified by disjoint files. PR4 depends
PR1-PR3, PR5 depends PR1-PR4, INDEX pending=0 before PR5. `[ev: tasks.md §Chain; PR2/PR3 notes]`

## Check 3 — 700 ceiling measured — PASS
PR1 header "~663; size:exception ceiling 700"; the PR1 **[lead] gate measures it**: "`git diff --stat` measured ≤700"
(and re-request-with-measured-number if exceeded, per the design). `[ev: tasks.md PR1 lead gate]`

## Check 4 — retrofit fixture ids in PR4 4.5 — PASS (expanded 11→13; all verified) + K20 nuance in 4.9
Task 4.5 retrofits **13** `# Mutation:` ids across the 9 lints (+ GP-pos on guard-pins itself = 14 lines / 10 scripts) —
the design D4e's 11 PLUS **LS7** (`lint-structure.bats:44`) and **WBT1c** (`lint-wb-threading.bats:32`), which are needed
so T4-smoke reaches **10 MATCH / 0 WARN over all 10 lints** (D4e as I validated only covered 7 lints/11 ids). I verified
the 2 new ids exist — both are even labeled `(mutation target)` by QA. So all 13 exist. **Accuracy note**: 4.5 says "11
fixture ids verified at the base by investigador1 `eae61fb27`" but lists 13 — LS7 and WBT1c were OUTSIDE my eae61fb27
verification (I've now confirmed them here). Reword to "13 ids: 11 at eae61fb27 + LS7/WBT1c verified separately."
Task **4.9 correctly carries the K20 per-lint nuance**: "lint-timers.sh uses 0/1/2 usage/3 env (:58/:63 — do NOT assert
usage=3 for lint-timers in a generic check)", citing eae61fb27. `[ev: tests/lint-structure.bats:44, lint-wb-threading.bats:32; tasks.md 4.5/4.9]`

## Check 5 — no inverted T3, no generic K20 usage→3 for lint-timers — PASS
Every T3 (PR2) sentence has the correct inverse semantics: 2.1 "WP-drift-neg exits 0 + 1 DRIFT row" (concept + covered →
DRIFT), "WP-drift-true-concept 0 DRIFT" (concept + absent → silent); 2.4 DRIFT grammar "slot `<name>`: concept marker but
a source slot exists" (DRIFT iff the slot EXISTS in source); 2.7 mutation flips WP-drift-true-concept to a FALSE DRIFT.
No backwards sentence. And 4.9 explicitly forbids the generic "usage → 3" for lint-timers. `[ev: tasks.md PR2 2.1-2.7; 4.9]`

## Self-verify
| # | Check | Verdict | Evidence |
|---|-------|---------|----------|
| 1 | 5 RED tips cited (close = skeleton, 97624cb not pinned) | PASS (note) | tasks.md RED lines |
| 2 | order T1 → (T3 ∥ T2) → T4 → close, disjoint-file parallelism | PASS | §Chain + PR2/PR3 notes |
| 3 | 700 ceiling measured at PR1 lead gate | PASS | PR1 lead gate |
| 4 | 13 retrofit ids (11+LS7+WBT1c) all exist; K20 nuance in 4.9 | PASS (attribution note) | bats existence; 4.5/4.9 |
| 5 | T3 semantics correct throughout; no generic lint-timers usage→3 | PASS | PR2 2.1-2.7; 4.9 |
Tally: 5 PASS · 0 FAIL · 2 minor notes. Markers: 5 [CERT].
