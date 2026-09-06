# C11 close PR5 second read — kit chore/c11-close 7f4e242 (v0.22.0)

investigador1, 2026-09-07. Fold content + gate checks. `[ev: git 7f4e242]`

## Verdict: PASS on the fold content. ONE minor finding (retro footers still say PENDING). The gate's 11/13 (CLOSE-tag + CLOSE-harness-run) and the c11-close.bats freeze hunk are known-pending, not fold issues.

## Fold content — all PASS
- **VERSION 0.22.0**; **0 attribution trailers**; **0 conflict markers** (the C8 archive is now clean). `[ev: VERSION; git grep]`
- **Promotion trailer**: 5 retros (shared-method-boundary, concept-row-drift, client-root, lint-guard-pins,
  close-process-meta-lessons). `[ev: close commit body]`
- **METHODOLOGY** — the K-lessons land where companero (cf0f52d97) specified, each with one `[ev: retro campaign11-…]`:
  **K25** shared parser (`[ev: campaign11-shared-method-boundary]`; carries the three awk mechanisms + `BASH_SOURCE`-not-`$KIT`
  seam); **K26** client-root single source (`[ev: campaign11-client-root]`); **K24 (7)** guard-pin grammar
  (`[ev: campaign11-lint-guard-pins]`); and the **K20 lint-timers exit-2 nuance** amended onto K20 with its own
  `[ev: campaign11-lint-guard-pins]` alongside the pre-existing schema-risk ev. `[ev: METHODOLOGY diff 7f4e242]`
- **CHANGELOG**: four T units, each names its tool with the words the c11-close gate greps — T1 `lib/method-boundary.sh`,
  T2 `client-root.bash`, T3 `lint-write-path` DRIFT, T4 `lint-guard-pins.sh` — each with a `[ev: retro campaign11-…]`. `[ev: CHANGELOG diff]`
- **INDEX**: all 5 campaign11 retros marked `folded`; fold-audit 93/93 (lead-reported). `[ev: INDEX @ 7f4e242]`
- **No doctrine duplicated from PR1-PR4**: the close does NOT re-touch `types/logic.md` / `build-verify.md` /
  `logic-authoring.md` — it edits only METHODOLOGY + CHANGELOG + VERSION + retros + BUILD-STATE + c11-close.bats. `[ev: diff --name-only]`
- **close-process retro carries all 12 agreed items** (enumerated in the intro + the 6-row fold table): LD5 bug-vs-rule,
  zero `# Mutation:` lines, three awk mechanisms, `$KIT` seam vs `BASH_SOURCE`, the spec agent's inferred golden table,
  I2/I3 defensive-with-reachable-but-absent-shapes (B832-G3/G4), fragment-merge slip + 0-markers-before-add rule,
  C8-archive markers + CLOSE-no-conflict-markers pin, SC1-smoke vacuity, amend-range retro anchor gap, two K12 breaches.
  It also refines my B832-G3/G4 framing well: "reclassify from 'inert' to 'defensive' only when no shape in the TARGET
  tree exercises the guard — not when no shape anywhere does." `[ev: close-process retro:9 @ 7f4e242]`

## Finding (minor, cosmetic — not fold-audit-blocking) — the 5 retro FOOTERS still say "PENDING"
Each C11 retro has `<!-- review-status: folded -->` (header, machine-readable) and its INDEX row folded, but the
**footer still reads `**Status**: PENDING — INDEX row appended: … pending`** — unchanged from when it was filed. In C10
(PR7) the fold updated the footer too, e.g. `**Status**: FOLDED — types/logic-authoring.md :105 … updated. [ev: retro …]`.
The fold-audit gates on the header + INDEX (both correct → 93/93 passes), so this does not block; but the header/footer
disagree within each file. Update the 5 footers to `FOLDED — <target>` for consistency with C10. `[ev: retro footers @ 7f4e242; C10 PR7 precedent]`

## Notes (known-pending, not findings)
- Gate 11/13: **CLOSE-tag** fills at merge (`git tag v0.22.0`) and **CLOSE-harness-run** is the Windows session owed by
  Cristian — both expected-pending, as in C9/C10.
- **c11-close.bats freeze hunk**: the lead flagged that the close commit left an inline default the client-root pin
  flags; QA re-issues that hunk pins-only and rebuilds. At 7f4e242 I find **no raw `Leon-Guanjuato` literal** in
  c11-close.bats (it `load`s `lib/client-root`), so re the client-root pin it already reads clean here — re-confirm on
  the rebuilt QA tip. Minor: the close-process retro intro says "six process-level lessons" but enumerates ~12 — a stale
  count word.

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | VERSION 0.22.0; 0 trailers; 0 markers; 5-retro promotion | [CERT] | git @ 7f4e242 |
| 2 | K24(7)/K25/K26 + K20-exit-2 land correctly, one campaign11 [ev:] each | [CERT] | METHODOLOGY diff |
| 3 | CHANGELOG four T units w/ gate tool words; no doctrine dup | [CERT] | CHANGELOG + name-only |
| 4 | close-process retro carries all 12 items; refines B832-G3/G4 | [CERT] | retro:9 |
| 5 | 5 retro footers still say PENDING (header/INDEX folded) — cosmetic | [CERT] | retro footers |
Tally: 5 [CERT] · 0 [INFER] · 0 unmarked.
