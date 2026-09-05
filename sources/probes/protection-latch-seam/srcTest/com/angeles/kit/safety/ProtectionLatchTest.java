package com.angeles.kit.safety;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Biting tests for {@link ProtectionLatch} — each pins ONE contract invariant and kills a
 * specific naive-implementation mutation (noted per test). Pure JUnit 4, no Baja, runs via
 * the kit's run-pure-test.sh (JDK 8 + JUnitCore).
 */
public class ProtectionLatchTest {

    /** T1: starts CLEAR with no first-out. */
    @Test public void startsClear() {
        ProtectionLatch l = new ProtectionLatch();
        assertFalse(l.isTripped());
        assertNull(l.getFirstOutReason());
    }

    /** T2: a trip condition sets the latch and captures the first-out reason + time. */
    @Test public void tripSetsAndCapturesFirstOut() {
        ProtectionLatch l = new ProtectionLatch();
        assertTrue(l.step(true, false, "HP switch", 1000L));
        assertTrue(l.isTripped());
        assertEquals("HP switch", l.getFirstOutReason());
        assertEquals(1000L, l.getFirstOutMillis());
    }

    /** T3: SET-DOMINANT — trip AND reset in the same cycle stays tripped. Kills a reset-dominant mutation. */
    @Test public void setDominatesReset() {
        ProtectionLatch l = new ProtectionLatch();
        assertTrue(l.step(true, true, "HP switch", 1000L));
        assertTrue(l.isTripped());
    }

    /** T4: FIRST-OUT held — a second, different trip while tripped must NOT overwrite it. Kills a re-capture mutation. */
    @Test public void firstOutIsNotOverwritten() {
        ProtectionLatch l = new ProtectionLatch();
        l.step(true, false, "HP switch", 1000L);
        l.step(true, false, "LP switch", 2000L);
        assertEquals("HP switch", l.getFirstOutReason());
        assertEquals(1000L, l.getFirstOutMillis());
    }

    /** T5: reset BLOCKED while the trip condition is still active. Kills a "reset ignores condition" mutation. */
    @Test public void resetBlockedWhileConditionActive() {
        ProtectionLatch l = new ProtectionLatch();
        l.step(true, false, "HP switch", 1000L);
        assertTrue("reset must not clear through an active fault",
                   l.step(true, true, "HP switch", 1500L));
        assertTrue(l.isTripped());
    }

    /** T6: reset HONORED only with the condition clear AND a reset requested. Kills an auto-reset / reset-without-request mutation. */
    @Test public void resetHonoredWhenConditionClear() {
        ProtectionLatch l = new ProtectionLatch();
        l.step(true, false, "HP switch", 1000L);
        assertTrue("latches: condition clears but no reset yet", l.step(false, false, null, 1500L));
        assertFalse("explicit reset with condition clear -> clears", l.step(false, true, null, 2000L));
        assertFalse(l.isTripped());
        assertNull(l.getFirstOutReason());
    }

    /** T7: NO RE-TRIP CHATTER — stays clear after reset; re-asserting re-trips with a FRESH first-out. Kills a chatter/stale-first-out mutation. */
    @Test public void noRetripChatterThenReTripsFresh() {
        ProtectionLatch l = new ProtectionLatch();
        l.step(true, false, "HP switch", 1000L);
        l.step(false, true, null, 2000L);
        assertFalse(l.step(false, false, null, 2500L));
        assertTrue(l.step(true, false, "LP switch", 3000L));
        assertEquals("LP switch", l.getFirstOutReason());
        assertEquals(3000L, l.getFirstOutMillis());
    }

    /** T8: a bare reset with no prior trip is a harmless no-op. */
    @Test public void resetWithoutTripIsNoop() {
        ProtectionLatch l = new ProtectionLatch();
        assertFalse(l.step(false, true, null, 1000L));
        assertFalse(l.isTripped());
    }
}
