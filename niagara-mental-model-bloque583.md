# Block 583 — Template file resolution & staging: `TemplateManager` is a singleton that resolves a template by name/uID+vendor across THREE directories in priority order (user → module → application), and a `memory:` ORD scheme backs an in-memory file space for staging — closing the template focus

**Session**: 2026-08-28
**Focus**: `template` (gap T7 — TemplateManager resolution + the memory scheme; the final gap, closes the focus)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `TemplateManager` + `BMemoryScheme` + `BMemoryFileSpace` +
`BNewNtplFromTemporary`; resolution order and the memory scheme token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-rt/vineflower/com/tridium/template/file/{TemplateManager,BMemoryScheme,
  BMemoryFileSpace,BNewNtplFromTemporary}.java`.

**Scope**: where template files live, how one is found, and how they are staged in memory during make/deploy.
Closes T7 and the `template` focus. Deepens [Block 200 §200.1] (`.ntpl` on disk). Does NOT re-open the zip
structure ([Block 200 §200.1]).

---

## 583.1 `TemplateManager` — a singleton with three directories, resolved in priority order [CERT]

`TemplateManager` is a singleton (`INSTANCE` `[CERT] :43`) holding THREE directories and a cache map for each
`[CERT] :34-42`: `templateDir` + `tInfo`, `modDir` + `mInfo`, `applicationDir` + `aInfo` (each a
`Hashtable<FilePath, TemplateInfo>`). Lookup is priority-ordered `[CERT] :47-63`:
```java
TemplateInfo thisInfo = findTemplate(this.tInfo, templateName, vendor);   // 1. user templateDir
if (null) thisInfo = findTemplate(this.mInfo, templateName, vendor);      // 2. module-shipped modDir
if (null) thisInfo = findTemplate(this.aInfo, templateName, vendor);      // 3. applicationDir
```
So a template name+vendor (or `uID`+vendor `[CERT] :69-81`) resolves **user directory first, then
module-provided templates, then the application directory** — a user template shadows a module one of the same
name. `getTemplateDir()` resolves through the ORD shorthand `file:~templates/` (`Pattern
"file:~templates/"` `[CERT] :45`). This is the resolution the subtemplate `locationOrd` ([Block 581] T4) and the
provisioning `^templateCache` ([Block 573]) ultimately go through.

## 583.2 The `memory:` ORD scheme and in-memory staging [CERT]

`BMemoryScheme extends BOrdScheme`, `ordScheme = "memory"` `[CERT] :14-26` registers a `memory:` ORD scheme.
`resolve(base, query)` `[CERT] :33-36` returns an `OrdTarget` into `BMemoryFileSpace.INSTANCE.resolveFile(path)`.
`BMemoryFileSpace extends BFileSpace`, `ABS_ORD = BOrd.make("memory:")`, `makeMemoryStore(MemoryPath)`
`[CERT] :29-55`. So there is a full in-memory `BFileSpace` addressable by `memory:` ORDs — used to STAGE a
template's files (bog + px + images) while it is being assembled or deployed, without touching disk. This is what
the T1 ([Block 577]) `TemplateSource` `AutoCloseable` contract releases on `close()`, and what [Block 200]
called `BMemoryFileSpace` staging.

## 583.3 `BNewNtplFromTemporary` — a .ntpl over a transient temp file [CERT]

`BNewNtplFromTemporary extends BObject implements BINtplFile` `[CERT] :19` wraps a `.ntpl` built into a
TEMPORARY file (holds the `TemplateManifest`; logs `"Failed to delete temporary file"` `[CERT] :121` on cleanup
failure). So a freshly-created template can be materialized to a temp file, used as a `BINtplFile`, and the temp
is deleted after — the transient build artifact of the make pipeline ([Block 577] `save`).

## 583.4 Focus close — the template engine, end to end [CERT-synthesis]

With T7 the `template` focus is complete. The engine, as the seven blocks now document it: a `.ntpl` is a
zip(bog+manifest) ([Block 200]); the **manifest** is a declarative index with a typed parameter model ([Block
580] T3) and subtemplate references ([Block 581] T4); the **API** (`NiagaraTemplate` + `TemplateSource` strategy,
[Block 577] T1) creates/reads them, staging in a `memory:` file space and resolving through the priority-ordered
`TemplateManager` (this block); **install** applies a template under a `ReplacingContext` with a module-compat
gate ([Block 578] T2); **upgrade** is a save→transfer→restore transaction that preserves wiring by handle ([Block
579] T5) and cascades to subtemplates version-gated ([Block 581]); and the whole thing is driven remotely by a
single streaming Fox circuit ([Block 582] T6). It is a coherent, well-factored config-reuse subsystem.

## 583.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | TemplateManager singleton with 3 dirs (templateDir/modDir/applicationDir) + 3 cache maps | [CERT] | TemplateManager.java:34-43 | token-checked ✓ |
| 2 | Resolution priority: tInfo(user) → mInfo(module) → aInfo(application) for name+vendor and uID+vendor | [CERT] | :47-81 | token-checked ✓ |
| 3 | `file:~templates/` ORD shorthand pattern for the template dir | [CERT] | :45 | token-checked ✓ |
| 4 | BMemoryScheme registers `memory:` OrdScheme → BMemoryFileSpace (BFileSpace) in-memory staging | [CERT] | BMemoryScheme.java:14-36; BMemoryFileSpace.java:29-55 | token-checked ✓ |
| 5 | BNewNtplFromTemporary = BINtplFile over a temp file, deleted after use | [CERT] | BNewNtplFromTemporary.java:19,121 | token-checked ✓ |
| 6 | Focus-close synthesis of the end-to-end template engine | [CERT-synthesis] | B200 + B577-B582 | cross-ref ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 577]** (T1) — the `TemplateSource` AutoCloseable staging released here as the `memory:` file space.
- **[Block 573]** (PV7) — provisioning's `^templateCache` resolves through this TemplateManager.
- **[Block 581]** (T4) — subtemplate `locationOrd` resolution goes through this priority order.
- **[Block 200 §200.1]** — the `.ntpl` on disk; this is where it is found and staged.

## Open gaps (this block)

- The `TemplateInfo` cache-invalidation timing (`updateTemplateMap` re-scan trigger) is named, low value.
  **T7 CLOSED; template focus investigable=0 → STOP.** Workbench `ui/` (44 classes) remains the only unopened
  template area — UI, out of the engine angle (candidate low-priority tail if ever wanted).
