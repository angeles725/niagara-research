# Block 845 — HARBOR lighting dashboard: HTML+CSS+JS feasibility and options (Px vs web module vs oBIX)

> **Focus:** harbor-greenmax-lighting. **Scope:** the feasibility verdict and the concrete design options for
> building the HARBOR operator lighting dashboard in **HTML + CSS + JS** — as a replacement for, or alongside,
> Niagara Px. Decides *how the operator surface is built*; the control-object contract it binds to is fixed by
> **B842 §6** (`panelScheduleSel`, `circuitScheduleSel`, `hoaMode`, `effectiveState`/`effectiveSchedule`), and the
> command chain it ultimately writes is **B841 §3** (`SchdlGMnn Rk` priority array on the Supervisor). Answers the
> operator/commercial thread "can we give them a modern web dashboard instead of Px, and what does 4.3 cost us?"
>
> **Station constraint `[CERT-live]` (B841/B838):** the field JACE **HM_Central** is a **Distech EC-Net JACE-8000
> on Baja 4.3.58.18**; the Supervisor **HM_BMS** is N4 and hosts the 106 `.px` dashboards. The 4.3 release is
> **pre-N4.13** — decisive for the servlet mechanism (§2 below).
>
> **Sources:**
> - **FUENTE 1 (corpus):** **B9 (UI stack)** §9.2.2 ux profiles, §9.2.3 hx (BHxProfile/BHxView), §9.3.1 Jetty +
>   BWebService, §9.3.2 BWebService/BWebServlet + the N4.0+ `WEB-INF/web.xml` module mechanism, §9.3.3 ServletView,
>   §9.3.5 web auth, §9.3.7 REST — cited per claim `[CERT-doc]`. **B841** (the live HARBOR map) and **B842 §6** (the
>   control contract) `[CERT-live]`/`[INFER]`. **B838** (the 4.3.58.18 platform).
> - **FUENTE 2 (team's real web-dashboard practice, memory):** `dashboardpan-r14-second-login`,
>   `condensadoras-panel-comppan-mapping`, `panccadia-access-model-viewer-writeserver`, plus the repo tools
>   `tools/dashboard-preview.py` and `tools/obix-nav.py`. Marked `[CERT]` (team code/tooling) where it is the evidence.
> - No external doc was fetched beyond the corpus; every framework fact traces to B9 or to the team's shipped code.

---

## 1. Verdict up front `[INFER]`

**Yes — an HTML + CSS + JS operator dashboard is feasible in Niagara N4, and specifically on this 4.3 JACE/Supervisor
pair.** Niagara has served browser HTML/JS for years: the ux/bajaux profiles render HTML5 client-side `[CERT-doc]`
(B9 §9.2.2), the hx framework emits HTML/JS server-side `[CERT-doc]` (B9 §9.2.3), and a custom `BWebServlet` can
return any HTML/JSON/CSS it wants over Jetty `[CERT-doc]` (B9 §9.3.2). The team already ships exactly this shape —
a servlet serving an `rc/` SPA with a JSON API (DashboardPan) `[CERT]` (memory). The only 4.3-specific nuance is
*which registration mechanism* the custom servlet uses (§2). The real question is therefore not "can we" but "which
of four routes, given a 4.3 station and a proposal that was almost certainly priced for Px" (§3–§7).

## 2. The 4.3 servlet constraint — confirmed `[CERT-doc]` + `[INFER]`

B9 §9.3.1 states verbatim: **"`BWebService` (`javax.baja.web`, **desde N4.13**) encapsula config HTTP/HTTPS"**
`[CERT-doc]`. HM_Central/HM_BMS run **4.3.58.18**, i.e. **pre-4.13** `[CERT-live]` (B841/B838) → **`BWebService`
does not exist on this station.**

What *does* exist pre-4.13 `[CERT-doc]` (B9 §9.3.2): the **standard web module mechanism, N4.0+** — a module ships
`src/WEB-INF/web.xml` declaring `<servlet-name>` + `<url-pattern>` (e.g. `/test/*`), reached at
`http://host/moduleName/test/...`; plus the `BWebServlet`/`BServletView` component types registered via the module
itself. **Implication `[INFER]`:** on 4.3 a custom HTML dashboard module is feasible, but it registers its servlet
the **older web-rt / `web.xml` module way (N4.0+)**, **not** through `BWebService`'s service config. This matters
for porting: the team's DashboardPan currently runs on a **4.14** JACE (PANCCADIA, `Niagara4.14` station path)
`[CERT]` (memory) where `BWebService` is present; moving that pattern to HARBOR 4.3 means its servlet registration
and HTTP/TLS wiring must use the pre-4.13 path. The SPA itself (`rc/` HTML/CSS/JS + JSON API) is version-agnostic;
only the servlet plumbing changes. *(The exact pre-4.13 DashboardPan build delta is gap B845-G1.)*

## 3. The four approaches at a glance

| # | Approach | Where it runs | Control/UX | Module? | 4.3 cost |
|---|----------|---------------|------------|---------|----------|
| A | Custom web module: `BWebServlet` subclass serving an `rc/` SPA + JSON API | Supervisor (or JACE) Jetty | highest | yes | servlet must use pre-4.13 `web.xml` reg (§2) |
| B | External/hosted HTML page, reads/writes points via **oBIX** (or REST/BQL) | off-station | high, decoupled | no | oBIX read proven; write via a write-server |
| C | **hx / HTML5 Hx** views (`BHxProfile`/`BHxView`) | Supervisor server-side | medium | light/none | in-framework, dev "parado" (B9) |
| D | Keep **Px** (status quo) | Supervisor, PX Editor | Px-bounded | no | zero; matches the quoted scope |

### A. Custom web module — `BWebServlet` + `rc/` SPA + JSON API `[CERT-doc]`/`[CERT]`
A `BWebServlet` overrides `doGet(WebOp)` and can return HTML, JSON or CSV `[CERT-doc]` (B9 §9.3.2). The team's
**DashboardPan** is exactly this: `BDashboardServlet` + a `DashboardDispatch.route` dispatcher serving `index.html`
+ css + js from the module `rc/`, with `GET/POST /api/...` JSON endpoints and an **XHR guard** (any `/api/*` without
`X-Requested-With: XMLHttpRequest` is bounced) `[CERT]` (memory + `tools/dashboard-preview.py` header). You iterate
the HTML/CSS/JS with **`tools/dashboard-preview.py`** — it serves the real `rc/` and mocks the servlet API, so
layout/palette/bugs surface **before** the gradle build+sign+deploy cycle `[CERT]` (tool docstring; it even caught
a real DashboardPan header regression). **Trade-off:** most control and the richest UX, reusable team tooling and
the `build-n4-module` skill — but it is a signed module deploy to the station, and on 4.3 needs the §2 registration.
**Threading caveat `[CERT-doc]`:** `doGet` runs on a **Jetty worker thread, not the engine thread**; the servlet must
reach components via FOX/BOX/`post()`, never a raw `.get()` (deadlock risk) (B9 §9.3.2/§Gotchas).

### B. External HTML page over oBIX / REST / BQL `[CERT]`/`[INFER]`
A plain HTML/CSS/JS page hosted anywhere reads and writes station points over a protocol instead of a station-served
servlet. **oBIX read is proven on this JACE family** — `tools/obix-nav.py` pulls hundreds of slot values in one oBIX
**Batch** round-trip (HTTP Basic, `api` user, self-signed TLS) `[CERT]` (tool). **oBIX write is also proven in the
team's real stack**, but deliberately *not* from the browser: the PANCCADIA model is **browser → write-server →
one oBIX write-user → station**; the browser never touches the station directly, and there is no who-changed-what
audit at the oBIX layer today `[CERT]` (memory `panccadia-access-model`). *(`obix-nav.py` itself is read-only by
design — it never writes.)* **Trade-off:** fully decoupled from the station UI, no module to sign, survives a station
reflash — but it needs a hosting tier + the write-server, and you own auth/audit yourself (the JACE's `api` user is
a shared service identity, not a per-operator one). REST API exists but is **N4.x modern / post-4.3-era** `[CERT-doc]`
(B9 §9.3.7) — confirm availability on 4.3 before relying on it (gap B845-G2); oBIX is the safer proven channel here.

### C. hx / HTML5 Hx views `[CERT-doc]`
`BHxProfile` generates a full HTML document and `BHxView` (extends `BServletView`) renders HTML snippets with
`write()`/`update()`/`process()`/`save()` lifecycle and async background events (B9 §9.2.3). **Trade-off:** in-framework,
no separate SPA, lighter to stand up — but HTML is generated server-side from Java (not hand-authored CSS/JS), the UX
ceiling is lower, and B9 notes hx **development is "parado" (stopped)**, future is bajaux. Least attractive for a
*modern* dashboard the client can be shown as a premium deliverable.

### D. Keep Px (status quo) `[CERT-live]`
HARBOR already has **106 `.px` files** in `HM_BMS/shared/px` — one per panel (`GreenMAXnn TabALxx.px`), an overview
(`GreenMAX Iluminacion.px`), and a template (`plantilla greenmax.px`) — edited in **PX Editor** `[CERT-live]` (B841
§4). **Trade-off:** fastest, no module, no code, matches the SEJOFA line item verbatim — but Px-bounded look/feel and
PX-Editor-only authoring.

## 4. What the dashboard must bind to (unchanged by the route) `[INFER]`
Whichever route, the surface binds the **B842 §6 control contract**: per **panel** `panelScheduleSel` (H1–H4) and
optional `panelHOA`; per **circuit** `circuitScheduleSel` (FollowPanel/H1–H4), `circuitHOA` (Hand/Off/Auto), and
read-only `effectiveState` + `effectiveSchedule`. Reads project those points; writes set the selector/HOA, which
resolve onto `SchdlGMnn Rk.in10` (selected schedule) and `.in8` (HOA override) on the Supervisor — **B842 §4 / B841
§3** `[CERT-live]` that in10 is the schedule slot and the priority array is otherwise free. The route choice changes
*transport and chrome*, never the contract or the command chain.

## 5. Write-safety — the non-negotiable `[INFER]` (built on `[CERT]`/`[CERT-doc]`)
Any HTML dashboard that **writes** `panelSel`/`circuitSel`/HOA is writing into a **priority array** on a live lighting
plant, so two rules hold regardless of route:

1. **Go through Niagara auth and attribute the write.** The team's shipped answer is the **R14 "second login before
   write"** pattern (B834 custom-login) `[CERT]` (memory `dashboardpan-r14-second-login`): login-1 is the shared
   station web session to *see* the dashboard; **login-2 is an individual Niagara username+password inside the
   dashboard** that unlocks the controls. It re-authenticates against **real local Niagara credentials** via
   `BPasswordCache` (local users confirmed; LDAP/SAML would be denied), holds a **JSESSIONID-keyed server-side
   write-session** (sliding TTL, no self-issued cookie → avoids CSRF/fixation), gates each write on `OPERATOR_WRITE`,
   and — crucially — **passes the authenticated `BUser` as the `set()` Context** so the write is natively
   permission-checked **and** generates an `AuditEvent` (a `set(...,null)` bypasses both). This is the safe write
   surface for route A. For route B, the equivalent is the **write-server + single oBIX write-user** tier `[CERT]`
   (memory `panccadia-access-model`) — but note it currently yields **no per-operator audit**, so R14-style
   attribution would have to be added in the write-server.
2. **Respect the priority array.** Write the **selected schedule to `in10`** and the **HOA override to `in8`**
   (release = null → falls back to the schedule), leaving **`in16` as the untouched fallback** so no circuit ever
   floats `[CERT-live]`+`[INFER]` (B842 §4, B841 §3/§10). One writer per priority slot (B842 §7).

## 6. Recommendation for HARBOR `[INFER]`
**Two-tier recommendation, given (a) the station is 4.3 [§2 servlet constraint], (b) the SEJOFA proposal almost
certainly priced "dashboards" as Px, and (c) writes must honor the priority array + station auth:**

- **Baseline / in-scope: keep Px (route D).** The SEJOFA scope lists "dashboards" against the existing 106-`.px`
  Px estate edited in PX Editor `[CERT-live]` (B841 §4–§5); delivering the redesign's new selector/HOA points on
  refreshed Px pages is the **fastest, lowest-risk, already-funded** path and needs no module on a 4.3 station.
- **Premium upsell: custom web module (route A), reusing DashboardPan tooling.** Offer the modern HTML+CSS+JS
  operator dashboard as a **priced extension** (it is not in the 2-state SEJOFA line, per B841 §5): a `BWebServlet`
  SPA binding the B842 §6 contract, iterated with `tools/dashboard-preview.py`, built via the `build-n4-module`
  skill, with the **R14 second-login write surface** from §5. The one new-work item vs PANCCADIA is the **pre-4.13
  servlet registration** (§2).
- **Route B (oBIX/external)** is the fallback if the client wants the dashboard **off-station** (e.g. a hosted page,
  no signed module on the JACE) — proven read path, but you must stand up the write-server + add attribution.
- **Route C (hx)** is not recommended — stopped framework, lower UX ceiling, no upside over A for a showcase surface.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | HTML/CSS/JS operator dashboard is feasible in N4 (ux HTML5, hx HTML, custom servlet) | [CERT-doc] | B9 §9.2.2, §9.2.3, §9.3.2 |
| 2 | `BWebService` (`javax.baja.web`) exists only **desde N4.13** | [CERT-doc] | B9 §9.3.1 (verbatim) |
| 3 | HM_Central/HM_BMS run 4.3.58.18 = pre-4.13 → no `BWebService` on this station | [CERT-live]+[INFER] | B841/B838 (4.3.58.18); [INFER] the "no BWebService" consequence |
| 4 | Pre-4.13 custom servlet registers via the N4.0+ `src/WEB-INF/web.xml` module mechanism | [CERT-doc] | B9 §9.3.2 (web.xml servlet-name/url-pattern, N4.0+) |
| 5 | Route A = `BWebServlet.doGet(WebOp)` returns HTML/JSON; the team's DashboardPan is this shape | [CERT-doc]+[CERT] | B9 §9.3.2; memory (BDashboardServlet/DashboardDispatch, rc/ SPA, XHR guard) |
| 6 | `dashboard-preview.py` serves real rc/ + mocks the API → iterate before build/sign/deploy | [CERT] | `tools/dashboard-preview.py` docstring |
| 7 | Servlet `doGet` runs on a Jetty worker thread, not engine thread → FOX/BOX/post, no raw .get() | [CERT-doc] | B9 §9.3.2 threading + Gotchas |
| 8 | oBIX **read** proven on this JACE family (Batch, Basic auth, self-signed TLS) | [CERT] | `tools/obix-nav.py` (one Batch pulls hundreds of slots) |
| 9 | oBIX **write** proven in team stack via browser→write-server→single oBIX write-user; no per-op audit today | [CERT] | memory `panccadia-access-model`; `obix-nav.py` is read-only |
| 10 | REST API is N4.x-modern (B9 §9.3.7) — availability on 4.3 not confirmed here | [CERT-doc]+[INFER] | B9 §9.3.7; [INFER] 4.3 caveat |
| 11 | hx = `BHxProfile`/`BHxView`, server-side HTML, development "parado" | [CERT-doc] | B9 §9.2.3 |
| 12 | Px status quo = 106 `.px` in HM_BMS/shared/px, per-panel + overview + template, PX Editor | [CERT-live] | B841 §4 |
| 13 | Surface binds B842 §6 contract; writes resolve to SchdlGMnn Rk in10/in8, in16 fallback untouched | [CERT-live]+[INFER] | B842 §4/§6, B841 §3/§10 |
| 14 | R14 second-login: BPasswordCache re-auth, JSESSIONID write-session, OPERATOR_WRITE, BUser as set() Context → audit | [CERT] | memory `dashboardpan-r14-second-login` (B834/B830) |
| 15 | DashboardPan currently runs on a 4.14 JACE → porting to 4.3 needs the pre-4.13 reg path | [CERT]+[INFER] | memory (Niagara4.14 station path); [INFER] the port consequence |
| 16 | Recommendation: Px for the quoted scope; route A web module as priced premium upsell | [INFER] | engineering judgment over claims 2–15 |

**Tally:** 16 claims — 6 [CERT-doc] (B9 framework facts), 3 [CERT] (team code/tooling/memory), 1 [CERT-live],
4 mixed ([CERT]/[CERT-live]+[INFER]), 2 [INFER]. Every [INFER] is an explicit reasoning or design step labeled as
such; the load-bearing constraint (BWebService since 4.13 → pre-4.13 reg on 4.3) rests on B9 §9.3.1 verbatim +
B841/B838 live version. No unmarked assertions, no invented citations.

## Connections
- **B841** — the live HARBOR map: 106 Px files, the `SchdlGMnn Rk` write targets, and the SEJOFA "dashboards" scope this block prices against.
- **B842 §4/§6** — the control-object contract (`panelScheduleSel`/`circuitScheduleSel`/`hoaMode`/`effective*`) and the in10/in8 write slots any dashboard route binds to.
- **B843 / B844** — the two control-logic build routes (custom module vs kitControl) that produce the points this dashboard exposes; route A here pairs naturally with B843's module.
- **B838** — the 4.3.58.18 platform + backup/restore reversibility before deploying any module to HM_Central.
- **B9 §9.2–§9.3** — the UI-stack substrate: ux/bajaux, hx, Jetty, BWebService/BWebServlet, ServletView, REST, web auth.
- **Memory** — `dashboardpan-r14-second-login` (the write surface), `panccadia-access-model` (oBIX write-server tier), `condensadoras-panel-comppan-mapping` (the DashboardPan servlet+rc/ SPA pattern in practice).

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B845-G1** — The exact pre-4.13 servlet-registration delta for route A: what changes in the DashboardPan build (`web.xml` module descriptor vs its current 4.14 `BWebService`-era wiring, TLS/port config, signing) to run on HM_BMS/HM_Central 4.3.58.18. Confirm by a test-module deploy.
- **B845-G2** — Whether the Niagara **REST API** and/or a writable **oBIX** endpoint are enabled/licensed on this 4.3 Distech EC-Net station (route B), and whether the `api` service-user can be scoped per-operator or must sit behind a write-server for attribution.
- **B845-G3** — Whether the dashboard should be hosted on the **Supervisor (HM_BMS)** — where the schedules and Px already live and writes to `SchdlGMnn Rk` are local — or on the **JACE**; and the client's decision between an on-station signed module (A) vs an off-station hosted page (B).
