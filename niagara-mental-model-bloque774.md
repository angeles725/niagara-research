# B774 · Authoring a background job — the `BSimpleJob`/`BJob`/`BJobStep` SPI and the selection rule (MAE3, D11)

> **Scope**: the AUTHOR template for background work — which base to subclass, the run/progress/log contract, how a
> job is submitted and tracked, and the multi-step batch recipe. B20/B511/B567 document the internals; this is the
> author template they lack ("BJob doRun" = 0 authoring hits). Focus: `module-authoring-exemplars` (MAE3 / dimension
> D11). Kit destination: `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled `baja` (`javax.baja.job.{BJob,BSimpleJob,BJobService,JobLog}`), `batchJob-rt`
> (`BJobStep`, `driver/BDeviceJobStep`, `BBatchJob`), `provisioningNiagara`/`tagdictionary` (step/job exemplars);
> verified this session at `organized/`. FUENTE 1: B20/B511/B567 (REMITTANCE). READ-ONLY. English (post-B115).

---

## 774.1 — The SELECTION rule: `BSimpleJob` (normal) vs `BJob` (own your threading) `[CERT]`
`public abstract class BJob extends BComponent` (`baja/…/job/BJob.java:63`) has two abstract hooks — `doRun(Context)`
(:163) and `doCancel(Context)` (:165) — and its `doSubmit` runs `doRun` ON THE CALLER'S THREAD (no thread spawned).
`public abstract class BSimpleJob extends BJob` (`BSimpleJob.java:9`) fixes that: its `doRun` spawns a `JobThread`
and swaps the author hook to `abstract run(Context)` (:34), with automatic `success()`/`failed()` bookkeeping and
interrupt-based cancel.
**Rule**: subclass **`BSimpleJob`** and implement **`run(Context)`** for the normal case (dedicated background thread,
auto success/fail, interrupt cancel — all free). Subclass **`BJob`** directly and implement both `doRun`+`doCancel`
ONLY when you must own threading/dispatch yourself (as `BBatchJob` does — §774.4).

## 774.2 — The author template (`BSimpleJob.run`) `[CERT]`
Implement `run(Context)`; inside it, report progress and log through the inherited API:
- **Progress + heartbeat**: `progress(int percent)` (`BJob.java:188`) = `setProgress` + `heartbeat()` (:193 →
  `setHeartbeatTime(Clock.time())`). Call `progress(pct)` periodically so the service's MonitorWorker sees the job
  alive.
- **Log**: `log()` (`BJob.java:226`) returns a `JobLog` with `start`/`message`/`success`/`failed(String,Throwable)`.
- **Terminals are automatic under BSimpleJob**: the `JobThread` calls `run(cx)` then `success()`, routing any Throwable
  to `failed(...)` → `complete(state)` sets progress 100 + endTime. (A raw `BJob` author calls `success()`/`failed()`
  itself.)

## 774.3 — Submit + lifecycle: `BJobService.submit(job,cx)` → an ORD handle `[CERT]`
`public class BJobService extends BComponent implements BIService` (`BJobService.java:42`). Submit via
`BOrd submit(BJob job, Context cx)` (:85) — or the job's own `submit(cx)` which delegates to the service.
`doSubmitAction` MOUNTS the job under the service (`add(typeName+'?', job, …)`), runs `job.doSubmit(cx)`, and **returns
the job's `getSlotPathOrd()`** — the caller's handle is an ORD to the mounted job. Track it by resolving the ORD and
reading `getJobState()`/`getProgress()`/`getHeartbeatTime()`/`readLog()`; cancel via the job's `cancel` action (→
`doCancel`). There is NO `join(timeout)` — waiting is polling state/heartbeat via the ORD. The service runs jobs on a
`ForkJoinPool` sized by `niagara.job.threads*`; `getService()` = `Sys.getService(TYPE)`.

## 774.4 — Multi-step batch: `BJobStep`/`BDeviceJobStep` under `BBatchJob` `[CERT]`
For multi-step work, `public abstract class BJobStep extends BComponent` (`batchJob-rt/…/BJobStep.java:19`) — a step is
NOT a `BJob`; steps live under stages under a `BBatchJob`. The concrete driver step
`BDeviceJobStep` has the author hook `protected abstract void doRun(BBatchJobService, BDeviceStepDetails, BDevice,
DeviceNetworkJobOp)` (`BDeviceJobStep.java:100`); its framework `run(...)` makes details, `checkCanceled`, calls
`doRun`, then auto-reports `success()`/`failed()` per device (exemplar: `provisioningNiagara BRobotJobStep`).
`BBatchJob extends BJob` (`BBatchJob.java:82`) overrides `doRun` to iterate `getAllStages()` calling
`stage.doRun(svc, this, op)`, tally failed/canceled, then `stage.jobComplete(op)`, then `complete()/success()`; its
`doSubmit` hands off to its own dispatcher queue, not a per-job thread. **Author**: subclass `BDeviceJobStep` (impl
`doRun`), add steps to a `BJobStage`, group stages in a `BBatchJob`, submit the batch job via `BJobService.submit`.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `BJob` abstract (doRun/doCancel); its doRun runs on the caller thread (no spawn) | [CERT] | BJob.java:63,163,165 |
| 2 | `BSimpleJob extends BJob` spawns a thread and swaps the author hook to abstract `run(Context)` (auto success/fail/cancel) | [CERT] | BSimpleJob.java:9,34 |
| 3 | Selection rule: BSimpleJob.run = normal async; raw BJob.doRun = own your threading | [CERT/INFER] | §774.1; [CERT] on both class contracts, [INFER] on the "when" |
| 4 | Progress/heartbeat/log API: `progress(pct)`, `heartbeat()`, `log()` | [CERT] | BJob.java:188,193,226 |
| 5 | `BJobService.submit(job,cx)` returns an ORD to the mounted job; track/cancel via that ORD; no join | [CERT] | BJobService.java:42,85 |
| 6 | Multi-step: `BJobStep`/`BDeviceJobStep.doRun` under `BBatchJob` (extends BJob) which sequences stages | [CERT] | BJobStep.java:19; BDeviceJobStep.java:100; BBatchJob.java:82 |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims. Spine grep-verified inline this session at `organized/`.

## Connections
- **B20** (Sys/services — `BJobService` is a `BIService`), **B511** (BJob internals — this supplies the missing AUTHOR
  hook `BSimpleJob.run`), **B567** (batchJob — the step author hook `BDeviceJobStep.doRun`). **B778** (`BJobService`
  is registered-by-placement like every service, per B778 §778.1). **B775** (MAE4 — a long-running job coordinates
  with the watchdog/heartbeat, the next block).

## Open gaps
- **MAE3-G1** — the `BBatchJob` dispatcher/queue internals (dispatch vs per-job thread) are named but not walked; a
  bounded follow-up if a builder needs custom batch concurrency.

## Kit implication (→ `types/logic.md`)
Add an "authoring a background job" recipe: subclass **`BSimpleJob`** and implement **`run(Context)`** (dedicated
thread + automatic success/fail + interrupt-cancel are free; report `progress(pct)` and use `log().*`); submit via
`BJobService.submit(job, cx)` and track the returned `BOrd` (poll `getJobState()`/`getProgress()` — there is no
join). Use raw `BJob` (`doRun`+`doCancel`) only to own threading. For multi-step work subclass
`BJobStep`/`BDeviceJobStep` (`doRun`), group steps into `BJobStage`s under a `BBatchJob`, submit the batch job.
