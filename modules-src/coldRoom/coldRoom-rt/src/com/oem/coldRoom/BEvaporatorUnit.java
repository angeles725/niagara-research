/*
 * Copyright 2026 OEM. All Rights Reserved.
 */
package com.oem.coldRoom;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

/**
 * BEvaporatorUnit — one evaporator (fan/compressor) + its solenoid valve, and,
 * for Room 3 units, a defrost resistance output. Design section 3.2.
 *
 * Actuation sequence mirrors kitControl {@code BBooleanDelay} (verified exemplar
 * com.tridium.kitControl.timer.BBooleanDelay):
 *   runCmd RISING  -> valveOut = true immediately, start startDelay,
 *                     on expiry -> evapOut = true
 *   runCmd FALLING -> evapOut = false immediately, then valveOut = false
 *
 * Only slots are declared here; the physical valve/evaporator/resistance
 * writables live in the driver tree and are wired to {@code valveOut} /
 * {@code evapOut} / {@code resistanceOut} via BLink at commissioning
 * (design section 6, priority level in8).
 */
@NiagaraType
@NiagaraProperty(
  name = "runCmd",
  type = "BStatusBoolean",
  defaultValue = "new BStatusBoolean(false)",
  flags = Flags.SUMMARY
)
@NiagaraProperty(
  name = "startDelay",
  type = "BRelTime",
  // Valve->evaporator delay, per unit (e.g. 2s Room 1, 5s Room 4). Configurable.
  defaultValue = "BRelTime.make(2000)",
  facets = @Facet("BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0))")
)
@NiagaraProperty(
  name = "valveOut",
  type = "BStatusBoolean",
  defaultValue = "new BStatusBoolean(false)",
  flags = Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY
)
@NiagaraProperty(
  name = "evapOut",
  type = "BStatusBoolean",
  defaultValue = "new BStatusBoolean(false)",
  flags = Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY
)
@NiagaraProperty(
  name = "resistanceOut",
  type = "BStatusBoolean",
  // Energized only during defrost, and only when hasDefrost == true (Room 3).
  defaultValue = "new BStatusBoolean(false)",
  flags = Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY
)
@NiagaraProperty(
  name = "coilTemp",
  type = "BStatusNumeric",
  // This evaporator's temperature sensor. Alarms only; does NOT affect control.
  defaultValue = "new BStatusNumeric()",
  flags = Flags.SUMMARY
)
@NiagaraProperty(
  name = "resistanceTemp",
  type = "BStatusNumeric",
  // Optional Room 3 end-of-defrost sensor (design section 5.3). Faulted/absent
  // -> defrost terminates by duration only.
  defaultValue = "new BStatusNumeric()"
)
@NiagaraProperty(
  name = "evapHighAlarmLimit",
  type = "double",
  defaultValue = "0d"
)
@NiagaraProperty(
  name = "evapLowAlarmLimit",
  type = "double",
  defaultValue = "0d"
)
@NiagaraProperty(
  name = "hasDefrost",
  type = "boolean",
  defaultValue = "false"
)
// Hidden timer callback for the valve->evaporator start delay (BBooleanDelay pattern).
@NiagaraAction(
  name = "startDelayExpired",
  flags = Flags.HIDDEN
)
public class BEvaporatorUnit
  extends BComponent
{
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ Slot-o-Matic regenerates this region: run `:coldRoom-rt:slotomatic`. @*/

  //region Property "runCmd"
  public static final Property runCmd = newProperty(Flags.SUMMARY, new BStatusBoolean(false), null);
  public BStatusBoolean getRunCmd() { return (BStatusBoolean)get(runCmd); }
  public void setRunCmd(BStatusBoolean v) { set(runCmd, v, null); }
  //endregion Property "runCmd"

  //region Property "startDelay"
  public static final Property startDelay = newProperty(0, BRelTime.make(2000), BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0)));
  public BRelTime getStartDelay() { return (BRelTime)get(startDelay); }
  public void setStartDelay(BRelTime v) { set(startDelay, v, null); }
  //endregion Property "startDelay"

  //region Property "valveOut"
  public static final Property valveOut = newProperty(Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY, new BStatusBoolean(false), null);
  public BStatusBoolean getValveOut() { return (BStatusBoolean)get(valveOut); }
  public void setValveOut(BStatusBoolean v) { set(valveOut, v, null); }
  //endregion Property "valveOut"

  //region Property "evapOut"
  public static final Property evapOut = newProperty(Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY, new BStatusBoolean(false), null);
  public BStatusBoolean getEvapOut() { return (BStatusBoolean)get(evapOut); }
  public void setEvapOut(BStatusBoolean v) { set(evapOut, v, null); }
  //endregion Property "evapOut"

  //region Property "resistanceOut"
  public static final Property resistanceOut = newProperty(Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY, new BStatusBoolean(false), null);
  public BStatusBoolean getResistanceOut() { return (BStatusBoolean)get(resistanceOut); }
  public void setResistanceOut(BStatusBoolean v) { set(resistanceOut, v, null); }
  //endregion Property "resistanceOut"

  //region Property "coilTemp"
  public static final Property coilTemp = newProperty(Flags.SUMMARY, new BStatusNumeric(), null);
  public BStatusNumeric getCoilTemp() { return (BStatusNumeric)get(coilTemp); }
  public void setCoilTemp(BStatusNumeric v) { set(coilTemp, v, null); }
  //endregion Property "coilTemp"

  //region Property "resistanceTemp"
  public static final Property resistanceTemp = newProperty(0, new BStatusNumeric(), null);
  public BStatusNumeric getResistanceTemp() { return (BStatusNumeric)get(resistanceTemp); }
  public void setResistanceTemp(BStatusNumeric v) { set(resistanceTemp, v, null); }
  //endregion Property "resistanceTemp"

  //region Property "evapHighAlarmLimit"
  public static final Property evapHighAlarmLimit = newProperty(0, 0d, null);
  public double getEvapHighAlarmLimit() { return getDouble(evapHighAlarmLimit); }
  public void setEvapHighAlarmLimit(double v) { setDouble(evapHighAlarmLimit, v, null); }
  //endregion Property "evapHighAlarmLimit"

  //region Property "evapLowAlarmLimit"
  public static final Property evapLowAlarmLimit = newProperty(0, 0d, null);
  public double getEvapLowAlarmLimit() { return getDouble(evapLowAlarmLimit); }
  public void setEvapLowAlarmLimit(double v) { setDouble(evapLowAlarmLimit, v, null); }
  //endregion Property "evapLowAlarmLimit"

  //region Property "hasDefrost"
  public static final Property hasDefrost = newProperty(0, false, null);
  public boolean getHasDefrost() { return getBoolean(hasDefrost); }
  public void setHasDefrost(boolean v) { setBoolean(hasDefrost, v, null); }
  //endregion Property "hasDefrost"

  //region Action "startDelayExpired"
  public static final Action startDelayExpired = newAction(Flags.HIDDEN, null);
  public void startDelayExpired() { invoke(startDelayExpired, null, null); }
  //endregion Action "startDelayExpired"

  //region Type
  @Override
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BEvaporatorUnit.class);
  //endregion Type

//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/

  public BEvaporatorUnit() {}

  ////////////////////////////////////////////////////////////////
  // Lifecycle
  ////////////////////////////////////////////////////////////////

  @Override
  public void atSteadyState()
  {
    // Re-sync outputs to the current command once the station is live.
    try { applyRunCmd(); } catch (Throwable t) { logError("atSteadyState", t); }
  }

  @Override
  public void changed(Property p, Context cx)
  {
    super.changed(p, cx);
    if (!isRunning()) return;
    // Filter by slot to avoid feedback loops (best-practices 1.1.3).
    try {
      if (p == runCmd) applyRunCmd();
      // coilTemp / resistanceTemp changes are consumed by alarm extensions and
      // by BDefrostController; no control action here.
    } catch (Throwable t) {
      // NEVER throw on the engine thread (best-practices 4.3).
      logError("changed", t);
    }
  }

  ////////////////////////////////////////////////////////////////
  // Actuation — valve-first, evaporator-after-delay (BBooleanDelay mirror)
  ////////////////////////////////////////////////////////////////

  /**
   * Apply the current {@code runCmd} to valve/evaporator outputs, honoring the
   * per-unit {@code startDelay}. Suppressed while this unit is in defrost — the
   * defrost controller owns the outputs then (design section 5.3/5.4).
   */
  void applyRunCmd()
  {
    if (inDefrost) return; // defrost controller drives outputs during defrost
    if (!getRunCmd().getStatus().isValid()) return;

    boolean cmd = getRunCmd().getValue();
    if (cmd && !lastCmd)
    {
      lastCmd = true;
      // Rising: open the valve NOW, then start the evaporator after startDelay.
      setBool(valveOut, true);
      if (getStartDelay().getMillis() == 0L)
        setBool(evapOut, true);
      else
        startDelayTicket = Clock.schedule(this, getStartDelay(), startDelayExpired, null);
    }
    else if (!cmd && lastCmd)
    {
      lastCmd = false;
      // Falling: stop the evaporator first, then close the valve.
      cancelTicket();
      setBool(evapOut, false);
      setBool(valveOut, false);
    }
  }

  /** Timer callback: startDelay elapsed -> energize the evaporator. */
  public void doStartDelayExpired()
  {
    if (inDefrost) return;
    if (getRunCmd().getStatus().isValid() && getRunCmd().getValue())
      setBool(evapOut, true);
  }

  ////////////////////////////////////////////////////////////////
  // Defrost hooks — called by the parent room's BDefrostController
  ////////////////////////////////////////////////////////////////

  /**
   * Enter defrost: close valve, stop evaporator, energize resistance.
   * Design section 5.3. No-op if this unit has no defrost hardware.
   */
  void enterDefrost()
  {
    if (!getHasDefrost()) return;
    inDefrost = true;
    cancelTicket();
    setBool(evapOut, false);
    setBool(valveOut, false);
    setBool(resistanceOut, true);
  }

  /** Exit defrost: de-energize resistance, return to normal cooling control. */
  void exitDefrost()
  {
    setBool(resistanceOut, false);
    inDefrost = false;
    lastCmd = false;   // force applyRunCmd() to re-evaluate the rising edge
    applyRunCmd();
  }

  boolean isInDefrost() { return inDefrost; }

  /**
   * True when the end-of-defrost temperature has been reached, given the
   * controller's terminate-on-temp configuration.
   */
  boolean resistanceTempReached(double threshold)
  {
    BStatusNumeric t = getResistanceTemp();
    if (t.getStatus().isNull() || !t.getStatus().isValid()) return false;
    return t.getValue() >= threshold;
  }

  ////////////////////////////////////////////////////////////////
  // Helpers
  ////////////////////////////////////////////////////////////////

  private void setBool(Property outProp, boolean v)
  {
    // TODO: link to proxy point (BLink to a BBooleanWritable, priority in8).
    // Writing the slot is what the BLink propagates to the field writable.
    ((BStatusBoolean)get(outProp)).setValue(v);
  }

  private void cancelTicket()
  {
    if (startDelayTicket != null) { startDelayTicket.cancel(); startDelayTicket = null; }
  }

  private void logError(String where, Throwable t)
  {
    // TODO [INFER]: route to a module Logger (e.g. Logger.getLogger("coldRoom"));
    // engine-thread exceptions are swallowed + logged, not thrown (best-practices 4.3).
  }

  ////////////////////////////////////////////////////////////////
  // Attributes (transient runtime state — not slots)
  ////////////////////////////////////////////////////////////////

  private boolean lastCmd = false;
  private boolean inDefrost = false;
  private Clock.Ticket startDelayTicket;
}
