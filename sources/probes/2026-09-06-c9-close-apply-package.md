# C9 PR13 — close apply package (kit `chore/c9-close`, target v0.20.0) — mirrors the C8 close package (steps A–H)

Author: companero (Fable), 2026-09-06. Template = `2026-09-06-c8-close-apply-package.md` (C8 close = v0.19.0, main c0447c2,
20 PRs + close #85, 21 retros folded). Counts marked `<n>` are filled at close time from the merged PRs — never predicted.
`[ev: c8 close package]` `[ev: kit CONTRIBUTING §4 §5 §6]` `[ev: proposal f610d21 §4 PR table]`

## A. Preconditions (all must be true before branching)
- PR1–PR12 (+ R14) merged or explicitly deferred with an issue number; `sweep-build-state` clean on main; `bats tests/` green;
  every retro created by the campaign's kit PRs exists under `retros/` with its `campaign9-*` slug.
- Branch `chore/c9-close` from kit `origin/main` (fresh fetch — the C8 lesson: a stale checkout produced a false close terrain).

## B. CHANGELOG — `## [v0.20.0] - <date>` inserted ABOVE `[v0.19.0]` (CONTRIBUTING §5: VERSION + CHANGELOG in the same commit)
```markdown
## [v0.20.0] - 2026-09-<dd>

Campaign 9 — "close the silent seams": demand-scope lint, silent-protection lint, ext-writable-shape lint, the alarm
patterns A/B doctrine, and the unified write audit (write-server + servlet → one change_log).

### Added
- `toolbelt/lint-demand-scope.sh` — <n> pins (PR2, #<pr>) [retro campaign9-demand-scope]
- `toolbelt/lint-silent-protection.sh` — <n> pins + four-root smoke (PR3, #<pr>) [retro campaign9-silent-protection]
- `toolbelt/lint-ext-writable-shape.sh` — EW1–EW10 + four-root smoke (PR10, #<pr>) [retro campaign9-ext-writable-shape]
- `types/logic.md §Protection anatomy` — tiers + alarm patterns A/B (PR12, #<pr>)
- write-audit doctrine line (one canonical sink, surfaces, config_session) (PR7 kit-doc half + PR12)
### Changed
- `BUILD-LOOP.md §5` — K22 real-tree smoke cross-reference; routing rows for the three new lints (K19)
- `skill/SKILL.md` toolbelt list; `report-module.sh` member rows (R2/R3/R10)
### Client / tunnel (not in this repo — referenced)
- PANCCADIA: CompPan rotation (PR1, Compresores 2.1.0), alarm CR-3 (PR8, Paccadia 2.1.0), alarm CP-1 (PR9, Compresores 2.2.0),
  servlet guards (PR6), HMI config login (R14), write-path matrix rows 62+2 (PR11)
- tunnel: write-server config login + audit schema + spool/replay (PR4/PR5), AuditHistory mirror flag-OFF (PR7)
### References
- corpus B820–B830; retros campaign9-demand-scope · campaign9-silent-protection · campaign9-ext-writable-shape ·
  campaign9-doctrine-fold · campaign9-close-process-meta-lessons; proposal f610d21
```
`VERSION` → `0.20.0` (MINOR: additive lints + doctrine, no removed contract — CONTRIBUTING §4).

## C. Retros to create/finish in this PR (one per KIT PR without its own retro; client/tunnel retros live in their repos)
| Slug | Source PR | Must contain |
|---|---|---|
| `campaign9-demand-scope` | PR2 | S7 naming conflict (RED script name vs proposal) resolved by QA re-issue d0f5942; mutation flip observed |
| `campaign9-silent-protection` | PR3 | four-root count+subject+absence table; R3↔R8 coupling (CR-3 WARN closes when PR8 lands) |
| `campaign9-ext-writable-shape` | PR10 | the same naming conflict (D-a) + `set<Slot>` matching-action seam; module-find cross-check |
| `campaign9-doctrine-fold` | PR12 | which folds landed where (anchors), dangling-token ordering lesson |
| `campaign9-close-process-meta-lessons` | PR13 | (1) **line numbers vs content**: S20 rev 1 copied anchor line numbers from design prose while the content was verified — anchors are counted at the tip with `git show`/`grep -n`, never copied; (2) **stale checkout**: the local client clone at 4f5f1c7 produced a false "matrix absent" and D1 drift — read the tip through a worktree; (3) silent `git push \| tail` failure → verify `origin/main == HEAD` after every push; (4) two sessions sharing a name → address peers with `[ref]`; (5) PATH mangled inside bash loops → `export PATH=/usr/bin:/bin:$PATH` first; (6) RED-vs-proposal script-name conflicts are lead decisions, resolved by QA re-issue, row token unchanged; (7) **a negative claim needs a grep that asked the question** ("coolOnSensorFault is HMI-only" came from a pattern that never searched for it — write-server.mjs:94 had it); (8) **read the code before naming a tool's defect** (bog-nav `--slot` was called "substring" from its symptoms; bog-nav.py:447 compared exactly — the cause was either-end matching, fixed endpoint-aware in 5bb1c223e); (9) **the deployed station bog lives on the Windows side** (`/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/stations/PANCCADIA/config.bog`) — two sessions searched WSL and one called it artifact absence |
Each retro: H1, context, what happened, lesson, promotion line (`Promotes: <doctrine anchor>` or `none`), `[ev:]` tokens.

## D. INDEX rows (`retros/INDEX.md`): one row per retro above, status `folded` for those PR12 already folded (demand-scope,
silent-protection, ext-writable-shape, doctrine-fold) and `pending` → `folded` for the meta-lessons retro once its lines land
in BUILD-LOOP/METHODOLOGY (the stale-checkout + line-number lessons fold into METHODOLOGY K-list as a NEW K23 "anchors are
counted at the tip; never copied from prose" — section-scoped edit).

## E. BUILD-STATE flip (section-scoped, exactly like C8)
- `BUILD-STATE.md` campaign section: `retro_pending: 5` → `0`; `campaign: 9` → `closed 2026-09-<dd> v0.20.0`; seed the C10
  line: `C10 seeds: per-user config re-auth (D-1 successor), defrost trial rooms 1/2/4 (green light pending), B830-G1 live confirm`.
- No other section touched (`sweep-build-state` diffs the section only).

## F. Guards (all must pass before the commit)
```bash
export PATH=/usr/bin:/bin:$PATH; cd <kit-root>
C9_CLOSE=1 bats tests/c9-close.bats        # NEW: pins CHANGELOG has [v0.20.0], VERSION == 0.20.0, 5 retros exist, INDEX rows folded
toolbelt/sweep-build-state.sh              # section-scoped flip only
toolbelt/sweep-fold-audit.sh --strict      # every [ev: retro campaign9-*] resolves; scans <kit-root> only
bats tests/                                # whole suite incl. kit-links (lint names resolve)
```
`tests/c9-close.bats` = copy of `tests/c8-close.bats` with the version/slug constants changed (C8 precedent).

## G. Commit (CONTRIBUTING §6: NO Co-Authored-By / AI trailers; conventional commit; bare-id promotion trailer)
```
chore(c9-close): v0.20.0 — CHANGELOG, VERSION, 5 retros folded, BUILD-STATE flip, K23

Retro: promotion (folds campaign9-demand-scope campaign9-silent-protection campaign9-ext-writable-shape campaign9-doctrine-fold campaign9-close-process-meta-lessons)
```

## H. Post-merge
`git tag v0.20.0 <merge-sha> && git push origin v0.20.0`; `install-skill` (SKILL.md refresh); `sdd-archive` for the C9 change;
sync `2026-09-06-c9-close-terrain` in niagara-research to the merge SHA (as `b8af5d2d8` did for C8); memory note
`build-n4-campaign9-close` (mirror of the C8 one).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | steps A–H mirror C8 | [CERT] | c8 close package (same headings) |
| 2 | VERSION+CHANGELOG same commit; tag vX.Y.Z; no trailers | [CERT] | CONTRIBUTING §4 §5 §6 |
| 3 | retro slugs = one per kit PR + meta | [CERT] | lead's list + proposal §4 |
| 4 | K23 wording | [INFER] | draft — lead/QA to accept |
