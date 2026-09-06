# C11 design.md validator read — kit design.md (post-6c7ec4f) — 7 gates, all with shell

investigador1, 2026-09-07. The design executor had NO Bash ([CERT-read]/[INFER] cites); I verified every falsifiable
claim against kit main source. `[ev: git @ kit main; design.md]`

## Verdict: PASS on all 7 gates. Both my proposal findings (size floor, T4 vacuity) are folded. Tasks can launch.

## Gate 1 — D1 fragment = function-only awk in `$MB_AWK`; three consumption seams — PASS
- lint-timers.sh:141 `_cf=$(awk '` = INLINE awk ✓; lint-ext-writable-shape.sh:61 `out=$(awk -v FILE="$f" '` = INLINE ✓;
  lint-silent-protection.sh:205 `cat > "$_TMP/main.awk" << 'AWKEOF'` = QUOTED heredoc → main.awk, run at :533-538 via
  `-f "$_TMP/main.awk"` ✓.
- The heredoc delimiter is **single-quoted** (`<< 'AWKEOF'`) → shell CANNOT interpolate `$MB_AWK` into it. The design's
  fix (printf `$MB_AWK` to a second `.awk`, add a **second `-f`** at :538; inline consumers use adjacent-quote
  `"$MB_AWK"' … '`) is the correct handling. mb_strip/mb_parse are FUNCTION-only, "no globals, no I/O, locals as trailing
  params" (design :101) — so one library serves all three mechanisms. `[ev: lint-*.sh @ kit main; design D1/D1b]`

## Gate 2 — sourcing via `${BASH_SOURCE[0]%/*}` not `$KIT` — PASS
retro-loop.bats:26-27 = `nr() { run env -C "$TK" KIT="$TK" "$NR" …}` / `kt() { run env -C "$TK" KIT="$TK" …}` — CONFIRMED
the writable-target seam: `$KIT` is repointed to `$TK` under retro-loop, so a `$KIT/lib/…` source would load from the test
target. `${BASH_SOURCE[0]%/*}` binds to the actually-running script's dir — correct and robust. `[ev: retro-loop.bats:26-27; design D1c]`

## Gate 3 — PEAK open `max_d > old_d && max_d >= 2`, close same iteration → one-liner `[i,i]` — PASS
Design D1e: `old_d`=before, `brace_depth`=after (NET), `max_d`=peak. Open ⟺ `max_d > old_d && max_d >= 2`, `m_dep=max_d`;
a one-liner has `max_d=old_d+1≥2` (open at i) and `brace_depth` returns to `old_d < m_dep` (close test same iteration →
close at i) → `[i,i]`; mb_parse contract `m_end == m_start for a one-liner` (:99). Coherent. `[ev: design D1e; mb_parse spec :96-100]`

## Gate 4 — lint-timers parser block :188-235 (not :188-202); usage exit 2 (not 3) — PASS (design CORRECTS the spec)
- Parser block: the open gate is :202 but the close test + array write run to :233-235 (`if (in_m && brace_depth < m_dep)
  { meth_start[n_meth]=… } }`). So the block is **:188-235**, not the spec/proposal's ":188-202". Design is right.
- Usage exit: lint-timers.sh:44 `# Exit: 0 no FAIL · 1 any FAIL · 2 usage · 3 env`; :58 `exit 2` (usage); :63 `exit 3`
  (env). lint-timers' **usage exit is 2**, not 3. IMPLICATION worth carrying to T4/close: the cross-cutting K20 line reads
  "usage → 3" generically, but lint-timers uses 2 for usage / 3 for env — any K20 assertion over lint-timers must not
  assume 3=usage. The design surfaced it; make sure the close/T4 gate honors it. `[ev: lint-timers.sh:44/:58/:63/:233-235]`

## Gate 5 — ext-writable inline `_scan_writes` → array iteration −42/+9; PR1 ≈663, ceiling 700 — PASS (folds my size finding)
D1k line-items exactly the B832 D6/D7 reconciliation I flagged: method-boundary.sh +150; lint-timers −48/+9; silent
−72/+14; **ext-writable −42/+9** (the inline `_scan_writes` at :179-184 → a 7-line array iteration over `mb_parse`
output, gated by `mn[k] in do_methods`); fixtures ~300; **total ≈663**, authored ≈353; **size:exception re-scoped to a
700 ceiling**, re-request with the measured number if exceeded, parser never split, fixtures never moved (RED-first). My
proposal Finding 1 is fully addressed. `[ev: design D1k/D1; B832 §3 D6/D7]`

## Gate 6 — D2 DRIFT = the `[concept]` branch inverted — PASS (truth table verified)
Anchors confirmed: lint-write-path.sh:441 `case "$_row" in *'[concept]'*) continue ;;` (the unconditional skip),
:432 `_covered_flat=…` (the covered set, reused not re-harvested). D2's plan captures `_is_concept` instead of
`continue`, then the covered branch emits DRIFT iff `_is_concept`, and a true concept row (`_is_concept` && not covered)
stays silent. Truth table checks out: concept+covered→DRIFT, concept+absent→silent, plain+covered→silent,
plain+absent→STALE. Exit expr `[FAILED]→1; --strict && (STALE||DRIFT)→1; else 0` keeps K20 {0,1}∪{3}, no new flag. Prose
`[concept]` stays silent via the existing `^[a-z][A-Za-z0-9]*$` slot-name filter. `[ev: lint-write-path.sh:432/:441; design D2a-e]`

## Gate 7 — D4 grammar/scope (9 lint-*.sh + itself); every retrofit fixture id exists — PASS (folds my T4-vacuity finding)
- Scope: `toolbelt/lint-*.sh` = exactly **9** (delays, demand-scope, ext-writable-shape, servlet, silent-protection,
  structure, timers, wb-threading, write-path); + itself = 10. Non-lint scripts out by rule (C12 seed for the 3 legacy
  free-prose NAMED MUTATION lines in slot-coverage/verify-module). `# Mutation:` today = **0 occurrences** (grammar is
  NEW).
- Anti-vacuity: D4 title "the RED must prove a positive MATCH"; D4a measures the real starting state; **D4e adds real
  `# Mutation:` headers, and I verified all 11 cited fixture ids exist at their exact lines** — S21-misparse@:436,
  S21-neg@:367, S23-pos@:188, S23-and@:270, EW-s22-neg2@:227, EW-s22-nondo@:245, WP-stale-perrow@:250,
  WP-stale-concept-decoy@:237, DS2@:51, LD1@:28, LSV1@:25. The grammar is matched against the real convention (leading
  @test token before the first colon), so "0 WARN over the kit" means "found and matched", not "found nothing". My
  proposal Finding 2 is fully addressed. `[ev: tests/*.bats id existence @ kit main; design D4a-e]`

## Self-verify
| # | Gate | Verdict | Evidence |
|---|------|---------|----------|
| 1 | D1 fragment/mechanisms + quoted-heredoc-no-interp | PASS | lint-*.sh:141/:61/:205/:533-538 |
| 2 | BASH_SOURCE not $KIT (retro-loop KIT=$TK seam) | PASS | retro-loop.bats:26-27 |
| 3 | PEAK open + close same iteration → [i,i] | PASS | design D1e; mb_parse spec |
| 4 | parser :188-235; usage exit 2 not 3 | PASS | lint-timers.sh:44/:58/:63/:235 |
| 5 | ext-writable −42/+9; ≈663; ceiling 700 | PASS | design D1k |
| 6 | D2 DRIFT invert :441; reuse :432; truth table | PASS | lint-write-path.sh:432/:441 |
| 7 | D4 scope 9+itself; 11 fixture ids exist | PASS | tests/*.bats existence |
Tally: 7 PASS · 0 FAIL. Markers: 7 [CERT].
