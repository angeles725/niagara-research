package com.angeles.kit.liveness;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.angeles.kit.liveness.LivenessDecision.Liveness.*;

/**
 * Biting tests for {@link LivenessDecision} — each pins one contract invariant. Pure JUnit 4; runs via the
 * kit run-pure-test.sh. Named mutation proven: `age > threshold` → `age >= threshold` makes T3 (boundary) fail.
 */
public class LivenessDecisionTest {

    private final LivenessDecision d = new LivenessDecision();
    private static final long P = 1000L;   // period
    private static final int  F = 3;       // factor → stall above 3000

    /** T1: a fresh tick (age &lt; period), not previously stalled → LIVE. */
    @Test public void freshIsLive() {
        assertEquals(LIVE, d.step(500L, 0L, P, F, false));     // age 500 < 1000
    }

    /** T2: age above factor×period → STALLED. */
    @Test public void oldIsStalled() {
        assertEquals(STALLED, d.step(3001L, 0L, P, F, false)); // age 3001 > 3000
    }

    /** T3: BOUNDARY — age exactly factor×period is NOT stalled (strict &gt;). Kills the `>=` chatter mutation. */
    @Test public void boundaryIsNotStalled() {
        assertEquals(LIVE, d.step(3000L, 0L, P, F, false));    // age == 3000, not > 3000
    }

    /** T4: a producer that never ticked (lastTick = arm-time) ages into a stall after grace. */
    @Test public void neverTickedStallsAfterGrace() {
        long arm = 10_000L;
        assertEquals(STALLED, d.step(arm + 4 * P, arm, P, F, false)); // age 4000 > 3000
    }

    /** T5: RECOVERY EDGE — previously stalled, a fresh tick drops age below one period → RECOVERED (once). */
    @Test public void recoveryIsAnEdge() {
        assertEquals(RECOVERED, d.step(500L, 0L, P, F, true));  // prevStalled + age 500 < 1000
    }

    /** T6: HYSTERESIS — previously stalled and age in the band (period ≤ age ≤ factor×period) → HOLD stalled, no chatter. */
    @Test public void hysteresisHoldsInBand() {
        assertEquals(STALLED, d.step(2000L, 0L, P, F, true));   // prevStalled + age 2000 (1000..3000) → hold
    }

    /** T7: clock jump BACKWARDS (now &lt; lastTick, negative age) → LIVE, never a false stall. */
    @Test public void backwardClockIsLiveNotStall() {
        assertEquals(LIVE, d.step(500L, 5000L, P, F, false));   // age = -4500 → clamped to 0 → LIVE
        // even if it was stalled, a backward jump must not read as still-stalled-forever via a huge age:
        assertEquals(RECOVERED, d.step(500L, 5000L, P, F, true)); // prevStalled + age 0 < period → recover
    }

    /** T8: after the RECOVERED edge, a plain live cycle is LIVE (edge is one-shot). */
    @Test public void afterRecoveryIsLive() {
        assertEquals(LIVE, d.step(400L, 0L, P, F, false));      // not prevStalled anymore → LIVE
    }
}
