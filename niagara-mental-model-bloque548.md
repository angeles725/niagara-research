# Block 548 — clHVAC Nordic + micro-modules: the cold-climate AHU family, room pre-control, and energy statistics — completing the clHVAC family enumeration

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC12 — clHVAC Nordic + micro-modules; last investigable gap)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read (completeness enumeration — no sub-agent).
**Primary sources** `[CERT]`: `organized/clHVACNordicAirCondition/…/nordic_air_condition/`,
`clHVACNordicGeneral/…/nordic_general/`, `clHVACRoomControl/…/room_control/`,
`clHVACEnergyManagement/…/energy_management/` (BCm* blocks).

**Scope**: the smallest clHVAC sub-modules, catalogued to COMPLETE the clHVAC family ([Block 87] architecture,
[Block 540] 4 representative sequences). These are the cold-climate ("Nordic") AHU variants plus room
pre-control and energy statistics — all on the SAME Eagle engine, so this is an enumeration, not new mechanics.

---

## 548.1 All ride the Eagle engine (REMITTANCE to B87/B540) [CERT]

Every block here `extends BControlFunctionSupport implements IEnvironment` `[CERT]` (spot-verified:
`BCmERC_Efficiency`, `BCmSPB_AirFlowControlNordic`, `BCmPSA_PlantModeNordic`, `BCmSCA_DXCoolingNordic`,
`BCmPRCH_PreCtrlHtg`) — the same Centraline/Eagle base class and `BControlProgramService` roster engine as the
main clHVAC library ([Block 87] §87.2, [Block 540]). No new execution model; these are additional `BCm*` domain
compositions of `Cf*` primitives. Naming convention `BCm<FAM>_<name>` (FAM = 3-letter family code).

## 548.2 The four sub-modules [CERT]

**`clHVACNordicAirCondition` — 12 BCm\* (cold-climate AHU):**
`BCmERC_Efficiency` + `BCmERC_WheelStatusAlarmNordic` (heat-recovery wheel efficiency + alarm),
`BCmSPB_AirFlowControlNordic` + `BCmSPA_StatPressControlNordic` (airflow / static-pressure control),
`BCmPSA_PlantModeNordic` (plant mode), `BCmSCA_DXCoolingNordic` (DX cooling), `BCmDMA_OnOffDamperNordic`
(on/off damper), `BCmCSA_ConstSupplyAirNordic` + `BCmCSA_CascContrNordic` (constant supply air / cascade
control), `BCmHCA_HeaterNordic` (heater), `BCmFNB_ModFanNordic` + `BCmFNA_2St_FanSimpleNordic` (modulating /
2-stage fan). The "Nordic" suffix marks cold-climate AHU variants (heat-recovery + freeze emphasis) parallel to
the main `clHVACAirConditioning` blocks ([Block 540]).

**`clHVACNordicGeneral` — 3 BCm\*:** `BCmLIN_LinearChar_5points` (5-point linear characteristic curve),
`BCmRSA_AHUManSwitchNordic` (AHU manual switch), `BCmSWM_SummerWinterMode` (summer/winter changeover).

**`clHVACRoomControl` — 2 BCm\*:** `BCmPRCC_PreCtrlClg` + `BCmPRCH_PreCtrlHtg` (pre-control cooling / heating —
room-level pre-conditioning).

**`clHVACEnergyManagement` — 4 BCm\*:** `BCmDDA_DEGDAYS` (degree-days, decompiled in [Block 540]),
`BCmTTB_Statistics`, `BCmTTB_SolarEnergy`, `BCmTTB_PulseCounter` (the `TTB` energy-metering/statistics family).

## 548.3 clHVAC family — now fully enumerated [CERT]

With this block the 9-module clHVAC family is completely addressed: `clHVAC` (250 primitives, [B540]),
`clHVACAirConditioning`/`clHVACHeating`/`clHVACChiller`/`clHVACEnergyManagement`/`clHVACGeneral` (main domain
sequences, [B540] + here), and the three Nordic/room micro-modules (here). All are one ecosystem on the Eagle
`BControlFunctionSupport` engine; the domain blocks (`BCm*`) are compositions of the 103 `Cf*` primitives.

## 548.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Nordic/micro blocks extend BControlFunctionSupport (Eagle engine, same as B87/B540) | [CERT] | BCmERC_Efficiency/SPB/PSA/SCA/PRCH.java class decls | token-checked ✓ |
| 2 | clHVACNordicAirCondition = 12 BCm* cold-climate AHU (efficiency/airflow/DX/fan/heater) | [CERT] | nordic_air_condition/ listing | token-checked ✓ |
| 3 | NordicGeneral 3 (linear char, man switch, summer/winter); RoomControl 2 (pre-ctrl clg/htg); EnergyMgmt 4 (DEGDAYS+TTB stats) | [CERT] | dir listings | token-checked ✓ |

**Marker tally**: [CERT] ×3 · [INFER] ×0. Block TYPE = EVIDENCE/CATALOG, COMPACT. All rows token-verified inline.

## Connections

- **[Block 87]** / **[Block 540]** — the clHVAC architecture + Eagle engine + representative sequences these
  complete.
- **[Block 542]** (honeywellFunctionBlocks) — the Nordic AHU blocks parallel Honeywell's DDC staging/setpoint
  blocks; different ecosystem, same building-control domain.

## Open gaps (this block)

- Individual Nordic sequence algorithms not decompiled (enumerated only) — covered-by-enumeration; open a child
  gap for a specific sequence (heat-recovery wheel, summer/winter changeover) only on demand.
