# Niagara N4 — own-modules-audit (OMB1): the operator's REAL build workflow — three deploy modes (A/B/C), the Clean+Slotomatic+Build vs Clean+Build variant rule, and the verified verdict on tests (niagaraTest is dead by a plugin 7.6.17 bug; pure-JUnit logic tests DO work)

**Focus**: own-modules-audit · **Gap**: OMB1 (real build workflow + variants + tests) · **Session**: 2026-08-29 · **Block**: B637 · **Type**: DOCUMENT/build-process (from real operator source).
**Sources** (`[CERT]` real operator source, operator-pointed 2026-08-29):
- `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/BUILD_WORKFLOW.md`
- `.../chihuahua/build.gradle.kts` · `settings.gradle.kts` · `chihuahua/chihuahua-rt/chihuahua-rt.gradle.kts`
- `.../chihuahua/chihuahua/run-tests-wsl.sh` · `build-and-deploy.ps1` · `deploy.sh` · `inspect-{build,jars}.ps1`
- `/home/cristian/modulos_niagara_n4/niagara-tools/scripts/ng-deploy.sh` (+ README/CLAUDE)

**Scope**: HOW the operator actually builds/deploys their modules (the "cómo se construye" request), grounded in their real repo — not the doc-side generic build ([B12], REMIT). Reference skeleton = [B636].

---

## 637.1 The build is deploy-mode-driven, not test-driven

`[CERT]` `BUILD_WORKFLOW.md:157-179` — the operator drives the build by WHICH profile changed, in three modes:

| Mode | Gradle tasks | Trigger |
|---|---|---|
| **A (full)** | `:chihuahua-rt:clean :chihuahua-ux:clean :chihuahua-rt:jar :chihuahua-ux:jar` | rt + ux both changed |
| **B (ux-only)** | `:chihuahua-ux:clean :chihuahua-ux:jar` | only JS/CSS/HTML/servlet changed |
| **C (rt-only)** | `:chihuahua-rt:clean :chihuahua-rt:jar` | only Java `BComponent` changed |

There is **no explicit `sign` or `dist` task** in the operator's sequences — signing is wired into the `jar` task lifecycle by the Tridium plugin automatically ([B632]: the jar comes out with `META-INF/*.SF/.RSA`). So "build" = clean + jar (+ implicit sign); "distribute" = copy the jar to the station's `modules/` dir (the deploy scripts, §637.4). This matches the reference install path ([B633]: `modules/<part>.jar`) but done by a file copy from dev, not `plat moduleinstall`.

---

## 637.2 The two variants the operator asked about: Clean+Slotomatic+Build vs Clean+Build

`[CERT]` `BUILD_WORKFLOW.md:432-440` + `chihuahua-rt.gradle.kts:38-49` — the variant is decided by ONE question: **did you touch a `@Niagara*` annotation?**

- **Clean + Slotomatic + Build** — run when `@NiagaraType`/`@NiagaraProperty`/`@NiagaraAction`/`@NiagaraTopic` were **added or modified**. Sequence adds `:chihuahua-rt:slotomatic` (or `-ux`) BEFORE clean+jar. Slotomatic regenerates the `// BAJA AUTO GENERATED CODE` region (slot constants, getters/setters, dispatch methods) inside the `.java` and updates the hash in the `/*@ …$ @*/` marker ([B631]: Slotomatic is the source-model codegen, not a JSR-269 APT — this is that step in practice).
- **Clean + Build** — run when NO annotation changed. Slotomatic is a no-op and is skipped.

**The rule, stated plainly** (`BUILD_WORKFLOW.md:432-440`): *skipping slotomatic when you DID change a slot annotation leaves the AUTO region stale* (wrong hash, missing slot constants) → compile errors or silent runtime failures; *running it when nothing changed* costs 3-5s and is harmless. So: "if you touched a `@Niagara*` annotation, always run slotomatic first; otherwise skip it." The `niagara-tools` KB even names the failure mode of over-relying on it — `docs/knowledge-base/wsl-build-gotchas.md` ("slotomatic myth") and `slotomatic.md`.

This is the operator's under-documented decision captured: the two variants are not two build systems, they are **build-with-codegen vs build-without**, gated on annotation changes.

---

## 637.3 The TESTS verdict (verified, not assumed)

The operator's read — "the programming tests never served / hindered the build" — is **CORRECT for the Niagara-integrated tests, and the cause is a documented plugin bug, not operator error** `[CERT]` `chihuahua-rt.gradle.kts:38-49`:
```
// Tests in srcTest/test/ are kept as DOCUMENTATION ONLY. niagaraTest discovery
// is broken in plugin 7.6.17 — moduleTestAnnotationProcessor never produces ...
// @NiagaraType are silently skipped (Total tests run: 0).
// Decision (2026-05-05): keep test files as authored skeletons ... rely on manual
// smoke-testing on the Windows station. See ... engram 'honeywell-mx60-chihuahua/test-discovery-decision'.
```
So `niagaraTest` (the `BTestNg`/`BTestNgStation` path of [B12] §12.3.1) returns **"Total tests run: 0"** under plugin 7.6.17 — the `moduleTestAnnotationProcessor` never registers the `@NiagaraType`-annotated test classes. Wiring those into the build added friction for zero coverage: the operator's instinct to drop them was right.

**But a working test path DOES exist and is worth keeping** `[CERT]` `run-tests-wsl.sh:1-12` — pure-logic JUnit ("type (a)") run standalone in WSL against `baja.jar`, 9 suites of helper logic: `ChiJsonUtilTest`, `ChiThresholdHelperTest`, `ChiAlarmHelperTest`, `ChiAuditHelperTest`, `ChiHistoryStrideTest`, `ChiLinkHelperTest` (rt+ux) + `PendingLinkTest`/`PendingLinkBuilderTest`/`LinkSlotNameUtilTest` (wb). Explicitly EXCLUDED: `ChiHistoryHelperTest` ("type (b)" — `BAbsTime.make` needs the NRE kernel, so it cannot run outside a station). This is the honest split:

| Test kind | Runs? | Value | Verdict |
|---|---|---|---|
| **(a) pure-logic JUnit** (helpers, no Baja runtime) | ✅ standalone in WSL vs baja.jar | fast, real coverage of parsing/threshold/link logic | **KEEP** |
| **(b/c) niagaraTest / station tests** (`@NiagaraType`, `BAbsTime`, NRE kernel) | ❌ 0 discovered (7.6.17 bug) + need a running station | high cost, zero yield here | **the ones to drop** |

So the precise answer: it was not a mistake — the **station/`niagaraTest`** tests are dead weight here (plugin bug + the inherent station-spin-up cost, [B12] §12.3.1 gotcha #9); the **pure-unit tests of your own helper logic** are the ones that pay off and should stay in `run-tests-wsl.sh`.

---

## 637.4 The deploy scripts (distribute step)

`[CERT]` real scripts — three layers, same shape (backup → build → copy → verify):
- **`niagara-tools/scripts/ng-deploy.sh --mode A|B|C`** (README:29) — canonical WSL wrapper: backup → `./gradlew` with the right `-P` overrides → copy jars to `STATION_MODULES_DIR` → verify the types count against `EXPECTED_*_TYPES`; phase exit codes 10/20/30/40/50.
- **`build-and-deploy.ps1`** — Windows fast-path (~5s): backup + build + deploy + verify types via `[System.IO.Compression.ZipFile]`; flags `-Module ux|rt|all`, `-NoDeploy`, `-NoBackup`.
- **`deploy.sh`** — project-local legacy wrapper: `ux|rt|wb|all`, and `all --bump` (increments `vendorVersion`).
- **`inspect-build.ps1`** (annotation-processor output) + **`inspect-jars.ps1`** (prod-vs-test jar diff) — diagnostics.

Each deploy takes a `_backups/` snapshot first (rolling pre-deploy jar + source tarball) — the operator's answer to [B633]'s finding that the daemon install has NO backup/rollback. Deploy here is a **dev-side file copy into `modules/`**, not the signed `plat moduleinstall` transfer — faster for iteration, but it bypasses the install-time signature gate ([B633] §633.3), relying on the station's own load-time verify (`moduleVerificationMode=low` live, [B398]).

---

## 637.5 What this establishes for the audit + how to improve

- **The build/variant knowledge is now captured** (was "never well documented" per the operator): modes A/B/C by changed profile; slotomatic iff `@Niagara*` changed. Recommend encoding the variant rule directly into `ng-deploy.sh` (auto-detect annotation changes via git diff and add `:slotomatic` only then) so it stops being tribal knowledge.
- **Tests**: keep `run-tests-wsl.sh` (type-a) in CI; formally retire the `niagaraTest`/station skeletons (or gate them behind a flag) so they stop implying coverage that the 7.6.17 bug prevents. If station-level tests are ever needed, pin a plugin version without the `moduleTestAnnotationProcessor` bug.
- **Deploy backup is good practice** the vendor path lacks ([B633]) — keep it.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Build modes A/B/C by changed profile; clean+jar (sign implicit, no explicit sign/dist task) | [CERT] | BUILD_WORKFLOW.md:157-179 | ✅ read |
| 2 | Slotomatic variant gated on @Niagara* annotation change; stale AUTO region if skipped when needed | [CERT] | BUILD_WORKFLOW.md:432-440 · chihuahua-rt.gradle.kts:38-49 | ✅ read verbatim |
| 3 | niagaraTest broken by plugin 7.6.17 moduleTestAnnotationProcessor → Total tests run: 0; tests kept as docs-only | [CERT] | chihuahua-rt.gradle.kts:38-49 | ✅ read verbatim |
| 4 | pure-JUnit type-(a) path works standalone in WSL vs baja.jar (9 suites); type-(b) excluded (NRE kernel) | [CERT] | run-tests-wsl.sh:1-12 | ✅ read verbatim |
| 5 | deploy = backup→build→copy to modules/→verify types; ng-deploy.sh / build-and-deploy.ps1 / deploy.sh --bump | [CERT] | ng-deploy README:29 · build-and-deploy.ps1 · deploy.sh | ✅ read |
| 6 | dev deploy is a file copy (bypasses install-time sig gate), _backups/ snapshot each time | [CERT]/[INFER] | deploy scripts + [B633] contrast | ✅ read+derive |

**Tally**: [CERT] ×5 · [INFER] ×1 · DOCUMENT/build-process block (real-source citations; ratio not an exhaustion signal). Tests bug + slotomatic rule + build modes token-checked verbatim in the operator's own gradle/docs.

## Connections

- **[B636]** — reference skeleton this audits against. **[B631]** — Slotomatic as source-model codegen (not APT) = the slotomatic step here. **[B632]** — the signed jar this build produces. **[B633]** — vendor install path vs the operator's file-copy deploy (+ the backup gap they mitigate). **[B12]** §12.1/§12.3.1 — doc-side build/tests (REMIT; §637.3 confirms gotcha #9 live).
- Forward: OMB2 (version-targeting via niagara_home path) · OMB3 (ANGELES signing + niagara-tools).

## Gaps uncovered

- None new for the backlog. Cross-project engram topic `honeywell-mx60-chihuahua/test-discovery-decision` exists (operator's other engram project) — noted for reference, not reachable from this project's search.
