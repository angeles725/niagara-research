# RESEARCH-STATE — focus: wb-ux-authoring (how modules AUTHOR their Workbench (-wb) and browser (-ux) layers — the authoring taxonomy across reference modules + a playbook for ours)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-09-03** (operator: "investiga todo lo que
> puedas sobre WB y UX de los módulos"). The WB/UX analog of the RT-authoring campaign (B729-B750) and the
> Honeywell organization taxonomy (B749/B750): NOT the wb/ux FRAMEWORK internals (already reconstructed —
> REMITTANCE), but how a module's own `-wb` and `-ux` parts are BUILT, distilled across the reference modules
> (Honeywell + Tridium core + our own), plus a playbook for ColdRoomPan/CompPan/DashboardPan. DESIGN/APPLIED
> corpus — high [INFER] expected at synthesis; every authoring FACT cites a class + file:line.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 750
gaps_closed: 5
known_gaps: 5
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: wb-ux-authoring
status: stopped (5/5 investigable closed; WB taxonomy B751 + UX taxonomy B752 + playbook B753; 2 requires-execution gaps open; next free block B754)
bootstrapped_on: 2026-09-03
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B754)

## Coverage

- **Covered blocks**: 750 corpus-wide (this focus: B751-B753) (shared-global)
- **Coverage metric**: 5 / 5 gaps closed
- **Deliverable**: B751 (WB) + B752 (UX) + B753 (playbook) — feed build-n4-module types/wb-widgets.md [seed] + types/dashboard.md [mature]

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | WBUX1 — WB authoring across HONEYWELL modules: agent registration, Manager/View/FieldEditor/Wizard/DeviceModel/Command patterns in real -wb parts | code | closed (sweep → B751: the device-model PLUGIN framework honeywellDeviceManager-wb; TB3026B tabbed-view "wizard"; honImporter BDialog step-wizard; command→BJob) |
| high | WBUX2 — WB authoring across TRIDIUM core + OUR modules: the reusable Manager/View/FieldEditor recipes + the "how much wb is enough" decision rule in practice | code | closed (sweep → B751: the 4-rung ladder; driver-wb base template, bacnet-wb showcase; kitControl 2FE/control 5FE bar; our -wb absent from corpus) |
| high | WBUX3 — UX (bajaux) authoring across modules: the BSingleton+BIJavaScript shim, JS/HTML payload, RPC/data channel, permissions | code | closed (sweep → B752: two data-channel dialects serverSideCall vs baja.rpc; JS spectrum ES5→built→React; vendor unrestricted-RPC vs our OPERATOR_WRITE RBAC) |
| high | WBUX4 — UX serving: servlet-served SPA (DashboardPan) vs bajaux @AgentOn view vs PX page — the three ways to put UI in a browser + live-data channels | code | closed (sweep → B752: BWebServlet self-register chain; PX binding taxonomy + \|view: hyperlink; BQL/REST-poll/Fox/oBIX channels; our reusable SPA template) |
| high | WBUX5 — SYNTHESIS: the WB + UX authoring taxonomy (recurring patterns) + the playbook for ColdRoomPan/CompPan/DashboardPan | synthesis+deliverable | closed (B753 — our modules sit at wb rung 0; keep the servlet-SPA + OPERATOR_WRITE RBAC; PX/Fox/plugin only on-need) |

`tried:` (none blocked — all source is existing corpus blocks + real jars in organized/; SOURCE-BEFORE-AGENT passes).

## Remittance (framework FACTS these build on — cited, not re-derived)

- WB framework: **B427** (BWidget/gx), **B428** (BWbShell), **B429** (wire sheet), **B430** (property sheet +
  field-editor dispatch), **B431** (BAbstractManager), **B432** (commands/undo); **B707** (wb best practices).
- UX framework: **B9**/**B22** (UI stack, bajaux, PX, BajaScript), **B29** (web tier/servlets/Jetty), **B433**
  (hx render), **B706** (ux best practices), **B724** (web-hmi panel hardware).
- Our modules: DashboardPan-ux/wb, chihuahua-ux/wb (B163-177); Reflow ux (B138-155, B50).

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-09-03 | (bootstrap — WB/UX authoring campaign) | — | no · inline | WBUX1-5 seeded; 4 Explore sweeps launched |
| 1 | 2026-09-03 | WBUX1 + WBUX2 (WB authoring) | B751 | yes · 2× Explore (Honeywell wb + Tridium/ours wb) ~70 tool calls | B751-G1 (our -wb no exemplar), B751-G2 (async-view threading) |
| 2 | 2026-09-03 | WBUX3 + WBUX4 (UX authoring) | B752 | yes · 2× Explore (bajaux + servlet/SPA/PX) ~60 tool calls | B752-G1 (BJsBuild dep graph), B752-G2 (Fox subscriberMixIn contract) |
| 3 | 2026-09-03 | WBUX5 (playbook) | B753 | no · inline (synthesis of B751+B752 × our modules) | B753-G1 (.px companion page), B753-G2 (route() test coverage) — requires-execution |

## Blocked gaps (each tagged with what it needs)

(none — all synthesis over existing corpus + jars.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0 (STOP — all 5 closed in one run)
- **Open gaps — requires-execution**: 2 (B753-G1 .px companion page; B753-G2 route() test coverage) + info-only B751-G2/B752-G1/G2
- **Open gaps — blocked**: 0
- Budget cap: none

## Dismissed file types

- (to be filled by the coverage pass.)
