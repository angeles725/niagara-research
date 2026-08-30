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

---

## 2. UX layer (`-ux`) — the web/browser tier

The `-ux` jar is the browser-facing tier. Ideal shape: a thin Java shim + all UI in `rc/` (HTML/JS/CSS).

### 2.1 Do (patterns)

1. **Thin Java shim + JS.** `BSingleton` + `BIJavaScript` + `@AgentOn(types={...}, requiredPermissions=...)` +
   `JsInfo.make(BOrd.make("module://<mod>/rc/.../X.js"), …)`. No UI logic in Java. [B421, B151]
2. **`requiredPermissions` on `@AgentOn` = view visibility only.** It controls whether the view is shown — it is
   NOT security. Enforce writes server-side, separately. [B421, B151]
3. **Server-authoritative RBAC, fail-closed, first line of every POST.** `BPermissions.has(OPERATOR_WRITE)`
   (bit, not role-name); deny on any exception. Browser controls are decorative (ADR D6). [B648, B653]
4. **Pure-web `-ux` (0 Java) is correct.** A high `.class` count in `-ux` is usually bundled libs — audit before
   calling it a defect. [B640, B645, B647]
5. **Live data: Fox-subscription primary + REST fallback.** Initial REST fetch → Fox subscriptions → ~5s REST
   fallback → buffer/replay early updates. [B653]
6. **Optimistic write + rollback + refresh.** [B653]
7. **Typed BQL** from epoch millis / fixed enum tokens / ORD-escaped sources — never user strings. [B652]

### 2.2 Don't (anti-patterns)

- **Auth-gate-only + userless dispatch** → any authenticated user writes (mcpbridge). Fix: `runAsUser`. [B643]
- **Agent-gate-as-security** — the REST endpoint bypasses `@AgentOn requiredPermissions`. [B151, B145]
- **Per-module uber-jar shading** (Gson/Jackson/Commons per dashboard). Fix: shared lib modules / jsonToolkit. [B643, B644, B645]
- **Assuming ES6 in the dashboard main app** — the JxBrowser renderer targets ES5; main app is ES5 IIFE, modern
  only as importmap/UMD islands. [B653]
- **Site data hardcoded in the `-ux` jar** (rack/location layout) — belongs in an `-rt` component tree. [B645]

### 2.3 Priority ux fixes

1. **(HIGH, safety)** Extend the fault-discrimination fix into the protection path (shared with rt). [B651]
2. **(HIGH, security)** Per-user RBAC on any MCP/AI bridge before station deployment. [B643]
3. **(MED, packaging)** Shared lib modules instead of per-dashboard shading. [B643, B644, B645]
4. **(MED, config)** Move hardcoded `-ux` site data into `-rt`. [B645]
5. **(LOW, default)** Fox-sub + REST-fallback live data as the template default. [B653]
