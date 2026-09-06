# C10 PR2 (S22 ext-writable per-slot) second read — feat/c10-ext-writable-per-slot c3523dc

investigador1, 2026-09-07. Six checks + four guard mutations (attributed) + real-tree EW10. Bats run in a real
niagara-tools worktree at c3523dc. `[ev: git c3523dc; worktree bats; ff1b659 runs + mutations]`

## Verdict: PASS on the fix. Guards correctly attributed; ONE hermetic-pin gap (do-bodies-only scope pinned only by the skippable EW10).

## Check 1 — execute()/changed()/generated setters excluded; no class-level has_action fallback — PASS
Exemption in `_check_prop` is now `if (pname in exempt_slot) return` (do-body write) OR the name-pattern
(`set<X>`/`apply<X>`/`<x>Cmd`). The C9 `if (has_action) return` is GONE — `has_action` survives only in two comment
lines, not as logic. Pass 2 only runs `if (length(do_methods) > 0)` and only enters a body when `mname in do_methods`,
so `execute()`, `changed()`, and slotomatic setters are never scanned. `[ev: c3523dc grep has_action; _check_prop]`

## Check 2 — doX mapping = cap1 of the action name — PASS
`_harvest_action`: `do_methods["do" toupper(substr(aname,1,1)) substr(aname,2)]` → ackAlarm→doAckAlarm,
tick→doTick, bumpSetpoint→doBumpSetpoint. `[ev: _harvest_action @ c3523dc]`

## Check 3 — the depth guard here is UNPINNED and REDUNDANT (not load-bearing like PR1)
Dropping `&& max_d >= 2` flips NOTHING: EW-s22-pos 0, EW-s22-neg 1, real CompPan-rt 1 — all baseline. Even a constructed
misparse shape (multi-line `defaultValue="new BAlarmRecord()"` before a brace-only class open + a real do-body) stays
CLEAN with the guard dropped. Reason: the `mname in do_methods` gate independently rejects the misparsed class-body name
("BAlarmRecord" ∉ do_methods), so the class body is never scanned as a method — the guard is belt-and-suspenders here,
NOT load-bearing (unlike PR1, closer to PR4's inert Edit 1). The retro honestly claims **no** depth-guard mutation (only
mutation 1 = drop do_methods, mutation 2 = class-level has_action), so there is no mis-attribution. Fine to keep as
defensive; worth a one-line note that it is redundant given the do_methods filter. `[ev: mutation d run; misparse fixture; retro §mutations]`

## Check 4 — real-tree EW10 — VERIFIED
| Module | WARN | subject |
|---|---|---|
| CompPan-rt | **1** | faultReset (exactly one; no other slot) |
| DashboardPan-rt | 1 | setpoint (BRoomPanel — unchanged) |
| ColdRoomPan-rt | 0 | — |
| DashboardPan-ux | 0 | — |
The EW10 contract flip (CompPan-rt 0→1) is correct: faultReset (BStatusBoolean SUMMARY|OPERATOR) has no do<Action> that
writes it; the class-level exemption that hid it in C9 is gone. `[ev: ff1b659 runs]`

## Check 5 — 0 attribution trailers — PASS. (RED 5116519, fix c3523dc.)

## Check 6 — per-guard mutation attribution (OBSERVED)
| Mutation | Flips | |
|---|---|---|
| drop doX mapping (`do_methods[aname]` raw) | **EW-s22-pos 0→1** (doBumpSetpoint not scanned → setpoint WARNs) | pinned ✓ |
| exempt on any action (class-level fallback) | **EW-s22-neg 1→0** and EW-s22-neg2 1→0 (faultReset re-exempted) | pinned ✓ |
| scan non-do bodies (drop `mname in do_methods`) | EW-s22-neg does NOT flip (still 1); **real CompPan-rt 1→0** (setFaultReset in execute/changed exempts) | pinned ONLY by skippable EW10 |
| drop depth guard (`max_d >= 2`) | nothing flips (see Check 3) | unpinned/redundant |

## Finding (minor, hermetic pin gap) — "do-bodies-only scope" is pinned only by the client-dependent EW10
Mutation "scan non-do bodies" flips the REAL CompPan-rt (1→0, because `setFaultReset(` lives in execute()/changed()) but
NOT the hermetic EW-s22-neg (its doTick/doAckAlarm have no faultReset write and there is no execute() writing it). So the
"scope to do<Action> bodies only" guarantee — the heart of S22 — is pinned hermetically by nothing; only EW10 catches it,
and EW10 SKIPs without `C9_CLIENT_ROOT`. Recommend one hermetic fixture: an OPERATOR complex slot whose ONLY writer is a
non-do body (an `execute()`/`changed()`/generated `setX(`) → must still WARN (scanning non-do bodies would wrongly exempt
it). Same class as the PR5 count gap; not merge-blocking (behaviour is correct, EW10 covers it when present).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | has_action logic removed; only exempt_slot + name-pattern exempt; do-bodies-only | [CERT] | grep + _check_prop |
| 2 | doX = do+cap1(name) | [CERT] | _harvest_action |
| 3 | depth guard flips nothing (incl. misparse shape); redundant w/ do_methods filter; retro claims no guard mutation | [CERT] | mutation d + misparse + retro |
| 4 | real EW10: CompPan-rt 1 faultReset, DashboardPan-rt 1 setpoint, others 0 | [CERT-live] | ff1b659 runs |
| 5 | mutation attribution: doX→pos, class-level→neg/neg2, non-do→real-only, guard→none | [CERT] | 4 mutation runs |
| 6 | do-bodies-only scope pinned only by skippable EW10 | [CERT] | mutation c: neg no-flip, real flip |
Tally: 5 [CERT] · 1 [CERT-live] · 0 [INFER] · 0 unmarked.
