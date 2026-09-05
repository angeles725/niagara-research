# B815 · Authoring a STATION-LEVEL / component-lifecycle test — mount the component, drive the interval, catch what lint and pure-JUnit cannot `[CERT]`

> The test residue over the build kit. `run-pure-test.sh` ([Block 743]) covers a Baja-free `final class` decision
> core; `lint-timers.sh` scans source tokens. NEITHER can mount a `BComponent`, fire `started()`/`stopped()`/
> `atSteadyState()`, or read a live `Clock.Ticket`. The gap they leave is the CROSS-METHOD, CROSS-LIFECYCLE
> invariant — e.g. "does defrost preserve the power-on ticket?" and "does stop cancel every outstanding ticket?".
> This block gives the copy-ready `BTestNg` station-lifecycle test shape (grounded in the live client
> `ColdRoomPan.BEvaporatorUnit`, which the client PR #1/#2 just made a four-ticket lifecycle), the toolbelt/gradle
> change it needs, and the honest split of what lint catches vs. what only a mounted test catches. Deliverable: a
> `BUILD-LOOP` test layer + a `types/logic.md` lifecycle-test recipe.
>
> **Sources**: docSource ORIGINAL (`javax.baja.test.BTest`, `javax.baja.test.TestHelper`, `javax.baja.sys.Clock`),
> decompiled (`javax.baja.test.BTestNgStation`), LIVE CLIENT source
> (`com.angeles.ColdRoomPan.BEvaporatorUnit`, Leon-Guanjuato repo `origin/main` c66e412 — the post-PR#1/#2 tree),
> the kit (`toolbelt/run-pure-test.sh`, exemplar `coldRoom-rt/build.gradle.kts`). REMITTANCE: [Block 805] §805.8
> (test layer), [Block 807] (`niagaraTest`=0 tests from WSL), [Block 787] (the timer-cancel lint that flagged the
> missing `stopped()`), [Block 762] (timer-cancel discipline), [Block 743] (pure-JUnit standalone), [Block 801]
> (Clock delay floor). Markers: `[CERT]` = verbatim `file:line` read this session · `[INFER]` = derived recipe.
>
> **Type:** `mixed` (framework REMITTANCE + the authoring recipe). Live-code frame from the client ColdRoomPan tree.
> **Correction (see §815.10):** an earlier pass first read the research-repo exemplar `com.oem.coldRoom`
> (one ticket) and wrongly called `powerOnTicket` fabricated; re-grounded on the live client four-ticket component.

## 815.1 — What the pure-JUnit + lint layer CANNOT reach `[CERT]`
- `run-pure-test.sh` classpath is `junit-4.13.2 + hamcrest-core-1.3` only, `-sourcepath <rt>/src`, runner
  `JUnitCore` — **no Baja jars, no NRE, no engine** (`run-pure-test.sh:7` header; [Block 743]). A test that
  `import javax.baja.*` fails at compile; it cannot instantiate a `BComponent` (needs the type registry),
  cannot call `Clock.schedule()` (needs `Nre.getEngineManager()`), cannot fire `started()`/`stopped()`/
  `atSteadyState()`, cannot observe a `Clock.Ticket`. `[CERT]`
- `lint-timers.sh` scans SOURCE TOKENS. It sees a `cancelRunTickets()` / `cancelTicket()` call is present, but
  cannot reason about WHICH tickets a method cancels, WHETHER a lifecycle callback exists at all, or WHETHER a
  scheduled ticket survives a stop or a defrost. Cross-method + cross-lifecycle invariants are invisible to a
  static token scan. `[CERT]`

## 815.2 — The harness: `BTest.createTestStation()` mounts a live station `[CERT]`
- `BTest extends BObject`; test methods are `public void test…()` discovered by reflection
  (`BTest.java:94` `m.getName().startsWith("test")`) — a `BTestNg` subclass adds the TestNG `@Test`/classloader
  hooks. `[CERT]`
- `BTest.createTestStation()` (`BTest.java:593`) returns a `TestStationHandler` (`:603`) that
  `implements AutoCloseable` (`:604`, `close()`→`releaseStation()` `:764-768`) — usable with try-with-resources.
  Its ctor builds a `BComponentSpace`, `new BStation()` named `"test"`, sets the hidden `Station.station` hook, and
  `space.setRootComponent(station)` to mount it (`:622-651`). `[CERT]`
- Add the component tree BEFORE start: the 3-arg `station.add(name, comp, null)` overload is the one BTest itself
  uses (`BTest.java:642`). `[CERT]`
- `startStation()` (`:668-680`): `Nre.clearPlatform()` → `loadPlatform()` → `startAllServices()` → `station.start()`
  → sets the `stationStarted`/`atSteadyState` FLAGS. `station.start()` cascades `started()` to the mounted children,
  so `isRunning()` becomes true. `[CERT]`
- `releaseStation()` (`:705`, via `stopStation()` `:685-696`) calls `station.stop()` (cascading `stopped()`),
  stops+clears services, unmounts, and deletes the test history dir. **Mandatory** (`:700-702`). `[CERT]`

## 815.3 — The `atSteadyState` catch is LOAD-BEARING here: BTest sets a FLAG, not the callback `[CERT]`
`TestStationHandler.startStation()` sets `Station.atSteadyState = true` as a bare flag (`BTest.java:679`) but does
**NOT** call `Nre.getEngineManager().atSteadyState(station)`. The full-stack `BTestNgStation` does BOTH
(`BTestNgStation.java:150-151`). This matters directly: in the client component the power-on stagger that schedules
`powerOnTicket` lives in `BEvaporatorUnit.atSteadyState()` (client `:810-825`). Under the bare `BTest` path that
callback never fires → `powerOnTicket` is never scheduled → a test asserting on it would be VACUOUS. So the test
MUST extend `BTestNgStation`, or call `Nre.getEngineManager().atSteadyState(handler.getStation())` explicitly after
`startStation()`. `[CERT]`

## 815.4 — There is NO deterministic test clock — the timer fires on WALL TIME `[CERT]`
- `Clock.schedule()`/`schedulePeriodically()` delegate straight to `Nre.getEngineManager()` (`Clock.java:171-260`).
  The platform provider is `private static final … AccessController.doPrivileged(…getPlatformProvider)`
  (`Clock.java:350`) — **no injection seam**. `[CERT]`
- `Clock.setTime(BAbsTime)` needs `NiagaraBasicPermission("SET_TIME")` and sets the real system clock
  (`Clock.java:124-137`); it is NOT a tick-advance for scheduled actions. No `TestClock`/`VirtualClock`/`MockClock`
  hook exists on `Clock`. `TestHelper.waitFor(…)` (`TestHelper.java:173`) is a real-time `Thread.sleep` poll loop. `[CERT]`
- **Consequence for the recipe:** do NOT wait for a timer to fire. Set the relevant delay LONG so the ticket does
  not expire during the test, drive the lifecycle transition, then read the `Clock.Ticket` HANDLE directly via
  `TestHelper.getPrivateField(evap, "powerOnTicket")` (`TestHelper.java:341`, reflection + `setAccessible`), and
  invoke the callback method directly (`evap.doPowerOnExpired()`) instead of waiting for the scheduled fire. This
  makes every assertion deterministic and wall-clock independent. `[CERT]`

## 815.5 — The two invariants worth a mounted test (client four-ticket lifecycle, PR#1/#2) `[CERT]`
Client `BEvaporatorUnit` (`origin/main` c66e412) holds FOUR tickets — `startDelayTicket`, `stopDelayTicket`,
`defrostEntryTicket`, `powerOnTicket` (`:1226-1229`) — and TWO cancellers: `cancelRunTickets()` (`:1195`, the three
run tickets only) and `cancelTicket()` (`:1203`, which calls `cancelRunTickets()` + also cancels `powerOnTicket`,
`:1205-1206`). The two lifecycle invariants a static scan cannot see:
1. **PR#2 — defrost PRESERVES `powerOnTicket`; `doPowerOnExpired` clears `startingUp` even during defrost.**
   `atSteadyState()` sets `startingUp = true` and schedules `powerOnTicket` (`:820,825`). `enterDefrost()`
   (`:1101`) calls `cancelRunTickets()` (`:1111`) — deliberately NOT `powerOnTicket` (comment `:1105-1110`: cancelling
   it would orphan `startingUp=true` and lock the unit in its hold after `exitDefrost()`). `doPowerOnExpired()`
   (`:1013`) sets `startingUp = false` (`:1015`) and then `if (inDefrost) return;` (`:1018`). A mounted test asserts:
   after `enterDefrost()` while `startingUp`, `powerOnTicket` is STILL non-null; after `doPowerOnExpired()`,
   `startingUp` is false. `lint-timers.sh` sees the `cancelRunTickets()` token either way — it cannot tell that
   `powerOnTicket` must survive. This is QA's recorded gap. `[CERT]`
2. **PR#1 — `stopped()` cancels ALL four tickets and clears the transient flags.** `stopped()` (`:849`) calls
   `cancelTicket()` (all four, `:856`) then clears `startingUp`/`inDefrost`/`freezeTripped`/`lastCmd` (`:864-867`).
   This closed the [Block 787] lint flag (a scheduled ticket firing on a stopped component). A mounted test proves
   it: schedule tickets, `stopStation()`, assert all four handles null + `startingUp==false` + `inDefrost==false`. `[CERT]`

## 815.6 — Copy-ready component-lifecycle test shape `[CERT + INFER]`
Test class lives in package `com.angeles.ColdRoomPan` (the driven `enterDefrost()`/`exitDefrost()` are
package-private; `doPowerOnExpired()` is public `:1013`). `[CERT]`

```java
package com.angeles.ColdRoomPan;                 // same package: reach package-private hooks [CERT]

import javax.baja.sys.*;
import javax.baja.status.BStatusBoolean;
import javax.baja.test.BTest;
import javax.baja.test.TestHelper;
import com.tridium.sys.Nre;
import org.testng.annotations.Test;

public class BEvaporatorUnitLifecycleTest extends javax.baja.test.BTestNg   // TestNG runner [CERT]
{
  /** PR#2 guard: defrost preserves powerOnTicket; doPowerOnExpired clears startingUp even in defrost. */
  @Test
  public void testDefrostPreservesPowerOnTicket() throws Exception
  {
    try (BTest.TestStationHandler h = BTest.createTestStation())            // AutoCloseable [CERT BTest:593,764]
    {
      BStation station = h.getStation();
      BColdRoom room = new BColdRoom();
      station.add("Room1", room, null);                                    // 3-arg add [CERT BTest:642]
      BEvaporatorUnit evap = new BEvaporatorUnit();
      evap.setHasDefrost(true);                                            // enable defrost path [CERT :1102]
      evap.setPowerOnDelay(BRelTime.make(600_000));                        // 10 min: powerOnTicket won't fire [CERT §815.4]
      room.add("Evap1", evap, null);

      h.startStation();                                                    // started() cascade [CERT :668-680]
      Nre.getEngineManager().atSteadyState(station);                       // REQUIRED: fires the power-on stagger [CERT §815.3]

      verify(TestHelper.getPrivateField(evap, "powerOnTicket") != null,    // scheduled at atSteadyState :825
             "atSteadyState must schedule powerOnTicket");
      verify((Boolean) TestHelper.getPrivateField(evap, "startingUp"),     // :820
             "startingUp must be true during the power-on stagger");

      evap.enterDefrost();                                                 // package-private [CERT :1101]
      verify(TestHelper.getPrivateField(evap, "powerOnTicket") != null,    // PR#2: preserved, NOT cancelled [CERT :1111]
             "enterDefrost must PRESERVE powerOnTicket (else startingUp orphans)");

      evap.doPowerOnExpired();                                             // simulate the ticket firing [CERT :1013]
      verify(!(Boolean) TestHelper.getPrivateField(evap, "startingUp"),    // cleared even in defrost [CERT :1015-1018]
             "doPowerOnExpired must clear startingUp even while inDefrost");
    }                                                                       // releaseStation() auto [CERT :764-768]
  }

  /** PR#1 guard: stopped() cancels ALL four tickets and clears the transient flags. */
  @Test
  public void testStopCancelsAllTickets() throws Exception
  {
    try (BTest.TestStationHandler h = BTest.createTestStation())
    {
      BStation station = h.getStation();
      BEvaporatorUnit evap = new BEvaporatorUnit();
      evap.setPowerOnDelay(BRelTime.make(600_000));
      station.add("Evap1", evap, null);
      h.startStation();
      Nre.getEngineManager().atSteadyState(station);                       // schedules powerOnTicket
      verify(TestHelper.getPrivateField(evap, "powerOnTicket") != null, "ticket scheduled");

      h.stopStation();                                                     // cascades stopped() [CERT :685-691, :849]
      for (String f : new String[]{"startDelayTicket","stopDelayTicket","defrostEntryTicket","powerOnTicket"})
        verify(TestHelper.getPrivateField(evap, f) == null,               // all four cancelled [CERT :856,:1203-1206]
               "stopped() must cancel " + f);
      verify(!(Boolean) TestHelper.getPrivateField(evap, "startingUp"), "stopped clears startingUp");  // :864
      verify(!(Boolean) TestHelper.getPrivateField(evap, "inDefrost"),  "stopped clears inDefrost");   // :865
    }
  }
}
```
`[INFER]` only on: the exact reflection field names as strings (they match the source `:1226-1229` / `startingUp`
`:1225`), and that `BColdRoom.add(evap)` needs no extra wiring for these two paths (both are driven directly).

## 815.7 — Toolbelt & gradle change (what actually ships) `[CERT + INFER]`
- **No WSL-runnable `run-station-test.sh` is possible.** `niagaraTest` (the native runner) needs `bin/test` + a dev
  license and discovers 0 tests from WSL ([Block 807]; `run-pure-test.sh:4-5`). A lifecycle test is AUTHORED in the
  `moduleTest` source set, COMPILED in WSL by `./gradlew :<mod>-rt:moduleTestJar`, but EXECUTED only on a Windows
  dev host or a JACE. So the kit deliverable is a `BUILD-LOOP` note + a `build.sh` `moduleTestJar` step, not a WSL
  runner. `[CERT]`
- **Gradle GAP (must be closed before the test compiles):** the kit exemplar `coldRoom-rt/build.gradle.kts`
  (48 lines) has the `com.tridium.niagara-module` plugin (which defines `moduleTestJar`, comment `:11`) but declares
  **no `test-wb` test dependency** (`:34-48`). `TestHelper`/`BTest`/`BTestNg` live in `test-wb`; the `moduleTest`
  source set needs a `testImplementation("Tridium:test-wb:4.14.0")` (coordinate per SDK-home convention). Any module
  authoring a lifecycle test — the client `ColdRoomPan-rt` included — needs the same. `[CERT]` the exemplar lacks it;
  `[INFER]` the exact coordinate string.
- **`build.sh`:** add a `:<mod>-rt:moduleTestJar` step after the main jar so the test set is compiled (a compile-only
  gate in WSL — it catches API drift even though it cannot run). `[INFER]`

## 815.8 — Lint vs. lifecycle-test split (which layer owns which check) `[CERT-grounded]`
| Check | `lint-timers.sh` (static) | Mounted lifecycle test (`niagaraTest`) |
|---|---|---|
| `cancelRunTickets()`/`cancelTicket()` token present | YES | YES |
| Defrost PRESERVES `powerOnTicket` (PR#2, else `startingUp` orphans) | NO (cannot tell which tickets a method drops) | YES (reflection read) |
| `doPowerOnExpired()` clears `startingUp` even in defrost | NO | YES (direct callback + read) |
| `stopped()` cancels ALL four tickets + clears flags (PR#1) | WARN on missing `stopped()` token only | YES — asserts all four null |
| Timer FIRES and energizes an output after delay | NO | YES (real-time poll; slow/flaky, §815.4) |
Rule of thumb: **lint gates the PRESENCE of a cancel; the mounted test gates WHICH tickets it drops, across methods
and lifecycle.** Ship both — they are not redundant.

## 815.9 — Kit implication `[CERT-grounded]`
1. `BUILD-LOOP`: add a **test layer** — pure-JUnit decision core ([Block 743]) for algebra, PLUS a `moduleTest`
   `BTestNg` station-lifecycle test for mount/timer/stop invariants; `build.sh` gains a `moduleTestJar` compile step;
   document that execution is native/JACE-only (WSL = compile-gate).
2. `types/logic.md`: a "lifecycle test recipe" — `createTestStation()` + try-with-resources + `atSteadyState()` via
   the engine + drive/invoke-the-callback + read-the-ticket-handle-by-reflection (never wait on wall-clock timers) +
   assert the defrost-preserves-power-on and stop-cancels-all invariants.
3. Scaffold: the `-rt` gradle template should ship the `testImplementation` on `test-wb` so a lifecycle test compiles
   out of the box (closes the §815.7 gap for every future module).

## 815.10 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | pure-JUnit (`run-pure-test.sh`) has no Baja/engine → cannot mount a component or read a Ticket | `[CERT]` | `run-pure-test.sh:7`; [B743] | Y — read |
| 2 | `createTestStation()`→`TestStationHandler` (AutoCloseable) mounts a live `BStation`; 3-arg `add` used by BTest | `[CERT]` | `BTest.java:593,603,642,651,764-768` | Y — read |
| 3 | `startStation()` sets `atSteadyState` FLAG only; the client power-on stagger is in `atSteadyState()` → test must fire the engine callback | `[CERT]` | `BTest.java:668-680`; `BTestNgStation.java:150-151`; client `:810-825` | Y — read |
| 4 | No deterministic test clock; `Clock` provider is `private static final` via doPrivileged; `setTime` needs SET_TIME | `[CERT]` | `Clock.java:124-137,171-260,350` | Y — read |
| 5 | Client `BEvaporatorUnit` has FOUR tickets; `enterDefrost` preserves `powerOnTicket` (`cancelRunTickets` only); `stopped()` cancels all four + clears flags | `[CERT]` | client `origin/main` c66e412 `:825,849,856,864-865,1013-1018,1101,1111,1195,1203-1206,1226-1229` | Y — `git show` |
| 6 | Reflection read of a private field via `TestHelper.getPrivateField(Object,String)` | `[CERT]` | `TestHelper.java:341` | Y — read |
| 7 | Exemplar `coldRoom-rt/build.gradle.kts` declares no `test-wb` test dependency → moduleTest won't compile as-is | `[CERT]` | `build.gradle.kts:11,34-48` | Y — read |
| 8 | `niagaraTest` = 0 tests from WSL; lifecycle test is compile-only in WSL, runs native/JACE | `[CERT]` | [B807]; `run-pure-test.sh:4-5` | Y — REMITTANCE |
| 9 | The two-test shape guards the PR#1/#2 invariants lint/pure-JUnit miss (QA's recorded gap) | `[INFER]` | §815.5/§815.6, composes rows 1-7 | recipe |

**Tally:** `[CERT]` ×8 · `[INFER]` ×1. The one `[INFER]` is the assembled recipe over eight verified primitives (§11).
**Correction log (§14):** the first pass of this block read the research-repo exemplar
`modules-src/coldRoom/coldRoom-rt/.../com/oem/coldRoom/BEvaporatorUnit.java` (328 lines, ONE `startDelayTicket`,
no `stopped()`) and, on that basis, wrongly declared `powerOnTicket`/`cancelRunTickets` "fabricated". That file is a
DIFFERENT, simplified `com.oem` exemplar — not the live component. investigador flagged the error; verified by
`git show` in the client Leon-Guanjuato repo: `com.angeles.ColdRoomPan.BEvaporatorUnit` declares four tickets on
BOTH the pre-fix tree (4f5f1c7) and `origin/main` (c66e412, after client PR #1/#2). This block is re-grounded on the
client `origin/main`. Lesson: for the LIVE invariant, open the client tree, not a same-named research exemplar.

## 815.11 — Connections & open gaps
- REMITTANCE: [Block 805] §805.8 (test layer in the build loop), [Block 807] (build-task matrix, `niagaraTest`
  0-from-WSL), [Block 787] (the timer-cancel lint whose flag `stopped()` closed), [Block 762] (timer-cancel
  discipline), [Block 743] (pure-JUnit standalone), [Block 801] (Clock delay floor). Pairs with [Block 812]
  (author-built liveness watchdog — the runtime analogue of this test's stop-invariant).
- **B815-G1** (build/PoC): **BUILD half CLOSED [CERT-live]** (§815.12, 2026-09-05). The RUN half stays
  requires-execution: `niagaraTest` needs the native runner on a Windows dev host / JACE (blocked in WSL, §815.12).
  The intended proof still open: run BOTH tests GREEN on the fixed tree, then on the pre-fix tree (4f5f1c7) confirm
  both FAIL — regression-guarding PR #1/#2.
- **B815-G2**: the `test-wb` moduleTest dep is now present on the client `ColdRoomPan-rt`; §815.12 found it is
  NECESSARY-BUT-NOT-SUFFICIENT (junit was also missing). Coordinate still to confirm for the scaffold template.

## 815.12 — EXECUTED evidence (§19 build-PoC, 2026-09-05) `[CERT-live]`
Client repo `Leon-Guanjuato` worktree `poc/lifecycle-btest` off `origin/main` deed38c; commit **c271d36** (local,
unpushed). Authored `BColdRoomLifecycleTest` (the §815.6 two-test shape, `extends BTestNg`) in
`ColdRoomPan-rt/srcTest/test/com/angeles/ColdRoomPan/`. Built against a read-only mirror of the Honeywell 4.14
install (`mirror-niagara-home.sh …OptimizerSupervisor-N4.14.0.162 ~/niagara-mirror-hon414`, 406 jars), JDK 8, plugin
7.6.17 ([Block 807] task matrix).
- **BUILD — GREEN `[CERT-live]`.** `./gradlew :ColdRoomPan-rt:moduleTestJar` → `BUILD SUCCESSFUL`:
  `compileModuleTestJava` + `writeTestModuleXml` + `moduleTestJar` all ran; produced `ColdRoomPan-rtTest.jar`
  (12 415 B). So the §815.6 station-test SHAPE compiles against the real Baja API. This CLOSES the build half of the
  [Block 790]/§815 "scaffold is buildable" claim for a station-lifecycle test.
- **Correction to §815.7 `[CERT-live]`.** The dep gap is NOT just `test-wb` (which `ColdRoomPan-rt` already declares,
  `moduleTestImplementation(":test-wb")`). The FIRST build FAILED with 47 errors `cannot find symbol: class Test` in
  the SIBLING pure-JUnit tests (`ColdRoomControlTest`/`…DelayTest`/`WritePathTest`, `import org.junit.Test`) —
  because the module never declared junit as a moduleTest dep. The pure tests had only ever been run via
  `run-pure-test.sh` (external cache junit, [Block 743]); `moduleTestJar` had never compiled the mixed `srcTest`.
  Adding `moduleTestImplementation("junit:junit:4.13.2")` (from `mavenCentral`, root repos) made it GREEN. So a
  module mixing pure-JUnit and Baja station tests in one `srcTest` needs BOTH `test-wb` AND `junit` on the moduleTest
  classpath — or separate source sets. (`CompPan-rt` has the same single-`:test-wb` gap.)
- **RUN — BLOCKED (the precise wall) `[CERT-live]`.** `./gradlew :ColdRoomPan-rt:niagaraTest` FAILED with:
  `A problem occurred starting process 'command '/home/cristian/niagara-mirror-hon414/bin/test''`. `file bin/test.exe`
  → `PE32+ executable (console) x86-64, for MS Windows`. The native test runner is a WINDOWS binary; WSL Linux cannot
  launch it. En route, three hardcoded Windows paths in the client `gradle.properties` also had to be overridden
  (`niagara_user_home`, `nodeHome`, and `niagara_home`→the Linux mirror) — each surfaced as a
  `URISyntaxException: Illegal character … C:\…`. This is the executed confirmation of [Block 807]/§815.7's
  "`niagaraTest` = 0 from WSL, native/JACE only": the blocker is the native `test.exe` runner (+ a dev license/
  station), reached only after the moduleTest jar built cleanly.
- **Net:** the test COMPILES and PACKAGES in WSL (a real compile-gate for API drift); it EXECUTES only on a Windows
  host / JACE. The RED→GREEN regression proof (B815-G1) awaits that host.
