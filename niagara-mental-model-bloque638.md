# Niagara N4 — own-modules-audit (OMB2): version-targeting is the `niagara_home` SDK PATH — the build compiles against whatever Niagara install that path points at (iSMA 4.13.2 SDK), deploys to a different station (Honeywell 4.14); switching 4.13/4.14/4.15 = repoint the path + match the plugin version (§14 reframes B636 dev#2)

**Focus**: own-modules-audit · **Gap**: OMB2 (version-targeting via compilation path) · **Session**: 2026-08-29 · **Block**: B638
**Sources** (`[CERT]` real operator source):
- `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/settings.gradle.kts`
- `.../chihuahua/build.gradle.kts` · `.../chihuahua/gradle.properties` · `.../chihuahua/BUILD_WORKFLOW.md`

**Scope**: the operator's statement "cambia la versión del gradle, se tiene que apuntar a la versión" and "se solían utilizar path para la compilación" — resolved to the concrete mechanism. Reference boot/profile side = [B630] (REMIT).

---

## 638.1 The one knob: `niagara_home` is the SDK you compile against

`[CERT]` `settings.gradle.kts:9-19` — `niagara_home` resolves through a 4-level chain (used for `pluginManagement` AND the dependency repos):
```kotlin
val niagaraHome = providers.gradleProperty("niagara_home")
  .orElse(providers.systemProperty("niagara_home")
  .orElse(providers.environmentVariable("NIAGARA_HOME")
  .orElse(providers.environmentVariable("niagara_home"))))
// plugin repo derived from it:  niagaraHome.map { "$it/etc/m2/repository" }   (:19)
```
`[CERT]` `build.gradle.kts:52-63` mirrors the same chain and points the dependency `flatDir` repos at that install:
```kotlin
val niagaraHomeForRepos = findProperty("niagara_home") ?: System.getProperty(...) ?: System.getenv("NIAGARA_HOME") ...
repositories { flatDir { dirs("$niagaraHomeForRepos/bin/ext", "$niagaraHomeForRepos/modules") } }
```
So **every framework dependency (`:baja`, `:nre`, `:web-rt`, …) is resolved from `niagara_home/bin/ext` + `niagara_home/modules`** — i.e. from whatever Niagara install the path points at. "El path para la compilación" is literally this: the module compiles against the jars in the `niagara_home` SDK directory.

Current value `[CERT]` `gradle.properties:18-21`:
```properties
niagara_home=C:\Niagara\iC-Niagara-4.13.2.18                       # the iSMA CONTROLLI 4.13.2 SDK
niagara_user_home=C:\Users\equipo\Niagara4.13\iSMA CONTROLLI
org.gradle.java.installations.paths=C:\Program Files\Zulu\zulu-8   # Java 8 (hard requirement, auto-detect OFF)
```
WSL override (`BUILD_WORKFLOW.md:147-148`): the same three as `-P` flags pointing at `/mnt/c/Niagara/iC-Niagara-4.13.2.18` and `/usr/lib/jvm/java-8-openjdk-amd64`.

---

## 638.2 Compile-against ≠ deploy-to (the intentional cross-version split)

`[CERT]` `BUILD_WORKFLOW.md:607-613` — the operator **compiles against the iSMA 4.13.2 SDK** but **deploys to the Honeywell OptimizerSupervisor-N4.14.0.162 station** (`modules/`). This is deliberate, and it is WHY [B632] saw chihuahua's manifest declare `<dependency name="baja" vendorVersion="4.13">`: the dependency floor is inherited from the SDK the build resolved against, not chosen by hand. A 4.13-compiled module runs on a 4.14 station because N4 accepts a lower dependency floor ([B630] §630.4 `checkBajaVersion`).

**§14 reframe of [B636] deviation #2.** B636 listed "builds against baja 4.13 while station is 4.14" as a deviation to bump when dropping 4.13 targets. With the real source: this is **not an oversight — it is the compile-against-SDK model**. Building against the OLDER (4.13.2) SDK is a deliberate portability choice: the module then loads on 4.13, 4.14, AND 4.15 stations (lowest common SDK). Bumping the floor to 4.14 would DROP 4.13 stations. The correct framing: the baja floor = the SDK you compile against, chosen for the widest station range you must support. (Back-pointer added to B636.)

---

## 638.3 Switching 4.13 → 4.14 → 4.15 (two coupled changes, no profile system)

`[CERT]` `settings.gradle.kts:84-99` — the Tridium plugin version is a hardcoded string applied to every plugin:
```kotlin
val gradlePluginVersion = "7.3.40"    // com.tridium.niagara / vendor / niagara-module / niagara-signing
val settingsPluginVersion = "7.3.0"   // com.tridium.settings.*
```
and it is resolved ONLY from `niagara_home/etc/m2/repository` (§638.1). So the plugin version **must match what the target SDK install ships**. Switching Niagara version is therefore TWO coupled edits:
1. Repoint `niagara_home` (gradle.properties or `-P`/env) at the new SDK install directory.
2. Update `gradlePluginVersion` in `settings.gradle.kts` to the plugin version that install provides (e.g. a 4.14/4.15 SDK ships `7.6.x`).

There is **no profile system, no per-version gradle.properties** — version is a single property + a matching plugin string. This is the operator's "cambia la versión del gradle, se tiene que apuntar a la versión": the gradle plugin version is coupled to the SDK path, and both move together.

**The 7.6.17 hazard** ([B637] §637.3): a newer SDK ships plugin `7.6.17`, whose `moduleTestAnnotationProcessor` bug breaks `niagaraTest`. So a naive "upgrade to the 4.14/4.15 SDK" also swaps in the broken test plugin — a reason the operator stays on the 4.13.2 SDK (plugin 7.3.40) for building while deploying to 4.14. Version-targeting is not just a path; the plugin behavior changes with it.

---

## 638.4 What this establishes + how to improve

- **The "paths for compilation" are documented now**: `niagara_home`/`niagara_user_home` (+ the Java-8 Zulu path) select the SDK; dependencies + plugins both come from there.
- **The 4.13-compile / 4.14-deploy split is correct**, not a bug — it maximizes station coverage. Keep it unless you need 4.14-only API.
- **Improvement**: since switching versions is two coupled edits (path + plugin string) with no profile system, a small `gradle.properties.4.13/4.14/4.15` set (or a `-Pniagara.target=` that maps to both the path and the plugin version) would make version-switching one action and prevent a path/plugin-version mismatch (which fails at plugin resolution). Also pin/avoid plugin 7.6.17 for any build that must run `niagaraTest` ([B637]).
- **Java 8 is a hard floor** (`auto-detect=false`, Zulu 8 path) — N4 modules must compile on Java 8 ([B637] noted the Java-9 `Map.of` ban); document it in the shop template.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | niagara_home resolves via gradle-prop→sys-prop→NIAGARA_HOME→niagara_home (settings + build) | [CERT] | settings.gradle.kts:9-14 · build.gradle.kts:52-56 | ✅ read verbatim |
| 2 | dependency repos = flatDir(niagara_home/bin/ext, niagara_home/modules) → deps come from the SDK path | [CERT] | build.gradle.kts:61-63 | ✅ read verbatim |
| 3 | gradle.properties points niagara_home at iC-Niagara-4.13.2.18; Java 8 Zulu, auto-detect off | [CERT] | gradle.properties:18-21,36-38 | ✅ read verbatim |
| 4 | compile against 4.13.2 SDK, deploy to Honeywell 4.14 station (intentional split) | [CERT] | BUILD_WORKFLOW.md:607-613 | ✅ read |
| 5 | plugin version 7.3.40 hardcoded, resolved only from niagara_home/etc/m2 → must match SDK | [CERT] | settings.gradle.kts:19,84-99 | ✅ read verbatim |
| 6 | switching version = repoint niagara_home + update gradlePluginVersion; no profile system | [CERT]/[INFER] | settings.gradle.kts:84-99 + gradle.properties (single prop) | ✅ read+derive |
| 7 | B636 dev#2 (baja 4.13) is deliberate compile-against-SDK, not an oversight | [INFER] §14 | §638.2 + [B632]/[B636] | ✅ derived |

**Tally**: [CERT] ×5 · [INFER] ×2 · build-process block. §14 reframe issued to [B636] dev#2 (back-pointer added). All path/version citations token-checked verbatim.

## Connections

- **§14 REFRAMES [B636]** deviation #2 (baja 4.13 = deliberate SDK-target, not oversight). Back-pointer added to B636.
- **[B630]** — the station-side profile/version resolution (this is the BUILD-side counterpart). **[B632]** — the 4.13 dependency in chihuahua's manifest, explained. **[B637]** — the 7.6.17 plugin hazard tied to SDK choice.
- Forward: OMB3 (ANGELES signing + niagara-tools).

## Gaps uncovered

- None new. 4.14/4.15 SDK installs are not on this disk to diff their plugin versions directly — the 7.6.17-vs-7.3.40 split is [CERT] from the operator's own notes ([B637]); confirming exact 4.15 plugin version would need that SDK (not blocking).
