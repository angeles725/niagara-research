# Design: build-n4-module-continuity

Hardens the `build-n4-module` kit so it *remembers*: a machine-checkable session ledger
(`BUILD-STATE.md`), a non-skippable retro close-gate, and promotion of write-only lessons into
living kit files. Delivered as 3 chained PRs (stacked-to-main, ff-only). Repo:
`/home/cristian/modulos_niagara_n4/niagara-tools`. Baseline: **60 bats green**, `VERSION` 0.5.0,
Conventional Commits, no AI attribution. NON-GOAL: no N4 module code, no PITest.

## Technical Approach

Copy the *proven* research-sdd continuity machine, sized down for a build loop: a single kit-root
index+state file with an HTML-comment envelope (`build-state.v1`, mirroring `research-state.v1`), a
PASO-0-style orient step, and a CERRAR-style close gate. Enforcement is a `bats` test + an ad-hoc
`sweep` helper + an opt-in pre-push hook, mirroring the retro-enforcement retro's D6(gate)/D7(visible)/
P6(marker) triad. The join between the always-on light record and the heavy retro artifact is one
field: `retro_pending`.

## Architecture Decisions

### Decision: ONE kit-root `BUILD-STATE.md`, not per-module-root state files
**Choice**: `build-n4-module-kit/BUILD-STATE.md` = index table + one section (with envelope) per module.
**Alternatives**: sdd-explore's per-module-root `BUILD-STATE.md` beside `build.gradle.kts` (client repo).
**Rationale**: the PR2 gate (`tests/build-retro-sync.bats` + pre-push hook) lives in the `niagara-tools`
repo and can only inspect files *inside* it; a state file in a separate client repo is invisible to the
gate. Also matches `FOCUSES.md` (single index, user-praised). Authoritative per decision #8100.

### Decision: the git-diff logic lives in the bats test + the hook, NOT in `toolbelt/sweep-build-state.sh`
**Choice**: `sweep-build-state.sh` inspects only *file content* (envelope integrity, marker/INDEX
integrity, `bytecode_major`/`retro_pending` consistency) and exits non-zero on drift. The "which paths
changed in this range" classification (which needs `git diff`) lives in `tests/build-retro-sync.bats`
and the `.githooks/pre-push` template — both allowed to call git.
**Alternatives**: put git-diff scanning inside `sweep-build-state.sh`.
**Rationale**: `tests/kit-links.bats` L2 **asserts no `toolbelt/*.sh` invokes git** ("scripts run inside
worktrees and on stations"). A git call in the sweep would turn an existing green test red. Splitting the
responsibilities keeps L2 green and keeps the sweep runnable on a locked station.

### Decision: `retro_pending` is the single enforcement hook (gated), the rest declared
**Choice**: gate `retro_pending==false` (on a non-trivial build-relevant diff), `last_commit` resolves to
a real in-range commit, `bytecode_major==52` when `last_build==PASS`. Declare (human-truthful, loosely
cross-checked): `verify_gate`, `deployed`, `open_issues`, `signed`, `profiles`.
**Alternatives**: gate every field; gate nothing (pure discipline — today's failure mode).
**Rationale**: same honest gated/declared split as `research-state.v1`. Gating only the load-bearing
fields keeps the check fast and false-positive-free; the always-on record leaves a `retro_pending: true`
breadcrumb even after an abrupt session end, closing the "context loss / manual close" skip path.

### Decision: pre-push hook is opt-in, lives in `.githooks/` (outside `toolbelt/`), never auto-installed
**Choice**: ship `niagara-tools/.githooks/pre-push` as a template; operator opts in with
`git config core.hooksPath .githooks`. It runs `bats tests/build-retro-sync.bats` over the push range.
**Rationale**: no CI exists (the P5 slot never filled); a hook is the only automatic gate. Keeping it
outside `toolbelt/` sidesteps the L2 "no git" rule (the hook legitimately needs git). Opt-in + no
auto-install honors the threat matrix (never mutate the operator's git config silently).

### Decision: version bumps — PR1 MINOR, PR2 MINOR, PR3 MINOR (not PATCH)
**Choice**: `VERSION` 0.5.0 → 0.6.0 (PR1) → 0.7.0 (PR2) → 0.8.0 (PR3), each with a CHANGELOG entry.
**Rationale**: CONTRIBUTING §4 makes a *new script surface* a MINOR. PR1 adds no script (MINOR only if we
count the new `BUILD-STATE.md`/BUILD-LOOP surface; treat as MINOR to be safe), PR2 adds
`sweep-build-state.sh`, PR3 adds `run-pure-test.sh` — so **PR3 is MINOR, not the PATCH the brief
tentatively named**. The doc-fold half of PR3 alone would be PATCH; the new toolbelt script forces MINOR.
Flag for `sdd-tasks`: confirm this at apply-time against §4.

## `build-state.v1` — exact schema

One envelope per module section in `BUILD-STATE.md`. Underscored field names (never collide with prose
greps). Enum/int values only.

```
<!-- build-state.v1 -->
schema: build-state.v1
module: ColdRoomPan
target_version: 4.15          # LOWEST niagara_home built against (build-verify rule); or "none"
profiles: rt,ux               # comma list of profiles with sources (rt|ux|wb)
last_build: PASS              # PASS | FAIL | not-run   (toolbelt/build.sh outcome)
bytecode_major: 52            # GATED: must be 52 when last_build==PASS
signed: true                  # true | false           (META-INF/NIAGARA4.SF present)
verify_gate: PASS             # PASS | FAIL | not-run   (toolbelt/verify-module.sh) — declared
deployed: station             # none | mirror | station — declared
target_station: JACE-192.168.1.140   # host/ORD or "none"
open_issues: 1                # int; must equal the count of numbered items in prose below
retro_required: true          # did this session change behavior / prove a lesson?
retro_pending: false          # GATED: must be false when retro_required==true on a non-trivial diff
last_commit: a1b2c3d          # GATED: short sha of a real commit that touched this module
last_session: 2026-09-04      # ISO date (YYYY-MM-DD)
<!-- /build-state.v1 -->
```

Prose under each envelope (3–6 lines, capped — a leave-off note, not a design doc): **built** this
session (slots/endpoints), **next**, **open issues** (numbered, matching `open_issues`), any `[CERT-live]`
deploy note.

### Worked example (seed content, from known reality)

```markdown
## Index
| module | last_build | verify_gate | deployed | open_issues | retro_pending | last_session |
|---|---|---|---|---|---|---|
| ColdRoomPan | PASS | PASS | station | 1 | false | 2026-09-03 |
| DashboardPan | PASS | PASS | station | 0 | false | 2026-09-04 |
| CompPan | PASS | PASS | mirror | 0 | false | 2026-09-02 |
| chihuahua | PASS | PASS | station | 0 | false | 2026-08-31 |

## ColdRoomPan
<!-- build-state.v1 -->
schema: build-state.v1
module: ColdRoomPan
target_version: 4.14
profiles: rt,ux
last_build: PASS
bytecode_major: 52
signed: true
verify_gate: PASS
deployed: station
target_station: PANCCADIA-Leon
open_issues: 1
retro_required: false
retro_pending: false
last_commit: <sha>
last_session: 2026-09-03
<!-- /build-state.v1 -->
Built: rt control (BColdRoom/BDefrostController) + ux HMI, deployed live at PANCCADIA León.
Next: extract DefrostControl to a pure class + tests.
Open issues:
1. BDefrostController timing/interlock logic is INLINE in the BComponent with ZERO pure tests —
   the subsystem that already shipped the started()/interval defrost bug (retros 2026-09-03). HIGH.
```

DashboardPan/CompPan/chihuahua get analogous sections (`retro_required: false`, `open_issues: 0`).

## "Build-relevant diff" — precise definition (the classifier for PR2)

Over the push/commit range, a changed path is **build-relevant** iff it matches ANY:
- a module source: `*/src/**/*.java`, `*/module.xml`, `*/module-permissions.xml`, `*/module.lexicon`,
  `*/module.palette`;
- a dashboard/HMI surface: `*/src/rc/**/*.html`, `*/src/rc/**/*.js`;
- a KIT behavior file: `build-n4-module-kit/toolbelt/*.sh`, `build-n4-module-kit/types/*.md`,
  `build-n4-module-kit/METHODOLOGY.md`, `build-n4-module-kit/BUILD-LOOP.md`,
  `build-n4-module-kit/build-verify.md`.

**NOT build-relevant** (never require a retro/state update on their own): `*.md` prose that is only
README/CHANGELOG/retros/INDEX, `VERSION`, `*.bats`/`tests/**` (test-only), comment/whitespace. When ANY
build-relevant path changed, PR2 REQUIRES in the same range: (a) `BUILD-STATE.md` modified with the touched
module's `last_commit` in-range and `last_session==today`; AND (b) EITHER a new/modified
`retros/<date>-<module>.md` carrying `<!-- review-status: pending -->` + a matching `retros/INDEX.md` row,
OR a tip-commit trailer `Retro: none (trivial: <reason>)` together with `retro_required: false`.

## File Changes

| File | Action | PR | Description |
|------|--------|----|-------------|
| `build-n4-module-kit/BUILD-STATE.md` | Create | 1 | Index table + seeded envelope per module (ColdRoomPan/DashboardPan/CompPan/chihuahua); DefrostController gap as ColdRoomPan open_issue #1 |
| `build-n4-module-kit/BUILD-LOOP.md` | Modify | 1 | New **§0.a Orient from BUILD-STATE** (after §0); reword **§7** as a HARD close gate + Output-Contract retro line |
| `~/.claude/skills/build-n4-module/SKILL.md` | Modify | 1 | Execution step 1: "read BUILD-STATE.md at orient"; step 6 → hard retro close-gate wording; Output Contract adds the `retro:` line |
| `VERSION` / `CHANGELOG.md` | Modify | 1,2,3 | 0.5.0→0.6.0→0.7.0→0.8.0; one CHANGELOG entry per PR with SDD slug + engram IDs |
| `build-n4-module-kit/retros/INDEX.md` | Create | 2 | Registry: `date · module · retro file · deltas · review-status(pending/folded) · target files` |
| `build-n4-module-kit/toolbelt/sweep-build-state.sh` | Create | 2 | Content-only integrity sweep (NO git); exit codes below. Lands test-first |
| `niagara-tools/tests/build-retro-sync.bats` | Create | 2 | The biting test (RED first, QA-authored); asserts the gate. git-diff logic lives here |
| `niagara-tools/.githooks/pre-push` | Create | 2 | Opt-in hook template running the bats gate over the push range |
| `build-n4-module-kit/toolbelt/run-pure-test.sh` | Create | 3 | Resolve junit+hamcrest from `~/.gradle`, compile pure class+test, run JUnitCore, print OK/FAILURES (<1s). Lands test-first |
| `build-n4-module-kit/METHODOLOGY.md` | Modify | 3 | Fold 4-layer QA stack + bite/fast-test gate + promoted rt rules |
| `build-n4-module-kit/build-verify.md` | Modify | 3 | Concrete JUnit paths + `run-pure-test.sh` recipe; 4-layer QA stack; bite+fast 4-point gate |
| `build-n4-module-kit/types/logic.md` | Modify | 3 | Promoted rt lessons (timer idiom, slot-retype, 0=block, pure-class extraction) |
| `niagara-tools/tests/build-retro-sync.bats` (run-pure) | Create | 3 | RED-first biting test for `run-pure-test.sh` |

## Script contracts / exit codes

`toolbelt/sweep-build-state.sh` (content-only, no git, shellcheck-clean, `set -euo pipefail`):
- `0` — all envelopes well-formed; every `retros/*.md` (unless `<!-- kit-retro: exclude -->`) has line-1
  marker (`review-status: pending|applied … · kit <sha>|dismissed`) AND one INDEX row; every INDEX row
  points at a real file; `bytecode_major==52` where `last_build==PASS`; no section with `retro_pending:
  true`.
- `1` — usage error (bad args / missing `BUILD-STATE.md`).
- `3` — integrity FAIL (drift found) — the exit-3 tier equivalent of research-sdd `verify-state.sh`.
  Prints each offending file/field to stderr.

`tests/build-retro-sync.bats` (RED-first; MAY call git against a fixture repo):
- Asserts: a build-relevant diff **without** a BUILD-STATE update + retro/INDEX row **and without** a
  `Retro: none (trivial: …)` trailer → the gate exits non-zero. Green case: same diff **with** the state
  update + retro row (or trivial trailer) → exits 0. Also asserts `sweep-build-state.sh` exit-3 on a
  broken envelope, and marker/INDEX integrity. Uses the fakebin/fixture pattern from `ng-deploy.bats`.

`toolbelt/run-pure-test.sh <module-rt-dir> <pkg> <PureClass> <TestClass>`:
- Resolve `JU=$(find ~/.gradle -name 'junit-4.13.2.jar' | head -1)`, `HC` likewise (hamcrest-core-1.3).
- `1` if the cache is empty (message: "run a gradle build once to fetch junit-4.13.2"). Else `javac
  -source 8 -target 8` the pure class + test, `java … org.junit.runner.JUnitCore <pkg>.<TestClass>`;
  print `OK` / `FAILURES`; propagate JUnitCore's exit. Target <1s.

## Ranked promotion list (PR3) — lesson → target file → exact insertion

1. **Timer `started()`+`atSteadyState()` idiom** (B729, prod defrost failure) → `types/logic.md` new
   "rt timer/lifecycle" subsection + `METHODOLOGY.md` rt checklist anti-pattern row ("timer armed ONLY in
   `atSteadyState()`"). Insert the Tridium 3-hook idiom verbatim.
2. **Never retype a slot with saved `.bog` data** (station won't boot) → `METHODOLOGY.md` rt checklist,
   new bullet after the `@NiagaraProperty` 3-places rule; cross-link in `types/logic.md`.
3. **`0` as a limit default blocks instead of disabling** (rack startup failure) → `METHODOLOGY.md`
   "Domain correctness" new bullet + `types/logic.md` slot-default note.
4. **Extract pure decision class BEFORE the BComponent; test it; bite-check** → `types/logic.md`
   template-method pattern section + `build-verify.md` Build/Tests item.
5. **`module.palette` = one entry per `@NiagaraType`** (empty palette ships silently) → `METHODOLOGY.md`
   rt checklist new bullet.
6. **4-layer QA stack** (pure JUnit / build-verify gate / live smoke / adversarial review) →
   `build-verify.md` new "How you know it's good" section + one-line pointer in `METHODOLOGY.md`.
7. **Exact standalone JUnit command + `run-pure-test.sh`** → `build-verify.md` replace the `<junit>`/
   `<hamcrest>` placeholders (line ~96) with the `find ~/.gradle` resolver + the script pointer.
8. **`niagaraTest` is docs, not a WSL gate** (7.6.17 discovers 0 tests) → `build-verify.md` QA section note.
9. *(optional 9–12, if budget allows)* `setSlot()` before `Clock.schedule()` ordering; `schedulePeriodically`
   period>0 guard; HOA manual-override slot pattern; hidden-action invocability — all → `types/logic.md`.

Target 5–12; items 1–8 are the committed set (≥5 satisfied). Each fold updates a `retros/INDEX.md` row's
`review-status` to `folded` with the kit sha.

## Data flow

    Session start ──► §0.a Orient: read BUILD-STATE.md ──► one-line leave-off per active module
                                                            (module · build/verify/deployed · next
                                                             · open_issues=N · retro_pending=Y/N)
    ...build work...
    Session close ──► §7 HARD gate: write retro (or trivial trailer) ──► update BUILD-STATE envelope
                          │                                                     │
                          └───────────► retro_pending flag ◄────────────────────┘
                                              │
    git push ──► .githooks/pre-push ──► bats build-retro-sync ──► sweep-build-state.sh (content)
                                              │                         │
                                        git-diff classify          exit 0 | 3
                                        build-relevant?  ──► FAIL (non-zero) if drift

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (bats) | build-relevant diff without state/retro → non-zero; with → 0; envelope integrity; marker/INDEX integrity | `tests/build-retro-sync.bats`, RED-first, fakebin+fixture repo (ng-deploy pattern) |
| Unit (bats) | `run-pure-test.sh` resolves jars, compiles, runs JUnitCore, exit propagation | RED-first biting test |
| Regression | existing 60 bats stay green; `kit-links.bats` L1/L2/L3 still pass (new files must resolve; sweep has no git) | run full `bats tests/*.bats` each PR |
| Lint | `shellcheck` clean on both new scripts | `shellcheck … build-n4-module-kit/toolbelt/*.sh …` |

## Threat Matrix

Applicable — this design adds shell scripts, a git-driven bats check, and a git hook.

| Row | Applicability | Safe behavior / RED test |
|-----|---------------|--------------------------|
| Shell injection via changed-path names | Applicable | Quote all path expansions; iterate `git diff --name-only -z` NUL-delimited; RED test with a space/newline path |
| git invoked from `toolbelt/*.sh` | Applicable (N/A by design) | Sweep uses NO git (keeps `kit-links.bats` L2 green); git only in bats + hook. RED = L2 stays green |
| Hook auto-install / silent git-config mutation | Applicable | Hook is a template; opt-in via `core.hooksPath`; never written by any script. No test needed beyond doc |
| Subprocess / network in `run-pure-test.sh` | Applicable | `find ~/.gradle` + `javac`/`java` only; no network; exit 1 if cache empty. RED test with empty cache |
| Executable-file classification (build-relevant) | Applicable | Precise glob allow/deny list above; RED tests for both a build-relevant and a doc-only path |
| Destructive fs ops | N/A | No `rm`/mutation in new scripts (sweep read-only; run-pure writes only to `<dir>/out`) |

## Migration / Rollout

No data migration. Each PR is independently revertible (`git revert` of its merge); reverting any slice
leaves the 60-bats baseline green. `BUILD-STATE.md` seeds from current reality — no historical backfill.
The hook is opt-in, so existing clones are unaffected until an operator enables `core.hooksPath`.

## Open Questions

- [ ] Confirm PR3 = MINOR (new `run-pure-test.sh` script) vs PATCH at apply-time against CONTRIBUTING §4.
- [ ] `retros/INDEX.md` `deltas` column: integer count vs short list — resolve in sdd-tasks.
- [ ] Whether §0.a orient's "one active module" heuristic reads the Index table's most-recent
      `last_session` or requires the operator to name the module — default: operator names it, orient
      prints that module's line (meta-work exempt).
