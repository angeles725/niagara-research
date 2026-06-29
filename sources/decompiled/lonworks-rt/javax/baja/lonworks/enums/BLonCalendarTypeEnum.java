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
      value = "calGreg",
      ordinal = 0
   ), @Range(
      value = "calJul",
      ordinal = 1
   ), @Range(
      value = "calMeu",
      ordinal = 2
   ), @Range(
      value = "calNul",
      ordinal = -1
   )}
)
public final class BLonCalendarTypeEnum extends BFrozenEnum {
   public static final int CAL_GREG = 0;
   public static final int CAL_JUL = 1;
   public static final int CAL_MEU = 2;
   public static final int CAL_NUL = -1;
   public static final BLonCalendarTypeEnum calGreg = new BLonCalendarTypeEnum(0);
   public static final BLonCalendarTypeEnum calJul = new BLonCalendarTypeEnum(1);
   public static final BLonCalendarTypeEnum calMeu = new BLonCalendarTypeEnum(2);
   public static final BLonCalendarTypeEnum calNul = new BLonCalendarTypeEnum(-1);
   public static final BLonCalendarTypeEnum DEFAULT = calGreg;
   public static final Type TYPE = Sys.loadType(BLonCalendarTypeEnum.class);

   public static BLonCalendarTypeEnum make(int ordinal) {
      return (BLonCalendarTypeEnum)calGreg.getRange().get(ordinal, false);
   }

   public static BLonCalendarTypeEnum make(String tag) {
      return (BLonCalendarTypeEnum)calGreg.getRange().get(tag);
   }

   private BLonCalendarTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
