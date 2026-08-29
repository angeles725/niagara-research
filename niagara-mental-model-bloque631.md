# Niagara N4 — module-anatomy (MA3): the type-registration pipeline — `<type>` in module-include.xml is READ (not written) by Slotomatic, baked into a `RegistryDatabase` of `NTypeInfo` (no Class), and the Class is loaded exactly once at `NModule.loadClass` via the `ModuleClassLoader` (§14 refines B12)

**Focus**: module-anatomy · **Gap**: MA3 (type-registration pipeline) · **Session**: 2026-08-29 · **Block**: B631
**Sources** (all `[CERT]` decompiled Java, vineflower `decompiled/` tree):
- `organized/devkit/devkit-wb/decompiled/com/tridium/slottool/Compiler.java` · `Slotomatic.java` · `.../devkit/wizards/NewDriverWizard.java`
- `organized/baja/baja/decompiled/com/tridium/sys/registry/Builder.java` · `RegistryDatabase.java` · `NTypeInfo.java`
- `organized/baja/baja/decompiled/com/tridium/sys/module/NModule.java` · `com/tridium/sys/schema/NType.java`
- `organized/baja/baja/decompiled/javax/baja/util/BTypeSpec.java`

**Scope**: how a Baja Type travels from source annotation to a runtime-resolvable `moduleName:typeName`. Classloader delegation internals = [B617] (REMIT). Slot/annotation semantics = [B4] (REMIT). This block traces the REGISTRATION spine and issues a §14 refinement to [B12].

---

## 631.1 The five stages, and the headline correction

`@NiagaraType`/`<type>` → **module-include.xml** (build input) → **RegistryDatabase** (`NTypeInfo`, structural, no Class) → **`NModule.types`** (className strings, lazy) → **`Registry.getType`/`BTypeSpec`** (resolve to `NTypeInfo`) → **`NModule.loadClass`** (the one Class load, via `ModuleClassLoader`) → **`NType`** self-registers, promoting the map entry from String → Type.

**§14 refinement of [B12] §12.1.4/§12.1.8.** B12 states module-include.xml is "auto-generado… actualizado automáticamente por annotation processor durante compilación (no editar a mano)". The decompiled evidence contradicts the mechanism: there is **no `javax.annotation.processing` Processor anywhere in the module corpus** (grep for `implements Processor`/`extends AbstractProcessor`/`@SupportedAnnotationTypes` = ∅), and `module-include.xml` is **READ, not written**, by the Slotomatic tool. See §631.2. (Boundary honesty: the `com.tridium.niagara-module` gradle plugin lives under `$NIAGARA_HOME/etc/m2`, OUT of the decompiled corpus, so I cannot prove the gradle `slotomatic` task never rewrites the file — but the mechanism is the Slotomatic source-model tool, categorically NOT a JSR-269 annotation processor, and the `<type>` list is an INPUT that drives code generation.)

---

## 631.2 Stage 1 — BUILD: module-include.xml is the INPUT that drives Slotomatic

`NiagaraTypeProcessor` (`slottool.model.annotation.processors`) is a JavaParser-based source-model builder for the IDE/Slotomatic — it parses `@NiagaraType` expressions into an in-memory model and emits NO files.

The Slotomatic entry point resolves the manifest as an INPUT path `[CERT]` `Slotomatic.java:140`:
```java
Path moduleIncludePath = modulePath.resolve("module-include.xml");
Compiler compiler = new Compiler(options, moduleIncludePath, moduleTestIncludePath);
```

`Compiler` READS the `<types>` node to learn which classes are Baja types (+ their `<on>` agents and `<ext>` file extensions) `[CERT]` `Compiler.java:67-99`:
```java
NodeList typesList = doc.getElementsByTagName("types");
if (typesList.getLength() == 0) log.warning("module-include.xml has no \"types\" node");
Node types = typesList.item(0); NodeList typeList = types.getChildNodes();
// ...<on type=..>→agentOn.addType, <ext name=..>→niagaraType.addFileExt
```

`Compiler.write()` writes the **`.java`** source (the generated slot code between `/*+ BEGIN BAJA AUTO GENERATED +*/` markers), NOT module-include.xml `[CERT]` `Compiler.java:296-301`; and it GUARDS on the class being listed in the manifest `[CERT]` `Compiler.java:270`:
```java
throw new IllegalArgumentException("Cannot update " + fullFileName + "; it is not in module-include.xml");
```
So module-include.xml is the driving registry: a class absent from `<types>` is not processed. New `<type>` entries are SCAFFOLDED by the wizard `[CERT]` `NewDriverWizard.java` (`fs.makeFile(path.merge("module-include.xml"))` + `w("<type name=\"…\" class=\"….B…\" />")`), or authored by hand — not emitted by an annotation processor. Attributes on `<type>`: `name`, `class` (+ optional `ordScheme`, child `<agent>/<on>`, `<file>/<ext>`); `abstract`/`final`/`modifiers` are NOT in the XML for jar modules — they come from bytecode at registry build (§631.3).

---

## 631.3 Stage 2 — REGISTRY BUILD: `<type>` + bytecode → `NTypeInfo`

`Builder.readType` reads each `<type>` from the module manifest and creates a `TypeBuild` keyed by `BTypeSpec.make(moduleName, typeName)`; `className` comes from the `class` attr. For `.jar` modules it runs `ClassScanner` over the bytecode to fill `modifiers`/`superClass`/`interfaceClasses` ([B630] §630.3 — this is the ONLY place ClassScanner runs); for `.sjar` it reads `extends`/`abstract`/`final` from the XML. The result is written into the `RegistryDatabase` `[CERT]` `Builder.java:231-232`:
```java
registry.db.types = b.typesByClass.values().toArray(new NTypeInfo[0]);
registry.db.typesBySpec = hashMap;   // BTypeSpec.toString() → NTypeInfo
```

`NTypeInfo` is a **pure structural record — no `Class<?>`** `[CERT]` `NTypeInfo.java:36-42` / `RegistryDatabase.java:61-62`: `typeSpec` (`BTypeSpec`), `modifiers` (int), `className` (String), `interfaces` (`NTypeInfo[]`). The registry is an index of module/superclass/interface/agent relationships that answers hierarchy queries WITHOUT loading a single class.

---

## 631.4 Stage 3 — BOOT: two independent structures

At boot ([B630]: no scan, deserialize) there are two parallel maps that must not be confused:

| Structure | Built from | Holds | Purpose |
|---|---|---|---|
| `NModule.types` (`Map<String,Object>`, [B629]) | the jar's `META-INF/module.xml` `<type>` list | `typeName → className(String)`, promoted to `Type` on load | drives CLASS LOADING for this module |
| `RegistryDatabase.typesBySpec` (`Map<String,NTypeInfo>`) | the prebuilt `.db` binary | `NTypeInfo` (structural, no Class) | drives TYPE-HIERARCHY QUERIES corpus-wide |

`RegistryDatabase.read()` rebuilds `typesBySpec` from the binary `[CERT]` `RegistryDatabase.java:272-294`; `getType(typeSpec)` is a map lookup returning `NTypeInfo` `[CERT]` `:116`. The two are independent: you can query a type's superclass/interfaces (registry) without ever loading its Class (module).

---

## 631.5 Stage 4 — RUNTIME: `moduleName:typeName` → NTypeInfo → Class → NType

Two distinct entry points on `BTypeSpec`:

- **Structural (no load)** `[CERT]` `BTypeSpec.java:97-98`: `getTypeInfo()` → `Sys.getRegistry().getType(spec)` → `RegistryDatabase.typesBySpec.get()` → `NTypeInfo`.
- **Resolving (loads Class)** `[CERT]` `BTypeSpec.java:101-108`:
```java
for (NModule module : Nre.getModuleManager().loadModuleParts(this.moduleName)) {
    if (!module.hasType(this.typeName)) continue;
    return module.getType(this.typeName);
}
throw new TypeNotFoundException(this.moduleName + ":" + this.typeName);
```

`NModule.getType(typeName)` pulls the map value; if still a `String`, it loads the class. The **one classloader boundary** `[CERT]` `NModule.java:179-185`:
```java
if (this.isSystemJar) return Class.forName(className);                    // bootstrap CL
return Class.forName(className, true, this.classLoader);                  // ModuleClassLoader (B617)
```
Loading a `B*` class runs its static initializer → `Sys.loadType(BFoo.class)` → `Introspector.introspect()` → `new NType(...)`, whose constructor self-registers `[CERT]` `NType.java:45`:
```java
this.module.register(this.typeName, this);   // atomically promotes NModule.types[typeName] String → NType
```
So the map entry is `String` (from XML) until first resolution, then permanently a live `NType`; subsequent `getType` calls are cache hits with no classloader re-entry. `RegistryDatabase.synthesizeType` (`:396`) is the parallel path for `auto.`/dynamically-created types with no jar class.

---

## 631.6 Stage 5 — three things, one addressing scheme

- **`BTypeSpec`** — the interned `moduleName:typeName` reference value (`BSimple`); no Class, no structure. What a slot/ORD/serialized `.bog` stores.
- **`NTypeInfo`** — the registry record: className + modifiers + superclass/interfaces + agents, from the prebuilt db; still no Class. Answers "what is this type's shape / who agents on it".
- **`NType`** — the fully loaded runtime type: has `Class<?> typeClass`, slots, module. Exists only after `NModule.loadClass`.

The Class is loaded exactly once, lazily, at first `getResolvedType`/`getType`, via the module's own `ModuleClassLoader` (or bootstrap for system jars). This is why a station can hold thousands of registered types (registry) while only the reachable subset is ever class-loaded.

---

## 631.7 What this means for building/distributing a module

- **module-include.xml is authored, not generated.** The `<type name= class=>` list is the source of truth Slotomatic reads to generate slot code and Builder reads to register types. Omit a `<type>` entry and the class is neither slot-processed nor registered — it exists as dead bytecode. (Chihuahua check for MA8: is every `B*` type listed?)
- **The registry knows structure without loading classes** — a module that ships a `<type>` whose `class` attr points at a missing/renamed FQCN passes registry build (structural) but throws at first `getResolvedType` (`Class not found for type moduleName:typeName`, [B629] NModule:244). Build-clean ≠ load-clean.
- **`isSystemJar` types bypass the ModuleClassLoader.** Only genuine system modules (baja etc.) load on the bootstrap loader; your module's types always load through its own `ModuleClassLoader` with your declared `<dependency>` visibility ([B617]).

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | No JSR-269 annotation processor in the module corpus | [CERT] | rg 'implements Processor'/'AbstractProcessor'/'@SupportedAnnotationTypes' organized = ∅ | ✅ grep |
| 2 | Slotomatic RESOLVES module-include.xml as input; Compiler READS `<types>`, writes only `.java` | [CERT] | Slotomatic.java:140 · Compiler.java:67-99,270,296-301 | ✅ read verbatim |
| 3 | new `<type>` scaffolded by wizard / authored by hand, not by an APT | [CERT] | NewDriverWizard.java (makeFile module-include.xml + w("<type…")) | ✅ read |
| 4 | Builder writes NTypeInfo[] + typesBySpec into RegistryDatabase; ClassScanner fills modifiers/super/interfaces (jar) | [CERT] | Builder.java:231-232 (+B630 readType/ClassScanner) | ✅ read verbatim |
| 5 | NTypeInfo has no Class — typeSpec/modifiers/className/interfaces only | [CERT] | NTypeInfo.java:36-42 · RegistryDatabase.java:61-62,116 | ✅ read |
| 6 | BTypeSpec.getTypeInfo = registry lookup (no load); getResolvedType = loadModuleParts→module.getType | [CERT] | BTypeSpec.java:97-108 | ✅ read verbatim |
| 7 | The one Class load: NModule.loadClass Class.forName(className,true,classLoader) / bootstrap if isSystemJar | [CERT] | NModule.java:179-185 | ✅ read verbatim |
| 8 | NType ctor self-registers, promoting NModule.types String→NType | [CERT] | NType.java:45 | ✅ read verbatim |
| 9 | B12 §12.1.4/§12.1.8 "annotation processor writes module-include.xml" is imprecise (§14 refine) | [INFER] from #1+#2 | this block §631.1-2 | ✅ derived |

**Tally**: [CERT] ×8 · [INFER] ×1 · ratio 0.13 (EVIDENCE block; investigable evidence intact). All [CERT] token-checked verbatim this iteration. §14 refinement issued to B12 (back-pointer added, see Connections).

## Connections

- **§14 REFINES [B12]** §12.1.4/§12.1.8 — module-include.xml is read by Slotomatic, not written by an annotation processor; no JSR-269 APT exists in the corpus. Back-pointer added to B12.
- **[B629]** — `NModule.types`/`readTypes` is stage 3's module-side map; `moduleName:typeName` errors. **[B630]** — the prebuilt RegistryDatabase + ClassScanner-at-rebuild this block's stage 2 populates.
- **[B617]** — the `ModuleClassLoader` used at stage 4's `loadClass`; REMIT. **[B4]** — slot/@NiagaraProperty semantics; REMIT.
- Forward: MA4 (jar layout) is where `<type>`/module.xml physically sit; MA7 opens `readPermissions`.

## Gaps uncovered

- None new. The `.db` `RegistryDatabase` binary format (read/write serialization) is deep infrastructure not needed for the "build/distribute" angle — noted, not a backlog row. MA3 answered read-only on disk.
