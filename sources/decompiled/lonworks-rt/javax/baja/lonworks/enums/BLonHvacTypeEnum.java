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
      value = "hvtGeneric",
      ordinal = 0
   ), @Range(
      value = "hvtFanCoil",
      ordinal = 1
   ), @Range(
      value = "hvtVav",
      ordinal = 2
   ), @Range(
      value = "hvtHeatPump",
      ordinal = 3
   ), @Range(
      value = "hvtRooftop",
      ordinal = 4
   ), @Range(
      value = "hvtUnitVent",
      ordinal = 5
   ), @Range(
      value = "hvtChillCeil",
      ordinal = 6
   ), @Range(
      value = "hvtRadiator",
      ordinal = 7
   ), @Range(
      value = "hvtAhu",
      ordinal = 8
   ), @Range(
      value = "hvtSelfCont",
      ordinal = 9
   ), @Range(
      value = "hvtNul",
      ordinal = -1
   )}
)
public final class BLonHvacTypeEnum extends BFrozenEnum {
   public static final int HVT_GENERIC = 0;
   public static final int HVT_FAN_COIL = 1;
   public static final int HVT_VAV = 2;
   public static final int HVT_HEAT_PUMP = 3;
   public static final int HVT_ROOFTOP = 4;
   public static final int HVT_UNIT_VENT = 5;
   public static final int HVT_CHILL_CEIL = 6;
   public static final int HVT_RADIATOR = 7;
   public static final int HVT_AHU = 8;
   public static final int HVT_SELF_CONT = 9;
   public static final int HVT_NUL = -1;
   public static final BLonHvacTypeEnum hvtGeneric = new BLonHvacTypeEnum(0);
   public static final BLonHvacTypeEnum hvtFanCoil = new BLonHvacTypeEnum(1);
   public static final BLonHvacTypeEnum hvtVav = new BLonHvacTypeEnum(2);
   public static final BLonHvacTypeEnum hvtHeatPump = new BLonHvacTypeEnum(3);
   public static final BLonHvacTypeEnum hvtRooftop = new BLonHvacTypeEnum(4);
   public static final BLonHvacTypeEnum hvtUnitVent = new BLonHvacTypeEnum(5);
   public static final BLonHvacTypeEnum hvtChillCeil = new BLonHvacTypeEnum(6);
   public static final BLonHvacTypeEnum hvtRadiator = new BLonHvacTypeEnum(7);
   public static final BLonHvacTypeEnum hvtAhu = new BLonHvacTypeEnum(8);
   public static final BLonHvacTypeEnum hvtSelfCont = new BLonHvacTypeEnum(9);
   public static final BLonHvacTypeEnum hvtNul = new BLonHvacTypeEnum(-1);
   public static final BLonHvacTypeEnum DEFAULT = hvtGeneric;
   public static final Type TYPE = Sys.loadType(BLonHvacTypeEnum.class);

   public static BLonHvacTypeEnum make(int ordinal) {
      return (BLonHvacTypeEnum)hvtGeneric.getRange().get(ordinal, false);
   }

   public static BLonHvacTypeEnum make(String tag) {
      return (BLonHvacTypeEnum)hvtGeneric.getRange().get(tag);
   }

   private BLonHvacTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
