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
      value = "daySun",
      ordinal = 0
   ), @Range(
      value = "dayMon",
      ordinal = 1
   ), @Range(
      value = "dayTue",
      ordinal = 2
   ), @Range(
      value = "dayWed",
      ordinal = 3
   ), @Range(
      value = "dayThu",
      ordinal = 4
   ), @Range(
      value = "dayFri",
      ordinal = 5
   ), @Range(
      value = "daySat",
      ordinal = 6
   ), @Range(
      value = "dayNul",
      ordinal = -1
   )},
   defaultValue = "dayNul"
)
public final class BLonDaysOfWeekEnum extends BFrozenEnum {
   public static final int DAY_SUN = 0;
   public static final int DAY_MON = 1;
   public static final int DAY_TUE = 2;
   public static final int DAY_WED = 3;
   public static final int DAY_THU = 4;
   public static final int DAY_FRI = 5;
   public static final int DAY_SAT = 6;
   public static final int DAY_NUL = -1;
   public static final BLonDaysOfWeekEnum daySun = new BLonDaysOfWeekEnum(0);
   public static final BLonDaysOfWeekEnum dayMon = new BLonDaysOfWeekEnum(1);
   public static final BLonDaysOfWeekEnum dayTue = new BLonDaysOfWeekEnum(2);
   public static final BLonDaysOfWeekEnum dayWed = new BLonDaysOfWeekEnum(3);
   public static final BLonDaysOfWeekEnum dayThu = new BLonDaysOfWeekEnum(4);
   public static final BLonDaysOfWeekEnum dayFri = new BLonDaysOfWeekEnum(5);
   public static final BLonDaysOfWeekEnum daySat = new BLonDaysOfWeekEnum(6);
   public static final BLonDaysOfWeekEnum dayNul = new BLonDaysOfWeekEnum(-1);
   public static final BLonDaysOfWeekEnum DEFAULT = dayNul;
   public static final Type TYPE = Sys.loadType(BLonDaysOfWeekEnum.class);

   public static BLonDaysOfWeekEnum make(int ordinal) {
      return (BLonDaysOfWeekEnum)daySun.getRange().get(ordinal, false);
   }

   public static BLonDaysOfWeekEnum make(String tag) {
      return (BLonDaysOfWeekEnum)daySun.getRange().get(tag);
   }

   private BLonDaysOfWeekEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
