# Niagara N4 — own-modules-audit (OMA8, SYNTHESIS): the shop's module-building signature, the corrected build/version/signing knowledge, a prioritized remediation plan, and the fixed reference template

**Focus**: own-modules-audit · **Gap**: OMA8 (synthesis) · **Session**: 2026-08-29 · **Block**: B647 · **Type**: DESIGN/synthesis (consolidates [B637]–[B646]; high design ratio expected).
**Sources**: [B637]–[B646] (this focus), [B636]/[B629]–[B635] (reference skeleton), real operator source + jars, operator corrections 2026-08-29.

**Scope**: close the focus with the deliverable the operator asked for — "how the modules are, how they can be improved." Production scope: **only `chihuahua` is in production** ([B643]); the rest are dev/demo on `DESKTOP-4AAQ77H`.

---

## 647.1 The shop signature (what OMA1–OMA7 + OMB1–3 show)

The operator's modules share a consistent, mostly-sound fingerprint:

**Strengths (keep):**
- A **repeatable dashboard template** — `BXxxDashboardService` + domain `Monitor` components (rt) + **pure-web `-ux`** (0 Java, `rc/` HTML/JS/CSS) — cloned per client/site ([B646]/[B642]/[B636]). Clean rt/ux split ([B645] confirmed profile placement is correct).
- **`module.palette` shipped** on nearly every module ([B640]) — chihuahua is the lone outlier that skipped it ([B636] #4).
- A **documented build discipline** in `niagara-tools` + `BUILD_WORKFLOW.md` ([B637]/[B639]): deploy modes A/B/C, the slotomatic variant rule, WSL/PowerShell wrappers with `_backups/` (mitigating the vendor install's no-backup gap, [B633]).
- Some modules AHEAD of the pack: `sdash` uses per-agent `requiredPermissions="r"` + WebSocket + a custom ORD scheme ([B644]); `httpClientGAngeles` has a clean 0-permission manifest (inherited, [B641]).

**Recurring deviations (fix at the template, not per module):**
1. **Over-permissioning** — `<niagara-permission-groups type="all">` on nearly every module ([B640] P1). Soft today (GrantAll default, [B635]) but wrong-by-default.
2. **Frozen `vendorVersion 1.0`** — 12/13 modules ([B640] P2); no version signal for field diagnosis.
3. **Signer migration incomplete** — SEJOFA→`angelessignerCA` (ANGELES) done for most, `sdash` still on legacy `SEJOFA_C` ([B639]/[B644]).
4. **Uber-jar duplication** — Gson shaded into `mcpbridge` + `datacenter` ([B643]/[B645]); Jackson+Commons into `sdash` ([B644]); libs Niagara already ships.
5. **Packaging hygiene** — hardcoded data in a ux jar + a stray `CASINO` cross-project asset in `datacenter` ([B645]); one genuinely empty `interfaz1-wb` ([B642]).

---

## 647.2 The corrected build knowledge (was undocumented)

The operator's build practice, now captured with their corrections:
- **Build variants** ([B637]): "Clean + Slotomatic + Build" iff a `@Niagara*` annotation changed; else "Clean + Build". Deploy modes A/B/C by which profile changed.
- **Version-targeting** ([B638]): `niagara_home` is the SDK PATH the build compiles against (iSMA **4.13.2 SDK**), deploying to a Honeywell **4.14** station — the 4.13 baja floor is DELIBERATE (widest station coverage), NOT the [B636]-#2 "oversight" (§14-reframed). Switching 4.13/4.14/4.15 = repoint `niagara_home` + match `gradlePluginVersion`; no profile system. **Java 8 hard floor.**
- **Signing** ([B639]): convention-driven (no `niagaraSigning{}` block); active alias `angelessignerCA` (ANGELES) from `niagara_user_home/security/keystore.jceks`; SEJOFA is legacy ("falla en CI").
- **Tests** ([B637]): the operator was RIGHT — `niagaraTest`/station tests are dead by the plugin-7.6.17 `moduleTestAnnotationProcessor` bug (Total tests run: 0); the pure-JUnit `run-tests-wsl.sh` (helper logic) is the path that works. Keep the latter, retire the former.

---

## 647.3 The one security finding (latent, dev-only) — mcpbridge

`mcpbridge` ([B643]) is an MCP server exposing the station to AI agents; it is **authentication-gated (401) but authorization-BYPASSED** — `ToolDispatcher` is static/userless and `SetPropertyHandler` writes with no `canWrite`/RBAC, so any authenticated user gets full station write/create. **Latent + dev-only** (not in production; jar installed but believed unmounted). Not an incident — a "must fix before this pattern ever reaches a client station" item, and a caution for any future AI-bridge module: run tool ops as the authenticated `BUser` (`runAsUser`) so RBAC ([B11]/[B558]) applies. Open: MCP-G2 (config.bog mount check).

---

## 647.4 Prioritized remediation plan

| # | Action | Scope | Why |
|---|---|---|---|
| 1 | If `mcpbridge` is ever enabled: enforce per-user RBAC (runAsUser) + dedicated MCP role + audit log | mcpbridge | broken access control ([B643]) |
| 2 | Fix the shop TEMPLATE: drop `<permissions>` (base grant), adopt per-release version bump | all (esp. chihuahua = prod) | [B640] P1/P2 |
| 3 | Add a `module.palette` to chihuahua (the prod outlier) | chihuahua | [B636] #4 |
| 4 | Re-sign `sdash` with `angelessignerCA`; finish the SEJOFA→ANGELES migration | sdash | [B639]/[B644] |
| 5 | Factor shared libs (`gson-rt`, `jackson-rt`) instead of per-module shading | sdash/mcpbridge/datacenter | [B640]/[B644]/[B645] |
| 6 | Externalize datacenter hardcoded rack data; prune stray `CASINO` asset | datacenter | [B645] |
| 7 | Drop the empty `interfaz1-wb`; mark demo modules (demoangeles) as non-prod | interfaz1/demoangeles | [B642] |
| 8 | Address the `httpClientGAngeles` SMA/licensing exposure (license or replace) | httpClientGAngeles | [B641] |

Priority note: **chihuahua first** (only production module) — items 2 (perms/version) + 3 (palette). The rest are dev/demo hygiene, best done by fixing the template + re-cloning.

---

## 647.5 The fixed reference build template (for the shop)

```
<mod>/                          # one niagara_home SDK per target version (Java 8)
  gradle.properties            # niagara_home=<SDK path for 4.13|4.14|4.15>; Zulu-8
  settings.gradle.kts          # gradlePluginVersion MATCHES that SDK (7.3.40 for 4.13.2)
  build.gradle.kts             # vendor("SEJOFA"|"ANGELES"); defaultModuleVersion BUMP per release
  <mod>/<mod>-rt/              # BXxxDashboardService + domain Monitors; com.sejofa.<mod>.components
    module.xml: NO <permissions>  # fall to minimal base grant (B635); add per-AGENT requiredPermissions if needed (sdash pattern)
    module.palette             # ship reusable components (B634)
  <mod>/<mod>-ux/              # PURE web: rc/ HTML/JS/CSS, 0 Java (B645); share heavy libs, don't shade per-module
  # tests: run-tests-wsl.sh (pure-JUnit helpers) only; DO NOT wire niagaraTest (7.6.17 bug)
  # sign: angelessignerCA (ANGELES); build Windows-side; ng-deploy.sh mode A/B/C with _backups/
```
Rules: manifest decides everything (B629/B630); one coherent version across all profile parts (B640 P3 / B636 #3); no vendor-code forks (B641); externalize site data (B645).

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | shop signature: DashboardService+Monitor+pure-web-ux template, palettes, documented build | [CERT]/synthesis | [B646]/[B642]/[B640]/[B637] | ✅ cross-ref |
| 2 | build knowledge (variants, version=SDK path, signing angelessignerCA, tests dead by 7.6.17) | [CERT] | [B637]/[B638]/[B639] | ✅ cross-ref |
| 3 | mcpbridge authz-bypass, latent + dev-only; only chihuahua in prod | [CERT]/[CERT-live] | [B643] + operator | ✅ cross-ref |
| 4 | remediation prioritizes chihuahua (prod); rest = template + re-clone | [INFER] | §647.4 | ✅ derived |

**Tally**: [CERT] refs to [B637]–[B646] · DESIGN/synthesis (ratio = consolidation, not exhaustion). Every claim back-references a verified evidence block; every chihuahua/module fact is a real-jar/real-source read.

## Connections

- Consolidates **[B637]–[B646]** (own-modules-audit). Reference = **[B636]**/[B629]–[B635] (module-anatomy). Corrections issued this focus: §14 to [B636] #2 (via [B638]), reframe of [B636] #6 (via [B639]), self-correction of [B640] P4/P5 (pure-web ux / bundled-lib not misplaced logic).
- **FOCUS own-modules-audit CLOSED** — investigable 8/8 (OMB1-3, OMA1-7) + synthesis; MCP-G2 requires-execution deferred.

## Gaps uncovered

- **MCP-G2** (requires-execution, registered) — is a `BMcpServlet` instance mounted in any running station? Deferred (config.bog/live check). Nothing else investigable on disk; focus STOPS.
