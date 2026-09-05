# B807 · The N4 build pipeline + module versioning — plugin-source-cited task matrix, the station-lock copy, and the reload code path `[CERT]`

> First-principles (Excavador) account of what each `niagara-module` gradle task actually DOES — cited to the
> decompiled plugin source line — plus what a module-version bump changes and the EXACT station code path on
> `Out-of-date: Module changed`. Deliverable for the kit: a BUILD-LOOP build-task matrix, a version-bump
> checklist, and the mirror recipe with a source cite for the station lock.
>
> **Sources**: the decompiled gradle plugin `niagara-plugins-7.6.17.jar` (vineflower; cites given as
> `class.kt/java:line` — an `extern` decompiled tree, so verify-block cannot resolve them, driver token-verified
> against the decompile); baja framework `organized/baja/baja/{vineflower,decompiled}/…` (NRegistry,
> RegistryChecksum, Station, ValueDocDecoder — driver-verified by grep); niagara-help devguide. Markers:
> `[CERT]` source `file:line` · `[CERT-doc]` devguide · `[INFER]`.
>
> **Type:** `mixed`. Connects [Block 793]/[Block 794] (build/scaffold), [Block 795]/[Block 799] (schema-risk),
> [Block 800] §800.8 (the live reload OUTAGE — its console lines ARE the ValueDocDecoder calls cited here),
> [Block 806] (resource budget), kit `build-verify.md`.

## 807.1 — Build-task matrix (plugin-cited) `[CERT]`
Root plugin `com.tridium.gradle.plugins.module.NiagaraModulePlugin.apply()` chains: `configureJavaConvention →
createNiagaraModuleTestSourceSet → registerModuleTasks (jar, writeModuleXml, moduleTestJar) → registerSlotomaticTasks
→ registerNiagaraTestTasks → configureCleanTask → configureJavaCompileTasks`.

| Task | What it does · on-disk effect | Plugin cite | When needed |
|---|---|---|---|
| `clean` | deletes `build/` AND the deployed `<niagara_home>/modules/<mod>.jar` (registered as an `@OutputFile`) | `SigningAwareArchiveTaskExtension.kt:76` (`outputs.file(installFile).optional(true)`) | before a version bump / slot regen |
| `slotomatic` | rewrites the `//region AUTO` block in `src/*.java` from `@NiagaraProperty/@NiagaraAction/@NiagaraType`; edits SOURCE, not `build/` | `SlotomaticTask.getIncludedFiles():110` (main + moduleTest source sets → `com.tridium.slottool.Slotomatic`) | any annotation change (MANDATORY before jar) [CERT-doc devguide `slot-o-matic.txt:94`] |
| `compileJava` | standard Gradle; `src/` → `build/classes/java/main/`. Plugin only forces UTF-8 | `NiagaraModulePlugin.configureJavaCompileTasks():1094` (`options.setEncoding("UTF-8")`) | any Java change |
| `writeModuleXml` | generates `META-INF/module.xml` from `moduleManifest{}` (name/vendor/vendorVersion/bajaVersion/deps/types) | `WriteModuleXml.kt:9`; emission `ModuleXml.toElementInternal():264` | any manifest/dep/version change |
| `jar` | assembles classes+META-INF+palette+lexicon → `build/libs/<mod>.jar`, SIGNS it, then COPIES it to `modules/` (§807.2) | signing wired `DefaultSigningExtension.kt:29` (`doLast(ArchiveSigningAction)`) | any code change |
| `moduleTestJar` | same for the `moduleTest` source set → `<mod>-test.jar`; `onlyIf { !testSrc.isEmpty }` | `NiagaraModulePlugin.kt:1236` (`MODULE_TEST_JAR_TASK`) | test changes |
| `niagaraTest` | native Niagara test runner (`bin/test` + dev license); **0 tests from WSL** → use `run-pure-test.sh` | `NiagaraModulePlugin.registerNiagaraTestTasks():1032` (`dependsOn moduleTestJar`) | (not a WSL gate) |
| `bajadoc` | Bajadoc HTML for `javax/baja/**` (excludes `com/tridium/**`) | `NiagaraModulePlugin.registerJavadocTasks():986` | docs |

## 807.2 — The station-lock copy (build.sh exit 31) — cited `[CERT]`
The `jar` task, on its `doLast`, installs the signed jar into the running station's module dir. The cite chain:
- **Where the target is set:** `ModuleSigningConfigurationAction.kt:13` — `extension.installDir.set(this.environment.niagaraHome.dir("modules"))`; `installFile = installDir.file(<mod>.jar)` (`SigningAwareArchiveTaskExtension.kt:56`).
- **The doLast action:** `DefaultSigningExtension.wireArchiveSigning():29` — `task.doLast(ArchiveSigningAction(extension))`, applied to every `Jar` task via `configureEach`.
- **The actual copy:** `ArchiveSigningAction.kt:53-56` — `build/libs/<mod>.jar → installFile` via `FilesKt.copyTo` / `FileUtils.copyFile` / `Files.copy(REPLACE_EXISTING)` (guarded by `if signing aliases configured`).

**WHY the lock (exit 31):** a RUNNING station holds an OS read lock on `<niagara_home>/modules/<mod>.jar`; the
doLast copy tries to overwrite it → `IOException` → gradle marks `jar` FAILED. **But `build/libs/<mod>.jar` was
already assembled + signed BEFORE the doLast** — the artifact is complete; only the install copy failed. So the
mirror recipe is correct: build against a writable MIRROR of `niagara_home` (`toolbelt/mirror-niagara-home.sh`),
the copy lands in the throwaway mirror's `modules/`, and `build/libs/<mod>.jar` is the deliverable — the live
install is never written. `[INFER, grounded in the cited copy step]`

## 807.3 — Module versioning `[CERT / CERT-doc]`
- **`vendorVersion`** = the module author's version, what the station's dependency check uses; **`bajaVersion`** =
  the baja framework version built against (legacy, default `"0"`, `ModuleXml.java:73`).
- **Where set:** the root `vendor { defaultModuleVersion("X.Y.Z") }` → `VendorExtension.kt:88`
  `getVendorVersion().set(version)` (fallback = `project.version`, `ModuleXml.java:76`).
- **Where stamped:** `ModuleXml.toElementInternal():264` — `root.setAttr("vendorVersion", …)` / `"bajaVersion"` into
  the generated `META-INF/module.xml`. A bump changes jar bytes → **the signature is invalidated → the `jar`
  task always re-signs** (§807.2). A JACE-bound module still needs the operator's project-CA re-sign; a Honeywell
  supervisor accepts gradle's DEV cert.
- **Dependency FLOOR:** `<dependency vendorVersion="4.14">` is a MINIMUM (≥), not exact — devguide `modules.txt`:
  "the lowest vendorVersion … required … any version greater … is considered usable" `[CERT-doc]`. The stamp is
  `Version.strip(2)` (default `getDependencyVersionLimit()=2`, `ModuleXml.java:74`) → `4.14.0.162` → `4.14`.

## 807.4 — The reload code path: `Out-of-date: Module changed` (driver-verified) `[CERT]`
1. `NRegistry.isRegistryUpToDate():234` iterates installed jars and calls `RegistryChecksum.checkUpToDate(jar)`.
2. `RegistryChecksum.java:44-45` — `if (snapshot.timestamp != file.lastModified()) throw RegistryException("Module changed \"<name>\"")` — **timestamp-based, NOT a hash** (replacing the jar changes lastModified). A brand-new jar → `:42` `"Module added"` (not a crash).
3. `NRegistry.java:268` — `log.info("Out-of-date: " + e.getMessage())` = the console line ([Block 800]); then `rebuild():280` re-discovers modules and rebuilds the type registry.
4. `Station.loadStation()` re-decodes `config.bog` via `ValueDocDecoder`; per slot it resolves the type against the freshly-rebuilt registry and applies `parent.set(prop, value, Context.decoding)`. Mismatches are non-fatal `warningAndSkip`: `ValueDocDecoder.java:497` `"Cannot set property …"` (a slot RETYPE — value dropped), `:502` `"Missing slot …"`, `:675` `"Missing frozen property: …"`.
5. If a type/module cannot load at all → `Station.java:211` `log(SEVERE,"Cannot load station")` + `:212 System.exit(-5)` — the station does NOT boot.

**Cross-confirmation with [Block 800] §800.8 `[CERT-live]`:** the 2026-09-03 PANCCADIA outage console lines are
EXACTLY these calls — `[sys.xml] Cannot set property RoomPanel.setpoint …` (`:497`), `Missing frozen property:
differentialUp …` (`:675`), `Missing slot StatusNumeric.startDelay` (`:502`), then `SEVERE [sys] Cannot load
station` (`:211`). So the live OUTAGE is the reload path hitting a slot RETYPE it cannot decode = the B795/B799
schema-risk class. **What survives a bump vs breaks it:** identical/added slots survive (added → default);
renamed/removed/RETYPED slots warn-skip (value lost, boots); a class removed or a module that won't load →
`Cannot load station` (fatal). A `@NiagaraProperty` type change + slotomatic regen is the schema-breaking case.

## 807.5 — Version-bump checklist (kit candidate) `[INFER, grounded]`
- **PATCH** (fix only, no slot/type change): bump `defaultModuleVersion`; close Workbench (free lock); `clean :jar`;
  `verify-module.sh`; deploy. Schema risk NONE.
- **MINOR** (new slots only): `clean :slotomatic :jar`; `verify-module.sh --src`; `schema-risk.sh` (B799); deploy,
  cold-boot, watch for `Cannot set property`. Risk LOW (new slots default).
- **MAJOR** (retype/rename/remove slots): document the schema delta; `clean :slotomatic :jar`; `verify-module.sh
  --src`; **run `schema-risk.sh` (expect LOSSY/OUTAGE per B799) and BACK UP config.bog**; deploy to a non-prod
  station first; watch for `Out-of-date` (expected) → `Cannot set property` (each = a lost slot) → `Cannot load
  station` (fatal, roll back); JACE: STORED repack + project-CA re-sign. Risk HIGH — retyped-slot values are LOST.

## 807.6 — Kit implication → BUILD-LOOP §4 + versioning `[CERT-grounded]`
- **Build-task matrix** (§807.1) → BUILD-LOOP §4 "which task, when": annotations→`slotomatic+jar`; logic→`jar`;
  new slot→`slotomatic+schema-risk+jar`; version bump→`clean+jar`.
- **Mirror recipe with a cite** (§807.2): the station lock is `ArchiveSigningAction.kt:53-56` copying into
  `ModuleSigningConfigurationAction.kt:13`'s `niagara_home/modules`; `build/libs/<jar>` is already good → mirror
  or free the lock.
- **Version-bump checklist** (§807.5) tied to `schema-risk.sh` (B799) for MAJOR bumps.

## 807.7 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `jar` copies the signed jar to niagara_home/modules on doLast (the lock) | `[CERT]` | `ModuleSigningConfigurationAction.kt:13`; `ArchiveSigningAction.kt:53-56` | Y — grep decompile |
| 2 | Reload = timestamp check → "Module changed" → rebuild → bog re-decode | `[CERT]` | `NRegistry.java:234,268`; `RegistryChecksum.java:44-45` | Y — grep organized/ |
| 3 | Retyped/missing slot → warningAndSkip; unloadable → Cannot load station + exit(-5) | `[CERT]` | `ValueDocDecoder.java:497,502,675`; `Station.java:211-212` | Y — grep, matches B800 §800.8 |
| 4 | vendorVersion set at VendorExtension:88, stamped at ModuleXml:264; dep floor = min (strip 2) | `[CERT/CERT-doc]` | `VendorExtension.kt:88`; `ModuleXml.java:74,264`; devguide modules.txt | Y |
| 5 | slotomatic rewrites AUTO region from annotations; mandatory before jar | `[CERT/CERT-doc]` | `SlotomaticTask.java:110`; devguide | Y |

**Tally:** `[CERT]` ×4 · `[CERT-doc]` ×1. Decompiled plugin cites are `extern` (not verify-block-resolvable) — driver token-verified in the decompile + jar.

## 807.8 — Connections & open gaps
- [Block 793]/[Block 794] (build/scaffold), [Block 795]/[Block 799] (schema-risk = the MAJOR-bump risk), [Block 800] §800.8 (the live reload OUTAGE), [Block 806] (resource budget), `build-verify.md`.
- OPEN GAPS: (1) the copy is guarded by "signing aliases configured" — behavior with NO signing profile unverified;
  (2) `niagaraTest` 0-discovery-from-WSL mechanism (task class in a separate module, not decompiled);
  (3) `configureCleanTask()` body was an empty stub in the decompile — extra clean logic unresolved.
