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
      value = "ipLinear",
      ordinal = 0
   ), @Range(
      value = "ipCubicSpline",
      ordinal = 1
   ), @Range(
      value = "ipReserved",
      ordinal = 2
   ), @Range(
      value = "ipNul",
      ordinal = -1
   )}
)
public final class BLonInterpEnum extends BFrozenEnum {
   public static final int IP_LINEAR = 0;
   public static final int IP_CUBIC_SPLINE = 1;
   public static final int IP_RESERVED = 2;
   public static final int IP_NUL = -1;
   public static final BLonInterpEnum ipLinear = new BLonInterpEnum(0);
   public static final BLonInterpEnum ipCubicSpline = new BLonInterpEnum(1);
   public static final BLonInterpEnum ipReserved = new BLonInterpEnum(2);
   public static final BLonInterpEnum ipNul = new BLonInterpEnum(-1);
   public static final BLonInterpEnum DEFAULT = ipLinear;
   public static final Type TYPE = Sys.loadType(BLonInterpEnum.class);

   public static BLonInterpEnum make(int ordinal) {
      return (BLonInterpEnum)ipLinear.getRange().get(ordinal, false);
   }

   public static BLonInterpEnum make(String tag) {
      return (BLonInterpEnum)ipLinear.getRange().get(tag);
   }

   private BLonInterpEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
