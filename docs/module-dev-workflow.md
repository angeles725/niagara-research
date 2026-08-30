# Niagara N4 Module Development Workflow — Runbook

> The end-to-end process and tool mechanics for building N4 modules — what each tool does and the
> edit→build→sign→deploy→test loop. Companion to `module-best-practices.md` (which gives the rules; this gives
> the process). Every step traces to a research block ([Block N]). Focus: `module-dev-workflow` (B711+).

---

## 1. The toolchain — what each tool is

### 1.1 The two homes
- **`niagara_home`** — the SDK install (`modules/`, `devkit`, gradle plugins under `etc/m2`). The active SDK home
  selects the **target version** (4.13/4.14/4.15). [B638]
- **`niagara_user_home`** — holds `security/keystore.jceks` (the signing keys the niagara-signing plugin uses). [B639]

### 1.2 The gradle-niagara plugins
- `com.tridium.niagara-module` (per part): compile → run `slotomatic` → jar with `META-INF/module.xml`.
- `com.tridium.niagara-signing` (root + parts): auto-signs the jar; no explicit config block. [B639]

### 1.3 Slotomatic — the codegen tool (NOT an annotation processor)
- Reads `module-include.xml` + your `@Niagara*` declarations (JavaParser source model, not JSR-269).
- Writes the generated slot region into your `.java` between `/*+ BEGIN BAJA AUTO GENERATED +*/` markers.
- Guards: a class not listed in `<type>` is not processed. [B631]

### 1.4 ng-deploy.sh — the deploy wrapper
- `backup → ./gradlew (mode A/B/C) → copy jars to STATION_MODULES_DIR → verify emitted types vs EXPECTED_*_TYPES`
  (phase exit codes 10/20/30/40/50). Uses a Robocopy WSL→Win→WSL bridge for the Windows-side security store. [B637, B639]

### 1.5 Devkit wizards
- Scaffold the module + `module-include.xml` + `<type>` entries + skeleton `.java`. [B631]

### 1.6 Tool → job quick-reference

| Tool | Does | Invoke |
|---|---|---|
| devkit wizard | scaffolds module + `<type>` list | once, at creation |
| `@NiagaraType`/`@NiagaraProperty` | declares types + slots | you write |
| `module-include.xml` | the `<type>` list Slotomatic reads | author/scaffold |
| Slotomatic (`:slotomatic`) | writes the AUTO slot region | when a `@Niagara*` changed |
| `niagara-module` | compile + jar + manifest | every build |
| `niagara-signing` | signs the jar from user-home keystore | automatic |
| `ng-deploy.sh` | backup→gradlew→copy→verify | to deploy |

---

---

## 2. The codegen round-trip (`@NiagaraType` → runtime type)

Five stages, from the annotation you write to a resolvable `moduleName:typeName`:

1. **Input:** `@NiagaraType` on the class + `<type name= class=>` in `module-include.xml`. Slotomatic READS the
   xml + your `@Niagara*` and WRITES the AUTO slot region into your `.java`.
2. **RegistryDatabase:** the `<type>` becomes an `NTypeInfo` (name + class-name string, no Class yet).
3. **NModule.types:** held as a className string, lazily.
4. **Resolve:** `BTypeSpec.resolve("mod:Type")` → the `NTypeInfo`.
5. **Load once:** `NModule.loadClass` loads the Class via `ModuleClassLoader`; `TYPE = Sys.loadType(...)`
   self-registers `NType`. Types load lazily, once, on first resolve — not at boot. [B631]

**The AUTO region** (between `/*+ BEGIN BAJA AUTO GENERATED +*/` markers) holds the slot constants, getters/
setters, action stubs, and `TYPE`. It carries a hash — add a `@Niagara*` slot without re-running Slotomatic and
the hash/constants go stale (compile errors / missing slots). **Never hand-edit inside the markers.** [B631, B650]

**The guard:** `Cannot update <file>; it is not in module-include.xml` — a class not listed in `<type>` is dead
bytecode (no slots, `resolve` never finds it, no error points at the omission). `module-include.xml` is the
driving registry; it is an INPUT you author, not annotation-processor output. [B631]

**Round-trip rule:** new type → add `<type>` + run `:slotomatic`; rename/remove → update `module-include.xml`
too; a forgotten `<type>` fails silently.
