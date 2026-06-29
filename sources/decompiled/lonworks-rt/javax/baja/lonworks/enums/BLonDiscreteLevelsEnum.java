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
      value = "stOff",
      ordinal = 0
   ), @Range(
      value = "stLow",
      ordinal = 1
   ), @Range(
      value = "stMed",
      ordinal = 2
   ), @Range(
      value = "stHigh",
      ordinal = 3
   ), @Range(
      value = "stOn",
      ordinal = 4
   ), @Range(
      value = "stNul",
      ordinal = -1
   )}
)
public final class BLonDiscreteLevelsEnum extends BFrozenEnum {
   public static final int ST_OFF = 0;
   public static final int ST_LOW = 1;
   public static final int ST_MED = 2;
   public static final int ST_HIGH = 3;
   public static final int ST_ON = 4;
   public static final int ST_NUL = -1;
   public static final BLonDiscreteLevelsEnum stOff = new BLonDiscreteLevelsEnum(0);
   public static final BLonDiscreteLevelsEnum stLow = new BLonDiscreteLevelsEnum(1);
   public static final BLonDiscreteLevelsEnum stMed = new BLonDiscreteLevelsEnum(2);
   public static final BLonDiscreteLevelsEnum stHigh = new BLonDiscreteLevelsEnum(3);
   public static final BLonDiscreteLevelsEnum stOn = new BLonDiscreteLevelsEnum(4);
   public static final BLonDiscreteLevelsEnum stNul = new BLonDiscreteLevelsEnum(-1);
   public static final BLonDiscreteLevelsEnum DEFAULT = stOff;
   public static final Type TYPE = Sys.loadType(BLonDiscreteLevelsEnum.class);

   public static BLonDiscreteLevelsEnum make(int ordinal) {
      return (BLonDiscreteLevelsEnum)stOff.getRange().get(ordinal, false);
   }

   public static BLonDiscreteLevelsEnum make(String tag) {
      return (BLonDiscreteLevelsEnum)stOff.getRange().get(tag);
   }

   private BLonDiscreteLevelsEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
