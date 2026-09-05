# RESEARCH-STATE — focus: module-ux-testing-and-write-surface (BOOTSTRAPPING)

> Multi-focus corpus (METHODOLOGY §16). Merges two operator/lead-requested angles that share ONE surface —
> the module `-ux` servlet/SPA write path: (angle 2 residue) **how to TEST the `-ux`/`-wb` layer off-station**,
> and (angle 3) **how to VERIFY + GATE the `-ux` servlet WRITE-SURFACE authorization**. Merged because the thing
> you would test IS the thing you would secure (the same BWebServlet write endpoint). Feeds the `/build-n4-module`
> kit: `types/dashboard.md` + `types/wb-widgets.md` + a testing doctrine.
>
> **Angle (§b2):** the `-ux` write-surface as a BUILD-KIT concern — (a) the off-station testable seams of a
> servlet + pure JS router / SPA, (b) the authorization enforcement a write endpoint must carry, and (c) the
> `-wb` layer's testability. READ-ONLY over the subject. Corpus language for NEW blocks = **English** (post-B115).
>
> **Bootstrapped** 2026-09-05 (lead investigador, campaign6 research lane; merge confirmed). Block range for this
> focus = **B762–B771** (lead-allocated; census lane owns B772–B791; global numbering, holes tolerated).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 0
gaps_closed: 0
known_gaps: 3
investigable_open: 1
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: module-ux-testing-and-write-surface
status: bootstrapping (0/3; investigable_open=1, synthesis pending evidence)
seeded_from: AUDIT-FIRST coverage check 2026-09-05 (corpus-nav: "test the servlet"/"pure router test"/"SPA unit test"/"ux test" = No matches → the -ux/-wb TESTING half is genuinely uncovered; the AUTHZ half is REMITTANCE)
seeded_on: 2026-09-05
gaps_total: 3 (UXT1 genuine · UXT2 playbook-synthesis · UXT3 bounded determination)
blocks_written: none yet; next for this focus = B762
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Gap-backlog (prioritized) — AUDIT-FIRST seeded (GAP NUMBERS ARE HYPOTHESES)

| Priority | Gap | Where | Status |
|---|---|---|---|
| high | **UXT1 — `-ux`/`-wb` OFF-STATION testing (the genuinely uncovered gap).** What of a `-ux` servlet is unit-testable in WSL without a station: the pure routing/dispatch seam separable from `BWebServlet` (request/response), a `DashboardReader`-style pure data-shaper, and the SPA JS as pure functions (`node --check` on extracted `<script>`, a headless node harness). Is there any off-station `-wb` test path, or is `-wb` strictly station/Workbench-only (`BTestNgStation`)? | test-wb (`javax/baja/test`), bajaux, our DashboardPan-ux / chihuahua-ux/-wb + corpus-nav confirms No-match | **OPEN — evidence sweep running → B762** |
| high | **UXT2 — the `-ux` write-surface PLAYBOOK (synthesis, no new decompile).** Wire the COVERED authorization knowledge + the UXT1 test seams into one build-kit doctrine: how to build → test → secure a `-ux` servlet write endpoint for OUR modules (OPERATOR_WRITE fail-closed check + category/permission map + CSRF/X-Requested-With guard + the audit trail), and WHERE each piece is tested (pure router unit test off-station, live smoke on the JACE). | synthesis of REMITTANCE below + UXT1 | **OPEN (blocked on UXT1) → B763** |
| medium | **UXT3 — `-wb` off-station testability determination (bounded).** Confirm whether a `-wb` FieldEditor/Manager/View has ANY WSL-runnable pure seam, or is entirely station-bound; if station-bound, say so cleanly (the honest negative) so the kit's `-wb` doctrine (`types/wb-widgets.md`, rung-0 today) states it. | test-wb, bajaui, wb-ux-authoring B751 | **OPEN (bounded) → B764 if it earns a block, else folded into UXT1** |

### REMITTANCE (already covered — will NOT be re-derived, only CITED)

**Control-logic (rt) testing — the covered half:**
- **B743** — the layered answer: math seam (pure JUnit, WSL) + scheduler seam (DI a `Sched` interface, fake-recorder) + live smoke; `BTestNgStation` boots a real station but is NOT WSL-runnable; the residual truth = no unit test proves the framework calls `started()`/`atSteadyState()`.
- **B12** — build/test lifecycle: `BTestNg` (unit), `BTestNgStation` (station-based), `srcTest/` co-location, TestNG, `TestHelper.waitFor()`, JaCoCo.
- **bloque TI** — Niagara Test Framework empirical audit (`test.exe`/`TestRunner`, `nre.properties`), and the niagara-help/`niagaraTest`-discovers-0-in-WSL fact.
- **B176** — chihuahua MX60 `niagaraTest` gap.

**Servlet write-surface AUTHORIZATION — the covered half:**
- **chihuahua-source CS3 (B648–B655)** — the ONLY production module: its write-path servlet ENFORCES RBAC via `ChiRbacHelper` + `ChiAuditHelper` (good, unlike the mcpbridge bypass B643). This is the exemplar of a correctly-gated write endpoint.
- **B752 (wb-ux-authoring)** — the RBAC contrast: vendor `-ux` = unrestricted; OURS must be `OPERATOR_WRITE` fail-closed.
- **access-control B558–B566** — the RBAC subsystem (users/roles/permissions/categories, `BCategoryService`, `BPermissions`, encoders, audit-wiring).
- **B58** — servlets + CSRF; **B74** — the `X-Requested-With: XMLHttpRequest` guard (the real guard code + its bypass surface).
- **MBP2 (module-best-practices)** — `-ux` thin-shim + server-side RBAC, `requiredPermissions` = visibility not authorization, ES5-strict.

**`-ux`/`-wb` authoring mechanics:** wb-ux-authoring **B751** (WB ladder rung 0-3, Manager/View/FieldEditor recipes), **B752** (3 serving recipes servlet-SPA / bajaux @AgentOn / PX), **MBP3** (`-wb` when-needed rule). Cite; do not re-derive the framework (B9/B22/B29/B427-432 = REMITTANCE).

## Stop control (METHODOLOGY §8)

- **Investigable open**: 1 (UXT1). UXT2 is synthesis gated on UXT1; UXT3 is a bounded determination that may fold into UXT1.
- This is a SMALL, synthesis-heavy focus by design — the authz half is fully covered (REMITTANCE), the genuine research is the single uncovered TESTING gap. Honest scope, not padded.
- STOP when: UXT1 documents the off-station `-ux`/`-wb` test seams [CERT with file:line] and UXT2 wires the build-kit playbook; then §18 retro with the `types/dashboard.md` + `types/wb-widgets.md` + testing-doctrine deltas for the kit.

## Iteration history

| Iter | Gap | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|
| seed | AUDIT-FIRST coverage check (corpus-nav testing terms → No-match; authz terms → covered) | — | inline (investigador1) | UXT1 (genuine), UXT2 (playbook), UXT3 (bounded) |
