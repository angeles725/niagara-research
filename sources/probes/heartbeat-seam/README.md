# Heartbeat/liveness decision seam — C9 fixture (§19 build-PoC)

Pure-Java liveness decision from B812 (the Baja monitor is a thin adapter around this). No Baja deps.
- `src/com/angeles/kit/liveness/LivenessDecision.java` — `step(nowMs, lastTickMs, periodMs, factor, prevStalled)` → LIVE / STALLED / RECOVERED.
- `srcTest/.../LivenessDecisionTest.java` — 8 biting JUnit 4 tests.

Contract: STALLED when age > factor×period (strict, never at the boundary) · RECOVERED is an EDGE (first tick
after a stall, age < period) · hysteresis holds through the band period..factor×period (no chatter) · a
never-ticked producer ages into a stall from arm-time · a backward clock jump (negative age) is treated as a
fresh tick (fail-safe alive, never a false stall — B775 §775.6 clockChanged / B801 time handling).

Run (JDK 8 + JUnitCore): build-n4-module-kit/toolbelt/run-pure-test.sh <this-dir> com.angeles.kit.liveness.LivenessDecisionTest
Result: OK (8 tests). Mutation-proven: `age > threshold` → `age >= threshold` fails T3 (boundaryIsNotStalled).

B812 wrapper shape: a `lastTick` TRANSIENT slot the producer stamps each cycle; a monitor component calls
step() on schedulePeriodically (period = factor×producer-period, floored ≥1s per B801); on STALLED it sets the
BStatus fault bit + raises a BAlarmRecord via BAlarmService (B805 §805.4); on RECOVERED it clears them.
