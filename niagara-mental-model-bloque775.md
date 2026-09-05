# B775 · Authoring a watchdog/monitor + choosing a timer — `BAbstractMonitor`, `Clock.schedulePeriodically`, and two corrected assumptions (MAE4, D2)

> **Scope**: the AUTHOR side of watchdog/monitor components and the timer-selection rule — operator-requested
> (watchdog timers). Corrects two seed assumptions with code. B729/B730 cover timer LIFECYCLE/arming; this is the
> monitor-authoring contract + the CONFIGURABLE-interval specialization + the watchdog-layer distinction. Focus:
> `module-authoring-exemplars` (MAE4 / dimension D2). Kit destination: `types/logic.md` + a `METHODOLOGY.md` note.
>
> **Sources**: FUENTE 3 decompiled `systemMonitor-rt` (`BAbstractMonitor`, `BAbstractAlarmMonitor`,
> `BAbstractMemoryMonitor`, `BSystemMemoryMonitor`, `BSystemMonitorService`), `baja` (`javax.baja.sys.Clock`),
> `kitControl-rt` (`util/BRandom`); native watchdog per B124/B681; verified this session at `organized/`. FUENTE 1:
> B729/B730/B737 (REMITTANCE). READ-ONLY. English (post-B115).

---

## 775.1 — The watchdog authoring contract: the `BAbstractMonitor` ladder `[CERT]`
- **Base**: `public abstract class BAbstractMonitor extends BComponent` (`systemMonitor-rt/…/sysmon/
  BAbstractMonitor.java:28`) — the single override is `public abstract void doRunCheck()` (:42), the per-cycle check
  body (invoked via the `runCheck` action on the worker thread). Base holds NO status slots; `isParentLegal` pins it
  under `BSystemMonitorService`; it hands the subclass `getLog()`/`reboot()`.
- **Alarm/status layer** (what most authors extend): `BAbstractAlarmMonitor extends BAbstractMonitor implements
  BIAlarmSource` — maintains `status` (BStatus), `lastAlarmMessage`, `lastAlarmTime` (BAbsTime), `generateAlarm`;
  new obligations `getToNormalText()`/`getToOffnormalText()`; the watchdog primitive is `raiseAlarm(BFormat, boolean)`
  — an EDGE-triggered latch that sets `status`, stamps `lastAlarmTime = BAbsTime.now()`, and fires offnormal/normal.
- **Domain-probe intermediates**: `BAbstractMemoryMonitor` (`abstract long checkMemory()`, `final doRunCheck`),
  `BAbstractCPUMonitor` (`abstract long checkCPU()`).
- **Concrete exemplar**: `BSystemMemoryMonitor extends BAbstractMemoryMonitor` — config slot
  `minimumSystemMemoryLimit`, readonly `freeSystemMemory`; `checkMemory()` samples free memory then the WATCHDOG
  compare `if (free>0 && free<limit){ raiseAlarm(msg,true); if(rebootIfLow) reboot(); } else raiseAlarm(false);`.
  This is a THRESHOLD watchdog (sampled value vs operator-set limit), NOT a heartbeat-age watchdog — the "subject" is
  the local platform (memory/CPU/sockets), sampled each cycle. 10+ siblings (CPU/heap/metaspace/socket/loaded-classes).

## 775.2 — CORRECTION: the cadence is a configurable trigger, not a "2s MonitorWorker poll" `[CERT]`
The seed assumed a ~2s MonitorWorker poll. FALSE: `BSystemMonitorService` drives checks from a
`BTimeTrigger(BIntervalTriggerMode.make(BRelTime … 15 min))` (`BIntervalTriggerMode` imported/used) — an
OPERATOR-EDITABLE interval, DEFAULT 15 MINUTES; `serviceStarted()` links `fireTrigger → checkSystem`, which loops
`getChildren(BAbstractMonitor.class).runCheck()`. `BSysMonWorker extends BWorker` is a WORK QUEUE + dedicated thread
(so checks run off the engine thread), NOT a fixed-interval poller. So: the poll period lives in the trigger
(configurable, 15-min default); the worker only provides the off-engine thread.

## 775.3 — Timer selection: `Clock.schedule` vs `Clock.schedulePeriodically`; `BTimer` is NOT a scheduler `[CERT]`
`javax.baja.sys.Clock` (`baja/…/sys/Clock.java`): one-shot `Clock.Ticket schedule(BComponent target, BRelTime|BAbsTime,
Action, BValue)` (:72,:76); repeating `Clock.Ticket schedulePeriodically(BComponent, BRelTime period, Action, BValue)`
(:80,:84). Both run the callback ON THE ENGINE THREAD; the returned `Clock.Ticket` (interface :126: `cancel()`,
`isExpired()`) is the handle you keep to disarm. **CORRECTION: there is NO `javax.baja.sys.BTimer`** — the only
`BTimer` in the corpus is `cl.hvac.BTimer` (a clHVAC wiresheet countdown control block, `extends
BControlFunctionSupport`), NOT a scheduling primitive. So the real selection axis is TWO: `schedule` (one-shot:
timeout/delayed retry) vs `schedulePeriodically` (repeating poll/check), keeping the `Ticket` to cancel. A
declaratively-authored, persisted, operator-editable cadence uses a `BTimeTrigger` + `BIntervalTriggerMode` linked to
an action (the §775.2 systemMonitor idiom).

## 775.4 — The POSITIVE configurable-interval exemplar: `BRandom` (kitControl) `[CERT]`
The clean "operator-settable poll period" idiom — `kitControl-rt/…/util/BRandom.java`:
- `@NiagaraProperty(name="updateInterval", type="BRelTime", defaultValue="BRelTime.make(1000)")` (:43,:48) — the
  settable interval; a `private Clock.Ticket ticket` (:52) handle.
- `started()` → `initTimer()` → `this.ticket = Clock.schedulePeriodically(this, getUpdateInterval(), execute, null)`
  (:98) — drives the periodic scheduler FROM the property.
- `changed(prop, cx)`: when `prop == updateInterval` (:103), clamp a floor then re-arm (`if (isRunning()) initTimer()`)
  — live re-schedule on operator edit.
- `stopped()` → `ticket.cancel()`.
Siblings `BSineWave`/`BRamp` share the idiom — a stable kitControl pattern. **Author obligation**: a `BRelTime`
`@NiagaraProperty` + a `Clock.Ticket` field; arm in `started()`, re-arm in `changed()` (with a sane floor), cancel in
`stopped()`. (This specializes B729/B730's arming discipline to a CONFIGURABLE interval.)

## 775.5 — Two watchdog LAYERS, distinct `[CERT]`
- **Author-level** `BAbstractMonitor` — threshold watchdogs on local platform resources; can `reboot()` (disabled in
  the SW service). Does NOT watch the engine thread or a `BJob` heartbeat.
- **Framework-native** `EngineWatchdog` (`common.dll`/`nre.dll` JNI, B124/B681) — the station-process/engine-thread
  heartbeat+restart, at the native/daemon layer, NOT via `BAbstractMonitor`.
- A `BJob` heartbeat (B774) is a THIRD, independent surface (polled via an ORD); no shipped `BAbstractMonitor`
  watches a job heartbeat [INFER: a job-watchdog author would `schedulePeriodically` a poll of `BJob.getProgress()`/
  heartbeat age — no exemplar found, find-zero not proof of absence].

## 775.6 — Timer self-heal: the `BTimeTrigger` exemplar (defense-in-depth layers 5, 6, and a partial 4) `[CERT]`
For a timer-driven component, `javax.baja.control.trigger.BTimeTrigger` (control-rt) is Tridium's first-party
self-heal pattern — cite it for the kit's "defense-in-depth for timer state machines" checklist:
- **ONE idempotent re-arm entry (`init()`), called from every hook** (layer 5): `init()` stops any old scheduler,
  rebuilds it from `triggerMode`, and recomputes `nextTrigger` (`BTimeTrigger.java:238-249`). It is invoked from
  `started()` (:221, if atSteadyState), `atSteadyState()` (:213, if running), `changed(triggerMode)` (:284-290), and
  `clockChanged()` (:294). `stopped()` cancels the scheduler (:229-233). This is the B729 arm-in-both rule generalized
  to a single ensureArmed() entry.
- **Self-heal on the classic dead-schedule cause — a clock jump (partial layer 4)**: `clockChanged(BRelTime) → init()`
  (:294-296) re-arms the WHOLE schedule when the system clock shifts (the discontinuity that would otherwise strand a
  pending ticket). `doCheckTime()` recomputes `nextTrigger` from `getNextTriggerTime(now, getLastTrigger())`
  (:266-278) = catch-up relative to the last ACTUAL fire, not a naive reschedule.
- **Expose the next fire (layer 6)**: `nextTrigger` is a `READONLY` property (:136), recomputed on every init/check;
  `lastTrigger` READONLY (:108).
- **What it is NOT (honest boundary)**: BTimeTrigger self-heals on EVENTS (clock change, lifecycle, config), not via an
  INDEPENDENT periodic monitor that polls "is my ticket null / overdue beyond tolerance?" — that independent-tick form
  (the lead's layer 4 proper) remains find-zero in the first-party corpus (MAE4-G1). So the kit cites BTimeTrigger for
  layers 5/6 + clock-jump self-heal, and marks the independent dead-ticket monitor as author-design ([INFER]).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Watchdog base = `BAbstractMonitor` (abstract `doRunCheck`); most authors extend `BAbstractAlarmMonitor` (status/lastAlarmTime, `raiseAlarm` edge-latch) | [CERT] | BAbstractMonitor.java:28,42; BAbstractAlarmMonitor (raiseAlarm) |
| 2 | Concrete monitor = threshold compare vs operator limit (BSystemMemoryMonitor.checkMemory → raiseAlarm/reboot); not heartbeat-age | [CERT] | BSystemMemoryMonitor.checkMemory |
| 3 | CORRECTION: cadence is a configurable `BIntervalTriggerMode` (default 15 min), not a 2s poll; `BSysMonWorker` = work queue | [CERT] | BSystemMonitorService (BIntervalTriggerMode import/use) |
| 4 | Timer selection = `Clock.schedule` (one-shot) vs `Clock.schedulePeriodically` (repeating) + keep `Clock.Ticket`; both on engine thread | [CERT] | Clock.java:72,76,80,84,126 |
| 5 | CORRECTION: no `javax.baja.sys.BTimer` — only `cl.hvac.BTimer` (a wiresheet block, not a scheduler) | [CERT] | find: no baja BTimer; clHVAC-rt/cl/hvac/BTimer.java |
| 6 | Configurable-interval exemplar: `BRandom` — `BRelTime updateInterval` + `Clock.Ticket` + started/changed(re-arm)/stopped(cancel) | [CERT] | BRandom.java:43,48,52,98,103 |
| 7 | Author `BAbstractMonitor` ≠ native `EngineWatchdog` (B124/B681) ≠ `BJob` heartbeat (B774) — three distinct layers | [CERT/INFER] | §775.5; B124/B681 native; B774 job |
| 8 | `BTimeTrigger` self-heals via one idempotent `init()` from started/atSteadyState/changed/clockChanged + exposes nextTrigger; NO independent dead-ticket monitor (find-zero) | [CERT] | BTimeTrigger.java:213,221,238-249,266-278,284-296,136,108 |

**Tally**: 7 [CERT], 1 [CERT/INFER]. No unmarked claims. Spine grep-verified inline this session; corrected the
sweep's/​seed's "2s poll" and "BTimer scheduler" assumptions on verification; §775.6 cites grep-verified at the
docSource BTimeTrigger.java.

## Connections
- **B729** (arm in started()+atSteadyState()), **B730** (execute discipline) — §775.4 is the CONFIGURABLE-interval
  specialization. **B737** (engine thread + watchdog mention). **B124/B681** (native EngineWatchdog). **B774** (BJob
  heartbeat — the third watchdog surface). **B778** (`BSystemMonitorService` is registered-by-placement).

## Open gaps
- **MAE4-G1** — no shipped `BAbstractMonitor` watches a `BJob` heartbeat or a remote-subject last-update age; a
  heartbeat-AGE watchdog is a design an author would build with `schedulePeriodically` but has no first-party exemplar
  (find-zero). Bounded follow-up if a builder needs a liveness (not threshold) watchdog. **PARTIAL (§775.6)**:
  `BTimeTrigger` self-heals its OWN schedule (clock-jump/lifecycle re-arm via `init()`), but an INDEPENDENT periodic
  dead-ticket / heartbeat-age monitor is still find-zero — the independent-tick form stays author-design.

> **Extended by [B801]**: the `Clock.schedule`/`schedulePeriodically` delay/period floor — a `<= 0` value throws
> `IllegalArgumentException` (`EngineManager.java:327/346/366/388`); the "floor" mentioned in the TIMER rule below is
> now pinned to code. Feeds a new non-positive-delay lint.

## Kit implication (→ `types/logic.md` + a `METHODOLOGY.md` note)
- `types/logic.md`: add the watchdog/monitor recipe — subclass `BAbstractAlarmMonitor` (override `doRunCheck()`/domain
  `checkX()` + `getToNormal/OffnormalText`, maintain `status`/`lastAlarmTime`, edge-latch via `raiseAlarm(...)`); and
  the TIMER rule — `Clock.schedule` (one-shot) vs `Clock.schedulePeriodically` (repeating, keep the `Clock.Ticket`,
  cancel on stop); `BTimer` is a clHVAC block, NOT a scheduler. Use `BRandom` (kitControl) as the positive
  configurable-`BRelTime`-interval exemplar (arm in `started()`, re-arm in `changed()` with a floor, cancel in
  `stopped()`).
- `METHODOLOGY.md` note: CORRECT the "MonitorWorker ~2s poll" assumption — systemMonitor cadence is an operator-editable
  `BTimeTrigger`/`BIntervalTriggerMode` (default 15 min); and distinguish the framework-native `EngineWatchdog`
  (engine/process heartbeat, B124/B681) from author-level `BAbstractMonitor` threshold watchdogs.
