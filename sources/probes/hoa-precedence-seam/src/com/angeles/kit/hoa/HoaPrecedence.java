package com.angeles.kit.hoa;

/**
 * HOA / output precedence for a DANGEROUS actuator (heat, compression). Pure Java (no Baja deps).
 *
 * <p>Encodes the rule <b>OFF &gt; sequence &gt; HAND &gt; AUTO</b> (B805 §805.11):
 * an operator OFF is a LOCKOUT that dominates EVERY automation — including a sequence that "owns"
 * the output (defrost, staging); a running sequence beats HAND/AUTO; HAND forces on outside a
 * sequence; AUTO passes the computed value. Maps to Niagara's priority array (BBooleanWritable
 * emergency level 1 / manual level 8 above automation 9-16); this plain-mode seam emulates
 * priority 1-2 for OFF — and, unlike ColdRoomPan's leaked `if (inDefrost) return`, OFF is checked
 * on the sequence path too, so it cannot be bypassed by an owning sequence.
 */
public final class HoaPrecedence {

    /** ColdRoomControl.HOA_*: 0=auto, 1=hand, 2=off. */
    public enum Mode { AUTO, HAND, OFF }

    /**
     * @param inDefrost true while the defrost sequence owns the output
     * @param mode      operator HOA selection
     * @param auto      the computed AUTO command (used only in AUTO, outside a sequence)
     * @return the resistance (heater) ON/OFF command
     */
    public static boolean resistanceCommand(boolean inDefrost, Mode mode, boolean auto) {
        if (mode == Mode.OFF) return false;   // OFF LOCKOUT dominates EVERYTHING, defrost included
        if (inDefrost)        return true;    // sequence owns the output when not locked out
        if (mode == Mode.HAND) return true;   // HAND forces on outside a sequence
        return auto;                          // AUTO = the computed value
    }

    private HoaPrecedence() {}
}
