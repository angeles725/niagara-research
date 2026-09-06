# C9 PR13 close — FINAL execute-only package (from REAL kit main 4200e27)

Author: companero (Fable), 2026-09-06. EXECUTE-ONLY for a sonnet apply worker. Cut from the REAL kit state, NOT drafts:
`/home/cristian/modulos_niagara_n4/niagara-tools` at **4200e27** (VERSION `0.19.0`; `## [Unreleased]` already carries the
Wave-1 lint block PR2/PR3/PR10). Supersedes the skeleton in `2026-09-06-c9-close-apply-package.md`. Branch `chore/c9-close`
from kit `origin/main` (fresh fetch). No Co-Authored-By / AI trailers (CONTRIBUTING §6). `[ev: kit @ 4200e27, 2026-09-06]`

## 0. Sequence (do these IN ORDER)
1. **SP-smoke re-pin kit PR FIRST (separate, pins-only, BEFORE PR13):** re-pin the silent-protection smoke to the
   post-PR9 baseline — CompPan-rt **1** at `tests/silent-protection.bats:294` / ColdRoomPan-rt **0** (PR8 surfaced CR-3, so
   the CR-3 WARN is gone; PR9 left CP-1 as the one remaining trip). Pins-only, no lint code change. Merge it, THEN PR13.
2. PR13 close (steps 1-6 below).
3. Post-merge lead steps (§7).

## 1. CHANGELOG (`CHANGELOG.md`) — RENAME, do NOT recreate
- Rename `## [Unreleased]` → `## [v0.20.0] - 2026-09-<dd>`. KEEP the existing Wave-1 block verbatim
  (`### Added — Campaign 9 Wave 1: kit lints (PR2, PR3, PR10)` — the three lint bullets already carry their retro tokens).
- Rename that heading to `### Added — Campaign 9: kit lints + doctrine (PR2, PR3, PR10, PR12)` and APPEND the PR12 bullet:
  `- **Doctrine fold (PR12):** \`types/logic.md\` §Protection anatomy (alarm patterns A/B), \`types/logic-authoring.md\` ext-writable anti-shape line, \`BUILD-LOOP.md §5\` lint routing + §K22 real-tree smoke cross-ref, unified write-audit doctrine line [ev: retro campaign9-doctrine-fold].`
- ADD a `### Client / tunnel (referenced; not in this repo)` block with the REAL PR numbers:
  `- PANCCADIA client (angeles725/niagara-panccadia-leon): rotation PR#10, servlet guards PR#11/#12, alarm CR-3 PR#13, alarm CP-1 PR#14, write-path rows PR#15 — CompPan 2.0.3→2.2.0, Paccadia 2.0.7→2.1.0, Dashboard 2.1.1→2.2.0.`
  `- tunnel write-server: config login PR#1, audit schema PR#2, AuditHistory mirror PR#3 (blessed, awaiting merge).`
  (**Confirm the PR#→work mapping** with `gh pr list` per repo before pasting — the lead named kit #86/#87/#88/#90 (PR2/PR3/PR10/PR12), client #10–#15, tunnel #1/#2/#3; map each number to its branch at close time.)
- `### Tests` line: bats total was **369 @test across 35 files** at 4200e27; after adding `tests/c9-close.bats` (§4) record the NEW total (369 + the c9-close case count). State the number you measured, not a prediction.
- `VERSION`: `0.19.0` → `0.20.0` — in the SAME commit as the CHANGELOG (CONTRIBUTING §5).

## 2. Retros — INDEX flips + the new meta-lessons retro
- Flip the four `pending` → `folded` in `build-n4-module-kit/retros/INDEX.md` (rows :88-91): campaign9-demand-scope,
  -silent-protection, -ext-writable-shape, -doctrine-fold. (They already exist under `retros/`.)
- CREATE `retros/2026-09-06-campaign9-close-process-meta-lessons.md` from investigador1's draft
  (`niagara-research sources/probes/2026-09-06-c9-retro-drafts.md`, 19 lessons) PLUS the W2/W3 lessons the lead named:
  TTL-never-compared; comment-satisfiable pins ×4 (a bats `contains` pin passes on a `//`/`/* */` comment → strip comments
  before the assertion); HashMap on a shared servlet field (concurrency — use a thread-safe map or per-request state);
  pre-push hook needs BUILD-STATE in the push RANGE (a branch push is not proof; the hook evaluates the whole PR on main —
  BUILD-LOOP §108 envelope-pairing); workers never edit QA REDs; workers never write outside their own worktree.
  Add its INDEX row as `folded`.
- Also fold niagara-tools **#89** (lint-timers companion-flag FP) as a doc-note reference in the close notes; seeded S21/S22
  in `campaign9-research-candidates.md` for C10 (no C9 code) — see `2026-09-06-c9-kit-ticket-lint-timers-fp.md`.

## 3. Fold-token audit — VERIFIED PASS (do not re-derive; re-run to confirm)
I ran `sweep-fold-audit.sh --strict <INDEX-with-the-4-rows-flipped> build-n4-module-kit` at 4200e27: **81 folded, 81 cited,
0 uncited, exit 0.** Core citation counts (segment-aligned matcher, floor 6, excluding `retros/`+INDEX):
| slug | cited-by (core) | note |
|---|---|---|
| campaign9-demand-scope | BUILD-LOOP.md:70 | `[ev: retro campaign9-demand-scope]` |
| campaign9-silent-protection | BUILD-LOOP.md:70, types/logic.md:102 (+SKILL) | folded |
| campaign9-ext-writable-shape | BUILD-LOOP.md:70, types/logic-authoring.md:105 (+SKILL) | folded |
| campaign9-doctrine-fold | BUILD-LOOP.md:108 `[ev: retro doctrine-fold]` | credited by the SEGMENT match (`-doctrine-fold-` ⊂ `-campaign9-doctrine-fold-`) — an "ambiguous citation" NOTE, NOT a failure; a `campaign9`-qualified citation would be cleaner but is not required for exit 0 |
| campaign9-close-process-meta-lessons | BUILD-LOOP.md:108 `[ev: retro close-process-meta-lessons]` | same segment-credit; add one explicit fold line if you want it unambiguous |
Re-run the audit after the INDEX flips + the new retro row; it MUST stay exit 0.

## 4. `tests/c9-close.bats` — cherry-pick from QA, do not author
Source: `qa/c9-close-checklist` tip **0895507** (or later — `git log --oneline -1 origin/qa/c9-close-checklist`).
```bash
git -C <kit> checkout 0895507 -- tests/c9-close.bats   # QA owns it; never edit it on chore/c9-close
```
It pins (per 0895507): K22 exactly-once guard (D12), CHANGELOG lint-entry (R13.5), the harness-run record pin
(harness-only pins are never reported WSL-green). Run `bats tests/c9-close.bats` with `C9_CLOSE=1` — must be green after
steps 1-2 land.

## 5. BUILD-STATE kit envelope (`build-n4-module-kit/BUILD-STATE.md`) — section-scoped
- `retro_pending: true` → `false` (line ~161; GATED field — the hook holds the close until the owed retros exist; they do).
- `last_commit:` → the chore/c9-close merge sha (fill post-merge or with the branch tip).
- `last_session:` → `2026-09-06 · Campaign 9 CLOSE v0.20.0 — kit PR2/PR3/PR10/PR12 + client PR#10-#15 + tunnel PR#1-#3; 5 retros folded (demand-scope, silent-protection, ext-writable-shape, doctrine-fold, close-process-meta-lessons); #89 lint-timers FP filed → S21/S22 C10.`
- Only the kit self-envelope section changes (`sweep-build-state.sh` diffs the section).

## 6. Gates + commit
```bash
export PATH=/usr/bin:/bin:$PATH; cd <kit>
C9_CLOSE=1 bats tests/c9-close.bats
toolbelt/sweep-build-state.sh
toolbelt/sweep-fold-audit.sh --strict build-n4-module-kit/retros/INDEX.md build-n4-module-kit   # exit 0 (81+ folded, 0 uncited)
bats tests/                                                                                     # whole suite green
```
Commit (CONTRIBUTING §6; canonical bare-id promotion trailer):
```
chore(c9-close): v0.20.0 — CHANGELOG+VERSION, 5 retros folded, BUILD-STATE flip

Retro: promotion (folds campaign9-demand-scope campaign9-silent-protection campaign9-ext-writable-shape campaign9-doctrine-fold campaign9-close-process-meta-lessons)
```

## 7. Post-merge (lead)
`git tag v0.20.0 <merge-sha> && git push origin v0.20.0`; `scripts/install-skill.sh` (SKILL.md refresh); `sdd-archive` the
C9 change; settle the campaign-9 ledger; sync a niagara-research close note to the merge sha.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | kit main 4200e27, VERSION 0.19.0, [Unreleased] has PR2/PR3/PR10 block | [CERT] | `git log`, `cat VERSION`, CHANGELOG @ 4200e27 |
| 2 | 4 campaign9 retros exist; INDEX rows :88-91 pending | [CERT] | `ls retros/`, INDEX.md @ 4200e27 |
| 3 | sweep-fold-audit --strict passes with the 4 rows folded (81/81/0) | [CERT] | ran it on a flipped scratch INDEX, exit 0 |
| 4 | bats 369 @test / 35 files; c9-close.bats not yet in kit | [CERT] | grep tests/ @ 4200e27 |
| 5 | c9-close.bats tip 0895507 on qa/c9-close-checklist | [CERT] | `git log origin/qa/c9-close-checklist` |
| 6 | SP-smoke baseline CompPan-rt 1 / ColdRoomPan-rt 0 post-PR9; PR#→work map | [ev: lead; confirm at close] | lead message; `gh pr list` per repo |
