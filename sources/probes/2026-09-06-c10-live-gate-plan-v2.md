# C10 live-gate plan v2 — ONE paired agenda Cristian can authorize (supersedes 2026-09-06-c9-live-gate-plan.md)

investigador1, 2026-09-06. Folds the post-C9 facts into the v1 plan: client main **ff1b659** (C9 merged — rotation 2.1.0,
servlet guards + R14 config login 2.2.0, CR-3 alarm 2.1.0, CP-1 alarm 2.2.0); tunnel stack **UNMERGED** (PRs #1-#3 blessed,
Cristian-owned); station runs OLDER jars (ColdRoomPan-rt 2.0.3 / CompPan-rt 2.0.1 / DashboardPan 2.0, RAR a34f9bdd2). All
writes TEST-room only, reversible, restored. Read-only first, then the minimum writes. Nothing runs without Cristian's go.
`[ev: corpus B822/B827/B829/B830]` `[ev: memory build-n4-campaign9-close]`

## Preconditions (no station contact) — the ORDER matters
| # | Precondition | Why |
|---|---|---|
| **P0** | **Deploy chain FIRST**: the pending 2.0.7/2.0.3/2.1.1 deploy (client PR#9 runbook), THEN the C9 jars (CompPan **2.1.0**→**2.2.0**, ColdRoomPan **2.1.0**, DashboardPan **2.2.0**) — additive slots, `schema-risk.sh` SAFE, no jar downgrade path (deploy runbook delta `7b4e385ee`). Until these land the station has NEITHER the alarms NOR R14 | the alarm + config-login live gates test code that is NOT on the station today |
| **P1** | **Tunnel merge (Cristian-owned)** before any surface-A gate: write-server PRs #1-#3 (config login, audit schema, AuditHistory mirror) merge to tunnel main + the two Supabase migrations applied (change-log-extend + optional dedupe index, PG15 proof) | S12-A write / mirror gates need the merged write-server + the `change_log` extended columns |
| **P2** | `AuditHistoryService` present at `/PANCCADIA/AuditHistory` (B829-G1 CLOSED by bog read) — re-confirm live | attribution rows land there |
| **P3** | ONE named test operator with a PASSWORD scheme (`BPasswordAuthenticator`) + `OPERATOR_WRITE` on the TEST room (B830 §830.6: LDAP/SAML/gauth cannot take the R14 path) | R14 second-login needs a re-verifiable operator |
| **P4** | Snapshot: `station-snapshot.sh` + the TEST room's setpoint/HOA/alarm-routing (restore values) | every write below has a recorded restore |

## Part 1 — READ-ONLY (run first; de-risks everything, closes nothing alone)
| # | Gate | Action (read) | Expect |
|---|---|---|---|
| R1 | B829-live | oBIX GET / Workbench AuditHistory — last 20 rows (`userName`) | baseline; past oBIX PUTs = the single write-server station user |
| R2 | B830-G1 | GET as the kiosk session; confirm `WebOp.getUser()` = the shared kiosk account | baseline kiosk identity |
| R3 | B827-G2 | AlarmService `defaultAlarmClass` routing wired to a console recipient; GET DashboardPan alarms bql (`sourceState='offnormal' or 'fault'`) | baseline: the RT modules now CAN alarm (post-deploy) — expect none until a trip fires |
| R4 | B822-G1 | oBIX GET the TEST `BRoomPanel`: is `<op name="applySetpoint" …>` advertised? (skip W4 if absent — additive setpointCmd path was not shipped in C9) | `<op>` present or absent |
| R5 | all | `triage-console.sh --console-dir` last hour: no SEVERE from our modules | clean baseline |

## Part 2 — MINIMUM WRITES (TEST room only; each with its restore)
| # | Gate | Write | Restore | Must observe | Closes |
|---|---|---|---|---|---|
| W1 | B830-G1 / B829-live (surface B, R14) | on the HMI: config-login as the test operator → write the TEST setpoint via the panel → logout | write the recorded setpoint back | AuditHistory row `userName` = **the test operator** (not kiosk); a write WITHOUT config-login → 403 `config_login_required`; kiosk `WebOp.getUser()` unchanged after logout; (optional) 5 wrong passwords → operator `lockOut=true`, self-clears 10 s | B830-G1; the C9 R14 live proof |
| W2 | B827-G2 (CR-3, Pattern A) | force the freeze condition on the TEST room (coil temp < freezeSetpoint) so `freezeTripped` latches → drives `freezeAlarmPt` | clear the override | ONE `BAlarmRecord`, `sourceState=offnormal`, in the console + DashboardPan bql; **out survives the point's execute cycle** (the B827-G2/PR8-risk-1 open item); clears + `toNormal` on recovery; ack works | B827-G2 (Pattern A); PR8 risk-1 |
| W3 | B827-G2 (CP-1, Pattern B) | drive TEST suction < suctionLowLimit (only if a TEST compressor path allows) → `newOffnormalAlarm` on the edge | remove the override | ONE record on the edge; holding the condition N cycles → NO second record (edge, not level); `toNormal` on recovery; restart re-seeds without re-firing | B827-G2 (Pattern B); B831-S23 note |
| W4 | B829-live (surface A) — **only after P1 tunnel merge** | write-server `/write` with a config token → oBIX PUT `…/CuartoTEST/setpoint/value` `<real>` | PUT the recorded value | `change_log` gains ONE row (operator email, old via pre-write GET, new, ts, result, surface='write-server', config_session); AuditHistory row `userName` = the shared oBIX write user (B829 §829.2) | B829-live (A); S12-A |
| W5 | B822-G1 (only if R4 advertised `<op>`) | oBIX `POST …/applySetpoint <real>` as the write user | POST the recorded value | 200 + setpoint changes (child `/value` GET, ≤~1.5 s); same POST WITHOUT `OPERATOR_INVOKE` → 403 | B822-G1 (informational) |

## Part 3 — Windows niagaraTest harness session (H1-H3) — pairs with the above, runs on the Windows side
| # | Item | What |
|---|---|---|
| H1 | Harness gate | `detect-tools.sh` / `run-station-test.sh`: `bin/test.exe` launches via WSL interop against a Windows-side COPY of the install (B815) |
| H2 | Lifecycle BTest | `BColdRoomLifecycleTest` GREEN on the fixed tree, FAIL on pre-fix 4f5f1c7 (S1 seam) — the executable coverage lint+JUnit cannot mount |
| H3 | Alarm-routing harness half | the CRA1/2/3 + CPB5 live-routing pins (declared harness-only in the RED headers) run on niagaraTest — confirm the alarm actually routes off-station (the WSL structural pins never claimed this) |

## After the session (no station contact)
| # | Step |
|---|---|
| A1 | Paste the AuditHistory diff (R1 vs W1/W4) + the `change_log` rows into `sources/probes/<date>-live-record.md` — the `[CERT-live]` evidence |
| A2 | Corpus upgrades: B829 (both trails live), B830-G1 CLOSED, B827-G2 CLOSED (+B821-G2), B822-G1 CLOSED or "not deployed"; PR8 risk-1 resolved; regen CATALOG |
| A3 | Enable the R7 mirror flag ONLY after A1 shows the surface-B AuditHistory row names the operator |
| A4 | Every restore value matches P4; console tail clean |

## What this cannot close (named)
- **B830-G2** (exact `getLogoffPeriod` chain), **B830-G3** (gauth TOTP) — code reads, not live.
- **B828-G2** (frozen enum on a NEW non-linked deploy) — observed for free if PR1 deployed (`rotationMode` renders with its range).
- **B831-G2** (the three C10 lint precisions) — pure lint, no live gate.
