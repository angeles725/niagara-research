# module-anatomy — Research State

> Operational state consumed by the loop (Research-SDD). Mirrored in engram
> (`research/niagara/module-anatomy/gaps`, `.../progress`). Visible and versionable source.
>
> **Focus angle (§16 / §b2).** The SKELETON of a Niagara N4 module — its structure, organization, and the
> mechanics of how it is BUILT and DISTRIBUTED — reconstructed from CODE (the reader/registry/classloader/
> install classes), not from docs. This is the code-side DEEPENING of [B12]/[B25], which are doc-side breadth
> blocks (they name `module.xml`, profiles, slotomatic, palette, lexicons from the devguide but never open the
> classes that READ them). Reference model built from REAL Tridium/Honeywell modules; the operator's own
> module `com.angeles.chihuahua` [B163–B177] is the CASE STUDY at synthesis — where it deviates from the
> reference skeleton = the concrete "how it can be improved" deliverable. READ-ONLY over disk (all confirmed
> sources under `organized/`); no live station needed (API2 available if a gap turns §12).
>
> Backlog from the AUDIT-FIRST coverage sweep 2026-08-29 (sonnet, 76 tool calls) with PRE-DECLARE
> REMITTANCES. Next block B629.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 625
gaps_closed: 1
known_gaps: 8
investigable_open: 7
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks**: 0 in this focus (corpus-wide count synced by the tool; global prefix `niagara-mental-model-bloque`)
- **Coverage metric**: 1 / 8 closed
- **Last iteration**: 2026-08-29 — bootstrap (AUDIT-FIRST sweep + backlog seeded)

## Remittances (already covered — cite, do NOT re-derive)

- **Manifest schema** (`module.xml` / `niagara-module.xml` elements: name/vendor/vendorVersion/preferredSymbol/runtimeProfile/`<dependency>`/`<modulePart>`/`<dirs>`/`<defs>`/`<types>`/`<lexicons>`) — [B12] §12.1.4-5 · [B76] §76 (verbatim control-rt/driver-rt manifests) · devguide `modules.txt`. NEW here = the READER classes, not the schema.
- **Runtime profiles** (rt/ux/wb/se/doc + the 5×5 permission matrix) — [B12] §12.1.6-7; `RuntimeProfile` enum javadoc at `docSource-doc/extracted/nre/javax/baja/nre/platform/RuntimeProfile.java`. NEW here = boot-time profile FILTERING (MA2).
- **Build** (gradle plugin `com.tridium.niagara-module`, tasks jar/slotomatic/sign/dist/bajadoc/niagaraTest, multi-profile layout) — [B12] (full) · [B176] (chihuahua real project) · [B25] (bajadoc gen).
- **Slotomatic / @NiagaraType annotations** (getters/setters + `module-include.xml` write) — [B12] §12.1.8 · [B176] · [B434]. NEW here = the runtime consumption of those entries (MA3).
- **Lexicons** (format, 4-level fallback, `%lexicon()%`, `lex!`, `$util.lexicon()`) — [B12] §12.2 (well covered; not reopened).
- **Signing chain / META-INF signing artifacts** (MANIFEST.MF hash tree, `.SF`/`.RSA`, detached `.sig`, cacerts P12, `nverify.exe`, TPK memcmp pin, per-module verdict) — REMIT ENTIRELY: [B18]/[B26]/[B113]/[B392]/[B482]/[B489]/[B492]/[B519]–[B524]/[B532]. MA4 cites for layout only.
- **Classloader isolation "safe-to-bundle" angle** (`ModuleClassLoader` delegation order, dependency-gated visibility) — [B617] §617.1-5. NEW here = the full boot scan → resolve order (MA2).
- **Palette format** (.palette = ZIP of BOG; datos no código) — [B12] §12.3.2 · [B203] (widget palette BOGs). NEW here = the runtime reader (MA6).
- **.dist format** (`meta-inf/dist.xml`, contents) — [B10] §10.3.2 · [B469] §469.2.
- **Supervisor-side fleet install** (`LocalInstallableRegistry`, combine-by-spec, passphrase-gated encrypted dist) — [B569] · [B579]. NEW here = the daemon-side install command (MA5).
- **Module-permissions architecture** (module.xml `<permissions>`, skipModuleValidation) — [B18]. NEW here = the `<permissions>` → `NiagaraPolicy` population pipeline (MA7).
- **Chihuahua module** (own `com.angeles.chihuahua` rt/ux/wb, RBAC write-gate, ES5 IIFE frontend, gradle multi-module build, slot-freeze 4.13→4.14) — [B163]–[B177]. The CASE STUDY compared against the reference at MA8.

## Gap-backlog

<!-- Priority: high | medium | low | deferred. Status leading token: pending | requires-execution |
     blocked-on-<reason> | ✅ | ~~. Sources confirmed by the AUDIT-FIRST sweep (file:line). -->

| Priority | Gap | Artifact type / source | Status |
|---|---|---|---|
| high | MA1 — the manifest READER: how `ModuleManifest(XElem)` parses `<types>`/`<dependencies>`/`<moduleParts>`/`<dirs>`/`<lexicons>` at install time, and how `BModulePart` (install-side) relates to `NModule` (runtime-side) — the install-vs-runtime dual representation of a module part | Java · organized/platform/platform-rt/.../install/installable/ModuleManifest.java + install/part/BModulePart.java | ✅ B629 — manifest parsed TWICE by independent readers: install-side ModuleManifest (platform-rt, Baja-serializable BModulePart; `<permissions>/<defs>/<lexicons>`→unknownElements) vs runtime-side NModule.readXml (baja); NO converter; BModule aggregates Map<RuntimeProfile,NModule> (first-part-wins header); type-res = literal `moduleName+":"+typeName` (NModule:226) |
| high | MA2 — the module BOOT scan: how the `modules/` dir is enumerated (`BootEnv.findModuleFile(name,profile)`), filtered by RuntimeProfile, and dependency-ordered (`ModuleManager.resolve()`/`loadModuleParts`), with `ClassScanner.scan(InputStream)` reading class bytecode to populate the registry | Java · organized/baja/baja/.../sys/BootEnv.java + sys/module/ModuleManager.java + sys/registry/ClassScanner.java | pending |
| high | MA3 — the TYPE-registration pipeline end-to-end: `@NiagaraType` → `NiagaraTypeProcessor` writes `<type>` in `module-include.xml` → boot `ClassScanner` populates `NModule.types[]` → runtime `NModule.loadType()` (`moduleName+':'+typeName`) / `BTypeSpec.getTypeInfo()` → `Registry.getType(spec)` | Java · organized/devkit/devkit-wb/.../annotation/processors/NiagaraTypeProcessor.java + baja/.../module/NModule.java + util/BTypeSpec.java + registry/Registry.java | pending |
| medium | MA4 — the physical JAR layout SKELETON: the complete entry map of a real module jar (`META-INF/module.xml`, MANIFEST.MF+.SF+.RSA [remit mechanism], `.class` by package, `module.palette` root, `<mod>.lexicon` + `lexicon/<lang>/`, embedded `rc/` icons, `.bajadoc`) — assembled once as the reference skeleton; `BModule` wraps the jar as a `BZipSpace` | Java · organized/baja/baja/.../sys/BModule.java + a real jar (devkit-wb) | pending |
| medium | MA5 — the daemon-side install command: how a module JAR moves from `!cleanDist`/registry to a station's `modules/` dir — `BModuleInstallable` (installable wrapper) → `BModuleInstallCommand` (platDaemon writes the jar) → restart handshake; continues [B569] past the supervisor transaction | Java · organized/platform/platform-rt/.../install/installable/BModuleInstallable.java + organized/platDaemon/platDaemon-rt/.../command/BModuleInstallCommand.java | pending |
| low | MA6 — the palette runtime reader: how Workbench discovers/exposes `module.palette` from a `BModule` via `BModulePaletteNode` (the nav node over the module zip space) — the load side of [B12] §12.3.2's format | Java · organized/baja/baja/.../sys/module/BModulePaletteNode.java | pending |
| low | MA7 — module `<permissions>` → Java security policy: how a module.xml `<permissions>` declaration ([B434]: devkit-wb has FilePermission/RuntimePermission) is wired into `NiagaraPermissionGroup`/`NiagaraPolicy`/`NiagaraPolicyUtil` for that module's classloader | Java · organized/baja/baja/.../sys/module/NModule.java (NiagaraPermissionGroup imports) + devguide-clean/security/security.txt | pending |
| high | MA8 — SYNTHESIS + CHIHUAHUA CASE STUDY: the reference module skeleton (from MA1-MA7 + remittances) as a single "how to build/distribute a module" model, then the operator's `com.angeles.chihuahua` [B163-B177] measured against it — every deviation named as a concrete improvement (manifest completeness, profile split correctness, type/permission declarations, dist/versioning hygiene). The focus deliverable, written at STOP | design synthesis over MA1-MA7 + [B163]-[B177] | pending |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 0 | 2026-08-29 | (bootstrap) AUDIT-FIRST sweep + backlog seeded | — | yes · sonnet (coverage sweep, 76 calls) | 8 |
| 1 | 2026-08-29 | MA1 manifest reader (install-vs-runtime dual) | B629 | yes · sonnet (4-class sweep) + inline verify | 0 |

## Blocked gaps (each tagged with what it needs)

- none — all 8 gaps are read-only investigable on disk (confirmed source paths from the sweep).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 7   ← the STATIC loop STOPS when this hits 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (focus reuses the existing decompiled corpus; no new census — subject artifacts already extracted under `organized/`)
