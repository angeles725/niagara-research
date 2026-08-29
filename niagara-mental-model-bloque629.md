# Niagara N4 — module-anatomy (MA1): the module.xml manifest is parsed TWICE by two independent readers — install-side `ModuleManifest`/`BModulePart` and runtime-side `NModule` — with no converter between them

**Focus**: module-anatomy · **Gap**: MA1 (manifest reader) · **Session**: 2026-08-29 · **Block**: B629
**Sources** (all `[CERT]` decompiled Java, vineflower tree):
- `organized/platform/platform-rt/decompiled/com/tridium/install/installable/ModuleManifest.java`
- `organized/platform/platform-rt/decompiled/com/tridium/install/part/BModulePart.java`
- `organized/baja/baja/decompiled/com/tridium/sys/module/NModule.java`
- `organized/baja/baja/decompiled/javax/baja/sys/BModule.java`

**Scope**: the READER side of a module manifest — the classes that consume `META-INF/module.xml`. The manifest SCHEMA (element/attribute list) is REMITTANCE: [B12] §12.1.4-5 (devguide) + [B76] (verbatim control-rt/driver-rt manifests). This block opens what B12 never did: the Java that PARSES it, and the fact that N4 does it twice.

---

## 629.1 The headline: one XML, two independent parsers

`META-INF/module.xml` inside every module jar is read by **two entirely separate code paths** that never share a parse:

| | Install side | Runtime side |
|---|---|---|
| Package | `com.tridium.install.*` (module `platform-rt`) | `com.tridium.sys.module.*` (module `baja`) |
| Reader | `ModuleManifest` → wrapped by `BModulePart` | `NModule.readXml(XElem)` |
| When | inspecting / transferring / installing a jar (daemon, software manager) | a running station/Workbench LOADING the module into the registry |
| Representation | a Baja-serializable `BPart` component (transmittable over a daemon session) | native Java fields on an `NModule`, aggregated by `BModule` |
| Dependency type | `com.tridium.install.BDependency` | `com.tridium.sys.module.Dependency` |

There is **no converter class** from the install representation to the runtime one. Each side re-parses the same XML into its own model. `ModuleManifest` is referenced ONLY inside `platform-rt` (grep: zero hits in `organized/baja/`); `NModule` never imports it. This is the load-bearing architectural fact of MA1 — the "manifest reader" is not one thing.

---

## 629.2 Install side — `ModuleManifest`: the streaming/DOM parser

`[CERT]` `ModuleManifest.java:55` — `private ModuleManifest(XElem manifestDom)` iterates the manifest DOM; a second constructor takes an `XParser` for streaming from an `InputStream`; factory `make(...)` overloads at ~`:183-198` accept either an `XElem` or an `InputStream`, returning a `corrupt`-status manifest on parse failure rather than throwing.

Root `<module>` attributes are copied via `loadManifestHeader` (`this.moduleElement = element.copy()`) and exposed by getters: `getModuleName()`/`getModulePartName()` (name + moduleName-fallback), `getVersion()` (packs vendor + vendorVersion into a `BVersion`), `getPreferredSymbol()`, `getRuntimeProfile()` (→ `RuntimeProfile` enum), `getModuleContent()`, build/archive metadata, `isInstallable()`, `isNreModule()`. Note `bajaVersion` is NOT read here — only the runtime `NModule` reads it (§629.4).

The child-element switch `[CERT]` `ModuleManifest.java:60-102` — each branch parses one element into a typed map:

```java
case "dirs":         moduleContentByPath.put(dir.get("name"), BModuleContent.make(dir.get("install","doc")));
case "dependencies": dependencyByName.put(dep.get("name"), BDependency.make(dep));
case "moduleParts":  relatedModuleParts.put(RuntimeProfile.valueOf(e.get("runtimeProfile")), e.get("name"));
case "types":        Collections.addAll(this.types, manifestElem.elems("type"));   // raw XElem list, unparsed
case "installation": noRunningStation + nested <dependencies> + <exclusions>
default:             unknownElements.add(manifestElem);   // <lexicons>, <defs>, <permissions> land HERE
```

Three facts worth pinning:
1. `<types>` is stored as a **raw `List<XElem>`** — the install side does NOT resolve type names to classes; it only carries them for inventory/signing. Type→class resolution is exclusively a runtime concern (§629.4).
2. `<lexicons>`, `<defs>`, and **`<permissions>` are NOT parsed by `ModuleManifest`** — they fall into the `default` branch as `unknownElements`. Permissions are (re)parsed by `NModule.readPermissions()` at runtime (MA7). The install side is deliberately blind to them.
3. `moduleParts` maps `RuntimeProfile → part-name`: this is how one part's manifest names its SIBLING parts (a `-wb` part knowing its `-rt`/`-ux` siblings), keyed by profile.

---

## 629.3 Install side — `BModulePart`: the Baja-serializable wrapper

`[CERT]` `BModulePart.java:102-103` — `public class BModulePart extends BPart` (a Baja serializable component, so it can be sent over a daemon session and shown in the software manager). It wraps one jar/`.sjar` file:

`[CERT]` `BModulePart.java:130` non-slot fields: `private BIFile file` (the physical jar handle), `private boolean localInstance` (false on a remote-daemon proxy), `private ModuleManifest manifest` (lazily loaded).

The manifest is extracted from the jar with a case fallback `[CERT]` `BModulePart.java:380-385`:
```java
InputStream manifestStream = DaemonFileUtil.getZipStream(file, new FilePath("META-INF/module.xml"));
// falls back to lowercase "meta-inf/module.xml" if the upper-case entry is absent
```

`loadManifest(ModuleManifest)` (~`:644`) copies the parsed manifest into `@NiagaraProperties` slots — `moduleName`, `runtimeProfileString`, `version` (`BVersion`), `status` (`BModuleStatus`), `synthetic` (true for `.sjar`), `fileSize`, `exclusions` (`BVector<BDependency>`), `relatedModulePartNames` (BVector keyed by `RuntimeProfile.name()`), `codeSigners`/`signatureFailureCause` (signing = REMIT, [B489]/[B492]), etc. The point of `BModulePart` is that this data survives serialization and travels to a supervisor/daemon that may not have the module loaded — it is the module's identity card BEFORE installation.

---

## 629.4 Runtime side — `NModule.readXml`: the independent runtime parse

`[CERT]` `NModule.java:378-383` — the station's own reader, structurally unrelated to `ModuleManifest`:
```java
void readXml(XElem manifest) { readAttributes(manifest); readDependencies(manifest);
                               readTypes(manifest); readExts(manifest); readPermissions(manifest); }
```

Runtime fields `[CERT]` `NModule.java:74-89`: `moduleName`, `modulePartName`, `runtimeProfile`, `bajaVersion` (read HERE, `:387+`), `vendorVersion`, `vendor`, `preferredSymbol`, `Dependency[] depends`, `byte manifestSchemaVersion`, and crucially `Map<String,Object> types` (`:87`, initial capacity 63). `readTypes` (`:418`) stores each `<type name=.. class=..>` as `typeName → className(String)`; the class is resolved LAZILY on first `getType(typeName)`.

Type resolution is `moduleName:typeName` throughout `[CERT]` `NModule.java:226`:
```java
throw new TypeNotFoundException(this.moduleName + ":" + typeName);   // and :236,:244 (class-load failures)
```
This is the on-disk proof of the `moduleName:typeName` ORD/type addressing the whole framework uses — it is literally string-concatenated here at the module boundary. (Full type-registration pipeline = MA3/B630.)

`NModule` does NOT expose `getManifest()` or a public `getDependencies()`; it holds no `ModuleManifest` reference at all. It is a self-contained runtime model.

---

## 629.5 `BModule`: one facade aggregating N `NModule`s, one per profile

The public `javax.baja.sys.BModule` is NOT one-jar-one-object — it aggregates the profile parts of a logical module:

`[CERT]` `BModule.java:65` — `private final Map<RuntimeProfile,NModule> nModulesByProfile = new TreeMap<>();`

`NModule.init(Supplier<BModule>)` `[CERT]` `NModule.java:100-104` does not CREATE its BModule — a supplier provides it, then the part registers itself: `((BModule)this.bmodule).addModulePart(this)`.

`BModule.addModulePart` `[CERT]` `BModule.java:242-258` — **first part wins the header**: the first `NModule` to register sets the shared `moduleName`, `description`, `preferredSymbol`, and root ORD (`module://<name>`); any later part whose `moduleName` differs throws `IllegalArgumentException`. Then `nModulesByProfile.put(profile, modulePart)` and a `BZipSpace` is opened over that part's zip file (`:258`) — so each profile part keeps its OWN jar/zip space (relevant to MA4). All per-profile accessors delegate: `getBajaVersion(profile)`/`getVendor(profile)`/... → `nModulesByProfile.get(profile)....` (`:128,:132,...`), and `getType(typeName)` routes through `typeKeyByName.get(typeName)` to the profile-part that declared it (`:177`).

So the runtime module graph is: **`BModule` (logical module) → many `NModule` (one per `RuntimeProfile`) → each with its own zip space + types map**. Profile is the primary key at every level — the same axis the install side keys `relatedModuleParts` and `relatedModulePartNames` by.

---

## 629.6 What this means for building/distributing a module

- The manifest is authored ONCE (auto-generated `module-include.xml` merged into `META-INF/module.xml`, [B12] §12.1.4) but CONSUMED by two independent readers with different tolerances. A manifest that installs cleanly (`ModuleManifest` = `BModuleStatus.ok`) can still fail at runtime if `NModule.readXml` chokes on `<types>`/`<permissions>` the install side skipped — the two are not guaranteed consistent by a shared parser.
- `<permissions>` and `<defs>` are invisible to the installer — they matter only once the station loads the module. Distribution/signing tooling that validates the install-side manifest tells you nothing about them (MA7).
- Profile is the real unit. "A module" at runtime is a set of profile parts under one `BModule`; each part is a separate jar with its own zip space, dependencies, and types. The `-rt`/`-wb`/`-ux` split ([B12] §12.1.6 permission matrix) is not just a build convention — it is materialized as distinct `NModule` objects keyed by `RuntimeProfile` in `nModulesByProfile`.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | `ModuleManifest` parses the manifest via an XElem/XParser switch on child element names | [CERT] | ModuleManifest.java:55, :60-102 | ✅ read |
| 2 | `<dirs>/<dependencies>/<moduleParts>/<types>/<installation>` parsed; `<lexicons>/<defs>/<permissions>` fall to `unknownElements` (default branch) | [CERT] | ModuleManifest.java:60-102 | ✅ read verbatim |
| 3 | `<types>` stored as raw `List<XElem>` (install side does not resolve type→class) | [CERT] | ModuleManifest.java:80-82 | ✅ read |
| 4 | `ModuleManifest` referenced only in platform-rt; zero hits in organized/baja | [CERT] | rg ModuleManifest organized/baja = ∅ | ✅ grep |
| 5 | `BModulePart extends BPart`, holds `BIFile file` + lazy `ModuleManifest manifest`; extracts META-INF/module.xml with lowercase fallback | [CERT] | BModulePart.java:102,130,380-385 | ✅ read |
| 6 | `NModule.readXml` independently parses attributes/dependencies/types/exts/permissions; holds no ModuleManifest | [CERT] | NModule.java:378-383, :74-89 | ✅ read |
| 7 | Runtime type resolution is literal `moduleName + ":" + typeName` | [CERT] | NModule.java:226 (+236/244) | ✅ read verbatim |
| 8 | `BModule` aggregates `Map<RuntimeProfile,NModule>`; first part sets shared header, later mismatched moduleName throws | [CERT] | BModule.java:65, 242-258 | ✅ read verbatim |
| 9 | No converter install→runtime; the two sides use different Dependency types | [INFER] from #4 + distinct packages | ModuleManifest/BDependency vs NModule/Dependency | ✅ derived |

**Tally**: [CERT] ×8 · [INFER] ×1 · ratio 0.13 (EVIDENCE block — low ratio, investigable evidence intact). All 8 [CERT] citations token-checked against the vineflower source (switch block, loadType, addModulePart, nModulesByProfile read verbatim this iteration).

## Connections

- **[B12]** §12.1.4-5 — the manifest SCHEMA (devguide); this block is its code-side reader. **[B76]** — verbatim real manifests parsed here.
- **[B617]** — `ModuleClassLoader` delegation; the `NModule`/`BZipSpace` per-part model here is what that classloader loads from. **MA2/B630** will open the boot scan that CALLS `readXml`.
- **[B489]/[B492]** — signing artifacts (`codeSigners`/`signatureFailureCause` on `BModulePart`) = REMIT.
- Forward: MA3 (type pipeline) opens `getType`/`ClassScanner`; MA7 opens `readPermissions`; MA4 the `BZipSpace`/jar layout.

## Gaps uncovered

- None new — MA1 answered read-only on disk. Confirms the MA2/MA3/MA7 sources are the correct next reads (all reached from `NModule.readXml`).
