/*
 * Copyright 2026 OEM. All Rights Reserved.
 */
package com.oem.coldRoom;

import java.util.List;

import com.oem.coldRoom.enums.BDefrostMode;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
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
 * BDefrostController — Room 3 only (design section 5). Child of a {@link BColdRoom};
 * coordinates the defrost of that room's {@link BEvaporatorUnit}s.
 *
 * Per-unit defrost sequence (section 5.3): close valve -> stop evaporator ->
 * energize resistance (delegated to {@link BEvaporatorUnit#enterDefrost()}); exit
 * on {@code duration} elapsed OR ({@code terminateOnResistanceTemp} AND
 * {@code resistanceTemp >= resistanceTempThreshold}).
 *
 * Interlock (section 5.4): at most ONE unit in defrost at a time. A unit that
 * becomes due while another is defrosting WAITS; once the other resumes normal
 * operation, the waiting unit starts a {@code staggerDelay} timer (default 4 min)
 * and then takes the token.
 */
@NiagaraType
@NiagaraProperty(
  name = "mode",
  type = "BDefrostMode",
  defaultValue = "BDefrostMode.interval"
)
@NiagaraProperty(
  name = "interval",
  type = "BRelTime",
  // "Defrost every N" (interval mode). Default 8h; configurable.
  defaultValue = "BRelTime.make(28800000)",
  facets = @Facet("BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0))")
)
@NiagaraProperty(
  name = "scheduleInput",
  // Schedule-mode trigger: link this from a BBooleanSchedule's out (WebScheduler).
  // TODO [INFER]: design section 5.1 names this an "BBooleanSchedule ref". Modeled
  // here as a linked BStatusBoolean input; alternatively it could be a BOrd/relation
  // to a BBooleanSchedule. Confirm the desired reference style.
  type = "BStatusBoolean",
  defaultValue = "new BStatusBoolean(false)",
  flags = Flags.SUMMARY
)
@NiagaraProperty(
  name = "duration",
  type = "BRelTime",
  // Max defrost length. Default 30 min; configurable.
  defaultValue = "BRelTime.make(1800000)",
  facets = @Facet("BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0))")
)
@NiagaraProperty(
  name = "terminateOnResistanceTemp",
  type = "boolean",
  defaultValue = "false"
)
@NiagaraProperty(
  name = "resistanceTempThreshold",
  type = "double",
  defaultValue = "0d"
)
@NiagaraProperty(
  name = "staggerDelay",
  type = "BRelTime",
  // Inter-unit stagger. Default 4 min (240000 ms); configurable.
  defaultValue = "BRelTime.make(240000)",
  facets = @Facet("BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0))")
)
@NiagaraAction(name = "intervalExpired", flags = Flags.HIDDEN)
@NiagaraAction(name = "durationExpired", flags = Flags.HIDDEN)
@NiagaraAction(name = "staggerExpired",  flags = Flags.HIDDEN)
@NiagaraAction(name = "pollTerminate",   flags = Flags.HIDDEN)
public class BDefrostController
  extends BComponent
{
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ Slot-o-Matic regenerates this region: run `:coldRoom-rt:slotomatic`. @*/

  //region Property "mode"
  public static final Property mode = newProperty(0, BDefrostMode.interval, null);
  public BDefrostMode getMode() { return (BDefrostMode)get(mode); }
  public void setMode(BDefrostMode v) { set(mode, v, null); }
  //endregion Property "mode"

  //region Property "interval"
  public static final Property interval = newProperty(0, BRelTime.make(28800000), BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0)));
  public BRelTime getInterval() { return (BRelTime)get(interval); }
  public void setInterval(BRelTime v) { set(interval, v, null); }
  //endregion Property "interval"

  //region Property "scheduleInput"
  public static final Property scheduleInput = newProperty(Flags.SUMMARY, new BStatusBoolean(false), null);
  public BStatusBoolean getScheduleInput() { return (BStatusBoolean)get(scheduleInput); }
  public void setScheduleInput(BStatusBoolean v) { set(scheduleInput, v, null); }
  //endregion Property "scheduleInput"

  //region Property "duration"
  public static final Property duration = newProperty(0, BRelTime.make(1800000), BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0)));
  public BRelTime getDuration() { return (BRelTime)get(duration); }
  public void setDuration(BRelTime v) { set(duration, v, null); }
  //endregion Property "duration"

  //region Property "terminateOnResistanceTemp"
  public static final Property terminateOnResistanceTemp = newProperty(0, false, null);
  public boolean getTerminateOnResistanceTemp() { return getBoolean(terminateOnResistanceTemp); }
  public void setTerminateOnResistanceTemp(boolean v) { setBoolean(terminateOnResistanceTemp, v, null); }
  //endregion Property "terminateOnResistanceTemp"

  //region Property "resistanceTempThreshold"
  public static final Property resistanceTempThreshold = newProperty(0, 0d, null);
  public double getResistanceTempThreshold() { return getDouble(resistanceTempThreshold); }
  public void setResistanceTempThreshold(double v) { setDouble(resistanceTempThreshold, v, null); }
  //endregion Property "resistanceTempThreshold"

  //region Property "staggerDelay"
  public static final Property staggerDelay = newProperty(0, BRelTime.make(240000), BFacets.make(BFacets.MIN, BRelTime.makeSeconds(0)));
  public BRelTime getStaggerDelay() { return (BRelTime)get(staggerDelay); }
  public void setStaggerDelay(BRelTime v) { set(staggerDelay, v, null); }
  //endregion Property "staggerDelay"

  //region Action "intervalExpired"
  public static final Action intervalExpired = newAction(Flags.HIDDEN, null);
  public void intervalExpired() { invoke(intervalExpired, null, null); }
  //endregion Action "intervalExpired"

  //region Action "durationExpired"
  public static final Action durationExpired = newAction(Flags.HIDDEN, null);
  public void durationExpired() { invoke(durationExpired, null, null); }
  //endregion Action "durationExpired"

  //region Action "staggerExpired"
  public static final Action staggerExpired = newAction(Flags.HIDDEN, null);
  public void staggerExpired() { invoke(staggerExpired, null, null); }
  //endregion Action "staggerExpired"

  //region Action "pollTerminate"
  public static final Action pollTerminate = newAction(Flags.HIDDEN, null);
  public void pollTerminate() { invoke(pollTerminate, null, null); }
  //endregion Action "pollTerminate"

  //region Type
  @Override
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BDefrostController.class);
  //endregion Type

//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/

  public BDefrostController() {}

  ////////////////////////////////////////////////////////////////
  // Lifecycle
  ////////////////////////////////////////////////////////////////

  @Override
  public void atSteadyState()
  {
    try { armTrigger(); } catch (Throwable t) { logError("atSteadyState", t); }
  }

  @Override
  public void stopped()
  {
    cancelAll();
    super.stopped();
  }

  @Override
  public void changed(Property p, Context cx)
  {
    super.changed(p, cx);
    if (!isRunning()) return;
    try {
      if (p == mode || p == interval)
        armTrigger();
      else if (p == scheduleInput && getMode().getOrdinal() == BDefrostMode.SCHEDULE)
        onScheduleChanged();
    } catch (Throwable t) {
      logError("changed", t);
    }
  }

  ////////////////////////////////////////////////////////////////
  // Trigger (design section 5.2)
  ////////////////////////////////////////////////////////////////

  /** Arm the interval free-running timer, or wait for schedule edges. */
  private void armTrigger()
  {
    cancelInterval();
    if (getMode().getOrdinal() == BDefrostMode.INTERVAL)
      intervalTicket = Clock.schedule(this, getInterval(), intervalExpired, null);
    // schedule mode: driven by scheduleInput rising edge (onScheduleChanged).
    // TODO [INFER]: kitControl BMultiVibrator (period+dutyCycle) is the native
    // analog for interval mode and could replace this hand-armed timer.
  }

  /** Interval elapsed -> both units are due; run a staggered cycle, then re-arm. */
  public void doIntervalExpired()
  {
    requestDefrostCycle();
    intervalTicket = Clock.schedule(this, getInterval(), intervalExpired, null);
  }

  private void onScheduleChanged()
  {
    BStatusBoolean s = getScheduleInput();
    if (!s.getStatus().isValid()) return;
    boolean now = s.getValue();
    if (now && !lastSchedule) requestDefrostCycle(); // rising edge starts defrost
    lastSchedule = now;
  }

  /** Queue every unit for defrost; the interlock serializes them. */
  private void requestDefrostCycle()
  {
    List<BEvaporatorUnit> units = units();
    for (int i = 0; i < units.size(); i++)
      requestDefrost(i);
  }

  ////////////////////////////////////////////////////////////////
  // Interlock state machine (design section 5.4)
  ////////////////////////////////////////////////////////////////

  /** Request the defrost token for unit index {@code i}. */
  private void requestDefrost(int i)
  {
    if (i == defrostingUnit) return;      // already defrosting
    if (defrostingUnit == -1)
      beginDefrost(i);                    // token free -> start now
    else if (waitingUnit == -1)
      waitingUnit = i;                    // hold the token; wait our turn
  }

  private void beginDefrost(int i)
  {
    List<BEvaporatorUnit> units = units();
    if (i < 0 || i >= units.size()) return;
    defrostingUnit = i;
    units.get(i).enterDefrost();          // close valve -> stop evap -> resistance on
    durationTicket = Clock.schedule(this, getDuration(), durationExpired, null);
    if (getTerminateOnResistanceTemp())
      pollTicket = Clock.schedule(this, POLL, pollTerminate, null);
  }

  /** Duration elapsed -> end this unit's defrost. */
  public void doDurationExpired() { terminateCurrent(); }

  /** Periodic check for the resistance-temp termination path. */
  public void doPollTerminate()
  {
    if (defrostingUnit == -1) return;
    BEvaporatorUnit u = unit(defrostingUnit);
    if (u != null && getTerminateOnResistanceTemp()
        && u.resistanceTempReached(getResistanceTempThreshold()))
    {
      terminateCurrent();
      return;
    }
    // TODO [INFER]: fixed POLL interval (5s). Prefer subscribing to the unit's
    // resistanceTemp change instead of polling if a cleaner hook is available.
    pollTicket = Clock.schedule(this, POLL, pollTerminate, null);
  }

  /** End the current unit's defrost; hand the token to any waiting unit after stagger. */
  private void terminateCurrent()
  {
    int u = defrostingUnit;
    cancelDuration();
    cancelPoll();
    if (u != -1)
    {
      BEvaporatorUnit unit = unit(u);
      if (unit != null) unit.exitDefrost(); // resistance off -> resume normal cooling
    }
    defrostingUnit = -1;

    if (waitingUnit != -1)
    {
      pendingStaggerUnit = waitingUnit;
      waitingUnit = -1;
      // The waiting unit starts staggerDelay AFTER the other resumes (design 5.4).
      staggerTicket = Clock.schedule(this, getStaggerDelay(), staggerExpired, null);
    }
  }

  /** Stagger elapsed -> the waiting unit takes the (now free) token. */
  public void doStaggerExpired()
  {
    int w = pendingStaggerUnit;
    pendingStaggerUnit = -1;
    if (w != -1) requestDefrost(w);
  }

  ////////////////////////////////////////////////////////////////
  // Helpers
  ////////////////////////////////////////////////////////////////

  private List<BEvaporatorUnit> units()
  {
    // Interlock operates over the parent room's evaporator units.
    // TODO [INFER]: assumes this controller is a child of a BColdRoom. Guard/adjust
    // if the deployment nests it elsewhere.
    Object parent = getParent();
    if (parent instanceof BColdRoom)
      return ((BColdRoom)parent).getUnits();
    return java.util.Collections.emptyList();
  }

  private BEvaporatorUnit unit(int i)
  {
    List<BEvaporatorUnit> u = units();
    return (i >= 0 && i < u.size()) ? u.get(i) : null;
  }

  private void cancelInterval() { if (intervalTicket != null) { intervalTicket.cancel(); intervalTicket = null; } }
  private void cancelDuration() { if (durationTicket != null) { durationTicket.cancel(); durationTicket = null; } }
  private void cancelPoll()     { if (pollTicket     != null) { pollTicket.cancel();     pollTicket = null; } }
  private void cancelStagger()  { if (staggerTicket  != null) { staggerTicket.cancel();  staggerTicket = null; } }

  private void cancelAll()
  {
    cancelInterval(); cancelDuration(); cancelPoll(); cancelStagger();
  }

  private void logError(String where, Throwable t)
  {
    // TODO [INFER]: route to a module Logger; swallow on the engine thread.
  }

  ////////////////////////////////////////////////////////////////
  // Attributes (transient interlock state — not slots)
  ////////////////////////////////////////////////////////////////

  private static final BRelTime POLL = BRelTime.make(5000); // resistance-temp poll period

  private int defrostingUnit = -1;     // index of the unit holding the token, or -1
  private int waitingUnit = -1;        // index of a unit that requested while busy, or -1
  private int pendingStaggerUnit = -1; // unit whose stagger timer is running, or -1
  private boolean lastSchedule = false;

  private Clock.Ticket intervalTicket;
  private Clock.Ticket durationTicket;
  private Clock.Ticket pollTicket;
  private Clock.Ticket staggerTicket;
}
