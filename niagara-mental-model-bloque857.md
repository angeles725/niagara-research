# Block 857 — GreenMAX custom dashboard module: BWebServlet + rc/ SPA cost/feasibility on EC-Net 4.3 (closes B853-G5)

> **Focus:** harbor-greenmax-lighting. **Scope:** whether a custom GreenMAX lighting dashboard module — a
> `BWebServlet` subclass serving an `rc/` SPA, the DashboardPan pattern — is a feasible and cost-justified
> option when the client wants enum/selector writes WITHOUT the Hx `/set` dialog. Deepens **B845 §3-A** into
> a concrete build-cost and wire-path estimate for the lighting case, corrects B845 §2's [INFER] on the
> pre-4.13 servlet-registration mechanism, and closes **B853-G5**.
>
> **Evidence (priority order, METHODOLOGY §6):**
> - **Team source `[CERT]`** — `Cliente/Leon-Guanjuato/Dashboard/DashboardPan/DashboardPan-ux/src/…/BDashboardServlet.java`
>   (servlet class), `DashboardRbacHelper.java` (write gate), `module-include.xml` (type declaration);
>   `DashboardPan-rt/…/BDashboardService.java` (station service).
> - **Corpus blocks `[CERT-doc]`** — **B9 §9.3.2** (BWebServlet + module mechanism), **B29 §29.2**
>   (BComponent-tree dynamic registry), **B803** (step-up / re-auth servlet pattern).
> - **Evidence from prior blocks (remittance)** — **B845** (4.3 servlet constraint, four-route comparison,
>   R14 write surface), **B853** (Hx write limits: `/set` dialog is the only numeric path; enum/boolean
>   writes ARE no-arg in Hx), **B855 §3** (upgrade recompile plan and cost vs kitControl), **B856** (Hx
>   engine specifics for this client). Cited as `[CERT-live]`/`[INFER]` per the original block's marker.
> - **Sources:** three queries to `niagara_help.py` — `guide-search "web servlet"` zero results,
>   `find "oBIX"` zero results, `find "BWebServlet"` zero results — recorded as data; all framework
>   claims verified from B9/B29/B803 and the team module source.

---

## 1. Verdict up front `[INFER]`

**Feasible, but priced higher than the Px+dialog baseline.** The GreenMAX dashboard module follows exactly
the same architecture as the team's deployed DashboardPan: a `BWebServlet` subclass registered via the
BComponent tree (`serviceStarted()` → dynamic Jetty mount), serving `rc/` static assets plus a minimal
JSON write API, guarded by standard Niagara session auth + `OPERATOR_WRITE`. The write path for lighting is
simpler than for cold rooms (enum ordinal or `BooleanWritable` action, not numeric setpoints). The one
concrete cost difference vs DashboardPan is that this module has never been compiled against the 4.3 SDK
— the build-sign-deploy cycle applies once and the upgrade recompile risk is the same as Route A
(B855 §3).

**Compared to the Px + `/set` dialog baseline (quoted scope):**
- **Baseline (free):** stock Px with `ActionBinding SelR{k}/set` → Hx opens the Set dialog → operator
  types a number. Confirmed working [CERT-live] (B853 §5-§6). No module, no build cost.
- **Module option (upsell):** a custom HTML page with dropdowns and labeled buttons. Cleaner UX,
  no dialog, but requires build + sign + deploy + one recompile per major N4 upgrade.

**Recommendation:** if the client (a) explicitly wants a richer browser UX and (b) is willing to pay
for a signed module deploy, the module option is justified. If the primary goal is "get rid of the Set
dialog", a simpler and cost-free alternative is B853-G2: replace `NumericWritable SelR{k}` with an
`EnumWritable` whose range labels match the schedule names — then the Hx property-sheet dropdown writes
directly with no dialog. That alternative should be priced first.

---

## 2. Architecture — how BWebServlet registers on 4.3 `[CERT]`/`[CERT-doc]` + B845 §2 correction

### 2.1 The registration mechanism: BComponent tree, NOT `web.xml` `[CERT-doc]` + `[CERT]`

**B845 §2 contained an [INFER] that needs correcting.** It stated: "on 4.3 a custom HTML dashboard
module…registers its servlet the older web-rt / web.xml module way (N4.0+)". B29 §29.2 refutes this
for `BWebServlet` subclasses `[CERT-doc]`:

> "Niagara **no usa** anotaciones `@WebServlet` ni descriptor XML — **la registry es dinámica en runtime
> via BComponent tree**."
>
> "un `BWebServlet` se monta bajo un `BComponent` vivo y su `serviceStarted()` corre → llama `register(this)`
> en `BWebServer` (la base abstract) → `BJettyWebServer.doRegister()` construye un `ServletContextHandler` +
> `ServletHolder` + lo agrega al `ContextHandlerCollection` en runtime."

The `web.xml` path described in B9 §9.3.2 applies to the **"Niagara Web Archive Modules"** pattern —
standard `javax.servlet.http.HttpServlet` subclasses declared in a module's `src/WEB-INF/web.xml`. That
is a separate, additional registration path added in N4.0; it is NOT the path a `BWebServlet` subclass
takes. A `BWebServlet` subclass registers itself dynamically when the BComponent is started. The team's
`BDashboardServlet` confirms this: it declares its type in `module-include.xml` (so Niagara knows the
class), overrides `getServletName()` → `"dashboardpan"`, and relies on the parent class's
`serviceStarted()` for the dynamic Jetty mount. No `web.xml` in the module `[CERT]`
(`DashboardPan-ux/module-include.xml`, `BDashboardServlet.java:80-84`).

**Implication for 4.3:** the `BWebServlet` class itself is N4.0+ `[CERT-doc]` (B9 §9.3.2). The dynamic
BComponent-tree registration mechanism is therefore available on the client's EC-Net 4.3.58.18 station
with the SAME call path. The `BWebService` (N4.13+ per B845 §2 `[CERT-doc]`) governs HTTP/TLS server
CONFIG (ports, certificates); it does NOT control whether a `BWebServlet` subclass can register. A GreenMAX
dashboard servlet placed under the station's Services node would mount at
`https://<station>/greenmax-dashboard/*` on 4.3 via the same mechanism. `[INFER]` (grounded in B29's
[CERT-doc] description of the registration path and the fact that BWebServlet is N4.0+.)

**Remaining pre-4.13 delta (B845-G1, still open):** the HTTP/TLS configuration (port, cert, self-signed
TLS registration, HTTPS enablement) is managed by the pre-4.13 web service rather than `BWebService`.
This is the concrete unknown before a test deploy; the servlet URL-mount mechanism itself is NOT the delta.

### 2.2 Module structure

A GreenMAX dashboard module follows the DashboardPan pattern `[CERT]`:

```
greenmax-dashboard-ux/
  src/
    META-INF/module.xml           (type declarations + module name)
    module-include.xml            (declares BGreenMaxDashboardServlet type)
    com/…/BGreenMaxDashboardServlet.java  (extends BWebServlet)
    com/…/DashboardGmRead.java    (read JSON: panel state, circuit selectors, LED status)
    com/…/DashboardGmWrite.java   (write: SelR{k} enum ordinal, Hor{n}_Eff action)
    rc/
      index.html                  (the SPA shell)
      app.js                      (panel/circuit UI, dropdown + button handlers)
      styles.css
```

The `BGreenMaxDashboardServlet.getServletName()` returns e.g. `"greenmax"` → all traffic to
`/greenmax/*` routes to it. `doGet` serves `rc/` static assets or `GET /api/status`. `doPost` handles
`POST /api/write`.

The SPA iterates at most 13 panels × ~30 circuits = ~390 data points in one `GET /api/status`
response. Given that the DashboardPan reads ~40 points from a cold room, a GreenMAX read handler
is larger but structurally identical. `[INFER]`

---

## 3. Write path: standard Niagara session auth + OPERATOR_WRITE `[CERT]`/`[CERT-doc]`

### 3.1 What the DashboardPan actually uses `[CERT]`

`DashboardRbacHelper.checkCanWrite` (`DashboardRbacHelper.java:33-65`) is the full write gate:
1. `req.getRemoteUser()` — the standard Niagara-session user. Returns null if unauthenticated → 401.
2. `BUserService.getUser(username)` → `user.getPermissions(context).has(BPermissions.OPERATOR_WRITE)`
   → 403 on fail; proceeds on pass.

No `BPasswordCache` re-auth; no second-login credential modal. The "R14 second login" in B845 §5 and
in memory (`dashboardpan-r14-second-login`) refers to the **audit** design requirement that produced
this feature, not a BPasswordCache call in the shipped DashboardPan code. The shipped code gates writes
on the already-authenticated Niagara session. `[CERT]` (`DashboardRbacHelper.java:33-107`)

**Security posture:** the operator logs into the station web UI once (standard Niagara HTTPS login →
JSESSIONID cookie). That session is the write credential. The servlet checks `OPERATOR_WRITE` on every
POST. The write is attributed to `req.getRemoteUser()` and logged via `svc.appendAudit(...)`.

**For a BPasswordCache step-up** (operator-types-password-before-write, a "second login" in the
stronger sense), see B803 §3 — the clean server-side re-auth path exists, but DashboardPan does NOT
implement it today. Adding it to a GreenMAX module would be possible but is extra scope beyond the
baseline write gate. `[CERT-doc]` (B803)

### 3.2 Applying this to GreenMAX writes `[INFER]` (grounded on B853 and [CERT] above)

The GreenMAX lighting writes are simpler than DashboardPan setpoints:

| Action | DashboardPan (cold room) | GreenMAX dashboard |
|--------|--------------------------|--------------------|
| Write type | Numeric setpoint (`BDouble`/`BStatusNumeric`) | Enum ordinal (int 1–5) for `SelR{k}`, or Boolean action (`active`/`inactive`/`auto`) for `Hor{n}_Eff` |
| Coerce logic | `coerceValue()` parses float string, handles `BEnum` range lookup | Same `BEnum` path for `SelR{k}`; for `Hor{n}_Eff` invoke `active`/`inactive`/`auto` via `component.invoke(action, null)` |
| Priority array | Writes to facade config slots only | Writes to `SelR{k}` (BBooleanSelect selector) and `Hor{n}_Eff` (BooleanWritable) — no priority-array writes needed from the SPA; the station-side kitControl chain propagates to `HorR{k}.in10` |
| Write gate | `DashboardRbacHelper.checkCanWrite` | Same pattern |

The priority array (B845 §5, B842 §4) is respected automatically: the SPA writes only to the
`SelR{k}` selector and the `Hor{n}_Eff` HOA writable; the existing kitControl chain translates those to
`HorR{k}.in10` and the station's priority array stays in the kitControl layer. The SPA does not
directly write to `in8`/`in10` slots. `[INFER]` (design choice; simpler and cleaner than a direct
priority-array write from the SPA)

**CSRF:** the servlet should verify the Niagara CSRF token (`x-niagara-csrfToken` header, `CsrfUtil`)
on every POST, as B803 §5 established. `[CERT-doc]` (B803 §5)

---

## 4. What is different for GreenMAX vs DashboardPan (cold room) `[INFER]`

| Dimension | DashboardPan (cold room) | GreenMAX lighting module |
|-----------|--------------------------|--------------------------|
| Write targets | Config setpoints on `BDashboardService` facade (setpoints, temperatures, times) | `SelR{k}` (enum) + `Hor{n}_Eff` actions |
| Write type | Numeric (double) or enum ordinal | Enum ordinal + no-arg boolean action only |
| Data model | ~40 rooms × ~10 slots = ~400 items | 13 panels × ~30 circuits × ~4 fields = ~1,560 items — still a single JSON response |
| Component resolution | Via `DashboardReader.SERVICE_ORD` → `BDashboardService` | Via known ords under `station:|slot:/Iluminacion/…` |
| Coerce complexity | Full float/bool/enum/string coerce | Enum ordinal (int 1–5) + boolean action — subset of the DashboardPan coerce |
| Custom `BComponent` dependency | Reads `BDashboardService` facade (rt module required) | Reads kitControl `BBooleanSelect`, `BooleanWritable`, `HorR{k}` — all stock types (no rt dependency) |
| Module split | `-rt` (facade) + `-ux` (servlet) | `-ux` (servlet) ONLY — no custom rt types needed |

The **single-module** nature is a significant simplification: the GreenMAX dashboard needs only a `-ux`
module with the servlet and `rc/` SPA. There are no custom `BComponent` types that need a `-rt`. This
reduces the build, sign, and deploy surface relative to DashboardPan. `[INFER]`

---

## 5. Cost and feasibility verdict vs the two cheaper routes `[INFER]`

### 5.1 Route comparison

| Route | Effort | Write UX | Module? | 4.3 risk | Upgrade cost |
|-------|--------|----------|---------|----------|--------------|
| **Px + `/set` dialog (quoted scope)** | Zero (already working B853) | Operator types a number in a dialog | No | None | None |
| **B853-G2: EnumWritable + Hx dropdown** | Low (Workbench config change: replace `NumericWritable SelR{k}` → `EnumWritable` with horario-name range) | Hx renders a labeled dropdown, no dialog | No | Confirmed that Hx renders enum dropdowns via property-sheet `[CERT-live]` (B853 §5) | None |
| **Custom dashboard module (this block)** | Medium-high: one `-ux` module, `rc/` SPA, write API, gradle build, Tridium signing, deploy to live station | Buttons + labeled dropdown in a custom-designed HTML page | Yes | BWebServlet N4.0+ dynamic registration works on 4.3 `[INFER]` (§2); pre-4.13 TLS config unknown (B845-G1) | One recompile per major N4 upgrade (B855 §3) `[CERT(process)]` |

### 5.2 Build/sign/deploy cost for the `-ux` module `[CERT(process)]`

From B855 §3 and the team's `build-n4-module` SKILL:
1. **SDK target:** build against the 4.3 SDK (`niagara_home` pointing at the client station's Niagara
   installation path, or a compatible 4.3 SDK). Dependency pins in `module.xml` set to `4.3`.
2. **Gradle build + Slot-o-Matic:** since there are no custom types (servlet only), Slot-o-Matic runs on
   `BGreenMaxDashboardServlet` only (straightforward; no `@NiagaraProperty` enum ranges to worry about).
3. **Tridium module signing:** requires a vendor certificate; same path as DashboardPan. The `build-n4-module`
   SKILL covers the signing workflow.
4. **Deploy:** copy the signed `.jar` to the station's `modules/` directory (or via Workbench Platform Install)
   and restart. The operator then adds a `BGreenMaxDashboardServlet` instance to the station's Services.
5. **Per major N4 upgrade:** bump `defaultModuleVersion` in `GROUP build.gradle.kts`, rebuild, re-sign,
   redeploy (B855 §3). Route B (kitControl) incurs zero recompile cost by comparison.

**One-time estimate [INFER]:** a skilled developer familiar with the DashboardPan codebase would need
approximately 2–4 days to build the GreenMAX variant (adapt the read handler for the 13-panel structure,
adapt the write handler for enum+boolean writes, author the SPA HTML/JS, write the JSON API, QA against the
4.3 station). The sign/deploy/station-config step adds a half-day. This is separate from the Px route cost
(which is essentially zero since the Px+dialog is already working).

### 5.3 The EnumWritable alternative (B853-G2) as the middle path `[INFER]`

Before quoting the full module upsell, evaluate whether B853-G2 satisfies the client:
- Replace each `NumericWritable SelR{k}` with an `EnumWritable` whose range tag-labels are the 5 horario names.
- In Hx, a writable enum renders as a property-sheet dropdown — labeled, one-click, no dialog. `[CERT-live]`
  (B853 §5: "Property-sheet / MultiSheet writes and right-click-edit of writables also work in the browser").
- No module required; Workbench-only config change.
- The caveat is that the range labels need to be populated with the actual horario names (static or
  dynamic — the dynamic case is gap B853-G2). If names are static (the client approves 5 fixed names),
  this can be done today.

If the EnumWritable dropdown is "good enough" for the operator, the custom module is unnecessary and
should not be quoted.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | BWebServlet registration is dynamic via BComponent tree (`serviceStarted() → register()`) — no web.xml for BWebServlet subclasses | [CERT-doc] | B29 §29.2 verbatim |
| 2 | B845 §2's claim that pre-4.13 registration uses "web.xml module way" is an [INFER] error for BWebServlet subclasses; web.xml applies to the separate Niagara Web Archive Module (plain HttpServlet) path | [CERT-doc]+[INFER] | B29 §29.2; B9 §9.3.2 (two separate paths described) |
| 3 | BWebServlet class is available N4.0+ | [CERT-doc] | B9 §9.3.2 |
| 4 | Therefore BWebServlet dynamic-registration works on EC-Net 4.3.58.18; pre-4.13 delta = HTTP/TLS server config, not servlet registration | [INFER] | B29 §29.2 [CERT-doc] + B845 §2 [CERT-live] version |
| 5 | DashboardPan write gate = `req.getRemoteUser()` + `BPermissions.OPERATOR_WRITE` (no BPasswordCache modal) | [CERT] | `DashboardRbacHelper.java:33-65` |
| 6 | DashboardPan servlet: `BDashboardServlet extends BWebServlet`, `getServletName()="dashboardpan"`, no web.xml in module | [CERT] | `BDashboardServlet.java:51,80-84`; `module-include.xml:1-4` |
| 7 | DashboardPan static assets served from `rc/` via `getClass().getClassLoader().getResourceAsStream(RESOURCE_BASE + path)` | [CERT] | `BDashboardServlet.java:408` |
| 8 | GreenMAX lighting writes are enum ordinals (SelR{k}) and no-arg boolean actions (Hor{n}_Eff) — simpler than DashboardPan numeric setpoints | [INFER] | B853 §6-§7 control shape; BDashboardServlet.java:366-373 (BEnum coerce path already handles this) |
| 9 | GreenMAX module is `-ux` only (no custom rt types); kitControl types (BBooleanSelect, BooleanWritable) are stock | [INFER] | B851/B853 control chain uses only stock kitControl |
| 10 | EnumWritable SelR{k} + Hx dropdown = a middle path (no module) — Hx renders writable enums via property-sheet `[CERT-live]` | [CERT-live]+[INFER] | B853 §5; B853-G2 |
| 11 | BPasswordCache step-up (stronger "second login") is architecturally available via B803 §3 but is NOT in the shipped DashboardPan write gate | [CERT]+[CERT-doc] | DashboardRbacHelper.java; B803 §3 |
| 12 | Upgrade recompile: bump defaultModuleVersion, rebuild, re-sign, redeploy — same as any Route A module (B855 §3) | [CERT(process)] | build-n4-module SKILL; B855 §3 |
| 13 | Build estimate ~2–4 dev-days to adapt DashboardPan to GreenMAX; sign/deploy ~0.5 day | [INFER] | engineering judgment over claims 5–12 |

**Tally:** 13 claims — 2 [CERT-doc] (B29, B9), 4 [CERT] (team code), 1 [CERT(process)] (SKILL), 1 [CERT-live]+[INFER],
2 [INFER] grounded on [CERT-live], 3 [INFER]. Every [INFER] is an explicit reasoning step; the load-bearing
correction to B845 §2 rests on B29 §29.2 [CERT-doc] + DashboardPan source [CERT].

---

## Connections

- **B845** — anchor block: four routes (A–D), the pre-4.13 servlet constraint (§2), the R14 write surface (§5),
  and the recommendation that the module is a priced upsell. §2 is corrected here: the registration delta is
  HTTP/TLS config, not web.xml. §5's R14 write surface matches the `DashboardRbacHelper` pattern confirmed above.
- **B853** — Hx write limits: §4 confirms why the module option is requested (numeric writes fail in Hx for stock
  Px); §5 shows the EnumWritable dropdown as the intermediate option. §7 gives the GreenMAX write shapes.
- **B855 §3** — upgrade recompile cost: the same `defaultModuleVersion` + rebuild + re-sign path applies here.
  Route A (custom module) vs Route B (kitControl) comparison directly applies.
- **B29 §29.2** — BComponent-tree dynamic registration (the definitive source for the BWebServlet mechanism,
  correcting the B845 §2 web.xml inference).
- **B803** — the step-up / BPasswordCache re-auth path, available for a stronger write gate on any critical write
  (not in the shipped DashboardPan baseline but architecturally documented for future use).
- **DashboardPan modules** — `BDashboardServlet.java`, `DashboardRbacHelper.java`, `module-include.xml` are the
  direct template for any GreenMAX dashboard module.

## Open gaps

- **B845-G1** (still open) — the pre-4.13 HTTP/TLS server configuration delta for a test-deploy on HM_BMS 4.3:
  confirm that the pre-`BWebService` TLS wiring supports the servlet's HTTPS path. The servlet-registration
  mechanism itself is now confirmed as NOT the delta (§2 above); only the HTTPS server config is still open.
- **B853-G2** (still open) — `EnumWritable SelR{k}` with horario-name range + Hx dropdown: evaluate whether
  this no-module path satisfies the operator before quoting the full custom module.
