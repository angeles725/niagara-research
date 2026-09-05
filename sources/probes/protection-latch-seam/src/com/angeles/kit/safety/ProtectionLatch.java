package com.angeles.kit.safety;

/**
 * Protection latch — a SET-DOMINANT SR latch with FIRST-OUT capture, for a safety trip that
 * must STAY tripped until an explicit operator reset. Pure Java (no Baja deps) so it unit-tests
 * off-station; a {@code Bxxx} wrapper drives {@link #step} from execute()/changed(), exposes a
 * reset ACTION (optionally step-up-gated per B803), and wires the first-out to a BAlarmSourceExt.
 *
 * <p>Fills the B805 §805.3 gap: kitControl ships only an edge-triggered D-latch (BLatch); there is
 * NO first-party set/reset safety latch. This is the author-built one — the C9 PR1 fixture.
 *
 * <p>Contract:
 * <ul>
 *  <li>SET-DOMINANT: a trip condition wins over a concurrent reset request (safety over convenience).</li>
 *  <li>FIRST-OUT: the reason is captured ONCE, on the CLEAR→TRIPPED edge; a later trip never overwrites
 *      it (no chatter, no re-capture) — so you always know what tripped FIRST.</li>
 *  <li>EXPLICIT RESET ONLY: the latch clears only on an explicit reset request AND only when the trip
 *      condition is already clear — an operator cannot reset THROUGH an active fault.</li>
 *  <li>NO RE-TRIP CHATTER: once tripped it holds without re-firing; after a reset it stays clear unless
 *      the trip condition re-asserts (then it captures a fresh first-out).</li>
 * </ul>
 */
public final class ProtectionLatch {

    private boolean tripped = false;
    private String  firstOutReason = null;
    private long    firstOutMillis = 0L;

    /**
     * Evaluate one control cycle.
     *
     * @param tripCondition true while the fault/limit is active
     * @param resetRequest  true on the cycle an operator asks to reset
     * @param reason        the trip reason to capture on the trip edge (may be null)
     * @param nowMillis     current time, stamped as the first-out time on the trip edge
     * @return true if tripped after this cycle
     */
    public boolean step(boolean tripCondition, boolean resetRequest, String reason, long nowMillis) {
        if (tripCondition) {
            if (!tripped) {                    // CLEAR -> TRIPPED edge: capture first-out exactly once
                tripped = true;
                firstOutReason = reason;
                firstOutMillis = nowMillis;
            }
            return true;                       // SET-DOMINANT: trip wins over any reset this cycle
        }
        if (resetRequest && tripped) {         // explicit reset, honored only with the condition clear
            tripped = false;
            firstOutReason = null;
            firstOutMillis = 0L;
        }
        return tripped;
    }

    public boolean isTripped()         { return tripped; }
    public String  getFirstOutReason() { return firstOutReason; }
    public long    getFirstOutMillis() { return firstOutMillis; }
}
