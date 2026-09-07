# Block 836 — Spyder Tool function-block reference: the wiresheet building blocks for a Spyder control application (B835-G3)

> **Focus:** spyder-commissioning. **Gap:** B835-G3 — what function blocks you drop on the Spyder Tool
> wiresheet (§6 step 6 of B835) to build the control logic, by category, with the key ones and their
> gotchas. Applies to BACnet MS/TP and LON Spyder alike (same Sequenced Control Engine).
>
> **Sources:** FUENTE 3 — `organized/honeywellFunctionBlocks/honeywellFunctionBlocks-rt/.../META-INF/module.xml`
> (registered `fbs.*` type packages). FUENTE 2 — `niagara-help/guides-clean/HoneywellFunctionBlocks/*`
> (per-block guides) + `HoneywellSpyder/honeywellSpyderTool-*` (wiresheet/wiring/alarms). FUENTE 1 —
> B40 §40.2 (`spyderApps Ver28` macro library) + B14 (templates).

---

## 1. Categories `[CERT]` (module.xml `fbs.*` sub-packages)

The `honeywellFunctionBlocks-rt` module registers its blocks under these packages
(`fbs.analog · fbs.builtin · fbs.control · fbs.datafunction · fbs.enums · fbs.io · fbs.logic · fbs.math ·
fbs.zonecontrol`) `[CERT]`. Practical grouping:

| Category | Purpose | Representative blocks |
|----------|---------|-----------------------|
| **Control** (`fbs.control`) | closed-loop + staging | PID, AIA, FlowControl, Stager, StageDriver, Cycler, RateLimit |
| **Zone/Temperature** (`fbs.zonecontrol`) | HVAC mode/occupancy/setpoint arbitration | SetTemperatureMode, OccupancyArbitrator, GeneralSetpointCalculator, TemperatureSetpointCalculator |
| **Logic** (`fbs.logic`) | boolean | And, Or, Xor, OneShot |
| **Math/Analog** (`fbs.math`/`fbs.analog`) | arithmetic, compare, select, psychrometrics | Add/Sub/Mul/Div, Limit, Average, Min/Max, Compare, Select, Switch, HystereticRelay, Enthalpy, FlowVelocity |
| **Data/Utility** (`fbs.datafunction`) | counters/overrides/runtime | Counter, Override, RuntimeAccumulate, PassThru |
| **I/O** (`fbs.io`) | physical points | UI/AO/DI/DO/triac/relay, FixedSylkInput/Output |

## 2. Key blocks `[CERT-doc]` (per-block guides)

- **PID** (`-Pid.txt`): P-I-D loop. `Out% = Bias + Kp·Err + Kp/Ti·∫ + Kp·Td·dErr/dt`, range −200..+200% or
  0–100%; inputs sensor/setPt/tr(throttling range)/intgTime/dervTime/deadBand; direct or reverse action.
- **AIA** (`-Aia.txt`): Adaptive Integral Action — use instead of PID when process lag causes integral
  wind-up; increment keyed on throttling range + `maxAOChange` (%/s).
- **FlowControl** (`-FlowControl.txt`): VAV damper flow controller (2nd half of a pressure-independent VAV
  cascade). In: commanded-flow % (from PID), sensed flow, min/max flow, duct area → `EFF_FLOW_SETPT` +
  `DAMPER_POS`; falls back to pressure-dependent if the flow sensor fails.
- **Stager** (`-Stager.txt`): turns a 0–100% (PID output) into a **count of stages ON**, with min-on/off
  timers and CPH cycling → feeds StageDriver.
- **StageDriver** (`-StageDriver.txt`): energizes individual stage outputs from that count using **FILO /
  FOFO / runtime-equalize (LL_RUNEQ)** lead-lag; keeps nonvolatile per-stage runtime.
- **SetTemperatureMode** (`-SetTemperatureMode.txt`): arbitrates effective mode (COOL/REHEAT/HEAT/
  EMERG_HEAT/OFF) from system switch, network mode, supply/space temps, setpoints; `controlType` 0=CVAHU,
  1=VAV.
- **OccupancyArbitrator** (`-OccupancyArbitrator.txt`): effective occupancy (Occupied/Unoccupied/Bypass/
  Standby) from schedule + wall-module override + network command + sensor.
- **General/TemperatureSetpointCalculator**: setpoint from occupancy state + reset input (temperature-
  specific variant for zones).

## 3. Wiring blocks to I/O and network `[CERT-doc]` (honeywellSpyderTool-WireSheet / -WiringDiagram)

- The wiresheet's three building blocks are **Function blocks + Physical points + NVs/Objects**.
- **Physical points** are dragged onto the sheet; the **Terminal Assignment View** maps each to a pin
  (Spyder II/BACnet: UI 1–6, DI 1–4, AO 1–3, DO 1–8; Micro: UI 1–4, AO 1–2, DO 1–4). Sylk I/O expanders
  (SIO6042/SIO4022/SIO12000) add points on relay-equipped models (`FixedSylkInput`/`FixedSylkOutput`).
  (Physical terminal NUMBERS per model → B835 §3.)
- **Network variables:** LON Spyder exposes NVI/NVO/NCI + Many-to-One and `Fixed_Droppable`/`Custom` NVs;
  BACnet Spyder uses BACnet Objects for the same role.

## 4. Gotchas `[CERT-doc]`

1. **Stop + Start the Sequenced Control Engine after certain config changes** — verbatim in the PID,
   FlowControl and AIA guides ("if users expect behavior like Spyder controller … Stop and start the
   Sequenced Control Engine"). Do it via right-click Sequenced Control Program → stop → start.
2. **PID forces Out=0** if any of sensor / setPt / tr / intgTime is unconnected or invalid.
3. **StageDriver wiresheet cap:** only 95 stages visible; beyond that use "Show Stages" / Link Editor.
4. **`Out Save`** per block retains the last output across power cycles; if off, output reverts on restart.
5. **Control Execution Alarm** fires if the engine's full cycle exceeds ~1 s → the app is too heavy.
6. This is the **N4-hosted `honeywellFunctionBlocks-rt`** module (runs inside Niagara's Sequenced Control
   Engine), not the legacy on-controller firmware blocks.

## 5. The `spyderApps Ver28` macro layer `[CERT]` (B40 §40.2)

On top of the primitive blocks, Honeywell ships **108 UserDefined macro blocks** in 19 categories (Alarms,
CVAHU, Control, Decode, Econo, General, Logic, Math, Metering, Psych, Sched, Time, Tstat, UnitVent,
UnitsConv, VAV_AHU, WallModConv, ZoneTerminal) — e.g. `PID_Enhanced`, `CascadeControl_RevAct/DirAct`,
`EconoLogicUnivAP_C7400`, `WetBulb_F/C`, `Interpolation_11Pts/22Pts`. This is the known input set of the
`spyderToIrmNxMigrator` (B25.4). Ready-made VAV apps also come as **Spyder Model 5/7 templates** (B14).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | rt module registers blocks under fbs.control/logic/math/analog/zonecontrol/datafunction/io/builtin | [CERT] | honeywellFunctionBlocks-rt module.xml |
| 2 | PID formula/inputs; direct/reverse; 0-forcing on unconnected inputs | [CERT-doc] | -Pid.txt |
| 3 | AIA = adaptive integral vs wind-up; FlowControl = VAV damper cascade w/ pressure-dep fallback | [CERT-doc] | -Aia.txt, -FlowControl.txt |
| 4 | Stager→count, StageDriver energizes stages w/ FILO/FOFO/LL_RUNEQ + runtime; 95-stage view cap | [CERT-doc] | -Stager.txt, -StageDriver.txt |
| 5 | SetTemperatureMode arbitration (CVAHU=0/VAV=1); OccupancyArbitrator states | [CERT-doc] | -SetTemperatureMode.txt, -OccupancyArbitrator.txt |
| 6 | Wiresheet = FBs + Physical points + NVs/Objects; Terminal Assignment View; Sylk I/O expanders | [CERT-doc] | honeywellSpyderTool-WireSheet/-WiringDiagram |
| 7 | "Stop/Start Sequenced Control Engine" note in PID/FlowControl/AIA guides | [CERT-doc] | -Pid/-FlowControl/-Aia (also seen in niagara_help find) |
| 8 | Out Save persistence; Control Execution Alarm >1 s | [CERT-doc] | index.txt, -AlarmsView.txt |
| 9 | spyderApps Ver28 = 108 macros / 19 categories; migrator input set | [CERT] | B40 §40.2 |

**Tally:** 9 claims — 2 [CERT] (module.xml, corpus B40), 7 [CERT-doc]. No unmarked assertions. Block/enum
package NAMES confirmed in module.xml; per-block behavior from the guides.

## Connections
- **B835** — the commissioning block; this is its step-6 "build the app" detail. Physical terminal numbers → B835 §3.
- **B14** — Spyder Model 5/7 VAV templates (ready-made apps).
- **B25.4 / B40** — the `spyderToIrmNxMigrator` and its `spyderApps Ver28` input set.

## Open gaps
- **B836-G1** — Per-block property/slot detail for the top-10 blocks (throttling range units, CPH limits,
  lead-lag tie-break) if an app needs exact tuning — from the guides' property tables.
