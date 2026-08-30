# B706 — Module best practices, UX layer (MBP2): the thin-Java-shim + JS pattern, why `requiredPermissions` is visibility-not-security, and the live-data patterns — distilled from the reference modules

> Focus: **module-best-practices** · Gap **MBP2** (ux layer). Block TYPE = **DESIGN/SYNTHESIS** (distilled from
> verified blocks + jars; high [INFER] ratio expected). Feeds `docs/module-best-practices.md` §2. Marker `[CERT]`
> where a claim re-cites verified code; `[INFER]` for guidance framing.

## 706.1 — UX patterns to copy

[CERT, sources cited]

- **P1 — Thin Java shim + JS.** The `-ux` Java class is a pure descriptor: `BSingleton` + `BIJavaScript` +
  `@AgentOn(types={...}, requiredPermissions=...)` + `JsInfo.make(BOrd.make("module://<mod>/rc/.../X.js"), …)`.
  Zero UI logic in Java; the UI lives in the JS at a `module://` ORD. ([Block 421] §421.2 `BBooleanEditor`;
  [Block 151] §151.2 `BReflow`).
- **P2 — `requiredPermissions` on `@AgentOn` = view VISIBILITY, not security.** It controls whether Workbench/Hx
  shows the view; it is NOT the authorization layer. A client that POSTs directly to the servlet/REST endpoint
  bypasses it. Server-side enforcement is separate and mandatory ([Block 421] §421.7, [Block 151] §151.3,
  [Block 644] §644.1).
- **P3 — Server-authoritative RBAC, fail-closed, first line of every POST.** Check
  `BPermissions.has(OPERATOR_WRITE)` (bit, not role-name) before parsing the body; 401 no-user, 403 insufficient,
  deny on any exception. The browser hides controls as "DECORATIVE ONLY (ADR D6)" convenience; the server
  re-enforces unconditionally ([Block 648] §648.1 `ChiRbacHelper`; [Block 653] §653.3).
- **P4 — Pure-web `-ux`: 0 Java is correct.** A `-ux` jar with no `.class` files and a full `rc/` tree
  (HTML/JS/CSS) is right design, not an empty shell. A HIGH class count in `-ux` is usually bundled libs —
  audit the histogram before calling it a defect ([Block 640] §640.5, [Block 645] §645.3, [Block 647] §647.5).
- **P5 — Fox-subscription primary + REST-poll fallback for live data.** Initial REST fetch (fast paint) → Fox
  subscriptions (real-time) → ~5 s REST fallback → buffer/replay subscription updates that arrive before the
  first fetch settles ([Block 653] §653.2 `EquipmentData.js`).
- **P6 — Optimistic write + rollback + refresh.** Update the store on the action, POST, roll back on failure,
  then re-read to confirm server state ([Block 653] §653.3 `AlarmLatchStore.js`).
- **P7 — Typed BQL, never user strings.** Build BQL from `long` epoch millis, fixed enum tokens, ORD-escaped
  sources — never interpolate user input ([Block 652] §652.2 `ChiAlarmHelper`).

## 706.2 — UX anti-patterns

[CERT, each cites the finding]

- **AP1 — Auth-gate-only / userless dispatch.** Gate on `getRemoteUser()!=null` then run all writes through a
  `static` userless dispatcher → any authenticated user (even read-only) gets full write. (mcpbridge
  `ToolDispatcher`, [Block 643] §643.2b.) Inverse-correct = `ChiRbacHelper` ([Block 648]).
- **AP2 — Agent-gate-as-security.** Using `@AgentOn requiredPermissions` to "protect" writes; the REST endpoint
  is a separate surface with no gate ([Block 151] §151.3 / [Block 145] §145.1 — Reflow config-update).
- **AP3 — Per-module uber-jar shading.** sdash-rt 2186 classes (96% Jackson+Commons), mcpbridge/datacenter each
  bundle their own Gson ([Block 644] §644.2, [Block 643] §643.1, [Block 645] §645.1). Fix = shared `gson-rt`/
  `jackson-rt`, or use `jsonToolkit` ([Block 335]).
- **AP4 — Assuming ES6 in the dashboard main app.** The Niagara WebKit/JxBrowser renderer targets **ES5**; the
  main app must be ES5 IIFE (`window.MX60`), with modern modules only as importmap islands (Three.js) or UMD
  globals (Chart.js) ([Block 653] §653.1 `DashboardApp.js:17`).
- **AP5 — Site data hardcoded into the `-ux` jar.** e.g. datacenter rack/location layout baked as JS in `rc/`
  → every site change needs a rebuild; belongs in a configurable `-rt` component tree ([Block 645] §645.4).

## 706.3 — Top ux improvements

[INFER, prioritized]

1. **(HIGH, safety)** Same fault-discrimination fix as rt (I1/B705) — the ux display path was fixed
   (`readNumericNullable`→null) but the protection path was not ([Block 651] §651.3).
2. **(HIGH, security)** Per-user RBAC on any MCP/AI bridge before it reaches a station (AP1).
3. **(MED, packaging)** Factor shared JS/Java libs (Gson, Jackson, Three.js, Chart.js) into shared modules
   instead of per-dashboard shading (AP3).
4. **(MED, config)** Move hardcoded `-ux` site data into an `-rt` component tree (AP5).
5. **(LOW, default)** Adopt the Fox-sub + REST-fallback live-data pattern (P5) as the shop-template default.

## Connections

- Distills `chihuahua-source` [Block 648]–[Block 655] (RBAC, ES5, live-data), `chihuahua` [Block 163]–[Block 177],
  `webEditors`/bajaux [Block 421], `own-modules-audit` [Block 640]–[Block 647], `nmodsreflow-ux` [Block 151].
  rt sibling → [Block 705] (MBP1). Deliverable: `docs/module-best-practices.md` §2 (ux).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | thin shim: BSingleton+@AgentOn+JsInfo module:// (P1) | [CERT] | [Block 421]/[Block 151] | cited |
| 2 | requiredPermissions = visibility not security (P2/AP2) | [CERT] | [Block 151] §151.3 | cited |
| 3 | server-authoritative fail-closed RBAC (P3) | [CERT] | [Block 648] §648.1 | cited |
| 4 | ES5-strict dashboard runtime (AP4) | [CERT] | [Block 653] §653.1 | cited |
| 5 | uber-jar shading (AP3) | [CERT] | [Block 644]/[Block 643]/[Block 645] | cited |
| 6 | ux improvement priorities | [INFER] | 706.3 | reasoned |

**Tally:** [CERT] ×5 · [INFER] ×1. Block TYPE = **DESIGN/SYNTHESIS** — ratio healthy. Re-cites already-verified
blocks; no new extraction.

## Open gaps (this focus)

MBP2 CLOSED (ux). Next: **MBP3** (wb layer — Workbench Swing: managers/views/field editors, when wb is actually
needed). Then MBP4 (cross-cutting), MBP5 (build), MBP6 (exemplar catalog + guide).
