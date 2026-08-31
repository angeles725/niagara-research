# B722 — The WSL2 module build loop for N4 (clean / slotomatic / jar) — the real chihuahua process (module-dev-workflow addendum)

> Focus: **module-dev-workflow** (closed 5/5) · ADDENDUM block. Documents the ACTUAL WSL2 build loop used on
> the operator's real modules, and CLARIFIES a corpus imprecision (slotomatic vs the Robocopy signing bridge).
> Born while building the `ColdRoomPan` cold-room module from WSL2. Source = the real `chihuahua` project's
> living docs (FUENTE-3 empirical, [CERT]): `BUILD_WORKFLOW.md`, `CLAUDE.md`, `gradle.properties`; corroborated
> by engram build-policy memories. Paths under `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua`.

## 722.1 — The WSL2 build model

[CERT] N4 modules build with **Java 8** against a `niagara_home` install; the plugins live in
`$niagara_home/etc/m2/repository` and installed modules resolve as flat-file deps (`:baja`, `:nre`, …). The
build runs fine from a WSL2 (Ubuntu, ext4) shell — the working copy of `chihuahua` lives WSL-native and is
built there (`CLAUDE.md:6,11`). `gradle.properties` stores Windows `C:\` paths, so a WSL build MUST override
them on the command line (the paths don't exist inside WSL):

```
-Pniagara_home=/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162
-Porg.gradle.java.installations.paths=/usr/lib/jvm/java-8-openjdk-amd64
```
[CERT] `BUILD_WORKFLOW.md §2`; the JDK-8 path + `niagara_home` override are the two mandatory WSL overrides.
Cross-version is deliberate: build against one Niagara version, deploy to another (`CLAUDE.md:6,9`).

## 722.2 — The build tasks + modes

[CERT] Tasks run from the PROJECT ROOT (where `gradlew` lives), never from the inner part folder
(`BUILD_WORKFLOW.md`). The canonical loop:

```
./gradlew :<mod>-rt:clean :<mod>-rt:slotomatic :<mod>-rt:jar \
  -Pniagara_home=/mnt/c/.../OptimizerSupervisor-N4.14.0.162 \
  -Porg.gradle.java.installations.paths=/usr/lib/jvm/java-8-openjdk-amd64
```
Quick compile-only check while integrating code: `:<mod>-rt:compileModuleTestJava` with the same overrides
(`CLAUDE.md:23`). Build MODES [CERT] `BUILD_WORKFLOW.md §3`, engram #1284: only-ux changed → `:<mod>-ux:clean
:<mod>-ux:jar`; only-rt changed → `:<mod>-rt:clean :<mod>-rt:jar`; both → both. ALWAYS include `clean` — else a
cached `META-INF/module.xml` with stale `<types>` is packaged. Built jar lands in `$niagara_home/modules/<mod>-<part>.jar`;
deploy = copy it to the target station's `modules/` dir. chihuahua wraps build+backup+copy+md5-verify in
`./deploy.sh <rt|ux|all> [--bump]` — runnable from WSL, no Windows needed (`CLAUDE.md:11-24`). A fresh wizard
project (e.g. `ColdRoomPan`) has NO `deploy.sh`; use the raw `gradlew` tasks above.

## 722.3 — Slotomatic runs in WSL (myth refuted) + the Robocopy clarification

[CERT] `BUILD_WORKFLOW.md §12` (L423-428) and §8 anti-patterns (L310): **"slotomatic requires Windows" is
FALSE — slotomatic runs in WSL with the `-P` overrides.** It regenerates the AUTO GENERATED region (slot
constants, `getXxx()`/`setXxx()`, Action dispatch) and updates the class hash marker. Run it ONLY when a
`@Niagara*` annotation changed (`BUILD_WORKFLOW.md §12` "cuándo se necesita"). "Cannot transform … no metadata"
warnings for `srcTest/` files are expected/benign (L465; `CLAUDE.md:45`).

**§14 CLARIFICATION of a corpus imprecision.** `docs/module-dev-workflow.md` (citing [B639]) frames it as
"Robocopy WSL→Win→WSL bridge **for slotomatic** + jar." That conflates two things. [B639]'s [CERT] quote is
accurate for what it cites — the `ng-deploy.sh` (niagara-tools) proposal, whose Robocopy bridge exists because
the **signing security store (`keystore.jceks`) is on the Windows side** (`proposal.md:320`). But **slotomatic
itself does NOT need the bridge** — it runs directly in WSL (chihuahua §12, empirical). chihuahua's own
`deploy.sh` builds AND signs in WSL without Robocopy: the keystore in `niagara_user_home` is reachable via
`/mnt/c`. So: the bridge (when used at all) is a SIGNING-STORE concern of one specific wrapper, not a slotomatic
requirement. Empirical (`BUILD_WORKFLOW.md`) wins over the runbook framing (protocol: código/empirical > doc).

## 722.4 — Version bump + activation (operator-only)

[CERT] `--bump` (raise `defaultModuleVersion` in root `build.gradle.kts`) is MANDATORY when adding/changing a
**frozen slot** (`@NiagaraProperty`/`@NiagaraAction`), else the new slot won't appear on an already-instantiated
component (`BUILD_WORKFLOW.md §10,§12`; `CLAUDE.md:19,37`). Activation steps are the OPERATOR's, not the
builder's (`CLAUDE.md:27-37`): restart the station (Java changes + frozen slots); if a frozen slot still doesn't
appear after restart, **close and reopen Workbench** (its client type cache doesn't refresh on station restart —
confirmed 2026-05-25); resource-only (`rc/` JS/CSS/HTML) needs only a browser refresh (Ctrl+Shift+R), no restart.

## 722.5 — Tests are documentation-only + gotchas

[CERT] Do NOT gate the build on `gradle test` / `niagaraTest`: plugin `com.tridium.niagara-module` 7.6.17 has a
bug — `moduleTestAnnotationProcessor` produces no metadata for `writeTestModuleXml`, so `@NiagaraType` tests are
silently skipped (`Total tests run: 0`). Tests in `srcTest/` are compilable documentation skeletons; pure-JUnit
type-a suites run standalone via `run-tests-wsl.sh`. (engram #1284, #1347; `chihuahua-rt.gradle.kts:38-49`.)
Gotcha: `compileModuleTestJava` pollutes `module-include.xml` with test types → `git checkout` that file before
commit (`CLAUDE.md:44`).

## Connections

- Focus `module-dev-workflow` runbook (`docs/module-dev-workflow.md`, [B711]-[B715]) — this block adds the
  empirical WSL2 command-level loop + corrects the slotomatic/bridge framing. Signing store → [B639]. Slot
  frozen / activation → [B637]. Real example `chihuahua` → focus `chihuahua` [B169]. Slotomatic codegen pipeline
  → [B712]. Applied while building `ColdRoomPan` (paccadia project).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | WSL build needs `-Pniagara_home` + `-Porg.gradle.java.installations.paths` (JDK 8) overrides | [CERT] | BUILD_WORKFLOW.md §2; gradle.properties | cited |
| 2 | canonical loop = clean + slotomatic (on annotation change) + jar, from project root | [CERT] | BUILD_WORKFLOW.md §3,§12 | cited |
| 3 | slotomatic runs in WSL; "requires Windows" is a refuted myth | [CERT] | BUILD_WORKFLOW.md §8 L310, §12 L423-428 | cited |
| 4 | the Robocopy bridge is a signing-store concern (ng-deploy), NOT a slotomatic requirement | [CERT]/[INFER] | B639 proposal.md:320 vs BUILD_WORKFLOW.md §12 | resolved |
| 5 | `--bump` mandatory for a new/changed frozen slot | [CERT] | BUILD_WORKFLOW.md §10; CLAUDE.md:19,37 | cited |
| 6 | tests documentation-only (plugin 7.6.17 bug); module-include.xml test-pollution gotcha | [CERT] | engram #1284/#1347; CLAUDE.md:44 | cited |

**Tally:** [CERT] ×5 · [CERT]/[INFER] ×1 (claim 4, contradiction resolution). Block TYPE = artifact/process
documentation (FUENTE-3 empirical real project + engram corroboration).

## Open gaps

- **B722-G1 CONFIRMED (2026-08-30, [CERT-live]).** A WSL2-native build of `ColdRoomPan-rt` signed successfully
  with ONLY `-Pniagara_home=/mnt/c/...` + `-Porg.gradle.java.installations.paths=/usr/lib/jvm/java-8-openjdk-amd64`
  — no `niagara_user_home` override, no Robocopy bridge. The jar carries `META-INF/NIAGARA4.SF` + `NIAGARA4.RSA`,
  signed by the auto-generated default DEV profile (`CN=…(Niagara4Modules)`, OU "For Development Purposes Only",
  self-signed). So slotomatic AND signing run entirely in WSL for a dev cert. Residual (deploy-time, not
  build-time): a LOCKED OEM (Honeywell) station may not TRUST a self-signed dev cert — production deploy may need
  the ANGELES production cert or the station trusting this one. That is a trust/deploy concern, not a WSL build
  limitation.
