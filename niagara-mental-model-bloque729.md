# B729 · Self-firing interval/periodic triggers in a BComponent — the `atSteadyState` + `started` + `clockChanged` lifecycle contract, and the `BDefrostController` interval-never-fires case

> **Scope**: how a Niagara `BComponent` correctly arms a self-firing timer (interval / periodic
> "do X every N"), which lifecycle callbacks it MUST override, and why a component that arms only in
> `atSteadyState()` silently never fires when it is mounted onto an already-running station
> (commissioning). Case study: the PANCCADIA León `BDefrostController` (ColdRoomPan-rt) whose
> `Modo = Intervalo` "never enters" defrost.
>
> **Sources**:
> - FUENTE 3 (Tridium original, docSource javadoc): `organized/docSource/docSource-doc/extracted/baja/javax/baja/sys/BComponent.java`; `.../extracted/control-rt/javax/baja/control/trigger/BTimeTrigger.java`; `organized/docSource/.../javax/baja/sys/Clock.java`; `organized/control/control-rt/vineflower/javax/baja/control/trigger/TriggerScheduler.java`.
> - FUENTE 3 (client code): `~/modulos_niagara_n4/Cliente/Leon-Guanjuato/Paccadia/ColdRoomPan/ColdRoomPan-rt/src/com/angeles/ColdRoomPan/BDefrostController.java`.
> - FUENTE 2 (niagara-help): `find atSteadyState` → 64 hits (bajadoc API ref, 21 files) — it is a documented standard callback; no dedicated guide on the started-vs-atSteadyState distinction (a real cero on the *distinction*, present on the *API*).
> - FUENTE 1 (corpus): B4 (Baja Object Model — lifecycle order), B10 (Platform & Station Lifecycle — boot sequence + steady-state timeout).
> - FUENTE (empirical): PANCCADIA León bitácora `2026-09-03-proceso-tiempos-dashboard-y-auditoria-deshielo.md` (operator: defrost "nunca entró") + live oBIX inspection this session.

---

## 729.1 — The four bootstrap/running callbacks, and the ONE that is not a bootstrap callback `[CERT]`

Verbatim javadoc, `BComponent.java` (docSource):

| Callback | line | When it fires (verbatim) |
|---|---|---|
| `started()` | :333-341 | "called when a component's **running state moves to true**. Components are started top-down, children after their parent." |
| `descendantsStarted()` | :343-347 | "after `started()` has been called on this component and all its descendants." |
| `stationStarted()` | :371-378 | "during station **bootstrap** after all components in the station have been started." |
| `atSteadyState()` | :380-384 | "during station **bootstrap** after the **steady state timeout** has expired." |

The load-bearing distinction `[CERT]`:

- **`started()` fires EVERY time a component's running state transitions to true** — at station boot AND
  when a component is later added, pasted, enabled, or re-mounted onto an **already-running** station.
- **`atSteadyState()` is a BOOTSTRAP-ONLY callback** — it fires once, during station bootstrap, after the
  steady-state timeout. It is NOT re-invoked for a component that becomes running AFTER the station already
  passed steady state.

Steady-state timeout: default 10s, config `niagara.steadystate` (B10 §post-start; B4 §6 calls
`atSteadyState()` the "last chance pre normal-operation"). So on a running station, dragging a component
into the tree gives it `started()` but **never** `atSteadyState()`.

**Consequence for a self-firing timer**: if a component arms its `Clock` ticket ONLY in `atSteadyState()`,
that arming code never runs for a late-mounted instance → the timer is silently never armed → the periodic
action never fires. There is no error; the component simply sits idle.

## 729.2 — The canonical Tridium pattern: `BTimeTrigger` overrides THREE hooks `[CERT]`

`BTimeTrigger` (`javax.baja.control.trigger`, docSource) is Tridium's own "fire every N" component. It arms
its scheduler in **both** lifecycle entry points, plus a clock-shift hook (`BTimeTrigger.java`):

```java
public void atSteadyState() { if (isRunning()) init(); }          // :213  — boot path
public void started()       { if (Sys.atSteadyState()) init(); }  // :221  — late-mount path
public void clockChanged(BRelTime shift) { init(); }              // :294  — wall-clock shift
public void changed(Property p, Context cx) {                     // :283  — reconfig
  if (!isRunning()) return;
  if (p == triggerMode) init();
}
private void init() {                                             // :237
  if (scheduler != null) scheduler.stop();
  scheduler = getTriggerMode().makeScheduler(this);
  checkTime(); scheduler.start();
  setNextTrigger( scheduler.getScheduledTriggerTime() != null
      ? scheduler.getScheduledTriggerTime()
      : scheduler.getNextTriggerTime(Clock.time(), getLastTrigger()) );
}
```

Why each guard exists `[CERT/INFER]`:
- `atSteadyState(){ if(isRunning()) init(); }` — arm on boot, but only if actually running.
- `started(){ if(Sys.atSteadyState()) init(); }` — arm on late-mount, but ONLY when the station is ALREADY
  at steady state. At boot, `started()` runs BEFORE steady state, so `Sys.atSteadyState()` is false and this
  path no-ops — leaving the boot arming to `atSteadyState()`. This is the idiom that gives exactly-once
  arming across both entry paths, and it doubles as the guard that avoids scheduling before the engine is
  ready (see B-connection to the `NotRunningException` at boot, §729.4). `[CERT: Sys.atSteadyState() = Station.atSteadyState]`
- `clockChanged(BRelTime shift){ init(); }` — a JACE clock adjustment (NTP, manual, DST) re-computes the
  next fire. `BTimeTrigger` schedules against an **absolute** `nextTrigger` (`BAbsTime`, default
  `END_OF_TIME`, :136) so a clock jump would otherwise leave a stale target.

`BTimeTrigger` delegates the actual policy to a `TriggerScheduler` (`triggerMode.makeScheduler(this)`),
an abstract with `isTriggerTime(BAbsTime)` / `getNextTriggerTime(BAbsTime, BAbsTime)` (`TriggerScheduler.java:20-22`);
the interval/daily/manual modes each supply one. Inlining the same math is functionally equivalent.

## 729.3 — `Clock` scheduling primitives `[CERT]`

`Clock.java` (docSource) offers four relevant entry points:

| signature | line | semantics |
|---|---|---|
| `schedule(BComponent, BRelTime delay, Action, BValue)` | :86 | invoke `Action` once after a RELATIVE delay |
| `schedule(BComponent, BAbsTime time, Action, BValue)` | :90 | invoke `Action` once at an ABSOLUTE wall-clock time |
| `schedulePeriodically(BComponent, BRelTime period, Action, BValue)` | :223 | invoke repeatedly every `period`; **throws `IllegalArgumentException` if period ≤ 0** |
| `schedulePeriodically(BComponent, BAbsTime start, BRelTime period, Action, BValue)` | :258 | periodic, first fire at `start` |

`Clock.millis()` is `System.currentTimeMillis()` (:40) — epoch/wall-clock, so a manual `now - last`
elapsed computation is valid across the two, but is exposed to wall-clock jumps (hence `clockChanged`).
Two valid designs: (a) one-shot `schedule` + re-arm in the action handler (what `BDefrostController` does),
or (b) `schedulePeriodically`. Design (a) needs the same lifecycle hooks as (b); the trigger source is
orthogonal to the arming problem.

## 729.4 — Case study: `BDefrostController` `Modo = Intervalo` "never enters" `[CERT]`

`BDefrostController.java` (ColdRoomPan-rt) arms its interval ticket like this:

```java
@Override public void atSteadyState() { try { armTrigger(); } catch (Throwable t){ logError("atSteadyState", t);} }  // :474-478
// NO started() override.  NO clockChanged() override.
private void armTrigger() {                                                                                            // :514
  cancelInterval();
  if (getMode().getOrdinal() != BDefrostMode.INTERVAL) { setNextDefrostTime(BAbsTime.NULL); return; }
  long intervalMs = getInterval().getMillis();
  BAbsTime last = getLastDefrostTime();
  long delayMs = (last.isNull() || intervalMs <= 0L) ? intervalMs
               : Math.max(0L, (Clock.millis()-last.getMillis() >= intervalMs) ? 0L : intervalMs-(Clock.millis()-last.getMillis()));
  intervalTicket = Clock.schedule(this, BRelTime.make(delayMs), intervalExpired, null);   // :534  (schedule BEFORE set)
  setNextDefrostTime(Clock.time().add(BRelTime.make(delayMs)));                            // :537
}
```

The logic itself is sound (verified: mode ordinal, Clock overload, action routing `intervalExpired →
doIntervalExpired`, `units()` non-empty confirmed live). The DEFECTS are lifecycle/robustness gaps vs §729.2:

1. **No `started()` override (primary).** During commissioning the controller is dragged into a
   RUNNING station → it gets `started()` but NOT `atSteadyState()` → `armTrigger()` never runs → the
   interval ticket is never created → defrost never fires. This matches the operator's report ("nunca
   entró", bitácora audit) and the live symptom (interval=21s after a restart still not firing; a full
   station restart DOES re-run `atSteadyState`, so the residual not-firing points additionally at a
   deployed-jar mismatch on the ATLAS — a separate open item).

2. **No `clockChanged()` override (secondary).** Relative-delay scheduling with no clock-shift re-arm. If
   `lastDefrostTime` is persisted with a FUTURE timestamp (clock set backward after a defrost), the else
   branch computes `intervalMs - (negative elapsed)` = a huge delay → schedules years out → never fires.
   `BTimeTrigger` avoids this via absolute-time scheduling + `clockChanged → init()`.

3. **`Clock.schedule` before `setNextDefrostTime` (:534 before :537).** Same coupling that made
   `BEvaporatorUnit.applyRunCmd` throw `NotRunningException` during `activateLinks` at boot (a link
   propagates into `runCmd` before the engine accepts schedules): if the `schedule` throws, the anchor is
   never set → `nextDefrostTime` stays null AND the ticket is unarmed. `armTrigger` is only reached from
   `atSteadyState`/`changed` (post-steady, mode/interval are CFG not link targets, so it does not throw at
   boot today — confirmed by the 19:16 ATLAS log showing NotRunningException ONLY in `applyRunCmd`), but
   the ordering is fragile.

## 729.5 — The fix: mirror `BTimeTrigger`'s hooks (no rewrite, no extra module) `[INFER, grounded in §729.2]`

There is NO missing Tridium module and NO uncallable API: the pattern is a plain `BComponent` +
`Clock.schedule` + an `Action` + the right lifecycle hooks — exactly what `BTimeTrigger` does. Add to
`BDefrostController`:

```java
public void started() throws Exception { super.started(); if (Sys.atSteadyState()) armTrigger(); } // late-mount
public void clockChanged(BRelTime shift) { armTrigger(); }                                          // clock jump
```

The `Sys.atSteadyState()` guard in `started()` is the same guard already applied (this session) to
`applyRunCmd` for the `NotRunningException` fix — a consistent idiom. Optional hardening: schedule against
an absolute `BAbsTime` (like `BTimeTrigger.nextTrigger`) instead of a relative delay. Bundle with the
`nextDefrostTime` slot work and the `applyRunCmd` guard into one build → one deploy + station restart.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `started()` fires whenever running-state → true (boot AND late-mount) | [CERT] | BComponent.java:333-341 verbatim javadoc |
| 2 | `atSteadyState()` is bootstrap-only, fires after the steady-state timeout | [CERT] | BComponent.java:380-384 verbatim javadoc |
| 3 | A component mounted on a running station gets `started()` but not `atSteadyState()` | [INFER] | direct corollary of #1+#2; no counter-evidence |
| 4 | `BTimeTrigger` overrides atSteadyState + started(`if Sys.atSteadyState()`) + clockChanged, all calling init() | [CERT] | BTimeTrigger.java:213,221,294,237 |
| 5 | `Clock` exposes schedule(rel), schedule(abs), schedulePeriodically(rel/abs); periodic throws on period ≤ 0 | [CERT] | Clock.java:86,90,223,258 |
| 6 | `Clock.millis()` = System.currentTimeMillis() (epoch) | [CERT] | Clock.java:40 |
| 7 | `BDefrostController` overrides ONLY atSteadyState (no started, no clockChanged) | [CERT] | BDefrostController.java:474-478; full-file grep |
| 8 | `armTrigger` schedules BEFORE setNextDefrostTime | [CERT] | BDefrostController.java:534 then :537 |
| 9 | Missing `started()` ⇒ interval never arms on a late-mounted (commissioned) instance | [INFER] | #1+#2+#3+#7; matches operator "nunca entró" + live symptom |
| 10 | No missing/uncallable module — pattern is buildable in a plain BComponent | [CERT] | BTimeTrigger is exactly that pattern (§729.2) |
| 11 | The canonical idiom (arm in started, cancel in stopped, re-arm in changed guarded by isRunning) is followed by ~25+ Tridium components across control/kitControl/kitLon/history/alarm | [CERT] | §729.7 corpus survey with per-class file:line |
| 12 | An anti-pattern check (`overrides atSteadyState AND NOT started`) returns ZERO hits in the Tridium first-party corpus — Tridium never arms only in atSteadyState | [CERT] | §729.7 sweep of the extracted real-javadoc tree |
| 13 | BCompressorControl and BEvaporatorUnit also have the anti-pattern; BCompressorControl is MED (periodic tick never starts on late-mount — `changed()` re-runs `execute()`, NOT armTick; armTick only in atSteadyState:1659), BEvaporatorUnit is LOW (only soft-start lost) | [CERT] | §729.6 audit, source-verified (:1659/:1692-1712/:1739) |

**Tally**: 9 [CERT], 4 [INFER] (row 13 split). No unmarked claims. Out of scope (named): B729-G1 was
CLOSED this session (ATLAS runs the current jar — not an old-jar issue); the exact `TriggerScheduler`
interval-mode `getNextTriggerTime` math (B729-G2).

## Connections

- **B4** (Baja Object Model) — lifecycle order; `atSteadyState()` as "last chance pre normal-operation".
- **B10** (Platform & Station Lifecycle) — boot sequence, steady-state timeout (default 10s, `niagara.steadystate`).
- **B41** — BComponent lifecycle hooks (`started/descendantsStarted/stationStarted/atSteadyState`); BModule has no hooks.
- ColdRoomPan module blocks / bitácoras — the `BDefrostController` defrost feature and its proceso+tiempos work.

## 729.6 — Own-module audit: who else arms a timer only in `atSteadyState()` `[CERT]`

Grep of the PANCCADIA León modules (`Clock.schedule` callers, `src/` only) for the anti-pattern
(`atSteadyState` present, `started` absent):

| Class | module | arms in atSteadyState | started() | re-arm paths besides atSteadyState | late-mount risk |
|---|---|---|---|---|---|
| `BDefrostController` | ColdRoomPan-rt | interval ticket (armTrigger) | **being added** (started+clockChanged, working tree) | `changed(mode\|interval)` only — no continuously-changing input | **HIGH** — the interval trigger has nothing to re-arm it on a bare late-mount → no defrost |
| `BCompressorControl` | CompPan-rt | control `tick` (armTick called ONLY in `atSteadyState:1659`) | **absent** | `changed():1692-1712` re-runs `execute()` only — **NOT** armTick; tick self-perpetuates via `doTick:1739` but only if atSteadyState started it | **MED** (corrected from MED-LOW — verified in source, not decompile) — on late-mount the periodic tick **never starts**: reactive staging (per `room*Calling` via execute) still works, but the lead/lag **HOUR-based rotation freezes** and the **start-prove timeout never expires** (a compressor drawing no amperage after `startProveDelay` is never faulted). Comment :1715 confirms the tick is needed even when no input changes. |
| `BEvaporatorUnit` | ColdRoomPan-rt | `powerOnTicket` soft-start only | **absent** | cooling/defrost outputs driven by `changed(runCmd)`/`enterDefrost`, not the atSteadyState ticket | **LOW** — late-mount only skips the soft-start stagger (a boot-inrush feature); functionally cosmetic |

Takeaway (corrected after source verification): the anti-pattern is present in three of our components, and
severity depends on whether the timer has a real ALTERNATE arming path — which, verified in source, only
`BEvaporatorUnit` does.
- `BDefrostController` — **full failure** on late-mount (no defrost at all).
- `BCompressorControl` — **partial failure** on late-mount: reactive staging survives (`changed()→execute()`),
  but `changed()` does **NOT** re-arm the tick, so the periodic tick never starts → lead/lag hour rotation
  frozen + start-prove timeout never expires. (My initial "MED-LOW / changed re-arms armTick" was wrong — it
  came from a stale decompile; the current source has armTick only in `atSteadyState:1659`.)
- `BEvaporatorUnit` — genuinely **LOW**: only the soft-start stagger is lost; outputs re-apply via
  `changed(runCmd)`/`enterDefrost`.

So **two of the three (`BDefrostController`, `BCompressorControl`) need `started()` for correctness, not
just consistency**. Fix all three with the same idiom (§729.5) in one build.

## 729.7 — Corpus-wide survey: the idiom is universal, and the anti-pattern is ABSENT in Tridium `[CERT]`

A sweep of the real-javadoc tree (`docSource/.../extracted/<module>/`) across control, kitControl, kitLon,
schedule, history, and alarm (~25+ timer-owning components) establishes the rule as canonical, not
anecdotal.

**The dominant skeleton** (~25+ classes): **arm the Clock ticket from `started()`** (directly, or via a
shared `init()`/`initTimer()`/`calculate()` helper) · **cancel it in `stopped()`** · **re-arm from
`changed()` guarded by `if(!isRunning()) return;`**. Two refinements layer on top:
1. **Steady-state gating** — when the first computation needs settled inputs: `started()` + `changed()` add
   a `Sys.atSteadyState()` check and defer the FIRST run to `atSteadyState()`. `BTimeTrigger`'s mirror pair
   (`started(){if(Sys.atSteadyState())init();}` + `atSteadyState(){if(isRunning())init();}`) is the purest
   form; the kitControl energy family is the volume implementation of the same intent, using a compute-gate
   `if(!Sys.atSteadyState() || !isRunning()) return;` (BNightPurge:628, BPsychrometric:388,
   BSetpointOffset:477, BElectricalDemandLimit:1841, BOptimizedStartStop:1086).
2. **Wall-clock robustness** — components doing absolute/calendar scheduling also override `clockChanged()`
   to recompute+re-arm (only 5 in Tridium: BTimeTrigger:294→init; BControlSchedule:302→execute;
   BTriggerSchedule:461→execute; BIntervalHistoryExt:281→scheduleCollection; BOptimizedStartStop:1070→initClockTicket).

Representative exemplars (extracted tree):

| Class · module | hooks that ARM | guard | primitive |
|---|---|---|---|
| BTimeTrigger · control | started + atSteadyState (mirror) + changed + clockChanged | isRunning / Sys.atSteadyState | 1-shot+rearm & periodic, ABS (via TriggerScheduler) |
| BControlSchedule · schedule | started + clockChanged | isRunning && isMounted | 1-shot+rearm, ABS |
| BIntervalHistoryExt · history | started + changed + clockChanged | isRunning, getActive | periodic, ABS-anchored |
| BHistoryExt · history | **descendantsStarted** + changed | isRunning, getEnabled, Sys.atSteadyState | 1-shot+rearm, ABS |
| BHistoryService · history | started (+ stationStarted for wiring) | service | periodic rel (1 min) |
| BBooleanDelay · kitControl | atSteadyState→calculate + changed | isRunning | 1-shot+rearm rel |
| BNumericDelay/BCurrentTime/BSineWave · kitControl | started→initTimer + changed | isRunning, getEnabled | periodic rel |
| BOptimizedStartStop · kitControl | started + clockChanged (no atSteadyState) | Sys.atSteadyState compute-gate | periodic, ABS (nextTopOfMinute) |

**Guard frequency**: `isRunning()` is the dominant gate (nearly every `changed()` + the mirror pair);
`Sys.atSteadyState()` is second (either as the arm trigger in `started()`, or as a compute-gate);
`isMounted()` only in the schedule module; `Sys.isStationStarted()` in BTriggerSchedule.started().
**Primitive**: both `schedulePeriodically` (steady cyclic work) and one-shot+re-arm (event/delay/calendar
firing) are common; **relative `BRelTime` is the default**, **absolute `BAbsTime`** is reserved for
wall-clock-aligned work (whole schedule module, BTimeTrigger daily mode, history active periods,
`Clock.nextTopOfMinute()` anchors).

**The decisive finding for our case `[CERT]`**: an explicit anti-pattern check —
`overrides atSteadyState() AND NOT overrides started()` — returned **ZERO hits across the entire Tridium
first-party corpus** (control/baja/kitControl/schedule/history/alarm real-javadoc set). Tridium NEVER arms
a timer solely in `atSteadyState()`. Our `BDefrostController` (pre-fix) is therefore a genuine deviation
from universal Tridium practice, not a defensible stylistic choice.

Gotchas surfaced: (a) the ticket may live in a HELPER object, not the BComponent — `BTimeTrigger` delegates
to `TriggerScheduler` (BDailyTriggerMode/BIntervalTriggerMode own the ticket), so a grep for `Clock.schedule`
ON the component misses it; schedules do the same via `nextEvent`. (b) `BSlidingWindowDemandCalc`
(kitControl energy) has its `!Sys.atSteadyState()` guard **commented out** (:832) — a latent inconsistency
in Tridium itself, i.e. the guard matters enough that omitting it is noted as a defect.

## 729.8 — How Tridium anchors an interval to absolute time (and guards a stale anchor) `[CERT-vineflower]`

`BIntervalTriggerMode.IntervalTriggerScheduler` (clean vineflower decompile,
`organized/control/control-rt/vineflower/.../BIntervalTriggerMode.java`):

```java
public void start() {
  scheduledTime = getNextTriggerTime(Clock.time(), getTrigger().getLastTrigger());
  if (interval.getMillis() > 0L)
    ticket = Clock.schedulePeriodically(getTrigger(), scheduledTime, interval, BTimeTrigger.checkTime, null);
}
public BAbsTime getNextTriggerTime(BAbsTime from, BAbsTime previous) {
  // ... (interval<=0 guarded) ...
  if (diff.getMillis() > interval.getMillis() * 3L)          // stale / bogus anchor
    result = from.add(BRelTime.make((long)(interval.getMillis() * rand.nextDouble())));  // randomized near-term
  else {
    result = previous.add(interval);                          // anchor = lastTrigger + interval
    while (result <= from) result = result.add(interval);     // advance to the next FUTURE grid point
  }
  // (startTime/daysOfWeek variant grid-aligns similarly)
}
```

Two lessons for our `armTrigger`:
1. **Grid anchoring**: next fire = `lastTrigger + N·interval` for the smallest N putting it in the future,
   then `schedulePeriodically` at that ABSOLUTE time. Our code fires once at `lastDefrostTime + interval`
   (or now if overdue) then re-arms a RELATIVE full interval — equivalent for the first fire, but it does
   NOT grid-align when overdue by more than one interval (it collapses to delay 0). Minor.
2. **Stale-anchor guard (the valuable one)**: Tridium falls back to `now + random·interval` when
   `diff > 3·interval` — this is EXACTLY the protection our code lacks for the future/skewed
   `lastDefrostTime` case (§729.4 defect #2), where `intervalMs - negativeElapsed` explodes into a
   multi-year delay. A `Math.abs(elapsed) > 3·interval → delayMs = interval` (or a randomized near-term)
   fallback in `armTrigger` would neutralize that edge case AND spread startup load. Recommended if we
   harden beyond the `started()`/`clockChanged()` hooks. The randomization also avoids a thundering herd
   when many units share one interval.

## Open gaps

- **B729-G1** — **CLOSED (not old-jar).** Panccadia read the LIVE `BDefrostController` type via oBIX
  (ATLAS, 2026-09-03 19:26): both `nextDefrostTime` AND `defrostStart` slots EXIST (present, value null) →
  the ATLAS runs the jar WITH our slots, not an older one. The cold-restart test therefore discriminates
  only: (1) late-mount / missing `started()` (probable; the hook fixes it) vs (2) `atSteadyState()` itself
  not arming on a cold boot (less likely; the guarded hook would NOT cover that).
- **B729-G2** — **CLOSED** (see §729.8, clean vineflower decompile). Interval mode anchors a
  `schedulePeriodically` to an ABSOLUTE `lastTrigger + N·interval`, with a `diff > 3·interval` stale-anchor
  fallback to a randomized near-term fire. Our relative one-shot+re-arm is functionally adequate with the
  `started()`/`clockChanged()` hooks; the `3·interval` guard is the one worthwhile extra hardening (kills
  the future-`lastDefrostTime` giant-delay edge case).
