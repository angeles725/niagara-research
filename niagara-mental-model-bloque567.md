# Block 567 — `batchJob`: the generic, driver-agnostic device-network batch framework beneath provisioning — jobs are `BJob`s serialized one-at-a-time, per-device steps fan out in parallel (cap 2), and `@AgentOn driver:DeviceNetwork` puts a "batch job" on EVERY driver network

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV1 — the `batchJob/driver` sub-framework every provisioning step is built on; the
generic substrate [Block 39] specialized but never opened)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 13 `batchJob/driver` classes + `BBatchJobService`; hierarchy,
concurrency knobs, and the agent target token-verified inline.
**Primary sources** `[CERT]`:
- `organized/batchJob/batchJob-rt/vineflower/javax/baja/batchJob/{BBatchJobService,BBatchJob}.java` +
  `driver/{BDeviceNetworkJob,BNetworkJobStage,BForEachDeviceStage,BDeviceJobStep,BNetworkJobStep,
  BNetworkBatchAgent,DeviceNetworkJobOp,BDeviceStepDetails,BNetworkStepDetails,BDeviceJobPrototype,
  BIDeviceNetworkJobSummary,BIDeviceStepSummary,BINetworkStepSummary}.java`.

**Scope**: the generic batch-execution engine in the `batchJob` module. [Block 39] documented the
Niagara-SPECIFIC provisioning job (`BNiagaraNetworkJob`, the 46-step catalog); this opens the driver-agnostic
framework it extends. Does NOT re-open the BJob base ([Block 511]) or the Niagara provisioning steps ([Block
39]) — REMITTANCE (connects to both).

---

## 567.1 A batch job IS a `BJob`, over a device network [CERT]

`public class BBatchJob extends BJob implements BILastModifiedRetainable` `[CERT] BBatchJob.java:67` — so the
whole framework inherits the [Block 511] job lifecycle (BJobState 6-state, progress/heartbeat, ForkJoinPool
submission, transient dynamic slot). `BDeviceNetworkJob extends BBatchJob` `[CERT] BDeviceNetworkJob.java:35` is
the concrete job over a driver's `BDeviceNetwork`. The class family `[CERT]`:
```
BJob ([B511])
└─ BBatchJob
   └─ BDeviceNetworkJob
BJobStage
├─ BNetworkJobStage        (runs steps at the NETWORK level)
└─ BForEachDeviceStage     (fans a step across each DEVICE)
BJobStep
├─ BNetworkJobStep         (one network-level operation)
└─ BDeviceJobStep (abstract, doRun per device → BDeviceStepDetails)
```
Provisioning's install/backup/credential/license steps ([Block 39]) are all subclasses of `BNetworkJobStep` /
`BDeviceJobStep` executed by these stages.

## 567.2 The service: one queue, a small thread cap [CERT]

`BBatchJobService` `[CERT]` holds the machinery: `jobQueue = new BThreadPoolJobQueue(1)` `[CERT] :124` — a
**single-thread** job queue, so **batch jobs run ONE AT A TIME** (serialized); `maxProvisioningThreads = 2`
`[CERT] :132,185` — the **per-job device parallelism** cap; plus `alarmClass`, `summaryManagerType`, and
`submitJobAction`/`disposeJob`/`makeTempFilePath` actions `[CERT] :58-101`. So there are TWO concurrency knobs
with different meanings: jobs are serialized (queue=1), but WITHIN a job the per-device fan-out runs up to 2
devices concurrently. (License gate on the feature `provisioning` is remittance to [Block 14 §14.11].)

## 567.3 The execution model: stages run steps, per-device steps parallelize [CERT]

`BNetworkJobStage.doRun(service, job, op)` `[CERT] :68-124` runs each step over the network's OK devices
(`step.run(service, nw, okDevices, op) → BNetworkStepDetails`), checking `BJobState` after each (failed → abort
stage; canceling → canceled). `BForEachDeviceStage.doRun` `[CERT] :178-211` is the interesting one: it
`getCombinedSteps(op)` (**adjacent-step combining** — the optimization [Block 39 §39.2.1] mentioned but did not
trace) then `safeExecuteParallel(service, op, steps, devices)` `[CERT] :211` — it partitions steps into
parallel-safe vs not, running the parallel-safe ones **across devices concurrently** (bounded by
`maxProvisioningThreads`). `BDeviceJobStep` `[CERT] :24-84` is the per-device unit: `run(svc, device, op) →
BDeviceStepDetails` wraps an abstract `doRun(svc, details, device, op)` each concrete step implements. So
authoring a new batch operation = subclass `BDeviceJobStep`, implement `doRun`, declare parallel-safety.

## 567.4 `@AgentOn driver:DeviceNetwork` — batch on EVERY driver [CERT]

`BNetworkBatchAgent extends BSingleton implements BIAgent`, `@NiagaraSingleton`,
`@AgentOn(types = {"driver:DeviceNetwork"})` `[CERT] BNetworkBatchAgent.java:22-30`. Because it agents on the
BASE `driver:DeviceNetwork` type, **every driver's network** (BACnet, Modbus, Niagara, LON, …) gets a batch-job
agent — the framework is not Niagara-specific. `provisioningNiagara` ([Block 39]) is one specialization
(`BNiagaraNetworkJob` over `BNiagaraNetwork`), but the same job/stage/step engine drives batch operations on any
driver. Progress is reported through the `BIDeviceNetworkJobSummary` / `BIDeviceStepSummary` /
`BINetworkStepSummary` interfaces `[CERT]`.

## 567.5 Thesis [CERT-synthesis]

Provisioning is not a monolith — it is a thin Niagara-network specialization of a **generic device-network batch
framework**. The reusable substrate (`batchJob`): a `BJob`-based job, network/per-device stages, an abstract
`BDeviceJobStep` contract with declared parallel-safety, adjacent-step combining, a single-thread job queue
(serialized jobs) and a per-job device-parallelism cap of 2. Any driver inherits it via
`@AgentOn driver:DeviceNetwork`. This reframes the [Block 39] 46-step catalog: those steps are the *Niagara
specialization's* contribution ON TOP of this engine; the engine itself is shared and driver-agnostic.

## 567.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BBatchJob extends BJob (B511 lifecycle); BDeviceNetworkJob extends BBatchJob | [CERT] | BBatchJob.java:67; BDeviceNetworkJob.java:35 | token-checked ✓ |
| 2 | Stage/step hierarchy: BNetworkJobStage + BForEachDeviceStage (BJobStage); BNetworkJobStep + abstract BDeviceJobStep (BJobStep) | [CERT] | driver/*.java:10-34,24-84 | token-checked ✓ |
| 3 | jobQueue = BThreadPoolJobQueue(1) → jobs serialized; maxProvisioningThreads default 2 | [CERT] | BBatchJobService.java:124,132 | token-checked ✓ |
| 4 | BForEachDeviceStage: getCombinedSteps (adjacent combining) + safeExecuteParallel (parallel-safe across devices) | [CERT] | BForEachDeviceStage.java:178-211 | token-checked ✓ |
| 5 | BNetworkJobStage runs steps over OK devices, aborts on failed/canceling | [CERT] | BNetworkJobStage.java:68-124 | token-checked ✓ |
| 6 | BDeviceJobStep abstract doRun per device → BDeviceStepDetails | [CERT] | BDeviceJobStep.java:53-84 | token-checked ✓ |
| 7 | BNetworkBatchAgent @AgentOn driver:DeviceNetwork (singleton) → every driver gets batch | [CERT] | BNetworkBatchAgent.java:22-30 | token-checked ✓ |
| 8 | Provisioning = specialization of generic batchJob framework | [CERT-synthesis] | rows 1-7 + [B39] | reasoned ✓ |

**Marker tally**: [CERT] ×7 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 7 of 8
rows token-verified inline.

## Connections

- **[Block 511]** — the BJob base; BBatchJob inherits its 6-state lifecycle + ForkJoinPool submission.
- **[Block 39]** — the Niagara SPECIALIZATION (BNiagaraNetworkJob, 46-step catalog) built on this engine; §39.2.1
  named adjacent-step combining, traced here to `getCombinedSteps`/`safeExecuteParallel`.
- **[Block 14 §14.11]** — the `provisioning` license gate on the service.
- **driver framework** — `driver:DeviceNetwork` is the agent target; ties to the framework-drivers focus.

## Open gaps (this block)

- The exact parallel-safety declaration on a step (how `safeExecuteParallel` decides) and `getCombinedSteps`
  combining rules are named, partially traced — deepen in a later PV block if needed. Focus continues at PV2
  (`BNiagaraProvisioningChannel`, the custom Fox channel).
