# B733 · Modulating (0-10V) outputs and math/PID models in Niagara — the writable→AO chain, `kitControl.BLoopPoint`, the math block family, and where they'd fit our refrigeration modules

> **Scope**: what Tridium ships for (a) driving a modulating analog output (0-10V) and (b) mathematical
> models / control math, and — as an APPLICATION note — where these would fit the PANCCADIA León modules,
> which today drive only ON/OFF outputs. Framework mechanics are cited to existing blocks (NRIO/IO,
> kitControl); the NEW content is the applicability verdict for our plant. Automatic campaign continuation.
>
> **Sources**: FUENTE 3 docSource `extracted/control-rt/javax/baja/control/BNumericWritable.java`,
> `extracted/kitControl-rt/com/tridium/kitControl/{BLoopPoint,BLoopAlarmAlgorithm,enums/BLoopAction}.java`,
> `extracted/kitControl-rt/com/tridium/kitControl/math/*` (read this session). FUENTE 1: NRIO/IO in
> B27/B28/B104/B105/B117/B279/B301/B692/B702; kitControl math/PID in B15/B39/B40/B106/B116/B117/B237; our
> rt modules (all boolean outputs).

---

## 733.1 — The 0-10V / analog-output chain `[CERT]`

Niagara does not "write 0-10V" from control logic directly; it uses a chain:

1. **Control logic computes a numeric command** — a `double`/`BStatusNumeric` in engineering units (0-100 %,
   RPM, kPa…).
2. **`BNumericWritable`** (control-rt) — the writable point: *"a writable control point with 16 input
   levels"* (`in1..in16` = the BACnet-style **priority array**), plus `fallback` and override expiration
   (`BNumericWritable.java:30,42-…`). Control sources write a level; the highest-priority active level wins;
   `fallback` covers "all null." This is the correct home for a commanded modulating value.
3. **BLink → an analog-output PROXY POINT** in the driver tree — NRIO (`nrio` module,
   `nrio-rt/com/tridium/nrio/points/BNrio*ProxyExt`), BACnet AO, or Modbus. The proxy point owns the field
   binding.
4. **Conversion/scale** on the proxy (units + a linear/facets conversion) maps engineering units → raw
   0-10V / DAC counts at the terminal. (The `nrioConversion` module is a migration converter, not the live
   scaler; live scaling lives in the point's conversion facets.)

So a modulating output = `BNumericWritable` (logic side) linked to an AO proxy point (driver side) with a
scale. NRIO/IO mechanics are already in B27/B105/etc.

## 733.2 — Control math & models `[CERT]`

- **PID loop — do NOT hand-roll it**: `kitControl.BLoopPoint` is the ready-made loop — `loopAction`
  (`BLoopAction` direct/reverse), proportional/integral/derivative gain constants + tuning facets, and a
  `BLoopAlarmAlgorithm`. PV in → CV out (feed a `BNumericWritable` → AO). This is THE modulating-control
  primitive.
- **Math function blocks**: `kitControl/math/` — `BAdd, BSubtract, BMultiply, BDivide, BAverage, BMinimum,
  BMaximum, BAbsValue, BPower, BSquareRoot, BExponential, BLogNatural, BLogBase10, BSine/BCosine/BArc*,
  BModulus, BNegative, BFactorial, BQuadMath` — compose arithmetic on the wire sheet (each is the
  template-method pattern of B730 §730.7: status handled in the base, `calculate()` the pure math).
- **HVAC model**: `kitControl/hvac/Psychrometric` — enthalpy/dew-point from temp+humidity (pure static,
  primitives → unit-testable), used by `BOutsideAirOptimization`. `BSequenceLinear` for linear sequencing.

## 733.3 — Applicability to our modules `[CERT/INFER]`

**Today every output we drive is ON/OFF** `[CERT]`: ColdRoomPan `valveOut`/`evapOut`/`resistanceOut`
(solenoid/fan/resistance booleans); CompPan compressor staging + `condenser1/2/3` (booleans). No
`BNumericWritable`, no 0-10V anywhere. That matches the plant's on/off devices (liquid solenoids, fixed-speed
fans, on/off compressors) — so the absence is correct, not a gap.

The **0-10V candidates** if modulating hardware is added `[INFER, domain-standard]`:
1. **Condenser-fan head-pressure control** — the classic 0-10V case. CompPan stages condensers on/off today;
   a modulating (EC/VFD) condenser fan holding discharge pressure would be `BLoopPoint` (PV=`dischargePressure`,
   SP=head setpoint, reverse action) → `BNumericWritable` → AO proxy. Pairs naturally with the existing
   floating-suction model.
2. **EEV / modulating valve superheat** (evaporator) — `BLoopPoint` (PV=superheat) → AO. Not present.
3. **Variable-speed / EC evaporator fan** — `evapOut` boolean → a 0-100 % command.

**Math models we already have**: CompPan's R404A dew-point table + floating-suction setpoint is a hand-coded
model (verified against the operator's table; `floatingSetpoint` NaN → fallback). For a FIXED curve like
that, a hand-coded Java table is fine and clearer than wiring kitControl math blocks. But for a **modulating
loop, use `BLoopPoint`, never a hand-rolled PID** — you get tuning, direct/reverse, and loop alarms for free.

**Verdict**: no change needed for the current on/off plant. IF a modulating device is specified (most likely
a condenser fan for head-pressure control), the idiomatic build is: add a `BNumericStatus`/`double` command
output on our component (or compute it in a `BLoopPoint` on the wire sheet) → `BNumericWritable` → AO proxy
point + scale. Keep the loop math in `BLoopPoint`, keep fixed curves as hand-coded tables.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BNumericWritable = writable point with 16 input levels (priority array) + fallback | [CERT] | BNumericWritable.java:30,42-… |
| 2 | A modulating output is BNumericWritable → AO proxy point (NRIO/BACnet/Modbus) + scale; logic doesn't write 0-10V directly | [CERT/INFER] | §733.1; NRIO points B27/B105; nrio-rt BNrio*ProxyExt |
| 3 | kitControl.BLoopPoint is the ready-made PID (loopAction direct/reverse, P/I/D, loop alarm) | [CERT] | BLoopPoint.java + BLoopAction + BLoopAlarmAlgorithm |
| 4 | kitControl/math has a full arithmetic block family; hvac has Psychrometric (pure model) | [CERT] | math/ listing; hvac/Psychrometric.java |
| 5 | Our modules drive only ON/OFF outputs today; no BNumericWritable/0-10V | [CERT] | our rt source (valveOut/evapOut/resistanceOut/condenser1-3 all boolean) |
| 6 | 0-10V candidates for us = condenser-fan head-pressure, EEV superheat, EC evap fan | [INFER] | domain-standard refrigeration modulation; matches our on/off gaps |

**Tally**: 4 [CERT], 2 [CERT/INFER-mixed]. No unmarked claims. Applicability items are design/feature notes,
gated on modulating hardware actually being specified.

## Connections
- **B730** §730.7 (math template-method pattern), **B731** (our-module backlog — this adds the modulating
  option), **B732** (BLoopPoint would also want a `BLoopAlarmAlgorithm`).
- NRIO/IO: B27/B28/B104/B105/B117/B279/B301/B692/B702. kitControl math/PID: B15/B39/B40/B106/B116/B117/B237.

## Open gaps
- **B733-G1**: `BLoopPoint` tuning internals (integral windup, sample time, the exact loop equation) — not
  opened here; relevant only when a modulating loop is actually built.
- **B733-G2**: the NRIO AO point's conversion/scaling facets (raw counts ↔ 0-10V ↔ engineering units) exact
  mechanics — cite B105/B27; deep-dive deferred until an AO is wired.
