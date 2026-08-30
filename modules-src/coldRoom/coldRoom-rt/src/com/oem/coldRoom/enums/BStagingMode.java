/*
 * Copyright 2026 OEM. All Rights Reserved.
 */
package com.oem.coldRoom.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.*;

/**
 * BStagingMode selects how a {@code BColdRoom} maps zone-sensor cooling calls
 * onto its evaporator units.
 * <ul>
 *   <li>{@code single} — one zone sensor drives all units (Rooms 2, 3, 4).</li>
 *   <li>{@code staged} — two zone sensors, staged mapping (Room 1, design section 4.2).</li>
 * </ul>
 *
 * Frozen enum authored per the kitControl {@code BLoopAction} exemplar
 * (organized/docSource/.../kitControl/enums/BLoopAction.java).
 */
@NiagaraType
@NiagaraEnum(
  range = {
    @Range("single"),
    @Range("staged")
  }
)
public final class BStagingMode
  extends BFrozenEnum
{
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ Slot-o-Matic regenerates this region: run `:coldRoom-rt:slotomatic`. @*/

  /** Ordinal value for single. */
  public static final int SINGLE = 0;
  /** Ordinal value for staged. */
  public static final int STAGED = 1;

  /** BStagingMode constant for single. */
  public static final BStagingMode single = new BStagingMode(SINGLE);
  /** BStagingMode constant for staged. */
  public static final BStagingMode staged = new BStagingMode(STAGED);

  /** Factory method with ordinal. */
  public static BStagingMode make(int ordinal)
  {
    return (BStagingMode)single.getRange().get(ordinal, false);
  }

  /** Factory method with tag. */
  public static BStagingMode make(String tag)
  {
    return (BStagingMode)single.getRange().get(tag);
  }

  /** Private constructor. */
  private BStagingMode(int ordinal)
  {
    super(ordinal);
  }

  public static final BStagingMode DEFAULT = single;

  //region Type

  @Override
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BStagingMode.class);

  //endregion Type

//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/
}
