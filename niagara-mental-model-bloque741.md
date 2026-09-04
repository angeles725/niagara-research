# B741 · QA / test plan for our modules — the 4-layer assurance stack, what we already cover (63 pure tests), and the one dangerous gap (defrost logic is untested)

> **Scope**: a concrete quality-assurance plan for ColdRoomPan / CompPan / DashboardPan, applying Tridium's
> testing model (bloque TI) to our reality (WSL, no `niagaraTest`). Inventories what we test today and names
> the highest-value gap. Foco: `own-modules-audit`.
>
> **Sources**: FUENTE 1 bloque TI (test infra + the 7.6.17 niagaraTest bug), B730 §730.7 (template-method
> pure logic), B729/B739/B740 (the incidents these tests would guard). FUENTE 3 our source tree + srcTest
> (read this session). Kit `build-verify.md`, hardening bitácora (pure-logic + JUnit-in-WSL recipe).

---

## 741.1 — The 4-layer assurance stack `[CERT/INFER]`
Tridium's own gate is `BTestNg` run by `niagaraTest` inside a station — which **does not run in our WSL**
(needs Windows `bin/test.exe` + a dev license; and plugin 7.6.17 discovers 0 tests → "tests-are-docs",
bloque TI). So our runnable assurance is a stack:
1. **Pure unit tests (JUnit, WSL-runnable)** — the load-bearing layer. Extract decision/safety logic into a
   pure class with ZERO Baja types (template-method, B730 §730.7); the `BComponent` is a thin adapter that
   reads slot value+status and delegates. Compile+run with plain `javac -source 8` + cached
   `junit-4.13.2.jar` — no station (hardening recipe).
2. **Build-verify gate** — `build-verify.md`: Java-8 compile + Slotomatic + signing ALL PASS before deploy.
   Catches type/slot/signing errors; not behavior.
3. **Live smoke test** — behavior on the station that pure tests can't reach (lifecycle, links, field IO):
   the disciplined cold-boot / output-sequence checklist (e.g. the defrost cold-boot test: Modo=Intervalo +
   Save + restart + read Modo AND nextDefrostTime; watch valve→fan→resistance sequence). This is where
   timing/commissioning bugs (B729/B737) surface.
4. **Adversarial review** — judgment-day / code review before deploy (already used on the proceso+timers work).

## 741.2 — What we cover today `[CERT]`
Three pure classes with real suites (63 `@Test`, all WSL-runnable):
| Pure class | Module | Tests | Covers |
|---|---|---|---|
| `ColdRoomControl` | ColdRoomPan-rt | **21** | cooling decision (`decideCall`/`computeCall`), hysteresis band, `freezeTrip`, sensor-fault posture |
| `CompressorControl` | CompPan-rt | **28** | staging, floating-suction/R404A dew-point math, start-prove, lead/lag |
| `DashboardDispatch` (+`DashboardRbacHelper`,`JsonUtil`) | DashboardPan-ux | **14** | servlet routing, RBAC, JSON contract |
The template-method split works: the Baja component stays a thin adapter, the logic is a `final class` with
static methods. This is the correct, proven pattern for us.

## 741.3 — The dangerous gap: defrost logic is untested `[CERT]`
`BDefrostController` is `public class … extends BComponent` — its logic (interval arming, the FIFO interlock,
terminate-by-temp, staggering) is **INLINE in the Baja component. There is NO `DefrostControl` pure class and
NO test.** This is exactly the subsystem that shipped the `started()`/interval bugs (B729) and the
future-`lastDefrostTime` giant-delay edge case (B729-G2). Unit tests would have caught the arming-math
errors before the field.

**Plan — extract `DefrostControl` (pure) + test it**:
- `long nextDelayMs(Long lastMs, long nowMs, long intervalMs)` — the armTrigger math: null last → full
  interval; `elapsed<interval` → remaining; overdue → 0; **and the §B729-G2 guard**: `|elapsed|>3·interval`
  → interval (kills the future/skewed-clock giant delay). Tests: never-defrosted, mid-interval, overdue,
  future-timestamp, interval=0.
- The interlock as a pure state machine — `request(i)/begin/queue(FIFO)/terminate/staggerExpired` transitions
  over an in-memory model (no Baja): tests for the dropped-3rd-request fix, dedup, stagger ordering,
  hasDefrost filtering.
- `boolean terminateOnTemp(double resistTemp, double threshold, boolean enabled, boolean sensorValid)` —
  terminate-by-temp decision.
The Baja `BDefrostController` keeps the `Clock.schedule`/slot wiring; the decisions move to `DefrostControl`.

## 741.4 — Per-module test targets (concrete backlog)
| Module | Add | Priority |
|---|---|---|
| ColdRoomPan | **`DefrostControl` pure class + tests** (interval math + interlock + terminate-temp) | **HIGH** (untested safety-relevant subsystem) |
| ColdRoomPan | freeze-stat edge cases if not already in the 21 (trip/clear hysteresis, sensor fault) | MED |
| CompPan | confirm start-prove + lead/lag-by-hours are in the 28 (the tick logic that late-mount froze, B737) | MED |
| all | keep the build-verify gate ALL-PASS + the live smoke checklist per deploy | ongoing |

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | niagaraTest doesn't run in WSL (Windows harness + dev license; 7.6.17 discovers 0 tests) | [CERT] | bloque TI |
| 2 | We have 3 pure test suites, 63 @Test total (ColdRoomControl 21, CompressorControl 28, DashboardDispatch 14) | [CERT] | srcTest files, @Test counts |
| 3 | The template-method split (pure class + Baja adapter) runs in WSL via javac+junit | [CERT] | hardening recipe; ColdRoomControl |
| 4 | BDefrostController logic is inline Baja — no pure class, no test | [CERT] | BDefrostController.java (extends BComponent); no DefrostControl class found |
| 5 | Extracting DefrostControl + tests would have guarded the started()/interval/future-timestamp bugs | [INFER] | B729/B729-G2; the bug was in armTrigger math |

**Tally**: 4 [CERT], 1 [INFER]. No unmarked claims.

## Connections
- **bloque TI** (test infra), **B730** §730.7 (pure-logic split), **B729/B739/B740** (the incidents tests
  would guard), **B731/B737** (module audit + composition). Kit `build-verify.md`, `types/logic.md`.

## 741.5 — PoC: DefrostControl extracted + tested (done) `[CERT]`
A Java-8 pure `DefrostControl` PoC was written and run (scratchpad, not yet in the module): `nextDelayMs`
(interval math incl. the `|elapsed|>3·interval` anti-future-clock guard), `terminateOnTemp`, and the FIFO
`Interlock` — **11 JUnit tests PASS, bytecode major 52 (Java 8)**. Proves the extraction is viable and the
arming math (the subsystem that shipped the bug) is now executably verified. Next: land `DefrostControl` in
ColdRoomPan-rt and have `BDefrostController` delegate (coordinate with Codig's build).

## Open gaps
- **B741-G1** — **CLOSED** (Codig verified): the CompressorControl 28 tests DO cover the start-prove +
  lead/lag-by-hours MATH (`ctl.step()` with `now` advanced: `proofOfRun_faultsAndRestagesAnother`,
  `stuckContactor_flagged`, `rampDown_shedsMostHoursFirst`, `stageUp_picksLeastHoursCompressor`).
- **B741-G2** — **Pure tests never catch a timer-NOT-ARMED bug.** Codig's key QA insight: the 28 CompPan
  tests (and any pure DefrostControl tests) exercise the logic ASSUMING something calls `step()`/`nextDelayMs`
  with `now` advanced — they live BELOW the timer. The late-mount bug (tick/interval never arms → the pure
  logic never runs → rotation/prove/defrost frozen) passes ALL pure tests green. This needs a distinct
  **lifecycle verification** (that `started()`/`atSteadyState()` actually arm the ticket) — a live readback
  of the anchor after a mount, or a harness that asserts the ticket exists — NOT a pure unit test. Add it as
  its own QA row (smoke layer §741.1.3).
