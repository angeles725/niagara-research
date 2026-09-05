# B816 · Write-path & overlap testing — the threading/link-override mechanism (a dashboard write to a LINK-TARGET slot lands then is SILENTLY overwritten; servlet `set()` and the engine serialize only on the raw value store), our modules' overlap cases (incl. the live armTrigger `Clock.schedule(0)` crash), and a write-path test matrix `[CERT]`

> **Scope**: the user wants tests that a change from the dashboard (or Workbench, or a link, or the engine) cannot
> silently overlap/overwrite another writer and corrupt control. This block establishes, from first principles + code,
> WHAT actually happens on a collision (so we know which overlap bugs are even possible and where), catalogs the real
> overlap cases in OUR modules (one is a live crash), and gives the kit a "write-path test matrix" template + a lint so
> every dashboard-writable slot has an encoded invariant.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT]) — `com.tridium.sys.schema.{ComplexSlotMap,ComponentSlotMap}`,
> `javax.baja.sys.{BComponent,BLink,Flags,LinkCheck}`, `javax.baja.sync.{Transaction,SyncBuffer}`. FUENTE 1 (own
> modules, [CERT]) — `ColdRoomPan-rt/{BDefrostController,BColdRoom}`, `DashboardPan-ux/BDashboardServlet`. REMITTANCE:
> [B19]/[B20]/[B134]-[B137] (engine/thread model), [B802] (link resolution), [B803]/[B813]/[B796] (servlet write path),
> [B815] (BTest), [B729]/[B730] (control timers), [B801] (Clock delay floor), [B776] (action protection). All
> load-bearing cites grep-verified at the enclosing method this session.

---

## 816.1 — The threading model: `set()` runs on the CALLING thread; only the raw value store is locked `[CERT]`
`BComponent.set()` delegates to `ComplexSlotMap.set()`, which runs entirely on the CALLING thread (a servlet/Workbench
HTTP thread, or the engine thread). The ONLY lock is `synchronized (this.instance)` — the per-component monitor — around
the raw value store (`ComplexSlotMap.java:740`). The callbacks run OUTSIDE that lock, synchronously, on the same thread,
AFTER it releases: `this.modified(prop, context, …)` (`:775/:777`) → `ComponentSlotMap.modified()` (`:705`) fires
`knobs.propagate(null)` (`:708`) then `fireComponentEvent(0,…)` → `comp.changed((Property)slot, context)`
(`ComponentSlotMap.java:711,774`). `changed()` is re-entrant — no guard; a `changed()` that calls `set()`/`schedule()`
recurses on the stack. **Consequence (the honest answer to "are overlap bugs possible?")**: a servlet `set()` and an
engine `execute()`/Clock callback on the SAME component serialize ONLY for the raw value store (last-writer-wins, no torn
value); but their callbacks — `changed()` and link propagation — run outside the lock and CAN interleave, with no
cross-thread ordering beyond per-slot write atomicity. So overlap bugs are real and live in the CALLBACKS, not the store.

## 816.2 — The OVERWRITE answer: a dashboard write to a LINK-TARGET slot lands, then is silently overridden `[CERT]`
On a source-slot change, `modified()` fires `knobs.propagate()` (`ComponentSlotMap.java:708`) BEFORE `changed()` (`:711`),
synchronously in the setting thread, in knob-array (link-activation) order (no spec ordering guarantee). The
`Flags.LINK_TARGET` bit (`=32768`, `Flags.java:24`) is set on the target slot at link ACTIVATION
(`BLink.java:205`), but it is **advisory metadata only**: it is checked ONLY at link-CREATION time in the Workbench
wiresheet (`LinkCheck.java:92` → `linkcheck.propAlreadyLinked`), NEVER by `ComplexSlotMap.set()` (no `LINK_TARGET` check
in the write path) nor by `BComponent.canWrite()` (which checks only `isReadonly`/`isOperator`, `BComponent.java:925-951`).
**So a servlet/Workbench manual write to a link-driven slot LANDS unconditionally (if the user has write permission and
the slot is not READONLY) — but it is EPHEMERAL: the next propagation of the source re-runs
`BLink.propagatePropertyToProperty` (reads the live source, re-`set`s the target), silently overwriting the manual value.
No rejection, no exception, no conflict signal.** This is the user's "overwrite": a dashboard write to a link TARGET does
not stick; a write to a non-linked OPERATOR slot does.

## 816.3 — Transaction is NOT cross-thread atomic `[CERT]`
A `set()` with a `Transaction` context queues the op instead of writing (`ComplexSlotMap.java:638`); `commit()` replays
the ops FIFO, each through the standard per-slot `synchronized(this.instance)` write. There is NO global lock held across
the batch and NO cross-batch atomic visibility — another thread can interleave between two op commits. A bare `set()` is
atomic only for its own value store. So "multi-slot atomic write" is ordering-preserving, not isolation-atomic; don't
rely on a Transaction to hide a half-written multi-slot state from the engine thread.

## 816.4 — Overlap cases in OUR modules (the test rows) `[CERT unless noted]`
1. **HEADLINE — the live `armTrigger` `Clock.schedule(0)` crash [CERT-live via B801-G1]**: `BDefrostController.armTrigger`
   computes `long d = Math.max(delayMs, 0L); intervalTicket = Clock.schedule(this, BRelTime.make(d), intervalExpired, null)`
   (`BDefrostController.java:555-556`). The OVERDUE path (`elapsed >= intervalMs → delayMs = 0`) AND a **dashboard write of
   `interval = 0`** (the slot's facet is `MIN = 0 s`, `:170`, so 0 is accepted) both yield `d = 0` → `Clock.schedule(0)` →
   `IllegalArgumentException("time <= 0")` ([B801]). This is the SOURCE LINE of the B801-G1 PANCCADIA crash (thrown 5×,
   `changed` + `atSteadyState` paths). `changed()` triggers it because `changed(interval) → armTrigger()`
   (`BDefrostController.java:510-521`). **A dashboard change silently breaks the module** (the throw is caught/logged; the
   interval timer is then NEVER armed → defrost stops). Fix = `Math.max(delayMs, 1L)` + facet `MIN ≥ 1 s` ([B801] §801.3).
2. **interval changed mid-defrost [CERT]**: `changed(interval)→armTrigger()` calls `cancelInterval()` ONLY
   (`:538`, cancels the interval ticket), NOT the running `durationTicket` — so a config change re-arms the next interval
   but the CURRENT defrost completes on its original `duration`. Safe, but a required test row (a write must not abort a
   running defrost).
3. **setpoint written mid-cooling [CERT]**: `BColdRoom.execute()` (`:435`) recomputes `call1/call2` via `computeCall`
   (a stateful BTstat-style hysteresis latch, prev-call passed in) against the NEW setpoint; an INVALID setpoint status →
   fail-safe hold + skip (`:443`). Deterministic on the engine thread; because the servlet write serializes on the value
   store (§816.1), `execute()` sees old-or-new, never torn — the invariant to assert.
4. **[INFER — to encode]** HOA `resistanceMode` flipped during defrost (does HOA re-apply after `exitDefrost`?); two
   dashboard writes within one engine cycle; CompPan condenser mode changed during a stage delay. Each is a matrix row.

## 816.5 — Test SHAPES `[INFER, grounded in §816.1-4 + B815]`
- **Pure-seam (deterministic, off-station)** — drive the step/compute function with INTERLEAVED inputs and assert the
  invariant (e.g. `computeCall` with a setpoint change between calls; an `armTrigger`-equivalent delay computation asserts
  `d ≥ 1`). This is where overlap logic is cheaply and deterministically testable ([B762]/[B805] §805.8 seam).
- **BTest station (`createTestStation`, [B815])** — mount the component, fire a servlet-equivalent `set()` AND a Clock
  callback, assert ordering/BStatus. NOTE [B815]: there is NO deterministic test clock → use the wall-time pattern
  (schedule short, `Thread.sleep`, assert) and keep these few (they are slow + timing-fragile).
- **Servlet contract ([B813]/[B796])** — the write endpoint's 5 DWS1 gates + that a write to a link-target slot is
  reported as advisory/ephemeral (don't let the UI imply it stuck).

## 816.6 — Kit implication `[INFER, grounded]`
PROPOSED `types/logic.md` + `types/logic-authoring.md` §"write-path & overlap": a **WRITE-PATH TEST MATRIX** template —
one row per **(writable slot × writer × timing) → expected invariant**:

| writable slot | writer | timing | expected invariant |
|---|---|---|---|
| e.g. `interval` | dashboard | idle / mid-defrost / overdue | armed with `d ≥ 1` (never `Clock.schedule(0)`); running defrost unaffected |
| `setpoint` | dashboard | mid-cooling | hysteresis latch holds; invalid status → fail-safe hold |
| a LINK-TARGET slot | dashboard | any | write is EPHEMERAL (overwritten next propagation) — UI must not imply it stuck |
| `resistanceMode` (HOA) | dashboard | mid-defrost | HOA re-applies after exitDefrost |

**Lints** (extend the [B788]/[B805]/[B810] split):
- **HARD**: an `OPERATOR`-writable slot that a dashboard writes to with NO matrix row / test; a `Clock.schedule`/
  `schedulePeriodically` reachable with a computed `≤0` delay (the armTrigger class — cross-ref [B801]).
- **WARN**: a dashboard write path targeting a slot that is a LINK TARGET (write silently overridden — a footgun).
- **REVIEW**: a `changed()` that re-enters `set()`/`schedule()` on the same component (re-entrancy, §816.1); reliance on
  a Transaction for cross-thread atomicity (there is none, §816.3).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `set()` runs on the calling thread; only the raw store is locked (`synchronized(this.instance)`); callbacks run OUTSIDE the lock, synchronously | [CERT] | ComplexSlotMap.java:740,775,777 |
| 2 | `modified()` fires `knobs.propagate()` BEFORE `changed()`, both synchronous same-thread; changed() re-entrant (no guard) | [CERT] | ComponentSlotMap.java:705,708,711,774 |
| 3 | A dashboard write to a LINK-TARGET slot LANDS then is overwritten next propagation; LINK_TARGET(32768) is advisory (set on activate, checked only at link-creation, NOT by set()/canWrite) | [CERT] | Flags.java:24; BLink.java:205; LinkCheck.java:92; BComponent.java:925-951; ComplexSlotMap set() (no LINK_TARGET check) |
| 4 | Transaction queues + FIFO-commits per-slot-locked; NO cross-batch atomic visibility | [CERT] | ComplexSlotMap.java:638; SyncBuffer.java (commit/add) |
| 5 | armTrigger `Math.max(delayMs,0)`→`Clock.schedule(0)` throws on overdue OR a dashboard `interval=0` (facet MIN=0s); source of the B801-G1 live crash | [CERT]+[CERT-live] | BDefrostController.java:170,510,538,555-556; B801 §801.4 (PANCCADIA 5×) |
| 6 | interval-change mid-defrost cancels only the interval ticket (durationTicket runs on); setpoint-mid-cooling latch holds / invalid→fail-safe | [CERT] | BDefrostController.java:538,700-701; BColdRoom.java:435,443 |

**Tally**: 5 [CERT] · 1 [CERT]+[CERT-live]. All framework + module cites grep-verified this session (a delegated map is a
hypothesis until the enclosing method is read). §816.5/§816.6 (shapes, matrix, lints) + the §816.4 item-4 rows are [INFER]
grounded in the [CERT] mechanism. Dedupe: the engine/thread model + link resolution + servlet gates + BTest are
REMITTANCE ([B19]/[B134-137]/[B802]/[B803]/[B813]/[B815]); this block adds the collision semantics, the overwrite answer,
the module overlap cases, and the test matrix.

## Connections
- **[B801]** (the Clock ≤0 floor — §816.4 item-1 IS its source line + a dashboard trigger; B801-G1 live crash), **[B802]**
  (link resolution — §816.2 adds the runtime override semantics), **[B796]/[B803]/[B813]** (servlet write path + step-up +
  CSRF — the writers), **[B815]** (BTest station — the test shape + the no-deterministic-clock caveat), **[B762]/[B805]**
  (pure-seam testing), **[B729]/[B730]** (control timers — armTrigger/tickets), **[B776]** (OPERATOR flag — which slots a
  dashboard can write). Kit: `types/logic.md`/`logic-authoring.md` §"write-path & overlap" + the matrix + the lints;
  CLIENT residue: fix `armTrigger` to `Math.max(delayMs,1L)` + facet MIN≥1s; add matrix tests for the 22 dashboard-writable slots.

## Open gaps
- **B816-G1** (requires-execution): reproduce the servlet-set()↔engine-execute() callback interleave on a live station
  (the per-slot store is [CERT] serialized; the callback interleave is [CERT] possible but its observable effect on our
  specific slots wants a station smoke test).
- **B816-G2** (bounded): the HOA-in-defrost re-apply + CompPan stage-delay overlap rows (§816.4 item-4) — named, not yet
  traced in source; a follow-up to encode them as matrix tests.
