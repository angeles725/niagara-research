# C9 live-gate plan — ONE paired session for the requires-execution gates (read-only first, TEST room only)

Author: investigador1, 2026-09-06. Purpose: let Cristian authorize a single live session instead of ad-hoc probes. Scope
= the gates no PR can close off-station: **B829-live / B830-G1** (AuditHistory attribution, surface A and B), **B827-G2**
(routed alarm reaches the console + the DashboardPan panel), **B822-G1** (`applySetpoint` invoke via oBIX + `OPERATOR_INVOKE`).
B823-G1 / B826-G1/G2 are already CLOSED `[CERT-live]` (record aa7054702). Station: PANCCADIA. **Every write goes to the
TEST room only** (the standing authorization), is reversible, and is restored before the session ends. Order = all
READ-ONLY steps first (0 risk), then the minimum writes. Each step: who runs it · exact action · what is written · what
must be observed · what closes. Nothing here runs without Cristian's explicit go. `[ev: corpus B822/B827/B829/B830]` `[ev: record aa7054702]`

## Preconditions (before the session — no station contact)
| # | Check | Evidence |
|---|---|---|
| P0 | **Deploy chain first.** The station RUNS ColdRoomPan-rt **2.0.3**, CompPan-rt **2.0.1**, DashboardPan-rt/ux **2.0** (station RAR `module.xml` read, niagara-research a34f9bdd2; DashboardPan-ux 2.0 re-confirmed by this author from the RAR) — repo a109249 (2.0.7/2.0.3/2.1.1) was NEVER deployed. The pending 2.0.7/2.0.3/2.1.1 deploy (client PR #9 runbook, `schema-risk.sh` SAFE) lands BEFORE any C9 jar; the live session runs on that chain, not on the C9 tips alone. Today the DEPLOYED DashboardPan 2.0 has NO numeric guard (the silent zero IS live) and ColdRoomPan 2.0.3 carries the defrost `time<=0` bug — both fixed in the repo only. `[CERT — RAR read a34f9bdd2]` `[ev: client PR #9]` |
| P1 | The PRs under test are DEPLOYED on the station: PR6 + R14 (servlet real Context + config login) for B830-G1; PR8 or PR9 (one alarm pattern) for B827-G2; B822-G1 needs `applySetpoint` action deployed (phase-2 item — **skip if not deployed**, informational only per proposal §10) | deploy runbook + `schema-risk.sh` SAFE on each |
| P2 | `AuditHistoryService` present at `/PANCCADIA/AuditHistory` (B829-G1 CLOSED by bog read) — re-confirm on the live tree (READ) | `bog-nav.py find --type h:AuditHistoryService` on the latest backup |
| P3 | Operator accounts: the kiosk account (viewer-only after R14, or whatever it is today) + ONE named test operator with `OPERATOR_WRITE` on the TEST room, password scheme (`BPasswordAuthenticator`) — B830 §830.6: LDAP/SAML/gauth users cannot take the config-login path | UserService read in Workbench (READ) |
| P4 | Snapshot BEFORE: `station-snapshot` / bog backup + the TEST room's current `setpoint`, HOA modes, alarm-class routing (so every write below has a recorded restore value) | `toolbelt/station-snapshot.sh` (READ + copy) |

## Part 1 — READ-ONLY (run first; closes nothing by itself, de-risks everything)
| # | Gate | Action (read) | Expected observation | Note |
|---|---|---|---|---|
| R1 | B829-live | oBIX GET `…/AuditHistory` (or Workbench AuditHistory view) — capture the last 20 rows: columns `timestamp, operation, target, slotName, oldValue, value, userName` | rows exist; `userName` for past oBIX PUTs = the single write-server station user (B829 §829.2) | baseline for the attribution diff |
| R2 | B830-G1 | GET `/dashboardpan/api/…` as the kiosk session; confirm `WebOp.getUser()`-derived identity in the response/log = the kiosk account | kiosk identity visible | baseline for CL9 live |
| R3 | B827-G2 | Workbench: open the AlarmService → `defaultAlarmClass` routing; confirm the console recipient is wired; GET the DashboardPan alarms endpoint (bql `sourceState='offnormal' or 'fault'`, `BDashboardServlet.java:502`) | current alarm list (expect none from our RT modules — B821 tier-1 unused) | baseline: zero rows from CompPan/ColdRoomPan |
| R4 | B822-G1 | oBIX GET the TEST room's `BRoomPanel` object: confirm `<op name="applySetpoint" in="obix:real"/>` is ADVERTISED (B822: an action is encoded as `<op>`) and the login user's permissions include `OPERATOR_INVOKE` on it | `<op>` present; if absent → PR not deployed → skip Part 2 step W4 | READ only |
| R5 | all | Console tail (`triage-console.sh --console-dir`) for the last hour: no SEVERE from our modules | clean baseline | |

## Part 2 — MINIMUM WRITES (TEST room only; each has its restore)
| # | Gate | Action (write) | Restore | Must observe | Closes |
|---|---|---|---|---|---|
| W1 | B829-live (surface A) | write-server `POST /write` with a config token (S12-A) → oBIX PUT `…/CuartoTEST/setpoint/value` `<real>` = current+0.5 | PUT the recorded value back | (a) `change_log` gains ONE row: `user_email` = the operator, `old_value` = pre-write GET, `new_value`, `ts`, `result`, `surface='write-server'`; (b) AuditHistory gains ONE row with `userName` = the **station write user** (not the operator) — B829 §829.2 confirmed live | B829-live (A) |
| W2 | B830-G1 (surface B) | on the HMI (kiosk session): config login as the test operator → write the TEST room setpoint via the panel (`POST /dashboardpan/api/setpoint`) → logout | write the recorded value back through the same path | (a) AuditHistory row `userName` = **the test operator** (not the kiosk) — `AuditEvent(…, user.getUsername())` `ComplexSlotMap.java:1687`; (b) the kiosk session identity unchanged after logout (CL9 live); (c) a write WITHOUT config login → 403 `config_login_required` (CL1 live); (d) wrong password ×1 → 401 and, after 5 within 30 s, the operator shows `lockOut=true` in UserService, self-clearing after 10 s (B830 §830.3) — **do the lockout only if Cristian wants it; it locks the test operator for 10 s** | B830-G1; B829-live (B) |
| W3 | B827-G2 | force the deployed alarm's trip on the TEST room: Pattern A — set the freeze condition (coil temp below `freezeSetpoint` via the simulated/override input) so `freezeTripped` latches; Pattern B — drive suction below `suctionLowLimit` (only if CompPan is under test and the rack is NOT running on the TEST path — otherwise use Pattern A only) | remove the override; confirm `toNormal` / point clears | (a) ONE `BAlarmRecord` in the console with `sourceState=offnormal`, `alarmClass=defaultAlarmClass`, source = the child point (A) or the component (B); (b) the DashboardPan alarms endpoint lists it; (c) holding the condition N cycles produces NO second record (CPB2 live); (d) on recovery the record goes `normal`, ack works (`ackAlarm`) | B827-G2 (= B821-G2 tier-1 confirm) |
| W4 | B822-G1 (only if R4 advertised the `<op>`) | oBIX `POST …/BRoomPanel/applySetpoint` body `<real val="<current+0.5>"/>` as the write user | POST the recorded value | 200 + `setpoint` changes (child `/value` GET confirms, propagation ≤ ~1.5 s per B826-G2); then the same POST as a user WITHOUT `OPERATOR_INVOKE` → 403/`PermissionException` | B822-G1 (informational) |

## After the session (no station contact)
| # | Step |
|---|---|
| A1 | Diff the AuditHistory rows (R1 vs after W1/W2) and the `change_log` rows; paste both into `sources/probes/<date>-c9-live-record.md` with timestamps — the record is the `[CERT-live]` evidence |
| A2 | Upgrade in the corpus: B829 (live confirm of both trails), B830-G1 → CLOSED, B827-G2 → CLOSED (+ B821-G2), B822-G1 → CLOSED or "not deployed"; regenerate CATALOG |
| A3 | Enable the R7 mirror flag ONLY after A1 shows the surface-B AuditHistory row names the operator (proposal §10: the mirror's live gate) |
| A4 | Confirm every restore value matches P4's snapshot; console tail clean |

## What this session cannot close (named)
- **B830-G2** (exact `getLogoffPeriod` chain) — code read, not live. **B830-G3** (gauth TOTP) — out of scope.
- **B828-G2** (frozen enum on a NEW non-linked deploy) — informational; observed for free ONLY if the deploy chain (P0 → PR1) has landed — `rotationMode` renders in Workbench with its range; add as R6 if so.
