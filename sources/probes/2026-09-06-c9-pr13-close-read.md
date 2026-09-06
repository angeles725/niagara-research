# C9 PR13 close-package FINAL second read — 0e0aa3429 vs kit main 4200e27

investigador1, 2026-09-06. Read-only; verified against live kit `origin/main` @ 4200e27. `[ev: git + sweep-fold-audit re-run]`

## Verdict
Execute-ready. Points 1/3/4 pass; step-0 SP-smoke re-pin is present and correct. ONE substantive recommendation on point 2
(the explicit question): the fold-audit PASSES (81/81/0, I re-ran it), but `campaign9-doctrine-fold` and the new
`campaign9-close-process-meta-lessons` rows have NO campaign9-qualified token in the core — they are credited only by an
AMBIGUOUS bare-segment match, and the audit itself emits "ambiguous citation token" NOTEs. Add qualified tokens for
cleanliness (not blocking).

## (1) INDEX flips + meta-lessons → pending 0 — PASS
Live INDEX @ 4200e27 has EXACTLY four pending rows (`:88-91`: campaign9-demand-scope/silent-protection/ext-writable-shape/
doctrine-fold). Flipping those four + adding the new `campaign9-close-process-meta-lessons` folded row → pending = 0. ✓ The
meta-lessons content (my 19 lessons + the 6 W2/W3: TTL-never-compared, comment-satisfiable pins ×4, HashMap-on-shared-field
concurrency, pre-push-needs-BUILD-STATE-in-range, workers-never-edit-REDs, workers-never-write-outside-worktree) is right —
those are the real W2/W3 findings (incl. my PR4 PF2 and PR6b HashMap). `[ev: INDEX.md:88-91 @ 4200e27]`

## (2) Fold-audit — PASSES, but campaign9 rows lack a qualified token (ANSWER: yes, add one)
I re-ran `sweep-fold-audit.sh --strict` on a scratch INDEX with the four rows flipped: **81 folded, 81 cited, 0 uncited,
exit 0** — the package's claim holds. BUT:
- `git grep 'campaign9-doctrine-fold'` in the CORE (excl `retros/`) = **0**. The only `doctrine-fold` tokens in core are
  `BUILD-LOOP.md:108` (`[ev: retro doctrine-fold]`, the envelope-pairing rule — the package's :108 cite is CORRECT) and
  `METHODOLOGY.md:92` (the campaign-8 doctrine promotion). So `campaign9-doctrine-fold` is credited ONLY by the bare
  `doctrine-fold` segment, which actually refers to the C6/C8 doctrine-fold — a coincidental match.
- The audit prints `NOTE ambiguous citation token credits …: campaign8-doctrine-fold doctrine-fold` (and the
  close-process-meta-lessons family) — the ambiguity is real and audited.
- The NEW `campaign9-close-process-meta-lessons` row will have the SAME issue (bare `close-process-meta-lessons` segment,
  already ambiguous across campaign4/6/8).
**Recommendation (yes):** add a campaign9-qualified token for each — a `[ev: retro campaign9-doctrine-fold]` and a
`[ev: retro campaign9-close-process-meta-lessons]` promotion line in `METHODOLOGY.md §Kit maintenance`, parallel to the
existing campaign-8 promotion lines (`:92`/`:121`). The C8 close DID add such a METHODOLOGY promotion line for its
meta-lessons; C9 currently does not (the package §2 adds only the INDEX row, no core promotion line — grep = 0). This clears
the ambiguity NOTEs and satisfies point 1's "a fold/promotion line" for the meta-lessons row explicitly. Not blocking — the
audit is exit 0 either way. `[ev: git grep @ 4200e27; sweep-fold-audit re-run]`

## (3) CHANGELOG plan — PASS
Rename `[Unreleased]` → `[v0.20.0]`, keep the Wave-1 block verbatim, rename the Added heading to include PR12, append the
PR12 doctrine bullet, add a `### Client / tunnel (referenced)` block. The package correctly flags "**Confirm the PR#→work
mapping with `gh pr list` per repo before pasting**" — honest (the PR numbers are asserted, not verified in-package). VERSION
0.19.0 → 0.20.0 in the SAME commit (CONTRIBUTING §5). ✓

## (4) VERSION / BUILD-STATE / bats / trailer / step-0 — PASS
- VERSION bump present. ✓
- BUILD-STATE `retro_pending: true → false`, section-scoped (`sweep-build-state.sh` diffs the kit self-section). ✓
- `tests/c9-close.bats` cherry-picked from `qa/c9-close-checklist` **0895507** — CONFIRMED still the tip
  (`git ls-remote` = 08955077…). ✓ Never authored on chore/c9-close.
- Trailer bare-id: `Retro: promotion (folds campaign9-demand-scope campaign9-silent-protection campaign9-ext-writable-shape
  campaign9-doctrine-fold campaign9-close-process-meta-lessons)` — 5 slugs, canonical. ✓
- **Step 0 = SP-smoke re-pin PR FIRST** (pins-only, before PR13): re-pin silent-protection smoke to CompPan-rt **1**
  (`silent-protection.bats:294`, CP-1 still flagged — the S23 Pattern-B gap, a C10 deferral) / ColdRoomPan-rt **0** (PR8
  surfaced CR-3). Correct and honest. ✓

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 4 pending rows → 0 with flips + meta-lessons | [CERT] | INDEX :88-91 @ 4200e27 |
| 2 | fold-audit 81/81/0 exit 0; campaign9-doctrine-fold has 0 core tokens; ambiguous NOTE | [CERT] | sweep-fold-audit re-run + git grep |
| 3 | c9-close.bats tip 0895507 current; trailer 5 slugs bare-id; step-0 SP re-pin correct | [CERT] | ls-remote; §0/§6 |
Tally: 3 [CERT] · 0 [INFER] · 0 unmarked.
