# Block 578 — `ApplicationTemplateInstaller`: the station-side install/upgrade engine — a module-compatibility gate, then a component-tree swap under a `ReplacingContext` (auto-start off) that clears the old app, installs the new, and preserves handles so relations survive; upgrade saves config first

**Session**: 2026-08-28
**Focus**: `template` (gap T2 — the station-side install path [Block 200] named out-of-scope)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `ApplicationTemplateInstaller` + `BApplicationInstallSpecs`; the
compatibility gate and the ReplacingContext transfer token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-rt/vineflower/com/tridium/template/application/{ApplicationTemplateInstaller,
  ApplicationTemplateUtil,BApplicationInstallSpecs}.java`.

**Scope**: how an application `.ntpl` is applied to a live station. [Block 200 §200.4] gave the make/install/
upgrade OVERVIEW; its header excluded `ApplicationTemplateInstaller`. T2 opens it. Consumes what T1 ([Block 577])
produces. Does NOT re-open the `.ntpl` structure ([Block 200 §200.1]) or `UpgradeUtil` (T5) — connects.

---

## 578.1 The request and the installer shape [CERT]

`BApplicationInstallSpecs extends BStruct` `[CERT] :32` is the install request: `upgrade` (bool),
`checkModules` (bool), `fileOrd` (BOrd → the .ntpl), `toBeRemoved` (BOrdList) `[CERT] :15-35`. The engine
`ApplicationTemplateInstaller implements AutoCloseable` `[CERT] :56` holds `applicationFile` (`BNtplFile`
resolved from the ord), `componentsToBeRemoved` (a `NameTree` built from the `toBeRemoved` ords via
`ApplicationTemplateUtil.makeNameTree`), and a `ProgressTracker` `[CERT] :57-67`.

## 578.2 The compatibility gate [CERT]

Before installing, `checkStationForCompatibleModules(target)` `[CERT] :75-`… calls
`applicationFile.checkRemoteModuleDependencies(target)` which returns THREE maps `[CERT]`:
`missingModules`, `mismatchedModules` (version mismatch), `missingPxModules`. The rule `[CERT]`:
```java
boolean incompatible = missingModules.size() > 0;   // missing module = HARD fail
// mismatchedModules / missingPxModules → progress WARNINGS, not blocking
if (incompatible) { ... }
```
So a template install is **gated on the target having every required module** (a MISSING module aborts);
version MISMATCH and missing PX modules are surfaced as warnings but do not block. The dependency list comes from
the manifest (T1 `addDependenciesToManifest`).

## 578.3 The transfer: a `ReplacingContext` swap with auto-start off [CERT]

`basicInstallToStation(target, keepInTarget, componentsToBeRemoved)` `[CERT] :163-183` is the core:
```java
Context cx = new ReplacingContext(Context.NULL, BFacets.make("niagaraAutoStart", BBoolean.FALSE));  // :169
this.clearApplicationComponents(target, keepInTarget, cx);                    // :171 remove old app (keep keepInTarget)
...
this.installApplicationComponents(source, componentsToBeRemoved, target, keepInTarget, cx);  // :183 install new
```
Two decisions matter:
- **`ReplacingContext`** (`com.tridium.sys.transfer.ReplacingContext`) — the transfer runs in "replace" mode, so
  existing components are swapped rather than duplicated.
- **`niagaraAutoStart = FALSE`** — components do NOT start during the swap (avoids half-installed logic running).

`installApplicationComponents` `[CERT] :191-` transfers annotations and preserves HANDLES
(`transferAnnotations(newSource, target, handleMap, needHandles)` `[CERT] :209`, `findApplicationRoots`
`[CERT] :212`, and a ReplacingContext branch `[CERT] :226-227`). Handle preservation is why LINKS and RELATIONS
into the swapped subtree survive the template replacement — the new components keep the old handles.

## 578.4 Install vs Upgrade [CERT]

- `install(Context)` → `installToStation` `[CERT] :111,119-131`: fresh apply; uses the passed
  `componentsToBeRemoved`; ends with `application.install.passwordNote` `[CERT] :131` — a reminder that
  passwords were stripped from the template ([Block 200 §200.3]) and must be re-entered.
- `upgrade(Context)` → `upgradeStation` `[CERT] :115,135-148`: first `application.upgrade.saveConfig`
  `[CERT] :140` (preserve the target's current config), then computes `componentsToBeRemoved` as the
  **missing optional components** (`getMissingOptionalComponents(target, manifest.optional)` `[CERT] :145`) —
  optionals present in the old but absent in the new template are removed — then the same `basicInstallToStation`.

So install is a clean apply-with-keep-list; upgrade is a config-preserving diff that also reconciles optional
components against the new manifest's `optional` list.

## 578.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BApplicationInstallSpecs (BStruct) = upgrade/checkModules/fileOrd/toBeRemoved; installer holds BNtplFile + NameTree + ProgressTracker | [CERT] | BApplicationInstallSpecs.java:15-35; ApplicationTemplateInstaller.java:56-67 | token-checked ✓ |
| 2 | checkRemoteModuleDependencies → missing/mismatched/missingPx maps; missingModules>0 = hard incompatible | [CERT] | ApplicationTemplateInstaller.java:75-100 | token-checked ✓ |
| 3 | basicInstallToStation runs under ReplacingContext with niagaraAutoStart=FALSE; clear then install | [CERT] | :163-183 | token-checked ✓ |
| 4 | installApplicationComponents transfers annotations + preserves handles (transferAnnotations/handleMap) so relations survive | [CERT] | :191-227 | token-checked ✓ |
| 5 | install ends with passwordNote (stripped passwords); upgrade saves config first + removes missing-optional components | [CERT] | :131,140-145 | token-checked ✓ |

**Marker tally**: [CERT] ×5 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 5 rows token-verified
inline.

## Connections

- **[Block 577]** (T1) — the API that CREATES the .ntpl this installer consumes.
- **[Block 200 §200.3]** — password stripping (the passwordNote reminder); §200.4 make/install/upgrade overview.
- **T5** (this focus) — `UpgradeUtil` / the `com.tridium.sys.transfer` layer; `ReplacingContext` here is the same
  transfer machinery `Mark`/`DeployToComp` uses.
- **[Block 573]** (PV7) — provisioning's deploy step drives this installer across a fleet.

## Open gaps (this block)

- `clearApplicationComponents`/`findApplicationRoots` exact root-detection rules and the handle-map algorithm are
  named, partially traced — deepen with T5 (transfer internals). Focus continues at T5 (UpgradeUtil transfer).
