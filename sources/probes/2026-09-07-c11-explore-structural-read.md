# C11 explore.md structural read — kit openspec/changes/build-n4-module-campaign11/explore.md (dab0807)

investigator1, 2026-09-07. The lead's four checks + a real-tree confirm + one doc-comment landmine. Verified against
kit dab0807 (the three lint files are byte-identical dab0807..main, so all line refs hold). `[ev: git @ dab0807; client scan @ ff1b659]`

## Verdict: PASS — all four items check out, T3's inverse-semantics hand-correction is complete. One minor finding to fold before design.

## (1) T1 lines + five golden cases — VERIFIED
The three source anchors are exact at dab0807: `lint-timers.sh:188-202` (Phase 2 parser, method-open at :202, NET
`brace_depth`), `lint-silent-protection.sh:302-364` (section D, NET), `lint-ext-writable-shape.sh:132-176` (Pass 2,
canonical PEAK `max_d` at :147). The five golden cases (explore §3.1 / §4 T1) = BMisparse multi-line · anyNoHardware
same-method local · CP-1 adapter · one-liner · accessor — matches B832 §5 with the B832-G1 accessor promoted from gap to
5th golden fixture (C11-g1-setter pins it). `[ev: lint files @ dab0807; B832 §5]`

## (2) T2 = 10 sites — VERIFIED exact
All ten line refs confirmed at dab0807: 5× `C9_CLIENT_ROOT` — ext-writable-shape.bats:26, demand-in-scope:27,
lint-silent-protection:30, lint-timers:418, lint-write-path:338 (each `:-…/main-ff1b659`); 2× `C9_CLIENT_REPO` —
c9-close:108, c10-close:90 (same default, the two I flagged); 3 live-checkout — c8-close:107 (`C8_CLIENT_REPO`),
lint-delays:53 + rc-scan:75 (override-less). The live checkout is 4f5f1c7 (pre-C9). Matches the "10 not 5" learning
(§Key learnings 3). `[ev: tests/*.bats @ dab0807; 4f5f1c7 lead-verified]`

## (3) T3 inverse semantics — VERIFIED, every sentence correct (hand-correction complete)
Checked all five T3 mentions; each states the CORRECT inverse — a `[concept]`-marked row whose slot IS now in the covered
set → DRIFT; a true concept row (slot absent) stays silent:
- §3.3 T3 (:63): "…whose backtick-inner slot name IS now in the covered set … is a stale marker → advisory … A true
  concept row (name absent) stays silent." ✓
- §4 W1 T3 (:75): "inverse of STALE … 0 DRIFT (five concept rows have no source slot)." ✓
- §6.1 (:111): "a synthetic marked row with a present slot → 1 DRIFT; decoy in a comment → none." ✓
- §Key learnings (:145): "DRIFT is STALE's inverse …" ✓
No backwards sentence survives — the by-hand correction is complete. `[ev: explore §3.3/§4/§6.1/§7 @ dab0807]`

## (4) T4 wording — VERIFIED
Every T4 mention says a lint header's named OBSERVED mutation must map to an existing bats fixture — NOT "skip blocks need
a comment": §3.3 (:64) "every OBSERVED mutation named in a lint's header … maps to an existing bats fixture name"; §4 W1
(:77) "scans lint headers for named mutations and tests/*.bats for fixture names"; §6.1 (:113). Correct per K24 item 7.
`[ev: explore §3.3/§4/§6.1]`

## CONFIRM — "real trees do not flip today" is empirically true
The peak-depth cut newly catches one-liner methods; the explore (§2 T1 row, risk 1) asserts the real-tree baselines are
identical before/after. Scanned all 42 client `.java` files at ff1b659: **0 one-liner methods** containing
`Clock.schedule`/`newOffnormalAlarm`, and 0 one-liner methods with a trip-write (`.setValue(`/`Math.min(target|onCount`).
So peak-depth catches nothing new on the current client corpus — the claim holds, and the one-liner behavior change is
pinned only by the synthetic golden fixture (as designed). `[ev: grep 42 client .java @ ff1b659]`

## FINDING (minor, fold before design) — the silent-protection net-depth comment is a landmine
`lint-silent-protection.sh:303-307` DOCUMENTS the net-depth as INTENTIONAL: "a method is detected only when the line's
NET brace change is > 0 … This correctly skips single-line methods like `Type get() { return x; }` … preventing the
method-open event from being attached to a post-close depth that spans until the class closes." The T1 cut switches this
lint to PEAK depth (C11-sp-oneliner pins a one-liner `step()` trip that must newly WARN — currently missed at dab0807). So
after the cut this comment CONTRADICTS the code. Two asks for the T1 design: (a) REPLACE the :303-307 comment when
migrating silent-protection to the shared peak-depth fragment; (b) note its stated rationale is misleading — the
"attaching to a post-close depth that spans until the class closes" fear is already prevented by the `brace_depth>=2`
guard + the close-detection, not by skipping one-liners. The explore's risk list (risk 3 covers the `/* */` strip) does
not mention this comment or that silent-protection's one-liner behavior changes; add it so the design doesn't ship a
comment that argues against its own code. `[ev: lint-silent-protection.sh:303-307 @ dab0807; QA C11-sp-oneliner d88af78]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | T1 line refs exact; files identical dab0807..main; 5 golden cases correct | [CERT] | lint files @ dab0807; B832 §5 |
| 2 | T2 = 10 sites, all line numbers exact (5 ROOT + 2 REPO + 3 live-checkout) | [CERT] | tests/*.bats @ dab0807 |
| 3 | every T3 sentence has the correct inverse semantics (DRIFT = present-slot [concept] row) | [CERT] | explore §3.3/§4/§6.1/§7 |
| 4 | T4 = header mutation → bats fixture (not skip-block-comment) | [CERT] | explore §3.3/§4/§6.1 |
| 5 | 0 one-liner schedule/trip methods in 42 client .java @ ff1b659 → "real trees do not flip" holds | [CERT] | client grep |
| 6 | lint-silent-protection:303-307 documents net-depth as intentional → contradicts the peak-depth cut | [CERT] | lint-silent-protection.sh @ dab0807 |
Tally: 6 [CERT] · 0 [INFER] · 0 unmarked.
