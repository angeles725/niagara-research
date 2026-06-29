package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "iomMinute",
      ordinal = 0
   ), @Range(
      value = "iomHour",
      ordinal = 1
   ), @Range(
      value = "iomDay",
      ordinal = 2
   ), @Range(
      value = "iomWeek",
      ordinal = 3
   ), @Range(
      value = "iomMonth",
      ordinal = 4
   ), @Range(
      value = "iomNul",
      ordinal = -1
   )},
   defaultValue = "iomNul"
)
public final class BLonIntervalOfMonthEnum extends BFrozenEnum {
   public static final int IOM_MINUTE = 0;
   public static final int IOM_HOUR = 1;
   public static final int IOM_DAY = 2;
   public static final int IOM_WEEK = 3;
   public static final int IOM_MONTH = 4;
   public static final int IOM_NUL = -1;
   public static final BLonIntervalOfMonthEnum iomMinute = new BLonIntervalOfMonthEnum(0);
   public static final BLonIntervalOfMonthEnum iomHour = new BLonIntervalOfMonthEnum(1);
   public static final BLonIntervalOfMonthEnum iomDay = new BLonIntervalOfMonthEnum(2);
   public static final BLonIntervalOfMonthEnum iomWeek = new BLonIntervalOfMonthEnum(3);
   public static final BLonIntervalOfMonthEnum iomMonth = new BLonIntervalOfMonthEnum(4);
   public static final BLonIntervalOfMonthEnum iomNul = new BLonIntervalOfMonthEnum(-1);
   public static final BLonIntervalOfMonthEnum DEFAULT = iomNul;
   public static final Type TYPE = Sys.loadType(BLonIntervalOfMonthEnum.class);

   public static BLonIntervalOfMonthEnum make(int ordinal) {
      return (BLonIntervalOfMonthEnum)iomMinute.getRange().get(ordinal, false);
   }

   public static BLonIntervalOfMonthEnum make(String tag) {
      return (BLonIntervalOfMonthEnum)iomMinute.getRange().get(tag);
   }

   private BLonIntervalOfMonthEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
