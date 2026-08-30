# RESEARCH-STATE — focus: station-organization (where field points vs control logic go in a station, and the recommended structure — a how-to)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** (operator question: adding points of a
> TC500 thermostat / IO-R-34 — where does the programming go? do points stay in a points-only area?). DOCUMENT/
> HOW-TO focus — synthesizes the DONE structural focuses into a recommended station structure; deliverable
> `docs/station-organization.md`. High [INFER] ratio expected.
>
> **Ángulo:** STATION ENGINEERING (not module building): the recommended two-layer structure — device/proxy
> points under their driver (points-only) + control logic organized by EQUIPMENT + navigation by tags/hierarchy.
> Control-logic library INTERNALS (kitControl) are REMITTANCE-DEFERRED (focus `kitControl` is planned/not-done);
> this focus covers ORGANIZATION + linking + navigation, which the done focuses cover.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 716
gaps_closed: 5
known_gaps: 5
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: station-organization
status: stopped (5/5 investigable; deliverable docs/station-organization.md)
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B716)

## Coverage

- **Covered blocks**: 711 corpus-wide (this focus: B716-) (shared-global)
- **Coverage metric**: 5 / 5 gaps closed (SO1-5 investigable=0); deliverable docs/station-organization.md complete
- **Deliverable**: `docs/station-organization.md` (how-to)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | SO1 the driver/points layer — where field points live (proxy points under their device, points-only), how the driver creates them (discovery/learn), TC500/BACnet vs IO-R-34/NRIO | synthesis+code | closed (B716 — /Drivers/Network/Device/points tree, proxy-point model, discovery/learn, TC500=BACnet IO-R-34=NRIO placement, points-only principle; how-to §1) |
| high | SO2 the equipment/application layer — organizing control logic by EQUIPMENT (folders/components), kitControl wire-sheet vs Program vs custom module; kept SEPARATE from the driver points | synthesis+code | closed (B717 — logic in /Config-or-/Services not /Drivers; 3 authoring methods; equip-tag grouping; official Philosophy B=blocks-near-points; how-to §2) |
| high | SO3 linking points ↔ logic — Niagara links, priority-array on writable points, the ChiLinkHelper pattern, decoupling logic from device addressing | synthesis+code | closed (B718 — BLink bridge, 16-level priority array, batch-editor+ChiLinkHelper, handle/tag-stable re-address; how-to §3) |
| medium | SO4 navigation & grouping — hierarchy + tags (Haystack/Niagara) so operator views group by equipment/location without duplicating the physical tree | synthesis+code | closed (B719 — tag-once-navigate-many: tags semantic layer + hierarchy alternate nav trees on-demand/stateless; RBAC preserved; how-to §4) |
| medium | SO5 reuse + synthesis — equipment templates (template subsystem) + provisioning for fleet + the recommended-structure verdict; deliverable docs/station-organization.md | synthesis+deliverable | closed (B720 — templates+provisioning reuse; recommended layered structure verdict; docs/station-organization.md finalized 5 sections; focus STOP) |

`tried:` (none blocked — synthesis over done focuses + the JACE config already read this session).

## Remittance (cited, not re-derived)

- Field-bus drivers (NRIO, BACnet, the driver/device/points model) → focus `framework-drivers` [Block 496]–[Block 506]; NRIO driver [Block 680]; the JACE's deployed NRIO points [Block 687].
- NiagaraNetwork (supervisor↔subordinate, device proxy) → focus `niagara-network-supervisor` [Block 414]–[Block 420].
- Control-logic LIBRARY internals (kitControl function blocks) → focus `kitControl` [Block 537]/[Block 538]/[Block 545] (DONE 2026-08-28 — corrected from bootstrap's 'planned'); this focus covers ORGANIZATION, not the block library. BProgram → [Block 541].
- Control model (`javax.baja.control`, writable points, priority array) → [Block 6]/[Block 429]; chihuahua equipment model + ChiLinkHelper → focus `chihuahua-source` [Block 648]–[Block 655].
- Hierarchy engine → focus `hierarchy` [Block 584]–[Block 590]; tag subsystem → focus `tags` [Block 260]–[Block 270].
- Equipment templates → focus `template` [Block 577]–[Block 583]; provisioning → focus `provisioning` [Block 567]–[Block 576].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap) | — | no · inline | SO1–SO5 seeded |
| 1 | 2026-08-30 | SO1 driver/points layer | B716 | yes · sonnet (synthesis of driver blocks) | 0 new |
| 2 | 2026-08-30 | SO2 equipment/logic layer | B717 | yes · sonnet (synthesis) | 0 new (§14: kitControl is DONE not planned) |
| 3 | 2026-08-30 | SO3 linking points↔logic | B718 | no · inline (B6/B650/B654 targeted read) | 0 new |
| 4 | 2026-08-30 | SO4 navigation & grouping | B719 | no · inline (tags/hierarchy in-hand) | 0 new |
| 5 | 2026-08-30 | SO5 reuse + SYNTHESIS (focus close) | B720 | no · inline | 0 new (focus STOP) |

## Blocked gaps (each tagged with what it needs)

(none.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass.)
