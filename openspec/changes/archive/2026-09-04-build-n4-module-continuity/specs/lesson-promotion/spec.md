# Lesson Promotion Specification (PR3)

## Purpose

Drain the retro pile of its highest-value lessons by folding them into the kit's
living files so future sessions encounter the rules before the mistakes.

## Requirements

### Requirement: Minimum Five Lessons Promoted

≥5 proven lessons MUST be added as explicit rules in METHODOLOGY.md, BUILD-LOOP.md,
or the relevant `types/*/build-verify.md` file. Lessons buried in retro prose do
NOT count as promoted. The five mandated minimum lessons are:

| # | Lesson | Source |
|---|--------|--------|
| 1 | Arm timer in `started()` + `atSteadyState()` — missing arm causes silent defrost failure | B729 |
| 2 | Never retype a slot with saved station data — .bog decode fails, station will not boot | B7xx |
| 3 | `0` as a limit-slot default blocks instead of disabling — causes rack startup failure | B7xx |
| 4 | Extract pure-class BEFORE authoring BComponent; test it; run bite check | B7xx |
| 5 | module.palette must list one entry per @NiagaraType — empty palette ships silently | B7xx |

#### Scenario: Lessons appear in living files

- GIVEN PR3 is merged
- WHEN a developer reads METHODOLOGY.md or BUILD-LOOP.md from scratch
- THEN all 5 lessons MUST appear as explicit numbered or headed rules — not
  merely referenced by retro filename

#### Scenario: Additional lessons welcome

- GIVEN the retro pile contains more than 5 high-value lessons
- WHEN PR3 is authored
- THEN additional lessons (up to ~12) MAY be promoted without changing the PR3
  scope or requiring a new PR

### Requirement: PR3 Is Documentation-Only

PR3 MUST NOT change any toolbelt script, any module source, any bats test, or
any executable file. All changes MUST be limited to `.md` files.

#### Scenario: diff contains only markdown

- GIVEN the PR3 candidate diff
- WHEN the diff is inspected
- THEN zero `.sh`, `.java`, `.bats`, `.gradle`, or `.xml` files MUST appear in
  the changed-file list

### Requirement: PR3 Version Bump Is PATCH Only

PR3 MUST increment the kit VERSION as a PATCH release (e.g. 0.5.1) and update
CHANGELOG. PR3 MUST NOT trigger a MINOR or MAJOR bump.

#### Scenario: VERSION file is PATCH after PR3

- GIVEN VERSION reads 0.5.0 after PR1+PR2 land
- WHEN PR3 is merged
- THEN VERSION MUST read 0.5.1

## Non-Goals for This Spec

- DefrostController extraction (BComponent → pure class) is NOT in PR3 scope;
  PR3 promotes the timer-arm rule to steer future work toward that extraction.
- No new toolbelt scripts ship in PR3.
