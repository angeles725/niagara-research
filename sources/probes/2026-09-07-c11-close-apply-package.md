# C11 close — apply package (kit `chore/c11-close`, target v0.22.0) — REFRESHED at merged main 53efff7

Author: companero (Fable), 2026-09-06. EXECUTE-ONLY. Cut from kit main **`53efff7`** (PR1 `127f25a`, PR2 `2aa32fd`, PR3
`53efff7` merged; PR4 lint-guard-pins running). VERSION `0.21.0` → `0.22.0`; no `## [Unreleased]` yet (the close writes the
`[v0.22.0]` block). No AI trailers (CONTRIBUTING §6). `[ev: kit 53efff7]`

## 1. Retros — INDEX flips + fold state (VERIFIED at 53efff7)
Three merged retros, all INDEX `pending`:
| Slug | Core citations @ 53efff7 | Close action |
|---|---|---|
| `campaign11-shared-method-boundary` | **1** (folded by PR1) | flip INDEX → folded |
| `campaign11-concept-row-drift` (NOTE: `-concept-row-drift`, not `-concept-drift`) | **1** (folded by PR2) | flip INDEX → folded |
| `campaign11-client-root` | **0** — NOT folded | **ADD a fold line before flipping** (else `sweep-fold-audit --strict` FAILs): METHODOLOGY new **K26** "single-source the client read tree" `[ev: retro campaign11-client-root]`; then flip |
| `campaign11-lint-guard-pins` (PR4, pending) | — | create via `new-retro.sh`; fold into BUILD-LOOP §5 (`lint-guard-pins.sh` pre-gate). **PR5 (close) MUST add the K24(7) guard-pin GRAMMAR sub-bullet** (it was removed from PR4): under METHODOLOGY K24(7): *"every `toolbelt/lint-*.sh` declares ≥1 header line `# Mutation: <fixture-id> -- <what it flips>` (ASCII `--`), each id an existing `@test`; `lint-guard-pins.sh` enforces it; 'clean' = MATCH count == lint count, never 0 found"* `[ev: retro campaign11-lint-guard-pins]` |
| `campaign11-close-process-meta-lessons` | — | create; §2 below |
Fold-audit gate: after all flips, `sweep-fold-audit.sh --strict INDEX kit-root` → 0 uncited (client-root's new K26 line + the guard-pins line make it pass).

### Additional doctrine folds this close carries
- **K24(7) guard-pin grammar (PR5, from PR4's dropped sub-bullet)** — see the `campaign11-lint-guard-pins` row above; the
  grammar line goes under METHODOLOGY K24(7) with `[ev: retro campaign11-lint-guard-pins]`.
- **lint-timers exit contract** — `lint-timers.sh` uses **0 no FAIL / 1 any FAIL / 2 usage / 3 env** (usage is exit **2**, not
  the generic 3). Fold a one-line note where METHODOLOGY **K20** (`:86`) gives the generic usage→3 example: "the disjoint-range
  rule permits a script to split usage(2) from env(3) — `lint-timers.sh` does" `[ev: retro campaign11-lint-guard-pins]`. (K20's
  0/1/2 vs 3/4 framing already allows this; the note prevents a future author from "fixing" lint-timers to exit 3.)

## 2. close-process meta-lessons (`campaign11-close-process-meta-lessons`) — from what APPLY taught
1. **I2/I3 are DEFENSIVE, not inert** — the Case-B `@`-line stop (I2) and the keyword exclusion (I3) have REACHABLE-BUT-ABSENT shapes (B832-G3/G4): the client tree happens not to contain an `if(…){` on the same line as a method open today, but the shape is reachable, so the guard earns its fixture (G-samemethod). Reclassify from "inert" to "defensive with a reachable shape".
2. **Fragment-merge slip (process)** — a merger raised on a single-line hunk and `git add -A && rebase --continue && push` was chained without checking → conflict markers were pushed for minutes. RULE: assert **0 conflict markers** (`! git grep -nE '^(<<<<<<<|=======|>>>>>>>)'`) BEFORE `git add`, and NEVER chain `add/continue/push` after an unchecked script.
3. **Real conflict markers already in a shipped archive** — the same sweep found leftover `<<<<<<<`/`>>>>>>>` in the C8 archive apply-progress.md (resolved `7053907`, both PR16+PR17 sections kept). New guard: **CLOSE-no-conflict-markers** pin (QA `46d3eff`) — the close bats fails if any tracked file carries a marker.
4. **c8-close SC1-smoke was VACUOUS** (`[ status 1 ] || [ status 0 ]` — always true) — the LD5 bug-vs-rule class again: a smoke that can't fail pins nothing. Tightened `d837ae5` (pins the blessed-tree verdict, not "either tree").
5. **The pre-push hook's range on an `--amend` lacks the retro anchor** — an amend re-writes the tip so the hook's push range no longer contains the `Retro:` trailer commit; workaround = an empty `Retro: none` commit to satisfy the envelope-pairing rule. Fold as a BUILD-LOOP §7 note.
6. **Two more K12 breaches** — workers ticked `tasks.md` in the MAIN checkout instead of their worktree; the recurring K12 (never write in the shared main checkout). Fold into K12's cross-ref.

## 3. CHANGELOG + VERSION
Write `## [v0.22.0] - 2026-09-<dd>`; `### Changed — Campaign 11: lint precision (T1-T4)`:
- **T1** shared `lib/method-boundary.sh` (function-only `MB_AWK`, peak-depth via `BASH_SOURCE`) — fixes the one-liner method false-negative; 3 real OBSERVED flips (lint-timers/lint-silent-protection now catch the one-liner; ext-writable unchanged) [ev: retro campaign11-shared-method-boundary]
- **T2** `tests/lib/client-root.bash` single-sources the client read tree — **10** hardcoded sites → 0 (5 C9_CLIENT_ROOT + 2 C9_CLIENT_REPO + 3 live-checkout); LD5 flips to exit 0 and c8-close SC1-smoke verdict tightened (both pinned a bug/vacuity, not a rule) [ev: retro campaign11-client-root]
- **T3** lint-write-path DRIFT — a `[concept]` marker on a slot that became real is flagged [ev: retro campaign11-concept-row-drift]
- **T4** `lint-guard-pins.sh` — every lint header's OBSERVED mutation must name a bats fixture (9 lints + itself) [ev: retro campaign11-lint-guard-pins]
`VERSION`: `0.21.0` → `0.22.0` (same commit).

## 4. `tests/c11-close.bats` freeze values (for QA)
- `VERSION` == `0.22.0`; tag `v0.22.0`.
- **SC-13 client versions CARRY OVER** unchanged: Compresores 2.2.0 / Paccadia 2.1.0 / Dashboard 2.2.0 (C11 kit-only, NO client jar).
- **Tool-pins** (kit-links resolves each): `lint-guard-pins.sh` + the four C11 bats (`golden-parser.bats`, `client-root-single-source.bats`, `write-path-drift` / the concept-drift bats, the guard-pins bats) + the existing suite.
- **CLOSE-no-conflict-markers** (46d3eff): 0 tracked files with `<<<<<<<`/`=======`/`>>>>>>>`.
- sweep-fold-audit --strict → 0 uncited after the 5 INDEX flips.
- bats total: record the measured count.

## 5. BUILD-STATE + INDEX + commit
- `BUILD-STATE.md`: `retro_pending: true`→`false` (section-scoped); `last_commit`/`last_session` (`Campaign 11 CLOSE v0.22.0 — shared parser + DRIFT + client-root + guard-pins; 5 retros folded; kit-only`).
- `retros/INDEX.md`: 5 rows → folded.
```
chore(c11-close): v0.22.0 — CHANGELOG+VERSION, 5 retros folded, BUILD-STATE flip

Retro: promotion (folds campaign11-shared-method-boundary campaign11-concept-row-drift campaign11-client-root campaign11-lint-guard-pins campaign11-close-process-meta-lessons)
```
Gate: `C11_CLOSE=1 bats tests/c11-close.bats` · `sweep-build-state.sh` · `sweep-fold-audit.sh --strict …` · `bats tests/`.
Post-merge (lead): tag v0.22.0, install-skill, sdd-archive C11, settle ledger.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 3 retros pending; slug is campaign11-concept-row-drift; client-root 0 citations | [CERT] | INDEX + `git grep ev: retro` @ 53efff7 |
| 2 | VERSION 0.21.0, no [Unreleased] | [CERT] | git @ 53efff7 |
| 3 | 46d3eff CLOSE-no-conflict-markers, d837ae5 SC1 tighten, 7053907 c8-archive markers | [CERT] | git log |
| 4 | C11 kit-only, client versions unchanged | [CERT] | no client jar |
