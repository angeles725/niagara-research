# Niagara N4 — own-modules-audit (OMA7): the six small SEJOFA dashboards are one repeatable TEMPLATE — `com.sejofa.<mod>.components` with a `BXxxDashboardService` + domain `Monitor`s (rt) and a pure-web `-ux` — cloned per client site, carrying only the systemic deviations

**Focus**: own-modules-audit · **Gap**: OMA7 (small SEJOFA dashboards) · **Session**: 2026-08-29 · **Block**: B646
**Sources** (`[CERT]` direct artifact): `electri`, `sanluis`, `sejofadashboard`, `tr3z`, `multivistaspersonalizados`, `dashboardups` (`-rt`/`-ux`) in `/mnt/c/…/modules/`. All dev/demo (only chihuahua is production, [B643]).

**Scope**: grouped grade vs [B636]; the shop's dashboard TEMPLATE. Systemic patterns = [B640] (REMIT).

---

## 646.1 One template, cloned per site

`[CERT]` manifest scan — the six share an identical shape:

| Module | description | rt types (namespace `com.sejofa.<mod>.components`) | ux web-assets |
|---|---|---|---|
| electri | "Electri Dashboard Multivistas - Backend RT" | `BElecDashboardService` + `BElecCarcamosMonitor`/`BElecMonitoreoMonitor` | 17 |
| sanluis | "Dashboard BMS" | `BSnlsDashboardService` + `BSnlsPiso4Monitor`/`BSnlsPiso5Monitor` | 26 |
| sejofadashboard | "SEJOFA Dashboard" | `BDashboardService` + `BDashboardConfig` + `BSejoFaServlet` | 21 |
| tr3z | "Dashboard multiples vistas" | `BTr3zDashboardService` + `BTr3zFireProtectionMonitor`/`BTr3zHighVoltageMonitor` | 11 |
| multivistaspersonalizados | "Dashboard Multivistas Personalizados" | `BMultpDashboardService` + `BMultpFireProtectionMonitor`/`BMultpHighVoltageMonitor` | 22 |
| dashboardups | "Dashboard" | `BUPSMonitor` | 9 |

The pattern is uniform: an rt `DashboardService` + a handful of domain `Monitor` components (per the site's equipment — Cárcamos, Pisos, FireProtection, HighVoltage, UPS), and a **pure-web `-ux`** (9-26 `rc/` HTML/JS/CSS assets, 0 Java — the [B640] §640.5 pure-web pattern, confirmed across all six). This is the SAME architecture as chihuahua ([B636]) and angeles ([B642]): `DashboardService` + Monitors + pure-web ux. The shop has a **repeatable dashboard template** it clones per client site (San Luis, TR3Z, etc.) — a genuine strength.

`sejofadashboard` is the slightly richer base (`BDashboardConfig` + `BSejoFaServlet` — a config component + servlet), plausibly the template's origin from which the site-specific ones (electri/sanluis/tr3z/multivistas) were forked.

---

## 646.2 Grade: template-level deviations only

`[CERT]` — all six carry ONLY the systemic deviations ([B640]): `vendorVersion 1.0`, `<niagara-permission-groups type="all">`, single build host, NIAGARA4 signer. None fork vendor code ([B641]), none have misplaced-logic ux ([B645] refuted the concern — their ux is pure-web), all ship a palette ([B640] good habit). The `-ux` pure-web parts are correct design, not empty shells ([B640] §640.5 correction applies to all six).

So the fix for the whole group is ONE template change, not six module fixes: correct the shared template (drop `type="all"`, version-bump policy) and re-clone/rebuild. This is the payoff of the OMA1→OMA7 arc: the deviations are template-level, so the remediation is too.

---

## 646.3 Recommendation

- **Fix the template once** ([B640] P1/P2): remove `<permissions>` (base grant suffices for a dashboard, [B635]); adopt per-release version bumps ([B637] `deploy.sh --bump`). Re-generate the site clones from the corrected template.
- **Consolidate**: six near-identical dashboards suggests a shared `-rt` base (common `DashboardService` + Monitor superclasses) + thin per-site modules, rather than full clones — less duplicate code, one place to fix bugs. (Optional refactor, not urgent — dev/demo.)
- **Keep**: the DashboardService+Monitor+pure-web-ux template itself is sound and matches the production chihuahua shape.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | all six = BXxxDashboardService + domain Monitors under com.sejofa.<mod>.components | [CERT] | manifest scan (this session) | ✅ unzip -p |
| 2 | all six ux are pure-web (9-26 rc/ assets, 0 Java) | [CERT] | ux web-asset counts | ✅ unzip -l |
| 3 | sejofadashboard is the richer base (BDashboardConfig + BSejoFaServlet) | [CERT] | sejofadashboard-rt.jar!module.xml | ✅ unzip -p |
| 4 | deviations are systemic/template-level only (ver 1.0, type=all); no vendor forks; palettes shipped | [CERT] | manifest scan + [B640] | ✅ unzip -p |

**Tally**: [CERT] ×4 · [INFER] ×0 · direct-artifact grouped block. Types/namespaces/ux-payload token-checked against the jars this session.

## Connections

- **[B640]** — systemic deviations (template-level); pure-web ux correction applies. **[B636]/[B642]** — same DashboardService+Monitor+ux template (chihuahua/angeles). **[B645]** — pure-web ux confirmed (not misplaced logic). **[B637]** — version-bump habit to adopt.
- Forward: OMA8 (the shop signature + one-template remediation).

## Gaps uncovered

- None. Whether the six share a common base or are full clones is a source-tree question (their source isn't all on disk like chihuahua's) — a refactor note, not a research gap.
