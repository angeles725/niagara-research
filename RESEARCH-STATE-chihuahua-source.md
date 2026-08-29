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
covered_blocks: 651
gaps_closed: 8
known_gaps: 8
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks**: 0 in this focus (corpus-wide count synced by the tool)
- **Coverage metric**: 8 / 8 closed (FOCUS CLOSED)
- **Last iteration**: 2026-08-29 — bootstrap (source-tree orient)

## Remittances (already covered — cite, do NOT re-derive)

- **chihuahua jar-level audit** — [B636] (over-permission, builds vs 4.13 [§14 deliberate B638], profile drift, no palette, ux weight). This focus is the SOURCE-level deepening.
- **Reference build template** — [B647] + [B637]-[B639] (build variants, version-targeting, signing, tests). Grade the source against it.
- **mcpbridge authz-bypass** — [B643] (the contrast: chihuahua HAS ChiRbacHelper/ChiAuditHelper — verify it does write-auth right).
- **chihuahua corpus (behavioral)** — [B163]-[B177] (the tri-part rt/ux/wb, RBAC write-gate, ES5 IIFE frontend documented earlier); this focus reconciles against current source.

## Gap-backlog

| Priority | Gap | Artifact type / source | Status |
|---|---|---|---|
| high | CS3 — the ux servlet WRITE-AUTH path: does `BChiServlet`/`ChiServletDispatch` enforce RBAC via `ChiRbacHelper` + audit via `ChiAuditHelper` on every write (contrast mcpbridge [B643] bypass)? per-op permission + runAsUser + CSRF | Java · chihuahua-ux/src/.../BChiServlet.java + ChiServletDispatch.java + ChiRbacHelper.java + ChiAuditHelper.java | ✅ B648 — AUTHZ ENFORCED CORRECTLY (inverse of mcpbridge): ChiRbacHelper.checkCanWrite = BPermissions.has(OPERATOR_WRITE) via BUserService (ADR D1/D2), 401 no-user/403 no-perm/fail-closed, FIRST line of all 8 write handlers before mutation; audit {ts,user,action,ord,old,new} JSON-lines ring ~500 on every write; dispatch pure+guards (traversal+XHR-CSRF). Caveats: global-not-category perm, set(null)=ambient ctx not runAsUser, plaintext audit, XHR-only CSRF — all documented in source |
| high | CS2 — the rt control/equipment model: `BChiUp`/`BChiCarcamo`/`BChiDatalogger`/`BPlanta` + Monitors + `ChiLinkHelper` — component design, protection slots, the writable/control logic, defensive behavior | Java · chihuahua-rt/src/.../components/*.java | ✅ B650 — monitor/dashboard + ONE computed output (effectiveSetpoint); Monitor=one-shot factory not poll; SW protections (applyProtections 10s+COV). SAFETY: readSlotVal collapses faulted sensor→0.0 → antifreeze(low-limit) fails SAFE (trips), overload(high-limit) fails to NON-trip (silently disabled if amp sensor faults) — recommend fault-alarm not 0.0. Permanent latches. STRONG defensive guards (null/NaN/epsilon/JSON/event-thread ADR-D7/steady-state). Stale slotomatic AWAITING REGEN on protXActive (B637). No System.out/TODO |
| high | CS6 — reconcile the internal `audit-2026-05-06/` findings (veredicto/inconsistencias/pendientes/live_updates_faltantes) against current source — what was fixed, what remains | docs+source · chihuahua/audit-2026-05-06/*.md + source | ✅ B651 — internal audit (2-agent adversarial, CONDITIONAL PASS 14/14) is RIGOROUS + fixes HOLD in current source (P1 fault-discrim readNumericNullable confirmed). KEY: P1 fixed the DISPLAY path (ChiEquipmentReader null-aware); the PROTECTION path (readSlotVal 0.0-collapse, B650) is the complementary RESIDUAL → extend fault-discrim into applyProtections. Open non-blockers: renderShell focus loss, timestamp tick, chart perf |
| high | CS1 — manifest/build/gradle vs the [B647] template: which recommended fixes are applied in source? (`<permissions>`, version, slotomatic markers, deps, part gradle) | source · build.gradle.kts + part .gradle.kts + module-include.xml + module-permissions.xml | ✅ B649 — §14 CORRECTION: 'over-permissioning' (B636#1/B640 P1) is UNTOUCHED Tridium scaffold — empty <niagara-permission-groups> (placeholders only, req-permission commented out), NO <java-permissions> → requests only base grant, NOT over-privileged (fleet-wide). Build else GOOD: real version 1.0→1.3 (best of fleet), plugin 7.3.40/4.13.2 SDK, slotomatic markers in 8 sources, correct multi-part. Real deviations: no palette (B636#4), dead jacoco/niagaraTest wiring (7.6.17) |
| medium | CS4 — the ux data/query helpers: `ChiHistoryHelper`/`ChiAlarmHelper`/`ChiAlarmQueryHelper`/`ChiThresholdHelper`/`ChiScheduleHelper`/`ChiEquipmentReader`/`ChiJsonUtil` — BQL/history/alarm patterns + the N4.14 gotchas | Java · chihuahua-ux/src/.../*.java | ✅ B652 — INJECTION-SAFE + N4.14-gotcha-aware: history via History API not BBqlGrid (dodges B359 NPE), stride downsample (B369), alarm BQL from long epoch millis + fixed ackState + escaped ORD (no user-string), threshold writes allowlisted+value-guarded, ChiJsonUtil.escapeJson complete (ctrl+U2028/9). Clean (no System.out/TODO) |
| medium | CS5 — the front-end architecture: the ES5 JS app (DashboardApp, stores/managers, AlarmLatchStore, CapabilityStore, Three.js/Chart.js), FRONTEND_ARCHITECTURE.md contract, cache-invalidation | JS · chihuahua-ux/src/rc/js/** + FRONTEND_ARCHITECTURE.md | ✅ B653 — strict-ES5 store/subscription SPA (window.MX60 IIFEs); live = BajaScript Fox subscriptions + 5s REST fallback (no SSE); optimistic-write+rollback; RBAC SERVER-AUTHORITATIVE (CapabilityStore DECORATIVE ADR D6, confirms B648); Three.js ES-module island + Chart.js UMD local-bundled. Production-quality |
| low | CS7 — the wb Batch Link Editor: `BBatchLinkEditor` + PendingLink/LinkSlotName/Direction utils — the Workbench-view tooling | Java · chihuahua-wb/src/.../*.java | ✅ B654 — BBatchLinkEditor = proper BWbComponentView, @AgentOn baja:Component requiredPermissions=rwi; bulk-link workflow accumulate→dry-run checkLink→Save All per-space Transaction + unique slot names; DISTINCT from rt ChiLinkHelper (author vs backup/restore); pure-Java testable model. A productivity strength |
| high | CS8 — SYNTHESIS: production-readiness verdict for chihuahua + the concrete fix list (from CS1-CS7 + [B647] remediation) — the deliverable | design synthesis over CS1-CS7 + [B647]/[B636] | ✅ B655 — VERDICT: chihuahua is WELL-BUILT / production-grade (RBAC+audit+defensive+injection-safe+internal-QA), clearly best of fleet. ONE real fix (HIGH/safety): overload protection fails-to-danger on faulted amp sensor (extend P1 fault-discrim into readSlotVal/applyProtections). Then MED run slotomatic; LOW palette/dead-jacoco/empty-perms-scaffold/audit-polish. Fleet gap = process not capability; template should inherit chihuahua practices. FOCUS CLOSED |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 0 | 2026-08-29 | (bootstrap) source-tree orient + backlog seeded | — | no·inline (fd/ls source tree) | 8 |
| 1 | 2026-08-29 | CS3 servlet write-auth (RBAC enforced, mcpbridge inverse) | B648 | yes·sonnet + inline re-grep (found 8 not 6 sites) | 0 |
| 2 | 2026-08-29 | CS1 build vs template + §14 over-perm=empty scaffold | B649 | no·inline (source read + verify) | 0 |
| 3 | 2026-08-29 | CS2 rt control/protection (overload fail-to-danger finding) | B650 | yes·sonnet + inline safety-verify | 0 |
| 4 | 2026-08-29 | CS6 reconcile internal audit-2026-05-06 (fixes hold; P1 display vs B650 protection) | B651 | no·inline (audit docs + source verify) | 0 |
| 5 | 2026-08-29 | CS4 ux data/query helpers (injection-safe, N4.14-aware) | B652 | yes·sonnet (combined) + inline security-verify | 0 |
| 6 | 2026-08-29 | CS5 frontend ES5 SPA (RBAC server-authoritative) | B653 | yes·sonnet (combined) + inline verify | 0 |
| 7 | 2026-08-29 | CS7 wb BatchLinkEditor (proper WB view) | B654 | yes·sonnet (combined) + inline verify | 0 |
| 8 | 2026-08-29 | CS8 SYNTHESIS production-readiness verdict (FOCUS CLOSED) | B655 | no·inline (synthesis over B648-B654) | 0 |

## Blocked gaps (each tagged with what it needs)

- none — all read-only over the source on disk.

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0   ← the STATIC loop STOPS when this hits 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- **FOCUS STOPPED** 2026-08-29: 8/8 (CS1-CS8); requires-exec residues=overload-fix live-validation + MCP-G2 (need API2); §18 retro pending
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (real source tree; no census — subject artifacts are the source files)
