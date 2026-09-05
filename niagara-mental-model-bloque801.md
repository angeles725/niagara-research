# B801 · The `Clock` timer delay/period floor — `schedule`/`schedulePeriodically` reject a non-positive value with `IllegalArgumentException` (extends B775) `[CERT]`

> **Scope**: the exact runtime contract that a Niagara timer delay/period must be STRICTLY POSITIVE. Author-facing
> `javax.baja.sys.Clock.schedule(...)` / `schedulePeriodically(...)` delegate to the engine, which throws
> `IllegalArgumentException` on a `<= 0` delay/period. Extends [B775] (timer authoring) and gives the kit a `[CERT]`
> cite for a new lint (a compile-time-constant non-positive delay to `Clock.schedule` is an authoring bug).
> **Corrects the working hypothesis that the check is at `EngineManager.java:497`** — the real checks are at
> `:327/:346/:366/:388` (4.14.0.162 build; verified by reading the method bodies).
>
> **Sources (read-only, FUENTE 3, all file:line [CERT])**:
> `organized/baja/baja/vineflower/javax/baja/sys/Clock.java` (public surface) +
> `organized/baja/baja/vineflower/com/tridium/sys/engine/EngineManager.java` (the impl with the floor).
> FUENTE 1: [B775] (Clock.schedule vs schedulePeriodically + Ticket), [B787] (the timer-ticket lint this feeds),
> [B729] (timer lifecycle). No live probe — pure code contract.

---

## 801.1 — The public surface delegates to the engine manager `[CERT]`
`javax.baja.sys.Clock` is the author-facing API; every overload forwards to `Nre.getEngineManager()`:
- `Clock.schedule(BComponent, BRelTime, Action, BValue)` → `Clock.java:72-73`
- `Clock.schedule(BComponent, BAbsTime, Action, BValue)` → `Clock.java:76-77`
- `Clock.schedulePeriodically(BComponent, BRelTime, Action, BValue)` → `Clock.java:80-81`
- `Clock.schedulePeriodically(BComponent, BAbsTime, BRelTime, Action, BValue)` → `Clock.java:84-85`

So the floor below lives in `com.tridium.sys.engine.EngineManager`, but it is what an author hits through `Clock`.

## 801.2 — The floor: delay/period must be `> 0` `[CERT]`
Every overload guards in the SAME order — `isRunning` → null-action → the value floor — then throws:

| Clock overload | EngineManager | Guard order | Non-positive throw |
|---|---|---|---|
| `schedule(…, BRelTime time, …)` | `EngineManager.java:320` | `!comp.isRunning()`→`NotRunningException` (:321-322); `action==null`→NPE (:323-324) | `t = time.getMillis(); if (t <= 0L) throw new IllegalArgumentException("time <= 0")` **:326-328** |
| `schedule(…, BAbsTime time, …)` | `:339` | same | `if (t <= 0L) throw … "time <= 0"` **:345-347** |
| `schedulePeriodically(…, BRelTime relPeriod, …)` | `:358` | same | `p = relPeriod.getMillis(); if (p <= 0L) throw … "period <= 0"` **:364-366** |
| `schedulePeriodically(…, BAbsTime start, BRelTime relPeriod, …)` | `:377` | same | `if (s <= 0L) throw … "start <= 0"` **:385-386**; else `if (p <= 0L) throw … "period <= 0"` **:387-388** |

Key facts for the kit:
- **`<= 0` — so `0` is rejected too**; the delay/period must be STRICTLY positive (≥ 1 ms). `BRelTime.make(0)` throws.
- **The floor is the LAST of three guards** — it fires only when the component is running and the action is non-null;
  it is a RUNTIME throw, not a compile check. A static lint must therefore flag a compile-time-constant `<= 0`
  BRelTime literal passed to `Clock.schedule`/`schedulePeriodically`, because the crash only surfaces once the
  component runs.
- The `BAbsTime` `schedule` overload's `"time <= 0"` means the ABSOLUTE epoch millis `<= 0` (i.e. `BAbsTime.NULL`
  / pre-1970) — passing a NULL anchor time throws; this is exactly why the [B775]/logic.md re-arm rule guards
  `isNull() → full interval` before scheduling.

## 801.3 — Kit implication `[INFER, grounded in §801.2]`
A new `lint-timers.sh` rule (or a `verify-module` check) can hard-FAIL on a compile-time-constant non-positive
delay/period handed to `Clock.schedule`/`schedulePeriodically` — e.g. `Clock.schedule(this, BRelTime.make(0), …)`
or a `BRelTime.makeSeconds(n)` where `n <= 0` is a literal — since it throws `IllegalArgumentException` the moment
the component runs. Cite this contract as `[ev: corpus B801]`. Pairs with [B787]'s ticket-without-`stopped()`-cancel
lint: B787 catches the leak, B801 catches the invalid-arg crash.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `Clock.schedule`/`schedulePeriodically` (4 overloads) delegate to `Nre.getEngineManager()` | [CERT] | Clock.java:72-73,76-77,80-81,84-85 |
| 2 | `schedule` (BRelTime & BAbsTime) throws `IllegalArgumentException("time <= 0")` on a non-positive value | [CERT] | EngineManager.java:326-328, 345-347 |
| 3 | `schedulePeriodically` throws `"period <= 0"` (BRelTime) and additionally `"start <= 0"` (BAbsTime overload) | [CERT] | EngineManager.java:364-366, 385-388 |
| 4 | The floor is `<= 0` (0 rejected; strictly positive required) and is the LAST guard after isRunning + null-action | [CERT] | EngineManager.java:321-328 (guard order) |
| 5 | Working hypothesis "EngineManager.java:497" is WRONG; real checks at :327/:346/:366/:388 | [CERT] | grep + read of the method bodies :320-392 |

**Tally**: 5 [CERT]. No unmarked claims. §801.3 kit implication is [INFER] grounded in the [CERT] floor.

## Connections
- **B775** (timer authoring — Clock.schedule vs schedulePeriodically + Ticket; this pins the delay/period floor it
  did not state), **B787** (timer-ticket/stopped-cancel lint — B801 is its invalid-arg sibling), **B729** (timer
  lifecycle), **B789** (poll-vs-subscribe). Kit: a new `lint-timers.sh` non-positive-delay rule cites `[ev: corpus B801]`.

## Open gaps
- **B801-G1** (requires-execution): confirm the throw is reached at station runtime (the guard order means it fires
  only for a running component) — a live smoke-test on a seeded `BRelTime.make(0)` schedule. The static contract is
  [CERT]; the runtime reach is [INFER] until a station run.
