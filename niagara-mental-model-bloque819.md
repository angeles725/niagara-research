# B819 · Zero-demand / idle-state doctrine for staged processes — demand decides WHETHER, a process variable only modulates HOW MUCH; NaN never counts as demand; every staged process has an explicit idle state (answers "why can't the compressors turn off?") `[CERT]`

> **Scope**: the user's question — "when no room asks for cooling, why can't the compressors turn off?" — generalized
> into a build-kit doctrine. Establishes, with Tridium exemplars + our CompPan, the rule that DEMAND is a first-class
> gate (not inferred from a pressure/temperature), that an invalid/NaN input is never demand or a setpoint, and that
> every staged process declares an explicit idle state. Verdict on our CompressorControl: the demand GATE is already
> CORRECT (a live "won't turn off" is a wiring/demand-source issue, not the math), with two residues (NaN setpoint,
> no why-running surface). Deliverable: doctrine for `types/logic.md` + a "zero-demand" write-path matrix row + a lint.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT]) — `kitControl-rt` (docSource) `BLoopPoint`, `enums/BDisableAction`,
> `hvac/{BSequenceLinear,BSequence,BLeadLagRuntime}`. FUENTE 1 (own module, [CERT], deed38c) — `CompPan-rt/
> CompressorControl.java`, `BCompressorControl.java`. REMITTANCE: [B805] (RT control — BLoopPoint NaN-fault-hold, HOA
> §805.11), [B729]/[B730] (control timers), [B816] (write-path/overlap matrix). Cites grep-verified this session.

---

## 819.1 — The answer: demand must be a first-class GATE `[CERT — positive exemplar]`
"Why can't the compressors turn off with no demand?" — because a design that stages on a PROCESS VARIABLE (suction
pressure) alone will hold compressors on to defend that variable even when no room calls. The fix is a DEMAND GATE.
Our `CompressorControl.step` already does it right: FASE 2 (suction modulation) ends with **`if (demandCount <= 0)
target = 0;`** (the demand GATE — "no room calling → rack off"), and the FASE 1 fallback is `target = demandCount`;
suction pressure only MODULATES (±1 stage within the hold band), safety limits (`suctionLowLimit`/`dischargeHigh`)
always win. **So the step LOGIC turns off at zero demand in BOTH modes** — a live "the compressors won't turn off"
symptom is therefore a WIRING / demand-source problem (demandCount never reaching 0: a stuck room-call, a mis-summed
count), NOT the control math. CompressorControl is a POSITIVE exemplar of "demand decides WHETHER, pressure decides
HOW MUCH".

## 819.2 — Tridium's kitControl idle exemplars — strong for PID, weaker for the rest `[CERT]`
- **BLoopPoint = the strongest exemplar**: an explicit enable gate `loopEnable` (a `BStatusBoolean`, `:230`), a
  configurable idle OUTPUT via `disableAction` (`:105`; `BDisableAction` = {`maxValue`,`minValue`,`hold`,`zero`},
  `BDisableAction.java:24-27`, default `zero`), a NaN/Infinite input → fault + HOLD (never drive; [B805] §805.1
  REMITTANCE), and `propagateFlags` (`:185`) forwarding the enable/input status to the output so an observer sees WHY.
- **BSequenceLinear**: guards the stage-count recompute with **`if (getIn().getStatus().isValid())`** (`:198`) — an
  invalid input is NOT treated as demand (holds the last stage count); `BSequence.setOutputsOff()` (`:847-851`) is the
  all-off primitive, called from `started()` and on zero-demand.
- **BLeadLagRuntime**: `calculate()` (`:1009`) drives ALL outputs off at zero demand (`if(!currentIn) … stopOutput`,
  `:936`) — clean binary idle — BUT `currentIn = getIn().getValue()` (`:914`) has **NO `isValid()` guard**, so a
  faulted-but-stale-`true` boolean keeps a lead running (a NaN-as-demand GAP).
**Verdict**: Tridium's idle doctrine is PARTIAL and inconsistent — rigorous for a PID loop, workable for a binary
sequencer, and (BInterstartDelay) sometimes absent; NaN-refusal, idle-output, and enable-slot exposure vary object to
object. So the kit must STATE the doctrine, not assume the framework enforces it.

## 819.3 — CompPan residues (two real gaps) `[CERT]`
1. **NaN SETPOINT not guarded**: FASE 2 is entered on `if (suctionValid && c.suctionBand > 0d)` — it guards the suction
   READING and the band, but NOT that `suctionSetpoint` is a finite number. A NaN `suctionSetpoint` makes `hi`/`lo`
   NaN → every `suction > hi` / `< lo` is false → no up/down hold → `target = onCount` (silent HOLD/freeze of the
   modulation). The demand gate still saves the rack (0→off), but modulation is silently dead instead of an explicit
   FASE-1 fallback. (Doctrine point 3.) FIX: guard the setpoint (`Double.isFinite(c.suctionSetpoint)`) and fall back
   to FASE-1 demand-count with a visible flag.
2. **No "why running" surface**: CompPan counts demand correctly (a room call counts only when status is usable AND
   true, `BCompressorControl.java:1998`) but exposes NO operator-visible slot naming the demand SOURCES + the active
   limit — an operator cannot see "running: rooms 1,3 calling; suction 18 vs sp 20; LP floor active". (Doctrine
   point 4 unmet — a client residue + a kit rule.)

## 819.4 — The doctrine (for `types/logic.md`) `[INFER, grounded in 819.1-3]`
1. **Every staged process declares its IDLE state explicitly** (all-off / hold / pump-down) and the timers that gate
   reaching it (minOn/minOff/stageDelay). Use a `disableAction`-style choice ([BLoopPoint]) — don't leave idle implicit.
2. **Demand is a first-class INPUT** (calling rooms / an enable), never inferred from a process variable alone. A
   process variable (suction pressure, temperature) only MODULATES how much; demand decides WHETHER (the CompPan gate).
3. **NaN/invalid never counts as demand OR as a setpoint** — guard with `BStatus.isValid()` / `Double.isFinite` before
   using a value ([BSequenceLinear:198]); the fallback must be EXPLICIT and VISIBLE, not a silent hold.
4. **A "why running" surface**: a slot/tile that names the active demand source(s) + the governing limit, so an
   operator sees why the process is on. It must ALSO expose HELD demand that isn't a live call — a **fault-HELD** call
   (a faulted sensor holding the last demand under a `coolOnFault?true:prev` posture) and a **deadband-HELD** state
   (a room within the hysteresis band still counted) — otherwise the process runs on invisible demand. Concretely (the
   CompPan/ColdRoomPan residue): a per-room `sensorFault` + `callingReason`/`inDeadband` slot, so "running: rooms 1,3
   calling; room 2 HELD (sensor fault); suction 18 vs sp 20" is legible. A held demand that no one can see is the
   silent-runaway failure mode this doctrine exists to prevent.
5. **The operator's zero-demand OVERRIDE (HOA OFF lockout) and the automatic path reach the SAME idle state** — OFF
   dominates ([B805] §805.11); an OFF and a no-demand both end all-off.
6. **Both directions are gated** — reaching idle respects minOn (don't short-cycle on the way down), and leaving idle
   respects stageDelay (don't stage up all at once).

## 819.5 — Kit implication `[INFER]`
- PR15 doctrine lines (§819.4) into `types/logic.md` §"zero-demand / idle state", each `[ev: corpus B819]`.
- A **write-path/overlap matrix row class "zero demand"** ([B816] §816.6): `demand slot × dashboard/link × any timing →
  all stages reach the defined idle within minOn; NaN demand = no demand (never on)`.
- **Lint candidate** (statically decidable): a stage/on-off decision that reads a PROCESS VARIABLE (suction/temp/pressure
  field) to command capacity but has NO demand/enable input in scope — flag it (the "pressure without demand" shape).
  Advisory: a boolean/numeric demand input used without a `getStatus().isValid()` guard (NaN-as-demand risk).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | CompressorControl gates on demand: FASE 2 `if(demandCount<=0) target=0`, FASE 1 `target=demandCount`; pressure only modulates; safety limits win | [CERT] | CompressorControl.java (deed38c) FASE2 demand-gate + FASE1 fallback + suctionLow/dischargeHigh caps |
| 2 | BLoopPoint = explicit enable (loopEnable BStatusBoolean) + configurable idle output (disableAction {zero/min/max/hold}) + NaN→fault-hold + propagateFlags | [CERT] | BLoopPoint.java:105,185,230; BDisableAction.java:24-27; B805 §805.1 (NaN) |
| 3 | BSequenceLinear guards demand with isValid(); BSequence.setOutputsOff is the all-off primitive | [CERT] | BSequenceLinear.java:198; BSequence.java:847-851 |
| 4 | BLeadLagRuntime drives all-off at zero demand but has NO isValid guard (stale-true faulted bool keeps a lead running) | [CERT] | BLeadLagRuntime.java:914,936,1009 |
| 5 | CompPan residues: NaN suctionSetpoint not guarded (silent HOLD); no why-running surface | [CERT] | CompressorControl.java (FASE2 entry guards suctionValid+band, not setpoint); BCompressorControl.java:1998 |

**Tally**: 5 [CERT]. All kitControl + CompPan cites grep-verified this session. §819.4 doctrine + §819.5 kit are [INFER]
grounded in the [CERT] exemplars. Dedupe: BLoopPoint control mechanics + HOA precedence are REMITTANCE ([B805]); this
block adds the ZERO-DEMAND/IDLE synthesis + the two CompPan residues.

## Connections
- **[B805]** (RT control — BLoopPoint disabled behavior + §805.11 HOA OFF-lockout = doctrine point 5), **[B816]** (the
  "zero-demand" write-path matrix row + the overlap discipline), **[B729]/[B730]** (the minOn/minOff/stageDelay timers
  that gate idle), **[B810]** (writable fallback — the idle output for a proxy). Kit: `types/logic.md` §"zero-demand /
  idle state" + the lint. CLIENT residue: CompPan NaN-setpoint guard + a why-running surface; the wiring/demand-source
  trace (why demandCount never reaches 0) is the parallel shard's.

## Open gaps
- **B819-G1** (bounded): the "pressure without a demand input" lint — precisely what static shape distinguishes a
  modulator (legit: pressure refines an already-demanded stage) from a driver (bug: pressure alone commands capacity);
  needs a data-flow rule, so it starts advisory.
