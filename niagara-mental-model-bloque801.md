# B801 · The `Clock` timer delay/period floor — `schedule`/`schedulePeriodically` reject a non-positive value with `IllegalArgumentException` (extends B775) `[CERT]`

> **Scope**: the exact runtime contract that a Niagara timer delay/period must be STRICTLY POSITIVE. Author-facing
> `javax.baja.sys.Clock.schedule(...)` / `schedulePeriodically(...)` delegate to the engine, which throws
> `IllegalArgumentException` on a `<= 0` delay/period. Extends [B775] (timer authoring) and gives the kit a `[CERT]`
> cite for a new lint (a compile-time-constant non-positive delay to `Clock.schedule` is an authoring bug).
> The floor is **proven reached at runtime** — the PANCCADIA station threw it 5× (§801.4, `[CERT-live]`).
> **Build-specific line numbers (cite the build)**: the decompiled Windows install (`organized/`, 4.14.0.162) has the
> checks at `:327/:346/:366/:388`; the LIVE PANCCADIA station (a Linux snap build) reports `EngineManager.java:497` /
> `Clock.java:173` in its stack traces (§801.4) — SAME check, different build line numbers.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT]) — `organized/baja/baja/vineflower/javax/baja/sys/Clock.java`
> (public surface) + `.../com/tridium/sys/engine/EngineManager.java` (the impl with the floor). FUENTE 1 — [B775]
> (Clock.schedule vs schedulePeriodically + Ticket), [B787] (the timer-ticket lint this feeds), [B729] (timer
> lifecycle). `[CERT-live]` — the PANCCADIA station console (§801.4, registered in the B800 console census).

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

## 801.4 — `[CERT-live]` the floor IS reached at station runtime (closes B801-G1)
The PANCCADIA station console threw `IllegalArgumentException: time <= 0` from a real `BDefrostController` timer arm
**five times** across two days `[CERT-live]`:
- `WARNING [18:58:19 03-Sep-26 CST][coldRoomPan] coldRoom: changed  java.lang.IllegalArgumentException: time <= 0`
  `at com.tridium.sys.engine.EngineManager.schedule(EngineManager.java:497)`
  `at javax.baja.sys.Clock.schedule(Clock.java:173)  at com.angeles.ColdRoomPan.BDefrostController.armTrigger(...)`
- also 02-Sep 16:20:10, 16:44:20, 19:43:26 (via `atSteadyState`), and 22:28:21 (via `changed`).
- Source: `/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/stations/PANCCADIA/console_backup_260903_1858.txt`
  (registered in the [B800] console census).

**Build-line reconciliation** `[CERT-live]`: the station frames read `EngineManager.java:497` / `Clock.java:173`; the
decompiled Windows install (§801.1-2) reads `:326-328/:345-347` and `Clock.java:72-77`. The PANCCADIA station is a
Linux **snap** build (different compile of the same 4.14 source), so its line numbers differ from `organized/`. SAME
check — decompiled line numbers are build-specific, so a cite must name the build. (This retracts my earlier
"the :497 hypothesis is wrong": :497 is the live-build line, :327 is the decompiled-build line; both are right.)

**Root cause of the live throw** `[INFER, grounded in the stack + B775]`: `BDefrostController.armTrigger` passed
`Clock.schedule` a computed re-arm delay that went `<= 0` (a stale/negative `elapsed` in the re-arm math) — exactly
the [B775]/logic.md re-arm hazard. Fix = floor the delay at a positive minimum (`max(interval - elapsed, 0)` is NOT
enough — 0 still throws; use `max(1, …)` ms) or `isNull()`→full interval. A live proof that the §801.3 lint bites.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `Clock.schedule`/`schedulePeriodically` (4 overloads) delegate to `Nre.getEngineManager()` | [CERT] | Clock.java:72-73,76-77,80-81,84-85 |
| 2 | `schedule` (BRelTime & BAbsTime) throws `IllegalArgumentException("time <= 0")` on a non-positive value | [CERT] | EngineManager.java:326-328, 345-347 |
| 3 | `schedulePeriodically` throws `"period <= 0"` (BRelTime) and additionally `"start <= 0"` (BAbsTime overload) | [CERT] | EngineManager.java:364-366, 385-388 |
| 4 | The floor is `<= 0` (0 rejected; strictly positive required) and is the LAST guard after isRunning + null-action | [CERT] | EngineManager.java:321-328 (guard order) |
| 5 | The throw IS reached at station runtime — PANCCADIA threw it 5× from BDefrostController.armTrigger | [CERT-live] | PANCCADIA console §801.4 (console_backup_260903_1858.txt) |
| 6 | Line numbers are build-specific: decompiled 4.14.0.162 = :327/:346; live Linux-snap station = :497/Clock:173 — same check | [CERT] + [CERT-live] | EngineManager.java:320-392 vs §801.4 station frames |

**Tally**: 4 [CERT] · 1 [CERT-live] · 1 [CERT]+[CERT-live]. No unmarked claims. §801.3 kit implication + the §801.4
root cause are [INFER] grounded in the [CERT]/[CERT-live] evidence.

## Connections
- **B775** (timer authoring — Clock.schedule vs schedulePeriodically + Ticket; this pins the delay/period floor it
  did not state), **B787** (timer-ticket/stopped-cancel lint — B801 is its invalid-arg sibling), **B729** (timer
  lifecycle), **B789** (poll-vs-subscribe). Kit: a new `lint-timers.sh` non-positive-delay rule cites `[ev: corpus B801]`.

## Open gaps
- **B801-G1** — **CLOSED [CERT-live]** (§801.4): the throw is reached at station runtime — PANCCADIA threw
  `time <= 0` from `BDefrostController.armTrigger` 5× (console_backup_260903_1858.txt). No open gaps remain.
