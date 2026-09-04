# RESEARCH-STATE — focus: interactive-composition (Niagara's visual flow-composition surface vs Node-RED — making our modules interactive, discoverable, and low-cognitive-load)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-09-03** (operator request: study how
> Tridium composes modules — base/children/parents/data-flow "schema" — and how to make our own module
> ecosystem INTERACTIVE and non-overwhelming, inspired by Node-RED's block model, using modern methods and
> connections). DESIGN/APPLIED corpus — synthesizes EXISTING evidence blocks (the wire-sheet + rt campaign)
> into a comparison + actionable recommendations. A high [INFER] ratio is EXPECTED (design synthesis, not new
> extraction); every FACT is cited to a code-grounded block.
>
> **Ángulo:** NOT re-deriving the module skeleton (module-anatomy) nor the wire-sheet editor internals (B429,
> workbench focus) — those are REMITTANCES. The NEW work is (1) reading Niagara's Wire Sheet AS a
> flow-programming surface and comparing it feature-by-feature with Node-RED (which is 0 hits in the corpus),
> and (2) a ranked, buildable set of recommendations to make OUR refrigeration modules interactive,
> discoverable, and low-cognitive-load — plus the modern connection options (browser flow view, Node-RED
> bridge via oBIX/MQTT/BACnet/REST). Read-only over the corpus + real jars in organized/.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 745
gaps_closed: 4
known_gaps: 4
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: interactive-composition
status: stopped (4/4 investigable closed in one run; deliverable docs/interactive-composition.md pending optional write; next free block B749)
bootstrapped_on: 2026-09-03
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B749)

## Coverage

- **Covered blocks**: 745 corpus-wide (this focus: B747-B748) (shared-global)
- **Coverage metric**: 4 / 4 gaps closed
- **Deliverable**: `docs/interactive-composition.md` (optional human-readable guide — B747+B748 already carry the full content)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | IC1 — Niagara Wire Sheet AS a flow-programming surface vs Node-RED: the feature-by-feature map (blocks/glyphs, pins/terminals, wires/links, palette, layout persistence, live values, deploy model, debug), and WHERE each wins on cognitive load | synthesis+code | closed (B747 — term-by-term table [CERT], live values via PropertyBarGlyph, SUMMARY=pin confirmed, closes B735-G1) |
| high | IC2 — Ranked, buildable recommendations for OUR modules to be interactive + not-overwhelming: compose-into-children, SUMMARY pins, icons/glyphs, palette assembly templates, status line, units, tags — each tied to a code-grounded block | synthesis+deliverable | closed (B748 — 6-row ranked playbook by impact÷cost, sequenced into B742) |
| medium | IC3 — Modern connections & methods: the browser-native flow view (bajaux vs Swing), Niagara↔Node-RED bridge (oBIX/MQTT/BACnet/REST), and where a low-code interactive layer fits WITHOUT replacing the control engine | synthesis+code | closed (B748 §748.4 — division of labor: Niagara=control engine of record, low-code=presentation over oBIX/MQTT/REST) |
| low | IC4 — The three visual-programming engines already in this install compared (Niagara Wire Sheet · kitControl function blocks · Honeywell Spyder FB tool) — what each teaches about a good interactive block model | synthesis+code | closed (B747 §747.4 — kitControl live vs Spyder compile→download; the stage/commit lesson) |

`tried:` (none blocked — all source is existing corpus blocks + real jars in organized/; SOURCE-BEFORE-AGENT passes).

## Remittance (the FACTS these recommendations are distilled FROM — cited, not re-derived)

- **The Wire Sheet editor** (glyphs mirror the component tree; SlotBarGlyph hotspots = wire terminals; links =
  orthogonal wixel-grid wires; palette drop = add component; layout persists as HIDDEN `wsAnnotation` slot in
  the BOG; edits are undoable workbench Commands; `@AgentOn(baja:Component, W)` so EVERY component has one)
  → [B429] (focus workbench, WB03), [B15] (wiresheet+property sheet).
- **Slot curation for a clean surface** (SUMMARY = wire-sheet pins; HIDDEN = removed from all UI;
  BIUnlinkableSlotsContainer = visible-but-unlinkable; facets units/precision) → [B735].
- **Composition into child components** (the fix for flat-slot sprawl; Station→Network→Device→Point→Extension)
  → [B737], consolidated in [B744].
- **The module palette** (BOG XML; pre-wired assembly templates so commissioning is drag-one-thing) → [B746],
  reader [B634].
- **Icons/glyphs** (getIcon → PNG/SVG module resource on the block) → [B738].
- **Units/facets on numeric slots** → [B745].
- **kitControl function blocks + Control palette** → [B24]; **Honeywell Spyder FB tool** (compile→binary→
  download visual FB engine) → [B106]/[B116]/[B119].
- **The rt block anatomy / lifecycle / engine thread** → [B744]/[B729]/[B730]/[B737].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-09-03 | (bootstrap — from the wire-sheet + rt campaign) | — | no · inline | IC1–IC4 seeded |
| 1 | 2026-09-03 | IC1 + IC4 | B747 | yes · Explore (wiresheet-wb code sweep, 28 tool calls) + inline synthesis | B747-G3 (BPalette widget not opened); closed B735-G1, B747-G1/G2 |
| 2 | 2026-09-03 | IC2 + IC3 | B748 | no · inline (design synthesis over B747 + campaign) | B748-G1 (bajaux flow view PoC), B748-G2 (Node-RED↔Niagara MQTT/oBIX PoC) — both requires-execution |

## Blocked gaps (each tagged with what it needs)

(none — all synthesis over existing corpus + jars.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0 (STOP — all four closed in one run)
- **Open gaps — requires-execution**: 2 (B748-G1 bajaux flow view PoC; B748-G2 Node-RED↔Niagara PoC)
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass.)
