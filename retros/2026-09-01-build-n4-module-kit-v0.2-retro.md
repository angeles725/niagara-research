<!-- propose-never-apply -->
# Retro — build-n4-module kit v0.2: three-session SDD chain (docs fold-in · verify gate · release)

**Date**: 2026-09-01 · **Scope**: the `build-n4-module` skill launcher + its kit (`niagara-tools/build-n4-module-kit`, repo `angeles725/niagara-tools` v0.3.0 → v0.4.0) · **Status**: PROPOSE-ONLY (the kit deltas in §6 are proposals for human review; nothing here is applied)

**Trigger**: the operator asked three parallel Claude Code sessions (Investigador1 = coordinator/planning, Investigador2 = research + docs fold-in, QA = reproduction + tests + merge gates) to fold the build bitácora lessons into the kit via SDD, autonomously, with commit/push/PR/merge allowed and no questions to the operator. This retro is written by the QA session.

---

## 1. What the change did (measured)

| Fact | Value | Source |
|---|---|---|
| SDD change | `build-n4-module-kit-v0.2` (explore · proposal · spec · design · tasks in `niagara-tools/openspec/changes/`); sdd-verify PASS-WITH-WARNINGS, 0 critical | engram #7937–#7943, verify #7947 |
| PRs merged (all `--ff-only`, 0 merge commits on `main`) | 6 — PR #2 docs-foldin (+285/−14, 10 files) · PR #1 toolbelt (+683/−26, 11 files) · PR #3 release (+73/−4, 7 files) · PR #5 `build.sh --help` leak fix + B7 · PR #4 `build.sh` usage line + third mirror guard in `build-verify.md` · PR #6 CHANGELOG test count | `gh api pulls`, `git log --merges` |
| Lessons folded into kit docs | 41 (dashboard 22 · logic 9 · build-verify 8 · METHODOLOGY/BUILD-LOOP/SOURCES/wb-widgets the rest) | design §1, tasks 1.2–1.18 |
| New toolbelt scripts | 4 — `verify-module.sh` (THE gate) · `build.sh` (rewrite) · `mirror-niagara-home.sh` (promoted + guards) · `stored-repack.sh` | PR #1 |
| New bats cases (each names its regression) | 27 (V1–V9 · B1–B7 · M1–M5 · S1–S3 · L1–L3) + `ng-deploy.bats` 26 unchanged = 53 | `bats tests/*.bats` → `1..53` |
| Committed binary fixtures | 0 — every jar/class/`niagara_home` is generated in-test by `tests/helpers/n4-fixtures.bash` | design §3.1 |
| Kit version / repo version | v0.1 → v0.2 (GROWING) / 0.3.0 → 0.4.0 | `README.md`, `VERSION`, `CHANGELOG.md` |
| Launcher | `~/.claude/skills/build-n4-module/SKILL.md` version 0.1 → 0.2, three-role doctrine, outside git (before/after recorded in PR #3 body + engram `launcher-diff`) | tasks 3.6–3.7 |
| Final gate on `main` @ 495388b = tag `v0.4.0` | bats 53/53 · shellcheck clean (scripts + toolbelt + bats + helpers) · `git log --merges` = 0 · real DashboardPan jars: 10 pass / 0 fail / 4 skip | QA tasks 3.8–3.18 + final gate |

**Doctrine that came out of it (settled by the coordinator after a spec/design conflict):** `verify-module.sh` is the gate, independent of who built the jar; `build.sh` is the recommended WSL build and runs the gate itself (slotomatic for every profile with sources); `scripts/ng-deploy.sh` is the station deploy wrapper (backup → build → copy → type-count verify; its slotomatic guard is `-rt` only). "Primary/fallback" wording was removed everywhere.

## 2. What the three-session workflow proved

- **Research and reproduction in parallel, before fold-in, pays for itself.** Investigador2 tagged every "verified/proven" claim; QA reproduced the WSL-checkable ones the same hour. Two claims were corrected before they reached a kit file (one-plugin-per-install, STORED = manual post-step), one was strengthened (the `.frame` trap), and the station-only ones (A1, B6, G2, G3) were folded with `[CERT-live]` and never asserted by a test.
- **Writing spec and design in parallel is fast but produces contradictions; a validator must read both.** QA found six spec-vs-design conflicts touching its slice (stored-repack CLI, test-file placement, `--niagara-home` flag vs positional, `--src` semantics, `==` vs `<=` on the baja version, report-line format) plus one doctrine contradiction (launcher "ng-deploy primary" vs the coordinator's decision). The coordinator's design-validation pass found more; ten in total were resolved before `tasks.md` existed. Rule: whoever implements reads BOTH artifacts and lists every contradiction in one message with a proposed resolution per line.
- **Tests with a named regression survive the operator's "no filler tests" rule.** Every `@test` title ends with the failure it guards, and each suite was proven to FAIL on the pre-fix state (kit-links red on the dangling names, build.sh adding the empty `-wb` scaffold, the old first-class-only bytecode check) before turning green. 27 cases (B7 was added the same day when Investigador2 found `build.sh --help` leaking a code line — the test was seen red on the old script first), 0 that could be deleted without losing coverage of a real incident.
- **A read-only pre-review loop on the docs branch shortens the merge gate to minutes.** QA ran the design §9 greps + `kit-links.bats` on every push Investigador2 announced and replied with deltas only. It caught the leftover `## Primary:` / `## Fallback:` headings that the author's forbidden-string grep did not cover.
- **Fixed merge order with an explicit "red by construction" contract works.** `kit-links.bats` was authored in PR2 but asserts PR1's renames; it was declared red on the un-rebased branch and green at PR2 merge time on the rebased head. A simulated rebased head (PR1 kit + PR2 scripts) proved it 3/3 green before the real rebase.
- **The operator's "keep working in worktrees, help each other" instruction removed the only idle time.** QA landed its slice as a draft PR before `tasks.md` existed (every contract decision was already settled) and used the wait to pre-review PR1 and write the GOTCHAS patch for PR3.

## 3. Defects and false defects found on the way

| # | Finding | Verdict | Where it went |
|---|---|---|---|
| D1 | `scripts/ng-deploy.sh` showed as modified (mode 100755 → 100644) on `main` | FALSE DEFECT — the git index always held 100755; only the working tree had drifted. Restored with `git checkout`; the planned "exec-bit" commit was dropped | tasks 2.x, design §5 |
| D2 | Retro A1 (restart-persistent `BAbsTime` timer) was about to be folded as `[CERT-live]` | Overstatement — the API was verified in source (`[CERT]`) but the restart re-arm was never smoke-tested on a station; folded as `[CERT] + [INFER · pending station smoke-test]` | `types/logic.md` |
| D3 | Retro said gradle plugin versions 7.6.1/7.6.3/7.6.5 are "common to both" installs | WRONG — each install ships exactly ONE `niagara-module` plugin: 4.13.2 → 7.3.40, 4.14 → 7.6.17, 4.15.3 → 7.6.22. `-PniagaraPluginVersion` override is mandatory, not optional | `build-verify.md`, `build.sh --plugin-version` |
| D4 | DashboardPan `index.html` plano: four values must agree with the image | The four DO agree, but only because `#frame { aspect-ratio:1248/891 }` shadows a stale `.frame { aspect-ratio:1247/771 }` by specificity. Rule folded: exactly ONE declaration, delete the stale one, never shadow | `types/dashboard.md`; module fix still open (§4) |
| D5 | Design's fixture recipe `printf '\312\376\272\276\0\0\0\%o'` | Prints a backslash + digits, not a byte. Fixed: render the escape first (`esc=$(printf '\\%03o' N)`) then `printf '%b'` | `tests/helpers/n4-fixtures.bash` |
| D6 | `build.sh` selected profiles by directory existence | Real bug: the empty `DashboardPan-wb` scaffold (gradle file, no sources) was added to every build; also only the first `.class` was checked for major 52 | `build.sh` predicate (gradle file AND sources), `verify-module.sh` every class |
| D7 | bitácora `mirror-niagara-home.sh` ran `rm -rf "$mir"` on any user path; tilde example not expanded by bash | Real hazard; promoted with two guards (exit 20) + `.niagara-mirror` marker; example uses `$HOME` | `toolbelt/mirror-niagara-home.sh` |
| D8 | QA's checkout step reused Investigador1's `release` worktree and detached its HEAD | Process slip, restored at once, no file touched. Rule: a QA worktree always gets a unique name; never `grep` for another session's worktree | this retro |

## 4. Open follow-ups (not in scope of v0.2)

1. **Missing `v0.3.0` tag** on the release commit of niagara-tools — tag it retrospectively (tasks 4.4).
2. **`CONTRIBUTING.md` §8 "no remote" claim is stale** — the repo has `origin` and PRs now (tasks 4.5).
3. **`ng-deploy.sh` runs slotomatic for `-rt` only** — a `@NiagaraType` edit in `-ux` (e.g. `BDashboardServlet`) is never regenerated by the deploy wrapper. Documented in `build-verify.md`; the fix (or integrating `verify-module.sh` into `ng-deploy.sh`) is a later change.
4. **Attribution-trailer conflict.** The operator's global CLAUDE.md and CONTRIBUTING §6 forbid `Co-Authored-By`/AI attribution; the harness directive mandates a `Co-Authored-By` + `Claude-Session` trailer. QA's commits carry none; Investigador1/2's commits carry the trailer. The operator decides once: strip the trailers (rewrite is off the table on `main`; accept as-is going forward) or update CONTRIBUTING §6.
5. **DashboardPan module: delete the stale `.frame { aspect-ratio:1247/771 }`** in `DashboardPan-ux/src/rc/index.html` (line 84) so the rule in D4 holds in the live module, then rebuild with `build.sh`.
6. **`SKILL.md` decision table still labels logic "seed"** while the kit says GROWING — a one-word launcher edit. (The `build.sh` usage line in `build-verify.md` was closed by PR #4.)
7. **STORED jar loading on a running station** is still `TODO(verify)` in the 5-rooms retro — field-confirm once.

## 5. The vital method to PRESERVE

- Reproduce before you fold. A claim is `[CERT]` only with a citation; station-only facts stay `[CERT-live]` with a bitácora § reference and are never turned into a WSL test.
- One implementation contract per line, from one owner, before code. The six-line conflict list with a "proposed" column resolved everything in one round trip.
- A test earns its place by naming the incident it would have caught, and by having been seen red.
- Merge order + rebase + `--ff-only`; the child PR proves `git diff --stat main...HEAD` shows only its files.
- Propose-never-apply for kit doctrine: the sessions folded PROVEN lessons into the kit; everything not proven stays here as a proposal.

## 6. PROPOSED kit deltas for the next version (propose-never-apply)

| # | Proposal | Target | Value / cost | Evidence |
|---|---|---|---|---|
| P1 | Make `verify-module.sh` a hard step of `ng-deploy.sh` (run on `build/libs` before copy; fail = no deploy) | `scripts/ng-deploy.sh` + `tests/ng-deploy.bats` | HIGH / MED (touches the deploy wrapper's exit surface → MINOR bump) | doctrine "a jar that has not passed the gate does not go to a station" is currently manual after ng-deploy |
| P2 | Add `-ux` slotomatic to `ng-deploy.sh` (`--with-slotomatic` runs it for every profile that has `@Niagara*` annotations) | `scripts/ng-deploy.sh` | HIGH / LOW | §4.3; `build.sh` already does it |
| P3 | `verify-module.sh --plano <index.html>`: assert the four plano values agree AND exactly one frame `aspect-ratio` declaration exists | `toolbelt/verify-module.sh` (opt-in) | MED / LOW | D4; grep-checkable today |
| P4 | Kit preflight script `toolbelt/preflight.sh <niagara_home>`: JDK 8 present, `etc/m2` plugin version vs `settings.gradle.kts` pin, station lock on the target jar, WSL `/mnt/c` path form | new | MED / LOW | BUILD-LOOP §0.b is prose only; every item was a real failure |
| P5 | Record the bats + shellcheck gate as a git pre-push hook template in the kit (opt-in install) | `toolbelt/` + CONTRIBUTING | LOW / LOW | gate is human/agent-run only (CONTRIBUTING §8) |
| P6 | Kit `retros/` convention: line 1 marker `<!-- review-status: pending -->` on new retros, `folded vX.Y · date` when folded; `kit-links.bats` L4 asserts every retro has a marker | `retros/`, `tests/kit-links.bats` | LOW / LOW | v0.2 used the marker as the only sweep hook (spec Q5) |
| P7 | Three-session template for kit changes: coordinator writes explore→tasks, researcher tags claims, QA reproduces + owns tests and merge gates; contradictions list before tasks; read-only pre-review loop per push | `research-sdd` kit / `build-n4-module` SKILL.md "Retro" step | MED / LOW | §2 of this retro |

## 7. Verdict

The kit went from a seeded v0.1 with a prose-only gate to a v0.2 whose gate is an executable, tested script, with 41 proven lessons folded and every superseded claim removed. The three-session split held because roles were disjoint (plan / research+docs / reproduce+test+gate) and every cross-session decision was written down once. The remaining risk is not in the kit but in the two deploy paths that still bypass the gate (§4.3, P1/P2).
