# C9 PR11 / R11 — the 62+2 `write-path-matrix.md` rows, ready to paste (client `docs/`)

Author: companero (Fable), 2026-09-06. Input = the R11 measurement (`2026-09-06-c9-r11-write-path-matrix-measurement.md`:
62 uncovered OPERATOR slots at a109249 = ColdRoomPan-rt 6 · CompPan-rt 15 · DashboardPan-rt 41) + PR1's two new S20 slots.
Link targets for the 41 facade slots come from `bog-nav links --from Cuarto1|Cuarto3 --slot <x>` on the PANCCADIA
`config.bog` (Cuarto1/Cuarto3 exemplars; the other rooms follow the same pattern — confirm per room at apply).
Columns and legend are the matrix's own: `Writable Slot | Writer | Timing | Invariant | Test`, ✅ tested / 🔶 partial /
❌ untested. `[ev: r11 measurement]` `[ev: bog-nav links @ PANCCADIA config.bog]` `[ev: module-find slots @ a109249]`

**Row key = the bare slot name** (that is the token `lint-write-path.sh` greps in the matrix — confirm the lint's match
rule at apply; the class prefix is for humans). Writer vocabulary: `HMI` = DashboardPan servlet dynamic `parent.set(prop,
toSet)` (`BDashboardServlet.java`), `WB` = Workbench property sheet, `oBIX` = external write (write-server / future).

## SC-9 check (exit 1 → 0)
```bash
export PATH=/usr/bin:/bin:$PATH; KIT=/home/cristian/modulos_niagara_n4/niagara-tools   # kit root (build-n4-module)
cd <client-root>   # the a109249 worktree
for r in ColdRoomPan-rt CompPan-rt DashboardPan-rt; do
  "$KIT/toolbelt/lint-write-path.sh" --matrix docs/write-path-matrix.md "$r/src"; echo "$r exit=$?"
done   # all three must print exit=0 after the rows land (today: 1/1/1 — R11 measurement)
```
(Take the exact flag spelling from the R11 measurement probe if `--matrix` differs; the three roots + exit 0 are the pin.)

---
## ColdRoomPan-rt (6) — module `Paccadia`
| Writable Slot | Writer | Timing | Invariant | Test |
|---|---|---|---|---|
| `BEvaporatorUnit.fanMode` (double, HOA 0=auto/1=on/2=off) | WB · HMI via facade link (`CuartoN.evapMFanMode` → `EvaporatorUnit_M.fanMode`) | read on every `execute()`; applied at the HOA stage (`:1238` family) | value ∈ {0,1,2}; `off` forces fan off even during defrost fan-off delay | 🔶 `EvaporatorUnitTest` (HOA apply) — add `fanModeOffOverridesAuto` |
| `BEvaporatorUnit.valveMode` (double, HOA) | WB · HMI via `evapMValveMode` link | every `execute()`; `valveInhibited()` `:1102` still wins on freeze | value ∈ {0,1,2}; `on` never overrides the freeze inhibit | 🔶 `ColdRoomControlTest.freezeTrip` — add `valveModeOnStillInhibitedByFreeze` |
| `BEvaporatorUnit.freezeProtect` (boolean) | WB · HMI via `evapMFreezeProtect` link | `recomputeFreeze()` `:1088` on change/execute | false ⇒ latch never trips (`freezeTripped` stays false) | ✅ `ColdRoomControlTest.freezeTrip*` (pure) |
| `BEvaporatorUnit.freezeDiffStop` (double) | WB · HMI via `evapMFreezeDiffStop` link | `recomputeFreeze()` | `0 < diffStop`; trip at `coil < setpoint - diffStop`; `diffRestart > diffStop` for hysteresis | ✅ `ColdRoomControlTest.freezeTrip*` |
| `BEvaporatorUnit.powerOnDelay` (BRelTime) | WB | `started()` → one-shot timer; `Clock.schedule` rejects `<= 0` (C9 gap lifecycle seam) | `> 0` or the delay is skipped (guard, not a crash) | ❌ add `EvaporatorUnitTest.powerOnDelayZeroSkipsSchedule` |
| `BColdRoomControl.coolOnSensorFault` (boolean) [confirm class at apply: `module-find slots --name coolOnSensorFault`] | WB · HMI (facade `coolOnSensorFault` has NO link — see DashboardPan row) | `execute()` when zone sensor status is fault/stale | true ⇒ cooling continues on fault; false ⇒ cooling off on fault | 🔶 `ColdRoomControlTest` — add `sensorFaultFollowsCoolOnSensorFault` |

## CompPan-rt (15 + 2 from PR1) — module `Compresores` (`BCompressorControl` config → `CompressorControl.Cfg`)
| Writable Slot | Writer | Timing | Invariant | Test |
|---|---|---|---|---|
| `condenser1Mode` / `condenser2Mode` / `condenser3Mode` (double, HOA) | WB · HMI | every `execute()` `:1891` (`cmdPreHoa` → HOA) | ∈ {0,1,2}; `off` removes the condenser from the stage set | 🔶 `CompressorControlTest` (HOA) — add `condenserModeOffDropsStage` |
| `faultReset` (boolean, one-shot) | WB · HMI | edge on `changed()`; auto-clears to false | true→false pulse; clears `faultLatch` only, never `freeze`/`suction` latches | ❌ add `faultResetPulseClearsLatchOnly` |
| `floatingSuction` (boolean) | WB | `execute()`: selects fixed vs floating suction target | when true, `suctionBand` bounds the float; when false, `suctionLowLimit` fixed | 🔶 `CompressorControlTest.step*` — add `floatingSuctionUsesBand` |
| `minOn` (BRelTime) | WB | stage-down guard `:246` family | `>= 0`; a stage cannot drop before `minOn` elapsed | ✅ `CompressorControlTest` (min-on) |
| `powerOnDelay` (BRelTime) | WB | `started()` one-shot; `Clock.schedule` rejects `<= 0` | `> 0` or skipped (guard) | ❌ add `powerOnDelayZeroSkipsSchedule` |
| `runningAmpsThreshold` (double) | WB · HMI | `execute()` start-prove check with `startProveDelay` | `> 0`; amps below threshold after prove delay ⇒ fault | 🔶 `CompressorControlTest.startProve*` — add `ampsThresholdZeroDisablesProve` |
| `stageDelay` / `stageUpDelay` / `stageDownDelay` (BRelTime) | WB · HMI | stage timers (`stageReady` `:221`, stage-up write `:229`) | `>= 0`; up/down delays independent; `stageDelay` is the legacy shared default | ✅ `CompressorControlTest` (stage timing) |
| `startProveDelay` (BRelTime) | WB | after a stage-up write `:229` | `>= 0`; prove window before `runningAmpsThreshold` applies | 🔶 same as `runningAmpsThreshold` |
| `suctionBand` (double) | WB · HMI | `execute()` | `> 0` when `floatingSuction`; ignored otherwise | 🔶 `floatingSuctionUsesBand` |
| `suctionLowLimit` (double) | WB · HMI | `execute()` — CP-1 shed `CompressorControl.java:215` + PR9 alarm edge | `> 0` enables the shed; `0` disables (no alarm) | ✅ `CompressorControlTest.cp1*` + PR9 `CompressorAlarmEdgeTest` |
| `suctionMismatchTol` (double) | WB | `execute()` sensor-mismatch check | `>= 0`; `0` disables | ❌ add `suctionMismatchTolZeroDisables` |
| `rotationMode` (int enum: 0 make-before-break / 1 break-before-make) — **PR1/S20** | WB · HMI | `execute()` at the swap decision (`pickLeastHoursOffAuto`) | ∈ {0,1}; E1–E4 rules (S20 rev 2) | ✅ `CompressorRotationTest` (17, PR1) |
| `rotationInterval` (BRelTime, MIN 0 / MAX 24 h, `SUMMARY|OPERATOR`; `0` = rotation DISABLED — the byte-identical sentinel, ROT5) — **PR1/S20** | WB · HMI | `execute()` | `>= 0`; `0` = rotation off | ✅ `CompressorRotationTest` |

## DashboardPan-rt (41) — module `DashboardPan`, class `BRoomPanel` (facade; the ROOM panel is the link SOURCE)
Class A = facade pass-through (a link carries the value into the logic module; the invariant lives THERE). Class B = local
config with no outgoing link. Class C = labels.
| Writable Slot | Writer | Timing | Invariant | Test |
|---|---|---|---|---|
| `defrostDuration` (A) | HMI (`parent.set`) · WB | link → `ColdRoom_N/DefrostController.duration` on change | `> 0`; see DefrostController rows | 🔶 structural: `bog-nav links --from CuartoN --slot defrostDuration` resolves for every room; logic test in Paccadia |
| `evap1FanMode` / `evap2FanMode` / `evap3FanMode` (A) | HMI · WB | link → `EvaporatorUnit_M.fanMode` (**Cuarto1 is crossed: evap1→Unit_3, evap3→Unit_1** — CHECK18) | ∈ {0,1,2} | 🔶 structural link pin per room + the crossed-tile pin (`bog-nav tiles`) |
| `evap1ValveMode` / `evap2ValveMode` / `evap3ValveMode` (A) | HMI · WB | link → `EvaporatorUnit_M.valveMode` (same crossing) | ∈ {0,1,2} | 🔶 same |
| `evapNFreezeSetpoint` ×3 (A) | HMI · WB | link → `EvaporatorUnit_N.freezeSetpoint` | plausible °C range for the room; `diffRestart > diffStop` | 🔶 structural + `ColdRoomControlTest.freezeTrip*` |
| `evapNFreezeDiffStop` ×3 (A) | HMI · WB | link → `EvaporatorUnit_N.freezeDiffStop` | `> 0` | 🔶 same |
| `evapNFreezeDiffRestart` ×3 (A) | HMI · WB | link → `EvaporatorUnit_N.freezeDiffRestart` | `> diffStop` | 🔶 same |
| `evapNFreezeProtect` ×3 (A) | HMI · WB | link → `EvaporatorUnit_N.freezeProtect` | boolean | 🔶 same |
| `evapHighLimit` / `evapLowLimit` (A) | HMI · WB | link → `EvaporatorUnit_1.evapHighAlarmLimit` / `evapLowAlarmLimit` (unit 1 only — the room's alarm reference) | `low < high` | 🔶 structural; add `evapLimitsOrdered` on the logic side |
| `resist1Mode` / `resist2Mode` (A) | HMI · WB | link → `ColdRoom_N/EvaporatorUnit_1|2.resistanceMode` | ∈ {0,1,2} | 🔶 structural |
| `resistanceTempThreshold` (A) | HMI · WB | link → `ColdRoom_N/DefrostController.resistanceTempThreshold` | `> 0` | 🔶 structural + `DefrostControllerTest` |
| `staggerDelay` (A) | HMI · WB | link → `ColdRoom_N/DefrostController.staggerDelay` | `>= 0` | 🔶 structural + `DefrostControllerTest` |
| `startDelay` (A) | HMI · WB | link → `EvaporatorUnit_1.startDelay` | `>= 0` | 🔶 structural |
| `comp1Mode` / `comp2Mode` (B — **no outgoing link at a109249**) | HMI · WB | none observed | writing it changes NOTHING in logic today | ❌ **decision**: wire a link to `CompPan` or drop OPERATOR (dead facade) |
| `intercambiadorMode` (B — no link) | HMI · WB | none observed | same | ❌ same decision |
| `fanMode` (B — no link; the per-evap `evapNFanMode` are the real ones) | HMI · WB | none observed | same | ❌ same decision |
| `coolOnSensorFault` (B — no link; the logic slot is written in Paccadia directly) | HMI · WB | none observed | same | ❌ same decision |
| `zoneHighLimit` / `zoneLowLimit` / `resistHighLimit` (B — local alarm colouring) | HMI · WB | read by `BRoomPanel` itself when computing tile state [confirm with `module-find callers`] | `low < high`; no logic effect | 🔶 add `RoomPanelStateTest.limitsColourTile` |
| `zoneTemp1Label` / `zoneTemp2Label` / `evapTemp1Label` / `evapTemp2Label` / `evapTemp3Label` / `resistTemp1Label` / `resistTemp2Label` (C) | HMI · WB | display only | free text, cosmetic | ✅ servlet round-trip (`DashboardServletTest`) — no invariant |

Row count: ColdRoomPan-rt 6 · CompPan-rt 15 (+2) · DashboardPan-rt 41 (26 A + 8 B + 7 C) = **62 + 2** ✓.

## Notes for the apply
- Class B rows carry a real finding (four OPERATOR facade slots with no consumer at a109249). The matrix row makes the
  lint pass; the DECISION (wire or demote) is a separate client issue — open it, do not fold it into PR11.
- Class A "Test" = a structural pin: a JUnit or bats test that runs `bog-nav links --from CuartoN --slot <x> --csv` against
  the committed `config.bog` and asserts the target path; that is what makes 🔶 honest (the logic invariant is tested in
  the logic module; the LINK is what the facade owns).
- Cuarto1 crossed tiles (CHECK18) must be written into the evap1/evap3 rows, not hidden in a footnote.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 62 uncovered = 6/15/41 | [CERT] | r11 measurement (lint-write-path on the three roots @ a109249) |
| 2 | 26 linked / 15 unlinked facade slots and their targets | [CERT] | bog-nav links --from Cuarto1/Cuarto3 (PANCCADIA config.bog) |
| 3 | Cuarto1 evap1↔Unit_3 crossing | [CERT] | bog-nav tiles (CHECK18) |
| 4 | invariants column | [INFER from the logic reads] | verify each against the pure tests named at apply |
| 5 | second S20 slot name | [pending] | S20 rev 2 package §slots |
