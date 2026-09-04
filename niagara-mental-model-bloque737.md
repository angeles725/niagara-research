# B737 · What runs and guards an RT module (engine thread + daemon/engine watchdog), and how Tridium COMPOSES components into children — the fix for our flat-slot sprawl

> **Scope**: two questions. (A) What starts/finalizes an RT component, what runs it, and what "watches" it
> (the engine thread + the niagarad daemon watchdog + the engine watchdog). (B) How Tridium DISTRIBUTES a
> component's surface into nested CHILD components (Network→Device→Point→Extension) instead of a flat wall of
> slots — the direct answer to "why do our modules have too many slots with no order/structure." Synthesizes
> B6 (engine), B10 (station lifecycle/watchdog), B729 (component lifecycle), B734 (points), with our 25-slot
> `BEvaporatorUnit` as the case.
>
> **Sources**: FUENTE 1 B6 (§6.1 engine thread), B10 (§ station lifecycle/watchdog), B729, B734.
> FUENTE 3 docSource `BControlPoint.java` (extensions-as-children), our rt source.

---

## 737.A — What runs and guards an RT component

### A.1 The lifecycle (start → run → finalize) `[CERT, B729/B6]`
- **Start**: at station boot the component tree starts top-down — `started()` (running-state → true, children
  after parent) → `descendantsStarted()` → `stationStarted()` (all components up) → `atSteadyState()` (after
  the steady-state timeout). A late mount fires only `started()` (B729 — why self-armed timers need it).
- **Run**: `changed()` on slot writes; timer/action callbacks; `execute()`/`onExecute` for points.
- **Finalize**: `stopped()` — cancel tickets, unsubscribe, null refs (B730 §730.10).

### A.2 What RUNS it: the single engine thread `[CERT, B6 §6.1]`
- **One engine thread executes EVERYTHING**: lifecycle callbacks, timers (`Clock.schedule*`), async actions,
  knob/link propagation. Event-driven, single-threaded, coalesced by `EngineManager`. **No scan cycle, no
  topological sort** — the developer designs acyclic chains and uses `Flags.ASYNC` where needed (B6 §6).
- **Hard rule**: ALL `BComponent` state mutation must run on the engine thread. Other threads (FOX workers,
  driver IO, UI) use `post()`/`postAsync()` to hop back before mutating.
- **Consequence**: a slow callback blocks the WHOLE station — everything "freezes" until it returns. This is
  why engine-thread discipline (never block, never throw, catch(Throwable), off-thread IO via BWorker) is
  non-negotiable (B730 §730.8/§730.10).

### A.3 What GUARDS it: the watchdogs `[CERT, B10]`
- **niagarad daemon** (native C/C++) — bootstraps, monitors, manages the station process. **Process
  watchdog**: polls `/proc/PID` (Linux) / handle (Windows) → detects unexpected EXIT. On crash: `EngineMonitor`
  raises "Station crashed"; **auto-restart** (configurable `BSystemPlatformService.autoRestartStation`)
  restarts it; a crash-LOOP backs off exponentially to "Failed" (watch `daemon.log`).
- **Engine watchdog** (`BSystemPlatformService`) — the platform-service watchdog for the engine (reboot/
  shutdown/save/restart). Where the daemon catches a full EXIT, the engine watchdog is the mechanism for a
  HUNG engine (a callback that never returns).
- So the "what keeps it cared for" chain: **engine thread runs the components → engine watchdog guards a hung
  engine → daemon guards the process (exit + auto-restart)**. Nothing preempts a running callback (single
  thread) — the watchdogs recover a DEAD/HUNG station, they do not rescue a merely slow one. Hence the
  discipline is the first line; the watchdog is the last.

## 737.B — Composition: children, not a wall of slots `[CERT]`

### B.1 How Tridium structures a component
Tridium components are **trees of child components**, not flat slot bags. The canonical spine:
**Station → Network → Device → Point → Extension**. Each level is a `BComponent` whose CHILDREN are the next
level. On a point (`BControlPoint`): `proxyExt` is a **frozen child component** (a slot whose TYPE is a
BComponent subtype, `BAbstractProxyExt`), and alarm/history/control **extensions are child components**
added under the point — *"considered an extension on the point… each extension updates the working variable
via PointExtension.onExecute; extensions executed in slot declaration order"* (`BControlPoint.java:47-74`).
Two ways to make a child:
- **Frozen child**: a `@NiagaraProperty(type = "BSomeComponent")` — every instance has that sub-component
  (e.g., `proxyExt`).
- **Dynamic child**: `add()`ed at runtime (e.g., the operator drops an alarm/history ext onto a point).

### B.2 Why OUR modules sprawl `[CERT/INFER]`
`BEvaporatorUnit` declares **25 frozen slots FLAT** on one component: `runCmd, startDelay, stopDelay,
fanRunMode, defrostFanOffDelay, valveOut, evapOut, resistanceOut, coilTemp, resistanceTemp,
evapHighAlarmLimit, evapLowAlarmLimit, hasDefrost, valveMode, fanMode, resistanceMode, freezeProtect,
freezeSetpoint, freezeDiffStop, freezeDiffRestart, powerOnDelay, …`. Nothing is grouped → a long property
sheet, a cluttered Link picker, no visual structure. The cause is **flattening instead of composing** — we
put every concern as a top-level property.

### B.3 The fix — group related slots into nested child components `[INFER, grounded in B.1]`
Model each CONCERN as a child `BComponent`, so the unit becomes a small tree:
```
BEvaporatorUnit
 ├─ (core) runCmd, hasDefrost, coilTemp, resistanceTemp
 ├─ timing   (BComponent)  startDelay, stopDelay, fanRunMode, defrostFanOffDelay, powerOnDelay
 ├─ outputs  (BComponent)  valveOut, evapOut, resistanceOut         (READONLY status)
 ├─ hoa      (BComponent)  valveMode, fanMode, resistanceMode       (OPERATOR)
 ├─ freeze   (BComponent)  freezeProtect, freezeSetpoint, freezeDiffStop, freezeDiffRestart
 └─ alarms   (BComponent)  evapHighAlarmLimit, evapLowAlarmLimit    (or move to point alarm ext, B732)
```
Benefits: collapsible tree, a short top-level property sheet, a clean Link picker (you drill into the child),
and each concern is a reusable, testable unit. Combine with the flag levers (B735): `SUMMARY` on the few
pins that belong on the wire sheet, `HIDDEN` on internals, `BIUnlinkableSlotsContainer` for
visible-but-not-linkable tunables. This is exactly how a point keeps ~6 visible slots while carrying
proxy+alarm+history+control behavior — the behavior lives in CHILD extensions, not flat slots.

Trade-off: nesting adds a level to ORDs/links (`unit/freeze/freezeSetpoint`), and links now target child
slots — worth it above ~12-15 slots on one component. For a small component, flat is fine.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Lifecycle: started→descendantsStarted→stationStarted→atSteadyState (boot), stopped (finalize); late mount = started only | [CERT] | B729; BComponent lifecycle |
| 2 | One engine thread runs all callbacks/timers/actions/propagation; no scan cycle; a slow callback freezes the station | [CERT] | B6 §6.1 (L47,71-75,397) |
| 3 | State mutation must be on the engine thread; other threads use post()/postAsync() | [CERT] | B6 §6.1 (L71-73) |
| 4 | niagarad daemon polls PID → detects exit → auto-restart (autoRestartStation), crash-loop backoff | [CERT] | B10 (L12,85-87,329) |
| 5 | BSystemPlatformService provides the engine watchdog (+reboot/shutdown/restart) | [CERT] | B10 (L38) |
| 6 | Tridium composes Network→Device→Point→Extension; extensions are child components; proxyExt is a frozen child | [CERT] | BControlPoint.java:47-74,101 |
| 7 | Our BEvaporatorUnit has 25 flat frozen slots → sprawl; fix = group concerns into child BComponents | [CERT/INFER] | grep count = 25; §B.3 [INFER] |

**Tally**: 6 [CERT], 1 [CERT/INFER]. No unmarked claims.

## Connections
- **B6** (engine thread), **B10** (station lifecycle/watchdog/daemon), **B729** (component lifecycle),
  **B730** (engine-thread discipline, BWorker), **B734** (points as child trees), **B735** (flags to curate the surface).

## Open gaps
- **B737-G1**: the engine watchdog's exact hung-detection mechanism (timeout, thread-liveness) vs the
  daemon PID poll — B10 names both; the internal detail not opened.
- **B737-G2**: a worked refactor of `BEvaporatorUnit` into child components (slotomatic child-property
  declaration + link migration) — an implementation task, not research.
