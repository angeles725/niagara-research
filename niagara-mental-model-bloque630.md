# Niagara N4 — module-anatomy (MA2): the module BOOT scan — `modules/` is enumerated by manifest `runtimeProfile` (not filename), dependency-resolved recursively, and loaded from a PREBUILT registry binary (`ClassScanner` runs only at rebuild)

**Focus**: module-anatomy · **Gap**: MA2 (module boot scan) · **Session**: 2026-08-29 · **Block**: B630
**Sources** (all `[CERT]` decompiled Java, vineflower `decompiled/` tree unless noted):
- `organized/baja/baja/decompiled/com/tridium/sys/module/DefaultModulesFileManager.java`
- `organized/baja/baja/decompiled/com/tridium/sys/module/ModuleManager.java`
- `organized/baja/baja/decompiled/com/tridium/sys/registry/NRegistry.java` · `.../registry/Builder.java` · `.../registry/ClassScanner.java`
- `organized/baja/baja/decompiled/com/tridium/sys/Nre.java`
- `organized/baja/baja/decompiled/com/tridium/sys/module/NModule.java` (continues [B629])

**Scope**: the runtime boot path that turns the `modules/` directory into loaded `NModule`/`BModule` objects (the callers of [B629]'s `readXml`/`init`). Classloader delegation once loaded = [B617] (REMIT). Signing verdict during load = [B489]/[B521] (REMIT).

---

## 630.1 The four stages of module boot

1. **Enumerate** `$NIAGARA_HOME/modules/` and filter each jar by its manifest (`DefaultModulesFileManager`).
2. **Load the registry** — a PREBUILT binary database, not a live class scan (`NRegistry.db()`).
3. **Resolve + load** module parts recursively in dependency order (`ModuleManager.loadDependency`/`resolve`).
4. **init** each `NModule` into its `BModule` at `postInit` ([B629] §629.5 continued).

Two facts here overturn the naive mental model: **profile is read from the XML manifest, not the `-rt`/`-wb` filename**, and **the type registry is a cached binary, not rebuilt at every boot**.

---

## 630.2 Stage 1 — directory enumeration and the manifest filter

`[CERT]` `DefaultModulesFileManager.java:261` — the directory is `NiagaraFiles.getModulesPath()` (derived from the `niagara.home` property = `$NIAGARA_HOME/modules/`), enumerated with `listFiles()`:
```java
for (File file : NiagaraFiles.getModulesPath().listFiles()) { ... this.makeManagedFile(file, cacheManifest); }
```

Each jar's `META-INF/module.xml` is read and gated `[CERT]` `DefaultModulesFileManager.java:228-233`:
```java
if (manifest.getb("nre", true) && manifest.getb("installable", true)) {
    if (manifest.get("runtimeProfile", null) == null) {
        // line 230: "modules/%s ignored, missing runtimeProfile manifest attribute (is it an AX module?)"
        return notManaged(aFile);
    }
    ...new DefaultManagedModuleFile(aFile, "modules", manifest, ...);
}
```

**The profile is the manifest attribute, not the filename** `[CERT]` `DefaultModulesFileManager.java:350`:
```java
this.runtimeProfile = RuntimeProfile.valueOf((String)aManifest.get("runtimeProfile", null), null);
```
Consequences worth pinning:
- A jar whose manifest lacks `runtimeProfile` is **silently ignored** with an explicit "is it an AX module?" warning (`:230`). Legacy Niagara AX modules (single-part, no profile attribute) simply do not load on N4 — this is the concrete on-disk enforcement of the AX→N4 break [B12] §12.2.1 named from docs.
- Renaming `foo-wb.jar` to `foo-rt.jar` changes nothing; the manifest decides. The filename suffix is convention only.
- Modules are indexed by `(moduleName, RuntimeProfile)` in a `ModuleFileSet`, so `bajaui-rt.jar` and `bajaui-wb.jar` are two parts of one logical module — exactly the per-profile `NModule` aggregation of [B629] §629.5.

---

## 630.3 Stage 2 — the registry is a PREBUILT binary, not a boot-time class scan

The Type registry ([B4]/MA3) is loaded from a serialized database file, not rebuilt by scanning classes at each boot `[CERT]` `NRegistry.java:217-223`:
```java
RegistryDatabase db() {
    if (this.db == null) {
        this.db = new RegistryDatabase();
        InputStream in = Nre.bootEnv.isRemote() ? Nre.bootEnv.read(this.dbRemote())
                                                : new BufferedInputStream(new FileInputStream(this.dbFile()));
        this.db.read(in);   // deserialize the prebuilt registry
    }
    return this.db;
}
```

A rebuild happens ONLY when the cache is stale `[CERT]` `NRegistry.java:234,280` (`isRegistryUpToDate()` → `rebuild()`), and staleness is checksummed **over supported-profile jars only** (§630.5). `ClassScanner` is invoked **exclusively inside `Builder` (registry rebuild)** — grep for `new ClassScanner`/`.scan(` finds callers only under `.../sys/registry/Builder.java`; there is no boot-path caller. `ClassScanner.scan(InputStream)` reads only the constant pool + `modifiers`/`superClass`/`interfaces[]` and checks for the literal `"loadType"` string (`hasLoadType`); it does NOT parse methods/fields/annotations.

**Verdict**: normal station/Workbench boot does zero bytecode scanning — it deserializes `registry.db`. Bytecode scanning is a build/rebuild-time cost. (Vineflower mangles the `Builder` call site to `n()`/`scannernentry`; the CFR tree shows it clean — a decompiler artifact, not ambiguity about which class calls it.)

---

## 630.4 Stage 3 — recursive depth-first dependency resolution

Load order is produced by **recursive resolve with a visited-set**, not a precomputed topological sort `[CERT]` `ModuleManager.java:137-139`:
```java
public synchronized NModule loadDependency(String modulePartName) {
    HashMap<String,NModule> pendingAdd = new HashMap<>();
    NModule result = this.loadDependency(modulePartName, pendingAdd);   // recurse
    pendingAdd.values().forEach(this::add);                             // commit ALL at once
    return result;
}
```
`doLoadByModulePartName` (`:277`) opens the jar, puts it in `pendingAdd`, then `resolve(m, pendingAdd)` walks `m.depends[]`; each unresolved dependency is either reused from `pendingAdd` (cycle break) or recursively loaded. `resolve` also enforces `checkBajaVersion`/`checkVendor` against each `<dependency>`'s declared versions. Because a dependency is fully resolved before control returns to its dependent, ordering is dependency-before-dependent; and because the whole `pendingAdd` set is committed with one `forEach(this::add)` at the top-level boundary (`:139`, `:190`), no half-resolved module is ever globally visible — the load is atomic per top-level request.

`makeModule(File)` is the `NModule` factory ([B629] link): `new NModule()` → set `moduleFile` → parse `META-INF/module.xml` → `m.readXml(manifest)` → `m.signed = entry.getCertificates() != null`. On the hot-load path (`postInit` already true) it immediately `m.init(() -> this.bmodule(m.moduleName))` + `checkLicensed()`.

---

## 630.5 Stage 5 — profile filtering is a JVM-startup gate

Which profiles load at all is fixed at JVM startup by the `-rp:` option `[CERT]` `Nre.java:265,468,1374`:
```java
// default = all five profiles (:1374)
supportedProfiles = {rt, ux, wb, se, doc};
// -rp:<csv> override (:468) — rt is ALWAYS added first
accum.add(RuntimeProfile.rt); for (v : csv.split(",")) accum.add(RuntimeProfile.valueOf(v));
```
`Nre.supportsProfile(p)` (`:1282`) tests membership. The registry rebuild checksums only supported-profile jars (`NRegistry.java:253-256`), so the `RegistryDatabase` contains only those profiles, and `ModuleManager.loadModuleParts` (which queries `Sys.getRegistry().getModules(name)`) can only ever load them.

The two-stage gate:
1. **JVM startup** — a station daemon launches with `-rp:rt,se`; Workbench adds `wb` (and `doc`). `rt` is always present.
2. **Registry** — only supported-profile parts are in the db → `getModules(name)` returns only them → a `runtimeProfile="wb"` module is simply invisible to a station daemon. This is the runtime realization of [B12] §12.1.6's profile permission matrix — not a compile-time rule but a boot-time membership filter.

---

## 630.6 What this means for building/distributing a module

- **The manifest `runtimeProfile` attribute is load-bearing, literally.** Ship a jar without it (or an AX-era manifest) and the station silently ignores it — no type error, just an absent module. A build that omits `moduleManifest { runtimeProfile.set(...) }` produces a jar that installs but never loads.
- **Registry staleness is the boot cost, not class scanning.** Dropping a new/updated module jar into `modules/` makes `isRegistryUpToDate()` false → one rebuild (the only time `ClassScanner` runs) → subsequent boots are cache reads. This is why a first boot after a module install is slower.
- **A `wb`-only feature is unreachable from a headless station** by construction (profile not in `-rp:rt,se`). The rt/wb split ([B12] §12.1.6) is enforced at boot membership, so a module that puts station-needed logic in a `-wb` part is broken on a JACE/supervisor daemon — a concrete anti-pattern to check chihuahua against at MA8.
- **Dependency versions are checked at resolve** (`checkBajaVersion`/`checkVendor`), so a `<dependency>` with a too-high `vendorVersion` fails the dependent's load, not just a runtime `NoClassDefFound`.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | modules dir = `NiagaraFiles.getModulesPath()` (`$NIAGARA_HOME/modules/`), enumerated via listFiles | [CERT] | DefaultModulesFileManager.java:261 | ✅ read |
| 2 | jar gated on `nre=true && installable=true`; missing `runtimeProfile` → ignored ("is it an AX module?") | [CERT] | DefaultModulesFileManager.java:228-230 | ✅ read verbatim |
| 3 | profile comes from manifest attr `RuntimeProfile.valueOf(manifest.get("runtimeProfile"))`, NOT filename | [CERT] | DefaultModulesFileManager.java:350 | ✅ read verbatim |
| 4 | registry loaded from prebuilt binary via FileInputStream(dbFile()) + db.read(in) | [CERT] | NRegistry.java:217-223 | ✅ read verbatim |
| 5 | rebuild gated on isRegistryUpToDate; ClassScanner called only from Builder (rebuild), no boot caller | [CERT] | NRegistry.java:234,280 + rg ClassScanner→Builder.java only | ✅ read + grep |
| 6 | dependency resolution recursive with pendingAdd visited-set, committed atomically via forEach(add) | [CERT] | ModuleManager.java:137-139,277 | ✅ read verbatim |
| 7 | makeModule: new NModule → readXml(manifest) → signed check → init on postInit path | [CERT] | ModuleManager.java (makeModule) + NModule.java:100-106 | ✅ read (B629) |
| 8 | supportedProfiles set by `-rp:` (default all 5; rt always added); supportsProfile membership test | [CERT] | Nre.java:265,468,1282,1374 | ✅ read verbatim |
| 9 | wb module invisible to station daemon (rt,se) because registry holds only supported profiles | [INFER] from #5+#8 | NRegistry:253-256 + Nre:1282 | ✅ derived |

**Tally**: [CERT] ×8 · [INFER] ×1 · ratio 0.13 (EVIDENCE block — investigable evidence intact). All 8 [CERT] token-checked verbatim this iteration (own grep pass over the vineflower tree).

## Connections

- **[B629]** — this block's `makeModule`/`readXml`/`init` are the callers of B629's NModule/BModule dual. **[B12]** §12.1.6/§12.2.1 — profile matrix + AX break, now shown enforced at boot.
- **[B617]** — `ModuleClassLoader` runs AFTER this scan loads the NModule; REMIT.
- **[B489]/[B521]** — `m.signed`/`checkLicensed` at makeModule = signing/license gate; REMIT.
- Forward: **MA3/B631** opens `ClassScanner`/`Builder`/`Registry.getType` (the type-registration pipeline this block deferred); **MA7** opens `readPermissions`.

## Gaps uncovered

- None new. Confirmed MA3's sources (Builder/ClassScanner/Registry) are correctly the next read — the registry-build path is exactly what MA3 must open. The `.db` binary format itself (RegistryDatabase.read/write) is a candidate sub-gap if MA3 needs it, noted for MA3 not as a new backlog row.
