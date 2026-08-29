# Niagara N4 — own-modules-audit (OMA1): the systemic patterns across ALL the operator's modules — universal over-permissioning, frozen `vendorVersion 1.0`, one build host, a signer migration in progress, and several near-empty `-ux`/`-wb` shells

**Focus**: own-modules-audit · **Gap**: OMA1 (systemic cross-module patterns) · **Session**: 2026-08-29 · **Block**: B640 · **Type**: direct-artifact synthesis.
**Sources** (`[CERT]` direct artifact — manifest scan of all custom jars in the live install `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/`, this session): `angeles`, `demoangeles`, `interfaz1`, `dashboardups`, `datacenter`, `electri`, `httpClientGAngeles`, `mcpbridge`, `multivistaspersonalizados`, `sanluis`, `sdash`, `sejofadashboard`, `tr3z` (rt/ux/wb). `nmodsreflow` = REMIT ([B138]–[B155]); `chihuahua` = REMIT ([B636]).

**Scope**: the deviations shared by MANY of the operator's modules — the "shop signature" — graded against the reference skeleton [B636]. Per-module deep-dives = OMA2–OMA7.

---

## 640.1 The cross-module matrix (measured)

`[CERT]` manifest scan (permGroups = count of `<niagara-permission-groups>`; class = `.class` count; ver = vendorVersion):

| Module | ver | permGroups | rt types/class | ux types/class | palette | signer |
|---|---|---|---|---|---|---|
| angeles | 1.0 | 3 | 15/15 | 1/6 | ✅ | NIAGARA4 |
| demoangeles | 1.0 | 3 | 16/16 | 1/2 | ✅ | NIAGARA4 |
| interfaz1 | 1.0 | 3 | 2/2 | **0/0** ux, **0/0** wb | ✅ | NIAGARA4 |
| dashboardups | 1.0 | 3 | 1/2 | **0/0** | ✅ | NIAGARA4 |
| datacenter | 1.0 | 3 | 4/4 | 1/**220** | ✅ | NIAGARA4 |
| electri | 1.0 | 3 | 12/12 | 1/1 | ✅ | NIAGARA4 |
| **httpClientGAngeles** | **4.14.0.162** | **0** | 87/122 | 9/9 (+wb 11/18) | rt✅ | NIAGARA4 |
| mcpbridge | 1.0 | 3 | 1/**206** (rt-only) | — | ✅ | NIAGARA4 |
| multivistaspersonalizados | 1.0 | 3 | 8/8 | 1/1 | ✅ | NIAGARA4 |
| sanluis | 1.0 | 3 | 6/6 | 1/6 | ✅ | NIAGARA4 |
| **sdash** | 1.0 | **4** | 12/**2186** | 2/2 | ✅ | **SEJOFA_C** |
| sejofadashboard | 1.0 | 3 | 3/3 | **0/0** | ✅ | NIAGARA4 |
| tr3z | 1.0 | 3 | 8/8 | **0/0** | ✅ | NIAGARA4 |

Five systemic patterns fall out.

---

## 640.2 Pattern 1 (MED, security-shaped): universal over-permissioning

`[CERT]` — **every module except `httpClientGAngeles` declares `<niagara-permission-groups>` = 3** (`type="all"` + `workbench` + `station`); `sdash-rt` declares 4. Only `httpClientGAngeles` declares **0** (falls to the minimal base grant, [B635]). This is the chihuahua deviation ([B636] #1) replicated shop-wide — a template habit, not a per-module decision.

As [B635] established, under the default `GrantAllPermissionGroupStore` this is granted anyway, so it is not an active escalation today; but it is maximal-intent everywhere, and would become real over-privilege the day a restrictive store is deployed. **§14 REFINED by [B649]**: source inspection of chihuahua shows the `type="all"` groups are the UNTOUCHED Tridium scaffold — EMPTY (placeholder comments, `<req-permission>` commented out), no `<java-permissions>` — so they request NOTHING beyond the base grant ([B635]); this pattern counted the empty group DECLARATIONS, not actual grants. Effective posture is minimal, not over-privileged; the fix is cosmetic cleanup, not a privilege reduction. (Applies fleet-wide — same scaffold.) **`httpClientGAngeles` proves the correct pattern is achievable** in the shop's own toolchain — it declares none. Fix: default new modules to NO `<permissions>` (OMA8 template).

---

## 640.3 Pattern 2 (MED, hygiene): frozen at `vendorVersion 1.0`

`[CERT]` — **12 of 13 modules are stuck at `vendorVersion="1.0"`**; only `httpClientGAngeles` tracks a real version (`4.14.0.162`). Yet these are production modules with real change history (chihuahua reached 1.3 with a version log, [B636]/[B638]; the `sw/` dir holds multiple versions). A frozen 1.0 across the fleet means the station cannot distinguish builds by version — every dependency check ([B630] `checkVendor`) sees "1.0", and a field diagnosis cannot tell which build is deployed. Fix: adopt the chihuahua `deploy.sh --bump` habit ([B637]) fleet-wide, or the `httpClientGAngeles` version-tracking pattern.

---

## 640.4 Pattern 3 (INFO): one build host, one identity migration

`[CERT]` — **all modules built on `buildHost=DESKTOP-4AAQ77H`** (the operator's dev machine = the live station host, [B156]). Single-developer/single-host build — fine for a small shop, but a bus-factor + no-CI signal (consistent with [B637]'s manual WSL/PowerShell deploy scripts). Signer: **12 modules use the `NIAGARA4` block name, `sdash` uses `SEJOFA_C`** — the [B639] migration in the wild: the shop moved from the SEJOFA chain to `angelessignerCA` (ANGELES), and `sdash` is the straggler still on the legacy chain. Fix: re-sign `sdash` with `angelessignerCA` (OMA4).

---

## 640.5 Pattern 4 (LOW, correctness): 0-class `-ux` parts are PURE-WEB ux (legitimate) — only one part is genuinely empty

> **§14 SELF-CORRECTION (same session, B640).** My first pass read this pattern from CLASS/TYPE counts alone and called `interfaz1-ux`, `dashboardups-ux`, `sejofadashboard-ux`, `tr3z-ux` "near-empty shells." **Reading the residue disproves it** `[CERT]`: those `-ux` jars ship `rc/` WEB ASSETS — `interfaz1-ux` (dashboard.html/js, alarmas, notifier), `dashboardups-ux` (9: dashboard.html/js, ChartManager.js, css), `sejofadashboard-ux` (21 css/js), `tr3z-ux` (11 incl. index.html). A `-ux` with 0 `.class`/0 `<type>` but a full `rc/` tree is a **legitimate pure-frontend ux module** — all UI is HTML/JS/CSS served from the jar ([B632]: ux payload = `rc/*`), no Java/Baja types required. This is CORRECT design, not a defect (it is the clean end of the profile split: zero Java in the browser tier). The lesson is my own ([B636]/kit "read the residue before theorising"): class-count ≠ emptiness for a ux part.

`[CERT]` — the ONE genuinely near-empty part is **`interfaz1-wb`**: 0 classes, 0 types, AND 0 web assets — only `META-INF` + `module.palette` + lexicon. THAT is an empty signed jar that still costs a load slot + signature-verify at boot ([B630]); confirm it is intentional (a wb part reserved for a future view) or drop it. Fix scope: just `interfaz1-wb`, not the pure-web ux parts.

**Contrast worth noting**: nearly all these modules DO ship a `module.palette` (palette=✅), which chihuahua did NOT ([B636] #4). So the shop's palette habit is good — chihuahua is the outlier that skipped it.

---

## 640.6 Pattern 5: three size anomalies (→ deep-dives)

`[CERT]` — three modules break the small-dashboard mold and get their own blocks:
- **`sdash-rt` = 2186 classes** (12 types, SEJOFA_C) → OMA4: almost certainly an uber-jar bundling a large library.
- **`mcpbridge-rt` = 206 classes, 1 type, rt-only** → OMA5: an MCP bridge; a novel integration module.
- **`datacenter-ux` = 220 classes, 1 type** → OMA3: heavy Java in the browser profile ([B636] #5 at extreme).

---

## 640.7 The shop signature (what OMA8 will consolidate)

The operator's modules share a consistent fingerprint: **tri/bi-profile dashboards, palette-shipping, single-host built, over-permissioned by template, frozen at 1.0, mid-migration from SEJOFA→ANGELES signing.** The recurring FIXES are few and template-level: (1) stop declaring `type="all"` permissions, (2) version-bump per release, (3) re-sign the SEJOFA straggler, (4) prune empty parts. `httpClientGAngeles` already embodies the corrected template (0 perms, real version) — it is the shop's own positive exemplar (OMA6).

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | 12/13 modules declare permGroups=3 (all+wb+station); only httpClientGAngeles=0; sdash=4 | [CERT] | manifest scan (this session) | ✅ unzip -p |
| 2 | 12/13 stuck at vendorVersion 1.0; only httpClientGAngeles=4.14.0.162 | [CERT] | manifest scan | ✅ unzip -p |
| 3 | all buildHost=DESKTOP-4AAQ77H; signer NIAGARA4 except sdash=SEJOFA_C | [CERT] | manifest scan + jar META-INF | ✅ unzip |
| 4 | 0-class -ux parts (interfaz1/dashboardups/sejofadashboard/tr3z-ux) ship rc/ web assets = pure-web ux (legit); only interfaz1-wb genuinely empty (§14 self-corrected) | [CERT] | unzip -l (web-asset counts 9/21/11; interfaz1-wb=0) | ✅ unzip -l |
| 5 | nearly all ship module.palette (unlike chihuahua, B636 #4) | [CERT] | manifest scan (palette=1) | ✅ unzip -l |
| 6 | anomalies: sdash-rt 2186 class, mcpbridge-rt 206 (rt-only), datacenter-ux 220 | [CERT] | manifest scan | ✅ unzip -l |
| 7 | over-permission is granted-anyway today (GrantAll) but future risk | [INFER] | [B635] | ✅ cross-ref |

**Tally**: [CERT] ×6 · [INFER] ×1 · direct-artifact synthesis (all real-jar reads). Every count token-checked against the jars this session.

## Connections

- **[B636]** — the rubric; chihuahua deviations #1 (perms) and #4 (palette) generalize/invert here. **[B635]** — over-permission is soft (GrantAll). **[B639]** — the NIAGARA4/SEJOFA_C signer migration. **[B630]** — empty parts still cost a load/verify slot.
- Forward: OMA4 (sdash 2186), OMA5 (mcpbridge), OMA3 (datacenter-ux), OMA6 (httpClientGAngeles exemplar), OMA2/OMA7 (per-module + empty shells), OMA8 (synthesis + template).

## Gaps uncovered

- None new; OMA1 is the systemic layer feeding OMA8. The three anomalies are already backlog rows (OMA3/4/5).
