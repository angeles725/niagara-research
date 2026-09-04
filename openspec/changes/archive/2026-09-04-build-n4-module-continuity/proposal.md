# Proposal: build-n4-module-continuity

## Intent
The `/build-n4-module` kit loses knowledge three ways. (A) **No session continuity**: sessions forget where the last one left a module — no ledger, no "record". (B) **Retros are write-only**: 30 retro files hold ~10 proven, sometimes safety-critical lessons never folded into the ~70-line living methodology. (C) **Tests do not bite**: QA discipline lives in retros, not as a mandated, fast, mutation-checked practice. Why now: unfiled lessons (timer arm, slot-retype outage, 0=block) have already caused production defrost failures and boot failures. This change makes the kit remember.

## Scope
### In Scope (one campaign, 3 chained PRs)
- **PR1 continuity registry** (MINOR): `BUILD-STATE.md` session ledger in the module root — adapt research-sdd `<!-- research-state.v1 -->` → `<!-- build-state.v1 -->`; read at BUILD-LOOP step 0 (Orient), written at step 7 (Retro); FOCUSES-style index if warranted.
- **PR2 retro-enforcement gate** (MINOR): make step 7 non-skippable via a `proposed_deltas_unfiled` counter + `toolbelt/verify-retro.sh` (fails when kit code changed without a retro/state update), plus a fold-back/promote sub-process so the pile shrinks.
- **PR3 promote proven lessons** (PATCH): fold ≥5 (target 5–12) write-only lessons into METHODOLOGY.md / BUILD-LOOP.md / types/ / build-verify.md — timer `started()`+`atSteadyState()`, never-retype-a-slot-with-saved-.bog, 0=disabled-for-limit-slots, module.palette check, safety-defaults, 4-layer QA stack, exact JUnit command, mutation-bite + fast-test rules.

### Out of Scope (non-goals)
- NO changes to the N4 modules themselves (BDefrostController et al.).
- NO PITest/mutation framework — mutation "bite" stays a manual break-and-verify rule.
- ng-deploy backup-bloat cleanup deferred.

## Approach
Stacked-to-main, sequential, ff-only, linear history. Strict TDD per niagara-tools CONTRIBUTING: each new toolbelt script (`verify-retro.sh`, `run-pure-test.sh`) lands TEST-FIRST — QA authors a biting bats test that fails first; trabajador writes the minimal script. Pre-commit `bats tests/*.bats && shellcheck` must exit 0; existing 60-test baseline stays green. Conventional Commits, no AI attribution. PR1/PR2 = MINOR + CHANGELOG (VERSION 0.5.0→…); PR3 = PATCH.

## Risks
| Risk | Likelihood | Mitigation |
|---|---|---|
| BDefrostController has zero tests + known prod bug | High | Out of scope here; PR3 promotes the timer-arm lesson so future work catches it |
| Retro pile keeps growing without a scheduled fold | Med | PR2 gate + fold-back sub-process shrinks debt each loop |
| Ledger ignored — discipline depends on step-7 gate | Med | `verify-retro.sh` fails the commit when kit code changes without a state/retro update |

## Rollback Plan
Each PR is independently revertible (git revert of its merge). PR1/PR2 add only new toolbelt scripts + docs; PR3 is doc-only. Reverting any slice leaves the 60-test baseline green.

## Success Criteria
- [ ] Existing 60 bats tests stay green.
- [ ] New scripts land test-first with biting tests (fail before implementation).
- [ ] ≥5 write-only lessons promoted into living kit files.
- [ ] `BUILD-STATE.md` exists and is read at BUILD-LOOP step 0.
- [ ] `proposed_deltas_unfiled` counter enforced at step 7.
