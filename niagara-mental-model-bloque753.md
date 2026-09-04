# B753 · WB + UX authoring playbook for OUR modules — what to build, what to skip, and the decisions for ColdRoomPan / CompPan / DashboardPan

> **Scope**: turn the WB authoring ladder (B751) and the UX serving/RBAC census (B752) into concrete decisions
> for our modules. What -wb (if any) each should have, which -ux recipe we already use and why it's the right
> one, and the gaps worth closing. Closes the wb-ux-authoring focus (WBUX5). DESIGN block — high [INFER];
> every FACT cites B751/B752 or our source.
>
> **Sources**: FUENTE 1 — B751 (wb ladder + recipes), B752 (ux recipes + RBAC contrast), B706/B707 (best
> practices), B729-B750 (rt + organization), B742/B731 (our backlog/audit). Our modules: DashboardPan-ux/rt,
> ColdRoomPan-rt, CompPan-rt (source on disk + prior blocks).

---

## 753.1 — WB: our modules need almost NO -wb, and that is CORRECT `[CERT/INFER]`
Apply the B751 ladder to ColdRoomPan/CompPan:
- Our components are plain `BComponent`s with standard slots (doubles, booleans, enums, `BStatus*`). By the
  ladder (B751 §751.1), **rung 0 — nothing** — the default property sheet + wire sheet render them fully.
  kitControl ships 152 rt types with only 2 wb FEs (B751); we are far below that bar. **Do not author a
  Manager or a custom View** — we are not a driver (no learned/discovered children) and our interaction is
  tabular/wire-sheet, so rungs 2-3 do not apply. [INFER, grounded in B751 + B734 (we're not points/drivers)]
- **The one candidate for rung 1 (a FieldEditor)**: a composite/opaque value that renders poorly by default —
  e.g. an HOA-mode or a defrost-schedule value. Today our HOA is a plain `double` 0/1/2 (deliberate, B740
  cross-module safety), which renders fine with a `trueText/range` facet — so even rung 1 is unnecessary
  unless we introduce a genuinely composite value type. **Recommendation: no -wb for now; if a value ever
  reads badly, add ONE `BWbFieldEditor @AgentOn(that type)` (B751 §751.2), nothing more.** [INFER]
- If we ever ship a family of refrigeration devices commissioned from a network, THEN the Honeywell
  device-model PLUGIN pattern (B751 §751.3) is the model to copy — but that is a driver-shaped future, not
  today's fixed-room modules.

## 753.2 — UX: we already use the right recipe (servlet-SPA), and better RBAC than the vendors `[CERT]`
DashboardPan-ux is a **servlet-served SPA** (`BWebServlet` + static HTML + REST-poll), which B752 §752.1 marks
as the correct recipe for a bespoke dashboard + custom JSON API. And our RBAC is the STRONGEST in the census:
we enforce `BPermissions.OPERATOR_WRITE` fail-closed on every write, while the Honeywell React SPAs (TC, Sylk)
ship `permissions="unrestricted"` RPCs with no server RBAC (B752 §752.5). **Keep the recipe; do not migrate to
bajaux.** The vendor React SPAs are heavier (5.6MB builds) and weaker on authorization — no reason to copy them.

Where the vendor patterns DO offer something to adopt selectively:
- **PX for engineer graphics** (B752 §752.3): if an integrator wants to hand-author an equipment schematic
  without touching our SPA, a `.px` page bound to our slot ords (`BoundLabelBinding ord="slot:…"
  statusEffect="color"`, `SetPointBinding` for writes) is a zero-Java option that rides the workbench PX
  runtime. Complementary to the SPA, not a replacement. [INFER]
- **Fox live-push** (B752 §752.4): our REST-poll (5s + 1s interpolation) is deliberate and works on the HMI
  panel; adopt Fox `subscriberMixIn` ONLY if a screen needs sub-second liveness — not worth it for
  refrigeration setpoints. [INFER]

## 753.3 — The concrete decisions `[INFER]`
| Module | -wb | -ux | Rationale |
|---|---|---|---|
| **ColdRoomPan** | none (rung 0) | none of its own | pure rt logic; visualized through DashboardPan |
| **CompPan** | none (rung 0) | none of its own | same — pure rt |
| **DashboardPan** | none | **keep the servlet-SPA + OPERATOR_WRITE RBAC** (B752) | correct recipe; strongest RBAC in the census |
| (future device family) | Honeywell device-model plugin (B751 §751.3) | servlet-SPA or bajaux manager | only if commissioned from a network |

## 753.4 — Gaps worth closing on the UX we HAVE `[INFER, from B752 §752.6]`
Not new -wb/-ux, but hardening the SPA we run:
1. **Keep the pure `route()` + its WSL unit test** — our best-practice, already in place; extend coverage as
   endpoints grow.
2. **Honor the two documented footguns**: never set a user Home Page to `/dashboardpan/` (raw path is not an
   ORD → every login fails); de-hardcode `SERVICE_ORD` if the service is ever relocated.
3. **Consider a `.px` companion page** for integrators who prefer workbench graphics over the SPA — optional,
   low effort, rides existing slot ords.
4. **Document the `{v,st}` flat-JSON contract + status precedence** as the stable API so the SPA and any
   future client share one path space (already the design; make it explicit).

## 753.5 — One-line rule `[INFER]`
**Author the LEAST wb the ladder allows (rung 0 for our components), keep the servlet-SPA + real permission-bit
RBAC for ux (it beats the vendor bajaux SPAs on both simplicity and authorization), and reach for a FieldEditor,
a device-model plugin, PX, or Fox only when a specific need crosses the bar B751/B752 set.**

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Our components sit at wb rung 0 (no Manager/View needed); we're not a driver | [CERT/INFER] | B751 §751.1 ladder; B734 (not points/drivers); kitControl 152rt/2wb bar |
| 2 | Rung 1 FieldEditor only if a composite value renders badly; our HOA=double renders fine with a facet | [INFER] | B751 §751.2; B740 (double not enum); B735 (facets) |
| 3 | DashboardPan servlet-SPA is the correct ux recipe for a bespoke dashboard | [CERT] | B752 §752.1 |
| 4 | Our OPERATOR_WRITE fail-closed RBAC is stronger than vendor unrestricted-RPC SPAs | [CERT] | B752 §752.5 (DashboardRbacHelper vs BThermostatWizardRPC) |
| 5 | PX companion + Fox push are selective, needs-driven adoptions, not defaults | [INFER] | B752 §752.3/§752.4 |
| 6 | Concrete decisions per module (table); future device family → Honeywell plugin pattern | [INFER] | B751 §751.3; our module shapes |

**Tally**: 2 [CERT], 1 [CERT/INFER], 3 [INFER]. High [INFER] expected (applied design); every FACT cites a
block. No unmarked claims.

## Connections
- **B751** (wb ladder + recipes), **B752** (ux recipes + RBAC), **B706**/**B707** (best practices), **B742**
  (our backlog), **B731** (audit), **B734** (why we're plain components not points), **B740** (HOA=double),
  **B735** (facets), and the build-n4-module kit `types/wb-widgets.md` (seed) + `types/dashboard.md` (mature).

## Open gaps
- **B753-G1**: a `.px` companion page for DashboardPan bound to our slot ords — an implementation task
  (requires-execution).
- **B753-G2**: extend the pure-`route()` unit-test coverage as `/api/*` endpoints grow — implementation.
