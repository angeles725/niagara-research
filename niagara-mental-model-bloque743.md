# B743 · Testing timer-arming & lifecycle — why the math seam isn't enough, the scheduler-seam pattern, `BTestNgStation`, and the layered answer for WSL (closes B741-G2)

> **Scope**: how to actually catch a "timer never armed on late-mount" bug — the class of failure that pure
> unit tests pass green (B741-G2). What Tridium offers (`BTestNgStation`), why it's not WSL-runnable, and the
> practical layered answer (math seam + scheduler seam + live smoke). Foco: `own-modules-audit`.
>
> **Sources**: FUENTE 3 docSource `extracted/test-wb/javax/baja/test/{BTestNgStation,BTest}.java`,
> `extracted/baja/javax/baja/sys/Clock.java` (read this session). FUENTE 1: bloque TI (test infra), B729
> (lifecycle), B741 (QA plan, G2). Our `CompressorControlTest`/`DefrostControl` PoC.

---

## 743.1 — Why the math seam alone can't catch it `[CERT]`
Our pure tests are testable because the logic takes TIME AS A PARAMETER: `CompressorControl.step(now, …)`,
`DefrostControl.nextDelayMs(lastMs, now, interval)`. The test advances `now` itself and calls the method —
so it verifies the MATH assuming something calls it. It lives BELOW the timer. The late-mount bug is that
`started()`/`atSteadyState()` never called `Clock.schedule` → the math never runs → nothing to assert on.
No amount of math testing reaches it (B741-G2).

`Clock` is the reason you can't just mock it: it is a **static singleton** backed by
`PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE` (`Clock.java:24-49`); `Clock.millis()` =
`System.currentTimeMillis()`, `Clock.schedule(...)` delegates to `Nre.getEngineManager()`. Not injectable
per-instance, and `Clock.setTime` sets the REAL system clock — not a unit-test seam.

## 743.2 — Tridium's tool: `BTestNgStation` (real station, not WSL) `[CERT]`
`BTestNgStation` (test-wb) is the lifecycle-test tool: `setupStation()` boots a real `BStation`
(`stationHandler.getStation()` + `configureTestStation(station, name, webPort, foxPort)`), you add your
component, and **the framework actually runs `started()`/`atSteadyState()`** — so you CAN assert the ticket
armed / the anchor populated. But it needs the **NRE kernel + the native harness (`bin/test.exe`) + a dev
license** → it does NOT run in WSL, and plugin 7.6.17's `niagaraTest` discovers 0 tests anyway (bloque TI).
So the idiomatic lifecycle test exists, but is unavailable in our WSL flow — only on a Windows dev box.

## 743.3 — The layered answer for us `[CERT/INFER]`
Three layers, each catching a different slice; none alone is enough:
1. **Math seam (pure JUnit, WSL)** — time as a parameter. Catches arming-math bugs (the
   future-`lastDefrostTime` giant delay, remaining-interval, terminate-by-temp). Done: DefrostControl PoC +
   the 63 existing tests.
2. **Scheduler seam (pure-ish JUnit, WSL) — the new recommendation** — don't call `Clock.schedule` directly
   in the arming method; call a tiny interface, e.g. `interface Sched { Object at(long delayMs); void cancel(Object t); }`.
   Production wires a Clock-backed impl; a test wires a FAKE that RECORDS `at(delayMs)` calls. Then
   `arm(mode, lastMs, now, interval, sched)` is unit-testable: assert "interval mode with null last → sched.at
   called once with `interval`," "schedule mode → sched NOT called," "re-arm cancels the prior ticket." This
   moves the "which path arms, with what delay, and does it cancel-before-reschedule" logic (B730 §730.10)
   BELOW a testable seam — catching most arming bugs WITHOUT a station. It still cannot prove the FRAMEWORK
   calls `started()`/`atSteadyState()` — that is framework contract, not our code.
3. **Live smoke (the JACE) — the only proof of the wiring** — after a mount/restart, read back the anchor
   (`nextDefrostTime` populated in interval mode; the compressor tick advancing). This is the sole check that
   the framework actually invoked our lifecycle hooks and they armed. It is the mandatory close for the
   late-mount class (the cold-boot test is exactly this).

The residual truth: **no unit test proves Baja invokes `started()`/`atSteadyState()`** — that's the framework
contract (documented [CERT] in B729 from BComponent javadoc) plus the live smoke. Our job is (a) make the arm
DECISION testable (seams), and (b) always run the smoke readback after a lifecycle-affecting deploy.

## 743.4 — Recommendation
- Keep the math seam (done). **Add the scheduler seam** when we extract `DefrostControl`/refactor
  `armTrigger` — so arm-path + cancel-before-reschedule are unit-tested, not just the delay math.
- Institutionalize the **lifecycle smoke readback** as a required post-deploy step (a QA-plan row, B741 §741.1.3):
  after any change to `started()`/`atSteadyState()`/arming, mount/restart and confirm the anchor populates.
- If we ever get a Windows dev box + dev license, a `BTestNgStation` test would automate layer 3.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Our pure tests inject time as a parameter (step(now)/nextDelayMs(...,now,...)) → below the timer | [CERT] | CompressorControlTest drive(); DefrostControl PoC |
| 2 | Clock is a static platform singleton; not per-instance mockable; setTime sets the real clock | [CERT] | Clock.java:24-49,124 |
| 3 | BTestNgStation boots a real station and runs started()/atSteadyState → can assert arming; needs kernel+harness+dev license (not WSL) | [CERT] | BTestNgStation.java:94-119; bloque TI |
| 4 | A scheduler seam makes the arm decision (path/delay/cancel) unit-testable in WSL without a station | [INFER] | standard DI seam; matches how the math seam already works |
| 5 | No unit test proves the framework CALLS started()/atSteadyState — that needs BTestNgStation or live smoke | [CERT/INFER] | framework contract (B729) + §743.2/743.3 |

**Tally**: 3 [CERT], 2 [CERT/INFER]. No unmarked claims.

## Connections
- **B741** (QA plan; this closes G2), **B729** (lifecycle contract), **B730** §730.10 (cancel-before-reschedule),
  bloque TI (test infra / niagaraTest limits), B742 (plan — add the seam in the hardening batch).

## Open gaps
- **B743-G1**: concrete `Sched` seam signature + a fake-recorder test for `armTrigger` — an implementation
  task, to do alongside the DefrostControl extraction.
