# graphql-admin — Research State

> Operational state consumed by the loop (Research-SDD). Mirrored in engram
> (`research/niagara/graphql-admin/gaps`, `.../progress`). Visible and versionable source.
>
> **Focus angle (§16 / PROMPT-LOOP §b2).** Feasibility + reference architecture of a **DIY GraphQL
> layer to ADMINISTER a Niagara N4 station from a custom "dashboard module"**, anchored in the REAL
> servlet/API/RBAC surface already documented by the corpus. Established fact: N4 has NO native GraphQL
> (the only trace in code is the `application/graphql` MIME constant in `httpClient-wb`). This focus is
> EVIDENCE-grounded DESIGN/APPLIED: each block reads the concrete Java seam a resolver would hook into,
> then states how a GraphQL admin layer maps onto it. READ-ONLY over disk; no live probes required.
>
> Backlog derived from the AUDIT-FIRST coverage sweep 2026-08-29 (sonnet, 39 tool calls) with
> PRE-DECLARE REMITTANCES: the servlet/auth/RBAC/data-access/subscription layers a GraphQL layer sits on
> are ALREADY covered and are cited by REMITTANCE, not re-derived — see "Remittances" below. The `apis`
> focus (API1–API9 → B507–B516) is FULL REMITTANCE for the transport primitives.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 611
gaps_closed: 5
known_gaps: 8
investigable_open: 3
requires_execution_open: 0
blocked_open: 0
deferred_open: 1
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks**: 0 in this focus (corpus-wide count synced by the tool; global prefix `niagara-mental-model-bloque`)
- **Coverage metric**: 5 / 8 closed
- **Last iteration**: 2026-08-29 — GQL-G6 closed (B615: native dashboard-ux is a thin JS-widget agent; build a sibling module)

## Remittances (already answered by an existing cited block — do NOT re-derive)

- Servlet mount / SPI — [B29] §29.2 (`BWebServlet` dynamic Jetty mount) · [B163] §163.2 (full author recipe, own source) · [B508] (URL routing table) · [B433] (`BHxView` servlet-view pattern)
- Request dispatch / CSRF — [B58] §58.1 (`BaseServlet` router + `CsrfGuard`) · [B165] (pure-function dispatch) · [B602] (CSRF synchronizer token) · [B507] (`/rpc` envelope) · [B508] (`/ord` pipeline)
- Auth — [B457] (SCRAM login recipe) · [B508] §508.3 (N4 Digest = SCRAM) · [B510] (`BAuthenticationScheme` SPI) · [B29] §29.4 (session/cookies)
- RBAC — [B11] (category/bits model) · [B30] (slot-level enforcement) · [B163] §163.3 (per-handler write-gate) · [B561] (`BCategoryService`) · [B589] (nav-tree permission)
- Data access — [B4]/[B5] (Baja object model + ORD/BQL) · [B38] (`BOrd` resolution) · [B408] (`BComponentSpace`) · [B509] (oBIX read/write face) · [B76] §76.2 (slot read) · [B536] (writable-point)
- Mutations — [B507] (`@NiagaraRpc`) · [B536]/[B544] (priority-array write) · [B595]–[B599] (transfer/deploy engine) · [B511] (`BJob` async)
- Live push — [B36]/[B42] (BajaScript BOX) · [B512]/[B554] (BOX wire + mux) · [B59] (custom WebSocket precedent) · [B555] (multi-user stack)
- JSON — [B76] §76.1 (`com.tridium.json`) · [B335]–[B349] (jsonToolkit marshaller) · [B347] §347.3 (3rd-party JAR bundling precedent, Gson)
- Dashboard frontend — [B204] (bajaux Widget) · [B216]–[B231] (Reflow builder) · [B163]/[B170]/[B171] (chihuahua frontend, closest precedent) · [B47] (headless SPA bootstrap)
- Module build — [B12] (module plugin) · [B176] (gradle pipeline + Java 8 constraint, own source)
- Transport primitives (`apis` focus) — API1 `@NiagaraRpc`→[B507] · API2 ORD-over-HTTP→[B508] · API3 oBIX→[B509] · API4 BOX→[B512] · API5 auth-SPI→[B510] · API6 Fox→[B513] · API7 BJob→[B511] · API8 BQL→[B514]

## Gap-backlog

<!-- Priority: high | medium | low | deferred. Status leading token: pending | requires-execution |
     blocked-on-<reason> | ✅ | ~~. All sources confirmed present on disk (SOURCE-BEFORE-AGENT §e2). -->

| Priority | Gap | Artifact type / source | Status |
|---|---|---|---|
| high | GQL-G1 — exact API to extract the session `BUser`/`Context` inside a custom `BWebServlet.doService()` so component reads/writes run AS the session user (RBAC-correct), not the platform user | Java · organized/web/web-rt/vineflower/javax/baja/web/{WebOp,BWebServlet}.java | ✅ B611 — Context = `req.getAttribute("niagara.context")`; WebOp IS a Context; baseline `hasOperatorRead()` pre-gated; thread `op` into every op |
| high | GQL-G2 — `@NiagaraRpc` method-body Context injection contract: does `NiagaraRpcServlet` inject the session-user Context into the dispatched method, and what signature lets a resolver do `set(val, ctx)` as that user | Java · web-rt NiagaraRpcServlet + javax/baja/rpc | ✅ B613 — fresh SecurableContext (user=getCurrentAuthenticatedUser) appended as last arg; method declares Context last; 4 gates (transport/TLS/object-perm/protectedTargets); "unrestricted" skips object gate |
| high | GQL-G3 — `OrdTarget.canRead()`/`canWrite()` per-resolver RBAC primitive (construction from an ORD, what the check evaluates: permission bit + category mask) | Java · organized/baja/baja/vineflower/javax/baja/naming/OrdTarget.java | ✅ B612 — OrdTarget IS a Context (user copied from cx); canRead/canWrite/canInvoke → BIProtected.canX(this); FAIL-OPEN if no protected ancestor |
| high | GQL-G4 — concrete servlet-handler call-site: run a BQL query + invoke a BComponent action / set a slot via the session Context, then serialize to JSON with `com.tridium.json.JSONWriter` | Java · bql-rt + javax/baja/sys/BComponent.java + com.tridium.json | ✅ B614 — every mutate/read API has a Context overload (thread `op`); null-context overload = footgun; BITable.cursor()→cell(); JSONWriter (B76); writable=priority-array (B536) |
| medium | GQL-G5 — module classloader isolation: is each `-rt` JAR parent-last isolated (bundled graphql-java/Gson coexist) or a flat shared classpath (conflict risk)? | Java · organized/baja/baja/vineflower/com/tridium/sys/module/{ModuleClassLoader,ModuleExtClassLoader,AutoClassLoader,SyntheticModuleClassLoader}.java | pending |
| medium | GQL-G6 — native `com.tridium.dashboard.ux` module: what it is (Hx Px-pane renderer? bajaux host?) and whether a GraphQL-backed dashboard module embeds alongside, replaces, or ignores it | Java · organized/dashboard/dashboard-ux/vineflower/com/tridium/dashboard/ux/{BDashboardCssResource,BDashboardJsBuild,BUxDashboardPane}.java | ✅ B615 — 3 singletons (BSingleton @AgentOn binds JS widget + JsBuild + CssResource); zero data backend; build a SEPARATE sibling module (chihuahua B163 model) |
| medium | GQL-G7 — BOX server-side channel extension seam: can a module register a NEW BOX channel type for GraphQL subscriptions, or must it roll its own WebSocket (B59 `BReflowWebSocketAcceptor` precedent)? | Java · organized/box/box-rt/vineflower/com/tridium/box/{BBoxChannel,BBoxService,BComponentSpaceSessionHandler}.java | pending |
| medium | GQL-G8 — graphql-java viability under the mandatory Java 8 bytecode constraint (B176): which release line is the last Java-8 build, and is a hand-rolled/alternative parser needed? | web (Maven Central release history) + corpus [B176] | pending |
| deferred | GQL-G9 — SYNTHESIS: reference architecture + build-vs-buy verdict (focus-closing block, written at STOP) | design synthesis over G1–G8 | pending (parked; never NEXT — §8b) |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 0 | 2026-08-29 | (bootstrap) AUDIT-FIRST sweep + backlog seeded | — | yes · sonnet (coverage sweep) | 8 |
| 1 | 2026-08-29 | GQL-G1 session-user Context seam | B611 | inline (constraint: load-bearing security seam, 3 files) | 0 |
| 2 | 2026-08-29 | GQL-G3 OrdTarget per-field RBAC + fail-open | B612 | inline (constraint: load-bearing security seam, 2 files) | 0 |
| 3 | 2026-08-29 | GQL-G2 @NiagaraRpc Context injection contract | B613 | yes · sonnet (rpc sweep) + inline token-verify | 0 |
| 4 | 2026-08-29 | GQL-G4 concrete resolver call-site (read/mutate/JSON) | B614 | inline (constraint: synthesis over remitted primitives) | 0 |
| 5 | 2026-08-29 | GQL-G6 native dashboard-ux module survey | B615 | inline (constraint: 3-file complete survey) | 0 |

## Blocked gaps (each tagged with what it needs)

- none (all 8 investigable gaps have a confirmed on-disk source; GQL-G8 resolves from web release notes, not a live system)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 3   ← the STATIC loop STOPS when this hits 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (focus reuses the existing decompiled corpus; no new census — subject artifacts already extracted under `organized/`)
