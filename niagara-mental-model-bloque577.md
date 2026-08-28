# Block 577 — The programmatic template API: `NiagaraTemplate` (AutoCloseable façade) over a `TemplateSource` strategy hierarchy — a type-dispatched factory for CREATE vs OPEN, a fail-loud base, and a 5-step `.ntpl` save pipeline (opens what B200 named out-of-scope)

**Session**: 2026-08-28
**Focus**: `template` (gap T1 — the `api/` + `api/impl/` layer; the programmatic create/read/save pipeline
[Block 200] explicitly excluded)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the api/impl classes; factory dispatch + save pipeline
token-verified inline. No docSource (decompiled only); the API is code-only (no official docs, [Block 200]-audit).
**Primary sources** `[CERT]`:
- `organized/template/template-rt/vineflower/com/tridium/template/api/{NiagaraTemplate,TemplateSourceType,
  TemplateType,TemplateScope}.java` + `api/impl/{TemplateSource,TemplateSourceFactory,NewTemplateSource,
  NewApplicationTemplateSource,NewStationTemplateSource,NewDeviceTemplateSource,NewComponentTemplateSource,
  DeployedTemplateSource,FileTemplateSource,InstalledApplicationTemplateSource,CombinedTemplateSource,
  TemplateSourceWithBase}.java` (17 in api/impl).

**Scope**: the Java API a module uses to author/read a template programmatically — the parallel to the Workbench
"Make Template" UI. [Block 200] is the breadth block; its header names `api/impl/*TemplateSource` as out-of-scope
"future deepening". T1 opens it. Does NOT re-derive the `.ntpl`/bog/manifest structure ([Block 200 §200.1]) or
the binding contract ([Block 200 §200.3]) — REMITTANCE.

---

## 577.1 `NiagaraTemplate` — an AutoCloseable façade over one source [CERT]

`public final class NiagaraTemplate implements AutoCloseable` `[CERT] :27` wraps a single
`TemplateSource source` `[CERT] :28` (private ctor). Its static factories map INTENT to a source `[CERT] :35-71`:
- `createFrom(BComponent)` → component/device template;
- `createStationTemplateFrom(BStation[, homeDir[, protectedHomeDir]])` → station template
  (`TemplateSourceFactory.create(station, false)`);
- `createApplicationFrom(BStation, …)` → application template (`create(station, true)`).

`AutoCloseable` matters: a `TemplateSource` holds a staged in-memory file space ([Block 200] `BMemoryFileSpace`),
so the façade is meant for try-with-resources (`close()` releases the staging).

## 577.2 `TemplateSource` — a fail-loud strategy base [CERT]

`public abstract class TemplateSource implements AutoCloseable` `[CERT] :16`. Every operation —
`save(OutputStream)`, `save(FilePath)`, `save()`, `getSourceType()`, `getTemplateType()`, `getTemplateScope()`,
`getTitle()` — **throws `UnsupportedOperationException("… not yet implemented!")` in the base** `[CERT] :32-57`.
Only `setUseMinorVersionOnDeployment` is concrete. So the base is a deliberately fail-loud skeleton: a concrete
subclass implements EXACTLY the operations it supports, and an unsupported operation throws loudly rather than
returning a wrong default. This is the strategy pattern with a defensive base.

## 577.3 The factory: type-dispatched CREATE, and a separate OPEN [CERT]

`TemplateSourceFactory` has two dispatch entry points `[CERT]`:
- **CREATE** (snapshot a live tree → new template) `[CERT] :37-53`:
```java
if (sourceComponent instanceof BStation)
   result = createAsApplication ? new NewApplicationTemplateSource(...) : new NewStationTemplateSource(...);
else if (sourceComponent instanceof BDevice)
   result = new NewDeviceTemplateSource(...);
else
   result = new NewComponentTemplateSource(...);
```
Concrete `New*` source picked by the source's TYPE (`BStation`/`BDevice`/other) × the `createAsApplication` flag.
- **OPEN** (read/inspect a deployed template) `[CERT] :56-63`: `InstalledApplicationTemplateSource.make(station)`
  for a deployed station; if a file source is also present, wraps both in a **`CombinedTemplateSource`**
  (deployed instance + its origin file). So reading a deployed template can compare live-vs-file.

Two families along two axes: SOURCE = {New (from live), File (from disk), Deployed/InstalledApplication (from a
live instance), Combined} × the template TYPE enums (`TemplateType`, `TemplateSourceType`, `TemplateScope`, all
`enum` `[CERT]`). The `TemplateSourceWith{Base,Config,Value}` classes are decorators that layer a base template /
config overrides / values onto a source.

## 577.4 The `.ntpl` save pipeline — 5 steps [CERT]

`NewTemplateSource extends TemplateSourceWithBase` `[CERT] :58`; `save(OutputStream)` `[CERT] :91-100` is the
real authoring pipeline:
```java
TemplateManifest manifest = new TemplateManifest();
this.collectTemplateInfo(manifest, config);              // 1. metadata (vendor/version/type/bindings)
Set<TemplateFileSpec> filesToStore = this.addFilesToManifest(manifest);  // 2. bog + px + images + data files
this.addDependenciesToManifest(filesToStore, manifest);  // 3. module dependencies
this.buildTemplateFile(out, manifest, filesToStore);     // 4→5. ManifestXMLWriter + ZipOutputStream → .ntpl
```
So authoring a template programmatically is: build the `TemplateManifest`, enumerate the files (the bog snapshot
+ referenced PX/images/data), record module dependencies, then write the ZIP (manifest XML + entries) via
`ZipOutputStream`. This is the code path behind the Workbench "Make Template" command ([Block 200 §200.6]) and
behind provisioning's template creation ([Block 573]) — both go through this API.

## 577.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | NiagaraTemplate = AutoCloseable façade over one TemplateSource; factories createFrom/createStationTemplateFrom/createApplicationFrom | [CERT] | NiagaraTemplate.java:27-71 | token-checked ✓ |
| 2 | TemplateSource abstract base: all ops throw UnsupportedOperationException (fail-loud); subclass overrides what it supports | [CERT] | TemplateSource.java:16-57 | token-checked ✓ |
| 3 | Factory CREATE dispatch by type × createAsApplication → NewApplication/NewStation/NewDevice/NewComponent | [CERT] | TemplateSourceFactory.java:37-53 | token-checked ✓ |
| 4 | Factory OPEN → InstalledApplicationTemplateSource + CombinedTemplateSource (deployed + file) | [CERT] | :56-63 | token-checked ✓ |
| 5 | TemplateType/TemplateSourceType/TemplateScope are enums; TemplateSourceWith{Base,Config,Value} are decorators | [CERT] | api/*.java:3-5; api/impl/TemplateSourceWith*.java | token-checked ✓ |
| 6 | NewTemplateSource.save = 5-step pipeline (manifest → collectInfo → addFiles → addDependencies → buildTemplateFile/zip) | [CERT] | NewTemplateSource.java:91-100 | token-checked ✓ |

**Marker tally**: [CERT] ×6 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 6 of 6 rows token-verified
inline. api/impl measured at 17 classes (matches audit).

## Connections

- **[Block 200]** — the breadth block; T1 opens the `api/impl/*TemplateSource` layer its header named out-of-scope.
- **[Block 200 §200.1]** — `.ntpl` = zip+bog+manifest (REMITTANCE); this is the API that WRITES that structure.
- **[Block 573]** (PV7) — provisioning creates templates through this same API.
- **T2/T5** (this focus) — the READ/INSTALL side (`ApplicationTemplateInstaller`) and the transfer internals
  (`UpgradeUtil`) consume what this API produces.

## Open gaps (this block)

- `collectTemplateInfo`/`addFilesToManifest` field-level detail (exactly which slots/bindings become manifest
  settings) folds into T3 (manifest grammar). `TemplateSourceWithConfig`/`WithValue` override semantics are named,
  low value. Focus continues at T2 (ApplicationTemplateInstaller).
