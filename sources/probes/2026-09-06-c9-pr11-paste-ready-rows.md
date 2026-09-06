# C9 PR11 — PASTE-READY write-path-matrix rows (exact complement at PR1's tip e5bee1c)

Author: companero (Fable), 2026-09-06. Re-derived read-only with `lint-write-path.sh` on the three client module roots at
**PR1's tip `e5bee1c`** (worktree `Cliente/Leon-Guanjuato-worktrees/pr1-s20`). PR1 already added the two rotation rows
(`rotationInterval`, `rotationMode`) to `docs/write-path-matrix.md:95-96` — DO NOT re-add them. The rows below are the EXACT
remaining uncovered set: **CompPan-rt 15 · ColdRoomPan-rt 6 · DashboardPan-rt 41 = 62** (each root `exit 1` today; `grep -c '^FAIL'`
= 15 / 6 / 41). Columns are the matrix's own (`docs/write-path-matrix.md:21`): `Writable Slot | Writer | Timing | Invariant |
Test`; legend ✅ tested / 🔶 partial / ❌ untested. Row key = the BARE slot name in backticks (what the lint greps).
Facade link targets from `bog-nav` on the PANCCADIA `config.bog` (see `2026-09-06-c9-pr11-write-path-matrix-rows.md` for the
per-slot derivation and `2026-09-06-c9-issue-dead-panel-writes.md` for the class-B dead writes).
`[ev: lint-write-path @ e5bee1c, 2026-09-06]` `[ev: bog-nav @ PANCCADIA config.bog]`

## SC-9 check (exit 1 → 0, run after pasting)
```bash
export PATH=/usr/bin:/bin:$PATH
K=/home/cristian/modulos_niagara_n4/niagara-tools/build-n4-module-kit
W=<client-root>            # the PR11 worktree at PR1's tip or later
for r in Compresores/CompPan/CompPan-rt Paccadia/ColdRoomPan/ColdRoomPan-rt Dashboard/DashboardPan/DashboardPan-rt; do
  "$K/toolbelt/lint-write-path.sh" "$W/$r"; echo "$r exit=$?"
done   # today 1/1/1 (15/6/41 FAIL); after the paste all three must print exit=0
```

## CompPan-rt (15) — append under the CompPan section of `docs/write-path-matrix.md`
| Writable Slot | Writer | Timing | Invariant | Test |
|---|---|---|---|---|
| `condenser1Mode` | WB · HMI (Condensadoras panel) | every `execute()` (cmdPreHoa→HOA) | ∈ {0,1,2}; `off` drops the condenser from the stage set | 🔶 `CompressorControlTest` (HOA) — add `condenserModeOffDropsStage` |
| `condenser2Mode` | WB · HMI | every `execute()` | ∈ {0,1,2}; independent of condenser1 | 🔶 same |
| `condenser3Mode` | WB · HMI | every `execute()` | ∈ {0,1,2}; independent | 🔶 same |
| `faultReset` | WB · HMI (one-shot) | edge on `changed()`, auto-clears | true→false pulse; clears the fault latch only, never freeze/suction latches | ❌ add `faultResetPulseClearsLatchOnly` |
| `floatingSuction` | WB | `execute()` (fixed vs floating target select) | true ⇒ `suctionBand` bounds the float; false ⇒ `suctionLowLimit` fixed | 🔶 `CompressorControlTest.step*` — add `floatingSuctionUsesBand` |
| `minOn` (BRelTime) | WB | stage-down guard | `>= 0`; a stage cannot drop before `minOn` elapsed | ✅ `CompressorControlTest` (min-on) |
| `powerOnDelay` (BRelTime) | WB | `started()` one-shot; `Clock.schedule` rejects `<= 0` | `> 0` or the delay is skipped (guard, not a crash) | ❌ add `powerOnDelayZeroSkipsSchedule` |
| `runningAmpsThreshold` | WB · HMI | `execute()` start-prove check | `> 0`; amps below threshold after the prove delay ⇒ fault | 🔶 add `ampsThresholdZeroDisablesProve` |
| `stageDelay` (BRelTime) | WB · HMI | shared legacy stage timer | `>= 0`; the default when up/down are unset | ✅ `CompressorControlTest` (stage timing) |
| `stageUpDelay` (BRelTime) | WB · HMI | stage-up timer | `>= 0`; independent of stageDownDelay | ✅ same |
| `stageDownDelay` (BRelTime) | WB · HMI | stage-down timer | `>= 0`; independent of stageUpDelay | ✅ same |
| `startProveDelay` (BRelTime) | WB | after a stage-up write | `>= 0`; prove window before `runningAmpsThreshold` applies | 🔶 tied to `runningAmpsThreshold` |
| `suctionBand` | WB · HMI | `execute()` | `> 0` when `floatingSuction`; ignored otherwise | 🔶 `floatingSuctionUsesBand` |
| `suctionLowLimit` | WB · HMI | `execute()` — CP-1 shed + PR9 alarm edge | `> 0` enables the shed; `0` disables (no alarm) | ✅ `CompressorControlTest.cp1*` + PR9 `CompressorAlarmEdgeTest` |
| `suctionMismatchTol` | WB | `execute()` sensor-mismatch check | `>= 0`; `0` disables | ❌ add `suctionMismatchTolZeroDisables` |

## ColdRoomPan-rt (6) — append under the ColdRoomPan section
| Writable Slot | Writer | Timing | Invariant | Test |
|---|---|---|---|---|
| `fanMode` | WB · HMI via facade `evapMFanMode` link | every `execute()`; applied at HOA | ∈ {0,1,2}; `off` forces fan off even during defrost fan-off delay | 🔶 `EvaporatorUnitTest` — add `fanModeOffOverridesAuto` |
| `valveMode` | WB · HMI via `evapMValveMode` link | every `execute()`; `valveInhibited()` wins on freeze | ∈ {0,1,2}; `on` never overrides the freeze inhibit | 🔶 `ColdRoomControlTest.freezeTrip` — add `valveModeOnStillInhibitedByFreeze` |
| `freezeProtect` | WB · HMI via `evapMFreezeProtect` link | `recomputeFreeze()` on change/execute | false ⇒ latch never trips | ✅ `ColdRoomControlTest.freezeTrip*` (pure) |
| `freezeDiffStop` | WB · HMI via `evapMFreezeDiffStop` link | `recomputeFreeze()` | `> 0`; `freezeDiffRestart > freezeDiffStop` for hysteresis | ✅ `ColdRoomControlTest.freezeTrip*` |
| `powerOnDelay` (BRelTime) | WB | `started()` one-shot; `Clock.schedule` rejects `<= 0` | `> 0` or skipped (guard) | ❌ add `EvaporatorUnitTest.powerOnDelayZeroSkipsSchedule` |
| `coolOnSensorFault` | WB (HMI write is DEAD — no link, see the dead-panel-writes issue) | `execute()` on zone sensor fault/stale | true ⇒ cooling continues on fault; false ⇒ off on fault | 🔶 add `sensorFaultFollowsCoolOnSensorFault`; wire the facade link (issue) |

## DashboardPan-rt (41) — append under the DashboardPan section (facade; A=linked pass-through, B=no link, C=label)
| Writable Slot | Writer | Timing | Invariant | Test |
|---|---|---|---|---|
| `defrostDuration` (A) | HMI · WB | link → `ColdRoom_N/DefrostController.duration` | `> 0` | 🔶 `bog-nav links --from CuartoN --slot defrostDuration` resolves; logic test in Paccadia |
| `evap1FanMode` (A) | HMI · WB | link → `EvaporatorUnit_M.fanMode` (**Cuarto1 crossed: evap1→Unit_3**) | ∈ {0,1,2} | 🔶 structural link pin + crossed-tile pin (`bog-nav tiles`) |
| `evap2FanMode` (A) | HMI · WB | link → `EvaporatorUnit_2.fanMode` | ∈ {0,1,2} | 🔶 structural |
| `evap3FanMode` (A) | HMI · WB | link → `EvaporatorUnit_M.fanMode` (**Cuarto1 crossed: evap3→Unit_1**) | ∈ {0,1,2} | 🔶 structural + crossed-tile |
| `evap1ValveMode` (A) | HMI · WB | link → `EvaporatorUnit_M.valveMode` (crossed like evap1FanMode) | ∈ {0,1,2} | 🔶 structural |
| `evap2ValveMode` (A) | HMI · WB | link → `EvaporatorUnit_2.valveMode` | ∈ {0,1,2} | 🔶 structural |
| `evap3ValveMode` (A) | HMI · WB | link → `EvaporatorUnit_M.valveMode` (crossed like evap3FanMode) | ∈ {0,1,2} | 🔶 structural |
| `evap1FreezeSetpoint` (A) | HMI · WB | link → `EvaporatorUnit_1.freezeSetpoint` | plausible °C; `diffRestart > diffStop` | 🔶 structural + `ColdRoomControlTest.freezeTrip*` |
| `evap2FreezeSetpoint` (A) | HMI · WB | link → `EvaporatorUnit_2.freezeSetpoint` | same | 🔶 same |
| `evap3FreezeSetpoint` (A) | HMI · WB | link → `EvaporatorUnit_3.freezeSetpoint` | same | 🔶 same |
| `evap1FreezeDiffStop` (A) | HMI · WB | link → `EvaporatorUnit_1.freezeDiffStop` | `> 0` | 🔶 structural |
| `evap2FreezeDiffStop` (A) | HMI · WB | link → `EvaporatorUnit_2.freezeDiffStop` | `> 0` | 🔶 structural |
| `evap3FreezeDiffStop` (A) | HMI · WB | link → `EvaporatorUnit_3.freezeDiffStop` | `> 0` | 🔶 structural |
| `evap1FreezeDiffRestart` (A) | HMI · WB | link → `EvaporatorUnit_1.freezeDiffRestart` | `> diffStop` | 🔶 structural |
| `evap2FreezeDiffRestart` (A) | HMI · WB | link → `EvaporatorUnit_2.freezeDiffRestart` | `> diffStop` | 🔶 structural |
| `evap3FreezeDiffRestart` (A) | HMI · WB | link → `EvaporatorUnit_3.freezeDiffRestart` | `> diffStop` | 🔶 structural |
| `evap1FreezeProtect` (A) | HMI · WB | link → `EvaporatorUnit_1.freezeProtect` | boolean | 🔶 structural |
| `evap2FreezeProtect` (A) | HMI · WB | link → `EvaporatorUnit_2.freezeProtect` | boolean | 🔶 structural |
| `evap3FreezeProtect` (A) | HMI · WB | link → `EvaporatorUnit_3.freezeProtect` | boolean | 🔶 structural |
| `evapHighLimit` (A) | HMI · WB | link → `EvaporatorUnit_1.evapHighAlarmLimit` (room reference) | `evapLowLimit < evapHighLimit` | 🔶 structural; add `evapLimitsOrdered` in logic |
| `evapLowLimit` (A) | HMI · WB | link → `EvaporatorUnit_1.evapLowAlarmLimit` | `< evapHighLimit` | 🔶 same |
| `resist1Mode` (A) | HMI · WB | link → `ColdRoom_N/EvaporatorUnit_1.resistanceMode` | ∈ {0,1,2} | 🔶 structural |
| `resist2Mode` (A) | HMI · WB | link → `ColdRoom_N/EvaporatorUnit_2.resistanceMode` | ∈ {0,1,2} | 🔶 structural |
| `resistanceTempThreshold` (A) | HMI · WB | link → `ColdRoom_N/DefrostController.resistanceTempThreshold` | `> 0` | 🔶 structural + `DefrostControllerTest` |
| `staggerDelay` (A) | HMI · WB | link → `ColdRoom_N/DefrostController.staggerDelay` | `>= 0` | 🔶 structural + `DefrostControllerTest` |
| `startDelay` (A) | HMI · WB | link → `EvaporatorUnit_1.startDelay` | `>= 0` | 🔶 structural |
| `comp1Mode` (A on Cuarto5 only; B on 1-4) | HMI (Cuarto5) · WB | Cuarto5 link → `ColdRoom_5/EvaporatorUnit.valveMode` | ∈ {0,1,2}; on Cuarto1-4 the HMI never renders it | 🔶 Cuarto5 structural; note dead on 1-4 |
| `comp2Mode` (A on Cuarto5 only; B on 1-4) | HMI (Cuarto5) · WB | Cuarto5 link → `ColdRoom_5/EvaporatorUnit2.valveMode` | ∈ {0,1,2} | 🔶 Cuarto5 structural |
| `fanMode` (A on Cuarto5 only; B on 1-4) | HMI (Cuarto5) · WB | Cuarto5 link → `ColdRoom_5/EvaporatorUnit.fanMode` (per-evap `evapNFanMode` are the real ones on 1-4) | ∈ {0,1,2} | 🔶 Cuarto5 structural |
| `intercambiadorMode` (B — DEAD) | HMI (Cuarto3) · WB | none — no link, no station control exists | writing it changes nothing (dead-panel-writes issue) | ❌ decision: create the station control + link (exit 1a), or drop the control (exit 2) |
| `coolOnSensorFault` (B — DEAD) | HMI · WB | none — link intended (`BRoomPanel.java:148`), never made | writing it changes nothing today | ❌ wire `CuartoN → ColdRoom_N.coolOnSensorFault` station-side (issue) |
| `resistHighLimit` (B) | HMI · WB | read by `BRoomPanel` for tile colour | `low < high`; no logic effect | 🔶 `RoomPanelStateTest.limitsColourTile` |
| `zoneHighLimit` (B) | HMI · WB | tile colour | `zoneLowLimit < zoneHighLimit` | 🔶 same |
| `zoneLowLimit` (B) | HMI · WB | tile colour | `< zoneHighLimit` | 🔶 same |
| `zoneTemp1Label` (C) | HMI · WB | display only | free text | ✅ servlet round-trip (`DashboardServletTest`) |
| `zoneTemp2Label` (C) | HMI · WB | display only | free text | ✅ same |
| `evapTemp1Label` (C) | HMI · WB | display only | free text | ✅ same |
| `evapTemp2Label` (C) | HMI · WB | display only | free text | ✅ same |
| `evapTemp3Label` (C) | HMI · WB | display only | free text | ✅ same |
| `resistTemp1Label` (C) | HMI · WB | display only | free text | ✅ same |
| `resistTemp2Label` (C) | HMI · WB | display only | free text | ✅ same |

Row count: 15 + 6 + 41 = **62** (matches the lint's FAIL count per root). The 2 rotation rows are already in the matrix.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 62 uncovered = 15/6/41 at PR1 tip; each root exit 1 | [CERT] | `lint-write-path.sh` @ e5bee1c, 2026-09-06 (grep -c '^FAIL') |
| 2 | rotationInterval/rotationMode already in the matrix | [CERT] | `docs/write-path-matrix.md:95-96` @ e5bee1c |
| 3 | facade link targets, Cuarto1 crossed tiles, Cuarto5-only comp/fan | [CERT] | bog-nav @ PANCCADIA config.bog (dead-panel-writes issue) |
| 4 | invariants | [INFER from the logic reads] | confirm each against the named pure test at apply |
