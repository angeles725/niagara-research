# Niagara N4 — own-modules-audit (OMA2): the ANGELES-namespace modules (`angeles`, `demoangeles`, `interfaz1`) — a clean equipment-component + Monitor pattern, split across two Java namespaces (`com.sejofa.*` vs `com.angeles.*`), carrying the systemic deviations plus one genuinely empty `-wb` part

**Focus**: own-modules-audit · **Gap**: OMA2 (ANGELES-namespace modules) · **Session**: 2026-08-29 · **Block**: B642
**Sources** (`[CERT]` direct artifact): `angeles-{rt,ux}.jar`, `demoangeles-{rt,ux}.jar`, `interfaz1-{rt,ux,wb}.jar` in `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/`.

**Scope**: grade the operator's direct-namespace modules against [B636]; systemic patterns = [B640] (REMIT, not re-argued). Reference = [B636]; chihuahua = [B636] (the sibling).

---

## 642.1 `angeles` — a clean HVAC equipment model

`[CERT]` `angeles-rt.jar!module.xml` — `moduleName="angeles"`, vendor ANGELES, 15 types under **`com.sejofa.angeles.components`**, a consistent **equipment + Monitor twin** pattern:
```
BAngChiller / BAngChillerMonitor · BAngCoolingTower / …Monitor · BAngDieselGenerator / …Monitor
BAngKitchenExhaust / …Monitor · BAngMinisplit / …Monitor · BAngVavBox / …Monitor
BAngPackageUnit / …Monitor · BAngDashboardService
```
This is a well-shaped domain model: one `BComponent` per equipment class + a paired `Monitor`, fronted by a `BDashboardService` — the same architecture chihuahua uses ([B636]/[B163]-[B177]). 15 types / 15 classes (1:1, no dead bytecode — [B636] invariant A.3.4 satisfied), ships a `module.palette` ([B640] good habit), `angeles-ux` is a thin/pure-web ux (1 type / 6 classes). Deviations are the systemic ones only: `vendorVersion 1.0` ([B640] P2), `<niagara-permission-groups type=all>` ([B640] P1).

---

## 642.2 `demoangeles` — a demo/template variant

`[CERT]` `demoangeles-rt.jar!module.xml` — `moduleName="demoangeles"`, 16 types under **`com.sejofa.demoangeles.components`**: `BDemangDashboardService` + monitors for Floor/Location/Plant/Fire/Piping/Climate/Hv + `BDemangRtu`. A DEMO module (the `Demang` prefix, the generic floor/location/plant monitors) — a template/showcase, not a client deliverable. Same 1:1 type:class discipline, palette shipped, `demoangeles-ux` minimal (1 type / 2 classes). Same systemic deviations. Recommendation: mark demo modules clearly (a `description` tag + keep them OFF production stations) — a demo on a live supervisor is attack surface with no operational value.

---

## 642.3 `interfaz1` — pure-web ux + one genuinely empty `-wb`

`[CERT]` `interfaz1-rt.jar!module.xml` — `moduleName="interfaz1"`, 2 types under **`com.angeles.interfaz1.components`**: `BDashboardComponent`, `BNotifierHoneywellComponent` (a Honeywell-notifier integration + a dashboard component). Profiles:
- `interfaz1-ux` — **pure-web ux** ([B640] §640.5 corrected): 0 classes/0 types but a full `rc/` tree (`dashboard.html/js`, `alarmas.html/js/css`, `notifier.js`) + palette + lexicon. Legitimate clean design.
- `interfaz1-wb` — **genuinely empty** `[CERT]`: 0 classes, 0 types, 0 web assets — only `META-INF` + `module.palette` + lexicon. This is the ONE real "empty shell" of the fleet ([B640] P4 corrected). It still costs a load slot + signature-verify at boot ([B630]). Fix: drop `interfaz1-wb` (or add the wb view it was reserved for).

---

## 642.4 The namespace split (INFO)

`[CERT]` — the operator's modules do NOT share one Java namespace under the ANGELES vendor:
- `com.sejofa.angeles.*`, `com.sejofa.demoangeles.*` (SEJOFA namespace)
- `com.angeles.interfaz1.*`, `com.angeles.chihuahua.*` ([B629]/[B636]) (ANGELES namespace)

Vendor attribute is `ANGELES` for all, but the Java package roots split between `com.sejofa` and `com.angeles`. This is harmless (classloader isolation is per-module, [B617]) but inconsistent — it reflects the shop's SEJOFA→ANGELES rebrand ([B639] signing migration) not fully carried into package names. Recommendation: standardize new modules on one root (e.g. `com.sejofa.<module>`) so the namespace matches the vendor/signing identity.

---

## 642.5 Grade summary (vs [B636])

| Module | Type model | Deviations | Verdict |
|---|---|---|---|
| angeles | ✅ equipment+Monitor, 1:1, palette | systemic (perms, ver1.0) | clean; apply template fixes |
| demoangeles | ✅ demo monitors, palette | systemic + demo-on-prod risk | mark as demo, keep off prod |
| interfaz1 | ✅ 2 types, pure-web ux | systemic + `interfaz1-wb` empty | drop empty wb part |

All three are structurally sound (correct profile split, no dead types, palette shipped); their only real issues are the shop-wide template ones ([B640]) plus the one empty `interfaz1-wb`. None fork vendor code (unlike httpClientGAngeles, [B641]).

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | angeles = 15 types com.sejofa.angeles equipment+Monitor twins + DashboardService, 1:1 class:type, palette | [CERT] | angeles-rt.jar!module.xml + scan | ✅ unzip -p |
| 2 | demoangeles = 16 types com.sejofa.demoangeles, demo monitors (Floor/Plant/Fire/…)+Rtu | [CERT] | demoangeles-rt.jar!module.xml | ✅ unzip -p |
| 3 | interfaz1-rt = 2 types com.angeles.interfaz1 (Dashboard + NotifierHoneywell); ux pure-web (rc/) | [CERT] | interfaz1-{rt,ux}.jar | ✅ unzip |
| 4 | interfaz1-wb genuinely empty (0 class/0 type/0 web asset) | [CERT] | interfaz1-wb.jar unzip -l | ✅ unzip -l |
| 5 | namespace split com.sejofa.* (angeles/demoangeles) vs com.angeles.* (interfaz1/chihuahua) under one ANGELES vendor | [CERT] | module.xml type classes | ✅ unzip -p |

**Tally**: [CERT] ×5 · [INFER] ×0 · direct-artifact block. All type/namespace/payload claims token-checked against the jars this session.

## Connections

- **[B640]** — systemic deviations (REMIT here) + the P4 correction (pure-web ux) applied. **[B636]** — the rubric + chihuahua sibling (same equipment+Monitor+DashboardService pattern). **[B639]** — the SEJOFA→ANGELES migration behind the namespace split. **[B630]** — empty `interfaz1-wb` still costs a boot slot. **[B641]** — these DON'T fork vendor code (contrast).
- Forward: OMA7 (small SEJOFA dashboards), OMA8 (template: standardize namespace + drop empty parts).

## Gaps uncovered

- None new. `BNotifierHoneywellComponent` (interfaz1) is a Honeywell-notifier integration; its wire protocol is out of scope for a packaging audit (would be a driver focus, not opened).
