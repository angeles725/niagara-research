# B813 · UX servlet authoring conventions — registration/routing/lifecycle, the JSON+facet+CSRF contract, from Tridium's own BWebServlet vs our dashboards `[CERT]`

> The `-ux` servlet AUTHORING MECHANICS not covered by the write-surface blocks: how a `BWebServlet` registers,
> routes, and serves; the server-side facet gap; the real CSRF token; and the request-path residue in our
> `BDashboardServlet`/`BChiServlet`. Backs PR12 (servlet lint). The write-surface seam, DWS1 gates, and step-up
> design are cited, not re-derived.
>
> **Sources**: REMITTANCE [Block 796] (DashboardPan-ux write-surface exemplar / DWS1 5-gate), [Block 803]
> (step-up + CSRF), [Block 762]/[Block 763] (DWS1 gates + wb/model seam), [Block 809] (wb sibling), [Block 58]/
> [Block 29] (servlet/CSRF filter), [Block 806]/[Block 9] (Jetty vs engine thread). NEW code (driver-verified by
> grep): `javax.baja.web.BWebServlet` (web-rt), `CsrfUtil` (docSource), `BComponent` (baja), our
> `BDashboardServlet`/`BChiServlet`. Markers: `[CERT]` code `file:line` (decompiled = extern) · `[INFER]`.
>
> **Type:** `mixed`. Connects [Block 796]/[Block 803]/[Block 763], [Block 806]/[Block 808] (servlet threading/health).

## 813.1 — Servlet authoring mechanics `[CERT]`
- **Registration is LIFECYCLE-driven, no `module.xml` entry:** `BWebServlet` extends `BAbstractService`; its `fw()`
  handles component IDs `11→register()` / `12→unregister()` (`BWebServlet.java:149-166`), and `register()` calls
  `BWebServer.register(this)` keyed on the `servletName` property (`:52-59`). Drop one instance under station
  `Services`, start it, it self-registers its URL prefix. Our modules HARDCODE `getServletName()` (`BDashboardServlet.java:81`
  → `"dashboardpan"`; `BChiServlet` → `"mx60"`) instead of the editable property — non-configurable but no
  wrong-name-in-prod risk. `[CERT]`
- **Routing:** `BWebServlet.doService()` resolves the `OrdTarget`, then `service(op)` dispatches by HTTP method to
  `doGet/doPost`. Inside, our modules delegate to a PURE dispatcher `route(method, path, headers, params)` →
  a sealed `RouteAction` ([Block 796] exemplar); `path = req.getPathInfo()` = the sub-path after the prefix. `[CERT/REMITTANCE B796]`
- **Static rc/ serving:** assets come from `getClass().getClassLoader().getResourceAsStream("rc/"+path)`
  (`BDashboardServlet.java:408`), with a traversal guard (`..`, `\\`, `\0`) IN the servlet — a valid alternative to
  Tridium's `/module/` route ([Block 5]/[Block 752]) but the guard must live in the servlet (both modules have it). `[CERT]`
- **JSON contract:** our error shape is `{"error":"…"}` + a correct HTTP status (400/401/403/404/405/500) — the
  REST-aligned shape, NOT the legacy Tridium `BNaServlet` `{"responses":[…]}`-always-200 envelope. `[CERT]`

## 813.2 — Request threading `[CERT / REMITTANCE]`
Tridium's `BWebServlet.doService()` runs entirely on the JETTY worker thread — no `invokeLater`/`post()`/Transaction
handoff (base + `BBoxServlet`/`BObixServer` alike). Our `BDashboardServlet.java:274` `parent.set(prop, toSet, null)`
writes a component slot ON the Jetty thread. A component READ on the Jetty thread is the ecosystem norm; a
component WRITE crossing into the engine-owned graph without `post()` is a **data race** ([Block 806] §806.5).
chihuahua guards it with a per-ORD `ReentrantLock` (`BChiServlet.java:769`); **DashboardPan does NOT** (G7). `[CERT]`

## 813.3 — Facet enforcement: NOT server-side `[CERT — proven absence]`
`BComponent.set()` does NOT consult `BFacets` min/max — the only facets API is `checkSetFacets(Slot,BFacets,Context)`
(`BComponent.java:662`), which CHANGES facet metadata, not enforces it; grep for `enforceRange|clampToFacets|
checkSet(.*facets` in the `set()` path = zero. Workbench/bajaux clamp on the CLIENT; the server accepts any
type-matching value. Our `BDashboardServlet.java:274` writes a raw coerced double with no min/max check → a client
sending `999999.0` to a `min=10,max=40` setpoint writes it. **Fix:** read `getSlot(prop).getFacets()` and validate
`min ≤ v ≤ max` before `set()`. `[CERT]`

## 813.4 — The CSRF correction + input contract `[CERT]`
- **The REAL Niagara CSRF token is `x-niagara-csrfToken` via `CsrfUtil.verifyCsrfToken`** — `CsrfUtil.java`
  (docSource) reads `req.getHeader(CSRF_TOKEN_HTTP_HEADER)` (`:113`), falls back to the `csrfToken` param, and
  compares to `NiagaraSuperSession.getCsrfToken()`; a `PointOperationServlet` (Honeywell) calls it BEFORE the
  write → `CsrfException`→HTTP 400. Our servlets check only `X-Requested-With: XMLHttpRequest` (a same-origin
  heuristic, not a session-bound token) and the framework `CsrfProtectedFilter` covers only `/rpc/*` ([Block 58]),
  NOT our `/dashboardpan/*` prefix — so our write endpoints rely on the weaker guard (G2, [Block 803] §803.5). `[CERT]`
- **Input contract:** our servlets correctly answer a missing/invalid ORD with HTTP 400 + `{"error":…}` and never
  write until the guards pass (`BDashboardServlet.java:215-229`). BUT `parseDouble()` returns **0.0 silently** on a
  `NumberFormatException` (`:386-390`) — a body `{"value":"abc"}` yields HTTP 200 + a write of 0.0 (G1). The
  contract requires rejecting the unparseable value with 400 BEFORE the write. `[CERT]`

## 813.5 — Cache headers + logging `[CERT]`
- Static assets get `Cache-Control: public, max-age=3600` (`BDashboardServlet.java:428`) with **no URL fingerprint**
  → a module update is invisible to the browser for up to 1 h (stale JS/CSS) (G3). Fix: a version/hash query on
  asset URLs (`main.js?v=<moduleVersion>`) or `no-cache`. `[CERT]`
- `handleCsrfProbe` logs `LOG.info(...)` UNCONDITIONALLY per probe (`BChiServlet.java:613`) — if the SPA polls it,
  that is console INFO spam (G5). Tridium guards per-request INFO with `isLoggable(Level.INFO)` or logs at FINE.
  Write-success INFO (`:279`) is borderline; use FINE or a real audit sink. `[CERT]`

## 813.6 — Kit implication → `types/dashboard.md` servlet section + PR12 lint (lintable-vs-review split) `[CERT-grounded]`
| # | Rule | Kind |
|---|---|---|
| L1 | `parent.set(...)` not preceded by a `BFacets` min/max range check | **LINT** (§813.3) |
| L2 | `Cache-Control: …max-age=N` on an asset URL with no version/hash segment | **LINT** (§813.5) |
| L3 | `LOG.info(...)` inside a request handler not guarded by `isLoggable(Level.INFO)` | **LINT** (§813.5) |
| L5 | `catch(NumberFormatException){return 0.0;}` feeding a `parent.set()` value (silent coercion) | **LINT** (§813.4) |
| L6 | a `doPost` write branch calling `parent.set()` with no prior RBAC helper call | **LINT** (§813.1/B763) |
| L4 | a write path that reaches `parent.set()` without `CsrfUtil.verifyCsrfToken` (or equiv) before body-parse | **REVIEW** (interprocedural, §813.4) |
Servlet section: registration=lifecycle (no module.xml), route()→sealed action, rc/ from classloader+traversal
guard, JSON `{"error"}`+status, and the four gaps as anti-patterns.

## 813.7 — Contract-test shapes (PR12) `[CERT-grounded]`
- **(a) missing/invalid value → 400 AND no write:** `POST /dashboardpan/api/setpoint {"ord":"Cuarto1/setpoint","value":"abc"}`
  → expect HTTP 400, no `BComponent.set()`. **Currently FAILS** (parseDouble→0.0, write, 200).
- **(b) critical write without a fresh step-up token → 401/403:** same POST with a valid session csrf token but no
  step-up → expect 401 (step-up required) / 403 (expired); 200+write only with a fresh step-up token bound to
  (session+user+target+purpose). **Currently:** 200+write on RBAC + X-Requested-With only (no step-up layer, [Block 803] §803.6).

## 813.8 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | BWebServlet self-registers via lifecycle (fw 11/12), servletName-keyed; no module.xml | `[CERT]` | `BWebServlet.java:149-166,52-59`; `BDashboardServlet.java:81` | Y — grep |
| 2 | Real CSRF = x-niagara-csrfToken via CsrfUtil.verifyCsrfToken; ours = X-Requested-With only | `[CERT]` | `CsrfUtil.java:113`; [B803] §803.5 | Y — grep |
| 3 | BComponent.set does NOT enforce facet min/max server-side (proven absence) | `[CERT]` | `BComponent.java:662` + zero clamp | Y — grep |
| 4 | parseDouble silently returns 0.0 → 200+write on bad input (contract-test (a) fails) | `[CERT]` | `BDashboardServlet.java:386-390,274` | Y — grep |
| 5 | max-age=3600 no fingerprint; handleCsrfProbe unconditional INFO | `[CERT]` | `BDashboardServlet.java:428`; `BChiServlet.java:613` | Y — grep |

**Tally:** `[CERT]` ×5. Decompiled cites `extern` — driver token-verified. Write-surface/CSRF-design REMITTANCE to B796/B803/B762/B763.

## 813.9 — Connections & open gaps (the PR12 punch-list)
- REMITTANCE: [Block 796] (write-surface), [Block 803] (step-up/CSRF), [Block 762]/[Block 763] (DWS1), [Block 809]
  (wb), [Block 58]/[Block 29] (CSRF filter), [Block 806]/[Block 808] (threading/health).
- GAPS (per-module fixes): G1 silent parseDouble→0.0; G2 X-Requested-With vs CsrfUtil; G3 max-age no fingerprint;
  G4 no server-side facet clamp; G5 INFO-per-probe; G6 no step-up layer (B803 §803.6 unimplemented); G7 DashboardPan
  lacks the per-ORD `ReentrantLock` chihuahua has (`BChiServlet.java:769`).
- **B813-G1** (requires-execution): the two §813.7 contract tests as off-station @Test (like [Block 796]'s 14) —
  RED now (both fail), GREEN after the PR12 fixes.
