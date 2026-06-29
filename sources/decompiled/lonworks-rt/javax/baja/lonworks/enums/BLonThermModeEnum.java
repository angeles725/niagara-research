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
      value = "thermNoControl",
      ordinal = 0
   ), @Range(
      value = "thermInOut",
      ordinal = 1
   ), @Range(
      value = "thermModulating",
      ordinal = 2
   ), @Range(
      value = "thermNul",
      ordinal = -1
   )}
)
public final class BLonThermModeEnum extends BFrozenEnum {
   public static final int THERM_NO_CONTROL = 0;
   public static final int THERM_IN_OUT = 1;
   public static final int THERM_MODULATING = 2;
   public static final int THERM_NUL = -1;
   public static final BLonThermModeEnum thermNoControl = new BLonThermModeEnum(0);
   public static final BLonThermModeEnum thermInOut = new BLonThermModeEnum(1);
   public static final BLonThermModeEnum thermModulating = new BLonThermModeEnum(2);
   public static final BLonThermModeEnum thermNul = new BLonThermModeEnum(-1);
   public static final BLonThermModeEnum DEFAULT = thermNoControl;
   public static final Type TYPE = Sys.loadType(BLonThermModeEnum.class);

   public static BLonThermModeEnum make(int ordinal) {
      return (BLonThermModeEnum)thermNoControl.getRange().get(ordinal, false);
   }

   public static BLonThermModeEnum make(String tag) {
      return (BLonThermModeEnum)thermNoControl.getRange().get(tag);
   }

   private BLonThermModeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
