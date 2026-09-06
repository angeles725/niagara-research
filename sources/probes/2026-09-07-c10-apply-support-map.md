# C10 apply-support map — branch/worktree map · per-PR command lists · PR6 patch

Author: companero (Fable), 2026-09-06. Same shape as the C9 branch/worktree map. Kit specs on main `2ff4a6e`; QA REDs are
on `origin/qa/c10-*` (tips below). Execute-only for the apply workers. No Co-Authored-By / AI trailers (CONTRIBUTING §6).
`[ev: git branch -a origin/qa/c10-*, 2026-09-06]` `[ev: kit main 2ff4a6e]` `[ev: client main ff1b659]`

## 1. Branch / worktree map
| PR | Slice | Branch | Repo (base) | QA RED (branch @ tip) | Worktree path | Real-tree smoke (expected) |
|---|---|---|---|---|---|---|
| P1 | S21 lint-timers FP | `feat/c10-lint-timers-fp` | kit (main 2ff4a6e) | `qa/c10-lint-timers-fp` **52ebd11** | `niagara-tools-worktrees/c10-lint-timers-fp` | ColdRoomPan-rt/src → BDefrostController no longer FAILs; field-flag case still FAILs |
| P2 | S23 silent-protection surfaces | `feat/c10-silent-protection-surfaces` | kit (main) | `qa/c10-silent-protection-surfaces` **f981754** | `niagara-tools-worktrees/c10-silent-protection-surfaces` | CompPan-rt/src → **0** (CP-1 recognised via Pattern B); ColdRoomPan-rt 0; DashboardPan 0 |
| P3 | S22 ext-writable per-slot | `feat/c10-ext-writable-per-slot` | kit (main) | `qa/c10-ext-writable-per-slot` **954ebd7** | `niagara-tools-worktrees/c10-ext-writable-per-slot` | CompPan-rt gains `faultReset` (**1** WARN); DashboardPan-rt 1 (`setpoint`); ColdRoomPan-rt/DashboardPan-ux 0 |
| P4 | S24 cwd-independent tests | `feat/c10-structural-cwd` | kit (main) | `qa/c10-structural-cwd` **a792d7a** | `niagara-tools-worktrees/c10-structural-cwd` | run-pure-test.sh from `/tmp` → the two WiringTests GREEN |
| P5 | S25 lint-write-path STALE | `feat/c10-write-path-strict` | kit (main) | `qa/c10-write-path-strict` **a56a72e** | `niagara-tools-worktrees/c10-lints` (exists @ a56a72e) or a fresh one | matrix-root STALE = **5** before PR6 marker, **0** after; uncovered FAIL unchanged |
| P6 | S26 client gitignore | `chore/c10-gitignore` | client (ff1b659) | none (chore) — also carries the 5 `[concept]` matrix marks | `Leon-Guanjuato-worktrees/c10-gitignore` | STALE flips 5→0 on the client tree; keep-set proof (§PR6) |
| PC | close | `chore/c10-close` | kit (main) | `qa/c10-close-checklist` **41bca42** | `niagara-tools-worktrees/c10-close` | c10-close.bats green; sweep-fold-audit --strict 0 uncited |
Kit worktrees dir exists (`/home/cristian/modulos_niagara_n4/niagara-tools-worktrees/`); client dir exists
(`/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato-worktrees/`, holds `c10-ff1b659` read-only + `main-a109249`).

## 2. Per-PR command list (kit slices P1-P5 — same skeleton; substitute branch/RED/smoke)
```bash
export PATH=/usr/bin:/bin:$PATH
K=/home/cristian/modulos_niagara_n4/niagara-tools; KIT="$K/build-n4-module-kit"
git -C "$K" fetch -q origin
# 1) worktree from main; MERGE the QA RED commit first (brings tests/<slice>.bats + specs), then run it RED
git -C "$K" worktree add -b feat/c10-<slice> ../niagara-tools-worktrees/c10-<slice> origin/main
W="$K/../niagara-tools-worktrees/c10-<slice>"
git -C "$W" merge --no-edit origin/qa/c10-<slice>            # the RED bats + delta specs (QA owns the branch; we merge, never edit it)
( cd "$W" && bats tests/<slice>.bats )                       # RED: fails for the right reason (rule not yet refined)
# 2) apply the refinement per the S<n> apply-package (toolbelt/<lint>.sh awk edit)
# 3) GREEN
( cd "$W" && bats tests/<slice>.bats && bats tests/ )        # slice green + whole suite green
( cd "$W" && bats tests/kit-links.bats )                     # script name resolves (if a new/renamed script)
shellcheck "$W/build-n4-module-kit/toolbelt/<lint>.sh"       # 0
# 4) real-tree smoke on client ff1b659 (read-only) — EXACT expected from the map column
CW=/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato-worktrees/c10-ff1b659
"$W/build-n4-module-kit/toolbelt/<lint>.sh" "$CW/<module>/src"   # assert the exact count/flip
# 5) retro stub + the always-conflict fragments (see §3), commit, push, PR
KIT="$W/build-n4-module-kit" "$W/build-n4-module-kit/toolbelt/new-retro.sh" kit campaign10-<slug>
```
Per-slice fills:
- P1 S21: `<slice>`=lint-timers-fp, `<lint>`=lint-timers.sh, `<module>`=Paccadia/ColdRoomPan/ColdRoomPan-rt, expected: no companion-flag FAIL; slug `campaign10-lint-timers-fp`.
- P2 S23: silent-protection-surfaces, lint-silent-protection.sh, Compresores/CompPan/CompPan-rt, expected **0** WARN; slug `campaign10-silent-protection-surfaces`.
- P3 S22: ext-writable-per-slot, lint-ext-writable-shape.sh, Compresores/CompPan/CompPan-rt, expected **1** WARN (`faultReset`); slug `campaign10-ext-writable-per-slot`. (Contract change — the RED 954ebd7 already re-pins EW10 CompPan-rt 0→1.)
- P4 S24: structural-cwd, run-pure-test.sh (subshell `cd "$rt"` fix), smoke = run from `/tmp`; slug `campaign10-structural-cwd`.
- P5 S25: write-path-strict, lint-write-path.sh, smoke = STALE 5→0 (matrix-root harvest, §S25 recipe); slug `campaign10-write-path-strict`.

## 3. Always-conflict fragment files (fragment-merge, never wholesale) — the C8/C9 rule
Each kit lint PR touches the SAME shared files; resolve by ADDING your one line/row, keeping every other PR's:
- `build-n4-module-kit/BUILD-LOOP.md` §5 — the lint routing bullet (only S25 changes an existing lint's flag row; S21/S23/S22 are refinements, no new row unless a flag/exit changes — they do not).
- `build-n4-module-kit/skill/SKILL.md` — toolbelt list (same; only a new/renamed script needs a line — none here).
- `build-n4-module-kit/toolbelt/report-module.sh` §5.5/§5.6 member rows (only if a lint's invocation changes — refinements keep the same call).
- `build-n4-module-kit/retros/INDEX.md` — new-retro.sh APPENDS the row (idempotent; each PR its own row → no textual conflict if appended in order).
- `build-n4-module-kit/BUILD-STATE.md` — new-retro.sh sets `retro_pending: true`; the close flips it false (section-scoped, `sweep-build-state.sh` diffs the section).
Rule: on a conflict in any of these, take BOTH sides' additions; never drop a peer's row (C9 lesson — pre-push hook needs BUILD-STATE in the push range).

## 4. Retro stub + PR body template
- Stub: `new-retro.sh kit campaign10-<slug>` → `retros/<date>-campaign10-<slug>.md` + INDEX row + BUILD-STATE retro_pending true (one atomic call). Fill the stub's What/Why/Lesson/Promotes.
- PR body template (one `[ev:]` per paragraph):
```
## What
<the refinement in one sentence>. [ev: retro campaign10-<slug>]
## Why
<the FP/FN it fixes, with the verified before/after>. [ev: niagara-tools #89 / S<n>]
## Evidence
RED qa/c10-<slice> <tip>; real-tree smoke on ff1b659: <exact expected>. [ev: <lint>.sh <anchor>]
## Risk
<contract change? none? harness?>. [ev: <spec req id>]
🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## 5. PR6 exact patch (client `chore/c10-gitignore` @ ff1b659) — gitignore + the 5 `[concept]` marks
```bash
export PATH=/usr/bin:/bin:$PATH
C=/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato
git -C "$C" worktree add -b chore/c10-gitignore ../Leon-Guanjuato-worktrees/c10-gitignore ff1b659
W="$C/../Leon-Guanjuato-worktrees/c10-gitignore"
# (a) .gitignore — append
printf '\n# Gradle incremental-compile cache — churns every build; NOT the deploy artifacts (libs/manifest stay tracked)\n**/build/tmp/\n**/build/classes/\n' >> "$W/.gitignore"
# (b) untrack the cache (keep the working files), scoped to tmp+classes ONLY
keep_before=$(git -C "$W" ls-files '**/build/libs/*.jar' '**/build/manifest/**/module.xml' | sort)
git -C "$W" rm -r --cached $(git -C "$W" ls-files '**/build/tmp' '**/build/classes')
keep_after=$(git -C "$W" ls-files '**/build/libs/*.jar' '**/build/manifest/**/module.xml' | sort)
# (c) PROOF no deploy artifact was untracked (must print OK; diff empty)
diff <(printf '%s\n' "$keep_before") <(printf '%s\n' "$keep_after") && echo "OK: 8 deploy artifacts still tracked (4 jars + 4 module.xml), none lost"
git -C "$W" status --porcelain | grep -c '^D ' # == 51 (43 .class + 8 tmp), 0 of them libs/manifest
```
(d) the FIVE `[concept]` matrix marks — `docs/write-path-matrix.md` @ ff1b659, append ` [concept]` inside the first cell:
```
:31  | `hoaMode` [concept] | …            (keep the rest of the row verbatim)
:32  | `hoaMode` [concept] + inhibit active | …
:33  | `inhibit` [concept] (defrost signal) | …
:36  | `freezeEnabled` [concept] | …
:52  | `hoaMode` [concept] + `setpoint` (two writes) | …
```
(:40 `setpoint`+hoaMode UNtouched — its backtick-inner is `setpoint`, a real slot.)
Then: `lint-write-path.sh <root>` STALE = 0 on the client tree; commit both (.gitignore untrack + matrix marks) as the
chore. No jar, no version bump. `[ev: git ls-files @ ff1b659 — 43 .class + 8 tmp; 4 jars + 4 module.xml keep]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | C10 QA RED tips (52ebd11/954ebd7/f981754/a792d7a/a56a72e/41bca42) | [CERT] | `git rev-parse origin/qa/c10-*` |
| 2 | new-retro.sh writes stub+INDEX+BUILD-STATE atomically | [CERT] | new-retro.sh header |
| 3 | fragment files BUILD-LOOP/SKILL/report-module/INDEX/BUILD-STATE | [CERT] | grep @ kit main |
| 4 | PR6 keep-set 4 jars + 4 module.xml, untrack 43 .class + 8 tmp | [CERT] | git ls-files @ ff1b659 |
| 5 | expected smokes (S21 no-FAIL, S22 1, S23 0, S25 5→0) | [CERT] | lint runs @ ff1b659 (lint-refinement + S25 packages) |
