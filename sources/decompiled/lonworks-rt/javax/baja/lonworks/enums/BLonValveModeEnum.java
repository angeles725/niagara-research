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
      value = "valveNormal",
      ordinal = 0
   ), @Range(
      value = "valveCooling",
      ordinal = 1
   ), @Range(
      value = "valveHeating",
      ordinal = 2
   ), @Range(
      value = "valveEmergency",
      ordinal = 3
   ), @Range(
      value = "valveStrokeAdp",
      ordinal = 4
   ), @Range(
      value = "valveStrokeSyn",
      ordinal = 5
   ), @Range(
      value = "valveError",
      ordinal = 6
   ), @Range(
      value = "valveOverridden",
      ordinal = 7
   ), @Range(
      value = "valveNul",
      ordinal = -1
   )}
)
public final class BLonValveModeEnum extends BFrozenEnum {
   public static final int VALVE_NORMAL = 0;
   public static final int VALVE_COOLING = 1;
   public static final int VALVE_HEATING = 2;
   public static final int VALVE_EMERGENCY = 3;
   public static final int VALVE_STROKE_ADP = 4;
   public static final int VALVE_STROKE_SYN = 5;
   public static final int VALVE_ERROR = 6;
   public static final int VALVE_OVERRIDDEN = 7;
   public static final int VALVE_NUL = -1;
   public static final BLonValveModeEnum valveNormal = new BLonValveModeEnum(0);
   public static final BLonValveModeEnum valveCooling = new BLonValveModeEnum(1);
   public static final BLonValveModeEnum valveHeating = new BLonValveModeEnum(2);
   public static final BLonValveModeEnum valveEmergency = new BLonValveModeEnum(3);
   public static final BLonValveModeEnum valveStrokeAdp = new BLonValveModeEnum(4);
   public static final BLonValveModeEnum valveStrokeSyn = new BLonValveModeEnum(5);
   public static final BLonValveModeEnum valveError = new BLonValveModeEnum(6);
   public static final BLonValveModeEnum valveOverridden = new BLonValveModeEnum(7);
   public static final BLonValveModeEnum valveNul = new BLonValveModeEnum(-1);
   public static final BLonValveModeEnum DEFAULT = valveNormal;
   public static final Type TYPE = Sys.loadType(BLonValveModeEnum.class);

   public static BLonValveModeEnum make(int ordinal) {
      return (BLonValveModeEnum)valveNormal.getRange().get(ordinal, false);
   }

   public static BLonValveModeEnum make(String tag) {
      return (BLonValveModeEnum)valveNormal.getRange().get(tag);
   }

   private BLonValveModeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
