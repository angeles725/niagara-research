# C11 close — apply package (kit `chore/c11-close`, target v0.22.0) — mirrors the C10 close

Author: companero (Fable), 2026-09-06. EXECUTE-ONLY. Cut from kit design main **`8da696c`** (VERSION `0.21.0` → `0.22.0`; the
four C11 lint PRs open `## [Unreleased]` as they merge). Fold targets at 8da696c line numbers. No AI trailers (CONTRIBUTING §6).
`[ev: kit 8da696c]`

## 1. Retro drafts — one per kit PR (new-retro.sh names) + fold target
`new-retro.sh kit <slug>` per PR (stub + INDEX row + BUILD-STATE retro_pending true).
| Slug | PR / slice | Lesson | FOLD TARGET |
|---|---|---|---|
| `campaign11-shared-method-boundary` | T1 | three copies of the section-D parser were three INVOCATION MECHANISMS, not styles; the shared library is FUNCTION-ONLY awk (`MB_AWK`) located via `BASH_SOURCE`; PEAK depth is the standard (net drops one-liner methods — B832) | BUILD-LOOP.md §5 (lint-timers/ext-writable/silent-protection now source `lib/method-boundary.sh`); METHODOLOGY new K25 "a duplicated parser is a divergence surface — one function-only fragment, one golden cross-lint contract" |
| `campaign11-concept-drift` | T3 | DRIFT is the `[concept]` branch INVERTED (a marker whose slot became real), not a new pass | BUILD-LOOP.md §5 lint-write-path bullet (+ DRIFT advisory) |
| `campaign11-client-root` | T2 | 10 absolute client-tree literals under 2+ var names; ONE `tests/lib/client-root.bash` default; a real-tree FAIL-smoke (LD5) pinned the defrost bug, not the rule | METHODOLOGY new K26 "a real-tree smoke that asserts a FAIL pins a BUG not a RULE and rots when the bug is fixed; synthetic fixtures pin rules" |
| `campaign11-lint-guard-pins` | T4 | every lint header's named OBSERVED mutation must map to a bats fixture (operationalises K24(7)) | BUILD-LOOP.md §5 (`lint-guard-pins.sh` in the pre-gate); METHODOLOGY K24(7) cross-ref |
### close-process meta-lessons (`campaign11-close-process-meta-lessons`) — seed
1. **LD5 bug-vs-rule** — a real-tree FAIL-smoke pins the current state (a bug), not a rule; synthetic fixtures pin rules (VERIFIED: lint-delays FAIL on 4f5f1c7, CLEAN on ff1b659).
2. **Zero `# Mutation:` lines in the kit** — the OBSERVED mutations live in prose headers, unpinned to fixtures until T4; a machine-checkable convention was missing.
3. **Three awk invocation mechanisms dictate a function-only fragment** — you cannot share a `BEGIN`/`END`/pattern program across `_cf=$(awk '…')` / `awk -v FILE` / … ; only function definitions splice into all three.
4. **`$KIT` is a writable-target seam; `BASH_SOURCE` binds to the running script** — the fragment is located via `${BASH_SOURCE[0]%/*}`, not `$KIT` (avoids the real-red class C8 lesson 1).
5. **The spec agent inferred a golden table** — the golden-set contract was reconstructed from the three copies, not authored; cherry-pick the QA RED (ed2088f), never re-author (K13).

## 2. CHANGELOG + VERSION
Rename `## [Unreleased]` → `## [v0.22.0] - 2026-09-<dd>`; `### Changed — Campaign 11: lint precision (T1-T4)`:
- shared method-boundary parser `lib/method-boundary.sh` (peak-depth; fixes the one-liner false-negative — B832) [ev: retro campaign11-shared-method-boundary]
- lint-write-path DRIFT (stale `[concept]` marker) [ev: retro campaign11-concept-drift]
- `tests/lib/client-root.bash` single-sources the client read tree (10→0 hardcodes) [ev: retro campaign11-client-root]
- `lint-guard-pins.sh` meta-check (header mutations ↔ fixtures) [ev: retro campaign11-lint-guard-pins]
`VERSION`: `0.21.0` → `0.22.0` (same commit).

## 3. `tests/c11-close.bats` freeze values (for QA)
- `VERSION` == `0.22.0`; tag `v0.22.0`.
- Client versions CARRY OVER unchanged: Compresores 2.2.0 / Paccadia 2.1.0 / Dashboard 2.2.0 (C11 is kit-only, NO client jar).
- sweep-fold-audit --strict → 0 uncited after the 5 INDEX flips (4 lint retros + meta-lessons).
- P1 size: the `git diff --stat` on `toolbelt` ≤ 700 (authored ≈353; goldens excluded).
- bats total: record the measured count (C11 adds golden-parser + the four slice REDs + c11-close).

## 4. BUILD-STATE + INDEX
- `BUILD-STATE.md` kit envelope: `retro_pending: true`→`false` (section-scoped); `last_commit:` merge sha; `last_session:`
  `2026-09-<dd> · Campaign 11 CLOSE v0.22.0 — shared method-boundary parser + DRIFT + client-root + guard-pins; 5 retros folded; kit-only, client versions unchanged.`
- `retros/INDEX.md`: 5 rows → `folded`.

## 5. Gates + commit
```bash
export PATH=/usr/bin:/bin:$PATH; cd <kit>
C11_CLOSE=1 bats tests/c11-close.bats
toolbelt/sweep-build-state.sh
toolbelt/sweep-fold-audit.sh --strict build-n4-module-kit/retros/INDEX.md build-n4-module-kit
bats tests/
```
```
chore(c11-close): v0.22.0 — CHANGELOG+VERSION, 5 retros folded, BUILD-STATE flip

Retro: promotion (folds campaign11-shared-method-boundary campaign11-concept-drift campaign11-client-root campaign11-lint-guard-pins campaign11-close-process-meta-lessons)
```

## 6. Post-merge (lead)
`git tag v0.22.0 <merge-sha> && git push origin v0.22.0`; `scripts/install-skill.sh`; `sdd-archive` C11; settle the ledger.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | kit design 8da696c, VERSION 0.21.0→0.22.0 | [CERT] | git @ 8da696c |
| 2 | C11 is kit-only, client versions unchanged | [CERT] | no client jar in C11 |
| 3 | LD5 bug-vs-rule reproduced | [CERT] | lint-delays FAIL @ 4f5f1c7 / clean @ ff1b659 |
| 4 | function-only fragment via BASH_SOURCE; goldens excluded from 700 | [CERT] | design D1b/D1c/D1k |
