# C11 PR1 (T1 shared method-boundary parser) second read — feat/c11-shared-method-boundary 6f39f8b

investigador1, 2026-09-07. Design-validated items + the I2/I3 question, verified with OBSERVED mutation runs in a
worktree at 6f39f8b. `[ev: git 6f39f8b; reproduced fragment-mutation runs]`

## Verdict: PASS on correctness. ONE finding: I2 and I3 are REDUNDANT-for-lint-output (like PR4's absolutise / I5), NOT load-bearing-unpinned — reclassify them out of the OBSERVED-mutation table (K24(7)).

## Design-validated items — all PASS
- **Fragment**: `lib/method-boundary.sh` is ONE shell var `MB_AWK` = awk **function** defs only (mb_strip, mb_parse), no
  BEGIN/END/pattern rules, all locals as trailing params (no globals/IO). ✓
- **Three mechanisms**: lint-timers.sh:49 + lint-ext-writable-shape source inline (`awk "$MB_AWK"' … '`); lint-silent-
  protection writes `printf '%s' "$MB_AWK" > …/method-boundary.awk` (:208) and runs a **second `-f`** (:476) — the correct
  handling of its single-quoted heredoc which cannot interpolate. ✓
- **Sourcing** via `${BASH_SOURCE[0]%/*}` (lint-timers.sh:49, silent-protection:24) — not `$KIT` (retro-loop KIT=$TK seam). ✓
- **PEAK open/close same iteration**: mb_parse `:67` `max_d > old_d && max_d >= 2`, close `:104` `brace_depth < m_dep`
  same loop → one-liner bounded to `[i,i]` (`m_end==m_start`). ✓
- **`:303-307` comment replaced (SC-4b)**: silent-protection:306-310 now documents PEAK ("open ⟺ max_d > old_d && max_d
  >= 2; close ⟺ brace_depth < m_dep"); no file documents NET as correct. ✓
- **ext-writable array iteration (D6/D7)**: the inline `_scan_writes` is gone — `:136` `_n = mb_parse(slines, NR, ms, me,
  mn)`, `:137-141` `for (k…) { if (!(mn[k] in do_methods)) continue; body=…; _scan_writes(body) }`. ✓
- **3×3 identity**: spot-checked all three lints at dab0807 (NET) vs 6f39f8b (PEAK) on ColdRoomPan-rt → OLD==NEW (0/0/0),
  IDENTICAL — consistent with 0 one-liner methods in the 42 client `.java`. ✓
- **0 attribution trailers** across the 4 commits. ✓

## Finding — I2 (Case-B `@`-stop) and I3 (Case-A keyword exclusion) flip NO fixture; they are redundant, not unpinned
The tasks list I1-I5 as OBSERVED mutations (task 1.10 / PR1 lead gate), with I2→S21-misparse and I3→G-samemethod. I
mutated the fragment (dropped each) and ran everything:
| mutation | golden | one-liner | S21-misparse | G-samemethod | if-split isolator | inner-class+@ isolator |
|---|---|---|---|---|---|---|
| baseline | 7/7 | 3/3 | 0 | 0 | 1 | 0 |
| **drop I2** (`@`-stop) | 7/7 | 3/3 | 0 | 0 | 1 | 0 |
| **drop I3** (kw excl) | 7/7 | 3/3 | 0 | 0 | 1 | 0 |

**Nothing flips.** Why they are masked, not merely un-fixtured:
- **I2** is redundant with **I1**: the S21-misparse class body is at depth 1, so `max_d >= 2` rejects it BEFORE Case B
  runs — the `@`-stop is never reached. The only shape where the `@`-stop could matter (a brace-only construct at depth
  ≥ 2 preceded by an annotation with `identifier(`, e.g. an inner class) does not flip either, because inner-class fields
  sit at depth ≥ 2 and lint-timers classifies them as LOCAL (not companion-flag candidates). No lint OUTPUT can change.
- **I3** (Case-A keyword exclusion `if|for|while|…`) is dead given the `!in_m` gate: nested control blocks inside a method
  are never candidate opens (in_m already 1), and those keywords cannot appear at class-body scope where `!in_m`. `mname
  = "new"` is unreachable (`new` is always followed by a type, so the regex names the type, not `new`). The if-split
  isolator (FIELD flag set in an `if` block, schedule outside, same method) stays FAIL under drop-I3 — the enclosing
  method still binds both.

This is the **PR4-absolutise / B832-G2 pattern**: a guard that no fixture can flip because another invariant already
covers every reachable case. Per **K24(7)** (my C10 close-lesson: an OBSERVED mutation must name a fixture it flips), I2
and I3 must NOT be carried as OBSERVED mutations. Recommendation: **reclassify I2/I3 like I5** (the `/* */` strip — the
design already marks it "no biting fixture; pinned by 3×3 baselines"): keep them in the fragment as defensive/documented
invariants, pinned by the aggregate golden set + 3×3 baselines, and REMOVE them from the I1-I5 OBSERVED-flip lead-gate
line. Only I1 (→S21-misparse), I4 (→C11-tl/sp-oneliner) and the accessor skip (→C11-g1-setter) are real OBSERVED flips.
Tell QA to stop trying to pin I2/I3 — a biting fixture does not exist (same as B832-G2). Not a correctness blocker; the
fix is right and I2/I3 are harmless. `[ev: fragment mutation runs @ 6f39f8b; K24(7)]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | fragment function-only; 3 mechanisms incl. second -f; BASH_SOURCE | [CERT] | lib/method-boundary.sh; :49/:208/:476 |
| 2 | :303-307 replaced (SC-4b); ext-writable _scan_writes → array iter | [CERT] | silent :306-310; ext-writable :136-141 |
| 3 | 3×3 identity spot-check ColdRoomPan-rt OLD==NEW | [CERT-live] | dab0807 vs 6f39f8b runs |
| 4 | drop-I2 and drop-I3 flip NOTHING (golden 7/7, oneliner 3/3, 4 isolators) | [CERT] | fragment mutation runs |
| 5 | I2 masked by I1 (depth-1 class body); I3 dead under !in_m gate | [CERT] | code + isolator runs |
| 6 | 0 trailers | [CERT] | git log |
Tally: 5 [CERT] · 1 [CERT-live] · 0 [INFER] · 0 unmarked.
