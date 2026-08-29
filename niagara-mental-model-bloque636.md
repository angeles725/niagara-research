# Niagara N4 — module-anatomy (MA8, SYNTHESIS): the reference module skeleton + the `com.angeles.chihuahua` case study — how a well-built N4 module is shaped, and the concrete deviations to fix

**Focus**: module-anatomy · **Gap**: MA8 (synthesis + chihuahua case study) · **Session**: 2026-08-29 · **Block**: B636 · **Type**: DESIGN/synthesis (high [INFER]/design ratio is expected and healthy — it consolidates evidence blocks [B629]–[B635], it does not open new code).
**Sources**: [B629]–[B635] (this focus), [B12]/[B76]/[B617]/[B398]/[B569]/[B632] (remittances), and the real operator jars `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/chihuahua-{rt,wb,ux}.jar` ([CERT] direct artifact, [B632]).

**Scope**: consolidate the code-side skeleton (Part A) and grade chihuahua against it (Part B). Signing crypto, RBAC, build tooling, lexicon internals = REMIT to the cited blocks.

---

## Part A — the reference module skeleton (the mental model)

### A.1 What a module IS, in one paragraph
A Niagara N4 module is a **signed jar per runtime profile**, aggregated at runtime into one logical `BModule`. Its `META-INF/module.xml` manifest is the single source of truth: it names the module, its `runtimeProfile`, its `<dependency>` set, and its `<type>`/`<permissions>` declarations. The station reads that manifest **twice** — once install-side ([B629] `ModuleManifest`/`BModulePart`), once runtime-side ([B629] `NModule.readXml`) — with no converter between them. Profile is the primary key at every layer: on disk (`(moduleName, RuntimeProfile)` index, [B630]), in the runtime graph (`BModule → Map<RuntimeProfile,NModule>`, [B629]), and in the dependency rules ([B12] §12.1.6 matrix).

### A.2 The lifecycle, end to end (which block owns each step)
```
AUTHOR    module.xml <type>/<dependency>/<permissions>  (hand/wizard; Slotomatic READS it, B631 §14-corrects B12)
  │       @NiagaraProperty/@Action → Slotomatic writes slot code INTO the .java
BUILD     gradle com.tridium.niagara-module: compile → jar → SIGN (NIAGARA4.SF/.RSA)   [B12 · B632]
  │       registry rebuild: <type>+bytecode(ClassScanner) → NTypeInfo[] in RegistryDatabase (.db)  [B630 · B631]
JAR       META-INF/{MANIFEST.MF,.SF,.RSA,module.xml} + classes(javax.baja.*=API / com.tridium.*=impl)
  │       + rt:module.palette+<part>.lexicon · wb:rc/ icons+css · ux:rc/*.js                        [B632]
DISTRIBUTE plat moduleinstall / software mgr: SIGNATURE GATE (vs verificationMode) → stop ALL stations
  │       → stream jar to $NIAGARA_HOME/modules/<part>.jar (overwrite in place, no backup) → restart [B633 · B569]
BOOT      DefaultModulesFileManager enumerates modules/ → filter by manifest runtimeProfile (not filename;
  │       AX modules silently skipped) → recursive-DFS dependency resolve → load prebuilt RegistryDatabase  [B630]
RESOLVE   NModule per profile → BModule aggregation (first-part-wins header)                           [B629]
LOAD      BTypeSpec moduleName:typeName → registry NTypeInfo (no Class) ; getResolvedType →
  │       NModule.loadClass → Class.forName(cn,true,ModuleClassLoader) → NType self-registers (String→Type) [B631 · B617]
RUNTIME   palette exposed lazily (BModulePaletteNode, ungated) ; <permissions> → base grant + 2 tracks
          (java-permissions per-CodeSource under SecurityManager ; niagara-groups under GrantAll default)   [B634 · B635]
```

### A.3 The five load-bearing invariants a builder must respect
1. **The manifest decides everything, not the filename.** `runtimeProfile`, `moduleName`, dependency versions all come from `module.xml`; a jar missing `runtimeProfile` is silently ignored ([B630]). `partName` = the on-disk filename at install ([B633]).
2. **Profile split is enforced at boot**, not just at compile: a `-wb` part is invisible to a `-rp:rt,se` station ([B630] §630.5). Station-needed logic in a wb part is unreachable headless.
3. **A module jar is a signed jar**; acceptance depends on the TARGET's trust anchor + `moduleVerificationMode` ([B392]/[B633]), which can be `low` ([B398]).
4. **`<type>` is authored, not auto-generated** ([B631] §14-corrects B12): a class absent from `<types>` is dead bytecode — neither slot-processed nor registered.
5. **Install is disruptive and irreversible-by-default** ([B633]): all stations stop, jars overwrite in place with no backup/rollback.

---

## Part B — the `com.angeles.chihuahua` case study

Measured against Part A, from the real jars ([B632] anatomy). Chihuahua is a genuine tri-profile module (rt/ux/wb, `vendor=ANGELES`, `moduleName=chihuahua`) — the fundamentals are RIGHT. The deviations below are refinements, ranked.

### B.1 Deviation table (each with evidence + fix)

| # | Sev | Deviation ([CERT] real jar) | Reference | Fix |
|---|---|---|---|---|
| 1 | MED | **`chihuahua-rt` AND `chihuahua-ux` declare `<niagara-permission-groups type="all"/>+workbench+station`** | Tridium `control-rt` declares NO `<permissions>` → minimal base grant ([B635]) | Remove `<permissions>` (fall to base grant) or scope to the one group actually needed. Under the default `GrantAllPermissionGroupStore` this is granted anyway ([B635]) so it is not an active escalation — but it is maximal-intent where minimal was correct, and becomes real over-privilege the day a restrictive store is deployed. It requests NO `<java-permissions>` (good: `checkTpk` stays false). |
| 2 | MED | **Builds against `baja 4.13`** (all three parts' `<dependency vendorVersion="4.13">`) while the station is **4.14** | deps checked at resolve; 4.14 accepts a 4.13 floor ([B630] §630.4) | Works today (backward-compatible floor). Bump the dependency floor to 4.14 when you no longer target 4.13 stations, so you can use 4.14 API and signal the real minimum. This is the slot-freeze thread of [B176]. **§14 REFRAMED by [B638]**: this is NOT an oversight — the baja floor = the SDK the build compiles against (`niagara_home`=iSMA 4.13.2), a DELIBERATE portability choice (a 4.13-compiled module loads on 4.13/4.14/4.15). Keep it unless you need 4.14-only API; bumping DROPS 4.13 stations. |
| 3 | MED | **Profile build drift**: `buildMillis` differ across parts (rt `1786913603356` newest; wb `1781426268252`; ux `1782079936843`) though all tagged `vendorVersion 1.3` | one coherent version per release | Rebuild + re-sign ALL three parts together per release. Divergent build times under one version number make field diagnosis ambiguous (which 1.3 is on the station?). |
| 4 | LOW | **`chihuahua-rt` ships NO `module.palette`** despite exporting reusable components (`BPlanta`, `BChiCarcamo`, `BChiUp`, `BChiDatalogger`, `BChiCarcamoMonitor`, `BChiDashboardService`) | rt modules that export components ship a palette ([B632]/[B634]) | Add a `module.palette` with pre-wired instances → drag-and-drop instead of hand-building. Cheap ([B634]: ungated, no code). |
| 5 | LOW | **`chihuahua-ux` carries 53 `.class`** (vs a typical thin ux like `ace-ux`=4) alongside 67 web assets | ux = browser-side; heavy logic belongs in rt ([B630]) | Audit the 53 ux classes: any pure business/data logic (not a `BChiServlet`/bajaux agent) belongs in `-rt` so a headless station can run it and the browser bundle stays lean. Not a bug — a layering review. |
| 6 | INFO | **Signed with alias `NIAGARA4`** (same SF/RSA name as Tridium jars) | modules signed with the developer's own cert | Whether this is a real trusted cert or a self-named `NIAGARA4` dev alias is a signing-pki question ([B392]/[B519], REMIT). It loads regardless because the live station runs `moduleVerificationMode=low` ([B398]). Verify the actual signing cert identity/chain before relying on it in a hardened deployment. |

### B.2 What chihuahua does RIGHT (do not "fix")
- Correct tri-profile split (rt components / ux dashboard+servlet / wb `BBatchLinkEditor` view) — the profile model of A.2 respected.
- `wb` agent properly declared: `<agent requiredPermissions="rwi"><on type="baja:Component"/>` ([B632]) — correct agent-on wiring ([B12] §12.3.4).
- `<type>` entries present for every exported `B*` component ([B632]) — no dead bytecode (invariant A.3.4 satisfied).
- Dependencies declared with `vendorVersion` per profile — resolve-checkable ([B630]).

### B.3 Priority order for the operator
1. **#1 permissions** — cheap, correctness/hygiene, and the only security-shaped item. Drop the `type="all"` groups.
2. **#3 build-together** — process fix; removes version ambiguity.
3. **#5 ux class audit** + **#4 add palette** — maintainability/usability.
4. **#2 baja floor** + **#6 cert identity** — deployment-posture, do when targeting hardened/4.14-only stations.

---

## Part C — the generalizable "build a module right" checklist (portable beyond chihuahua)
1. One `module.xml` per profile; set `runtimeProfile`, `moduleName`, `preferredSymbol`, real `vendor`/`vendorVersion` (invariant A.3.1).
2. List EVERY exported `B*` type in `<types>` (A.3.4); put API in `javax.*`/your-namespace, impl in an internal package ([B632]).
3. Split by profile honestly: browser JS/agents → `-ux`; Swing views → `-wb`; runtime logic/components → `-rt` (A.3.2, deviation #5).
4. Declare only the `<permissions>` you need; prefer none (base grant) for ordinary component/service/dashboard modules (deviation #1, [B635]).
5. Ship a `module.palette` if you export reusable components (deviation #4).
6. Build + sign + version all profiles together (deviation #3); keep the `<dependency>` floor at the real minimum station version (deviation #2).
7. Remember install is disruptive: back up `modules/` before a field upgrade ([B633]); confirm the target's `moduleVerificationMode`/trust anchor will accept your signature ([B633]/[B392]).

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Reference skeleton/lifecycle consolidates MA1-MA7 accurately | [INFER]/synthesis | [B629]-[B635] | ✅ cross-ref |
| 2 | chihuahua-rt+ux declare niagara-permission-groups all/workbench/station; no java-permissions | [CERT] | [B632] chihuahua jar module.xml | ✅ artifact |
| 3 | all three parts depend on baja 4.13; station is 4.14 | [CERT] | [B632] chihuahua-{rt,wb,ux} module.xml | ✅ artifact |
| 4 | buildMillis differ across parts under one vendorVersion 1.3 | [CERT] | [B632] chihuahua jar manifests | ✅ artifact |
| 5 | chihuahua-rt has no module.palette; ux has 53 .class + 67 web assets | [CERT] | [B632] jar taxonomy | ✅ artifact |
| 6 | default permission-group store grants all; verificationMode=low live softens the sig gate | [CERT]/[CERT-live] | [B635] (GrantAll) · [B398] | ✅ cross-ref |
| 7 | chihuahua does the profile split + agent + <types> correctly | [CERT] | [B632] | ✅ artifact |

**Tally**: [CERT] ×5 · [CERT-live] ×1 · [INFER]/synthesis ×1 · DESIGN/synthesis block (ratio read as consolidation, not exhaustion). Every chihuahua claim is a real-jar read ([B632]); every skeleton claim back-references a verified evidence block.

## Connections

- Consolidates **[B629]–[B635]** (the module-anatomy focus). Case study grounded in **[B632]** (real chihuahua jars) + **[B163]–[B177]** (chihuahua corpus).
- Remittances: **[B12]** (build/doc-side), **[B617]** (classloader), **[B398]/[B18]** (live security posture), **[B569]** (fleet install), **[B392]** (signing).
- **FOCUS module-anatomy CLOSED** 8/8 at this block.

## Gaps uncovered

- None investigable on disk. Optional future (NOT opened here): live §12 validation of the chihuahua improvements on the running station (API2) — deferrable, not required for the reference/case-study deliverable. The `.db` RegistryDatabase binary format and `nre`-internal `NiagaraPolicyUtil` are deeper infra out of this focus's scope (REMIT). Focus STOPS: investigable=0 after this synthesis.
