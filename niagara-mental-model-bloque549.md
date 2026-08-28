# Block 549 — kitControl focus SYNTHESIS: the four control-programming ecosystems of N4, the writable-point spine, the programming rules, and the HVAC control-safety verdict

**Session**: 2026-08-28
**Focus**: `kitControl` (focus-closing synthesis — 13 blocks B536–B548 + B543)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: SYNTHESIS over the focus's own cited blocks (no new decompilation). Cross-references only.

**Scope**: consolidates the `kitControl` focus, which answered the operator's request — documentation about
kitControl / control modules / control logic, how control modules are programmed, the programming RULES, HVAC
control, the Java-8 question, and control-logic SAFETY. This is the terminal artifact at focus level ([Block 8]
§8), not a corpus-level stop.

---

## 549.1 The headline — N4 has FOUR parallel control-programming ecosystems [CERT-synthesis]

The single most unifying finding of the focus: a Niagara N4 station hosts **four independent control-block
ecosystems**, each with its own base class and EXECUTION MODEL:

| Ecosystem | FB base class | Executes on | Model | Blocks |
|-----------|---------------|-------------|-------|--------|
| **kitControl** (Tridium native) | `BControlPoint` | Niagara JVM | EVENT-driven (link propagation) | [B536][B537][B539] |
| **clHVAC** (Centraline/Eagle) | `BControlFunctionSupport` | Niagara JVM | ROSTER (`BControlProgramService`) | [B540][B548] |
| **honeywellFunctionBlocks** (Honeywell DDC) | `BFunctionBlock` | Niagara JVM | SCAN (Sequenced Control Engine, `iterationInterval`) | [B542] |
| **honIrmControl** (BEATS/IRM Nano) | `BNanoFunctionBlock` | **physical IRM device** | SCAN on HARDWARE (NanoCmd download) | [B546] |

Three run control math IN the station JVM (differing only in HOW they're driven — on change, on a roster, on a
fixed scan); the fourth (IRM Nano) is a proxy layer — Niagara compiles and downloads a binary program and the
control runs on the embedded controller. They share NO code (`honIrmControl` imports none of the others) — an
engineer choosing a control library is choosing an ecosystem, not a palette within one.

## 549.2 The writable-point spine [CERT-synthesis]

All of it ultimately writes to a **writable control point** ([Block 536] KC1): `BControlPoint` +
`WritableSupport`'s **16-level priority array**, scanned 1→16 first-valid-wins, relinquish = null-status,
`fallback` (ordinal 17) as the never-null default; levels 1 (emergency) and 8 (manual) are `READONLY`-persisted
and alone raise `OVERRIDDEN`. The RULES for wiring into it ([Block 538] KC3): one link per level, levels 1/8
(and Boolean 6) are unlinkable/action-only, schedule→In16 by convention. The WRITE PATH out to the device
([Block 544] KC8): link→InN→re-arbitrate→proxyExt→Tuning→driver — and the key transport fact: **the 16-level
array survives 1:1 to a BACnet object's priority array (N4 level → WriteProperty Priority), but COLLAPSES to a
single value for Modbus and every other register driver.** So "priority array" is a BACnet-native concept that
N4 emulates internally for everyone else.

## 549.3 How control modules are programmed [CERT-synthesis]

- **Compose, don't code** ([Block 537] KC2): ~130 kitControl function blocks (math/logic/timer/latch/mux/PID/
  HVAC/energy) wired on the wire sheet; multi-input blocks skip null inputs (all-null→null out).
- **The rules** ([Block 538] KC3): links are directional/owned-by-target, conversion links auto-insert on type
  mismatch, execution is event-driven with NO topological-order guarantee on a standard sheet (only the ACE
  edge-controller sheet has explicit Level/Order); composites reuse the link/knob machinery ([Block 545] KC9 —
  each exposed slot IS a link).
- **The loop** ([Block 539] KC4): `BLoopPoint` PID — gain-based, event-driven, with ramp anti-windup and a
  `disableAction`; official tuning says kP=(range/throttling-band), PI recommended, PID seldom used.
- **The escape hatch** ([Block 541] KC6): the `program` module — freeform **Java 8** (bytecode class-major 52,
  operator-confirmed) compiled by the bundled `javac`, source+bytecode stored in the `.bog`, sandboxed
  (untrusted-domain perms, `program.requireSigning`, superUser-only edit). The docs recommend `Expr` (BQL) over
  Program for simple logic.

## 549.4 HVAC control [CERT-synthesis]

Three of the four ecosystems ARE HVAC libraries: **clHVAC** ([Block 540]) with real Centraline sequences
(weather-compensated heating curve, AHU economizer-shutoff mixing damper, 12-chiller runtime-equalized lead-lag,
degree-days); **honeywellFunctionBlocks** ([Block 542]) with DDC BPid/BStager/BStageDriver + zone setpoint
recovery; **honIrmControl** ([Block 546]) with the same DDC blocks running on the IRM device. Plus kitControl's
own HVAC/energy blocks (BOptimizedStartStop, BNightPurge, BPsychrometric, BLeadLag…, [Block 537]). The Nordic/
room/energy micro-modules ([Block 548]) complete the clHVAC family.

## 549.5 The control-SAFETY verdict (operator-requested) [CERT-synthesis]

[Block 543] (KC13) is the operator's key deliverable: HVAC control has FIVE defensive layers and is SAFE by
default in several (999 sensor-absent sentinel, `disableAction=zero`, output clamp, frost protection at 16 °C,
never-null fallback, emergency level 1) — but has **six default-UNSAFE gaps** the engineer must close:
`disableAction=hold` freezes the last command; `propagateFlags=0` hides a bad sensor; `BLoopAlarmAlgorithm` is
alarm-only (no interlock); the `999` sentinel is a convention not a guarantee; `rampTime=0` gives no anti-slam;
and clHVAC strips the Baja status envelope. The vocabulary to describe a sensor fault EXISTS (`BReliability`:
noSensor/overRange/openLoop/shortedLoop, [Block 547]) but must be surfaced via `propagateFlags`. The residual
requires-execution work (KC13-G1) is a LIVE-station audit of which real loops use those unsafe configs.

## 549.6 Coverage + what's left

13/13 investigable gaps closed (KC1–KC13); 1 requires-execution deferred (KC13-G1, needs a live station +
operator authorization). Refinements issued to prior blocks: [Block 536] (Boolean In6 unlinkable),
[Block 87] §87.3 ([CERT-a]→[CERT], counts clarified). Count clarifications: [Block 103] (158=rt+ux+wb),
[Block 105] (203=vf, 140 factory FBs). Java-8 confirmed by bytecode.

## 549.7 Self-verify

| # | Claim | Marker | Basis | Verdict |
|---|-------|--------|-------|---------|
| 1 | Four independent control ecosystems, distinct base class + execution model | [CERT-synthesis] | B536/B540/B542/B546 | cross-ref ✓ |
| 2 | Writable-point 16-level spine; BACnet preserves priority, Modbus collapses | [CERT-synthesis] | B536/B538/B544 | cross-ref ✓ |
| 3 | Program module = Java 8, sandboxed; Expr preferred for simple logic | [CERT-synthesis] | B541/B538 | cross-ref ✓ |
| 4 | HVAC across clHVAC/honeywell/IRM + kitControl HVAC blocks | [CERT-synthesis] | B540/B542/B546/B537 | cross-ref ✓ |
| 5 | Safety: 5 layers, 7 safe-by-default, 6 unsafe-unless-configured gaps | [CERT-synthesis] | B543/B547 | cross-ref ✓ |

**Marker tally**: [CERT-synthesis] ×5 (each traces to cited evidence blocks) · [INFER] ×0. Block TYPE =
SYNTHESIS (focus-closing) — it introduces no new claims, only consolidates cited ones. This is the correct
high-ratio synthesis case ([Block 8] §8).

## Connections

- Consolidates: [B536][B537][B538][B539][B540][B541][B542][B543][B544][B545][B546][B547][B548] (the whole
  focus).
- Cross-focus: `signing-pki`/`security-audit` (program signing + control-safety companion), `modbus`/BACnet
  (write path), `workbench` (wire sheet/composite editor UI), [Block 88] (IRM Nano protocol).

## Open gaps (focus level)

- **KC13-G1** (requires-execution): live station-wide safety-config audit. Deferred — needs a running station
  and operator authorization (§12 dynamic phase).
- No read-only-investigable gaps remain in this focus.
