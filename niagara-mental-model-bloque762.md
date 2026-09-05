# B762 · Off-station testing of the `-ux`/`-wb` web tier — the pure-seam taxonomy (`route()`→RouteAction, `wb/model/` lambda-injection, the JS residue) and the WSL-vs-station boundary

> **Scope**: how to unit-test a module's `-ux` (servlet/SPA) and `-wb` (Workbench) layers OFF-station (WSL, no
> running station), as a reusable BUILD-KIT authoring pattern. Generalizes the single `DashboardDispatch` fact
> that [B741] §741.2 records (14 tests, "servlet routing/RBAC/JSON") into the seam taxonomy the kit needs, and
> adds the two things B741 does not cover: the `-wb` off-station path and the SPA-JS testability status.
> Focus: `module-ux-testing-and-write-surface` (UXT1).
>
> **Sources**: FUENTE 3 our real modules (read+grep-verified this session): DashboardPan-ux under
> `Cliente/Leon-Guanjuato/Dashboard/DashboardPan/`, chihuahua-ux/-wb under
> `Cliente/Honeywell/MX60/chihuahua/chihuahua/`. FUENTE 1: [B741] (QA plan / assurance stack), [B743] (control-
> logic layered testing), [B12]/[bloque TI] (`niagaraTest` 0-discovery in WSL), [B170] (chihuahua ES5-strict
> IIFE frontend), [B752] (`-ux` RBAC contrast). READ-ONLY. English (post-B115).

---

## 762.1 — The seam principle: a thin Baja adapter over a pure core `[CERT]`
Niagara's web entry points are Baja-bound and un-mockable: `WebOp` is a `final class`, and a `BWidget`/`BWebServlet`
carries the framework. So the tested unit CANNOT be the servlet/widget itself — it must be a **plain-Java core the
Baja shell delegates to**. Both our `-ux` modules encode exactly this, in the same words:
> *"This package-private final class is the ONLY place where routing decisions are made … Because [WebOp] is a
> final class … keep all routing logic in this class and accept plain-Java inputs."* — `ChiServletDispatch.java:8-15`,
> mirrored verbatim in `DashboardDispatch.java:11-13`.
This is the template-method split [B730 §730.7 / B741 §741.2] applied to the web tier: **adapter (Baja) → pure
core (plain Java) → unit test**. The core takes only plain inputs, so it runs under `javac -source 8` + cached
`junit-4.13.2.jar` with no station (the [B741] runnable stack).

## 762.2 — `-ux` servlet: the `route()` → `RouteAction` seam `[CERT]`
The testable seam is a pure routing function returning a sealed action hierarchy the adapter dispatches:
- `DashboardDispatch` — `final class` (`DashboardDispatch.java:30`), "the ONLY place routing decisions are made"
  (:11), returns a nested `abstract static class RouteAction` hierarchy (:38-43) the servlet `instanceof`-checks.
  Inputs are plain (method, path, `Function` header/param lookups) → zero Niagara dependency.
- Proven testable: `DashboardDispatchTest` = **14 `@Test`**, plain `org.junit` + `HashMap::get` as the lookup, zero
  Baja imports.
- Parent exemplar: `ChiServletDispatch.route(...)` — same pattern, ~30 `RouteAction` subtypes (histories, alarms,
  thresholds, CSRF probe); tested by `ChiServletDispatchTest` (11 `@Test`) + `BChiServletTest` (also pure JUnit
  despite the `BChi` name).

## 762.3 — `-ux` purity gradient: not every companion is equally testable `[CERT]`
Within the same `-ux` module the pure-core discipline is applied UNEVENLY — this gradient IS the authoring lesson:
- **Fully pure** → `JsonUtil` (0 baja imports; escape/format/extract static methods). Freely unit-tested.
- **Partially pure** → `DashboardRbacHelper.checkCanWrite(...)` takes `HttpServletRequest/Response` (servlet API,
  mockable) but `resolveOperatorWrite` reaches into Baja (`DashboardRbacHelper.java:33,78`).
- **Least pure** → `DashboardReader.buildEquipmentResponse(BComponent context)` (`DashboardReader.java:143`,
  imports `javax.baja.sys.BComponent`:12) — the data-shaper still takes a live component and walks ORDs, so it is
  NOT off-station testable as written. The fix template is §762.4's lambda-injection: inject the Baja touch as a
  `Function`, keep the shaping pure.

## 762.4 — `-wb`: the `wb/model/` lambda-injection seam (the key discovery) `[CERT]`
B741 covers no `-wb` testing; the corpus had no `-wb` unit-test path. chihuahua-wb proves one exists off-station:
- `chihuahua-wb/src/.../wb/model/` holds 6 Baja-free util classes (`LinkSlotNameUtil`, `PendingLinkBuilder`,
  `PendingLink`, `DirectionLabelUtil`, `DirectionButtonUtil`, `SearchResultUtil`) with **33 `@Test`** total, all
  plain JUnit.
- The seam is **Baja injected as a lambda, not a type**: `LinkSlotNameUtil.generate(Predicate<String> slotExists,
  String base)` (`LinkSlotNameUtil.java:30`; `import java.util.function.Predicate`:3), with the docstring stating
  *"No baja types in this class — the caller supplies a Predicate that wraps the baja slot-existence check
  (`name -> component.getSlot(name) != null`)."* (:9). Production wires the Baja-backed predicate; the test wires a
  plain `Predicate`.
- The Workbench view `BBatchLinkEditor` is the thin adapter holding all the Baja/bajaui imports (~32). So the `-wb`
  pattern is IDENTICAL to `-ux`: a pure `model/` package + a Baja-heavy `BWidget` shell.

## 762.5 — SPA JS: syntax-only today; the pure functions exist but aren't exported `[CERT]`
- The only off-station JS gate today is **`node --check` = ES5 SYNTAX validation, not behavior** (PORT-SPEC /
  mx60 proposal use it to validate edits).
- The pure functions EXIST but lack a node seam: `Router.js` has `parseHash(hash)` (:30) and `buildHash(page,
  params,query)` (:68), exposed only on `window.MX60` (:162-168) inside an IIFE — there is **no
  `module.exports`/`typeof module` seam** (grep: none). So they cannot be `require`d by a node test.
- DashboardPan's SPA is worse: a single ~2300-line `rc/index.html` with one inline `<script>` — no extractable JS
  files at all. No `package.json`, no `*.test.js`, no jsdom anywhere under the module tree.

## 762.6 — The WSL-vs-station boundary `[CERT]`
The line that decides where a test can run:
- **WSL-runnable (pure)**: `route()`/`RouteAction`, `wb/model/` utils, `JsonUtil` — plain inputs, plain JUnit.
- **Station-only (Windows + running station + dev license)**: anything through `HttpURLConnection` or a live
  `BStation`/`WebOp`, and `-wb` view PAINT (gx). The explicit counterexample: `BChiServletIntegrationTest` —
  *"These tests require a running Niagara station … Station HTTPS accessible at STATION_BASE_URL"* (:18-23),
  hits the station over `HttpURLConnection` (:8). `niagaraTest` (plugin 7.6.17) discovers 0 tests in WSL anyway
  [bloque TI], so even the pure suites run only via manual `javac`+junit ("tests-are-docs") — off the build gate.

## 762.7 — Build-kit implications (proposed deltas — for the §18 retro)
- **`types/dashboard.md`**: add the `route()`→`RouteAction` seam as the `-ux` servlet authoring pattern (extract
  routing into a package-private `final` class taking `Function` lookups, return a sealed `RouteAction`, servlet
  is an `instanceof` adapter over the `final` WebOp; test with plain JUnit + `HashMap`). Add the purity gradient:
  keep shapers pure by injecting the Baja touch as a `Function`, per §762.3/762.4 — `DashboardReader(BComponent)`
  is the anti-pattern to refactor.
- **`types/wb-widgets.md`** (rung-0 seed today): add the `wb/model/` lambda-injection pattern — a Baja-free
  `model/` package with the slot check injected as a `Predicate<String>`; the `BWidget` view stays the adapter.
  Upgrades the file from "seed" to an exemplar-backed testable `-wb` pattern.
- **JS residue note**: for a testable SPA, split inline JS into files and add a dual-export seam
  (`if (typeof module!=='undefined') module.exports = …`) + a node harness; DashboardPan's monolithic 2300-line
  `index.html` is the anti-pattern; chihuahua's file-split IIFE is halfway (needs the export shim).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The tested unit is a plain-Java core the Baja shell delegates to, because WebOp is a `final class` | [CERT] | ChiServletDispatch.java:8-15; DashboardDispatch.java:11-13 |
| 2 | `-ux` seam = `route()`→`RouteAction` sealed hierarchy; `DashboardDispatch` is a package-private `final class` | [CERT] | DashboardDispatch.java:30,11,38-43 |
| 3 | `DashboardDispatchTest` = 14 pure `@Test` (zero Baja imports) | [CERT] | DashboardDispatchTest.java (grep -c @Test = 14) |
| 4 | `-ux` purity gradient: JsonUtil pure (0 baja imports) → RbacHelper partial → DashboardReader takes `BComponent` | [CERT] | JsonUtil.java (0 `^import.*baja`); DashboardRbacHelper.java:33,78; DashboardReader.java:12,143 |
| 5 | `-wb` off-station path exists: `wb/model/` 6 Baja-free classes, 33 `@Test`, Baja injected as `Predicate` | [CERT] | LinkSlotNameUtil.java:3,9,30; wb/model srcTest (sum @Test = 33) |
| 6 | SPA JS off-station is syntax-only (`node --check`); `parseHash`/`buildHash` pure but no `module.exports` seam | [CERT] | Router.js:30,68,162-168; grep no `module.exports`/`typeof module` |
| 7 | Boundary: `HttpURLConnection`/live `BStation` = station-only; `BChiServletIntegrationTest` needs a running station | [CERT] | BChiServletIntegrationTest.java:8,18-23 |
| 8 | DashboardPan has no `-wb` (0 `.java`); the `-wb` pattern is proven only by chihuahua-wb | [CERT] | find DashboardPan-wb `.java` = 0 |

**Tally**: 8 [CERT], 0 [INFER]. No unmarked claims. All file:line grep-verified this session at the real client-repo paths (not the abbreviated paths a sweep first reported).

## Connections
- **[B741]** — the QA plan that records `DashboardDispatch` (14 tests) + the template-method split + the 4-layer
  assurance stack; B762 generalizes its one `-ux` fact into the web-tier seam taxonomy and adds `-wb` + JS.
- **[B743]** — the CONTROL-logic layered answer (math seam / scheduler seam / live smoke); B762 is its web-tier
  sibling (the same "pure core below a Baja shell" idea, applied to servlet/model instead of timer).
- **[B730] §730.7** (template-method pure split), **[bloque TI]/[B12]** (`niagaraTest` 0-discovery in WSL),
  **[B170]** (chihuahua ES5-strict IIFE), **[B752]** (`-ux` OPERATOR_WRITE RBAC contrast — the write-surface UXT2
  playbook will wire this in), **[B58]/[B74]** (servlet CSRF / X-Requested-With guard).

## Open gaps
- **UXT1-G1 — JS behavioral testing has no path.** `node --check` proves syntax only; `parseHash`/`buildHash` are
  pure and ready but lack a `module.exports` seam + a node harness. Largest uncovered `-ux` layer. (Implementation
  task, not research: add the export shim + a `node --test` harness.)
- **UXT1-G2 — DashboardPan SPA is a monolithic 2300-line inline `index.html`**; no function is extractable without
  a file split (chihuahua already split; DashboardPan did not).
- **UXT1-G3 — `DashboardReader` not yet pure** (takes `BComponent`); the `wb/model/` `Predicate`-injection pattern
  is the fix template, un-applied to the `-ux` reader.
- **UXT1-G4 — `-wb` view PAINT (gx) remains station/Workbench-only**; the `model/` split tests logic, never the
  rendered widget. (Feeds UXT3.)
- Next in this focus: **UXT2** — the `-ux` write-surface PLAYBOOK (wire §762 seams + the covered authz: OPERATOR_WRITE
  fail-closed [B752], ChiRbacHelper enforcement [chihuahua-source CS3], CSRF [B58/B74]) → B763.
