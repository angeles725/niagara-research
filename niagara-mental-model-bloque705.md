# B705 — Module best practices, RT layer (MBP1): the clean-`-rt` patterns, the anti-patterns the audits found, and the concrete fixes — distilled from the reference modules

> Focus: **module-best-practices** · Gap **MBP1** (rt-layer). Block TYPE = **DESIGN/SYNTHESIS** — distilled from
> already-verified blocks + the real jars in `organized/`; a high [INFER] ratio is expected and healthy (this
> turns cited facts into guidance, it does not extract new facts). Spot-checked 2 code citations before writing
> (`DefaultModulesFileManager.java:132-136`, `BNumericWritable` slot flags) — both confirmed. Feeds
> `docs/module-best-practices.md`. Marker `[CERT]` where a claim re-cites verified code; `[INFER]` for the
> guidance framing.

## 705.1 — RT patterns to copy

[CERT, sources as cited]

- **P1 — Type pipeline:** `@NiagaraType` on the class + one `<type name= class=>` in `module-include.xml` →
  Slotomatic generates the AUTO slot region → the last generated line `TYPE = Sys.loadType(BFoo.class)` is the
  single class-load/self-register gate. A class absent from `<types>` is dead bytecode ([Block 631] §631.2,
  [Block 636]; `Slotomatic.java:140`, `Compiler.java:67-99`). **Run `:rt:slotomatic` before build only when a
  `@Niagara*` annotation changed.**
- **P2 — Slots with correct flags + typed defaults:** the canonical exemplar is `BNumericWritable` (control-rt):
  `flags=1` SUMMARY, `2` hidden-from-summary, `10` OPERATOR, `257` ADMIN+SUMMARY; actions `flags=256`
  OPERATOR-invokable; mark computed/diagnostic props `Flags.TRANSIENT` so they don't serialize ([CERT]
  `BNumericWritable.java:57-58`; [Block 650] `controlLockContentionCount`).
- **P3 — Lifecycle placement:** `started()` → `super` first, wire children idempotently, start executors after;
  `stopped()` → cancel handles before `super`; `changed(prop,cx)` → `super` first, filter by `prop.getName()`,
  `catch(Throwable)` and SWALLOW (never throw on the engine thread), dispatch heavy work off-thread;
  `atSteadyState()` → gate writes to live hardware on `Sys.atSteadyState()` ([Block 650]:
  `BChiUp.java:1608-1649`, `BChiDashboardService.java:243-464`, `ChiLinkHelper.java:669-676`).
- **P4 — Never block the engine thread:** `changed()` submits to a named `ScheduledExecutorService`; periodic
  work via `scheduleAtFixedRate`; per-resource `ConcurrentHashMap<String,ReentrantLock>` keyed by ORD instead
  of a service-wide lock ([Block 650] `BChiDashboardService.java:221-328`).
- **P5 — `runtimeProfile` is the load-bearing manifest attribute:** a jar whose `META-INF/module.xml` lacks
  `runtimeProfile` is **silently not loaded** ("is it an AX module?" warning, no error) — the profile is the
  attribute, not the filename; `-wb` logic is invisible to a headless daemon ([Block 630] §630.2-5; verified
  `DefaultModulesFileManager.java:132-136`).
- **P6 — Server-authoritative write gate (positive exemplar):** gate every write handler's FIRST line with an
  RBAC check using `BPermissions.has(OPERATOR_WRITE)` (not role-name matching), fail-closed on exception, audit
  each mutation; client UI hides/shows only as convenience ([Block 648] `ChiRbacHelper.java:142-175`).
- **P7 — Fault-status discrimination:** guard `BStatusNumeric` reads with `isFault()||isNull()` before using
  the value; **direction matters** — fault→0.0 is fail-safe for a low-limit trip but fail-to-danger for a
  high-limit trip ([Block 650] §650.3, [Block 651] §651.3, [Block 655]).
- **P8 — Ship a `module.palette`:** a `-rt` module exporting reusable components should include a `module.palette`
  BOG at the jar root — ungated, lazy-decoded, fault-tolerant; makes components drag-and-drop ([Block 634]
  §634.2-4).

## 705.2 — Anti-patterns the audits found

[CERT, each cites the block that found it]

- **AP1 — RBAC bypass via userless static dispatch (mcpbridge):** `ToolDispatcher.dispatch(...)` is `static`,
  carries no `BUser`/`Context`, resolves + writes with no `canWrite` — any authenticated user gets full station
  write/create ([Block 643] §643.2b). **Fix: run ops via `runAsUser` so `OrdTarget.canWrite()` applies.**
- **AP2 — Throwing/blocking on the engine thread** in `changed()`/`started()` ([Block 650]; fix = P3/P4).
- **AP3 — Faulted sensor → 0.0 in protection logic** — the display path was fixed (2026-05-06 audit,
  `ChiEquipmentReader.readNumericNullable`→null) but the protection path still collapses to 0.0, silently
  disabling overload protection ([Block 651] §651.3, [Block 655] #1).
- **AP4 — Empty `<permissions>` scaffold** — 12/13 modules carry the untouched New-Module-Wizard permission
  block (empty → harmless today under GrantAll, but noise + over-broad the day a restrictive store deploys)
  ([Block 635], [Block 649] §649.1). **Fix: delete it; use per-`<agent>` `requiredPermissions` (sdash pattern,
  [Block 644]).**
- **AP5 — Stale Slotomatic AUTO region** — `BChiUp` carries `AWAITING SLOTOMATIC REGEN` on 8 slots + an action
  ([Block 650], [Block 637] §637.2). **Fix: regen before release.**
- **AP6 — Uber-jar library shading** — `sdash-rt` bundles 2186 classes (96% third-party Jackson/Commons);
  mcpbridge/datacenter each bundle their own Gson ([Block 643]/[Block 644]/[Block 645]). **Fix: extract shared
  `gson-rt`/`jackson-rt` dependency modules.**
- **AP7 — Premature write before `Sys.atSteadyState()`** — can push placeholder values to live BACnet outputs
  ([Block 650] `ChiLinkHelper.java:669-676`).

## 705.3 — Top rt improvements (actionable)

[INFER, prioritized]

1. **(HIGH, safety)** Extend the fault-status fix from the display path into `applyProtections()` — treat a
   faulted sensor as a FAULT, not 0.0 (AP3). This is a latent life-safety defect on the one production module.
2. **(HIGH, security)** Give mcpbridge per-user RBAC via `runAsUser` + a least-privilege MCP role + a tool-call
   audit log, before it reaches any client station (AP1).
3. **(MED, build)** Run Slotomatic on chihuahua before the next release; auto-detect annotation changes in the
   deploy script and add `:slotomatic` only when needed (AP5).
4. **(LOW, UX)** Add a `module.palette` to chihuahua-rt (6+ exported components, zero-cost) (P8).
5. **(LOW, hygiene)** Standardize the shop template: no `<permissions>` block, per-agent `requiredPermissions`,
   `vendorVersion` bumped per release ([Block 640]/[Block 647]).

## Connections

- Distills focus `module-anatomy` [Block 629]–[Block 636] (skeleton/type-reg/manifest/palette/permissions),
  `chihuahua-source` [Block 648]–[Block 655] (rt control/RBAC/fault), `own-modules-audit` [Block 637]–[Block 647]
  (build/over-permission/exemplars). Deliverable: `docs/module-best-practices.md` (rt section seeded).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | type pipeline + slotomatic gate (P1) | [CERT] | [Block 631]/[Block 636] + Slotomatic.java:140 | cited |
| 2 | slot flags 1/2/10/257, actions 256 (P2) | [CERT] | BNumericWritable.java:57-58 | spot-checked ✓ |
| 3 | runtimeProfile silent-ignore (P5) | [CERT] | DefaultModulesFileManager.java:132-136 | spot-checked ✓ |
| 4 | RBAC write-gate exemplar (P6) vs userless dispatch (AP1) | [CERT] | [Block 648]/[Block 643] | cited |
| 5 | fault→0.0 protection defect (AP3/I1) | [CERT] | [Block 651]/[Block 655] | cited |
| 6 | rt improvement priorities | [INFER] | 705.3 | reasoned |

**Tally:** [CERT] ×5 · [INFER] ×1 (guidance). Block TYPE = **DESIGN/SYNTHESIS** — ratio read as healthy. 2/2
spot-checked code citations confirmed; the rest re-cite already-verified blocks.

## Open gaps (this focus)

MBP1 CLOSED (rt). Next: **MBP2** (ux layer — bajaux/BSingleton+@AgentOn, JS/web, front-end structure). Then
MBP3 (wb), MBP4 (cross-cutting), MBP5 (build), MBP6 (exemplar catalog + guide).
