# Block 434 — devkit-wb is NOT an SDK: it is the Niagara Developer Kit tooling — module/driver wizards, Slotomatic, and lexicon tools, Workbench-only

> Research of **`devkit-wb`** (focus `workbench`, gap WB08) — audited under the premise "the Workbench
> extension SDK." That premise is REFUTED: devkit-wb is the Niagara **Developer Kit tooling** module
> (code-generation wizards + Slotomatic + lexicon tools), Workbench-only, and most of its bulk is a bundled
> parser library. Scope: what it actually is, its four tool subsystems, its runtime gate, and its relation to
> the build/compile pipeline. Does NOT cover the base extension contracts (those live in `bajaui`/`workbench-wb`
> — [Block 427], [Block 432]).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `devkit-wb.jar`
> sha256 `52f440e35758c1a8575528877b3867a8891ee497966594fc8e0260cdc9c06dcf`.
>
> Sources: Vineflower impl (`sources/decompiled/devkit-wb/`) + `META-INF/module.xml`. Method: package census
> + module.xml + entry-class reading, all re-verified live. CAVEAT: some `slottool` classes decompile with a
> mangled class-name token (`Slotomatic` → `public final ln`) — file names + structure are real, so those are
> cited by file/existence. Markers: `[CERT]` (`file:line`) · `[INFER]` deduction.
>
> Workbench tooling. Connects [Block 426] (Slotomatic generates the auto-region the Program/module compiler
> then compiles), [Block 428] (`BLexiconTool` is a `BWbNavNodeTool` in the Tools menu), [Block 12] (build
> lifecycle — devkit drives it as a developer, does not hook the station's).

---

## 434.1 — Premise refuted: dev tools, not an SDK (and not examples) `[CERT]`

The gap assumed an "extension SDK." The package census refutes it: `[CERT]`

| Package | .java | Nature |
|---|---|---|
| `com.github.javaparser.*` | **506** | bundled open-source JavaParser (a dependency, NOT Tridium code) |
| `com.tridium.slottool.*` | 131 | Slotomatic slot code-generator |
| `com.tridium.devkit.*` | 29 | the wizards + lexicon tools + codegen util |
| `com.tridium.gradle.*` | 17 | Velocity template engine for Gradle scaffolding |

So of the "683" headline classes, **506 are a vendored JavaParser** and only ~177 are Tridium. `[CERT]`
devkit-wb exposes **zero** developer-contract base classes/interfaces and ships **zero** example modules — it
is DEV TOOLING. `[CERT]`/`[INFER]` `module.xml` names it "**Niagara Developers Kit**", `preferredSymbol="dev"`.
`[CERT]`

## 434.2 — Workbench-only, autoloaded, broad filesystem rights `[CERT]`

`module.xml:2`: `runtimeProfile="wb"`, `autoload="true"`, `nre="true"`. `[CERT]` `runtimeProfile="wb"` is
definitive — **devkit-wb never loads on a station**, only in a Workbench-capable NRE, and autoloads when
present. `[INFER]` there is no license/feature gate in the XML — installing the devkit IS the gate (it ships in
the separate developer installer, not the base station). Its permissions include
`FilePermission <<ALL FILES>> read,write,delete` and `RuntimePermission exitVM.*` (`module.xml:84`,`:86`).
`[CERT]` `[INFER]` consistent with a tool that scaffolds arbitrary directories on the developer's disk — a
notable trust surface if this module were ever present on a production host (it is not, by `runtimeProfile`).

## 434.3 — The four tool subsystems `[CERT]`

1. **New Module Wizard** — `BNewModuleTool` → `NewModuleWizard`
   (`sources/decompiled/devkit-wb/com/tridium/devkit/wizards/NewModuleWizard.java`). A 3-step wizard (metadata
   + runtime profiles → dependencies → packages) that on finish drives a `VelocityGenerator` +
   `GradleProjectGenerator` + `NiagaraModuleGenerator` to scaffold a full Gradle multi-part module on disk.
   `[CERT]` It reads an existing `build.gradle.kts` to pre-fill vendor/version if a parent project exists.
   `[INFER]`
2. **New Driver Wizard** — `BNewDriverTool` (`.../wizards/BNewDriverTool.java`) → NDriver / Video / Either
   variants, substituting the `Nfoo` placeholder in the `gradle/ndriver/` + `gradle/videodriver/` Velocity
   `.vm` templates. `[CERT]`
3. **Slotomatic** — `com.tridium.slottool.Slotomatic` (`.../slottool/Slotomatic.java`, class token mangled).
   The Baja slot code-generator: reads annotated Java (its own JavaCC `BajaParser` + the bundled JavaParser)
   and generates/updates the `// AUTO-GENERATED` region in `BComponent` subclasses (Property/Action/Topic/
   Enum/OrionType/Singleton processors). `[CERT]` `[INFER]` this is the SAME Slotomatic the Niagara Gradle
   plugin runs at build time — shipped here so Workbench can trigger it interactively.
4. **Lexicon tools** — `BLexiconTool extends BWbNavNodeTool` (`.../devkit/ui/lexicon/BLexiconTool.java:13`), a
   Tools-menu nav-node tool ([Block 428]) whose agents are `BLexiconEditor` (edit `.lexicon` files),
   `BLexiconModuleBuilder` (compile lexicons → a lexicon jar), `BLexiconModuleMigrator`, and `BLexiconReport`.
   `[CERT]` Plus `PaletteGenerator` (`.../devkit/util/PaletteGenerator.java`) — emits a `module.palette` from
   `src/`. `[CERT]`

## 434.4 — Not a build-lifecycle hook `[CERT]`/`[INFER]`

A search for `toolPolicy`/`CompileContext`/build-pipeline hooks across devkit's Tridium classes returned zero.
`[CERT]` devkit-wb DRIVES Slotomatic (the developer-side build step) as an interactive API call, but does not
hook the station's build/compile lifecycle ([Block 12], [Block 426]). `com.tridium.gradle.*` is a Velocity
template engine that EMITS Gradle build files for new modules — it is a code generator targeting Gradle, not a
Gradle plugin itself. `[CERT]`/`[INFER]`

## 434.5 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | devkit-wb = dev TOOLING, not SDK/examples; 506 of 683 classes are bundled JavaParser | `[CERT]` | package census; §434.1 |
| 2 | `module.xml` = "Niagara Developers Kit", `runtimeProfile="wb"` (never on station), autoload | `[CERT]` | `module.xml:2` |
| 3 | Permissions `<<ALL FILES>>` + `exitVM.*` — scaffolds arbitrary dirs | `[CERT]` | `module.xml:84`,`:86` |
| 4 | New Module Wizard → Velocity/Gradle/NiagaraModule generators scaffold a module | `[CERT]` | `wizards/NewModuleWizard.java` |
| 5 | New Driver Wizard → NDriver/Video `.vm` templates (`Nfoo` placeholder) | `[CERT]` | `wizards/BNewDriverTool.java` |
| 6 | Slotomatic = slot code-gen for the `// AUTO-GENERATED` region (same as the Gradle build step) | `[CERT]` | `slottool/Slotomatic.java` |
| 7 | `BLexiconTool extends BWbNavNodeTool` (Tools menu); + PaletteGenerator | `[CERT]` | `ui/lexicon/BLexiconTool.java:13` |

**Marker tally**: `[CERT]` ≈ 17 · `[INFER]` 7 ([INFER]/[CERT] ≈ 0.41). Type: **EVIDENCE block** (survey +
premise correction) — ratio healthy. VERIFY-BEFORE-ACTING: the "SDK" premise was REFUTED by the package census
before writing; `runtimeProfile="wb"`, the JavaParser count, and each entry class were re-verified live. Mangled
`slottool` class tokens (`Slotomatic`→`ln`) are cited by file/existence, not by an invented line body. Tokens
confirmed: `runtimeProfile="wb"`, `Niagara Developers Kit`, `<<ALL FILES>>`, `BNewModuleTool`/`BNewDriverTool`,
`Slotomatic`, `BLexiconTool extends BWbNavNodeTool`.

## 434.6 — Connections

- **[Block 426]** — Slotomatic generates the code (`// AUTO-GENERATED` slot region) that the Program/module
  compiler then compiles; devkit is the generator, B426 is the compiler.
- **[Block 428]** — `BLexiconTool` is a `BWbNavNodeTool`, one of the Tools-menu nav-node tools the shell hosts.
- **[Block 12]** — the module build lifecycle; devkit drives it as a DEVELOPER convenience (interactive
  Slotomatic + Gradle scaffolding), it does not hook the station's build machinery.
- **coverage** — this closes the audit's "Dev & test tools" cluster's devkit half; `test-wb` (the CppUnit/BTest
  harness) is a separate item already touched in [Block 130]/[Block 257].

<!-- research-block: focus workbench, gap WB08 (devkit-wb) — CLOSED at body grade; premise "SDK" REFUTED → dev tooling -->
