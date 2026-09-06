# C9 branch / worktree map — all 14 PRs in one table

Author: companero (Fable), 2026-09-06. Branches from proposal f610d21 §4 (PR1–PR13); R14 has no PR row there (see the flag).
Bases: kit = `niagara-tools` `origin/main` (fresh fetch at branch time); client = `a109249`; tunnel = `9acb47c`.
Worktree convention: sibling `<repo>-worktrees/<name>` (CodeGraph rule: never under /tmp). Existing today: client
TWO roots, both registered to `Cliente/Leon-Guanjuato` (`git worktree list` 2026-09-06): `modulos_niagara_n4/Leon-Guanjuato-worktrees/{c9-a109249,c9-alarm-cp1,c9-alarm-cr3,c9-config-login,c9-rotation,c9-s12-servlet,lifecycle-btest}` AND `Cliente/Leon-Guanjuato-worktrees/{main-a109249 (the lead's read tree, a109249), pr1-s20 (feat/c9-comppan-rotation @ cbab006 — the W1 PR1 apply tree)}`; the `Cliente/Leon-Guanjuato` checkout itself is STALE at 4f5f1c7 — never read from it;
kit `niagara-tools-worktrees/` (exists, EMPTY); tunnel has NO worktrees dir yet → create
`/home/cristian/tunnel/clientes/Leon-Guanajuato/Pancaddia-worktrees/`. `[ev: ls 2026-09-06]`

| PR | Branch | Repo (base) | RED (branch @ tip) | Worktree path | Apply package | Order/constraints |
|---|---|---|---|---|---|---|
| PR1 | `feat/c9-comppan-rotation` | client CompPan-rt (a109249) | `qa/c9-comppan-rotation` cf28572 | `…/Leon-Guanjuato-worktrees/c9-rotation` (exists) | s20-rotation rev 2 | first of the client wave; Compresores `:33` 2.0.3→2.1.0 |
| PR2 | `feat/c9-demand-scope` | kit (main) | `qa/c9-demand-in-scope` d0f5942 | `…/niagara-tools-worktrees/c9-demand-scope` | s7-demand-scope-lint | independent |
| PR3 | `feat/c9-silent-protection` | kit (main) | `qa/c9-silent-protection` e38e503 | `…/niagara-tools-worktrees/c9-silent-protection` | s18-silent-protection-lint | smoke table re-pinned after PR8 |
| PR4 | `feat/c9-s12-config-login` | tunnel (9acb47c) | `qa/c9-s12-write-server` e7e6615 | `…/Pancaddia-worktrees/c9-s12-config-login` | r5-change-log-audit rev 2 (login half) | before PR5 or together |
| PR5 | `feat/c9-s12-audit-schema` | tunnel (9acb47c) | `qa/c9-s12-write-server` e7e6615 (same RED) | `…/Pancaddia-worktrees/c9-s12-audit-schema` | r5-change-log-audit rev 2 (schema+spool half) | before PR7 |
| PR6 | `feat/c9-s12-servlet-guards` | client DashboardPan-ux (a109249) | `qa/c9-s12-servlet` 4c18837 | `…/Leon-Guanjuato-worktrees/c9-s12-servlet` (exists) | s12 plan Part 2 | before R14 |
| PR7 | `feat/c9-s12-audit-mirror` | tunnel (9acb47c) + kit doc line | `qa/c9-s12-audit-mirror` 0a14df8 | `…/Pancaddia-worktrees/c9-s12-audit-mirror` (+ kit line via PR12) | pr7-audit-mirror | after PR5; flag OFF |
| PR8 | `feat/c9-alarm-cr3` | client ColdRoomPan-rt (a109249) | `qa/c9-alarm-cr3` 70a357b | `…/Leon-Guanjuato-worktrees/c9-alarm-cr3` (exists) | pr8-pr9-alarm §A | Paccadia `:33` 2.0.7→2.1.0; closes the R3 CR-3 WARN |
| PR9 | `feat/c9-alarm-cp1` | client CompPan-rt (a109249) | `qa/c9-alarm-cp1` 8b43488 | `…/Leon-Guanjuato-worktrees/c9-alarm-cp1` (exists) | pr8-pr9-alarm §B | AFTER PR1 (Compresores `:33` 2.1.0→2.2.0 fragment-merge) |
| PR10 | `feat/c9-ext-writable-shape` | kit (main) | `qa/c9-ext-writable-shape` 3726722 | `…/niagara-tools-worktrees/c9-ext-writable-shape` | pr10-ext-writable-shape-lint | needs the D-a name decision (QA re-issue) |
| PR11 | `docs/c9-write-path-measured-rows` | client docs/ (a109249) | extends `qa/c8-write-path` 5e357d1 (SC-9 exit 0 is the pin) | `…/Leon-Guanjuato-worktrees/c9-write-path-rows` | pr11-write-path-matrix-rows | after PR1 (its 2 rows) or PR1 carries them |
| PR12 | `docs/c9-doctrine` | kit (main) | none (sweep-fold-audit --strict + kit-links) | `…/niagara-tools-worktrees/c9-doctrine` | pr12-doctrine-fold-drafts | after PR3/PR10 (names) and after the campaign9-* retros exist |
| PR13 | `chore/c9-close` | kit (main) | `tests/c9-close.bats` (new) | `…/niagara-tools-worktrees/c9-close` | c9-close-apply-package | LAST |
| R14 | **`feat/c9-s12-hmi-config-login`** (proposed) | client DashboardPan-ux (a109249) | `qa/c9-s12-config-login` cc1c948 | `…/Leon-Guanjuato-worktrees/c9-config-login` (exists) | r14-config-login rev 3 | after PR6; **FLAG**: the proposal has no PR row for R14 and its natural name `feat/c9-s12-config-login` collides with PR4's tunnel branch — different repos, so git allows it, but the PR list would read as duplicates; recommend the `-hmi-` name |

## Worktree creation (per repo, once)
```bash
export PATH=/usr/bin:/bin:$PATH
# kit
cd /home/cristian/modulos_niagara_n4/niagara-tools && git fetch -q origin && git worktree add -b feat/c9-demand-scope ../niagara-tools-worktrees/c9-demand-scope origin/main
# client (base a109249)
cd /home/cristian/modulos_niagara_n4/Leon-Guanjuato && git worktree add -b docs/c9-write-path-measured-rows ../Leon-Guanjuato-worktrees/c9-write-path-rows a109249
# tunnel (create the sibling dir first)
cd /home/cristian/tunnel/clientes/Leon-Guanajuato/Pancaddia && mkdir -p ../Pancaddia-worktrees && git worktree add -b feat/c9-s12-config-login ../Pancaddia-worktrees/c9-s12-config-login 9acb47c
```
Existing client worktrees were created on the RED branches (c9-rotation, c9-alarm-*, c9-s12-servlet, c9-config-login) — at
apply, branch the `feat/*` from a109249 INSIDE that worktree (`git switch -c feat/… a109249`) and merge/cherry-pick the RED
test files, or add a second worktree; do not commit implementation on the `qa/*` branches (QA owns them).

## Wave suggestion (from the constraints column)
Wave 1 (parallel, no coupling): PR1 · PR2 · PR3 · PR4+PR5 · PR6 · PR10(after D-a). Wave 2: PR7 · PR8 · PR9 · PR11 · R14.
Wave 3: PR12 → PR13.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | branch names PR1–PR13 | [CERT] | proposal f610d21 §4 :99-111 |
| 2 | RED tips | [CERT] | QA branch heads listed in the C9 assignments file (7c3bbf2c7) |
| 3 | existing worktrees; tunnel dir absent | [CERT] | `ls` 2026-09-06 |
| 4 | R14 branch name | [INFER/proposal] | lead decides |
