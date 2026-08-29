# chihuahua-source — Research State

> Operational state consumed by the loop (Research-SDD). Mirrored in engram
> (`research/niagara/chihuahua-source/gaps`, `.../progress`). Visible and versionable source.
>
> **Focus angle (§16 / §b2).** SOURCE-LEVEL audit of `chihuahua` — the operator's ONLY PRODUCTION module
> ([B643]) — against the reference build template [B647]. B636 audited the packaged jar; this reads the REAL
> SOURCE at `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/` (rt 17 java, ux 25, wb 13
> + a Three.js/Chart.js front-end + an internal `audit-2026-05-06/`). Nace del pedido "no estoy seguro que
> tan bien esté chihuahua". Key question: does the ux servlet write path ENFORCE RBAC (ChiRbacHelper +
> ChiAuditHelper) — the RIGHT way, contrasting mcpbridge's bypass [B643]? READ-ONLY over the source.
>
> Backlog from a source-tree orient 2026-08-29. Next block B648.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 643
gaps_closed: 0
known_gaps: 8
investigable_open: 8
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks**: 0 in this focus (corpus-wide count synced by the tool)
- **Coverage metric**: 0 / 8 closed
- **Last iteration**: 2026-08-29 — bootstrap (source-tree orient)

## Remittances (already covered — cite, do NOT re-derive)

- **chihuahua jar-level audit** — [B636] (over-permission, builds vs 4.13 [§14 deliberate B638], profile drift, no palette, ux weight). This focus is the SOURCE-level deepening.
- **Reference build template** — [B647] + [B637]-[B639] (build variants, version-targeting, signing, tests). Grade the source against it.
- **mcpbridge authz-bypass** — [B643] (the contrast: chihuahua HAS ChiRbacHelper/ChiAuditHelper — verify it does write-auth right).
- **chihuahua corpus (behavioral)** — [B163]-[B177] (the tri-part rt/ux/wb, RBAC write-gate, ES5 IIFE frontend documented earlier); this focus reconciles against current source.

## Gap-backlog

| Priority | Gap | Artifact type / source | Status |
|---|---|---|---|
| high | CS3 — the ux servlet WRITE-AUTH path: does `BChiServlet`/`ChiServletDispatch` enforce RBAC via `ChiRbacHelper` + audit via `ChiAuditHelper` on every write (contrast mcpbridge [B643] bypass)? per-op permission + runAsUser + CSRF | Java · chihuahua-ux/src/.../BChiServlet.java + ChiServletDispatch.java + ChiRbacHelper.java + ChiAuditHelper.java | pending |
| high | CS2 — the rt control/equipment model: `BChiUp`/`BChiCarcamo`/`BChiDatalogger`/`BPlanta` + Monitors + `ChiLinkHelper` — component design, protection slots, the writable/control logic, defensive behavior | Java · chihuahua-rt/src/.../components/*.java | pending |
| high | CS6 — reconcile the internal `audit-2026-05-06/` findings (veredicto/inconsistencias/pendientes/live_updates_faltantes) against current source — what was fixed, what remains | docs+source · chihuahua/audit-2026-05-06/*.md + source | pending |
| high | CS1 — manifest/build/gradle vs the [B647] template: which recommended fixes are applied in source? (`<permissions>`, version, slotomatic markers, deps, part gradle) | source · build.gradle.kts + part .gradle.kts + module-include.xml + module-permissions.xml | pending |
| medium | CS4 — the ux data/query helpers: `ChiHistoryHelper`/`ChiAlarmHelper`/`ChiAlarmQueryHelper`/`ChiThresholdHelper`/`ChiScheduleHelper`/`ChiEquipmentReader`/`ChiJsonUtil` — BQL/history/alarm patterns + the N4.14 gotchas | Java · chihuahua-ux/src/.../*.java | pending |
| medium | CS5 — the front-end architecture: the ES5 JS app (DashboardApp, stores/managers, AlarmLatchStore, CapabilityStore, Three.js/Chart.js), FRONTEND_ARCHITECTURE.md contract, cache-invalidation | JS · chihuahua-ux/src/rc/js/** + FRONTEND_ARCHITECTURE.md | pending |
| low | CS7 — the wb Batch Link Editor: `BBatchLinkEditor` + PendingLink/LinkSlotName/Direction utils — the Workbench-view tooling | Java · chihuahua-wb/src/.../*.java | pending |
| high | CS8 — SYNTHESIS: production-readiness verdict for chihuahua + the concrete fix list (from CS1-CS7 + [B647] remediation) — the deliverable | design synthesis over CS1-CS7 + [B647]/[B636] | pending |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 0 | 2026-08-29 | (bootstrap) source-tree orient + backlog seeded | — | no·inline (fd/ls source tree) | 8 |

## Blocked gaps (each tagged with what it needs)

- none — all read-only over the source on disk.

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 8   ← the STATIC loop STOPS when this hits 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (real source tree; no census — subject artifacts are the source files)
