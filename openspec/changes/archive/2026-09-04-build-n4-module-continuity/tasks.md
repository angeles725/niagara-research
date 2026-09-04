# Tasks: build-n4-module-continuity

## Review Workload Forecast (overall)

| Field | Value |
|-------|-------|
| Estimated changed lines | ~750 total (3 PRs) |
| 400-line budget risk | Low per PR (each ≤350 lines) |
| Chained PRs recommended | Yes |
| Suggested split | PR1 continuity-registry → PR2 retro-enforcement → PR3 lesson-promotion |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Kit ledger + orient + close-gate | PR 1 | `bats tests/*.bats` (60 green) | Read BUILD-STATE.md in session; verify orient prints | `git revert` PR1 merge leaves 60-bats green; SKILL.md reverts separately |
| 2 | Retro registry + sweep script + pre-push hook | PR 2 | `bats tests/build-retro-sync.bats && bats tests/*.bats` | Push a kit-file change without retro; gate must fire | `git revert` PR2 merge; sweep and hook disappear; baseline unchanged |
| 3 | Lesson fold + run-pure-test.sh | PR 3 | `bats tests/*.bats && shellcheck toolbelt/*.sh` | `toolbelt/run-pure-test.sh <rt-dir> <pkg> <Pure> <Test>` | `git revert` PR3 merge; .md reverts; script disappears; no module code touched |

---

## PR1 — continuity-registry (VERSION 0.5.0 → 0.6.0)

### PR1 Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~150 (BUILD-STATE.md ~100, BUILD-LOOP.md ~25, SKILL.md ~20, VERSION+CHANGELOG ~10) |
| 400-line budget risk | Low |
| Base | main |

### Phase 1.1 — Foundation: BUILD-STATE.md ledger

- [ ] **P1-T1** `[trabajador]` Create `build-n4-module-kit/BUILD-STATE.md`: write Index table (columns: module, last_build, verify_gate, deployed, open_issues, retro_pending, last_session) seeded with ColdRoomPan/DashboardPan/CompPan/chihuahua rows. *(Spec: Kit-Single Build-State Ledger / Scenario: Ledger seeded at PR1 merge)*
- [ ] **P1-T2** `[trabajador]` In BUILD-STATE.md, add one `<!-- build-state.v1 -->` envelope section per module. Frozen fields: schema, module, module_repo, module_root, profiles, target_version, plugin_version, last_build, bytecode_major(=52), signed, verify_gate, deployed, target_station, pure_tests, open_issues, retro_required, retro_pending, last_commit, last_session. GATED fields annotated inline: bytecode_major, retro_pending, last_commit. DECLARED fields annotated inline: verify_gate, deployed, open_issues, signed, profiles, pure_tests, module_repo, module_root.
- [ ] **P1-T3** `[trabajador]` Make ColdRoomPan the canonical worked example section: include prose (Built/Next/Open issues numbered) + open_issue #1 = "BDefrostController timing/interlock logic is INLINE with ZERO pure tests — started()/interval defrost bug (HIGH)". DashboardPan open_issue = "U5 servlet write-surface: no OPERATOR flag/whitelist check (HIGH)". Set `open_issues: 1` for each. *(Spec: Ledger seeded at PR1 merge)*
- [ ] **P1-T4** `[trabajador]` CompPan: seed with `open_issues: 0`, `pure_tests: 31`, `deployed: mirror`. chihuahua: seed with `open_issues: 0`, `deployed: station`. DashboardPan: `pure_tests: 14`.

### Phase 1.2 — Core: BUILD-LOOP.md orient + close-gate

- [ ] **P1-T5** `[trabajador]` In `build-n4-module-kit/BUILD-LOOP.md`, insert new **§0.a Orient from BUILD-STATE** after §0: read BUILD-STATE.md for the named module; print one-line leave-off: `<module> · <last_build>/<verify_gate>/<deployed> · next:<target_version> · open_issues=N · retro_pending=Y|N`. Add meta-work exemption note. *(Spec: Orient Step / Scenario: Orient prints leave-off)*
- [ ] **P1-T6** `[trabajador]` In BUILD-LOOP.md, reword **§7** as a HARD close gate: session MUST NOT close unless BUILD-STATE.md updated AND either (retro_pending:true + INDEX row) or tip-commit trailer `Retro: none (trivial: <reason>)`. Add Output Contract: git commit message MUST include `retro:` line. *(Spec: Hard Close Gate)*

### Phase 1.3 — SKILL.md update

- [ ] **P1-T7** `[trabajador]` In `~/.claude/skills/build-n4-module/SKILL.md` execution steps: step 1 → "read BUILD-STATE.md at orient, print leave-off for named module (meta-work exempt)"; step 6 → hard retro close-gate wording matching §7; Output Contract section → add `retro:` line requirement. *(Spec: SKILL.md BUILD-STATE Reference)*

### Phase 1.4 — Versioning + verification

- [ ] **P1-T8** `[trabajador]` Bump `VERSION` 0.5.0 → 0.6.0. Add CHANGELOG entry with SDD slug `build-n4-module-continuity/PR1`, engram IDs 8100/8103/8104. Conventional Commit, no AI attribution.
- [ ] **P1-T9** `[QA]` Run `bats tests/*.bats`; assert all 60 existing tests pass. Assert `kit-links.bats` L1/L2/L3 still green. No sweep script exists yet — kit-links L2 should be trivially satisfied. *(Spec: 60-Bats Baseline Stays Green)*

---

## PR2 — retro-enforcement (VERSION 0.6.0 → 0.7.0)

### PR2 Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~250 (INDEX.md ~40, sweep-build-state.sh ~60, build-retro-sync.bats ~110, pre-push ~30, VERSION+CHANGELOG ~10) |
| 400-line budget risk | Low |
| Base | main (after PR1 merged ff-only) |

> **TDD CONTRACT**: P2-T1 (RED bats) MUST be committed before P2-T3 (sweep script). The bats file must run RED (failing) when committed because the script does not yet exist.

### Phase 2.1 — RED-first: QA authors build-retro-sync.bats

- [ ] **P2-T1** `[QA]` Create `niagara-tools/tests/build-retro-sync.bats` — commit this file BEFORE sweep-build-state.sh exists so it runs RED. Test cases MUST cover:
  - (a) **Sweep content checks**: malformed `<!-- build-state.v1 -->` envelope → sweep exits 3; `retro_required:true` + `retro_pending:true` + no INDEX row → sweep exits 3; marker missing on a retro file → sweep exits 3; all clean → sweep exits 0. (No git in sweep — these are pure file-content assertions.)
  - (b) **Hook/git-diff classification** (using fixture repo via fakebin pattern from ng-deploy.bats): kit-file diff (`build-n4-module-kit/toolbelt/fix.sh`) without BUILD-STATE.md update AND no retro/trailer → gate exits non-zero; same diff WITH BUILD-STATE update + INDEX row → gate exits 0; same diff WITH `Retro: none (trivial: corrected typo)` trailer → gate exits 0; module-repo path (`ColdRoomPan/src/Foo.java`) changed → NOT build-relevant in niagara-tools repo → gate does NOT fire.
  - (c) **Threat matrix RED tests**: path with embedded space → NUL-delimited iteration handles it correctly; path ending in `.bats` → classified NOT build-relevant. *(Spec: Sweep-Build-State Script; Opt-In Pre-Push Hook; Threat matrix)*
- [ ] **P2-T2** `[QA]` Verify P2-T1 is RED: run `bats tests/build-retro-sync.bats`; confirm failure because `toolbelt/sweep-build-state.sh` does not yet exist.

### Phase 2.2 — retros/INDEX.md

- [ ] **P2-T3** `[trabajador]` Create `build-n4-module-kit/retros/INDEX.md` with header row: `date | module | retro file | deltas | review-status | target files`. Seed with the 29 existing retro files; `deltas` column = integer count of changed behaviors; `review-status` = `pending` for newly filed, `folded` for already-applied retros (rt-hardening, 5rooms, hmi-touch-ux, self-firing-timer). *(Spec: Retro Registry Index; decision #8102 — exclude already-folded)*

### Phase 2.3 — sweep-build-state.sh (makes P2-T1 GREEN)

- [ ] **P2-T4** `[trabajador]` Create `build-n4-module-kit/toolbelt/sweep-build-state.sh`:
  - Header: `#!/usr/bin/env bash`, `set -euo pipefail`, shellcheck-clean, NO git invocation (kit-links L2 hard constraint).
  - Arg: optional `<build-state-file>` path, defaults to `build-n4-module-kit/BUILD-STATE.md`.
  - Exit 1: usage error or missing file.
  - Exit 3 (integrity FAIL, prints each offending field/file to stderr): malformed envelope (`<!-- build-state.v1 -->` open without matching close); `bytecode_major != 52` when `last_build == PASS`; `retro_required: true` + `retro_pending: true` (non-trivial, no trailing-retro exemption); retro file in `retros/` without line-1 `review-status:` marker (unless `<!-- kit-retro: exclude -->`); INDEX row pointing at non-existent file; retro file without matching INDEX row.
  - Exit 0: all checks pass.
  - *(Spec: Sweep-Build-State Script TDD-first; Design: Script exit codes)*
- [ ] **P2-T5** `[QA]` Run `bats tests/build-retro-sync.bats`; confirm sweep content tests are now GREEN. Record any remaining RED items as scope failures before proceeding.

### Phase 2.4 — pre-push hook template

- [ ] **P2-T6** `[trabajador]` Create `niagara-tools/.githooks/pre-push` (executable bit set, no auto-install):
  - Iterates pushed commits with `git diff --name-only -z <base>..<tip>` (NUL-delimited to handle spaces).
  - Classifies build-relevant paths per the precise definition: `build-n4-module-kit/toolbelt/*.sh`, `build-n4-module-kit/types/*.md`, `build-n4-module-kit/METHODOLOGY.md`, `build-n4-module-kit/BUILD-LOOP.md`, `build-n4-module-kit/build-verify.md`. Module src globs (`*/src/**/*.java` etc.) are listed but classified as NOT build-relevant IN this repo (per decision #8100 — module repos are separate).
  - On build-relevant diff: requires BUILD-STATE.md modified + (retro file + INDEX row OR tip-commit trailer `Retro: none (trivial: …)`). Exits non-zero with named missing requirement on failure.
  - NOT build-relevant: `*.bats`, `tests/**`, `retros/**`, `VERSION`, `CHANGELOG.md`, `*.md` at repo root.
  - *(Design: pre-push opt-in; Threat matrix: hook silent config mutation — template only)*
- [ ] **P2-T7** `[trabajador]` Add opt-in install note to `build-n4-module-kit/BUILD-LOOP.md` (§1 Prerequisites or new §0.b) and `CONTRIBUTING.md`: `git config core.hooksPath .githooks` (per-clone opt-in). Never auto-install. *(Design: Decision pre-push opt-in)*

### Phase 2.5 — Versioning + full verification

- [ ] **P2-T8** `[trabajador]` Bump `VERSION` 0.6.0 → 0.7.0. Add CHANGELOG entry (SDD slug + sweep-build-state.sh noted as new toolbelt script triggering MINOR).
- [ ] **P2-T9** `[QA]` Run `bats tests/*.bats`; assert all 60 baseline + build-retro-sync.bats pass. Assert kit-links L2 still green (sweep has no git). Run `shellcheck build-n4-module-kit/toolbelt/sweep-build-state.sh`. *(Spec: 60-Bats Baseline Stays Green)*

---

## PR3 — lesson-promotion (VERSION 0.7.0 → 0.8.0)

### PR3 Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~350 (run-pure-test.sh ~55, bats for it ~60, METHODOLOGY.md ~80, build-verify.md ~80, types/logic.md ~60, types/dashboard.md ~25, VERSION+CHANGELOG ~10) |
| 400-line budget risk | Low-Medium |
| Base | main (after PR2 merged ff-only) |

> **TDD CONTRACT**: P3-T1 (RED bats for run-pure-test.sh) MUST be committed before P3-T3 (the script). Bats runs RED because the script does not yet exist.

> **NON-GOAL**: No N4 module source changes (DefrostController extraction, servlet fix stay out). Record as BUILD-STATE open_issues only. No .java/.gradle/.xml/.bats changes except the run-pure-test.sh bats test.

### Phase 3.1 — RED-first: QA authors run-pure-test bats

- [ ] **P3-T1** `[QA]` Create `niagara-tools/tests/build-retro-sync-run-pure.bats` (or add a describe block to build-retro-sync.bats) — RED-first biting test for `toolbelt/run-pure-test.sh`:
  - Empty gradle cache (no junit jar) → exits 1 with fetch-hint message. *(Threat matrix: subprocess/network)*
  - Valid gradle cache + trivial pure class + JUnit test with bite assertion → exits 0, prints `OK`.
  - Test class with NO bite assertion (only presence check) → exits non-zero (test fails). Ensures mutation sensitivity. *(Spec: Mutation-Sensitive Boundary Assertion)*
  - Compile error → exits non-zero.
  - `<1 s` elapsed for a trivial suite (timing guard via `time` or `$SECONDS`). *(Spec: Fast Suite)*
- [ ] **P3-T2** `[QA]` Run the new bats; confirm RED (script missing).

### Phase 3.2 — run-pure-test.sh (makes P3-T1 GREEN)

- [ ] **P3-T3** `[trabajador]` Create `build-n4-module-kit/toolbelt/run-pure-test.sh <rt-dir> <pkg> <PureClass> <TestClass>`:
  - `JU=$(find ~/.gradle -name 'junit-4.13.2.jar' | head -1)`, `HC=$(find ~/.gradle -name 'hamcrest-core-1.3.jar' | head -1)`.
  - Exit 1 if either is empty: "run a gradle build once to fetch junit-4.13.2 + hamcrest-core-1.3".
  - `mkdir -p <rt-dir>/out`.
  - `javac -source 8 -target 8 -cp "$JU:$HC" <PureClass>.java <TestClass>.java -d <rt-dir>/out`.
  - `java -cp "<rt-dir>/out:$JU:$HC" org.junit.runner.JUnitCore <pkg>.<TestClass>`.
  - Print `OK` or `FAILURES`; propagate JUnitCore exit code. No network. *(Spec: run-pure-test.sh TDD-First; Design: Script contracts)*
- [ ] **P3-T4** `[QA]` Run `bats tests/build-retro-sync-run-pure.bats`; confirm GREEN.

### Phase 3.3 — Lesson fold: TOP-5 + items 6-8 (documentation only)

- [ ] **P3-T5** `[trabajador]` **S1 — Never retype a live slot**: Add explicit headed rule to `build-n4-module-kit/METHODOLOGY.md` rt checklist (after `@NiagaraProperty` 3-places rule): "Never retype a slot that has saved `.bog` data — the station will not boot." Cross-link in `types/logic.md`. *(PR3 top-5 #2; decision #8102 S1)*
- [ ] **P3-T6** `[trabajador]` **L2 — `0` blocks, not disables**: Add bullet to METHODOLOGY.md "Domain correctness" section. Add slot-default note to `types/logic.md`. *(PR3 top-5 #3; decision #8102 L2)*
- [ ] **P3-T7** `[trabajador]` **L1 — Long.MIN_VALUE time-sentinel overflow**: Add rule to `types/logic.md` slot-default note section: "A time/interval sentinel of Long.MIN_VALUE overflows silently and latches the rack off — use 0 or an explicit DISABLED state." *(decision #8102 L1)*
- [ ] **P3-T8** `[trabajador]` **U5 — Servlet write-surface rule**: Add explicit headed rule to `build-n4-module-kit/types/dashboard.md` (or create file if absent): "Any generic write endpoint MUST check the OPERATOR permission flag and a whitelist of writable ORDs before executing — unauthenticated writes are a security hole." Note as BUILD-STATE open_issue for DashboardPan (no module code change). *(decision #8102 U5)*
- [ ] **P3-T9** `[trabajador]` **B5 — module.palette one entry per @NiagaraType**: Add bullet to METHODOLOGY.md rt checklist: "`module.palette` MUST list one entry per `@NiagaraType`; an empty or incomplete palette ships silently and breaks commissioning." *(PR3 top-5 #5; decision #8102 B5)*
- [ ] **P3-T10** `[trabajador]` **Timer `started()`+`atSteadyState()` idiom** (B729 prod defrost failure): Add "rt timer/lifecycle" subsection to `types/logic.md`: arm timer in BOTH `started()` AND `atSteadyState()` — timer armed only in `atSteadyState()` silently fails on initial cycle. Insert Tridium 3-hook idiom. Add anti-pattern row to METHODOLOGY.md rt checklist. *(PR3 top-5 #1; design ranked #1)*
- [ ] **P3-T11** `[trabajador]` **Extract pure class BEFORE BComponent** (item #4): Add template-method pattern section to `types/logic.md`. Add "Build/Tests" item to `build-verify.md`: "Extract the pure decision class FIRST; test it (with bite assertions); verify it compiles; THEN wire into BComponent." *(Spec: Pure-Class Extraction Mandate; design ranked #4)*
- [ ] **P3-T12** `[trabajador]` **4-layer QA stack** (item #6): Add "How you know it's good" section to `build-verify.md`: pure JUnit (run-pure-test.sh) → build-verify gate → live smoke → adversarial review. Add one-line pointer in METHODOLOGY.md. *(Spec: qa-test-gate domain; design ranked #6)*
- [ ] **P3-T13** `[trabajador]` **Exact JUnit cmd + run-pure-test.sh** (item #7): In `build-verify.md` ~line 96, replace `<junit>`/`<hamcrest>` placeholders with `find ~/.gradle` resolver and `toolbelt/run-pure-test.sh` pointer. *(design ranked #7)*
- [ ] **P3-T14** `[trabajador]` **niagaraTest docs-only note** (item #8): Add QA note to `build-verify.md`: "`niagaraTest` is documentation, not a WSL gate — Niagara 7.6.17 discovers 0 tests from WSL. Use `run-pure-test.sh` for the actual pure-class gate." *(design ranked #8)*

### Phase 3.4 — Contradiction resolution (ONE rule each)

- [ ] **P3-T15** `[trabajador]` **Jar-lock resolution** (contradiction #1): In `build-verify.md` §mirror section, replace dual guidance with single rule: "Free the lock first — close Workbench, or if the holder is not a live production supervisor, stop it and build directly. Use the mirror strategy ONLY when the holder is a running station you must not stop." Remove any conflicting alternative. *(decision #8102 contradiction #1)*
- [ ] **P3-T16** `[trabajador]` **Slot-default resolution** (contradiction #2): In `METHODOLOGY.md`, replace "default to current behavior" with the scoped rule: "When adding a slot to already-deployed logic, preserve the current behavior (upgrade safety). When a NEW slot gates a safety action, it defaults to the safe posture — safety wins; flag the behavior change in the retro and version note." *(decision #8102 contradiction #2)*

### Phase 3.5 — Fold INDEX review-status for promoted retros

- [ ] **P3-T17** `[trabajador]` For each retro entry whose lesson was folded in P3-T5 through P3-T16, update its `review-status` column in `retros/INDEX.md` to `folded · kit <sha>` (sha of the PR3 merge commit — use `HEAD` as placeholder, finalize at commit time).

### Phase 3.6 — Versioning + full verification

- [ ] **P3-T18** `[trabajador]` Bump `VERSION` 0.7.0 → 0.8.0 (MINOR — run-pure-test.sh is a new toolbelt script per CONTRIBUTING §4). Add CHANGELOG entry (SDD slug, engram IDs, PR3 scope summary). Confirm MINOR vs PATCH against CONTRIBUTING §4 at apply-time.
- [ ] **P3-T19** `[QA]` Run `bats tests/*.bats`; assert all baseline (60) + build-retro-sync + run-pure-test bats green. Run `shellcheck build-n4-module-kit/toolbelt/sweep-build-state.sh build-n4-module-kit/toolbelt/run-pure-test.sh`. Verify kit-links L1/L2/L3 still green (no new git calls in toolbelt). *(Spec: 60-Bats Baseline; Threat matrix: git-in-toolbelt N/A assertion)*
- [ ] **P3-T20** `[QA]` **sdd-verify grading** — grade PR3 against spec scenarios: ≥5 lessons in living files as explicit headed rules; types/dashboard.md contains U5 rule; contradiction-1 and -2 each state ONE rule; no .java/.bats/.gradle/.xml added; VERSION is 0.8.0; CHANGELOG entry present.

---

## Task Summary

| PR | Owner mix | Tasks | Key constraint |
|----|-----------|-------|----------------|
| PR1 | trabajador (T1-T8) + QA (T9) | 9 | No TDD needed (no scripts); SKILL.md is out-of-repo (declared, not gated) |
| PR2 | QA RED-first (T1-T2, T5, T9) + trabajador (T3-T4, T6-T8) | 9 | TDD strict: T1 RED before T4; sweep NO git |
| PR3 | QA RED-first (T1-T2, T4, T19-T20) + trabajador (T3, T5-T18) | 20 | TDD strict: T1 RED before T3; all .md folds are doc-only; no module code |

**Total tasks: 38** | **Sequential within each PR; PRs are sequential (ff-only stacked-to-main)**

## Parallel / Sequential Map

- P1-T1 through P1-T4 are parallelizable (independent sections of BUILD-STATE.md).
- P1-T5 and P1-T6 are parallelizable (different sections of BUILD-LOOP.md).
- P2-T1 MUST precede P2-T3 (RED-first TDD contract).
- P2-T3, P2-T4, P2-T7 are parallelizable after P2-T1.
- P3-T1 MUST precede P3-T3 (RED-first TDD contract).
- P3-T5 through P3-T16 are parallelizable (different target files).
- P3-T17 depends on P3-T5 through P3-T16 (must know which retros were folded).
- P3-T18 through P3-T20 are sequential final steps.
