# C11 explore-draft second read — niagara-research e6666eeb9 (draft) + B832 self-check

investigador1, 2026-09-07. Second read of companero's C11 explore draft (`sources/probes/2026-09-07-c11-explore-draft.md`
@ e6666eeb9) and my own B832. Kit main is now **dab0807** (v0.21.0, C10 closed) — the draft was authored at 2f3300f, so
some facts drifted at the close. `[ev: git dab0807; grep runs]`

## Verdict: sound seeds, recommendation good (T1→T3→T2→T4 kit-first wave). Two refinements — T2's count is stale post-close (and the close is evidence FOR T2), and P2's framing should credit R14.

## T1 — ACCURATE (B832 self-check: solid)
The draft's T1 uses B832 correctly: net-depth (lint-timers.sh:202 / lint-silent-protection.sh:326) drops one-liner methods
(silent FN), peak-depth ext-writable.sh:139-147 catches; fragment adopts `max_d`; invariants 1-5 and the golden set (incl.
the one-liner + B832-G1/G2) match B832 verbatim. companero reproduced the FN independently before folding. B832 itself: 5
[CERT] (all empirically run) + 1 [INFER] (ext-writable depth-guard redundancy, from the PR2 read) — no overclaim on
re-read. T1 is correctly the keystone. `[ev: B832 §2/§4/§5; C11 draft T1]`

## T2 — premise valid, SPECIFICS STALE post-C10-close (refine); the close is evidence FOR T2
Draft says "6 bats hardcode the default (`main-a109249`/`c10-ff1b659`)". At **dab0807** the real state is **7 bats, ALL
defaulting to `main-ff1b659`** (c9-close, c10-close, demand-in-scope, ext-writable-shape, lint-silent-protection,
lint-timers, lint-write-path) — 0 still on a109249 or c10-ff1b659. What changed: the C10 close (PR7) HAND-retargeted 3 of
them a109249→ff1b659 (`b6b65a2`), and c10-close.bats was added. **That hand-sed of 3 files across the close is exactly the
churn T2 wants to delete** — so the close strengthens T2's case, and the seed should update the count to 7/ff1b659 and
cite the PR7 retarget as the motivating incident. `[ev: git grep C9_CLIENT_(ROOT|REPO) @ dab0807 — 7 files, all ff1b659; PR7 b6b65a2]`

## T3 — accurate; line ref slightly narrow
Draft cites the S25 STALE pass at `lint-write-path.sh:424-438`; at dab0807 the "Per-row STALE pass" spans :422-458 (comment
:422, emit :451). Points to the right region — fine for an explore seed. T3 (inverse STALE: a `[concept]` row whose slot
LATER appears in source → WARN) is a clean inverse on the same covered set. `[ev: lint-write-path.sh:422-458 @ dab0807]`

## P2 — sharpen the "attribution-vs-RBAC" framing: attribution is ALREADY shipped (R14)
The draft frames P2 as an open "attribution-vs-RBAC" question. My C10 HMI probe
(`2026-09-06-c10-hmi-per-operator-login-options.md`) concluded R14 (shipped) ALREADY solves attribution — the change_log
already names the acting operator. So per-operator login is NOT needed for attribution; the genuinely open decision is
whether to add per-operator **RBAC / VIEW** on top. Recommend rewording P2 to "attribution is shipped (R14/change_log);
the open product decision is per-operator RBAC/VIEW — does login GATE writes per operator, or only scope the view?" — same
decision, but it doesn't re-litigate a solved problem. `[ev: probe 2026-09-06-c10-hmi-per-operator-login-options.md; C9 R14]`

## Everything else — sound
- Dependencies (§2): T1-first, T3-after-S25, T2/T4 independent, P1→P2 — all correct. P1 supersedes the C9 shared-password
  step-up ✓.
- §0 prerequisites (deploy chain, harness, tunnel) unchanged from C10 and correctly held as Cristian-gates; the KIT wave
  (T1-T4) needs none ✓. Client carry-over versions (Compresores 2.2.0 / Paccadia 2.1.0 / Dashboard 2.2.0) match the C10
  close BUILD-STATE.
- Recommendation (T1→T3→T2→T4, WSL-only, nothing from Cristian) is the right opening — and T4 (unpinned-guard meta-check)
  operationalises the C10 close-lesson (K24 item 7), which is a strong self-reinforcing seed.

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | T1/B832 accurate on re-read; no overclaim; companero reproduced | [CERT] | B832 + draft T1 |
| 2 | T2 is 7 bats all ff1b659 at dab0807 (not 6/a109249+c10-ff1b659); PR7 hand-retargeted 3 | [CERT] | git grep @ dab0807; b6b65a2 |
| 3 | T3 STALE pass at :422-458 (draft :424-438 slightly narrow) | [CERT] | lint-write-path.sh @ dab0807 |
| 4 | attribution already shipped via R14 → P2 is an RBAC question, not attribution-vs-RBAC | [CERT] | c10-hmi-per-operator-login probe |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
