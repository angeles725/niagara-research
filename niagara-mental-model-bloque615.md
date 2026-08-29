# Block 615 — graphql-admin (GQL-G6): the native `dashboard-ux` module is a thin JS-widget agent, not a data backend — a GraphQL dashboard module is a sibling, not an extension

> **What**: What the native Tridium `com.tridium.dashboard.ux` module actually is, and whether a
> GraphQL-backed "dashboard module" should embed in it, replace it, or ignore it. Answer: the native
> `dashboard-ux` Java layer is THREE singletons that register a bajaux JavaScript widget for the
> `dashboard:DashboardPane` type plus its JS/CSS build — zero data logic, zero extension seam. A
> GraphQL admin dashboard is a SEPARATE module (the chihuahua [B163] model), coexisting as a sibling.
> **Scope**: the three `dashboard-ux` Java classes. The rt-side `dashboard:DashboardPane` component type and
> the JS bundle behavior are out of scope (the Java layer here only *registers* the JS). The bajaux
> JavaScript-widget pattern is REMITTANCE to [B204]/[B421]; the separate-module precedent to [B163].
> **Block type**: EVIDENCE (small module survey) + a one-line DESIGN verdict.
> **Subject version**: Niagara N4.14.0.162.
> **Sources** (all read in full — the module's entire Java surface is 3 files):
> - `organized/dashboard/dashboard-ux/vineflower/com/tridium/dashboard/ux/BUxDashboardPane.java`
> - `organized/dashboard/dashboard-ux/vineflower/com/tridium/dashboard/ux/BDashboardJsBuild.java`
> - `organized/dashboard/dashboard-ux/vineflower/com/tridium/dashboard/ux/BDashboardCssResource.java`
> **Method**: vineflower, full read (clean, no scrubbing). Markers: `[CERT]` `file:line`; `[INFER]` = verdict.

---

## 615.1 — The whole `dashboard-ux` Java layer: 3 singletons `[CERT]`

- **`BUxDashboardPane extends BSingleton implements BIJavaScriptWidget`**, annotated
  `@NiagaraType(agent=@AgentOn(types={"dashboard:DashboardPane"}))` + `@NiagaraSingleton` `[CERT]`
  (`BUxDashboardPane.java:14-20`). Its only behavior is `getJsInfo(Context) → JsInfo.make(BOrd.make(
  "module://dashboard/rc/ux/DashboardPane.js"), BDashboardJsBuild.TYPE)` `[CERT]` (`:23,32-34`). So it is
  an AGENT that binds the JavaScript widget `DashboardPane.js` to the `dashboard:DashboardPane` component
  type — the Java class contains no rendering, no data access, no servlet.
- **`BDashboardJsBuild extends BJsBuild`** = the JS bundle declaration:
  `super("dashboard", BOrd.make("module://dashboard/rc/dashboard.built.min.js"), {BBajauiJsBuild.TYPE,
  BDashboardCssResource.TYPE})` `[CERT]` (`BDashboardJsBuild.java:13,22`) — names the built/minified JS and
  its dependencies (the bajaux base build + the dashboard CSS).
- **`BDashboardCssResource extends BCssResource`** = one stylesheet:
  `super({BOrd.make("module://dashboard/rc/dashboard.css")})` `[CERT]` (`BDashboardCssResource.java:12,21`).

That is the entire `-ux` Java surface. The functional dashboard (the savable per-user Px-widget pane the
devguide calls "Dashboards") lives in `rc/dashboard.built.min.js` — a JavaScript bundle — with the Java side
doing nothing but registration. This is the SAME thin-Java / all-UI-in-JS pattern documented for `webEditors`
in [B421] (`BSingleton` + `@AgentOn` delegating to JS).

## 615.2 — Implications for a GraphQL dashboard module `[INFER]`

- **No data backend to extend.** The native module offers no servlet, no query API, no resolver seam — it is
  a front-end widget registration. A GraphQL admin layer gains nothing by hooking into it.
- **No coupling required.** `@AgentOn(types={"dashboard:DashboardPane"})` binds ONLY the `DashboardPane`
  type; a custom module's own component types and JS widgets are independent. The two coexist as siblings
  with no conflict.
- **Verdict**: build the GraphQL dashboard as a SEPARATE module — its own `-rt` (the `BWebServlet`/resolver
  from [B611]–[B614]) and its own `-ux` (a bajaux JS front-end, registered exactly like these 3 singletons:
  a `BJsBuild` + `BCssResource` + an `@AgentOn` widget or a `BWebServlet`-served SPA). This is the chihuahua
  [B163] precedent (separate module, separate servlet path `/mx60/`, separate JS), which is the stronger
  model than trying to extend the native `dashboard`. The native module can stay installed and untouched.

## 615.3 — Connections

- **[B163]/[B170]/[B171]** — chihuahua: the separate-dashboard-module precedent (own servlet + own JS
  front-end + RBAC write-gate). The recommended shape for the GraphQL dashboard module's `-ux` half.
- **[B421]** — `webEditors`: the same `BSingleton` + `@AgentOn` → JS thin-Java pattern the native
  `dashboard-ux` follows; confirms this is a Niagara-wide convention, not dashboard-specific.
- **[B204]** — bajaux Widget + rt→web bridge (what `BIJavaScriptWidget`/`JsInfo`/`BJsBuild` plug into).
- **[B216]–[B231]** — Reflow: the full-builder alternative to a hand-built dashboard module.
- **[B611]–[B614]** — the `-rt` GraphQL resolver half this `-ux` half would talk to.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BUxDashboardPane` = BSingleton + `@AgentOn("dashboard:DashboardPane")` + BIJavaScriptWidget | `[CERT]` | BUxDashboardPane.java:14-20 | ✓ read |
| 2 | Its only behavior binds JS `module://dashboard/rc/ux/DashboardPane.js` via `getJsInfo` | `[CERT]` | BUxDashboardPane.java:23,32-34 | ✓ read |
| 3 | `BDashboardJsBuild` declares the built JS bundle + deps (bajaui build + CSS) | `[CERT]` | BDashboardJsBuild.java:13,22 | ✓ read |
| 4 | `BDashboardCssResource` declares one stylesheet | `[CERT]` | BDashboardCssResource.java:12,21 | ✓ read |
| 5 | The native module has no servlet/query/data backend — pure JS-widget registration | `[CERT]` | (all 3 files, full read) | ✓ read |
| 6 | GraphQL dashboard = separate sibling module (chihuahua model), not an extension | `[INFER]` | verdict from #1-#5 + [B163] | ✓ reasoned |

**Tally**: `[CERT]` = 5 · `[INFER]` = 1 · others = 0. **[INFER]/[CERT] ratio** ≈ 0.2. Block type = EVIDENCE
(complete small-module survey — the module's entire Java surface was read). G6 closed.
**Tokens checked**: all 3 files read in full; every `[CERT]` traces to a read line. No scrubbing.
