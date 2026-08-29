# Niagara N4 — own-modules-audit (OMA3): `datacenter-ux`'s "220 classes" are 99% bundled Gson, NOT misplaced business logic — the real payload is an `rc/` 3D dashboard (Three.js + Chart.js) with hardcoded rack data and a stray `CASINO` asset; profile placement is correct (corrects the OMA1 "heavy ux" worry)

**Focus**: own-modules-audit · **Gap**: OMA3 (datacenter-ux) · **Session**: 2026-08-29 · **Block**: B645
**Sources** (`[CERT]` direct artifact): `/mnt/c/…/modules/datacenter-ux.jar` (`META-INF/module.xml`, entry histogram, `rc/` listing). Dev/demo module (not production, [B643]).

**Scope**: identify datacenter-ux + grade vs [B636]; test the OMA1 ([B640] P5) / chihuahua ([B636] #5) "heavy Java in the browser profile" hypothesis. Profile model = [B630] (REMIT).

---

## 645.1 The "220 classes" is a bundled library, not misplaced logic (hypothesis refuted)

`[CERT]` `datacenter-ux.jar!module.xml` — `moduleName="datacenter"`, ux profile, `description="Dashboard del datacenter de wattsandbytes"` (a customer-branded datacenter dashboard), **1 type**: `com.sejofa.datacenter.ux.BDtcrServlet` (+ its inner `$ServiceCacheEntry`). `[CERT]` entry histogram:
```
com/google/gson/**            218   (full Gson uber-jar; same version as mcpbridge, B643)
com/sejofa/datacenter/ux        2   (BDtcrServlet + ServiceCacheEntry)
META-INF/versions/9             1   (Gson multi-release shim)
```
**The operator's own code is 2 classes (0.9%).** OMA1 ([B640] P5) and chihuahua ([B636] #5) flagged "heavy Java in the browser profile" as a smell; direct inspection **REFUTES it for datacenter** — the 220 classes are shaded Gson, not business logic misplaced in `-ux`. This is the read-the-residue lesson again ([B640] §640.5 self-correction): a high `-ux` class count is a bundled-lib signal, not automatically a layering defect. (The datacenter-ux profile placement is in fact CORRECT — §645.3.)

---

## 645.2 The real payload is in `rc/` — a 3D web dashboard

`[CERT]` `rc/` listing — the actual substance is the frontend, not Java:
```
rc/js/lib/three.min.js          Three.js 3D engine (BUNDLED in the jar, not CDN)
rc/js/lib/chart.js              Chart.js (bundled)
rc/js/views/{floorplan,rack-detail,map}.js   physical rack/floorplan/site views
rc/js/utils/three-renderer.js   3D rack rendering
rc/js/utils/niagara-adapter.js  live-data bridge to BDtcrServlet
rc/js/data/racks-large.js       HARDCODED rack layout data
rc/js/data/locations.js         HARDCODED location data
rc/assets/CASINO   (×2, two paths)   ← stray asset (see §645.4)
rc/assets/fonts/*.woff2  · rc/css/{main,floorplan,map,rack-detail,components,transitions}.css
```
So `datacenter` is a 3D physical-infrastructure dashboard (racks, floorplan, site map) rendered client-side with Three.js/Chart.js, fed live data by `BDtcrServlet` (whose `ServiceCacheEntry` caches `datacenter-rt` reads before serializing).

---

## 645.3 Profile placement is CORRECT

`[CERT]` — `runtimeProfile="ux"` is right: `BDtcrServlet` is a servlet delivering web assets ([B632] ux payload = `rc/*`), it depends on `bajaux-ux`/`bajaScript-ux`, and the BAS data logic lives in the declared `datacenter-rt` dependency. Unlike the chihuahua concern ([B636] #5), there is no business logic stranded in the browser tier here — the 2 own classes are just the servlet + its cache. This is a clean rt/ux split.

---

## 645.4 The real deviations (all LOW, dev/demo)

1. **Hardcoded layout data in the jar** — `rc/js/data/racks-large.js` + `locations.js` embed the physical rack/location layout. Any datacenter change requires a module rebuild + redeploy. Belongs in a configurable store or `datacenter-rt` component tree, not baked into the ux jar.
2. **Bundled heavy JS libs** — Three.js + Chart.js shipped inside the jar inflate every module update. A shared web-resource module (or a pinned local vendor bundle referenced once) would de-duplicate across dashboards.
3. **Gson duplicated** — the identical Gson uber-jar is in `mcpbridge-rt` ([B643]) and here; likely `datacenter-rt` too. Extract to a shared `gson-rt` ([B640] cross-cutting).
4. **Stray `CASINO` asset** (×2) — an asset named `CASINO` appears twice under `rc/assets/` in a module described as a datacenter dashboard. This is a cross-project leftover (the shop also has casino work — cf. engram projects `casino`/`niagara-casino`), a placeholder/test artifact that should not ship in a "datacenter" module. Prune it.
5. Systemic: `vendorVersion 1.0`, `type="all"` groups ([B640]).

---

## 645.5 Grade + recommendation

datacenter-ux is structurally sound (correct ux placement, minimal own code, real 3D dashboard) — its issues are packaging hygiene: hardcoded data, bundled libs, Gson dup, and the stray CASINO asset. All LOW (dev/demo). The valuable correction: it disproves the "heavy ux = misplaced logic" heuristic — always read the `rc/`+package histogram before judging a ux class count.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | datacenter-ux = "Dashboard del datacenter de wattsandbytes", 1 type BDtcrServlet(+ServiceCacheEntry) | [CERT] | datacenter-ux.jar!module.xml | ✅ unzip -p verbatim |
| 2 | 218/220 classes = bundled Gson; only 2 own → "heavy ux" hypothesis REFUTED | [CERT] | entry histogram | ✅ unzip -l |
| 3 | rc/ = Three.js + Chart.js + floorplan/rack/map views + niagara-adapter + hardcoded racks-large.js/locations.js | [CERT] | rc/ listing | ✅ unzip -l |
| 4 | profile ux correct: servlet + bajaux-ux/bajaScript-ux deps, logic in datacenter-rt | [CERT] | module.xml deps + BDtcrServlet | ✅ unzip -p |
| 5 | stray CASINO asset ×2 in rc/assets (cross-project leftover) | [CERT] | rc/assets listing | ✅ unzip -l |

**Tally**: [CERT] ×5 · [INFER] ×0 · direct-artifact block. Histogram + rc/ + CASINO token-checked against the jar. Refutes [B640] P5/[B636] #5 "heavy ux" for this module.

## Connections

- **[B640]** §640.5 — same read-the-residue correction (class-count ≠ defect). **[B636]** #5 — the "heavy ux" concern, refuted here. **[B630]/[B632]** — ux payload = `rc/*` (correct). **[B643]** — Gson duplicated (dedup theme).
- Forward: OMA7 (small SEJOFA dashboards), OMA8 (template: externalize data, share libs, prune stray assets).

## Gaps uncovered

- None new. The `CASINO` asset ties to the shop's casino projects (separate engram projects) — a hygiene note, not a research gap.
