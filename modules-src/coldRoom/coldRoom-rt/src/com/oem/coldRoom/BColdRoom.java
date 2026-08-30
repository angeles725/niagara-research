/*
 * Copyright 2026 OEM. All Rights Reserved.
 */
package com.oem.coldRoom;

import java.util.ArrayList;
import java.util.List;

import com.oem.coldRoom.enums.BStagingMode;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

/**
 * BColdRoom — reusable "equip" container: one instance per physical cold room
 * (design sections 3.1 / 4). Holds shared setpoint/differential, the zone-sensor
 * inputs, and (as dynamic children) 1..3 {@link BEvaporatorUnit}s plus an optional
 * {@link BDefrostController} (Room 3).
 *
 * Placement: under /Config (e.g. /Config/ColdRooms/Room1..Room4), tagged {@code equip}
 * (station-organization section 2). Logic reads inputs and writes unit outputs; those
 * unit outputs reach the field via BLink to driver writables (design section 6).
 *
 * Control:
 *   - Per zone sensor: hysteresis, reproducing kitControl BTstat deadband
 *     (verified com.tridium.kitControl.hvac.BTstat.calculate(), sp +/- diff/2, HOLD
 *     between thresholds). See {@link #computeCall}.
 *   - Staging map (design section 4.2) mapping calls onto units.
 */
@NiagaraType
@NiagaraProperty(
  name = "setpoint",
  type = "BStatusNumeric",
  // TODO [INFER]: units facet. Confirm the exact BUnit lookup tag for Celsius,
  // e.g. BFacets.make(BFacets.UNITS, BUnit.getUnit("celsius")); "celsius" tag unverified.
  defaultValue = "new BStatusNumeric(0d)",
  flags = Flags.SUMMARY
)
@NiagaraProperty(
  name = "differential",
  type = "double",
  // Hysteresis band. Default 1.0 degree, min 0 (design section 3.1).
  defaultValue = "1d",
  facets = @Facet("BFacets.make(BFacets.MIN, 0d)")
)
@NiagaraProperty(
  name = "roomHighAlarmLimit",
  type = "double",
  // Feeds the room high-temp visual alarm only (does NOT affect control).
  defaultValue = "0d"
)
@NiagaraProperty(
  name = "zone1",
  type = "BStatusNumeric",
  defaultValue = "new BStatusNumeric()",
  flags = Flags.SUMMARY
)
@NiagaraProperty(
  name = "zone2",
  // Second zone sensor; used only when stagingMode == staged (Room 1).
  type = "BStatusNumeric",
  defaultValue = "new BStatusNumeric()",
  flags = Flags.SUMMARY
)
@NiagaraProperty(
  name = "stagingMode",
  type = "BStagingMode",
  defaultValue = "BStagingMode.single"
)
@NiagaraProperty(
  name = "cooling",
  type = "BStatusBoolean",
  // Computed room cooling demand (call1 OR call2). Read-only output.
  defaultValue = "new BStatusBoolean(false)",
  flags = Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY
)
public class BColdRoom
  extends BComponent
{
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ Slot-o-Matic regenerates this region: run `:coldRoom-rt:slotomatic`. @*/

  //region Property "setpoint"
  public static final Property setpoint = newProperty(Flags.SUMMARY, new BStatusNumeric(0d), null);
  public BStatusNumeric getSetpoint() { return (BStatusNumeric)get(setpoint); }
  public void setSetpoint(BStatusNumeric v) { set(setpoint, v, null); }
  //endregion Property "setpoint"

  //region Property "differential"
  public static final Property differential = newProperty(0, 1d, BFacets.make(BFacets.MIN, 0d));
  public double getDifferential() { return getDouble(differential); }
  public void setDifferential(double v) { setDouble(differential, v, null); }
  //endregion Property "differential"

  //region Property "roomHighAlarmLimit"
  public static final Property roomHighAlarmLimit = newProperty(0, 0d, null);
  public double getRoomHighAlarmLimit() { return getDouble(roomHighAlarmLimit); }
  public void setRoomHighAlarmLimit(double v) { setDouble(roomHighAlarmLimit, v, null); }
  //endregion Property "roomHighAlarmLimit"

  //region Property "zone1"
  public static final Property zone1 = newProperty(Flags.SUMMARY, new BStatusNumeric(), null);
  public BStatusNumeric getZone1() { return (BStatusNumeric)get(zone1); }
  public void setZone1(BStatusNumeric v) { set(zone1, v, null); }
  //endregion Property "zone1"

  //region Property "zone2"
  public static final Property zone2 = newProperty(Flags.SUMMARY, new BStatusNumeric(), null);
  public BStatusNumeric getZone2() { return (BStatusNumeric)get(zone2); }
  public void setZone2(BStatusNumeric v) { set(zone2, v, null); }
  //endregion Property "zone2"

  //region Property "stagingMode"
  public static final Property stagingMode = newProperty(0, BStagingMode.single, null);
  public BStagingMode getStagingMode() { return (BStagingMode)get(stagingMode); }
  public void setStagingMode(BStagingMode v) { set(stagingMode, v, null); }
  //endregion Property "stagingMode"

  //region Property "cooling"
  public static final Property cooling = newProperty(Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY, new BStatusBoolean(false), null);
  public BStatusBoolean getCooling() { return (BStatusBoolean)get(cooling); }
  public void setCooling(BStatusBoolean v) { set(cooling, v, null); }
  //endregion Property "cooling"

  //region Type
  @Override
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BColdRoom.class);
  //endregion Type

//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/

  public BColdRoom() {}

  ////////////////////////////////////////////////////////////////
  // Lifecycle
  ////////////////////////////////////////////////////////////////

  @Override
  public void atSteadyState()
  {
    try { execute(); } catch (Throwable t) { logError("atSteadyState", t); }
  }

  @Override
  public void changed(Property p, Context cx)
  {
    super.changed(p, cx);
    if (!isRunning()) return;
    try {
      // Re-run the control map when any control input changes.
      if (p == zone1 || p == zone2 || p == setpoint
          || p == differential || p == stagingMode)
        execute();
    } catch (Throwable t) {
      logError("changed", t); // never throw on the engine thread
    }
  }

  ////////////////////////////////////////////////////////////////
  // Control (design sections 4.1 hysteresis + 4.2 staging)
  ////////////////////////////////////////////////////////////////

  /**
   * Compute cooling calls and drive each child unit. Named {@code execute()} to
   * match the design; it is invoked from {@link #changed} and {@link #atSteadyState},
   * not by an engine execute-hook.
   */
  void execute()
  {
    boolean staged = getStagingMode().getOrdinal() == BStagingMode.STAGED;

    double sp = getSetpoint().getValue();
    double diff = getDifferential();

    call1 = computeCall(getZone1(), sp, diff, call1);
    call2 = staged ? computeCall(getZone2(), sp, diff, call2) : false;

    boolean roomCall = call1 || call2;
    getCooling().setValue(roomCall);

    // Staging map (design section 4.2). Unit identity is child order:
    // index 0 = unit 1, index 1 = unit 2 (middle, OR of both zones), index 2 = unit 3.
    // TODO [INFER]: identity-by-order is a design synthesis. If a room can have
    // children in a non-deterministic order, add an explicit "stage"/"role" slot on
    // BEvaporatorUnit and map by that instead.
    List<BEvaporatorUnit> units = getUnits();
    for (int i = 0; i < units.size(); i++)
    {
      boolean run;
      if (!staged)
      {
        run = call1;                              // Rooms 2, 3, 4: all units on the single call
      }
      else if (i == 0)      run = call1;          // Room 1 unit 1: zone-1 call
      else if (i == 1)      run = call1 || call2; // Room 1 unit 2: OR of both zones (native BOr analog)
      else                  run = call2;          // Room 1 unit 3: zone-2 call
      driveUnit(units.get(i), run);
    }
  }

  /**
   * Hysteresis for one zone sensor, reproducing BTstat.calculate() (direct action):
   *   high = sp + diff/2 ; low = sp - diff/2
   *   cv >= high -> true ; cv <= low -> false ; otherwise HOLD previous.
   * A faulted/null sensor holds the previous state (fail-safe: never asserts a new
   * cooling call from a bad reading — best-practices 1.1.7).
   */
  static boolean computeCall(BStatusNumeric zone, double sp, double diff, boolean prev)
  {
    if (zone.getStatus().isNull() || !zone.getStatus().isValid())
      return prev; // hold on fault/null
    double cv = zone.getValue();
    double half = diff / 2.0;
    double high = sp + half;
    double low  = sp - half;
    if (cv >= high) return true;
    if (cv <= low)  return false;
    return prev;    // deadband -> hold (hysteresis)
  }

  private void driveUnit(BEvaporatorUnit u, boolean run)
  {
    // A unit currently in defrost keeps its outputs owned by the defrost controller;
    // its runCmd is still updated so it resumes correctly on defrost exit.
    u.getRunCmd().setValue(run);
    u.applyRunCmd();
  }

  ////////////////////////////////////////////////////////////////
  // Child access
  ////////////////////////////////////////////////////////////////

  /** The evaporator units, in child (slot) order. */
  List<BEvaporatorUnit> getUnits()
  {
    List<BEvaporatorUnit> out = new ArrayList<>();
    for (BEvaporatorUnit u : (BEvaporatorUnit[])getChildren(BEvaporatorUnit.class))
      out.add(u);
    return out;
  }

  /** The optional defrost controller (Room 3), or null. */
  BDefrostController getDefrostController()
  {
    BDefrostController[] d = (BDefrostController[])getChildren(BDefrostController.class);
    return (d.length > 0) ? d[0] : null;
  }

  private void logError(String where, Throwable t)
  {
    // TODO [INFER]: route to a module Logger; swallow on the engine thread.
  }

  ////////////////////////////////////////////////////////////////
  // Attributes (transient hysteresis hold state — not slots)
  ////////////////////////////////////////////////////////////////

  private boolean call1 = false;
  private boolean call2 = false;
}
