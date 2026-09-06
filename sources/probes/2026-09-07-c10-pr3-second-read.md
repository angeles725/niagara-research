# C10 PR3 (S23 silent-protection Pattern B) second read — feat/c10-silent-protection-pattern-b 439388f

investigador1, 2026-09-07. Five checks + OBSERVED mutations + independent real-tree smoke. `[ev: git 439388f; reproduced runs]`

## Verdict: PASS on the fix — real-tree 1→0 verified, SP1-SP8 unchanged. ONE durability finding: the Pattern-B AND is load-bearing but UNPINNED.

## Check 1 — follow direction is pure→adapter via the B<Pure> pair only — PASS
Surface criterion (D3c): `in_list(this_class, ALARM_CLASSES) || in_list("B" this_class, ALARM_CLASSES)`. `this_class` is
the file's class; the follow prepends exactly one `B` (pure `CompressorControl` → adapter `BCompressorControl`), one
level, no recursion. Retro documents it (`§ what happened`: "adapter→pure follow: this_class ∈ ALARM_CLASSES OR
'B' this_class ∈ ALARM_CLASSES"; lesson "drop B<Pure> follow → :294 WARNs again"). The limitation (an adapter NOT named
`B`+Pure is not followed) is implied by "the B<Pure> naming pair only" — adequate, could be one explicit sentence. `[ev: lint-silent-protection.sh surface criterion @ 439388f; retro §]`

## Check 2 — Pattern-B AND is load-bearing — CONFIRMED, but UNPINNED in the RED (finding)
Pass 0b: `if (cn!="" && (ha || (bi && (bn||bc)))) print cn` — `bi`=implements+BIAlarmSource, `bn`=newOffnormalAlarm,
`bc`=new AlarmSupport(. The AND (`bi && (bn||bc)`) is real. OBSERVED M2 probe (adapter that `implements BIAlarmSource`
but has NO alarm method, beside a trip; correct = WARN):
| | WARN |
|---|---|
| PRISTINE (AND) | 1 (correct — interface alone is not a surface) |
| M2 (AND→OR) | 0 (wrongly cleared) |
So the AND is load-bearing. **BUT** the shipped **S23-neg has NO adapter at all** (only `CompressorControl.java`), so M2
does NOT flip it: PRISTINE(AND) WARN=1, M2(OR) WARN=1 — both WARN. The retro/task describe the M2 mutation as flipping an
"implements-only adapter" fixture, but no such fixture exists in the RED. **The AND is unpinned** — same class as the PR1
depth guard and the single-line S21-misparse. `[ev: Pass 0b @ 439388f; M2 probe run; shipped S23-neg run]`

### Proposed pin (add to the SP RED)
A trip beside an adapter that `implements BIAlarmSource` with NO `newOffnormalAlarm`/`new AlarmSupport(` → assert WARN;
M2 (AND→OR) flips it to CLEAN. That is the fixture the retro's M2 lesson describes but the RED lacks.

## Check 3 — depth-guard baseline (no C9 pin moved; CompPan-rt still 1 before Pattern B) — CONFIRMED
Full SP bats pristine: **SP1-SP8 all green** (the section-D `brace_depth >= 2` guard shifted no C9 pin). And OLD lint +
ONLY the guard (no Pattern B) on the real CompPan-rt → **WARN=1** (unchanged from OLD's 1): the guard alone does not
surface CP-1; Pattern B is what takes it to 0. `[ev: pristine bats; OLD+guard-only CompPan-rt run @ ff1b659]`

## Check 4 — one-WARN-per-trip dedupe + effect-slot exemption untouched — PASS
The diff touches only Pass 0b (new), the comment strip, section B (`file_has_alarm` → `this_class`+ALARM_CLASSES lookup),
the section-D depth guard, and surface criterion (1). The dedupe and the effect-slot exemption LOGIC are not in any diff
hunk (the only EFFECT_SLOTS lines in the diff are the unchanged `-v EFFECT_SLOTS=` passthrough). Behaviour intact:
SP3 (effect-slot exemption → still 1 WARN) and SP1 (one-WARN grammar) both pass. `[ev: diff 439388f; SP1/SP3 green]`

## Check 5 — 0 attribution trailers — PASS
Two commits (RED 0711f22, fix 439388f); co-authored-by/generated-with/claude/anthropic count = 0. `[ev: git log]`

## Independent real-tree SP-smoke @ ff1b659 — VERIFIED
| Module | OLD (f90b8d1) | NEW (439388f) |
|---|---|---|
| CompPan-rt | 1 WARN (CP-1 CompressorControl.java:294) | **0 WARN** (CP-1 surfaced by the B<Pure> Pattern-B follow) |
| ColdRoomPan-rt | — | 0 (CR-3 Pattern A) |
| DashboardPan-rt / -ux | — | 0 / 0 |
Matches the S23 contract exactly. `[ev: OLD vs NEW runs @ ff1b659]`

## Meta-observation (for the lead) — a recurring pattern across C10 PRs
Three OBSERVED mutations are DESCRIBED in tasks/retros but not backed by a fixture that actually flips: PR1 depth guard
(drop → all bats stay green), PR1 S21-misparse committed single-line (drop guard → stays CLEAN), PR3 Pattern-B AND
(M2 → shipped S23-neg does not flip). SC-7's intent is a fixture that goes RED-then-GREEN, not prose describing a
mutation. Suggest a close-lesson: every OBSERVED mutation named in a lead gate must name the fixture it flips, and QA
must confirm the flip — not just that the mutation is plausible.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | follow is `"B"+this_class`, one level; documented in retro | [CERT] | surface criterion + retro |
| 2 | AND load-bearing (M2 probe flips implements-only); shipped S23-neg does NOT flip under M2 (unpinned) | [CERT] | M2 + S23-neg runs |
| 3 | guard moved no C9 pin (SP1-SP8 green); OLD+guard-only CompPan-rt still 1 | [CERT] | bats + client run |
| 4 | dedupe + effect-slot untouched; SP1/SP3 green | [CERT] | diff + bats |
| 5 | 0 trailers | [CERT] | git log |
| 6 | real-tree CompPan-rt 1→0, ColdRoom/Dashboard 0 | [CERT-live] | OLD vs NEW @ ff1b659 |
Tally: 5 [CERT] · 1 [CERT-live] · 0 [INFER] · 0 unmarked.
