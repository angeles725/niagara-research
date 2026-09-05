# B793 · Building B790's minimal module: the skeleton is gate-green [CERT], and the 10 corrections the build forced

> **§19 build/PoC** (requires-execution) — actually BUILDS the minimal N4 module template of [Block 790] through
> the kit's Java-8 + slotomatic + verify gate, upgrading B790's [INFER] "gate-green by construction" (B790 §790.2
> row 8) to **[CERT]** with real commands and outputs. Closes the *buildable + gate-green* half of **B790-G1**;
> the *deploys-and-boots-on-a-station* half stays requires-execution (no station this run — see gaps). The
> valuable payload is §793.3: the 10 places B790's INFER-level template did NOT build as written.
>
> **Sources**: the build of a scratch module `MinimalPan` (`~/modulos_niagara_n4/_scratch/MinimalPan/`, outside all
> client repos, untracked) against `niagara_home=/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162` (plugin 7.6.17)
> with JDK 8 `/usr/lib/jvm/java-8-openjdk-amd64`; the kit `build-n4-module-kit/{build-verify.md, toolbelt/*}`.
> Driver-verified: the verify gate was RE-RUN on the built jar (exit 0, ALL PASS) — see §793.2.
> Method: create per B790 → `preflight.sh` → `build.sh` (clean+slotomatic+jar+gate) → `verify-module.sh` →
> `slot-coverage.sh` → pure JUnit. Markers: `[CERT]` reproduced command output / verified artifact · `[INFER]` deduction.
>
> **Type:** `mixed` (execution evidence + a correction synthesis). Connects [Block 790] (the shape proven here),
> [Block 788] (the biting checks it must pass), [Block 784]/[Block 780]/[Block 776]/[Block 775]/[Block 787]
> (the cited element shapes), kit `build-verify.md`.

---

## 793.1 — The build (exact commands + outputs) `[CERT]`

Module: single profile `MinimalPan-rt`, `com.angeles.MinimalPan.BMinimalPan extends BComponent`, one
`Flags.SUMMARY|Flags.OPERATOR` property (`setpoint`), a `baja:RelTime` `interval`, one `Flags.HIDDEN` action
(`tickExpired`), a `Clock.Ticket` armed in `started()`+`atSteadyState()` and cancelled in `stopped()`, plus a
zero-Baja pure class `MinimalPanLogic` with a JUnit test.

| Step | Command (kit toolbelt) | Result |
|---|---|---|
| pure test | `run-pure-test.sh …/MinimalPan-rt com.angeles.MinimalPan.MinimalPanLogicTest` | `OK (4 tests)`, exit 0 |
| preflight | `preflight.sh /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162 <root>` | `PASS plugin-pin 7.6.17` · `PASS jar-lock` · `FAIL jdk8` (false negative, §793.3 #9) |
| build | `build.sh --profiles rt --target-version 4.14 --plugin-version 7.6.17 <root> MinimalPan <niagara_home>` | `:MinimalPan-rt:slotomatic` (rewrote AUTO region) → `:jar` → `BUILD SUCCESSFUL`; gate `ALL PASS`; exit 0 |
| slot-coverage | `slot-coverage.sh` (parse mode) | `pct=100.0 (type-set)`, `missing=` none |

## 793.2 — Gate results — B790's row-8 [INFER] upgraded to [CERT] `[CERT]`

`verify-module.sh` re-run by the driver on `…/MinimalPan-rt/build/libs/MinimalPan-rt.jar` → **exit 0, ALL PASS**:

| Gate check | Result | Driver proof |
|---|---|---|
| bytecode major 52 (Java 8) | PASS — 2 classes all major 52 | `unzip -p jar BMinimalPan.class \| od -An -t d1 -j6 -N2` → `0 52` |
| signed | PASS — `META-INF/NIAGARA4.SF` present | `unzip -l` → `NIAGARA4.SF` (719 B) + `NIAGARA4.RSA` (2168 B) |
| types resolve | PASS — 1 declared type → class | verify-module `types` PASS |
| baja stamp ≤ target | PASS — stamped `baja 4.14` | `unzip -p jar META-INF/module.xml \| grep baja` → `vendorVersion="4.14"` |
| palette non-empty | PASS — 1 component entry | verify-module `palette` PASS |

So B790's "the skeleton PASSES the verify gate … GREEN by construction" is now **empirically [CERT]**, not INFER.
(The `Clock.schedulePeriodically(BComponent,BRelTime,Action,BValue)` signature B790 used at INFER is confirmed
present in baja 4.14 — it compiled.)

## 793.3 — The 10 corrections the build forced on B790 (key deliverable) `[CERT]`

B790's template is INFER-level; the real build/plugin forced these. **S** = a genuine B790 spec error (→ §14 on
B790, owner investigador1); **T** = environment/tooling note (B790 not wrong).

| # | Kind | B790 said | The build required |
|---|---|---|---|
| 1 | **S** | region marker `/*+ … BEGIN BAJA AUTO … +*/` | slotomatic requires the `//region /*+ … +*/` … `//endregion` form (the `//region` keyword); it rewrote the markers in place |
| 2 | **S** | lexicon SOURCE file `<MOD>-rt.lexicon` | source file must be `module.lexicon`; the plugin renames it to `MinimalPan-rt.lexicon` **inside the jar**. B790 named the jar entry, not the source |
| 3 | **S** | author `<MOD>-rt/module.xml` in the source tree | the source has `module-include.xml` (types only); the plugin GENERATES `META-INF/module.xml` from it + `moduleManifest{}` + `vendor{}`. A hand-authored source `module.xml` is wrong |
| 4 | **S** | `preferredSymbol="mp"` | the plugin assigns `preferredSymbol="MinimalPan-rt"` (profile dir name); a custom symbol needs a manifest property the minimal block lacks → palette `m="mp=…"` would not resolve |
| 5 | T | 3-part baja floor `4.14.0` | plugin stamps 2-part `4.14` under `--target-version 4.14`; gate passes either way |
| 6 | T | palette folder `b:UnrestrictedFolder` | real exemplars + this build use `b:Folder`; gate accepts either |
| 7 | **S** | profile file `build.gradle.kts` | `findProjects()` convention discovers `MinimalPan-rt.gradle.kts` (named after the dir) |
| 8 | **S** | shows generated `tickExpired()` invoke wrapper only | the developer must ALSO hand-write `doTickExpired()` — Baja calls `do<Action>()` as the real callback; without it the action fires and no-ops |
| 9 | T | (n/a) | `preflight.sh` reports `FAIL jdk8` for the WSL `openjdk-8` at `/usr/lib/jvm/java-8-openjdk-amd64` (no `release` file); `build.sh`'s `[ -d $J8 ]` passes, so it does not block — a preflight tooling gap |
| 10 | — | INFER `Clock.schedulePeriodically(...)` | confirmed present in baja 4.14 (compiled) — INFER→CERT |

## 793.4 — Kit implication → `types/logic.md` + `toolbelt/scaffold-module.sh` (PR9) `[CERT-grounded]`

The built, gate-green `MinimalPan` IS the RED→GREEN fixture B790 named for `scaffold-module.sh` (PR9 candidate).
The corrections above are exactly what the scaffolder must emit to be buildable (not B790's INFER text):
- emit source `module.lexicon` (NOT `<MOD>-rt.lexicon`) and `module-include.xml` (NOT `module.xml`);
- emit `<MOD>-<profile>.gradle.kts` (findProjects convention) + a plugin-version-overridable `settings.gradle.kts`;
- emit the class WITHOUT a hand AUTO region (let slotomatic generate `//region…//endregion`) and WITH a
  hand-written `do<Action>()` handler;
- RED→GREEN test: `scaffold-module.sh <MOD> <vendor> <symbol>` → `build.sh` exit 0 + `verify-module.sh` ALL PASS
  + the B788 biting checks (palette non-empty, no dup lexicon keys, Clock.Ticket has a `stopped()`-cancel) pass;
  a mutation that empties the palette / dups a key / drops the `stopped()`-cancel must FAIL.

## 793.5 — Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | MinimalPan builds green (clean+slotomatic+jar), exit 0 | `[CERT]` | `build.sh` BUILD SUCCESSFUL; slot-coverage 100% | Y — driver re-ran gate |
| 2 | Built jar passes verify-module.sh (bytecode 52, signed, types, baja 4.14, palette) | `[CERT]` | driver re-run exit 0 ALL PASS; `od`→`0 52`; `NIAGARA4.SF/RSA` | Y |
| 3 | B790 §790.2 row-8 "gate-green by construction" upgraded INFER→CERT | `[CERT]` | §793.2 | Y |
| 4 | 10 corrections forced; 6 are genuine B790 spec errors (S) | `[CERT]` | §793.3 (from the real build) | Y — each reproduced |
| 5 | Live station boot still unproven (build/gate only) | `[INFER]` | no station this run | honest gap |

**Tally:** `[CERT]` ×4 · `[INFER]` ×1. No unmarked claims.

## 793.6 — Connections & open gaps
- [Block 790] — the shape proven here; §793.3 lists 6 spec corrections its owner should §14 into B790. [Block 788]
  — the biting checks the skeleton passes. [Block 784]/[Block 780]/[Block 776]/[Block 775]/[Block 787] — element
  shapes. Kit `build-verify.md` — the build doctrine used.
- **B793-G1** (requires-execution, was the residue of B790-G1): DEPLOY the built jar to a station and confirm it
  BOOTS (anchor slot populated after cold boot). Build + gate are [CERT]; live boot needs a station.
