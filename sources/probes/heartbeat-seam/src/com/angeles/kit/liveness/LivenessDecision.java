package com.angeles.kit.liveness;

/**
 * Liveness decision — the PURE seam of the B812 heartbeat/liveness monitor (the Baja monitor is a thin
 * adapter around this). Decides whether a producer that is supposed to "tick" every {@code periodMs} has
 * STALLED. No Baja deps → unit-tests off-station.
 *
 * <p>Contract:
 * <ul>
 *  <li>STALLED when {@code age > factor*periodMs} (STRICTLY above — never at or before the boundary).</li>
 *  <li>RECOVERED is an EDGE: reported once, on the first cycle a fresh tick brings {@code age < periodMs}
 *      after a stall; the next live cycle is plain LIVE.</li>
 *  <li>Hysteresis: while {@code prevStalled}, HOLD stalled through the band {@code periodMs ≤ age ≤ factor*periodMs}
 *      — recover only when {@code age < periodMs}. No chatter at the boundary.</li>
 *  <li>A producer that never ticked reduces to "age from arm-time": the wrapper stamps {@code lastTick} at
 *      started()/atSteadyState() (B775 §775.6), so a never-updated lastTick simply ages into a stall after the grace.</li>
 *  <li>Clock jump BACKWARDS ({@code now < lastTick}, i.e. negative age): treated as a FRESH tick (age=0 → LIVE),
 *      NEVER a stall. WHY: a backward clock step (the B775 §775.6 clockChanged re-arm) or a future-stamped lastTick
 *      is a clock artifact, not evidence of a dead producer — a monitor must not false-alarm on it (fail-safe toward
 *      "alive"; a real stall re-appears on the next forward-time cycle). Consistent with B801 (time handling floors).</li>
 * </ul>
 */
public final class LivenessDecision {

    public enum Liveness { LIVE, STALLED, RECOVERED }

    /**
     * @param nowMs       current time
     * @param lastTickMs  time of the producer's last tick (or the arm-time if it never ticked)
     * @param periodMs    expected tick period (&gt; 0)
     * @param factor      stall multiplier (e.g. 3 → stall after 3 missed periods)
     * @param prevStalled the decision's own previous stalled state (level), for the edge + hysteresis
     * @return LIVE, STALLED, or the one-shot RECOVERED edge
     */
    public Liveness step(long nowMs, long lastTickMs, long periodMs, int factor, boolean prevStalled) {
        long age = nowMs - lastTickMs;
        if (age < 0L) age = 0L;                          // backward clock jump / future lastTick → fresh (fail-safe alive)

        long stallThreshold = (long) factor * periodMs;

        if (age > stallThreshold) {                      // STRICT: never stall at/below the boundary
            return Liveness.STALLED;
        }
        if (prevStalled) {                               // hysteresis: hold until a fresh tick drops age below one period
            return (age < periodMs) ? Liveness.RECOVERED : Liveness.STALLED;
        }
        return Liveness.LIVE;
    }
}
