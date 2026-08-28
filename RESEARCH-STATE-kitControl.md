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
covered_blocks: 6
gaps_closed: 6
known_gaps: 12
investigable_open: 6
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: kitControl
status: active (6/12; KC1→B536 … KC6→B541)
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; verified inline)
seeded_on: 2026-08-28
gaps_total: 12 investigable (KC1–KC12)
gaps_closed: 6 (KC1→B536, KC2→B537, KC3→B538, KC4→B539, KC5→B540, KC6→B541)
blocks_written: B536–B541 (KC1–KC6); next global block = B542
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Gap-backlog (prioritized) — from the AUDIT-FIRST coverage matrix

All "Where" paths are under `/home/cristian/niagara-research/organized/`. Distinct-class counts are the
vineflower `.java` count over the module tree; RE-MEASURE before using any count as a denominator (GAP
NUMBERS ARE HYPOTHESES). Doc HTML counts are `docKitControl` / `docHoneywellFunctionBlocks` / `*-doc`
reference pages. All candidate dirs existence-verified 2026-08-28.

| Priority | Gap | Where | Status |
|---|---|---|---|
| high | **KC1 control module internals** — the WRITABLE-POINT control model that every control app writes to: `BControlPoint`/`BNumericWritable`/`BBooleanWritable`/`BEnumWritable`, WritableSupport, the 16-level priority array + relinquish-default + null propagation, point extensions (override/alarm/history), override lifecycle, action/trigger surface. B6 §6.3 gives only a concept table; B276 only the BACnet-writable slice | `control-rt,-ux,-wb` (~45 vf) + docSource `javax/baja/control` | **COVERED → B536** (WritableSupport.onExecute = 1→16 first-valid-wins scan, null-status=relinquished, fallback=ordinal 17 relinquish-default; only winning levels 1/8 raise OVERRIDDEN; in1/in8/fallback READONLY-persisted, other inN TRANSIENT [refines B6 §6.3.6]; BPriorityLevel enum has NO emergency/manual constant — semantics are convention; actions emergencyOverride→in1, override→in8+Clock.schedule TTL revert, set→fallback; proxyExt always first extension [confirms B6 §6.3.2]) |
| high | **KC2 kitControl FB catalog** — the native function-block library enumerated BY BLOCK (never done): the ~157 named blocks (math, logic, comparison, latches, timers, selectors, hvac, energy, conversion, string, util) with their inputs/outputs/facets, mapped against the 163-page official reference. B6 §6.3.3 gives only a by-category list | `kitControl-rt` (~207 vf) + `docKitControl` (163 HTML) | **COVERED → B537** (MEASURED 151 B*.java → ~130 deployable across 10 pkgs: math26/util37/conv20/logic13/energy10/hvac7/timer5/const4/root13/enums16-not-blocks; 116 doc block pages. BLoopPoint PID in ROOT pkg not hvac [error=SP−PV, anti-windup clamp maxOutput/kPkIconst, hold=NaN-guard]; latch clock=rising-edge vs latch-action=both-edges; switch/select invalid→hold+invalid-flag; multi-input null contract [BQuadMath nonNullCount, nulls skipped not zeroed, BAnd nullOnInactive]. CONFIRMS B6 §6.3.3 — all 7 HVAC/energy blocks exist, none hallucinated) |
| high | **KC3 control-module programming RULES** — consolidated: link legality (which slot types may legally connect), knob/mark semantics, type coercion/conversion-link matrix, execution-order guarantees, and cycle/feedback handling. Today scattered in B6 §6.2 prose + 396 wire-sheet / 52 control-logic official guides — no single rules artifact | derived (B6 baja core) + niagara-help guides | **COVERED → B538** (RE-SCOPED to OFFICIAL Tridium rules layer since B6 §6.2 already covers the code kernel [REMITTANCE]. [CERT-doc]x55 from niagara-help guides: priority-link rules [1 link/level, In1/In8 unlinkable action-only — reconciles B536; Boolean In6 unlinkable=min on/off, refines B536], conversion links auto on type-mismatch, link owned-by-target, wire-sheet delete/pin/knob, execution=EVENT-DRIVEN no topo-order guarantee on standard sheet [ACE has Level/Order], actions sync-default/async-coalesce, composite AVOID-folder caution + resource cost, status propagate opt-in + NEVER into a point [BP2], Philosophy-B placement, naming rules. 7/12 rows token-verified inline) |
| high | **KC4 PID / LoopPoint** — the core HVAC control primitive as a dedicated treatment: the kitControl loop/PID block(s), tuning params (P/I/D), direct vs reverse action, integral windup, ramp/rate, execution against the engine. Subset of KC2 but deserves its own depth | `kitControl-rt` (hvac/util pkgs) + `docKitControl` PID pages | **COVERED → B539** (ramp = rate limiter maxChange=rampConst·Δ/rampTime with ramp-aware anti-windup errorSum rescale; executeTime clamp [100ms,60s] def 500ms; direct negates PID sum [pv=-pv]=cooling, reverse=heating; disableAction max0/min1/hold2/zero3 [prop default zero, hold=NaN-hold] pre-loads errorSum for bumpless re-enable; BLoopAlarmAlgorithm extends BTwoStateAlgorithm = SP−PV deviation alarm w/ deadband hysteresis, grandparent-legal; propagateFlags=whitelist AND-mask default 0 [no input status to out, own fault survives]. OFFICIAL tuning: kP=(maxOut−minOut)/throttlingRange, PI RECOMMENDED [kI repeats/min start 0.5], PID SELDOM USED [kD in SECONDS start <10s]. niagara-help has NO PID tuning guide — only docKitControl) |
| high | **KC5 clHVAC application library** — the Centraline Eagle HVAC control-sequence libraries, under-covered vs their mass (B87 is one concept-level block over ~756 classes): AHU/air-conditioning, heating, chiller, energy-management, room-control application blocks and the encoded control sequences | `clHVAC`, `clHVACAirConditioning`, `clHVACHeating`, `clHVACChiller`, `clHVACEnergyManagement`, `clHVACGeneral` (~756 vf · ~1,400 doc HTML) | **COVERED → B540** (upgrades B87 §87.3 [CERT-a]→[CERT]. Structure: clHVAC-rt 250 vf/103 BControlFunctionSupport primitives; 83 BCm* domain blocks. Sequences decompiled: BCmVTB_HtgCirc weather-comp heating curve = 2-pt linear OAT→Tsupply [−10°C/65°C→10°C/85°C] + room correction + 5-way mode mux + CfPidPlus valve + CfValueRamp 25K/min + frost/heat-demand; BCmDMB_MixingDamper OAT-scheduled min OA-damper + CfWindow mode∈[20,30]→force 0% recirc [economizer shutoff]; BCmSQA_ChillerSeq 12-chiller runtime-equalized lead-lag [(N+1)/N add, (N−1)/N remove, 600s inter-stage, 100h rotation, 60s alarm lockout]; BCmDDA_DEGDAYS HDD/CDD base 15/22/20°C. Engine BControlProgramService extends BAbstractService confirmed. 80/83 domain blocks covered-by-sample) |
| medium | **KC6 program module runtime** — B426 covers ONLY compilation (spawned javac). Uncovered: `BProgram` execution model, freeform vs robot program, program slots/wiring, program-ext lifecycle, the program-wb editor | `program-rt,-wb` (~55 vf) | **COVERED → B541** (BProgram extends BComponent, doExecute→impl.onExecute [ProgramBase 3 hooks]; executeOnChange link-driven. Freeform=persisted slot-wired vs Robot=one-shot run() not persisted superUser-only. Slots→SourceWriter generated getters/setters, BProgramAction reflects on<Name>(). Source in BProgramCode.source + bytecode in BCode.classFile BBlob — ALL in .bog. SANDBOX: program.requireSigning gate [unsigned+off=warning only, ties B18/security-audit], ProgramProtectionDomain=untrusted perms [file:^ only, exec blocked], ProgramRuntime.exec gated allowProgramRuntimeExec=false+audited, edit superUser-only. program-wb 4-tab editor, ProgramCompiler extends B426 Compiler. Expr-over-Program [B538 BP6] runtime reason) |
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

- **Open gaps — read-only investigable**: **6** (KC7–KC12). All source dirs existence-verified 2026-08-28.
- **Gaps closed**: 6 (KC1→B536 … KC6→B541).
- **requires-execution / blocked**: 0.
- **Coverage metric**: 6 / 12 investigable gaps closed.
- **NEXT**: KC7 (honeywellFunctionBlocks per-FB catalog — the ~158 DDC blocks vs 50-page doc; B103=engine) → B542.

## Iteration history

| Iter | Gap | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|
| seed | AUDIT-FIRST coverage sweep (control/kitControl/program/rules/HVAC) | — | yes · sonnet (verified inline) | KC1–KC12 seeded from the coverage matrix |
| 1 | KC1 control module internals (writable-point model, arbitration, override, extensions) | B536 | yes · sonnet (sweep) + inline token-verify | none new (KC8 continuation noted) |
| 2 | KC2 kitControl FB catalog (151 classes → ~130 blocks; PID/latch/switch/select; multi-input null contract) | B537 | yes · sonnet (sweep) + inline token-verify | none new (KC4 will deepen BLoopPoint) |
| 3 | KC3 programming RULES (official Tridium rules layer, re-scoped; reconciled w/ code kernel B6 §6.2 + B536/B537) | B538 | yes · sonnet (doc sweep) + inline token-verify (7/12 rows) | none new; refines B536 (Boolean In6) |
| 4 | KC4 PID/BLoopPoint deep (ramp, executeTime, direct/reverse, disableAction, loop alarm, propagateFlags, official tuning) | B539 | yes · sonnet (code+doc sweep) + inline token-verify (7/11 rows) | none new (alarm-ext chain out of focus) |
| 5 | KC5 clHVAC control sequences (heating curve, mixing damper/economizer, 12-chiller lead-lag; upgrades B87 §87.3 [CERT-a]→[CERT]) | B540 | yes · sonnet (decompile sweep) + inline token-verify (5/10 rows) | none new (80/83 domain blocks covered-by-sample; degree-days window [INFER]) |
| 6 | KC6 program module runtime (BProgram exec, freeform/robot, slot wiring, .bog storage, signing+SecurityManager sandbox, program-wb editor) | B541 | yes · sonnet (code sweep) + inline token-verify (5/11 rows) | none new (batch/module ProgramModule pkg out of focus; ties B18/security-audit/signing-pki) |
