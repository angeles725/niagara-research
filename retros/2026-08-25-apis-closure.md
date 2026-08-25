# §18 Retro — focus: apis — 2026-08-25

<!-- review-status: pending -->

> Self-retrospective (METHODOLOGY §18). Produced by a fresh-context retro agent. Proposes kit
> deltas only; does NOT edit the kit. Evidence references: blocks B507–B515, RESEARCH-STATE-apis.md,
> session 2026-08-25, niagara-research corpus.

---

## Run summary

- **Focus:** apis ("investigate every N4 API")
- **Session:** 2026-08-25, niagara-research corpus
- **Mode:** Orchestrated AUTO — 8 API blocks B507–B514 + focus synthesis B515 (9 blocks total)
- **Gaps:** 8/8 API1–API8, all closed (8 sonnet sweeps + inline synthesis)
- **Bootstrap:** 2-agent PARALLEL AUDIT-FIRST coverage sweep that mapped ~40 API surfaces against the
  corpus → ~30 REMITTANCE + 8 genuine gaps; nHaystack/BACnet-WS proven-absent; call-site-cookbook
  PARTIALs recorded-not-seeded. Pre-flight §e2 confirmed source for 7 of 8 gaps inline; API6 seeded
  as "source-locate-first."
- **Execution pattern:** each iteration = one delegated `sonnet` sub-agent sweep → driver re-verifies
  ALL load-bearing file:line inline → one block per commit → pushed at focus stop.
- **REMITTANCE pattern:** 3 of 8 seeded gaps (API5, API6, API8) were flagged REMITTANCE-risk at seed
  time; each sweep was instructed to deliver a REMITTANCE verdict FIRST; all 3 turned out genuine.
- **Synthesis:** niagaraRpc verb unification axis (3 transports: web/B507, box/B512, fox/B513) visible
  only at B515 synthesis; one §14 refine of B290.
- **Yield:** 0 net-new gaps uncovered across 8 iterations (AUDIT-FIRST had already bounded the universe).

---

## Proposed kit deltas

| # | Title | Evidence | Priority | Kit file / section |
|---|---|---|---|---|
| D3 | Parallel AUDIT-FIRST for large-scope enumeration requests over a mature corpus | it.0 bootstrap: 2 parallel `sonnet` agents split the taxonomy by domain (network/web/auth vs programmatic/client/platform), together mapping ~40 surfaces in one pass; ~30 surfaces confirmed REMITTANCE, 8 seeded as genuine. Kit text says "delegate an audit sweep" (singular); no mention of parallel agents or REMITTANCE-dominant expectation on mature corpora. | MEDIUM | `PROMPT-LOOP.md` — AUDIT-FIRST BACKLOG paragraph (step e) + `METHODOLOGY.md` §13 "Audit-first as a backlog seed" |
| D4 | Per-gap REMITTANCE-verdict-first in sweep prompt for REMITTANCE-risk gaps | 3/8 gaps (API5 vs B494, API6 vs B134/B414-420, API8 vs B406) were flagged REMITTANCE-risk at seed time; each sweep was structured to return a REMITTANCE verdict FIRST before any investigation, so the driver could close without authoring a block. All 3 turned out genuine (preventing false closures), but the discipline prevented wasted sweeps. Kit covers PRIOR COVERAGE CHECK (driver-level, before delegating) and REMITTANCE as a closure category, but does not say "for a gap flagged REMITTANCE-risk, the sweep prompt should demand an explicit REMITTANCE verdict as its FIRST output." | LOW | `PROMPT-LOOP.md` — NORMAL CYCLE step 3 PRIOR COVERAGE CHECK paragraph |

---

## Reinforced observations (already in kit — not new deltas)

| Obs | Kit coverage | Notes |
|---|---|---|
| AUDIT-FIRST backlog seeding is the high-value move for mature/large corpus + broad request | PROMPT-LOOP step e "AUDIT-FIRST BACKLOG"; §13 "Audit-first as a backlog seed" — "proven on the protocols focus (matrix → 6 well-shaped gaps)" | Reinforced by this focus: 2-agent audit seeded 8 well-shaped gaps from ~40-surface enumeration. Worked as designed. |
| Coverage matrix "known-vs-gap" column captures REMITTANCE surfaces | PROMPT-LOOP step e matrix format (`subsystem × current-depth × static-vs-dynamic × known-vs-gap`). | The `known-vs-gap` column correctly identified ~30 REMITTANCE surfaces. Reinforced; the format works for this case. |
| REMITTANCE as a closure category (§8) | §8: "A gap closes by remittance when a later sweep shows it is ALREADY fully answered by an EXISTING cited block/section, with NO new substance to add." | Reinforced: the entire REMITTANCE list (~30 surfaces) was cited-not-re-derived. 3 REMITTANCE-risk gaps checked and confirmed genuine. |
| PRIOR COVERAGE CHECK at iteration level | PROMPT-LOOP step 3: "PRIOR COVERAGE CHECK: before any tool sweep, read corpus blocks whose INDEX.md description overlaps this gap." | Reinforced: each of the 3 REMITTANCE-risk gaps had overlapping blocks read before the sweep was delegated. |
| Source-locate-first for unknown-location source (observation c) | BOOTSTRAP e2: "for each planned gap, CONFIRM readable source material actually exists … A gap with NO reachable source must be marked blocked-on-<reason>." | API6 pre-flight: not found in docSource → seeded "source-locate first." Sweep located it in fox-rt then proceeded. Reinforced by e2 (existence check covers this case). Not a new pattern. |
| Synthesis surfacing cross-cutting axes (framework-drivers observation e) | PROMPT-LOOP §8 terminal trigger: "synthesis block is a valid terminal artifact." | Reinforced: niagaraRpc verb's 3 transports (web/B507 + box/B512 + fox/B513) became visible as ONE unified verb only at B515 synthesis. Same cross-cutting-axis-at-synthesis behavior as framework-drivers' SDK-bundling axis (B506). Consistent with prior retro observation (e); not a new delta. |
| D1 (systematic offset accumulation in sweeps) — NOT reinforced | Framework-drivers D1 proposed adding SYSTEMATIC-OFFSET CAVEAT to VERIFY BEFORE ACTING. | This run has no reported systematic offset issues. All blocks note "all load-bearing re-verified inline" as precaution; no iteration reports discarded sweep line numbers. D1 not reinforced here. |
| D2 (delegated sweeps over-assert external/operational status) | Framework-drivers D2. | No external service endpoint status assertions in this run (all APIs are internal Java source; no live-endpoint claims). D2 not reinforced here. |

---

## Delta details

### D3 — Parallel AUDIT-FIRST for large-scope enumeration requests

**Evidence:** it.0 bootstrap in RESEARCH-STATE-apis.md: "2 parallel `sonnet` audits (network/web/auth + programmatic/client/platform) mapped ~40 API surfaces; MOST already have dedicated blocks (REMITTANCE list). 8 genuine uncovered surfaces seeded (API1–API8)."

The request was a broad enumeration ("investigate every N4 API"). The high-value bootstrap move was splitting the taxonomy across two parallel agents — each covering a different domain partition — and having each return a sub-matrix of (API surface × has-dedicated-block). The combined result was ~40 surfaces catalogued in one pass: ~30 REMITTANCE (already have dedicated blocks), 2 proven-absent, a handful of PARTIAL call-site-cookbook surfaces (recorded-not-seeded), and 8 genuine gaps.

**Gap in current kit:** Both the PROMPT-LOOP step e "AUDIT-FIRST BACKLOG" paragraph and §13 "Audit-first as a backlog seed" describe the pattern as:

> "delegate an audit sweep (Explore/general-purpose sub-agent) that returns a COVERAGE MATRIX"

Two things are missing:
1. For a large-scope enumeration (>~20 surfaces to cross-reference), splitting the taxonomy across PARALLEL agents substantially reduces pass time and avoids context bloat on a single agent.
2. For a MATURE corpus with a broad "investigate every X" request, the coverage matrix will predominantly identify REMITTANCE surfaces; the audit's PRIMARY deliverable is the SMALL DELTA SET, not a full investigation plan. Making this expectation explicit prevents over-seeding (seeding REMITTANCE surfaces as genuine gaps) and under-seeding (stopping at first few genuine gaps without completing the enumeration).

**Proposed addition (after "Derive the prioritized backlog from that matrix" in AUDIT-FIRST BACKLOG):**

> PARALLEL AUDIT FOR LARGE TAXONOMIES: when the enumeration domain is large (>~20 surfaces, or the taxonomy spans multiple architectural layers), split the domain by partition (e.g. network/auth vs. programmatic/client) and run N parallel audit agents, one per partition, each returning its sub-matrix. Merge sub-matrices before seeding. REMITTANCE-DOMINANT EXPECTATION: for a mature corpus with a "broad enumeration" request ("investigate every X"), most surfaces will already be covered in the corpus — the audit's primary value is identifying the small delta set; seeding ONLY the non-covered gaps is the correct output. Record the REMITTANCE list in RESEARCH-STATE as a named section so it is not re-audited in future passes. (Evidence: apis focus, 2026-08-25: 2 parallel sonnet audits, ~40 surfaces, ~30 REMITTANCE, 8 genuine gaps seeded.)

**Priority:** MEDIUM — the single-sweep language is not wrong, but the parallel pattern is a significant efficiency gain for large-scope enumeration over a mature corpus, and the REMITTANCE-dominant expectation prevents over-seeding.

---

### D4 — Per-gap REMITTANCE-verdict-first in sweep prompt for REMITTANCE-risk gaps

**Evidence:** 3 of 8 gaps were flagged "REMITTANCE-risk" in the backlog at seed time:
- API5 (BAuthenticationScheme SPI) — risk: B494 covered OEM implementations, might already document the framework
- API6 (Fox client API) — risk: B134 (wire), B414-420 (station-internal), B471 (hand-rolled); none might document the public API but unclear
- API8 (BQL/NEQL call contracts) — risk: B406 (engine), B5/B21 (grammar); call contracts might already be implicit

For each, the sweep was told to return a REMITTANCE verdict FIRST before any tool use. All 3 returned genuine (B494 was a 1-paragraph impl summary; the framework contract was new; B406 was the engine, not the call sites). Without the discipline, each sweep might have investigated fully before concluding remittance — wasting up to 20+ tool uses per gap.

**Gap in current kit:** PROMPT-LOOP step 3 says:

> "PRIOR COVERAGE CHECK: before any tool sweep, read corpus blocks whose INDEX.md description overlaps this gap"

This is a DRIVER-level step, before delegating. When the PRIOR COVERAGE CHECK is INCONCLUSIVE (some coverage exists, uncertain if new substance remains), the driver cannot resolve it inline without the sweep's findings. The kit does not say what to do next: how should the gap be flagged to the sweep, and what should the sweep's FIRST action be?

**Proposed addition (at end of PRIOR COVERAGE CHECK paragraph):**

> REMITTANCE-RISK FLAG: when the PRIOR COVERAGE CHECK finds partial corpus coverage for a gap but cannot determine whether genuine new substance exists, flag the gap as REMITTANCE-risk in the backlog and include this flag in the sweep prompt: "check REMITTANCE FIRST — state whether this gap is fully answered by [Block N] §N.x with no new substance, BEFORE any tool use." A sweep that returns 'REMITTANCE — no new substance, cite [Block N] §N.x' is a valid closure; the driver closes without authoring a block. This prevents wasted investigation if the gap is remittance at fine grain even when the audit cleared it at coarse grain. (Evidence: apis focus API5/API6/API8, 2026-08-25: 3/8 gaps REMITTANCE-risk; all 3 turned out genuine.)

**Priority:** LOW — PRIOR COVERAGE CHECK and REMITTANCE as closure category already cover the principle; this addition makes the sweep-prompt protocol explicit for the ambiguous-at-driver-level case.

---

## Addendum 2026-08-25 — D5 (kit delta surfaced by `research-sdd-archive --dry-run`)

**D5 — HIGH — `verify-state` / `research-sdd-archive` do not support SHARED-GLOBAL multi-focus corpora**
- Kit target: `toolbelt/verify-state.sh` (covered_blocks check) + `research-sdd-archive.sh` gate.
- Evidence: `verify-state.sh` counts a focus's blocks by FOCUS PREFIX (`<focus>-blockN.md`). This corpus uses
  shared-global numbering (`niagara-mental-model-bloqueN.md`) across ALL 34 focuses (METHODOLOGY §16 permits
  this). Consequence: there is NO `covered_blocks` value that passes — the focus's real count (e.g. 12) fails
  both the prefix count (0 focus-prefixed files) and the total (513 on-disk). Declaring `block_scope:
  shared-global` in the envelope did NOT make the check pass (framework-drivers/apis still FAIL
  `covered_blocks=12 != 513`). 14 focuses FAIL identically → `research-sdd-archive` REFUSES on every close for
  this corpus, which is why manual close is used here.
- Proposed: `verify-state` should, when `block_scope: shared-global` is declared, TRUST the declared
  `covered_blocks` (or derive it from the iteration-history block list) instead of prefix/total counting; and
  the archive gate should treat a shared-global focus as passing when its envelope is internally consistent.
- NOTE: the `verify-sources` FABRICATED-CITE gate WAS reconcilable and was fixed this session (8 pre-existing
  rows → verify-sources exit 0). Only the `verify-state` structural limitation remains.
