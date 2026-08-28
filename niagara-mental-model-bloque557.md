# Block 557 — The two JVM control-engine schedulers decompiled: Eagle's Clock-timer scan vs Honeywell's dedicated-thread Sequenced Control Engine — completing the four-ecosystem execution model (locates the engine B542 could not find)

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC16 — the JVM control-engine schedulers; the residue B549 named but never decompiled)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the two scheduler classes + the honeywell engine thread; the
scan loops token-verified inline.
**Primary sources** `[CERT]`:
- `clHVAC/clHVAC-rt/vineflower/cl/hvac/base/BControlProgramService.java` (373 lines).
- `ipcCommBus/ipcCommBus-rt/vineflower/com/honeywell/ipccommbus/point/BSequencedControlProgram.java` +
  `.../engine/EngineThread.java` (98 lines) + `MetricsThread.java`.

**Scope**: [Block 549] named four control-execution MODELS but did not decompile the two JVM SCHEDULERS
themselves; [Block 542] said "a separate Sequenced Control Engine component invokes executeBlock" but could not
locate it. This block decompiles both — the clHVAC/Eagle roster engine and the honeywell Sequenced Control
Engine (found in `ipcCommBus`, not `honeywellFunctionBlocks`) — closing the execution-model picture.

---

## 557.1 Eagle — `BControlProgramService` (clHVAC): a Clock-timer scan on the engine thread [CERT]

`BControlProgramService extends BAbstractService` `[CERT] :30`. It scans on the **Niagara Clock**:
`Clock.schedulePeriodically(this, getCycleTime(), triggerPulse, null)` `[CERT] :212,267` — so it runs on the
shared engine thread at a fixed `cycleTime`. Each pulse `[CERT] :128-171`:
```java
synchronized (functionList) {
   for (Object f : functionList) {
      if (f instanceof BControlFunctionSupport) {
         if (++n > 100) { n = 0; Thread.sleep(20L); }   // cooperative yield every 100 blocks
         ((BControlFunctionSupport)f).doExecute();
      }
   }
   long runTime = ticksNow - ticks;
   if (runTime > getCycleTime().getMillis())
      _myLog.error("Control program runtime excceeds scheduled cycletime!");   // overrun: LOG, no drop
}
```
RULES `[CERT]`:
- **Flat roster in registration order** — it iterates `functionList` (a flat list of `BControlFunctionSupport`),
  NOT sorted by an execution-order property.
- **Cooperative yield**: `Thread.sleep(20 ms)` every 100 blocks — because it shares the engine thread, it
  deliberately gives it up periodically.
- **Overrun handling = log only**: if a cycle's runtime exceeds `cycleTime`, it logs an error but does NOT
  skip or reschedule — it just runs long.
- **Startup delay** holds a `startup` flag for `startupDelay` ms; **HIT-license-gated** (fault if invalid).
  Publishes a single `execTime`.

## 557.2 Honeywell — `BSequencedControlProgram` + `EngineThread` (ipcCommBus): a dedicated real-time thread [CERT]

`BSequencedControlProgram extends BApplicationFolder` `[CERT] :103`, with an `iterationInterval` property
`[CERT] :105` (read-only while running). Unlike Eagle, it does NOT use the Niagara Clock — it starts a
**DEDICATED `EngineThread`** (+ a `MetricsThread`) `[CERT] :201-212`. `EngineThread extends Thread` `[CERT]
EngineThread.java:13`, `run()` `[CERT] :36-58`:
```java
int iterationInterval = engine.getIterationInterval();
executionParams.setIterationInterval(iterationInterval);
while (!isStopThread()) {
   ... executeFunctionBlocks(appFolder, executionParams) ...
   long sleepTime = iterationInterval - executionTime;
   if (sleepTime > 0) Thread.sleep(sleepTime);          // pace to a fixed iteration interval
}
```
`executeFunctionBlocks` `[CERT] :80-96` **recursively walks the `BApplicationFolder` TREE**
(`getChildren(IHoneywellExecutionBlock.class)`), calling `executeHoneywellComponent(executionParams)` on each —
but **SKIPS a block whose outputs are overridden** (`!isOutputPropertiesOverridden()`, the FB-level override
[Block 542]). A separate `initializeFunctionBlocks` init pass runs first (`initHoneywellComponent`).

RULES `[CERT]`:
- **Dedicated thread + fixed cadence**: paces to `iterationInterval` by sleeping the remainder each loop — a
  real-time scan isolated from the Niagara engine (so a long control program does not stall the station engine).
- **Tree order, recursive** into sub-`BApplicationFolder`s — the order is the child/`ExecutionOrder` tree order
  ([Block 542]'s "Force Order" assigns it).
- **Rich metrics**: `lastExecutionTime`, `averageExecutionTime`, **`performanceMissCount`** (an overrun
  COUNTER, unlike Eagle's log-only), `cycleStartTime` + `lastDeviationInCycleStartTime`.
- **Explicit stop/start actions** (`requestSequencedControlEngineStop`/`startSequencedControlEngine`) — this is
  the stop/start-to-reinit [Block 542] referenced. IPC/Brand-license + Windows-OS/IPC-network gated.

## 557.3 The four schedulers, now complete [CERT-synthesis]

| Ecosystem | Scheduler | Thread | Cadence | Order | Overrun |
|-----------|-----------|--------|---------|-------|---------|
| kitControl | Niagara **event engine** (EngineManager, [B6] §6.1) | engine thread | event-driven (on change) | topology-free | n/a |
| clHVAC/Eagle | `BControlProgramService` | Niagara Clock, **engine thread** | fixed `cycleTime` | flat registration order | LOG only |
| honeywellFunctionBlocks | `BSequencedControlProgram` + `EngineThread` | **dedicated thread** | fixed `iterationInterval` | tree/ExecutionOrder | COUNTED (`performanceMissCount`) |
| honIrmControl | (none in JVM) | IRM **hardware** | hardware scan | `NanoCmdSetPredecessor` order | on device |

So [Block 549]'s "four ecosystems" now have their SCHEDULERS anchored: kitControl is purely event-driven (no
fixed cycle); Eagle time-slices on the shared engine thread with cooperative yields; honeywell isolates control
onto its own real-time thread with performance metrics; IRM offloads to hardware entirely. The honeywell design
is the most robust (isolated thread + metrics + explicit lifecycle), reflecting its Spyder/IPC DDC heritage.

## 557.4 §14 note — locates the engine [Block 542] could not find [CERT]

[Block 542] §542.2 stated "a separate Sequenced Control Engine COMPONENT invokes `executeBlock` … not
decompiled." It searched `honeywellFunctionBlocks`; the engine actually lives in **`ipcCommBus`**
(`BSequencedControlProgram` + `com.honeywell.ipccommbus.engine.EngineThread`), because it drives the IPC/Spyder
controllers. NOW LOCATED + decompiled here. [Block 542] gets a back-pointer. Refinement: the engine does NOT
call `executeBlock` (the `@NiagaraAction`) per cycle — the dedicated thread calls
`executeHoneywellComponent(executionParams)` directly on each `IHoneywellComponent`, bypassing the action
dispatch (the action is for external/manual invocation).

## 557.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Eagle: Clock.schedulePeriodically(cycleTime) on engine thread; iterates functionList flat, doExecute | [CERT] | BControlProgramService.java:128-151,212 | token-checked ✓ |
| 2 | Eagle: 20ms cooperative yield every 100 blocks; overrun logged not dropped | [CERT] | :140-147,156-158 | token-checked ✓ |
| 3 | Honeywell: BSequencedControlProgram + dedicated EngineThread + iterationInterval | [CERT] | BSequencedControlProgram.java:103,105,201-212; EngineThread.java:13,36 | token-checked ✓ |
| 4 | Honeywell EngineThread paces to iterationInterval by sleeping remainder; recursive tree walk; skips overridden | [CERT] | EngineThread.java:36-58,80-96 | token-checked ✓ |
| 5 | Honeywell metrics: lastExecutionTime/averageExecutionTime/performanceMissCount/cycle deviation | [CERT] | BSequencedControlProgram.java:145-183 | token-checked ✓ |
| 6 | Engine located in ipcCommBus (not honeywellFunctionBlocks); refines B542 §542.2 | [CERT] | path | logic-checked |
| 7 | Four-ecosystem scheduler table (event / Clock-scan / dedicated-thread / hardware) | [CERT-synthesis] | B6/B540/B542/B546 + here | cross-ref ✓ |

**Marker tally**: [CERT] ×6 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 7
rows token-verified inline (both scan loops).

## Connections

- **[Block 549]** — completes the four-ecosystem execution model (named the models; this decompiles the JVM
  schedulers).
- **[Block 540]/[Block 548]** — clHVAC blocks run by this `BControlProgramService` (whose scan is now open).
- **[Block 542]** §542.2 — REFINED/back-pointed: the honeywell Sequenced Control Engine is located
  (`ipcCommBus`), and it calls `executeHoneywellComponent` directly, not the `executeBlock` action per cycle.
- **[Block 6]** §6.1 — the kitControl event engine (the fourth scheduler, contrast).
- **[Block 546]** — IRM's hardware scheduler (the on-device analog).

## Open gaps (this block)

- The `MetricsThread` internals + the exact `ExecutionOrder` sort that builds the child tree are named, not
  decompiled — low value; open on demand. Focus re-STOPs at investigable=0.
