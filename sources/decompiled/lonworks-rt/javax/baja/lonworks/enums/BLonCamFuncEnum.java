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
      value = "cmfRel",
      ordinal = 0
   ), @Range(
      value = "cmfTour",
      ordinal = 1
   ), @Range(
      value = "cmfAbs",
      ordinal = 2
   ), @Range(
      value = "cmfNul",
      ordinal = -1
   )},
   defaultValue = "cmfNul"
)
public final class BLonCamFuncEnum extends BFrozenEnum {
   public static final int CMF_REL = 0;
   public static final int CMF_TOUR = 1;
   public static final int CMF_ABS = 2;
   public static final int CMF_NUL = -1;
   public static final BLonCamFuncEnum cmfRel = new BLonCamFuncEnum(0);
   public static final BLonCamFuncEnum cmfTour = new BLonCamFuncEnum(1);
   public static final BLonCamFuncEnum cmfAbs = new BLonCamFuncEnum(2);
   public static final BLonCamFuncEnum cmfNul = new BLonCamFuncEnum(-1);
   public static final BLonCamFuncEnum DEFAULT = cmfNul;
   public static final Type TYPE = Sys.loadType(BLonCamFuncEnum.class);

   public static BLonCamFuncEnum make(int ordinal) {
      return (BLonCamFuncEnum)cmfRel.getRange().get(ordinal, false);
   }

   public static BLonCamFuncEnum make(String tag) {
      return (BLonCamFuncEnum)cmfRel.getRange().get(tag);
   }

   private BLonCamFuncEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
