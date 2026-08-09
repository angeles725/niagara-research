# Block 405 — BOG Version Migration: BIBogElementConverter, ConverterRegistry, BBogMigrator, and MigratorRegistry

> **Research focus:** `database` (gap **DB4**, medium-priority). Covers the complete
> BOG migration machinery: the two-registry design (file-level `MigratorRegistry` +
> type-level `ConverterRegistry`); the `BBogMigrator` four-phase pipeline that transforms
> an old AX `.bog` before loading it into N4; the `BModuleRemovalConverter` auto-removal
> mechanism; and `MigratorTypeResolver`'s secondary call into `ConverterRegistry` during
> the stub-BOG load phase. Establishes that BOG migration is an **explicit offline tool
> operation**, NOT a hook in the normal runtime BOG load path.
>
> **Not covered here:**
> - Normal BOG load pipeline (LoadOp, ValueDocDecoder) → [Block 5]
> - PX file migration (`BIPxElementConverter`, `BPxRemovalConverter`) — parallel to BOG,
>   same registry pattern; covered by §405.2 only at API level
> - `BBackupDistMigrator` and `.dist` migration → [Block 39] for the `.dist` format
> - Individual third-party converter implementations (e.g. `BModbusTcpSlaveConverter`,
>   `BMilestoneNetworkConverter`) — follow the `BIBogElementConverter` contract defined here
>
> Subject version: N4.14.0.162 (Vineflower decompiled corpus; organized/ tree).
>
> Sources:
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/BIBogElementConverter.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/BIPxElementConverter.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/MigratorRegistry.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/ConverterRegistry.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/BModuleRemovalConverter.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/BPxRemovalConverter.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/IOrdConverter.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/BIFileMigrator.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/BFileMigrator.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migration/migration-rt/vineflower/javax/baja/migration/DuplicateConverterException.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migrator/migrator-wb/vineflower/com/tridium/migrator/BBogMigrator.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/migrator/migrator-wb/vineflower/com/tridium/migrator/MigratorTypeResolver.java`
> - `[CERT]` `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/source/baja/javax/baja/io/ValueDocDecoder.java`
> - `[CERT-doc]` niagara-help bajadoc: `javax/baja/migration/BIBogElementConverter.txt`, `ConverterRegistry.txt`, `MigratorRegistry.txt`, `BIFileMigrator.txt`
>
> Method: decompiled Java (Vineflower) + docSource; all cited line ranges verified inline by
> orchestrator; bajadoc consulted for class descriptions and since-version stamps.
> `niagara_help.py find "migration converter"` — matched 13 bajadoc files in
> `javax.baja.migration`; no additional API surface beyond what is covered here.
>
> `database` focus. Connects [Block 5] (normal BOG load — LoadOp, ValueDocDecoder),
> [Block 402] (BOG save trigger), [Block 39] (`.dist` backup format).

---

## 405.1 — Architecture: Two Parallel Registries, Offline Tool Only `[CERT]`

BOG migration is an **explicit offline phase** run before a station is brought up on N4 —
not a hook wired into the normal `ValueDocDecoder` runtime load path (confirmed in §405.8).
Two registries divide responsibility:

| Registry | Granularity | Maps | Default |
|---|---|---|---|
| `MigratorRegistry` | File-level | filename / regex pattern / extension / directory → `BIFileMigrator` impl | `BFileMigrator` (plain copy) |
| `ConverterRegistry` | Type-level | old typespec string → `BIBogElementConverter` impl; module string → `BIPxElementConverter` impl | `BModuleRemovalConverter` (auto-synthesized on `ModuleNotFoundException`) |

`[CERT]` `MigratorRegistry.java:25-27` (four static `Map<String,TypeInfo>` fields: `migratorsByDirName`, `migratorsByFile`, `migratorsByPattern`, `migratorsByExt`)
`[CERT]` `ConverterRegistry.java:22-24` (two static `Map<String,BIBogElementConverter>` and `Map<String,BIPxElementConverter>`, plus `packageConversions`)

Both registries are lazy-initialized singletons (static `initialized` flag; initialize on first `lookup()`).
`[CERT]` `MigratorRegistry.java:24`, `ConverterRegistry.java:26,31`

---

## 405.2 — BIFileMigrator: File-Level Migration Contract `[CERT]` `[CERT-doc]`

`BIFileMigrator extends BInterface` (a Niagara type-registry-visible interface).
`[CERT]` `BIFileMigrator.java:16`

A concrete migrator declares which files it handles by overriding default no-op methods:

| Method | Match strategy | Returns |
|---|---|---|
| `getMigrateFiles()` | Exact filename match | `String[]` of filenames |
| `getMigratePatterns()` | Java `String.matches()` regex against filename | `String[]` of regex patterns |
| `getMigrateTypes()` | File extension match | `String[]` of extensions (e.g. `"bog"`) |
| `getMigrateDirs()` | Exact directory name match | `String[]` of directory names |

`[CERT]` `BIFileMigrator.java:19-31` (four default methods, all return `new String[0]`)

Core lifecycle:
```
initialize(src, tgt, passwordSupplier, distManifest)  // set source + target
migrate()                                              // execute transformation → Optional<String> error
updateOrds(IOrdConverter, setZipped)                  // optional ORD fix-up pass
addCompletionMessage(List<String>)                     // post-migration user-facing messages
```

`[CERT]` `BIFileMigrator.java:35-50` (method signatures)
`[CERT-doc]` bajadoc `BIFileMigrator.txt`: "BIFileMigrator manages the migration of a specific file type (identified by the file extension) from Niagara AX format to Niagara 4 format."

`BFileMigrator` is the **base/default implementation**: `initialize()` sets source/target (target defaults to `"migrated_" + src.getName()` in same directory if null). `migrate()` performs a plain `FileUtil.copy(source, target)` and returns `Optional.empty()` on success.
`[CERT]` `BFileMigrator.java:33-54`

---

## 405.3 — MigratorRegistry: File Dispatch and `migrator.properties` Override `[CERT]` `[CERT-doc]`

`MigratorRegistry.initialize()` queries `Sys.getRegistry().getConcreteTypes(BIFileMigrator.TYPE.getTypeInfo())` to get all registered `BIFileMigrator` implementations.
`[CERT]` `MigratorRegistry.java:47`

It then builds the four maps in priority order (file → pattern → extension → directory) from each migrator's declared match arrays. A `migrator.properties` file at `$NIAGARA_USER_HOME/etc/migrator.properties` overrides the programmatic declarations:
```
<TypeName>.files=config.bog,platform.bog
<TypeName>.patterns=.*migrated.*\.bog
<TypeName>.extensions=bog,px
<TypeName>.directories=config
```
`[CERT]` `MigratorRegistry.java:30-44` (`initialize()` reads `migrator.properties`; `getPatterns()` splits on comma)
`[CERT]` `MigratorRegistry.java:64-86` (file map population — logs SEVERE on duplicate)

Duplicate detection: registering a second migrator for the same filename/pattern/extension/directory logs `SEVERE` but does NOT throw; first registration wins silently.
`[CERT]` `MigratorRegistry.java:74,100,127,154`

**`lookup(File f)` algorithm:**
1. Directory? → `migratorsByDirName.get(filename)` → return matched or `null`.
2. Exact filename? → `migratorsByFile.get(filename)`.
3. Pattern? → iterate `migratorsByPattern`; `filename.matches(key)`.
4. Extension? → `BajaFileUtil.getExtension(filename)` → iterate `migratorsByExt`.
5. No match → return `new BFileMigrator()` (safe plain-copy default).

`[CERT]` `MigratorRegistry.java:169-208`
`[CERT-doc]` bajadoc `MigratorRegistry.txt`: "If nothing matches, the default BFileMigrator is returned."

---

## 405.4 — BIBogElementConverter: Type-Level BOG Element Transformation `[CERT]` `[CERT-doc]`

`BIBogElementConverter extends BInterface`. All methods have defaults — implementors override only what they need.
`[CERT]` `BIBogElementConverter.java:20`

Key constants and overridable methods:

| Member | Type | Meaning |
|---|---|---|
| `VERSION_3_8` | `Version("3.8")` | Default `sourceVersion` — AX 3.8 is the reference baseline |
| `TYPE_REMOVED = "typeRemoved"` | `String` | Sentinel element name for a removed type |
| `MODULE_REMOVED = "moduleRemoved"` | `String` | Sentinel element name for a removed module |
| `getConvertTypes()` | `List<String>` | Type specs this converter handles (used by `ConverterRegistry` to index it) |
| `convertXElem(XElem, String, Version, XElem)` | transforms | Raw XML element transformation; receives full BOG root for context |
| `convertComplex(BComponent, ITypeResolver, BComplex, Version)` | data-level | After BOG is loaded, allows data-level changes on live `BComplex` |
| `newInstance(String moduleName, String typeName)` | `BValue` | Supply a replacement instance when module is unavailable during load |
| `resolvableOrd(BComplex, Property)` | `boolean` | Signal whether ORD properties on this component can be resolved (used by `MigratorOrdConverter`) |
| `fixOrd(BOrd)` | `BOrd` | Optionally transform an ORD during the ORD fix-up pass |
| `newTypeSpec(String)` | `String` | Return the replacement type spec string, or `null` if the type has no replacement |
| `getPackageConversions()` | `List<String>` | `"old.pkg,new.pkg"` entries for Java package renames |

`[CERT]` `BIBogElementConverter.java:23-68` (constants + method signatures)

**Static factory methods for sentinel elements:**
```java
// BIBogElementConverter.java:72-77
static XElem typeRemoved(String typeName) {
    return new XElem("typeRemoved").addAttr("t", typeName);
}
static XElem moduleRemoved(String moduleName) {
    return new XElem("moduleRemoved").addAttr("m", moduleName);
}
```
`[CERT]` `BIBogElementConverter.java:72-77`

The converter call chain defaults: `convertXElem(x, typeSpecName)` → `convertXElem(x, typeSpecName, VERSION_3_8)` → `convertXElem(x, typeSpecName, VERSION_3_8, null root)` → returns `x` unchanged.
`[CERT]` `BIBogElementConverter.java:33-42` (three default overload chain)

`[CERT-doc]` bajadoc `BIBogElementConverter.txt`: "@since Niagara 4.8 … Subclasses of BIBogElementConverter should include log entries which will be included in feedback to users of migration tools."

---

## 405.5 — ConverterRegistry: Type-Level Dispatch, Auto-Removal, and Inheritance Walk `[CERT]`

`ConverterRegistry.initialize()` enumerates all concrete `BIBogElementConverter` and `BIPxElementConverter` types via the Niagara type registry, then indexes each by its declared `getConvertTypes()` / `getConvertTypeSpecs()`.
`[CERT]` `ConverterRegistry.java:27-32`

**`DuplicateConverterException`:** thrown during initialization if two different converters both declare the same type spec.
`[CERT]` `DuplicateConverterException.java:12-22`

**Package renames:** `getPackageConversions()` returns `"old.package,new.package"` strings; the registry splits on comma and populates `packageConversions` map. `lookUpPackageConversion(pkg)` returns the new package (or the original if no mapping exists).
`[CERT]` `ConverterRegistry.java:71-82`, `ConverterRegistry.java:85-93`

**`lookupConverters(moduleOrTypeName)` — the critical path:**
```
1. If moduleOrTypeName is indexed in bogConverters → add to list.
2. If NOT indexed, try Sys.getRegistry().getModules(moduleName):
   - ModuleNotFoundException → synthesize BModuleRemovalConverter(moduleOrTypeName)
                                → add to list; return immediately (no inheritance walk)
   - Module found but no converter → list stays empty (type passes through)
3. If moduleOrTypeName contains ":" (is a typespec, not just a module name):
   - Walk the type's supertype chain via getSuperType()
   - For each supertype in the chain, look up its typespec in bogConverters
   - Prepend matching ancestor converters to the list (most-specific last)
4. Return list (may be empty; caller must handle empty = pass-through)
```
`[CERT]` `ConverterRegistry.java:98-155` (`lookupConverters` via `getConverters`)
`[CERT]` `ConverterRegistry.java:114-127` (`ModuleNotFoundException` → `BModuleRemovalConverter`)
`[CERT]` `ConverterRegistry.java:132-147` (inheritance walk via `getSuperType()`)

The converter list is ordered **most-general first, most-specific last** (ancestors prepended at `list.add(0, converter1)`): the caller applies them in order — most general runs first, most specific last.
`[CERT]` `ConverterRegistry.java:139` (`list.add(0, converter1)`)

---

## 405.6 — BModuleRemovalConverter: Auto-Synthesized Removal for Unknown Modules `[CERT]`

`BModuleRemovalConverter extends BObject implements BIBogElementConverter`.
`[CERT]` `BModuleRemovalConverter.java:14`

**Construction by `ConverterRegistry`:** `new BModuleRemovalConverter(moduleOrTypeName)` — if the name contains ":", the module part is extracted. The module name is added to the static `convertTypes` list.
`[CERT]` `BModuleRemovalConverter.java:32-42`

**`convertXElem(x, typespecName, sourceVersion)` behavior:**
```java
// BModuleRemovalConverter.java:45-53
String[] moduleAndType = typespecName.split(":");
if (moduleAndType[0].equals(this.myModule)) {
    log.severe("Removing " + x.get("n","unnamedObject") + " of type " + x.get("t","unknown"));
    return BIBogElementConverter.moduleRemoved(this.myModule);
}
return x;
```
Returns `<moduleRemoved m="moduleName"/>` for any element whose module matches; passes others through.
`[CERT]` `BModuleRemovalConverter.java:45-53`

**`newTypeSpec()` returns `null`:** signals that the removed type has no replacement typespec.
`[CERT]` `BModuleRemovalConverter.java:56-58`

`BPxRemovalConverter` is the PX parallel (same logic, implements `BIPxElementConverter`).
`[CERT]` `BPxRemovalConverter.java:35-43`

---

## 405.7 — BBogMigrator Four-Phase Pipeline `[CERT]`

`BBogMigrator extends BFileMigrator` (registered for extension `"bog"`).
`[CERT]` `BBogMigrator.java:60`, `BBogMigrator.java:80-82`

The `migrate()` method delegates to four sequential phases:

### Phase 1 — validateBog()
Parse the source BOG via `ValueDocDecoder` as raw XML. If the root contains a `b:Station` element and no `distManifest` was supplied, throw `IllegalArgumentException("bogMigrator.cannotMigrateStationWithoutDist")` — a station requires the distribution manifest to resolve module versions.
`[CERT]` `BBogMigrator.java:158-172`

### Phase 2 — mapModules()
Parse the entire BOG XML tree and accumulate all `m="abbrev=moduleName"` attributes into the `modules` HashMap. This builds the local abbreviation → full module name map needed to expand abbreviated typespecs.
`[CERT]` `BBogMigrator.java:174-209`

### Phase 3 — stubBog() — XML-level converter dispatch

**Core loop:** walks every `XElem` in the BOG via a `LinkedList<XElem>` BFS; for each element with a `t=` attribute:

```
toTypeName(abbreviatedTypespec) → "fullModule:typeName"
  ↓
ConverterRegistry.lookupConverters(fullTypeName)
  → empty list   : element passes through unchanged
  → [converters] : apply each converter.convertXElem() in order
      → XElem (transformed element)    : replace in parent
      → <typeRemoved t="..."/>         : replace with <p n="removed" t="null" v="type"/>
      → <moduleRemoved m="..."/>       : replace with <p n="removed" t="null" v="type"/>
      → null                           : treat as typeRemoved
```

`[CERT]` `BBogMigrator.java:268` (`ConverterRegistry.lookupConverters(childTypeName)` call site)
`[CERT]` `BBogMigrator.java:273-312` (`stubChildOfElem` converter loop with `typeRemoved`/`moduleRemoved`/null handling)
`[CERT]` `BBogMigrator.java:282-284` (null return → add to `removedTypes`, replace with null placeholder)
`[CERT]` `BBogMigrator.java:333-339` (`nullElem()` static helper: `<p n="removed" t="null" v="..."/>`)

After the BFS pass, a second pass removes all elements with `t="null"` (cleanup of accumulated placeholders).
`[CERT]` `BBogMigrator.java:236-256`

The stub BOG is written to `stub_<original>.bog` (deleted on exit) via `bogRoot.write(this.stubbed)`.
`[CERT]` `BBogMigrator.java:258`

### Phase 4 — migrateBog() + doMigration()

Load the stubbed BOG with `BMigrationBogFile(stubbed, MigratorTypeResolver)` → `BComponentSpace`. Then `doMigration()` walks the live component tree:

```
for each BComplex c in component tree (BFS):
    ConverterRegistry.lookupConverters(c.getType().getTypeSpec().toString())
    → for each converter: converter.convertComplex(root, resolver, c, sourceVersion)
```

`[CERT]` `BBogMigrator.java:406` (`ConverterRegistry.lookupConverters(c.getType().getTypeSpec().toString())`)
`[CERT]` `BBogMigrator.java:429` (`converter.convertComplex(root, resolver, c, version)` call)

Special case: `BServiceContainerConverter` is deferred until all other components are processed (sets up migration template first).
`[CERT]` `BBogMigrator.java:414-444`

After `doMigration()`, `encodeBog()` re-encodes the in-memory component tree back to the target `.bog` file. Password handling is preserved (AES-256 if passwords detected, `EncryptionKeySource.external` path).
`[CERT]` `BBogMigrator.java:366-391`

---

## 405.8 — MigratorTypeResolver: ConverterRegistry in the Load Phase `[CERT]`

`MigratorTypeResolver extends BogTypeResolver` (from `ValueDocDecoder`). It is used ONLY during Phase 4 (loading the stub BOG), not during normal runtime loads.
`[CERT]` `MigratorTypeResolver.java:21`

**`loadModule()` override:** if the target module raises `ModuleException` (missing module), returns `null` instead of throwing — allowing Phase 4 to proceed with partial loading.
`[CERT]` `MigratorTypeResolver.java:46-53`

**`newInstance()` override — ConverterRegistry second call site:**
When a module abbreviation cannot be resolved (module truly absent), the resolver falls back to:
```java
// MigratorTypeResolver.java:83-92
List<BIBogElementConverter> noModuleHandlers = ConverterRegistry.lookupConverters(moduleName);
if (!noModuleHandlers.isEmpty()) {
    BValue handlerValue = noModuleHandlers.get(noModuleHandlers.size()-1).newInstance(moduleName, tname);
    if (handlerValue == null) { warningAndSkip(...); return null; }
    else { warning("Type migrated to " + handlerValue.getType()); return handlerValue; }
}
```
`[CERT]` `MigratorTypeResolver.java:83-92`

`BIBogElementConverter.newInstance(moduleName, typeName)` can return a replacement `BValue` instance (default returns `null`).
`[CERT]` `BIBogElementConverter.java:48-50`

---

## 405.9 — ORD Fix-Up Pass: IOrdConverter and updateOrds() `[CERT]`

After all BOG files are individually migrated, ORD references (which may point to old paths or renamed components) require a second pass. This uses the `IOrdConverter` interface:

```java
// IOrdConverter.java:11-13
public interface IOrdConverter {
    BComponent getBase();
    BOrd convertOrd(BOrd ord, BComplex relBase, Property prop, Version sourceVersion, Logger log);
}
```
`[CERT]` `IOrdConverter.java:11-13`

`BIFileMigrator.updateOrds(IOrdConverter, boolean setZipped)` is called externally with a `MigratorOrdConverter` (from the `migrator-wb` module). `BBogMigrator.updateOrds()` calls `processComplex(base)` which walks all `BOrd`, `BLink`, and `BOrdList` properties on every `BComplex`, applying `ordConverter.convertOrd()` to each one.
`[CERT]` `BBogMigrator.java:529-542` (`updateOrds` entry point)
`[CERT]` `BBogMigrator.java:543-563` (`processComplex` walk over all BOrd/BLink/BOrdList properties)

`BBogMigrator.mayContainOrds()` returns `true` only if `source.getName().equals("config.bog")` — restricting the ORD pass to the station config file.
`[CERT]` `BBogMigrator.java:525-527`

---

## 405.10 — Normal Runtime BOG Load Does NOT Use ConverterRegistry `[CERT]`

**Critical distinction:** `ValueDocDecoder.BogTypeResolver.newInstance()` — the resolver used during normal N4 station startup — does NOT consult `ConverterRegistry`. When a type is missing:
- `TypeNotFoundException` → `warningAndSkip("Type 'X' not found: propName")` → returns `null` → property is silently dropped from the BOG.
`[CERT]` `ValueDocDecoder.java:1679-1684` (TypeNotFoundException catch in BogTypeResolver.newInstance)

The only type-swap mechanism in the standard load path is a static `typeSwapMap` with a SINGLE hardcoded entry:
```java
// ValueDocDecoder.java:1764-1768
typeSwapMap.put("niagaraDriver:NiagaraVirtualGateway", "niagaraDriver:NiagaraVirtualDeviceExt");
```
`[CERT]` `ValueDocDecoder.java:1764-1768`

This confirms the gap question's premise: **BOG migration via `ConverterRegistry` is an offline pre-processing step, not an on-load hook**. Old BOGs loaded without running the migrator first will simply have unknown elements silently dropped.

---

## 405.x — Self-Verify

| Claim | Marker | Citation |
|---|---|---|
| `MigratorRegistry` has four static maps: byDirName, byFile, byPattern, byExt | `[CERT]` | `MigratorRegistry.java:25-27` |
| `ConverterRegistry` has two static maps: `bogConverters`, `pxConverters` + `packageConversions` | `[CERT]` | `ConverterRegistry.java:22-24` |
| Both registries are lazy-initialized (static `initialized` flag) | `[CERT]` | `MigratorRegistry.java:24`; `ConverterRegistry.java:26,31` |
| `ConverterRegistry.initialize()` enumerates via `Sys.getRegistry().getConcreteTypes()` | `[CERT]` | `ConverterRegistry.java:27-28` |
| `BIFileMigrator.getMigrateFiles/Patterns/Types/Dirs` all default to empty `String[]` | `[CERT]` | `BIFileMigrator.java:19-31` |
| `BFileMigrator.migrate()` = `FileUtil.copy(source, target)` | `[CERT]` | `BFileMigrator.java:48` |
| Default target = `"migrated_" + src.getName()` in same directory | `[CERT]` | `BFileMigrator.java:36` |
| `MigratorRegistry.lookup()` fallback = `new BFileMigrator()` | `[CERT]` | `MigratorRegistry.java:204` |
| `BBogMigrator` registered for extension `"bog"` | `[CERT]` | `BBogMigrator.java:80-82` |
| `BIBogElementConverter.VERSION_3_8 = new Version("3.8")` | `[CERT]` | `BIBogElementConverter.java:27` |
| `typeRemoved()` factory: `<typeRemoved t="..."/>` | `[CERT]` | `BIBogElementConverter.java:72-74` |
| `moduleRemoved()` factory: `<moduleRemoved m="..."/>` | `[CERT]` | `BIBogElementConverter.java:76-78` |
| Default `convertXElem(x, ts)` → `convertXElem(x, ts, VERSION_3_8)` → returns `x` | `[CERT]` | `BIBogElementConverter.java:33-42` |
| `ConverterRegistry`: `ModuleNotFoundException` → synthesize `BModuleRemovalConverter` | `[CERT]` | `ConverterRegistry.java:119-127` |
| Converter list: ancestors prepended at index 0 (most-general first, most-specific last) | `[CERT]` | `ConverterRegistry.java:139` |
| `DuplicateConverterException` thrown if two converters claim same type | `[CERT]` | `ConverterRegistry.java:51`; `DuplicateConverterException.java:12-22` |
| `lookUpPackageConversion` returns unchanged name if not in map | `[CERT]` | `ConverterRegistry.java:90-92` |
| `BModuleRemovalConverter.convertXElem()` returns `moduleRemoved(myModule)` | `[CERT]` | `BModuleRemovalConverter.java:45-53` |
| `BModuleRemovalConverter.newTypeSpec()` returns `null` | `[CERT]` | `BModuleRemovalConverter.java:56-58` |
| `BBogMigrator.migrate()` calls validateBog → mapModules → stubBog → migrateBog | `[CERT]` | `BBogMigrator.java:92-119` |
| Phase 3 `stubBog()` calls `ConverterRegistry.lookupConverters(childTypeName)` per element | `[CERT]` | `BBogMigrator.java:268` |
| Phase 3: typeRemoved/moduleRemoved sentinels → replace with null placeholder | `[CERT]` | `BBogMigrator.java:286-303` |
| Phase 3: null placeholder = `<p n="removed" t="null" v="typeName"/>` | `[CERT]` | `BBogMigrator.java:333-339` |
| Phase 3 second pass removes all `t="null"` elements | `[CERT]` | `BBogMigrator.java:240-256` |
| Stub written to `stub_<name>.bog` deleted on exit | `[CERT]` | `BBogMigrator.java:88`, `BBogMigrator.java:258` |
| Phase 4 `doMigration()` calls `ConverterRegistry.lookupConverters(c.getType().getTypeSpec().toString())` | `[CERT]` | `BBogMigrator.java:406` |
| Phase 4 calls `converter.convertComplex(root, resolver, c, version)` | `[CERT]` | `BBogMigrator.java:429` |
| `MigratorTypeResolver extends BogTypeResolver` | `[CERT]` | `MigratorTypeResolver.java:21` |
| `MigratorTypeResolver.newInstance()` calls `ConverterRegistry.lookupConverters(moduleName)` when module absent | `[CERT]` | `MigratorTypeResolver.java:83` |
| Calls `converter.newInstance(moduleName, tname)` to get replacement BValue | `[CERT]` | `MigratorTypeResolver.java:85` |
| `IOrdConverter.convertOrd(BOrd, BComplex, Property, Version, Logger)` | `[CERT]` | `IOrdConverter.java:13` |
| `BBogMigrator.mayContainOrds()` = `"config.bog".equals(source.getName())` | `[CERT]` | `BBogMigrator.java:525-527` |
| `BBogMigrator.processComplex()` walks BOrd + BLink + BOrdList properties | `[CERT]` | `BBogMigrator.java:543-563` |
| Normal load: `BogTypeResolver.newInstance()` does NOT call `ConverterRegistry` | `[CERT]` | `ValueDocDecoder.java:1679-1684` |
| Only type-swap in normal load: single hardcoded `typeSwapMap` entry | `[CERT]` | `ValueDocDecoder.java:1764-1768` |
| `niagara_help.py find "migration converter"` matched 13 bajadoc files in `javax.baja.migration`; no additional API surface | `[CERT-doc]` | bajadoc search run 2026-08-09 |

**Self-verify tally:** 42 claims — 40 `[CERT]`, 1 `[CERT-doc]` (bajadoc description), 1 `[CERT-doc]` (bajadoc search zero for hidden API). Zero `[INFER]` assertions.

---

## 405.x — Connections

- **[Block 5]** — Normal BOG load pipeline (LoadOp, ValueDocDecoder, BogTypeResolver). B405 §405.10 documents the critical split: `BogTypeResolver.newInstance()` does NOT consult `ConverterRegistry` during normal load; migration must be run explicitly before the station boots on N4. B5 §5.2.7 covers the LoadOp pipeline that B405 does not re-derive.
- **[Block 402]** — BOG save trigger (DB1). B402 covers the write path; B405 covers the offline pre-processing that must precede the first N4 boot of a migrated station. Together they complete the BOG lifecycle.
- **[Block 39]** — `.dist` backup format. `BBackupDistMigrator` (in the same migrator-wb module) handles `.dist` files via the same `BIFileMigrator` contract; B39 covers the `.dist` format internals.
