# Block 547 — kitControl enums and constants reference: the 16 packaged enum types (incl. the BReliability sensor-fault vocabulary) and the four constant-source blocks

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC11 — packaged enum/constant tables)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read (narrow reference gap — no sub-agent).
**Primary sources** `[CERT]`: `organized/kitControl/kitControl-rt/vineflower/com/tridium/kitControl/enums/*.java`
(16 files), `.../constants/*.java` (4 files).

**Scope**: the enumeration TYPES the kitControl blocks reference (the "range" tables a grep of the block code
only indexes, per the project protocol — enums live in the type, not the logic) and the four constant-source
blocks. COMPACT reference block. The behavior-carrying enums (`BDisableAction`, `BLoopAction`) are already
decompiled in their blocks ([Block 539]); this catalogs the full set for lookup.

---

## 547.1 The 16 kitControl enum types [CERT]

`[CERT] .../enums/` — each with its `@Range` tags:

| Enum | Range (ordinals in order) | Used by |
|------|---------------------------|---------|
| `BLoopAction` | direct, reverse | BLoopPoint ([B539]) |
| `BDisableAction` | maxValue, minValue, hold, zero | BLoopPoint disable ([B539]/[B543]) |
| `BReliability` | noFaultDetected, noSensor, overRange, underRange, openLoop, shortedLoop, noOutputValue, unreliableOther, processError | point reliability (SAFETY — see §547.2) |
| `BNightPurgeMode` | disabled, inputError, lowTemperature, freeCooling, noFreeCooling, satisfied | BNightPurge ([B537]) |
| `BOutsideAirOptimizationMode` | disabled, inputError, lowTemperature, freeCooling, noFreeCooling | BOutsideAirOptimization ([B537]) |
| `BRaiseLowerFunction` | offState, staticState, lowerState, raiseState, resetRaiseState, resetLowerState | BRaiseLower |
| `BNullValueOverrideSelect` | useInValue, specifyOutValue | BNullValueOverrideSelect block |
| `BResetLimitsExceededMode` | useExceededLimit, setStatusToNull | BReset |
| `BAlarmCountEnum` | unackedAlarmCount, openAlarmCount, inAlarmCount, totalAlarmCount, anyCount | BAlarmCountToRelay |
| `BOffHeatCool` | off, heat, cool | HVAC mode blocks |
| `BTwoSpeed` | off, slow, fast | fan/stage blocks |
| `BStopRun` | stop, run | equipment command |
| `BSecure` | access, secure | door/security |
| `BOccupied` | unoccupied, occupied | occupancy |
| `BOffOn` | off, on | binary command |
| `BEnglishMetric` | english, metric | BPsychrometric unit select |

## 547.2 `BReliability` — the sensor-fault vocabulary (ties [Block 543]) [CERT]

The most safety-relevant enum. `BReliability` `[CERT]` enumerates 9 point reliability states, including the
explicit SENSOR-FAILURE reasons: `noSensor`, `overRange`, `underRange`, `openLoop`, `shortedLoop`,
`noOutputValue`, `processError`. This is the BACnet-style Reliability property model — a richer failure
taxonomy than a bare fault bit. It is the vocabulary a point CAN carry to describe HOW a sensor failed, which
[Block 543]'s KC13 analysis showed is often NOT propagated by default (`propagateFlags`) — the enum EXISTS to
express the failure reason, but the loop must be configured to surface it. `BResetLimitsExceededMode`
(`useExceededLimit` vs `setStatusToNull`) and `BNullValueOverrideSelect` (`useInValue` vs `specifyOutValue`)
are the two other explicit null/limit-handling choices, both bearing on the fail-safe posture.

## 547.3 The constant-source blocks [CERT]

`.../constants/` — `BNumericConst`, `BBooleanConst`, `BEnumConst`, `BStringConst`. Each is a trivial source
block: an `out` property of the matching status type plus `facets`, with no inputs `[CERT] BNumericConst.java:40-41`.
They inject a fixed value into a wire-sheet (the constant leg of a control expression) — the counterpart to a
linked input. No logic; the value is the configured `out`.

## 547.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | 16 kitControl enum types with the listed ranges | [CERT] | enums/*.java @Range | token-checked ✓ |
| 2 | BReliability = 9-state sensor-fault vocabulary (noSensor/overRange/openLoop/shortedLoop…) | [CERT] | enums/BReliability.java | token-checked ✓ |
| 3 | BResetLimitsExceededMode + BNullValueOverrideSelect = null/limit handling choices | [CERT] | enums/*.java | token-checked ✓ |
| 4 | 4 constant blocks (Numeric/Boolean/Enum/String), out+facets only, no inputs | [CERT] | constants/BNumericConst.java:40-41 | token-checked ✓ |

**Marker tally**: [CERT] ×4 · [INFER] ×0. Block TYPE = EVIDENCE/REFERENCE, COMPACT. All rows token-verified
inline (this is a lookup table).

## Connections

- **[Block 539]** — BLoopAction/BDisableAction behavior (this catalogs them).
- **[Block 537]** — BNightPurgeMode/BOutsideAirOptimizationMode (the HVAC-energy block modes).
- **[Block 543]** (KC13) — BReliability is the sensor-fault vocabulary the safety analysis referenced;
  §547.2 shows the enum exists but requires `propagateFlags` to surface.

## Open gaps (this block)

- None. This is a complete enum/constant reference for kitControl.
