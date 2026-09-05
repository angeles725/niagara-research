# B815 · Authoring a STATION-LEVEL / component-lifecycle test — mount the component, drive the interval, catch what lint and pure-JUnit cannot `[CERT]`

> The test residue over the build kit. `run-pure-test.sh` ([Block 743]) covers a Baja-free `final class` decision
> core; `lint-timers.sh` scans source tokens. NEITHER can mount a `BComponent`, fire `started()`/`stopped()`, or
> read a live `Clock.Ticket`. The gap they leave is the CROSS-METHOD, CROSS-LIFECYCLE invariant — e.g. "does the
> timer ticket actually get cancelled when the component is stopped or enters defrost?". This block gives the
> copy-ready `BTestNg` station-lifecycle test shape, the toolbelt/gradle change it needs, and the honest split of
> what lint catches vs. what only a mounted test catches. Deliverable: a `BUILD-LOOP` test layer + a `types/logic.md`
> lifecycle-test recipe.
>
> **Sources**: docSource ORIGINAL (`javax.baja.test.BTest`, `javax.baja.test.TestHelper`, `javax.baja.sys.Clock`),
> decompiled (`javax.baja.test.BTestNgStation`), OEM source (`com.oem.coldRoom.BEvaporatorUnit`), the kit
> (`toolbelt/run-pure-test.sh`, `coldRoom-rt/build.gradle.kts`). REMITTANCE: [Block 805] §805.8 (test layer),
> [Block 807] (build-task matrix / `niagaraTest` = 0 tests from WSL), [Block 762] (timer-cancel discipline),
> [Block 743] (pure-JUnit standalone), [Block 801] (Clock delay floor). Markers: `[CERT]` = verbatim `file:line`
> read this session · `[INFER]` = derived, no runnable Tridium exemplar.
>
> **Type:** `mixed` (framework REMITTANCE + the authoring recipe). Live-code frame from the OEM `coldRoom-rt` tree.

## 815.1 — What the pure-JUnit + lint layer CANNOT reach `[CERT]`
- `run-pure-test.sh` classpath is `junit-4.13.2 + hamcrest-core-1.3` only, `-sourcepath <rt>/src`, runner
  `JUnitCore` — **no Baja jars, no NRE, no engine** (`run-pure-test.sh:7` header; [Block 743]). A test that
  `import javax.baja.*` fails at compile; it cannot instantiate a `BComponent` (needs the type registry),
  cannot call `Clock.schedule()` (needs `Nre.getEngineManager()`), cannot fire `started()`/`stopped()`, cannot
  observe a `Clock.Ticket`. `[CERT]`
- `lint-timers.sh` scans SOURCE TOKENS. It sees the call-site `cancelTicket()` is present in a method, but cannot
  reason about WHICH ticket a method cancels, WHETHER a lifecycle callback exists at all, or WHETHER a scheduled
  ticket survives a stop. Cross-method + cross-lifecycle invariants are invisible to a static token scan. `[CERT]`

## 815.2 — The harness: `BTest.createTestStation()` mounts a live station `[CERT]`
- `BTest extends BObject`; test methods are `public void test…()` discovered by reflection
  (`BTest.java:94` `m.getName().startsWith("test")`) — a `BTestNg` subclass adds the TestNG `@Test`/classloader
  hooks. `[CERT]`
- `BTest.createTestStation()` (`BTest.java:593`) returns a `TestStationHandler` (`:603`) that
  `implements AutoCloseable` (`:604`, `close()`→`releaseStation()` `:764-768`) — so it is usable with
  try-with-resources. Its ctor builds a `BComponentSpace`, `new BStation()` named `"test"`, sets the hidden
  `Station.station` hook, and `space.setRootComponent(station)` to mount it (`:622-651`). `[CERT]`
- Add the component tree BEFORE start: the 3-arg `station.add(name, comp, null)` overload is the one BTest itself
  uses (`BTest.java:642`). `[CERT]`
- `startStation()` (`:668-680`): `Nre.clearPlatform()` → `loadPlatform()` → `startAllServices()` → `station.start()`
  → `Station.stationStarted = true; Station.atSteadyState = true`. `station.start()` cascades `started()` to the
  mounted children, so `isRunning()` becomes true (which the `changed()` guard requires). `[CERT]`
- `releaseStation()` (`:705`, via `stopStation()` `:685-696`) calls `station.stop()` (cascading `stopped()`),
  stops+clears services, unmounts, and deletes the test history dir. **Mandatory** (`:700-702`); use
  try-with-resources or a `finally`. `[CERT]`

## 815.3 — The `atSteadyState` catch: BTest sets a FLAG, not the per-component callback `[CERT]`
`TestStationHandler.startStation()` sets `Station.atSteadyState = true` as a bare flag (`BTest.java:679`) but does
**NOT** call `Nre.getEngineManager().atSteadyState(station)`. The full-stack `BTestNgStation` does BOTH
(`BTestNgStation.java:150-151`: `Station.atSteadyState = true;` then `Nre.getEngineManager().atSteadyState(...)`).
So per-component `atSteadyState()` callbacks (e.g. `BEvaporatorUnit.atSteadyState()` → `applyRunCmd()`,
`BEvaporatorUnit.java:192-197`) do NOT fire under the bare `BTest` path. Either extend `BTestNgStation`, or call
`Nre.getEngineManager().atSteadyState(handler.getStation())` yourself after `startStation()`, or drive the slot
directly (`setRunCmd(...)` → `changed()` → `applyRunCmd()`) which does NOT need `atSteadyState`. `[CERT]` for the
flag/callback split; `[INFER]` that a given component needs the manual engine call (component-specific).

## 815.4 — There is NO deterministic test clock — the timer fires on WALL TIME `[CERT]`
- `Clock.schedule()`/`schedulePeriodically()` delegate straight to `Nre.getEngineManager()` (`Clock.java:171-260`).
  The platform provider is `private static final … AccessController.doPrivileged(…getPlatformProvider)`
  (`Clock.java:350`) — **no injection seam**. `[CERT]`
- `Clock.setTime(BAbsTime)` needs `NiagaraBasicPermission("SET_TIME")` and sets the real system clock
  (`Clock.java:124-137`); it is NOT a tick-advance for scheduled actions. No `TestClock`/`VirtualClock`/`MockClock`
  hook exists on `Clock`. `TestHelper.waitFor(…)` (`TestHelper.java:173`) is a real-time `Thread.sleep` poll loop,
  not deterministic advance. `[CERT]`
- **Consequence for the recipe:** do NOT wait for a timer to fire. Set `startDelay` LONG so the ticket does not
  expire during the test, drive the lifecycle transition, then read the `Clock.Ticket` HANDLE directly via
  `TestHelper.getPrivateField(evap, "startDelayTicket")` (`TestHelper.java:341`, reflection + `setAccessible`;
  field is `BEvaporatorUnit.java:326`). This makes the assertion deterministic and independent of wall time. `[CERT]`

## 815.5 — The two invariants worth a mounted test (honest, verified in the OEM tree) `[CERT]`
Reading `BEvaporatorUnit` (`coldRoom-rt`): the component holds ONE ticket (`startDelayTicket`, `:326`) and ONE
canceller (`cancelTicket()`, `:309-312`). (There is no `cancelRunTickets()` and no `powerOnTicket` — an earlier
sweep hypothesized those; they are NOT in the source.) The real invariants:
1. **enterDefrost cancels the ticket (PASSES today).** `applyRunCmd()` rising edge with `startDelay>0` schedules
   `startDelayTicket = Clock.schedule(...)` (`:238`); `enterDefrost()` (`:266-274`) calls `cancelTicket()` (`:270`)
   and sets `resistanceOut=true, evapOut=false, valveOut=false`. A mounted test asserts the ticket handle is null
   after `enterDefrost()` — the cross-method invariant lint cannot see. `[CERT]`
2. **stop() does NOT cancel the ticket (FAILS today — this is the payoff).** `BEvaporatorUnit` declares **no
   `stopped()` override**: a `runCmd` rising edge schedules `startDelayTicket`, and if the component is stopped
   before expiry the ticket is never cancelled — a leaked `Clock.Ticket` (the exact FAIL already documented against
   this type: "schedules a Clock ticket but stopped() does not cancel it", report-module contract; [Block 762]
   timer-cancel discipline). `lint-timers.sh` can only WARN on the missing token; a lifecycle test PROVES it: mount,
   `runCmd=true`, `handler.stopStation()`, then assert the ticket was cancelled — it was not → test RED. This is
   what the mounted layer buys over lint. `[CERT]`

## 815.6 — Copy-ready component-lifecycle test shape `[CERT + INFER]`
Test class lives in package `com.oem.coldRoom` (the driven methods `enterDefrost()`/`applyRunCmd()`/`isInDefrost()`
are package-private, `BEvaporatorUnit.java:224,266,285`). `[CERT]`

```java
package com.oem.coldRoom;                      // same package: reach package-private hooks [CERT]

import javax.baja.sys.*;
import javax.baja.status.BStatusBoolean;
import javax.baja.test.BTest;
import javax.baja.test.TestHelper;
import org.testng.annotations.Test;

public class BEvaporatorUnitLifecycleTest extends javax.baja.test.BTestNg   // TestNG runner [CERT]
{
  /** Invariant 1 (green today): enterDefrost() cancels the start-delay ticket. */
  @Test
  public void testEnterDefrostCancelsStartDelayTicket() throws Exception
  {
    try (BTest.TestStationHandler h = BTest.createTestStation())            // AutoCloseable [CERT BTest:593,764]
    {
      BStation station = h.getStation();
      BColdRoom room = new BColdRoom();
      station.add("Room1", room, null);                                    // 3-arg add [CERT BTest:642]
      BEvaporatorUnit evap = new BEvaporatorUnit();
      evap.setHasDefrost(true);                                            // enable defrost path [CERT :268]
      evap.setStartDelay(BRelTime.make(600_000));                          // 10 min: ticket won't fire in-test [CERT §815.4]
      room.add("Evap1", evap, null);
      h.startStation();                                                    // cascades started(); isRunning()=true [CERT :668-680]

      evap.setRunCmd(new BStatusBoolean(true));                            // changed()->applyRunCmd() schedules ticket [CERT :206,238]
      verify(TestHelper.getPrivateField(evap, "startDelayTicket") != null, // reflection read [CERT TestHelper:341, field :326]
             "runCmd rising must schedule startDelayTicket");

      evap.enterDefrost();                                                 // package-private [CERT :266]
      verify(TestHelper.getPrivateField(evap, "startDelayTicket") == null,
             "enterDefrost must cancel startDelayTicket");
      verify(evap.getResistanceOut().getValue(),  "resistanceOut true in defrost");   // [CERT :273]
      verify(!evap.getEvapOut().getValue(),        "evapOut false in defrost");        // [CERT :271]
    }                                                                       // releaseStation() auto [CERT :764-768]
  }

  /** Invariant 2 (RED today — the bug the mounted layer catches): stop() leaks the ticket. */
  @Test
  public void testStopCancelsScheduledTicket() throws Exception
  {
    try (BTest.TestStationHandler h = BTest.createTestStation())
    {
      BStation station = h.getStation();
      BEvaporatorUnit evap = new BEvaporatorUnit();
      evap.setStartDelay(BRelTime.make(600_000));
      station.add("Evap1", evap, null);
      h.startStation();
      evap.setRunCmd(new BStatusBoolean(true));                            // schedules startDelayTicket
      verify(TestHelper.getPrivateField(evap, "startDelayTicket") != null, "ticket scheduled");

      h.stopStation();                                                     // cascades stopped() [CERT :685-691]
      // FAILS today: BEvaporatorUnit has no stopped() override -> ticket never cancelled. [CERT §815.5]
      verify(TestHelper.getPrivateField(evap, "startDelayTicket") == null,
             "stopped() must cancel the outstanding start-delay ticket (regression guard)");
    }
  }
}
```
`[INFER]` only on: whether `BColdRoom.add(evap)` alone (vs. an explicit engine `atSteadyState`) suffices for a
component that relies on `atSteadyState()` — here we drive via `setRunCmd()` so it is not needed.

## 815.7 — Toolbelt & gradle change (what actually ships) `[CERT + INFER]`
- **No WSL-runnable `run-station-test.sh` is possible.** `niagaraTest` (the native runner) needs `bin/test` + a dev
  license and discovers 0 tests from WSL ([Block 807]; `run-pure-test.sh:4-5`). A lifecycle test is AUTHORED in the
  `moduleTest` source set, COMPILED in WSL by `./gradlew :coldRoom-rt:moduleTestJar`, but EXECUTED only on a Windows
  dev host or a JACE. So the kit deliverable is a `BUILD-LOOP` note + a `build.sh` `moduleTestJar` step, not a WSL
  runner. `[CERT]`
- **Gradle GAP (must be closed before the test compiles):** `coldRoom-rt/build.gradle.kts` (48 lines) has the
  `com.tridium.niagara-module` plugin (which defines `moduleTestJar`, comment `:11`) but declares **no `test-wb`
  test dependency**. `TestHelper`/`BTest`/`BTestNg` live in `test-wb`; the `moduleTest` source set needs a
  `testImplementation("Tridium:test-wb:4.14.0")` (coordinate per the SDK-home convention, `:37-47`). `[CERT]` the
  dependency is absent; `[INFER]` the exact coordinate string.
- **`build.sh`:** add a `:<mod>-rt:moduleTestJar` step after the main jar so the test set is compiled (a compile-only
  gate in WSL — it catches API drift even though it cannot run). `[INFER]`

## 815.8 — Lint vs. lifecycle-test split (which layer owns which check) `[CERT-grounded]`
| Check | `lint-timers.sh` (static) | Mounted lifecycle test (`niagaraTest`) |
|---|---|---|
| `cancelTicket()` token present in a method | YES | YES |
| A scheduled ticket is actually cancelled on `enterDefrost()` | NO (cross-method) | YES (reflection read) |
| A scheduled ticket survives `stopped()` (leak) | WARN on missing `stopped()` token only | YES — proves the leak RED |
| Outputs correct after a lifecycle transition (evap/valve/resistance) | NO | YES |
| Timer FIRES and energizes `evapOut` after delay | NO | YES (real-time poll; slow/flaky, §815.4) |
Rule of thumb: **lint gates the presence of the cancel; the mounted test gates its correctness across methods and
lifecycle.** Ship both — they are not redundant.

## 815.9 — Kit implication `[CERT-grounded]`
1. `BUILD-LOOP`: add a **test layer** — pure-JUnit decision core ([Block 743]) for algebra, PLUS a `moduleTest`
   `BTestNg` station-lifecycle test for mount/timer/stop invariants; `build.sh` gains a `moduleTestJar` compile step;
   document that execution is native/JACE-only (WSL = compile-gate).
2. `types/logic.md`: a "lifecycle test recipe" — `createTestStation()` + try-with-resources + drive-the-slot +
   read-the-ticket-handle-by-reflection (never wait on wall-clock timers) + assert the stop/defrost cancel invariant.
3. Scaffold: the `-rt` gradle template should ship the `testImplementation` on `test-wb` so a lifecycle test compiles
   out of the box (closes the §815.7 gap for every future module).

## 815.10 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | pure-JUnit (`run-pure-test.sh`) has no Baja/engine → cannot mount a component or read a Ticket | `[CERT]` | `run-pure-test.sh:7`; [B743] | Y — read |
| 2 | `createTestStation()`→`TestStationHandler` (AutoCloseable) mounts a live `BStation`; 3-arg `add` used by BTest | `[CERT]` | `BTest.java:593,603,642,651,764-768` | Y — read |
| 3 | `startStation()` sets `atSteadyState` FLAG only; per-component callback needs `BTestNgStation`/manual engine call | `[CERT]` | `BTest.java:668-680`; `BTestNgStation.java:150-151` | Y — read |
| 4 | No deterministic test clock; `Clock` provider is `private static final` via doPrivileged; `setTime` needs SET_TIME | `[CERT]` | `Clock.java:124-137,171-260,350` | Y — read |
| 5 | `BEvaporatorUnit` has ONE ticket + `cancelTicket()`; `enterDefrost()` cancels it; NO `stopped()` override (leak) | `[CERT]` | `BEvaporatorUnit.java:238,266-274,309-312,326` | Y — read |
| 6 | Reflection read of a private field via `TestHelper.getPrivateField(Object,String)` | `[CERT]` | `TestHelper.java:341` | Y — read |
| 7 | `coldRoom-rt/build.gradle.kts` declares no `test-wb` test dependency → moduleTest won't compile as-is | `[CERT]` | `build.gradle.kts:10-48` | Y — read |
| 8 | `niagaraTest` = 0 tests from WSL; lifecycle test is compile-only in WSL, runs native/JACE | `[CERT]` | [B807]; `run-pure-test.sh:4-5` | Y — REMITTANCE |
| 9 | The copy-ready two-test shape catches the cancel-on-defrost + cancel-on-stop invariants lint/pure-JUnit miss | `[INFER]` | §815.5/§815.6, composes rows 1-7 | recipe |

**Tally:** `[CERT]` ×8 · `[INFER]` ×1. The one `[INFER]` is the assembled recipe over eight verified primitives (§11).
Correction logged: the pre-authoring sweep framed the test on a `cancelRunTickets()`/`powerOnTicket` swap that does
NOT exist in `BEvaporatorUnit`; reframed onto the two invariants that ARE in the source (§815.5).

## 815.11 — Connections & open gaps
- REMITTANCE: [Block 805] §805.8 (test layer in the build loop), [Block 807] (build-task matrix, `niagaraTest`
  0-from-WSL), [Block 762] (timer-cancel discipline — the leak this catches), [Block 743] (pure-JUnit standalone),
  [Block 801] (Clock delay floor). Pairs with [Block 812] (author-built liveness watchdog — the runtime analogue of
  this test's stop-invariant).
- **B815-G1** (build/PoC, requires-execution): add `testImplementation` on `test-wb` to `coldRoom-rt`, author the
  §815.6 two tests, run them via `niagaraTest` on a Windows dev host / JACE, and confirm test 1 GREEN + test 2 RED
  (the missing-`stopped()` leak) — then add a `stopped(){ cancelTicket(); }` override and confirm test 2 flips GREEN.
- **B815-G2**: confirm the exact `Tridium:test-wb:<ver>` coordinate the local gradle-niagara install resolves
  (§815.7 `[INFER]`), and whether the scaffold template can ship it by default.
