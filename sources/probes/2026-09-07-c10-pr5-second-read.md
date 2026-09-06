# C10 PR5 (S25 write-path STALE + --strict) second read — feat/c10-write-path-stale 386c54e (kit PR #96)

investigador1, 2026-09-07. Eight checks + real-tree smoke + a per-row emit mutation. Ran the bats in a real
niagara-tools worktree at 386c54e (a scratch copy breaks the upward matrix discovery). `[ev: git 386c54e; worktree bats; ff1b659 runs]`

## Verdict: PASS on the fix (22/22 bats, real-tree 5-STALE root-invariant). One MINOR durability gap: the per-row EMIT is pinned only by the client-dependent smoke.

## Checks 1-8
1. **STALE per-row + matrix-root (R-S25.1-3a)** — PASS. Loop emits one STALE per unmarked data row; `MATRIX_ROOT=dirname(dirname(MATRIX))` and the covered set is harvested matrix-root-wide → root-invariant.
2. **`[concept]` literal only + comment strip** — PASS. `_row=sed 's/<!--[^>]*-->//g'` first, then `case "$_row" in *'[concept]'*) continue`. A `[concept]` inside `<!-- -->` is stripped → does NOT exempt (WP-stale-concept-decoy green).
3. **covered set = props ∪ actions ∪ bog, multi-line harvest** — PASS. `grep -hoE 'name[[:space:]]*=[[:space:]]*"…"'` over all matrix-root Java catches every `@NiagaraProperty`/`@NiagaraAction` name (any flag, multi-line), ∪ `_bog_extra`. Real-tree proof: the matrix action rows (intervalExpired/forceDefrost) are NOT STALE; WP-stale-action/summary green.
4. **grammar STATUS-first with matrix line** — PASS. `STALE  lint-write-path  <MATRIX>:<line>  slot <name>: no source slot with that name` — same column order as the FAIL row (:412). Verified on the real tree (rows carry `:31/:32/:33/:36/:52`).
5. **uncovered FAIL path byte-identical** — PASS. The FAIL emit (`printf 'FAIL  lint-write-path  %s  slot %s: no matrix row\n'` :412) is NOT in the diff. Exit expression changed `exit "$FAILED"` → `[ "$FAILED" -eq 1 ] && exit 1; [ "$STRICT" -eq 1 ] && [ "$STALE" -eq 1 ] && exit 1; exit 0` — behaviourally identical for the FAIL path (FAILED∈{0,1}).
6. **restructured arg loop, exit-3 preserved** — PASS. No-arg → MODULE_ROOT empty → usage exit 3; unknown `-*` → exit 3; second positional → exit 3; non-dir root → exit 3; a dash-leading root exits 3 both old (not-a-dir) and new (unknown-opt). `--strict` accepted before OR after the root (WP-stale-strict uses `--strict <d>`; my real-tree used `<root> --strict`). WP-usage/WP7/WP9b (exit 3) green.
7. **real-tree 5 from three roots** — VERIFIED. `--strict` on CompPan-rt, DashboardPan-rt, ColdRoomPan-rt each → exit 1, **exactly 5 STALE, identical rows**: hoaMode@31, hoaMode@32, inhibit@33, freezeEnabled@36, hoaMode@52. Root-invariant; per-row (hoaMode ×3).
8. **0 trailers** — PASS.

## Finding (minor, durability) — the per-row EMIT count is pinned only by the skippable client smoke
Applying the C10 close-lesson (every load-bearing behaviour needs a hermetic pin), I mutated the emit to key by NAME
instead of ROW: on the real tree the count collapsed **5 → 3** (hoaMode dedupes) — so per-row IS load-bearing. But
**WP-stale-perrow did NOT flip** under that mutation (stayed green): it pins the *marker* per-row rule (a `[concept]` on
one row does not exempt a same-named row), which my emit-dedup mutation doesn't touch (the marked row is `continue`d
before the emit; only one unmarked row remains → 1 either way). The "5 not 3" emit-per-row count is pinned **only by
WP-stale-smoke**, which SKIPs without `C9_CLIENT_ROOT`. So in CI without the client tree, a regression to per-name emit
would pass silently. Recommend one hermetic fixture: **two UNMARKED rows with the same missing name → expect 2 STALE**
(per-name emit gives 1). Not merge-blocking — the behaviour is correct and the client smoke covers it when present.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 22/22 bats green in a real worktree (scratch copy breaks matrix discovery) | [CERT] | worktree bats |
| 2 | real-tree 5 STALE, root-invariant ×3 roots, per-row hoaMode×3, STATUS-first w/ :line | [CERT-live] | ff1b659 runs |
| 3 | FAIL emit not in diff; exit expr behaviourally identical for FAILED∈{0,1} | [CERT] | diff 386c54e |
| 4 | covered set catches actions (action rows not STALE); [concept] comment-stripped | [CERT] | real-tree + WP-stale-action/decoy |
| 5 | per-name emit mutation → real tree 5→3 (per-row load-bearing) but WP-stale-perrow doesn't flip → emit-per-row pinned only by skippable smoke | [CERT] | mutation run |
| 6 | arg-loop exit-3 preserved; --strict before/after root | [CERT] | WP-usage/WP7/WP9b + runs |
Tally: 5 [CERT] · 1 [CERT-live] · 0 [INFER] · 0 unmarked.
