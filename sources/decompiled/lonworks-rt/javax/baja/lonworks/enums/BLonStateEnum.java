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
      value = "stOn",
      ordinal = 1
   ), @Range(
      value = "stNul",
      ordinal = -1
   )}
)
public final class BLonStateEnum extends BFrozenEnum {
   public static final int ST_OFF = 0;
   public static final int ST_ON = 1;
   public static final int ST_NUL = -1;
   public static final BLonStateEnum stOff = new BLonStateEnum(0);
   public static final BLonStateEnum stOn = new BLonStateEnum(1);
   public static final BLonStateEnum stNul = new BLonStateEnum(-1);
   public static final BLonStateEnum DEFAULT = stOff;
   public static final Type TYPE = Sys.loadType(BLonStateEnum.class);

   public static BLonStateEnum make(int ordinal) {
      return (BLonStateEnum)stOff.getRange().get(ordinal, false);
   }

   public static BLonStateEnum make(String tag) {
      return (BLonStateEnum)stOff.getRange().get(tag);
   }

   private BLonStateEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
