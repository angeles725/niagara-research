# C11 proposal.md second read — kit 30e9e62 (v0.22.0 target)

investigador1, 2026-09-07. The three C10-style checks (scope/waves, exact-count criteria, no presence-only) + the T1
size-estimate honesty + the PR4-vacuity flag. `[ev: proposal @ 30e9e62; B832; git @ dab0807]`

## Verdict: PASS to spec/design. Three checks clean. Two findings for the design phase (neither blocks propose).

## Check 1 — scope vs waves — PASS
§2.1 in-scope = W1 (R1 T1 / R2 T3 / R3 T2 / R4 T4 / R5 close), kit-only, WSL-only, nothing from Cristian. §2.2 out-of-scope
= P1-P5 + the three prerequisites (deploy chain, harness, tunnel merge), each with the exact gate that opens it; §11
repeats them as Cristian gates. "ONE wave (W1), 5 PRs; W2 gated." Clean W1/W2 split, wave order T1→T3→T2→T4→close matches
§4 and the dependency note. `[ev: proposal §2.1/§2.2/§11]`

## Check 2 — exact-count acceptance criteria (not presence-only) — PASS
SC-1..SC-14 are exact-count with subjects throughout: SC-4 "9 verdicts (3 lints × 3 modules) byte-identical before/after";
SC-5 "exactly 1 DRIFT, exit 0; --strict → 1; true concept row silent; decoy → no DRIFT; FAIL exit 1 identical; exit 3
preserved; 00e7118 → 0 DRIFT"; SC-6 "10 → 0, suite green with all three vars unset, one override pin each"; SC-7 "LD5 =
clean exit 0 (was exit 1 + FAIL BDefrostController); RC8 = 1 FAIL :701"; SC-8 "positive → 1 WARN; negative → 0 WARN;
usage → 3; kit → 0 WARN". `[ev: proposal §10]`

## Check 3 — no presence-only — PASS
SC-9 mandates "every smoke pin asserts exact count + subject + absence (K22); a smoke that cannot run is a BLOCKER" and
"names the exact fixture it flips (K24(7)); no PR merges on a 'would flip' claim." No criterion is satisfiable by mere
presence. `[ev: proposal SC-9]`

## Finding 1 (T1 size estimate) — reasonable but the breakdown UNDER-budgets the hardest part
The ~500-600 estimate breaks down as "fragment ~120 + three call-site cuts ~120 + golden/one-liner fixtures ~300" (§4
Size). The fragment (~120) and fixtures (~300) are honest. But **"three call-site cuts ~120" treats them as deletions**
and omits the real integration cost: per B832 §3 (D6/D7) the three lints CONSUME the parser output differently —
`lint-timers`/`lint-silent-protection` build `meth_start[]/meth_end[]/(meth_name[])` arrays for a later pass, while
`lint-ext-writable-shape` consumes each body INLINE (`_scan_writes(body)`, no array). A shared fragment must expose ONE
output contract that all three adapt to — that adaptation (especially reworking ext-writable's inline consumer, or making
the fragment serve both an array and an inline hook) is design work, not deletion, and is not line-itemized. The estimate
is a plausible FLOOR, not a confident ceiling; if the fragment must serve both consumption patterns PR1 can exceed 600.
Recommend `sdd-design` fix the fragment's output contract (array vs inline, and how ext-writable adapts) BEFORE the
estimate is locked, and re-confirm the size:exception band. `[ev: B832 §3 D6/D7; proposal §4 Size]`

## Finding 2 (PR4 vacuity — the lead's specific flag) — guards a vacuous PARSER, not a vacuous GRAMMAR
The proposal correctly forbids a vacuous parser: RK7 + SC-8 + the PR4 lead gate require a POSITIVE fixture (a header
naming a mutation with no fixture → must WARN), so a parser that can never WARN fails its own suite. **But that does not
guard the grammar against the REAL lint headers.** The `# Mutation:` header grammar is deferred to design (RK7). If the
chosen grammar does not match the ACTUAL C10 lint-header format, the lint finds 0 mutations in the real kit → SC-8's
"run over the kit → 0 WARN" passes VACUOUSLY (0 mutations checked reads identically to "every mutation has a fixture").
The synthetic positive (miss) and negative (match) fixtures both use the design's own grammar, so neither proves the
grammar extracts a real header. Recommend the T4 RED add a **positive-MATCH-against-a-real-header** case: feed an ACTUAL
C10 lint header (e.g. lint-timers' or lint-write-path's OBSERVED-mutation line) and assert the lint extracts that named
mutation and resolves it to its real fixture — so "0 WARN over the kit" means "found and matched", not "found nothing".
`[ev: proposal RK7/SC-8/PR4 gate; K24(7)]`

## Notes (not findings) — sound calls by the proposal
- The **LD5 finding** (§1 fourth, RK5, SC-7, Alternatives) — a real-tree smoke asserting `exit 1 + FAIL BDefrostController`
  pins the ColdRoomPan defrost `time<=0` BUG (fixed post-C9), not a rule; on ff1b659 it flips clean and the delay-floor
  rule stays pinned by synthetic LD1/LD3/LD6. Well-reasoned and consistent with [[coldroompan-defrost-time-le-0-bug]] /
  [[client-reads-use-a109249]]. Lead-re-measured; I did not independently re-run lint-delays (outside my four checks).
- Golden set grew 5→7 by splitting the one-liner into per-lint cases (G-oneliner-timers/-silent/-extwritable) + accessor —
  justified (each lint must be asserted on the one-liner independently). SC-4b captures my :303-307 comment finding.

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | scope cleanly W1 in / W2+prereqs out, each with a gate; wave order consistent | [CERT] | proposal §2/§11 |
| 2 | SC-1..14 exact-count + subject; SC-9 mandates K22 count+subject+absence | [CERT] | proposal §10 |
| 3 | no presence-only criterion | [CERT] | proposal §10 SC-9 |
| 4 | size breakdown omits the D6/D7 output-consumption reconciliation → 500-600 is a floor | [CERT] | B832 §3; proposal §4 |
| 5 | PR4 guards a vacuous parser (positive fixture) but not a grammar-vs-real-header mismatch | [CERT] | proposal RK7/SC-8 |
Tally: 5 [CERT] · 0 [INFER] · 0 unmarked.
