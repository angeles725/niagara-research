/*
 * Copyright 2026 OEM. All Rights Reserved.
 */
package com.oem.coldRoom.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.*;

/**
 * BDefrostMode selects the defrost trigger source for a {@code BDefrostController}
 * (design section 5.1).
 * <ul>
 *   <li>{@code interval} — free-running "defrost every N" timer.</li>
 *   <li>{@code schedule} — wall-clock defrost driven by a BBooleanSchedule.</li>
 * </ul>
 */
@NiagaraType
@NiagaraEnum(
  range = {
    @Range("interval"),
    @Range("schedule")
  }
)
public final class BDefrostMode
  extends BFrozenEnum
{
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ Slot-o-Matic regenerates this region: run `:coldRoom-rt:slotomatic`. @*/

  /** Ordinal value for interval. */
  public static final int INTERVAL = 0;
  /** Ordinal value for schedule. */
  public static final int SCHEDULE = 1;

  /** BDefrostMode constant for interval. */
  public static final BDefrostMode interval = new BDefrostMode(INTERVAL);
  /** BDefrostMode constant for schedule. */
  public static final BDefrostMode schedule = new BDefrostMode(SCHEDULE);

  /** Factory method with ordinal. */
  public static BDefrostMode make(int ordinal)
  {
    return (BDefrostMode)interval.getRange().get(ordinal, false);
  }

  /** Factory method with tag. */
  public static BDefrostMode make(String tag)
  {
    return (BDefrostMode)interval.getRange().get(tag);
  }

  /** Private constructor. */
  private BDefrostMode(int ordinal)
  {
    super(ordinal);
  }

  public static final BDefrostMode DEFAULT = interval;

  //region Type

  @Override
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BDefrostMode.class);

  //endregion Type

//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/
}
