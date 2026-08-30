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

*(codegen mechanics, authoring artifacts, the dev loop, and test/debug are added as WF2–WF5 close.)*
