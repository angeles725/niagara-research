# B794 · `scaffold-module.sh`: the minimal-module generator round-trips gate-green [CERT] — the PR9 apply reference

> **§19 build/PoC** (requires-execution) — a working `scaffold-module.sh` PROTOTYPE that EMITS [Block 793]'s
> corrected minimal-module shape and is proven to round-trip: scaffold → preflight → build → verify gate
> ALL PASS → slot-coverage → lint-timers PASS → a mutation (drop the `stopped()`-cancel) → lint-timers FAIL →
> restore. This is the campaign-7 **PR9 apply reference**, NOT a merge-as-is; it lives in scratch, never in
> `niagara-tools`. Realises the `toolbelt/scaffold-module.sh` spec B790/B793 named (buildable + test-anchored).
>
> **Sources**: the prototype `~/modulos_niagara_n4/_scratch/scaffold-proto/scaffold-module.sh` and its emitted
> modules (ScaffoldPan by the build worker; VerifPan by the driver re-run), built against
> `niagara_home=/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162` (plugin 7.6.17), JDK 8
> `/usr/lib/jvm/java-8-openjdk-amd64`; kit `build-n4-module-kit/toolbelt/{preflight,build,verify-module,slot-coverage,lint-timers}.sh`.
> Driver-verified: the whole chain re-run on a fresh `VerifPan` — shellcheck 0 findings, build ALL PASS, and the
> lint GREEN→RED→GREEN cycle reproduced (§794.2).
> Method: author the generator → scaffold → build/verify/lint → mutation proof. Markers: `[CERT]` reproduced
> command output · `[INFER]` deduction.
>
> **Type:** `mixed` (execution evidence + a generator synthesis). Connects [Block 793] (the corrected shapes it
> emits, proven), [Block 790] (the skeleton), [Block 787]/[Block 788] (the biting checks), [Block 792] (the
> dup-lexicon-key check), kit `build-verify.md`.

---

## 794.1 — The generator `[CERT]`

`~/modulos_niagara_n4/_scratch/scaffold-proto/scaffold-module.sh` — stdlib bash, emits a complete buildable
single-profile `-rt` module.

| Aspect | Value |
|---|---|
| CLI | `scaffold-module.sh <ModuleName> <out-dir> [--vendor <v>] [--target-version <x.y>] [--plugin-version <v>]` |
| Defaults | vendor `Angeles`, target `4.14`, plugin `7.6.17`; component `B<ModuleName>` |
| Exits | `0` ok · `2` usage · `3` env (no JDK 8 / not a niagara_home) — typed, toolbelt convention |
| Conventions | `usage:` line to stderr; NO `$HOME`/`~` resolution; prose "version control", never a bare command name; **shellcheck 0.10.0 → 0 findings** (driver-run against the pinned binary) |

## 794.2 — Round-trip evidence (driver-reproduced on a fresh `VerifPan`) `[CERT]`

| Step | Command | Result |
|---|---|---|
| scaffold | `scaffold-module.sh VerifPan <out>` | exit 0, emitted tree |
| preflight | `preflight.sh <niagara_home> <root>` | PASS jdk8 · PASS plugin-pin 7.6.17 · PASS jar-lock |
| build | `build.sh --profiles rt --target-version 4.14 --plugin-version 7.6.17 <root> VerifPan <niagara_home>` | `:slotomatic`→`:jar`→**BUILD SUCCESSFUL** (7s), exit 0 |
| verify gate | `verify-module.sh` (in build.sh) | **ALL PASS** — bytecode 52, signed, types, baja 4.14, palette, +typecount/facets |
| slot-coverage | `slot-coverage.sh module-include.xml module.lexicon` | `pct=100.0 (type-set)`, missing none |
| lint-timers GREEN | `lint-timers.sh <src>` | **PASS** timer-ticket `BVerifPan.java: timer cancelled in stopped()` |
| dup lexicon keys | `grep '^k=' \| sort \| uniq -d` | none |
| palette | `grep -c '<p n='` | 1 (non-empty) |
| **MUTATION** (RED) | delete `ticket.cancel()` from `stopped()`, re-lint | **FAIL** timer-ticket `…: schedules a Clock ticket but stopped() does not cancel it`, exit 1 |
| restore | re-emit / restore the class, re-lint | **PASS** timer-ticket |

The RED↔GREEN swing on the mutation is the biting proof: the check carries the bite, not an unrelated grep.

## 794.3 — The 6 corrected shapes it emits (each grep-checked) `[CERT]`

| # | B793 correction | Emitted evidence |
|---|---|---|
| C1 | source lexicon = `module.lexicon` (plugin renames to `<MOD>-rt.lexicon` in jar) | `module.lexicon` present; jar carries `VerifPan-rt.lexicon` |
| C2 | source manifest = `module-include.xml` (NOT `module.xml`) | `module-include.xml` present; no source `module.xml` |
| C3 | no hand AUTO region (slotomatic generates it) | `grep -c 'BEGIN BAJA AUTO'` on emitted source = 0; present only post-build |
| C4 | hand-written `do<Action>()` (Baja's real callback) | `doTickExpired()` declared+bodied in the emitted class |
| C5 | profile file `<MOD>-rt.gradle.kts` (findProjects) | `VerifPan-rt.gradle.kts` present |
| C6 | plugin version overridable | `providers.gradleProperty("niagaraPluginVersion").getOrElse("7.6.17")` in settings.gradle.kts |

## 794.4 — Kit implication → `build-n4-module-kit/toolbelt/scaffold-module.sh` (campaign-7 PR9) `[CERT-grounded]`

- This prototype IS the PR9 apply reference: the campaign-7 writer lifts it into `toolbelt/` (it already matches
  the toolbelt conventions — typed exits, usage line, no `$HOME`, shellcheck-clean, prose "version control").
- Its RED→GREEN fixture contract (what QA writes as the biting test): `scaffold-module.sh <MOD> <out>` → `build.sh`
  exit 0 + `verify-module.sh` ALL PASS + `lint-timers.sh` PASS + no dup lexicon keys + non-empty palette; and a
  template mutation (drop the `stopped()`-cancel / empty the palette / dup a key) MUST flip the corresponding check
  to FAIL. §794.2 demonstrates the stopped()-cancel arm of that contract end-to-end.

## 794.5 — Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `scaffold-module.sh` emits a module that builds gate-green (ALL PASS) | `[CERT]` | §794.2 driver re-run on VerifPan | Y |
| 2 | shellcheck 0.10.0 → 0 findings; typed exits 0/2/3; no `$HOME` | `[CERT]` | driver ran pinned shellcheck | Y |
| 3 | lint-timers PASS on emitted; a dropped `stopped()`-cancel → FAIL; restore → PASS | `[CERT]` | §794.2 mutation cycle | Y — reproduced |
| 4 | Emits all 6 B793-corrected shapes (not B790 INFER) | `[CERT]` | §794.3 grep checks | Y |
| 5 | Palette symbol caveat: emitted `m="<sym>=<MOD>"` uses a derived symbol; a station assigns `preferredSymbol=<MOD>-rt` (B793 C4) — gate-neutral, a runtime concern | `[INFER]` | B793 §793.3 #4 | flagged |

**Tally:** `[CERT]` ×4 · `[INFER]` ×1. No unmarked claims.

## 794.6 — Connections & open gaps
- [Block 793] — the corrected shapes this generator emits (proven round-trip here). [Block 790] — the skeleton.
  [Block 787]/[Block 788] — the biting checks. [Block 792] — the dup-lexicon-key check. Kit `build-verify.md`.
- **B793-G1** still open (requires-execution): deploy an emitted module to a station and confirm it BOOTS. The
  scaffolder proves build + gate + lint by construction; live-boot needs a station. The palette-symbol caveat
  (§794.5 row 5) is the one shape whose runtime resolution is unverified without a station.
