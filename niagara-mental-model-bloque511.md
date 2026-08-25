# Block 511 — `apis` API7: the `BJob` / `BJobService` API — N4's background-job framework (a job is a live `BComponent`, run on a `ForkJoinPool`, tracked by a 6-state lifecycle + incremental log, retained 3-per-type/10-min, and observable over Fox at its ORD)

> **Focus:** `apis`, gap **API7** — the module-author API for submitting/tracking a long-running asynchronous
> job (used by provisioning, discovery, backup, batch across ~every subsystem). READ-ONLY, decompiled; no run.
> Markers §3. **REMITTANCE-checked:** [B20] §20.7 is a ~40-line summary inside a residuals block; no dedicated
> job block exists — genuine.
> **Sources:** FUENTE 3 — `organized/baja/baja/vineflower/javax/baja/job/` (`BJob`/`BJobService`/`BJobState`) +
> `com/tridium/sys/service/ServiceManager.java`; docSource javadoc. FUENTE 1 — [B20] (summary), [B500]
> (mbus discovery jobs), [B39] (provisioning). Evidence delegated to a `sonnet` sweep; ALL load-bearing
> file:line RE-VERIFIED inline.

## §511.1 — The model: a job is a live component `[CERT]`

`public abstract class BJob extends BComponent` (`BJob.java:63`) — **not** a plain `Runnable`; each submitted
job becomes a live station component (observable, slotted). An author writes a job two ways:
- **subclass `BJob`** and implement the two abstract hooks `doRun(Context)` + `doCancel(Context)` — "all work on
  a background thread, never block the caller";
- **subclass `BSimpleJob`** (spawns an inner `JobThread`, override just `run(Context)`) or `BRunnableJob` (wraps a
  `java.lang.Runnable`).

`doSubmit()` is the framework callback (sets `running`, `startTime`, `progress=-1`, clears log, calls `doRun`) —
authors never override it.

## §511.2 — Lifecycle: `BJobState` (6 states) `[CERT]`

`BJobState` (`:21-26`): `unknown(0)` → `running(1)` → `canceling(2)` → `canceled(3)` / `success(4)` / `failed(5)`.
`isRunning()` = `==running`; `isComplete()` = `success||canceled||failed`. `canceling` is the transitional state
set by `doCancel` before the background thread actually stops; `BJob.failed()` redirects an `InterruptedException`
/`JobCancelException` (or a `canceling` state) to `canceled()` — so interrupt-driven cancel resolves cleanly.

## §511.3 — Submission + thread pool `[CERT]`

`BJob.submit(cx)` → `BJobService.getService().submit(this, cx)` (`BJobService.java:85`) → the `submitAction` Baja
action → `doSubmitAction()` (`:89`, under `doPrivileged`): **mounts the job as a dynamic TRANSIENT slot** under
the service (`add(typeName+'?', job, Flags.TRANSIENT)` — the `?` = a dynamic child), calls `job.doSubmit(cx)`,
then `ServiceManager.houseKeeping(this)` (`:94`). The service owns a **`ForkJoinPool executor`** (`:46`) sized
`availableProcessors * niagara.job.threadsPerCPU` (default **2**, `:107-110`), overridable; jobs that need it get
it via `getExecutor()` (`:164`). (`BSimpleJob` uses its own raw `JobThread`; the pool is for jobs that submit
tasks explicitly.)

## §511.4 — Progress + tracking `[CERT]`

`progress(int)` sets the `progress` property (0-100, or **-1 = unknown**) and `heartbeat()`; `heartbeat()` alone
bumps `heartbeatTime` to prove liveness. `progress` + `jobState` carry **`Flags.TRANSIENT | READONLY`** (`:65`,
state prop) — in-memory + subscription-observable, **not persisted to the BOG**. The job **log** (`JobLog`, an
in-memory `JobLogItem` list with typed severity, optional circular `setLimit`) is read two ways: `readLog()` (full
dump) and **`readLogFrom(BLong seq)`** (HIDDEN|NO_AUDIT action returning items ≥ a sequence number) — the
incremental tail the Workbench UI polls. `submit()` returns the job's **ORD** (`slot:/Services/JobService/<Type>?`)
so any Fox client resolves it and subscribes to `jobState`/`progress`/`heartbeatTime` changes.

## §511.5 — Cancel + retention `[CERT]`

`cancel()` → `doCancel` sets `canceling` + `thread.interrupt()`; throw `JobCancelException` from `run()` for
cooperative cancel. **Auto-retention** (`ServiceManager.houseKeeping`, run after each submit — not on a timer):
keep at most **`jobMaxCountPerType = 3`** completed jobs per type (`ServiceManager.java:37`), and only remove
those older than **`jobMinAgeToKeep = 600000` ms = 10 min** (`:38`, oldest-by-`endTime` first). `dispose()` is
manual removal and **throws if the job is still running** — a running job must be canceled first.

## §511.6 — Exposure & permissions `[CERT]`/`[INFER]`

`BJobService` is a live component at `slot:/Services/JobService`; any Fox session with read permission observes
its child jobs in real time; `readLogFrom` is a remotely-invocable (HIDDEN) Baja action. `spy()` renders a
`/spy` job-manager table (Name/State/Progress/Start/Heartbeat/End + pool stats). `implements
BIRestrictedComponent` → **one `BJobService` per `BServiceContainer`** (topology restriction, not RBAC).
`[INFER]` **RBAC:** neither `submitAction` nor `BJob.cancel()`/`dispose()` carry job-specific permission flags —
authorization is the station's standard action-permission model (an operator with write on the job component can
cancel it); there is no dedicated job-permission type. No oBIX-specific job type exists.

## §511.7 — Usage breadth `[CERT]`

**~237 distinct classes** extend `BJob`/`BSimpleJob`/`BRunnableJob` (grep of the vineflower trees) — the job
framework is pervasive across every subsystem. Canonical examples: **`BStationSaveJob`** (`extends BSimpleJob`,
`run()` → `Station.saveSync(this)` — the BOG-save job, tied to [B408]/[B411]); **`BNDiscoveryJob`** (`extends
BJob`, carries `discoveryFolder`/`discoveryPreferences`, results populate child components — the ndriver
discovery pattern behind e.g. [B500]'s `BMbusPrimaryDeviceSearchJob`); **`BBatchJob`** (`extends BJob`, captures
`submitUser`, iterates stages — provisioning/batch, [B39]). Also Andover/LON/Honeywell backup/discover/download
jobs, `BSearchTask`, alarm-status jobs — discovery, backup, batch, provisioning everywhere.

## §511.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | BJob = abstract BComponent; doRun/doCancel hooks; BSimpleJob/BRunnableJob; doSubmit callback | `[CERT]` | `BJob.java:63` | PASS |
| 2 | BJobState 6 states unknown/running/canceling/canceled/success/failed; isComplete | `[CERT]` | `BJobState.java:21-26` | PASS |
| 3 | submit→doSubmitAction mounts TRANSIENT dynamic slot; ForkJoinPool threadsPerCPU=2 | `[CERT]` | `BJobService.java:85,89,46,107-110` | PASS |
| 4 | progress(-1..100)+heartbeat; TRANSIENT|READONLY props; readLogFrom incremental; job ORD for Fox | `[CERT]` | `BJob.java:65`; readLogFrom action | PASS |
| 5 | cancel→canceling→interrupt; retention 3-per-type + 10-min min age; dispose throws if running | `[CERT]` | `ServiceManager.java:37,38,405,413` | PASS |
| 6 | live at /Services/JobService, Fox-observable + /spy; BIRestrictedComponent one-per-container; no job-specific RBAC | `[CERT]`+`[INFER]` | `BJobService` spy/BIRestrictedComponent | PASS |
| 7 | ~237 BJob subclasses; BStationSaveJob/BNDiscoveryJob/BBatchJob | `[CERT]` | grep=237; the three classes | PASS |

**Tally:** 7 claims — all `[CERT]` load-bearing + `[INFER]` (RBAC-via-standard-actions). Block TYPE = **EVIDENCE**;
API7 CLOSED. All load-bearing tokens re-verified inline (subclass count RE-MEASURED 42→237).

## §511.9 — Connections & focus status

- The async spine under many corpus subsystems: BOG-save ([B408]/[B411]), driver discovery ([B500] mbus,
  ndriver), provisioning/backup ([B39]). Its jobs are the transient components the Workbench job bar tracks.
- Observability rides Fox (property subscription on the job ORD) — the [B134]/[B508] transports; `readLogFrom` is
  the incremental-log API a UI or an external client tails.
- **Focus status:** `apis` 5/8 (API1–API3, API5, API7 closed). NEXT = API4 (BOX protocol wire), then API6 (Fox
  client — source-locate), API8 (BQL contracts).
