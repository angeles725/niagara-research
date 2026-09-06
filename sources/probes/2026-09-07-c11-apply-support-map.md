# C11 apply-support map — branch/worktree map · per-PR command lists · PR bodies

Author: companero (Fable), 2026-09-06. Same shape as the C10 map. Kit design on main **`8da696c`** (v0.21.0 → v0.22.0);
QA REDs on `origin/qa/c11-*`. Execute-only. No Co-Authored-By / AI trailers. `[ev: kit design 8da696c]` `[ev: qa/c11-* tips]`

## 1. Branch / worktree map
| PR | Slice | Branch | QA RED(s) (tip) | Worktree | Real-tree smoke (expected @ ff1b659) |
|---|---|---|---|---|---|
| P1 | T1 shared parser | `feat/c11-shared-method-boundary` | `qa/c11-parser-oneliner` **d88af78** + `qa/c11-golden-parser` **ed2088f** (7 cases) | `niagara-tools-worktrees/c11-shared-method-boundary` | golden 7 cases pass on all 3 lints; 3×3 baselines IDENTITY except the one-liner (timers/silent flip 0→1); `git diff --stat` ≤ **700** |
| P2 | T3 concept-drift | `feat/c11-concept-drift` | `qa/c11-concept-drift` **77352a7** | `niagara-tools-worktrees/c11-concept-drift` | a `[concept]` row for a now-real slot → DRIFT row + exit 0 (1 under --strict); genuine concept → none |
| P3 | T2 client-root | `feat/c11-client-root` | `qa/c11-client-root` **54078f6** (path-based, 10 offenders → 0) | `niagara-tools-worktrees/c11-client-root` | 10 hardcodes → 0; lib exports ROOT+REPO+C8_REPO from one ff1b659 default; LD5 flips to exit 0 (rule stays LD1/LD3/LD6) |
| P4 | T4 guard-pins | `feat/c11-lint-guard-pins` | `qa/c11-guard-pins` **d6b840d** | `niagara-tools-worktrees/c11-lint-guard-pins` | scans 9 lints + itself; a header mutation with no fixture → WARN; all pinned → clean |
| PC | close | `chore/c11-close` | `qa/c11-close-checklist` (not yet cut) | `niagara-tools-worktrees/c11-close` | c11-close.bats green; sweep-fold-audit --strict 0 uncited |
PR ORDER (design): P1 first (lands `MB_AWK`; P2/P4 do not depend on it but the parser is the keystone); env-unset full bats
after P3 (T2 must pass with `C9_CLIENT_ROOT` unset). Note the actual QA branch names differ from the proposal's guesses
(`qa/c11-parser-oneliner`/`qa/c11-guard-pins`, not `c11-shared-parser`/`c11-lint-guard-pins`).

## 2. Per-PR command list (kit slice skeleton)
```bash
export PATH=/usr/bin:/bin:$PATH
K=/home/cristian/modulos_niagara_n4/niagara-tools; KIT="$K/build-n4-module-kit"
git -C "$K" fetch -q origin
git -C "$K" worktree add -b feat/c11-<slice> ../niagara-tools-worktrees/c11-<slice> origin/main
W="$K/../niagara-tools-worktrees/c11-<slice>"
git -C "$W" merge --no-edit origin/qa/c11-<red>        # cherry-pick the RED (bats + fixtures), never edit it
( cd "$W" && bats tests/<slice>.bats )                 # RED
# --- P1 only: 3×3 real-tree baselines BEFORE the cut (identity is the expected result) ---
CW=/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato-worktrees/main-ff1b659
for lint in lint-timers lint-ext-writable-shape lint-silent-protection; do
  for m in Compresores/CompPan/CompPan-rt Paccadia/ColdRoomPan/ColdRoomPan-rt Dashboard/DashboardPan/DashboardPan-rt; do
    "$W/build-n4-module-kit/toolbelt/$lint.sh" "$CW/$m/src" > "baseline-$lint-${m//\//_}.before"; done; done
# --- apply per design D1-D4 ---
# --- GREEN + OBSERVED mutation per invariant (each names its golden fixture; K24(7)) ---
( cd "$W" && bats tests/<slice>.bats && bats tests/golden-parser.bats && bats tests/ )
shellcheck "$W/build-n4-module-kit/toolbelt/<lint>.sh" "$W/build-n4-module-kit/toolbelt/lib/method-boundary.sh"
( cd "$W" && bats tests/kit-links.bats )
# P1: after-cut 3×3 baselines; diff must be IDENTITY except the one-liner; measure the ceiling
git -C "$W" diff --stat origin/main -- build-n4-module-kit/toolbelt | tail -1   # <= 700 authored (goldens excluded)
# P3: full bats with the env var UNSET (T2 must pass without C9_CLIENT_ROOT)
( cd "$W" && env -u C9_CLIENT_ROOT -u C9_CLIENT_REPO -u C8_CLIENT_REPO bats tests/ )
KIT="$W/build-n4-module-kit" "$W/build-n4-module-kit/toolbelt/new-retro.sh" kit campaign11-<slug>
```

## 3. Always-conflict fragment files (fragment-merge, take BOTH sides)
`BUILD-LOOP.md §5` · `skill/SKILL.md` toolbelt list · `report-module.sh` member rows · `retros/INDEX.md` (new-retro appends) ·
`BUILD-STATE.md` (new-retro sets retro_pending; close flips it). On conflict: keep every PR's row; never drop a peer's.

## 4. PR body template (one `[ev:]` per paragraph)
```
## What
<the refinement in one sentence>. [ev: retro campaign11-<slug>]
## Why
<the FP/FN/drift it fixes, verified before/after>. [ev: B832 / S<n> / close-lesson]
## Evidence
RED qa/c11-<red> <tip>; real-tree on ff1b659: <exact expected>; git diff --stat <n> (<=700). [ev: <lint>.sh <anchor>]
## Risk
<contract change? none? size ceiling?>. [ev: design 8da696c D<n>]
🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | QA RED tips (parser-oneliner d88af78, golden ed2088f, concept-drift 77352a7, client-root 54078f6, guard-pins d6b840d) | [CERT] | git rev-parse origin/qa/c11-* |
| 2 | lint-timers usage exit 2, block :188-235, ext-writable :137-178 (−42/+9) | [CERT] | design 8da696c :23,:112,:114; lint-timers.sh :58 |
| 3 | function-only MB_AWK via BASH_SOURCE; goldens excluded from the 700 ceiling | [CERT] | design D1b/D1c/D1k |
| 4 | env-unset full bats after P3 | [CERT] | design (T2 must pass with the var unset) |
