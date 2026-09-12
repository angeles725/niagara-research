<!-- review-status: pending -->
<!-- Marker lifecycle: maintainer flips 'pending' → 'applied <date> · kit <sha>' (or 'dismissed') once folded; sweep-retros.sh reads this (METHODOLOGY §18). -->
# Retro — niagara-research · build-n4-module · 2026-09-11 · Module worktree location incident

> **Research retro.** Captured the incident where build sessions ran in an unauthorized
> worktree (`/home/cristian/niagara-panccadia-leon`) instead of the canonical
> `Cliente/` tree. The lesson was recorded as engram memory (`work-trees-under-cliente`)
> but was invisible to the fold pipeline — no retro file, no proposed kit delta, no guard.
> PROPOSE-ONLY: this file does not edit `$KIT`. Every proposed delta below carries its
> `[ev: retro module-worktree-location-retro]` citation token.

## Incident summary

**Date:** 2026-09-06 / 2026-09-07
**What happened:** Build work for the Leon-Guanjuato modules ran from the worktree
`/home/cristian/niagara-panccadia-leon` instead of the required
`/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato/`. This violated the
canonical module-build root rule (all `build-n4-module` sessions must run under
`/home/cristian/modulos_niagara_n4/Cliente/`).

**Why it went undetected:** The rule existed only as an engram memory key
(`work-trees-under-cliente`), which is invisible to the fold pipeline, to
`sweep-retros.sh`, and to any new session's orient step. No `orient-guard.sh`, no
preflight check, no BUILD-LOOP gate enforced the constraint at session start. The
incident was captured after the fact during a manual audit.

**Impact:** A design drift analysis (Campaign 9) was carried out against a stale
checkout. The stale tree predated real changes; results from that session had to be
re-anchored against the canonical checkout. No compiled artifact was lost, but
engineering time was wasted and a re-anchor cycle was required.

## Proposed kit deltas (fold list)

| # | Proposed delta (one line) | Target (file · §) | Evidence (block · key cite) | Citation token | Priority |
|---|---|---|---|---|---|
| D1 | Add `toolbelt/orient-guard.sh <module-root>`: PASS (exit 0) if under sole legal prefix `/home/cristian/modulos_niagara_n4/Cliente/`; FAIL (exit 1) otherwise; WARN (exit 0) with `BUILD_N4_CLIENTE_OVERRIDE=1`; usage exit 3; output row `PASS\|FAIL\|WARN  orient-guard  <detail>`; canonicalize via `realpath` + `cd -P` fallback; append trailing `/` before compare to prevent `ClienteX/` false-positive; test seam `BUILD_N4_LEGAL_ROOT_PREFIX` (always WARN-loud when set); VCS-free | `toolbelt/orient-guard.sh` (new) | work-trees-under-cliente incident 2026-09-06/07; stale-checkout lesson K12 (C9 close) | `[ev: retro module-worktree-location-retro]` | HIGH |
| D2 | Route orient-guard into BUILD-LOOP.md §0.a as the FIRST orient bullet, before reading BUILD-STATE; call `orient-guard.sh <module-root>` and abort on exit 1 | `BUILD-LOOP.md` §0.a | D1 above | `[ev: retro module-worktree-location-retro]` | HIGH |
| D3 | Route orient-guard into `skill/SKILL.md` step 1 (orient), prepended to the BUILD-STATE read; cite `[ev: retro module-worktree-location-retro]`; K19 routing obligation | `skill/SKILL.md` step 1 | D1 above; K19 routing doctrine | `[ev: retro module-worktree-location-retro]` | HIGH |
| D4 | Add `tests/orient-guard.bats` covering PASS / FAIL-no-override / WARN-override-1 / FAIL-non-1-override / symlink / relative-path / trailing-slash / ClienteX-sibling / usage-error; all via `BUILD_N4_LEGAL_ROOT_PREFIX` tmpdir seam | `tests/orient-guard.bats` (new) | D1 above; bats-testable guard doctrine | `[ev: retro module-worktree-location-retro]` | HIGH |
| D5 | Add L9 pin in `tests/kit-links.bats`: assert `orient-guard.sh` is named in BOTH `BUILD-LOOP.md` §0.a AND `skill/SKILL.md` step 1; a missing routing entry turns L9 RED | `tests/kit-links.bats` L9 | D2+D3 above; K19 routing doctrine | `[ev: retro module-worktree-location-retro]` | HIGH |

## Lessons

1. **Engram memory alone is not a fold artifact.** An incident lesson captured only in
   engram (`work-trees-under-cliente`) is invisible to sweep-retros.sh, to the fold
   pipeline, and to future sessions' orient step. A formal retro file in
   `niagara-research/retros/` is the required entry point for promotion.

2. **BUILD_N4_CLIENTE_OVERRIDE=1 is the sole sanctioned escape hatch.** Any work
   outside `/home/cristian/modulos_niagara_n4/Cliente/` that does not explicitly set
   `BUILD_N4_CLIENTE_OVERRIDE=1` must be treated as an unauthorized location and fail
   closed. Setting any other value (e.g. `0`, `yes`) is treated as unset — fail-closed
   semantics prevent accidental bypass via truthy-looking values.

3. **Stale-checkout drift is a correctness risk.** Running a design analysis (Campaign 9)
   against a worktree that was behind the canonical branch introduced design-anchor
   errors that required a re-anchor cycle. Always verify the worktree is on the canonical
   branch before starting a session.

4. **Canonicalization before prefix compare prevents bypass.** A symlink, relative path,
   or trailing-slash variant could otherwise evade a naive string-prefix check. The guard
   must resolve the canonical absolute path (via `realpath` or `cd -P`) and append a
   trailing `/` before comparing to the legal prefix — so `ClienteX/` does not match
   `Cliente/`.

5. **K19 routing requires dual-doc enforcement.** A guard that exists in `toolbelt/`
   but is not cited in BOTH `BUILD-LOOP.md` §0.a and `skill/SKILL.md` step 1 will be
   silently skipped by future sessions. The bats L9 pin makes the routing obligation
   machine-verifiable.

## Still requires-execution (do NOT fold as closed)

- Live smoke: run `orient-guard.sh` on the canonical `Cliente/ColdRoomPan` path →
  confirm PASS; run on a `/tmp/test` path → confirm FAIL with override message.
- Verify `bats tests/orient-guard.bats` GREEN after kit implementation lands.
- Confirm `bats tests/kit-links.bats` L9 GREEN after routing is wired.
