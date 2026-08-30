# Niagara N4 Module Best-Practices Guide

> A practical, evidence-grounded guide to writing clean, correct, maintainable N4 modules — distilled from the
> reference modules (Tridium core, the `httpClientGAngeles` exemplar, the production `chihuahua`) and the
> corpus audits. Every rule traces to a research block ([Block N]). Focus: `module-best-practices` (B705+).
>
> Layers covered: **rt** (runtime) · ux (web) · wb (Workbench) · cross-cutting · build. This file grows as each
> gap closes.

---

## 1. RT layer (`-rt`) — the runtime

The rt jar is what a headless station daemon actually loads (`-rp:rt,se`). Anything a running station needs
MUST live here, not in `-wb`.

### 1.1 Do (patterns)

1. **Type pipeline.** `@NiagaraType` on the class + one `<type name= class=>` in `module-include.xml`. Slotomatic
   generates the `BAJA AUTO GENERATED` region; its last line `TYPE = Sys.loadType(BFoo.class)` self-registers the
   type. A class not in `<types>` is dead bytecode. Run `:rt:slotomatic` **only when a `@Niagara*` annotation
   changed**, then Clean + Build. [B631, B636, B637]
2. **Slots with correct flags + typed defaults.** Copy `BNumericWritable` (control-rt):
   `flags=1` SUMMARY (table-visible), `2` hidden, `10` OPERATOR-writable, `257` ADMIN+SUMMARY; actions `256`
   OPERATOR-invokable. Mark computed/diagnostic properties `Flags.TRANSIENT` so they never serialize to bog. [B650]
3. **Lifecycle placement.**
   - `started()`: `super.started()` first; wire children idempotently; start executors last.
   - `stopped()`: cancel/null scheduler handles before `super.stopped()`.
   - `changed(prop, cx)`: `super` first; filter by `prop.getName()` to avoid feedback loops; wrap the body in
     `catch(Throwable){ log; }` — **never throw on the engine thread**; dispatch heavy work off-thread.
   - `atSteadyState()`: gate any write to live hardware on `Sys.atSteadyState()`. [B650]
4. **Never block the engine thread.** `changed()` submits to a named `ScheduledExecutorService`; use
   `scheduleAtFixedRate` for periodic work; lock per-resource with a `ConcurrentHashMap<String,ReentrantLock>`
   keyed by ORD, not a service-wide lock. [B650]
5. **`runtimeProfile` is load-bearing.** It is the `META-INF/module.xml` attribute, not the filename. A jar
   missing it is **silently not loaded**. Keep station logic out of `-wb`. [B630]
6. **Server-authoritative write gate.** Gate every write handler's first line with
   `BPermissions.has(OPERATOR_WRITE)` (not role-name matching); fail-closed on exception; audit each mutation.
   Client UI hides/shows only for convenience. [B648]
7. **Fault-status discrimination.** Guard `BStatusNumeric` reads with `isFault()||isNull()`. Direction matters:
   fault→0.0 is fail-safe for a low-limit trip, fail-to-danger for a high-limit trip. Treat a faulted sensor as
   a FAULT. [B650, B651, B655]
8. **Ship a `module.palette`.** For any module exporting reusable components — ungated, lazy, fault-tolerant;
   makes them drag-and-drop. [B634]

### 1.2 Don't (anti-patterns)

- **Userless static dispatch that writes** (mcpbridge `ToolDispatcher`): any authenticated user gets full write.
  Fix: `runAsUser` so `OrdTarget.canWrite()` applies. [B643]
- **Throwing/blocking on the engine thread.** [B650]
- **Faulted sensor → 0.0 in protection logic** (silently disables high-limit protection). [B651, B655]
- **Empty `<permissions>` scaffold** left from the New-Module Wizard (12/13 modules): delete it; use per-`<agent>`
  `requiredPermissions`. [B635, B644, B649]
- **Stale Slotomatic AUTO region** (`AWAITING SLOTOMATIC REGEN`): regen before release. [B637, B650]
- **Uber-jar library shading** (sdash bundles 2186 classes, 96% third-party): extract shared `gson-rt`/`jackson-rt`. [B644]
- **Premature write before `Sys.atSteadyState()`.** [B650]

### 1.3 Priority fixes for the current shop modules

1. **(HIGH, safety)** Extend the fault-status fix into `applyProtections()` on chihuahua — a faulted amp sensor
   currently disables overload protection. [B651, B655]
2. **(HIGH, security)** Give mcpbridge per-user RBAC (`runAsUser`) + a least-privilege role + tool-call audit. [B643]
3. **(MED, build)** Run Slotomatic on chihuahua before the next release. [B637, B650]
4. **(LOW, UX)** Add a `module.palette` to chihuahua-rt. [B634]
5. **(LOW, hygiene)** Standard template: no `<permissions>` block, per-agent `requiredPermissions`, per-release
   version bump. [B640, B647]

---

*(ux, wb, cross-cutting, and build sections are added as MBP2–MBP6 close.)*
