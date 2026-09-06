# C9 apply-time structural read protocol — what the second reader checks on each GREEN diff

Author: investigador1, 2026-09-06. Purpose: make every apply-time read **execute-only**: when a PR tip lands, the reader
runs this checklist against the GREEN diff (tip vs its parent) and reports drift as file:line pairs. Invariants are the
design D-ids at niagara-tools `d2857d1` and the RED contracts at their current tips; anchors at client **a109249**
(read from the worktree `Leon-Guanjuato-worktrees/main-a109249`, never the stale checkout). Every check names the
grep/read that decides it. `[ev: design d2857d1]` `[ev: REDs cf28572 / d0f5942 / e38e503 / 269be48 / 4c18837 / cc1c948 / 70a357b / 8b43488 / e7e6615 / 0a14df8]`

**Universal (every PR):** (U1) parent of the tip == the blessed base (client a109249 / kit main / tunnel 9acb47c after
the RK3 rebase); (U2) the RED's test files are byte-identical to the RED tip (`git diff <red-tip> <pr-tip> -- <test paths>`
is empty) — GREEN must not edit the pins; (U3) no AI-attribution trailer (`git log --format=%B | grep -iE 'co-authored|generated with|claude'` = 0);
(U4) the named OBSERVED mutation flip is recorded in the PR body (count + subject, campaign-7 meta-lesson 1); (U5) for
client PRs `schema-risk.sh` verdict is in the PR body and reads SAFE; (U6) version bump matches the spec (SC13-style pin).

---

## PR1 — S20 rotation (client CompPan-rt; RED `qa/c9-comppan-rotation` cf28572, 17 pins) — D1
| # | Invariant | How to decide |
|---|---|---|
| 1.1 | Step 2b (completion) sits between a109249 `:217 if (target > N) target = N;` and `:219 // 3) Move ONE stage`; step 3b (arm) between the `:246` brace and `:248 // 4) Manual HOA override`; both before `:255 cmdPreHoa` | read the diff hunks' context lines, not their numbers |
| 1.2 | Completion drops **`rotOut` explicitly** (`cmd[rotOut] = false`) and sets `lastStageMs = now` — never delegates the drop to `pickMostHoursOn` | grep `cmd[rotOut]` in 2b; `pickMostHoursOn` call count unchanged (=1, at the stage-down) |
| 1.3 | `rotSinceMs[]` is the rotation clock: stamped in the arm action AND at the `:229` stage-up write; re-seeded in `seedRestart` (`:346-349`) and on the `rotationIntervalMs` 0→non-zero edge; zeroed in `resetTransient` (`:328-335`); gate 8 = `(now - rotSinceMs[out]) >= rotationIntervalMs && (now - cmdSince[out]) >= minOnMs` | grep `rotSinceMs` ≥ 5 sites; gate 8 text; no `cmdSince[out] >= ... rotationIntervalMs` |
| 1.4 | `pickLeastHoursOffAuto` exists with skip `cmd[k] \|\| modes[k] != MODE_AUTO` and is called ONLY from 3b; `pickLeastHoursOff` (`:352-363`) is byte-unchanged | `git diff` shows no hunk inside `:352-363`; grep callers |
| 1.5 | Contract symbols exactly: `Cfg.rotationIntervalMs` (long), `Cfg.rotationMode` (int), `ROTATION_MAKE_BEFORE_BREAK`/`ROTATION_BREAK_BEFORE_MAKE` (static final int), package-visible `swaps` incremented in completion only; `resetTransient` clears `rotOut`, `rotArmedMs`, `rotSinceMs[]`, `swaps` | javac of the RED is the proof; read `resetTransient` |
| 1.6 | E1-E4 rules present: demand rises → cancel (no `swaps++`); demand falls → drop first; `dischargeHigh`/LP mid-window → drop, never re-arm; `rotOut` HAND/OFF → skip + clear | read 2b/3b bodies against ROT12-ROT15 |
| 1.7 | Golden: `rotation-golden`/trace helpers unchanged from the RED; oracle NOT regenerated; trace line `now\|cmd[]\|stagesOn\|lastStageMs` (`rotSinceMs`/`swaps` not in the line) | U2 covers the file; grep the oracle path in the diff = absent |
| 1.8 | Adapter: two `@NiagaraProperty` (`rotationInterval` BRelTime MIN 0 / MAX 24 h, `SUMMARY\|OPERATOR`; `rotationMode` `BRotationMode` new frozen enum, ordinals 0/1); `cfg.rotationIntervalMs = getRotationInterval().getMillis()` + `cfg.rotationMode = getRotationMode().getOrdinal()` beside `:1907-1909`; `ctl.step(` at `:1971` unchanged; existing HOA doubles `:392-409` untouched (B828 §828.7) | grep; `git diff` no hunk in `:392-409` |
| 1.9 | `module-include.xml` registers `BRotationMode`; lexicon keys for the `@Range` tags; `defaultModuleVersion` 2.0.3 → **2.1.0** | grep |

## PR2 — S7 `lint-demand-scope.sh` (kit; RED d0f5942 DS1-DS7 + smoke) — D2/D3
| # | Invariant | How to decide |
|---|---|---|
| 2.1 | Path `toolbelt/lint-demand-scope.sh`; row `WARN  demand-in-scope  <file>:<line>  <method> reads <pv> with no demand-shaped input in scope`; exits 0/1(--strict)/3(usage) — WARN-only, never hard FAIL (B820 §820.3) | run the bats; grep the row printf |
| 2.2 | Scope = params **plus enclosing-class fields** (DS3); rule keys on a PV READ used as a comparison operand (DS4 no-flag) | read the awk rules |
| 2.3 | D9b dot-dir prune `-name '.*' -prune`; DS-smoke = real `CompressorControl.java` alone → 0 WARN on `step` | run |
| 2.4 | K19 routing: BUILD-LOOP §5 + `skill/SKILL.md` name the script; `kit-links.bats` green; shellcheck 0 | run |

## PR3 — S18 `lint-silent-protection.sh` (kit; RED e38e503 SP1-SP8 + smoke) — D2/D3/D10
| # | Invariant | How to decide |
|---|---|---|
| 3.1 | Pass-0 **dir-wide index** before the per-file loop (the cross-file one-level field→slot follow, SP2); effect-slot exemption; allowlist `{*Alarm,*Fault,*Skip*,*Reason,*Status,*Mismatch,*Stuck,*Available,*Fallback}` (B824 §824.2 :34); private allowlist-named FIELD is NOT a surface (SP8) | read the passes; run SP2/SP8 |
| 3.2 | Exactly ONE row per trip site (SP1/SP3/SP8 `grep -c WARN == 1`); dedupe `<file>:<line>` | run |
| 3.3 | SP-smoke flat dir of the 3 real files: output has `CompressorControl.java` (CP-1 `:215`) AND `BEvaporatorUnit.java` (CR-3 `:1287`) AND NOT `dischargeHighAlarm` (CP-2 clean via `:1994`) | run against the a109249 worktree files |
| 3.4 | PR3 acceptance beyond the pin: exact WARN count + subjects + absence on ALL FOUR client module roots recorded in the retro (lesson 11) | PR body |
| 3.5 | Cross-lane: after PR8 lands, CR-3 becomes surfaced → the smoke expectation flips; whichever merges second updates the pin (D9) | note in both PRs |

## PR4/PR5 — S12-A write-server (tunnel; RED `qa/c9-s12-write-server` e7e6615 S12A-1..9, already rebased on 9acb47c; +S12A-8 failed-write audit row, +S12A-9 spool replay idempotency) — D6/D7
| # | Invariant | How to decide |
|---|---|---|
| 4.1 | `buildServer(cfg, deps)` returns the handler without binding; `main()` binds behind the import.meta guard; `deps = {supabase, station, clock, spool}` injectable | read the export; node:test opens no socket |
| 4.2 | `POST /config/login` mints a **server-held** token bound to `(email, purpose)` with absolute TTL + sliding inactivity; `/config/logout` deletes; `/write` + `/alarms/ack` 401 without/expired; `/equipment` + `GET /alarms` ungated; ORD allowlist server-held → 403 before the station is touched; **no user+password store anywhere** | grep `password` writes = 0 persisted; read the token map |
| 4.3 | Per write: GET old → PUT/POST station → capture `result` → insert exactly ONE `change_log` row; `old_value` from the pre-write GET never the body; on insert failure the write still returns the station outcome + JSON-lines spool, drained on next success (S12A-6) | read the handler order |
| 4.4 | Schema migration additive-only: ADD `ts, config_session, result, surface, client_ip` beside the 9 existing columns; no DROP/retype; the 9acb47c insert still passes | read the migration |
| 4.5 | Setpoint body = child leaf `${ord}/value` bare `<real>` (S12A-7, B826 preferred form) | grep |

## PR6 — S12-B servlet guards + real Context (client DashboardPan-ux; RED 4c18837, 7 pins) — D8/D8a/D8b
| # | Invariant | How to decide |
|---|---|---|
| 6.1 | `DashboardWriteGuards.evaluate(xhr, user, operatorWrite, ord, value, AuditSink)` static; `handleSetpointWrite` (`:195`) calls it once and returns on non-200 | read |
| 6.2 | Guard order 302 → 401 → 403 → 400(value) → 400(ORD); guard 4 moved INTO `evaluate` with its type-scoping (numeric/reltime only; boolean/enum/string untouched); the existing `:274-288` block is what moves — mutation = deleting it re-exposes `parseDouble` 0.0 at `:403-407` | diff hunks; run guard4/4b |
| 6.3 | `parent.set(prop, toSet, null)` at `:291` is GONE **everywhere** (`grep -rn 'set(.*, null)'` on the write path = 0); the write passes a user-bearing Context; `catch (PermissionException …)` explicit → `SC_FORBIDDEN`, no catch-all before it | grep both patterns |
| 6.4 | Fire-and-forget module audit `:312-313` preserved; exactly one audit entry per success (success pin); audit failure never fails the write | run |
| 6.5 | `defaultModuleVersion` → 2.2.0 (fragment-merge with R14) | grep |

## PR6b / R14 — config login (client DashboardPan-ux; RED cc1c948 CL1-CL11 + CLW1-CLW5 + SC13) — D8c
| # | Invariant | How to decide |
|---|---|---|
| 14.1 | Login order = getUser → canLogin → `instanceof BPasswordCache` → validate → authenticateOk/Failed (B830 §830.2); **CL2/CL5/CL6 never reach validate**; CL6 = **401** (never leak the scheme); `authenticateFailed` called exactly once on a wrong password | read `ConfigLoginGuard.login`; run CL2/3/5/6 |
| 14.2 | **Session stores the USERNAME, never the `BUser`**; re-resolves per request; TTL + sliding; `/api/config/logout` revokes | read `ConfigSession` |
| 14.3 | CLW3 regex: third arg of `parent.set(prop, toSet, X)` is a **bare identifier**; the literal `parent.set(prop, toSet, null)` absent | grep |
| 14.4 | CLW4 regex: `catch (PermissionException …) { … SC_FORBIDDEN … }` with a **flat body** (no `}` before `SC_FORBIDDEN`) | grep + read |
| 14.5 | Guard order **1 XHR → 2 kiosk-auth-401 → 6 session-403 → 3 OPERATOR(config-session user)-403 → 4 → 5**; RBAC on the write path evaluates the config-session user (kiosk may be viewer-only; CL10 = framework `PermissionException` authoritative) | read the write path |
| 14.6 | `WebOp.getUser()` untouched (CL9: kiosk identity unchanged); password never persisted/logged (`grep -n password` only in the login handler, read-then-discard) | grep |
| 14.7 | Dispatch: `POST /api/config/login` + `/api/config/logout` singletons `ConfigLogin`/`ConfigLogout` beside `SetpointWrite` (`:59-63`), inside the XHR-guarded `/api/*` POST branch (`:117-132`) | read `DashboardDispatch` |
| 14.8 | MIR5 consequence: identity → the `user` column; `config_session` stays NULL for surface B (AuditEvent has no session field, `ComplexSlotMap.java:1687`) | R7 pin text |

## PR7 — R7 AuditHistory→`change_log` mirror (tunnel; REDs MIR1-MIR5 to author) — D7a
| # | Invariant | How to decide |
|---|---|---|
| 7.1 | Flag-gated **OFF by default**; reads `/PANCCADIA/AuditHistory`; inserts `surface='servlet'`, `config_session=NULL` | read the flag + insert |
| 7.2 | Idempotence key `(ts, user, target, old, new)` as a **unique index**; fixture replayed twice → same row count (MIR idempotence pin) | read migration; run |
| 7.3 | Station gains no outbound dependency; servlet holds no Supabase credential | grep the client diff = 0 tunnel refs |
| 7.4 | Live enablement = B829-live / B830-G1 gate, never a PR gate | PR body |

## PR8 — R8 CR-3 alarm, Pattern A (client ColdRoomPan-rt; RED 70a357b CRA1s/2s/3s/4/5/6) — D9
| # | Invariant | How to decide |
|---|---|---|
| 8.1 | Child `BBooleanPoint freezeAlarmPt` + child `BAlarmSourceExt` with `BBooleanChangeOfStateAlgorithm` `alarmValue = true`; `recomputeFreeze()` (`:1092` region) writes the point's out from `freezeTripped` on change | read the slot block + the one drive line |
| 8.2 | **Additive-only vs the 21-slot a109249 baseline** (CRA4 string in the RED): no rename/retype/remove; `schema-risk.sh` SAFE | run CRA4; PR body |
| 8.3 | Harness-only halves (live routing, clear, `sourceState==offnormal`) are **declared in the header, absent from WSL** — the PR body must not report them green; CRA5 mutation flip recorded | PR body wording |
| 8.4 | ColdRoomPan `defaultModuleVersion` **2.1.0** (CRA6); cross-lane note to PR3 (CR-3 WARN closes) | grep |

## PR9 — R9 CP-1 alarm, Pattern B (client CompPan-rt; RED 8b43488 CPB1-4/W1-W4/SC13) — D10
| # | Invariant | How to decide |
|---|---|---|
| 9.1 | `CompressorControl.AlarmEdge` static nested, Baja-free: `int decide(int trip, boolean nowOffnormal, boolean recoveredPastDeadband)` → `static final int FIRE/CLEAR/NONE`; `reseed(boolean[])` never fires; per-trip `wasOffnormal` independent | javac + run CPB1-4 |
| 9.2 | Adapter `implements BIAlarmSource`; **transient** `new AlarmSupport(this, "defaultAlarmClass")` in `started()`; `newOffnormalAlarm` only under `== AlarmEdge.FIRE`; `toNormal(BFacets.DEFAULT, null)` on CLEAR; `started()` calls `reseed(` from the current condition | run W1-W4; read |
| 9.3 | Deadband on recovery (~1 psi) — no chatter; **exactly one** record over N executes while offnormal (CPB2; mutation = level-triggered) | run |
| 9.4 | Additive-only vs the BCompressorControl a109249 slot set (CPB6); `schema-risk.sh` SAFE; Compresores **2.2.0** (after PR1's 2.1.0) | run; grep |
| 9.5 | CPB5 (`sourceState` on the routed record) declared harness-only — not reported green from WSL | PR body |

## PR10 — S19 `lint-ext-writable-shape.sh` (kit; RED `qa/c9-ext-writable-shape` 269be48 EW1-EW11 — EW11 = no-sources exit 3 (K20); EW10 = exact a109249 contract via `C9_CLIENT_ROOT`; four-root exact counts are PR10 ACCEPTANCE, not a bats pin)
| # | Invariant | How to decide |
|---|---|---|
| 10.1 | Bare complex `OPERATOR` property (`BStatusNumeric/Boolean/Enum`) with no writing `@NiagaraAction` → WARN naming the child `…/value` leaf; plain double/boolean/reltime clean; complex-with-action clean; SUMMARY-only ignored (EW4); `--strict` → 1; no arg → 3; D9b prune | run EW1-EW9 |
| 10.2 | EW10 exact contract on the `C9_CLIENT_ROOT` worktree: exactly 1 WARN `BRoomPanel.setpoint` on DashboardPan-rt, 0 on ColdRoomPan-rt / CompPan-rt / DashboardPan-ux (SKIP if the root is absent — a SKIP is not a PASS); EW11 empty/no-Java dir → exit 3 + ERROR row | run against the worktree |
| 10.3 | K19 routing + `kit-links.bats` | run |

## PR11 — R11 write-path matrix rows (client docs; RED `qa/c8-write-path` 5e357d1) — D11
| # | Invariant | How to decide |
|---|---|---|
| 11.1 | Matrix lives in the CLIENT repo (`<client-root>/docs/write-path-matrix.md`), sized by the MEASURED uncovered set (27 slots + `rotationInterval`/`rotationMode`), not a fixed nine; each row's Test column names a test present in `srcTest/` | run `lint-write-path.sh` on the client root → exit 0; delete one row → that slot FAILs |

## PR12 / PR13 — doctrine fold + close (kit docs)
| # | Invariant | How to decide |
|---|---|---|
| 12.1 | BUILD-LOOP §5 ONE module-root/profile convention; METHODOLOGY **K22**; `types/logic-authoring.md` slot types for externally written values; `types/logic.md` alarm patterns A/B; `types/dashboard.md` one-liner; one `[ev:]` per row; every new script routed in BUILD-LOOP + SKILL (K19) | grep tokens; `kit-links.bats` |
| 13.1 | Close: every campaign9 retro row has a core token BEFORE its INDEX flip (the C8 §19/§20 lesson); `sweep-fold-audit --strict` clean; BUILD-STATE `retro_pending:false` section-scoped; canonical `Retro: promotion (folds <ids>)` trailer | run the three guards |
