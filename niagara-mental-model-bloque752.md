# B752 · How modules AUTHOR the browser (-ux) layer — the three UI-serving recipes, the two bajaux data-channel dialects, PX authoring, and the RBAC contrast (code-grounded)

> **Scope**: the UX half of the WB/UX census (WB half = B751). How real `-ux` parts put a UI in a browser,
> across Honeywell (EagleHawk HMI, AlarmConsole, TC/Sylk React SPAs, Venom PX) and our own exemplars
> (DashboardPan, chihuahua). Extracts the three serving recipes, the two bajaux data-channel dialects, the PX
> binding model, the live-data channels, and the permission/RBAC contrast — with file:line. Not the ux
> FRAMEWORK internals (B9/B22/B29 = REMITTANCE). The playbook for our modules = B753. Foco: **wb-ux-authoring**
> (WBUX3+WBUX4).
>
> **Sources**: FUENTE 3 — decompiled `-ux` parts + our live DashboardPan-ux/chihuahua-ux on disk. FUENTE 1 —
> B9/B22 (UI stack/bajaux/PX/BajaScript), B29 (web tier/servlets/Jetty), B433 (hx render), B706 (ux best
> practices — thin-shim+JS, requiredPermissions=visibility-not-security, server RBAC), B724 (web-hmi panel).
> Every recipe cites a class/resource + file:line; generalizations [INFER].

---

## 752.1 — Three ways to put a UI in a browser `[CERT]`

| Way | Registration | Serving path | Best for |
|---|---|---|---|
| **Servlet-served SPA** (ours) | `extends BWebServlet` (a `BAbstractService`) — **self-registers** on start via `BWebService.register(this)` keyed by `getServletName()`; no route XML | `/<servletName>/*` → your `doGet/doPost`; you own routing, static-from-`rc/`, JSON APIs | a bespoke dashboard + custom JSON API (DashboardPan, chihuahua) |
| **bajaux `@AgentOn` view** | `@NiagaraType(agent=@AgentOn(types={…}, requiredPermissions=…))` on a `BSingleton implements BIJavaScript` | framework binds the view to those component TYPES; `getJsInfo()` returns a `module://…/rc/X.js` ord the browser loads | a manager/editor/widget view ON a component type (all Honeywell -ux) |
| **PX page** | author a `.px` XML in `module/px/`; reference via `\|view:px:…` or a component's default view | rendered by the PX service (workbench `WbPxMedia`) or to browser via hx (B433) | engineer-authored equipment graphics, no Java (Venom Graphics: 60 .px) |

Evidence: `BWebServlet.java:49,168-173` (self-register); `BDashboardServlet.java:51,81` (our servlet);
`BBacnetAwsDeviceUxManager.java:32,35,40` (bajaux @AgentOn + JsInfo); `VENOM_VAV_003n.px:1-3` (PX).

## 752.2 — The bajaux recipe + its TWO data-channel dialects `[CERT]`
The shim: a `BSingleton implements BIJavaScript` (often `+BIFormFactorMax`, `+BIOffline`) registered
`@AgentOn(types=…)`; `getJsInfo()` returns `JsInfo.make(BOrd.make("module://<mod>/rc/X.js"), <BJsBuild>.TYPE)`.
The JS then reaches the server by ONE of two dialects:
- **`serverSideCall` → `BIServerSideCallHandler`**: `fal.serverSideCall({typeSpec:"…:FALServerSideCallHandler",
  methodName, value})`; server = a second `BSingleton implements BIServerSideCallHandler`
  `@AgentOn(types=…, requiredPermissions="ri")` whose methods return BValue/JSON
  (`BFALServerSideCallHandler.java:29-176`, EagleHawk HMI). Live refresh via a `subscriberMixIn` Fox
  subscription (`getSubscriber().attach('changed', update)`).
- **`baja.rpc` → `@NiagaraRpc B*Rpc`**: `baja.rpc({typeSpec, method, args})`; server = a `BComponent` with
  static `@NiagaraRpc(permissions="…", transports={web,box})` methods (`BThermostatWizardRPC.java:168-176`,
  `BSylkActuatorToolRPC.java:47-55`). Used by the React SPAs (TC, Sylk).

**JS payload spectrum** `[CERT]`: ES5 hand-rolled jQuery/Handlebars (EagleHawk, 10KB) → a `BJsBuild` built
bundle extending the stock console (AlarmConsole, 62KB, references `nmodule/alarm/rc/console`) → a thin
bajaux bootstrap that `ReactDOM.render`s a full React SPA (TC `TCWidget.js:9-56` + a 5.6MB build; Sylk +
d3/pdfmake). `BJsBuild` declares the build module + `rc/*.built.min.js` and its dependencies
(`BWebEditorsJsBuild`, `BHistoryJsBuild`).

## 752.3 — PX authoring `[CERT]`
A `.px` is XML: `<px version="1.0" media="workbench:WbPxMedia">` + `<import><module name="baja|bajaui|gx|kitPx"/>`
(the widget-type modules referenced) + `<content>` = a widget tree (`ScrollPane→TabbedPane→CanvasPane`) with
absolute `layout="x,y,w,h"` and `image="module://kitPxHvac/…gif"` ords. **A widget shows live data by nesting
a `*Binding` child that carries an `ord`**, with a converter child mapping the bound value to a widget
property. Binding taxonomy (counts across 270 .px): `ValueBinding` (generic + hyperlink), `WidgetIdBinding`
(bajaux id hooks), `WbFieldEditorBinding` (editable), `BoundLabelBinding` (read-only), `SetPointBinding`
(write-back on a widget event), `ActionBinding` (invoke an action). Recipes:
- Read onto a label: `<BoundLabel><BoundLabelBinding ord="slot:../Monitor/SpaceTemp" statusEffect="color">
  <ObjectToString name="text" format="%out.value%"/></BoundLabelBinding></BoundLabel>` (`VENOM_VAV_003n.px:32-35`).
- Write a setpoint: `<CheckBox><SetPointBinding ord="slot:SpaceTemp/AlarmEnable" widgetEvent="actionPerformed"
  widgetProperty="selected"/></CheckBox>`.
- Invoke: `<ActionBinding ord="station:|slot:/save" widgetEvent="actionPerformed"/>`.
**Ord/hyperlink scheme**: relative `slot:../Monitor/SpaceTemp`; cross-space chains with `|`
(`station:|slot:/save`); a hyperlink navigates to a VIEW via `…|view:<module>:<ViewName>` (e.g.
`slot:points|view:wiresheet:WireSheet`) — the `|view:` suffix selects which agent view renders the target.

## 752.4 — Live data to the browser, per style `[CERT]`
- **BQL/NEQL from a servlet**: `bql="station:|alarm:|bql:select * where sourceState='offnormal' …"` →
  `BOrd.make(bql).get(this,null)` → `BITable`, iterate `table.cursor()`, serialize JSON
  (`BDashboardServlet.java:485-499`). The canonical "servlet runs a station query and streams JSON".
- **REST-poll** (ours): the SPA `setInterval(poll, 5000)` + a 1s local tick interpolating JACE-computed
  elapsed/remaining between polls (`index.html` poll loop; `DashboardReader.java:222-272`). No Fox.
- **Fox subscription** (live push): the bajaux/PX path — `subscriberMixIn`/`Subscriber.subscribe()` — used by
  manager and PX views implicitly; NOT used by our servlet SPA (by design).
- **oBIX/REST** and **`module://` JS resource** round out the channels.

## 752.5 — The permission/RBAC contrast — the load-bearing finding `[CERT]`
- **Vendor bajaux modules treat `requiredPermissions` as VISIBILITY only and skip real server RBAC**: TC and
  Sylk register agents with NO `requiredPermissions` and expose `@NiagaraRpc(permissions="unrestricted")`
  methods (`BThermostatWizardRPC.java:169`, `BSylkActuatorToolRPC.java:48`). EagleHawk gates the call-handler
  agent at `ri`, AlarmConsole at `r` — visibility, not authorization. This confirms B706's warning.
- **Our servlet exemplars are the ONLY ones enforcing real server RBAC**: `DashboardRbacHelper.checkCanWrite`
  is the FIRST call in every write handler, reads the PERMISSION BIT `BPermissions.OPERATOR_WRITE` (not role
  names), fail-closed (no user → 401, lacks-write → 403) (`DashboardRbacHelper.java:33-105`). chihuahua adds a
  CSRF `X-Requested-With` gate + an audit trail (`ChiRbacHelper`/`ChiAuditHelper`).
- **License gating** (Sylk) is a distinct third gate: `getJsInfo()` returns an error-widget JS payload unless
  `licenseOK()` (`BSylkActuatorWidget.java:27-37`).

## 752.6 — Our reusable servlet-SPA template `[CERT, distilled]`
The DashboardPan/chihuahua pattern, worth copying: (1) one self-registering `BWebServlet`; (2) a PURE
`route()` returning a `RouteAction` (Niagara-free, unit-testable in WSL — `DashboardDispatch.java:108-164` +
its test); (3) static SPA from `rc/` via classloader with a traversal guard + cache headers; (4) a flat JSON
contract keyed by relative ord, value `{v,st}` where the read key == the write ord, with `BStatus` propagated
by an explicit precedence tag (`DashboardReader.java:321-332`); (5) server-side clock math (JACE computes
elapsed/remaining off one `Clock.millis()` snapshot; client interpolates); (6) fail-closed RBAC by permission
bit as the first line of every write + fire-and-forget audit; (7) an XHR-guard on `/api/*` (require
`X-Requested-With` else 302) as light CSRF. **Two documented footguns**: never set a user Home Page to a
servlet path (a raw path is not a resolvable ORD → `SyntaxException: Missing scheme name` → every login fails;
bookmark the URL instead); and `SERVICE_ORD` is hardcoded (update if the service is relocated).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Three serving recipes: servlet-SPA (self-register by name) / bajaux @AgentOn view / PX | [CERT] | BWebServlet.java:49,168-173; BBacnetAwsDeviceUxManager.java:32-40; VENOM_VAV_003n.px:1-3 |
| 2 | bajaux shim = BSingleton+BIJavaScript + JsInfo(module://…/rc/X.js) | [CERT] | BFastAccessListWidget.java:12-24; BConfigurationWizard.java:23-26 |
| 3 | Two data-channel dialects: serverSideCall→BIServerSideCallHandler vs baja.rpc→@NiagaraRpc B*Rpc | [CERT] | BFALServerSideCallHandler.java:29-176; BThermostatWizardRPC.java:168-176 |
| 4 | JS spectrum ES5-handrolled → BJsBuild bundle → thin shim + React SPA | [CERT] | FastAccessListWidget.js; honAlarmConsole.built.min.js; TCWidget.js:9-56 |
| 5 | PX = XML widget tree + nested *Binding(ord)+converter; binding taxonomy; \|view: hyperlink scheme | [CERT] | VENOM_VAV_003n.px:32-35; Smart_IO.px:134 |
| 6 | Live data: BQL-from-servlet, REST-poll, Fox subscription, oBIX, module:// | [CERT] | BDashboardServlet.java:485-499; DashboardReader.java:222-272 |
| 7 | Vendor bajaux = visibility perms + unrestricted RPC (no RBAC); ours = OPERATOR_WRITE bit fail-closed | [CERT] | BThermostatWizardRPC.java:169; DashboardRbacHelper.java:33-105 |
| 8 | License gating (Sylk) switches JS payload on licenseOK() | [CERT] | BSylkActuatorWidget.java:27-37 |

**Tally**: 8 [CERT]. No unmarked claims.

## Connections
- **B9**/**B22** (UI stack/bajaux/PX), **B29** (web tier/servlets), **B433** (hx), **B706** (ux best practices —
  the RBAC prescription this confirms vendors violate), **B724** (web-hmi panel), **B187** (ord schemes in
  bindings). Forward: **B751** (WB half), **B753** (playbook).

## Open gaps
- **B752-G1**: the `BJsBuild` build-dependency graph (how a built bundle pulls webEditors/history JS) mapped
  by declaration, not by tracing the build.
- **B752-G2**: the Fox `subscriberMixIn` client contract (attach/detach lifecycle) — used by bajaux/PX, not by
  our REST SPA; a deepening if we ever adopt live push.
