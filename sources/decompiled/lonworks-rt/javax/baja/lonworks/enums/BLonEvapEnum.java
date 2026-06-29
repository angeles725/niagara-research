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
      value = "evapNoCooling",
      ordinal = 0
   ), @Range(
      value = "evapCooling",
      ordinal = 1
   ), @Range(
      value = "evapEmergCooling",
      ordinal = 2
   ), @Range(
      value = "evapNul",
      ordinal = -1
   )}
)
public final class BLonEvapEnum extends BFrozenEnum {
   public static final int EVAP_NO_COOLING = 0;
   public static final int EVAP_COOLING = 1;
   public static final int EVAP_EMERG_COOLING = 2;
   public static final int EVAP_NUL = -1;
   public static final BLonEvapEnum evapNoCooling = new BLonEvapEnum(0);
   public static final BLonEvapEnum evapCooling = new BLonEvapEnum(1);
   public static final BLonEvapEnum evapEmergCooling = new BLonEvapEnum(2);
   public static final BLonEvapEnum evapNul = new BLonEvapEnum(-1);
   public static final BLonEvapEnum DEFAULT = evapNoCooling;
   public static final Type TYPE = Sys.loadType(BLonEvapEnum.class);

   public static BLonEvapEnum make(int ordinal) {
      return (BLonEvapEnum)evapNoCooling.getRange().get(ordinal, false);
   }

   public static BLonEvapEnum make(String tag) {
      return (BLonEvapEnum)evapNoCooling.getRange().get(tag);
   }

   private BLonEvapEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
