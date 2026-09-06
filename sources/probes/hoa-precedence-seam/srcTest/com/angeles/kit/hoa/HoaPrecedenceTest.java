package com.angeles.kit.hoa;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.angeles.kit.hoa.HoaPrecedence.Mode.*;
import static com.angeles.kit.hoa.HoaPrecedence.resistanceCommand;

/**
 * Biting tests for {@link HoaPrecedence} — OFF &gt; sequence &gt; HAND &gt; AUTO. The named mutation
 * (swap the OFF/defrost order → check inDefrost first) reproduces the live v2.0.5 bug and fails T2.
 */
public class HoaPrecedenceTest {

    /** T1: OFF locks the output out outside defrost. */
    @Test public void offLocksOutIdle() {
        assertFalse(resistanceCommand(false, OFF, true));
    }

    /** T2: OFF dominates the SEQUENCE — heater stays OFF even DURING defrost. Kills the swap-order mutation (the live bug). */
    @Test public void offDominatesDefrost() {
        assertFalse("OFF must dominate the defrost sequence", resistanceCommand(true, OFF, true));
    }

    /** T3: the defrost sequence drives ON when in AUTO. */
    @Test public void defrostDrivesOnInAuto() {
        assertTrue(resistanceCommand(true, AUTO, false));
    }

    /** T4: AUTO passes the computed value outside a sequence. */
    @Test public void autoPassesComputed() {
        assertTrue(resistanceCommand(false, AUTO, true));
        assertFalse(resistanceCommand(false, AUTO, false));
    }

    /** T5: HAND forces ON outside a sequence even when AUTO would be off. */
    @Test public void handForcesOnIdle() {
        assertTrue(resistanceCommand(false, HAND, false));
    }

    /** T6: during defrost the sequence commands ON regardless of HAND (both want ON; consistent). */
    @Test public void defrostConsistentWithHand() {
        assertTrue(resistanceCommand(true, HAND, false));
    }
}
