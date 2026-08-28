# RESEARCH-STATE — focus: kitControl (PLANNED)

> Multi-focus corpus (METHODOLOGY §16). This focus was SEEDED by an AUDIT-FIRST coverage sweep (§13),
> NOT hand-guessed — see the coverage matrix in the iteration history below (2026-08-28, delegated sonnet;
> verified inline). It answers the operator's request: documentation about **kitControl / control modules /
> control logic**, **how control modules are programmed** with control logic, the **programming RULES of
> control modules**, and **HVAC control**.
>
> **Angle (§b2):** the CONTROL-PROGRAMMING axis of N4 — the native block library (`kitControl`), the
> writable-point control model (`javax.baja.control`), the freeform-logic module (`program`), the wiring
> RULES (links/execution/priority-array), and the HVAC control libraries (OEM `honeywellFunctionBlocks`,
> `honIrmControl`, Centraline `clHVAC*`). Decompiled-Java + official `docKitControl`/`docHoneywellFunctionBlocks`
> reference. READ-ONLY. Corpus language for NEW blocks = **English** (post-B115 convention).
>
> **The engine half is already DEEP → REMITTANCE (not re-opened):** the event-driven execution engine, the
> Link kernel model, and the Wire Sheet editor mechanics are covered by [Block 6] (§6.1 execution, §6.2 links,
> §6.3 control points + kitControl category table) and [Block 429] (wire sheet). This focus attacks the
> CATALOG + RULES + HVAC-application frontier those blocks left open — it consolidates and deepens, it does
> not re-derive B6/B429.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 1
gaps_closed: 1
known_gaps: 12
investigable_open: 11
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: kitControl
status: active (1/12; KC1 closed → B536)
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; verified inline)
seeded_on: 2026-08-28
gaps_total: 12 investigable (KC1–KC12)
gaps_closed: 1 (KC1→B536)
blocks_written: B536 (KC1); next global block = B537
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Gap-backlog (prioritized) — from the AUDIT-FIRST coverage matrix

All "Where" paths are under `/home/cristian/niagara-research/organized/`. Distinct-class counts are the
vineflower `.java` count over the module tree; RE-MEASURE before using any count as a denominator (GAP
NUMBERS ARE HYPOTHESES). Doc HTML counts are `docKitControl` / `docHoneywellFunctionBlocks` / `*-doc`
reference pages. All candidate dirs existence-verified 2026-08-28.

| Priority | Gap | Where | Status |
|---|---|---|---|
| high | **KC1 control module internals** — the WRITABLE-POINT control model that every control app writes to: `BControlPoint`/`BNumericWritable`/`BBooleanWritable`/`BEnumWritable`, WritableSupport, the 16-level priority array + relinquish-default + null propagation, point extensions (override/alarm/history), override lifecycle, action/trigger surface. B6 §6.3 gives only a concept table; B276 only the BACnet-writable slice | `control-rt,-ux,-wb` (~45 vf) + docSource `javax/baja/control` | **COVERED → B536** (WritableSupport.onExecute = 1→16 first-valid-wins scan, null-status=relinquished, fallback=ordinal 17 relinquish-default; only winning levels 1/8 raise OVERRIDDEN; in1/in8/fallback READONLY-persisted, other inN TRANSIENT [refines B6 §6.3.6]; BPriorityLevel enum has NO emergency/manual constant — semantics are convention; actions emergencyOverride→in1, override→in8+Clock.schedule TTL revert, set→fallback; proxyExt always first extension [confirms B6 §6.3.2]) |
| high | **KC2 kitControl FB catalog** — the native function-block library enumerated BY BLOCK (never done): the ~157 named blocks (math, logic, comparison, latches, timers, selectors, hvac, energy, conversion, string, util) with their inputs/outputs/facets, mapped against the 163-page official reference. B6 §6.3.3 gives only a by-category list | `kitControl-rt` (~207 vf) + `docKitControl` (163 HTML) | **pending** |
| high | **KC3 control-module programming RULES** — consolidated: link legality (which slot types may legally connect), knob/mark semantics, type coercion/conversion-link matrix, execution-order guarantees, and cycle/feedback handling. Today scattered in B6 §6.2 prose + 396 wire-sheet / 52 control-logic official guides — no single rules artifact | derived (B6 baja core) + niagara-help guides | **pending** |
| high | **KC4 PID / LoopPoint** — the core HVAC control primitive as a dedicated treatment: the kitControl loop/PID block(s), tuning params (P/I/D), direct vs reverse action, integral windup, ramp/rate, execution against the engine. Subset of KC2 but deserves its own depth | `kitControl-rt` (hvac/util pkgs) + `docKitControl` PID pages | **pending** |
| high | **KC5 clHVAC application library** — the Centraline Eagle HVAC control-sequence libraries, under-covered vs their mass (B87 is one concept-level block over ~756 classes): AHU/air-conditioning, heating, chiller, energy-management, room-control application blocks and the encoded control sequences | `clHVAC`, `clHVACAirConditioning`, `clHVACHeating`, `clHVACChiller`, `clHVACEnergyManagement`, `clHVACGeneral` (~756 vf · ~1,400 doc HTML) | **pending** |
| medium | **KC6 program module runtime** — B426 covers ONLY compilation (spawned javac). Uncovered: `BProgram` execution model, freeform vs robot program, program slots/wiring, program-ext lifecycle, the program-wb editor | `program-rt,-wb` (~55 vf) | **pending** |
| medium | **KC7 honeywellFunctionBlocks per-FB catalog** — B103 covers the ENGINE (`BFunctionBlock`, converters); the ~158 DDC blocks themselves (math/control/analog/zonecontrol/logic/datafunction) are not catalogued against the 50-page official doc | `honeywellFunctionBlocks-rt` (158 vf · 50 doc HTML) | **pending** |
| medium | **KC8 priority-array write arbitration end-to-end** — the consolidated write path: a kitControl block writes → writable point 16-level arbitration → relinquish default → driver proxy. B6 §6.2.6 + B46 touch pieces; no end-to-end arbitration-rules block | `control-rt` + `kitControl-rt` write blocks | **pending** |
| medium | **KC9 composites** — the composite as a REUSE/programming construct: glyph slot-promotion mechanics, how a composite interacts with links and execution, reuse of control logic. B24 mentions (26) + 52 official guides, no dedicated block | `wiresheet-wb` + niagara-help guides | **pending** |
| low | **KC10 honIrmControl per-FB catalog** — engine covered (B105/B242/B493); the ~163 IRM Nano FBs vs 134 doc pages not enumerated block-by-block | `honIrmControl-rt` (218 vf · 134 doc HTML) | **pending** |
| low | **KC11 kitControl enums / const tables** — packaged enum semantics (trigger modes, transition/latch types) + constants package not extracted | `kitControl-rt` enums+constants (module_nav resources) | **pending** |
| low | **KC12 clHVAC Nordic + micro-modules** — the smallest clHVAC modules for completeness: `clHVACNordicAirCondition`, `clHVACNordicGeneral`, `clHVACEnergyManagement`, `clHVACRoomControl` | (~7+11+3 vf) | **pending** |

### REMITTANCE (already covered — will NOT be opened)

- **Execution engine (event-driven, no scan cycle) → [Block 6] §6.1.** EngineManager, post/postAsync,
  coalescing, clock ticks. Cite B6; do not re-derive.
- **Link kernel model / knobs / cycle detection basics → [Block 6] §6.2.**
- **Wire Sheet editor mechanics → [Block 429]** (glyphs mirror component tree, layout in hidden slot,
  links delegate to workbench commands). KC9 (composites) is the residue, not the editor itself.
- **honeywellFunctionBlocks control ENGINE → [Block 103]; honIrmControl / IRM Nano ENGINE → [Block 105]/[B242]/[B493].**
  Only the per-FB CATALOGS remain (KC7, KC10).
- **honPlantController family → [Block 91]/[B250]/[B242].** Covered.
- **kitPxHvac → [Block 203]:** a PX widget PALETTE (BOG), not control code (measured 0 `.java`). Not a gap.
- **kitControl↔writeback via a driver → [Block 37]** (KNX proxy↔virtual↔kitControl↔writeback) and **[Block 46]**
  (priority-array writes from external SPA). The transport-specific path is covered; KC8 is the generic
  arbitration residue.
- **program module COMPILATION → [Block 426].** Only compilation; runtime is KC6.

### Dismissed file types / modules (recorded, NOT gaps)

- `kitPxHvac` — 0 `.java` (BOG palette). Dismissed: not control code (REMITTANCE → B203).
- `honPlantControllerHMI` / `*Migrator` / `*EHMigrator` — HMI + migration tooling, not control-logic
  primitives. Dismissed as out of the control-programming angle.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **11** (KC2–KC12). All source dirs existence-verified 2026-08-28.
- **Gaps closed**: 1 (KC1→B536).
- **requires-execution / blocked**: 0.
- **Coverage metric**: 1 / 12 investigable gaps closed.
- **NEXT**: KC2 (kitControl FB catalog — enumerate ~157 blocks) → B537.

## Iteration history

| Iter | Gap | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|
| seed | AUDIT-FIRST coverage sweep (control/kitControl/program/rules/HVAC) | — | yes · sonnet (verified inline) | KC1–KC12 seeded from the coverage matrix |
| 1 | KC1 control module internals (writable-point model, arbitration, override, extensions) | B536 | yes · sonnet (sweep) + inline token-verify | none new (KC8 continuation noted) |
