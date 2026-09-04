# B730 · Tridium `-rt` component-authoring idioms to adopt — execution/commit, `changed()` discipline, status/fault, timers, slot flags, actions, facets, and off-thread IO

> **Scope**: the reusable authoring idioms Tridium uses INSIDE its own `-rt` BComponents (control, kitControl,
> driver), distilled into an adopt-list for a custom control module (rooms / evaporators / compressors /
> defrost). This is the "how to write the logic well" companion to B4 (the slot-system MECHANICS) and B729
> (the timer LIFECYCLE contract). Pairs with the kit doc `build-n4-module-kit` (actionable checklist).
>
> **Sources**:
> - FUENTE 3 (Tridium docSource real-javadoc + source, `organized/docSource/docSource-doc/extracted/<mod>-rt/…`): control-rt (`BControlPoint`, `BNumericWritable`), kitControl-rt (`BQuadMath`, `BAverage`, `BRaiseLower`, `BSequence`, `BLeadLagRuntime`, `BSlidingWindowDemandCalc`, `BOutsideAirOptimization`, `BOptimizedStartStop`, `BSwitch`), driver-rt (`BProxyExt`, `BPollScheduler`). Gathered by a delegated survey sweep → marked `[CERT-a]`; items I read directly this session (BControlPoint contract excerpts, BComponent lifecycle in B729) are `[CERT]`.
> - FUENTE 1 (corpus): B4 (Baja Object Model — slot taxonomy/flags), B729 (timer lifecycle), module-anatomy focus (B629-639, slotomatic/@NiagaraType).

---

## 730.1 — Execution & commit (the re-entrancy-safe output path) `[CERT-a]`

The single most important shape, from `BControlPoint`:
- **Never write `out` directly — mutate the passed `BStatusValue` and let the framework diff+commit.**
  `onExecute(BStatusValue out, Context cx)` mutates the parameter; the base commits only if it changed
  (`BControlPoint.java:307-310`, `if(!out.equivalent(working)) out.copyFrom(...)`; contract note :318-324
  "Never modify the out property directly"). Writing the slot from inside execution re-enters `changed()`
  and can loop.
- **Commit only on real change** → stops feedback storms / subscriber churn on the engine thread.
- **Per-execute timing** is free via `Clock.nanoTicks()` metering (`BControlPoint.java:290,313-315`).

For a room/evaporator/compressor: model outputs as `BStatusNumeric`/`BStatusBoolean`, drive them from one
`execute()`-style method, and commit only on change.

## 730.2 — `changed()` discipline `[CERT-a]`

The canonical shape (`BRaiseLower.changed:596-660`, `BSequence.changed:761-774`, `BLeadLagRuntime.changed:920-943`):
1. `super.changed(p,cx);`
2. **`if(!isRunning()) return;`** — no work during load/decode.
3. **Dispatch on WHICH slot changed** (`if(p==room1Calling || …) execute();`) — each property handled once,
   no blanket recompute.
4. **Feedback-loop / significance guard** before writing a slot back: `BRaiseLower` applies a **deadband**
   (`:621-644`, `if(Math.abs(last-inValue) > HALF*deadBand)`) so a change→setSlot→change cycle can't chatter
   a relay. This is the single best defense against a defrost/compressor relay oscillating through a link.

## 730.3 — Status / fault / null propagation `[CERT-a]`

Honest degradation, everywhere in math/hvac/energy:
- **Check `getStatus().isValid()` before trusting a value**; if too few valid inputs, force the output
  null/fault: `BQuadMath.onExecute:180-231` (`if(nonNullCount<minInputs()) out.setStatus(BStatus.nullStatus)`),
  `BAverage.calculate:43-61`, `BOutsideAirOptimization.doCalculate:612-621`.
- **Propagate aggregated input status to the output**: `out.setStatus(propagate(BStatus.make(a|b|c|d)))`
  (`BQuadMath.java:229`) → fault/stale/down flow through the graph automatically.
- **Operator-selectable propagation** via a `propagateFlags` slot (base `BKitNumeric`), handled in `changed()`
  (`BRaiseLower.java:600-608`).
- **Bulk fault-mark all outputs** in one helper on sensor loss (`BSlidingWindowDemandCalc.setDataFaultStatus:880-890`).

For refrigeration: a bad probe → output fault, NOT a false "warm"/"cold" reading that trips a bogus alarm.

## 730.4 — Timers (see B729 for the lifecycle contract) `[CERT-a]`

- **Periodic**: `Clock.schedulePeriodically`, ticket cancelled+rescheduled in `started()`, cancelled in
  `stopped()` (`BSlidingWindowDemandCalc:810-826`, field `Clock.Ticket ticket=null`).
- **One-shot delay**: `Clock.schedule(this, relTime, action, null)` where the fired action is a
  **`HIDDEN|ASYNC` `@NiagaraAction`** (`BSequence:157-164` decl, `:860-888` schedule, `:808-822` `doXxx`
  handler) → the callback runs as an audited async action off the setter's stack.
- **Cancel-before-reschedule** (`BSequence.startOnDelayTimer:860-873` cancels the opposite ticket AND its
  own) → no overlap/double-fire on reconfigure.
- **`stopped()` teardown**: cancel every ticket, null the reference (`BRaiseLower.stopped:586-590 → :959-963`).
- **First calc in `atSteadyState()`** so you don't command compressors/defrost off partial boot data
  (`BOutsideAirOptimization.atSteadyState:607-610`, `BLeadLagRuntime:912-916`). See B729 for why `started()`
  is ALSO required (late-mount).

## 730.5 — Slot flags: measured frequency + intent `[CERT-a]`

Counts across kitControl-rt + control-rt: `SUMMARY` 634 · `TRANSIENT` 616 · `READONLY` 280 · `OPERATOR` 198 ·
`HIDDEN` 79 · `DEFAULT_ON_CLONE` 22 · `ASYNC` 18 · `CONFIRM_REQUIRED` 4 · `NON_CRITICAL` 2 · `FAN_IN` 2.

| Flag | Intent | Example |
|---|---|---|
| `TRANSIENT` | runtime state, NOT persisted (recomputed in started()) | every live output/timer-state slot |
| `READONLY` | computed output — links/UI read, can't write; pair with TRANSIENT for live outputs | `BSequence:298,551` |
| `SUMMARY` | show on property-sheet summary / wire-sheet pin | important inputs/outputs |
| `OPERATOR` | editable at operator permission (tunable by non-admin) | setpoints/delays |
| `HIDDEN` | internal actions (timer-expiry) + unused variable-arity slots | `BSequence.initNumberOutputs` |
| `ASYNC` | action runs on the async queue, not the caller's stack | every timer-fired action `BSequence:157-164` |
| `DEFAULT_ON_CLONE` | reset calc state to default when the component is copied | `BOptimizedStartStop:112,466` |
| `FAN_IN` | slot legally accepts multiple inbound links | `BAlarmCountToRelay:30` |
| `CONFIRM_REQUIRED` | action prompts the operator before firing | (rare) |

## 730.6 — Actions, facets, dynamic slots `[CERT-a]`

- **Typed action parameters** via `@NiagaraAction(parameterType=…, defaultValue=…)` (`BNumericWritable:206-236`,
  `BNumericConst:44`) — strongly-typed args with defaults, editable in WB. (Relevant to the "add a PUBLIC
  `forceDefrost`/`Deshielar ahora` action" idea: a non-HIDDEN action is linkable/invokable; HIDDEN ones are
  not — see B729/Codig retro.)
- **`getSlotFacets(Slot)` projection**: project a `facets` config slot (units/precision) onto outputs
  (`BSequence.getSlotFacets:894-898`, `BSlidingWindowDemandCalc:837-854`, `BOutsideAirOptimization:595-605`)
  → set units once, all outputs inherit.
- **Facet range validation at declaration**: `BFacets.makeInt(null,min,max)` / MIN-MAX on inputs
  (`BSequence.numberOutputs:54 makeInt(null,2,10)`, `BRaiseLower.in:64`) — UI enforces range, but you still
  **clamp defensively** in code (`BRaiseLower:628-635`).
- **Dynamic slot flags at runtime**: `setFlags(getSlot(name), …)` in try/catch to hide/show a
  variable-width output bank (`BSequence.initNumberOutputs/initSlot:776-805`) → one class supports 2-10 outputs.
- **Detect a slot is actually wired** via `getLinks(slot)` before trusting it (`BLeadLagRuntime.isRuntimeLinked:1085-1104`).

## 730.7 — Pure-logic separation / testability `[CERT-a]`

Partial. Tridium's **template-method split**: the base BComponent does all Baja I/O + status in
`onExecute`/`doExecute`, delegating arithmetic to an abstract method whose signature is *almost* Baja-free —
`BQuadMath.onExecute:180` handles status, subclasses implement `double calculate(BStatusNumeric…)` :239 +
`int minInputs()` :249; `BAverage.calculate:43-55` is 12 lines of math. Fully-pure helpers exist only for
heavy reusable formulas: `Psychrometric.enthalpy(float,float)` takes/returns primitives → unit-testable with
no station. Everyday hvac/energy logic (BLeadLagRuntime, BRaiseLower, BOptimizedStartStop) is **inline in the
BComponent** and NOT unit-testable without a running component. **Takeaway**: reach for pure statics only for
genuinely reusable formulas (this is exactly what we did with `ColdRoomControl.decideCall`, and it paid off —
it is the only part of ColdRoomPan that has real unit tests; see B729 case study / hardening bitácora).

## 730.8 — Off-thread IO (hardware) `[CERT-a]`

If you talk to hardware/refrigerant controllers directly: follow the driver subscription contract —
`BProxyExt.readSubscribed/readUnsubscribed` callbacks, `write():883`, and the rule "any IO should be done
asynchronously on another thread — never block the calling thread" (`BProxyExt.java:851-870`;
`BPollScheduler.subscribe/unsubscribe:1130,1154`). Our modules wire to proxy points via BLink instead of
doing IO themselves, so this is mostly N/A today — relevant only if we add direct device IO.

## 730.9 — Top idioms for OUR refrigeration module, and the easy-to-get-wrong list

**Adopt first** (rooms/evaporators/compressors/defrost):
1. Compute into a working value, commit only on real change; check `isValid()` on every sensor; force
   null/fault when insufficient (§730.1, §730.3).
2. `changed()` = `isRunning()` guard + slot dispatch + deadband before writing back (§730.2).
3. Timer lifecycle: cancel-before-schedule, `HIDDEN|ASYNC` action callbacks, cancel+null in `stopped()`,
   plus `started()`+`atSteadyState()` arming (§730.4 + B729).
4. Flag conventions: `TRANSIENT` state, `READONLY` outputs, `SUMMARY`/`OPERATOR` tunables,
   `DEFAULT_ON_CLONE` for calc state so a cloned evaporator/room doesn't inherit stale runtime (§730.5).
5. `catch(Throwable)+log` around any per-child loop on the engine thread (a room iterating its evaporators
   must not let one bad unit abort the whole cycle) (§730.10 gotcha).
6. `getSlotFacets` projection + range facets for units/precision on temp/pressure outputs (§730.6).

## 730.10 — Subtle / easy to get wrong `[CERT-a]`

- **`catch(Throwable)` on the engine thread is NOT pervasive in kitControl** — kitControl leans on the
  framework wrapper `BControlPoint.executeExtensions:394-401`. **Where no such wrapper exists (your OWN
  `changed()`, your OWN timer action), YOU must add the try/catch**, or one exception silently corrupts
  engine state. This is the biggest gap between "what the framework does for you" and "what your custom code
  must do." (Our BColdRoom/BDefrostController/BEvaporatorUnit/BCompressorControl already wrap their handlers
  in try/catch→logError — consistent with the rule; keep it.)
- **Cancel-before-reschedule, arm only in `started()`/`atSteadyState()`** — forgetting either leaves orphan
  tickets that fire after the value moved on.
- **`stopped()` teardown is mostly-but-not-uniform in Tridium** — several kitControl classes cancel tickets
  in `reinitialize()`/`calculate()` but do NOT override `stopped()`. Don't assume the base nulls your
  tickets; do it explicitly.
- **Never write `out` directly** (§730.1) — re-enters `changed()`.
- **`DEFAULT_ON_CLONE` on transient calc state** — omit it and a copied component carries another instance's
  mid-cycle numbers.
- **Gate log-string construction with `isLoggable(Level.FINE)`** on hot paths (`BRaiseLower:707-709`).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Control components mutate the passed BStatusValue and commit only on change; never write `out` directly | [CERT-a] | BControlPoint.java:307-310,318-324 |
| 2 | Honest degradation: check isValid(), force null/fault on insufficient inputs, propagate status | [CERT-a] | BQuadMath:180-231,229; BAverage:43-61 |
| 3 | `changed()` = super + isRunning guard + slot dispatch + deadband | [CERT-a] | BRaiseLower:596-660; BSequence:761-774 |
| 4 | Timer callbacks are HIDDEN\|ASYNC actions; cancel-before-reschedule; teardown in stopped() | [CERT-a] | BSequence:157-164,860-888; BRaiseLower:586-590,959-963 |
| 5 | Flag intents as measured (TRANSIENT state, READONLY output, DEFAULT_ON_CLONE calc-state, ASYNC timer-action, FAN_IN multi-link) | [CERT-a] | frequency counts + BSequence:298,551; BOptimizedStartStop:112,466; BAlarmCountToRelay:30 |
| 6 | Typed action params via @NiagaraAction(parameterType,defaultValue); getSlotFacets projection; range facets + defensive clamp | [CERT-a] | BNumericWritable:206-236; BSequence:894-898,54; BRaiseLower:64,628-635 |
| 7 | Pure-logic separation is partial (template-method + pure formula helpers); everyday logic is inline | [CERT-a] | BQuadMath:180,239,249; Psychrometric.enthalpy |
| 8 | Engine-thread catch(Throwable) is NOT pervasive in kitControl — custom code must self-guard | [CERT-a] | BControlPoint.executeExtensions:394-401 + absence in kitControl handlers |
| 9 | Our modules already wrap handlers in try/catch→logError (consistent) | [CERT] | ColdRoomPan/CompPan source read this session |

**Tally**: 8 [CERT-a], 1 [CERT]. No unmarked claims. `[CERT-a]` = verbatim file:line gathered by a delegated
docSource survey; a representative subset (BControlPoint, BSequence, BQuadMath, BRaiseLower) is corroborated
by the citations' internal consistency, not personally re-opened line-by-line — treat as high-confidence
pending a spot re-read if a specific line is load-bearing for a change.

## Connections

- **B4** — Baja Object Model: the slot-system MECHANICS (Property/Action/Topic taxonomy, flags, frozen/dynamic, SlotCursor).
- **B729** — timer lifecycle contract (started/atSteadyState/clockChanged) + our own-module audit.
- **module-anatomy focus (B629-639)** — slotomatic/@NiagaraType runtime, module skeleton.
- Kit doc: `build-n4-module-kit` actionable rt checklist (this block's applied companion).

## Open gaps

- **B730-G1** — **CLOSED (cite, not re-derive).** TOPICS = event broadcast: declare a `Topic`, call
  `fire(topic, eventValue, cx)`; consumers link/subscribe. Canonical exemplars in **alarm-rt** [CERT-a,
  names decompiler-mangled]: `BAlarmClass.fire(alarm/escalatedAlarm1-3, event)`,
  `BAlarmSourceExt.fire(toOffnormal/toFault/toNormal, event)`, `BAlarmService`, `BAlarmRecipient` — Topics
  are the alarm event backbone. Taxonomy in B4 §4.1.1 (Property=state · Action=command · Topic=notification).
  RELATIONS = typed graph edges beyond direct BLinks; used by hierarchy/tags/BQL (`BRelationLevelDef`
  inbound/outbound relation ids, hierarchy-rt) — already covered deeply in **B5** (ORD/queries/hierarchy/tags),
  tags **B260-270**, hierarchy **B584-590**. For OUR modules: a Topic could model a "defrost started/ended"
  EVENT for subscribers, but today `defrostActive` (a Property) + the dashboard suffices; Relations are not
  needed (we use BLinks + the parent-child tree).
- **B730-G2** — **CLOSED (cite).** `BWorker` is a `BComponent` wrapping a `Worker` thread pump for
  off-engine work (`baja/javax/baja/util/BWorker.java:19-21` "BComponent wrapper for Worker" [CERT-a]) —
  post Runnables to it for heavy/blocking work, never block the engine thread. Already covered in the
  engine/threading corpus (B7 and others). Not needed by our modules (link-driven, no heavy compute/IO); it
  becomes relevant only if we add direct device IO or a long computation (§730.8).
