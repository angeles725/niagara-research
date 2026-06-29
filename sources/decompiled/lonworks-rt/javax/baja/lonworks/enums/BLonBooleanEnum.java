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
      value = "boolFalse",
      ordinal = 0
   ), @Range(
      value = "boolTrue",
      ordinal = 1
   ), @Range(
      value = "boolNul",
      ordinal = -1
   )}
)
public final class BLonBooleanEnum extends BFrozenEnum {
   public static final int BOOL_FALSE = 0;
   public static final int BOOL_TRUE = 1;
   public static final int BOOL_NUL = -1;
   public static final BLonBooleanEnum boolFalse = new BLonBooleanEnum(0);
   public static final BLonBooleanEnum boolTrue = new BLonBooleanEnum(1);
   public static final BLonBooleanEnum boolNul = new BLonBooleanEnum(-1);
   public static final BLonBooleanEnum DEFAULT = boolFalse;
   public static final Type TYPE = Sys.loadType(BLonBooleanEnum.class);

   public static BLonBooleanEnum make(int ordinal) {
      return (BLonBooleanEnum)boolFalse.getRange().get(ordinal, false);
   }

   public static BLonBooleanEnum make(String tag) {
      return (BLonBooleanEnum)boolFalse.getRange().get(tag);
   }

   private BLonBooleanEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
