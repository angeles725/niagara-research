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
      value = "dvPumpCtrl",
      ordinal = 0
   ), @Range(
      value = "dvValvePos",
      ordinal = 1
   ), @Range(
      value = "dvNul",
      ordinal = -1
   )},
   defaultValue = "dvNul"
)
public final class BLonDeviceSelectEnum extends BFrozenEnum {
   public static final int DV_PUMP_CTRL = 0;
   public static final int DV_VALVE_POS = 1;
   public static final int DV_NUL = -1;
   public static final BLonDeviceSelectEnum dvPumpCtrl = new BLonDeviceSelectEnum(0);
   public static final BLonDeviceSelectEnum dvValvePos = new BLonDeviceSelectEnum(1);
   public static final BLonDeviceSelectEnum dvNul = new BLonDeviceSelectEnum(-1);
   public static final BLonDeviceSelectEnum DEFAULT = dvNul;
   public static final Type TYPE = Sys.loadType(BLonDeviceSelectEnum.class);

   public static BLonDeviceSelectEnum make(int ordinal) {
      return (BLonDeviceSelectEnum)dvPumpCtrl.getRange().get(ordinal, false);
   }

   public static BLonDeviceSelectEnum make(String tag) {
      return (BLonDeviceSelectEnum)dvPumpCtrl.getRange().get(tag);
   }

   private BLonDeviceSelectEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
