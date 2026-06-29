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
      value = "hvacAuto",
      ordinal = 0
   ), @Range(
      value = "hvacHeat",
      ordinal = 1
   ), @Range(
      value = "hvacMrngWrmup",
      ordinal = 2
   ), @Range(
      value = "hvacCool",
      ordinal = 3
   ), @Range(
      value = "hvacNightPurge",
      ordinal = 4
   ), @Range(
      value = "hvacPreCool",
      ordinal = 5
   ), @Range(
      value = "hvacOff",
      ordinal = 6
   ), @Range(
      value = "hvacTest",
      ordinal = 7
   ), @Range(
      value = "hvacEmergHeat",
      ordinal = 8
   ), @Range(
      value = "hvacFanOnly",
      ordinal = 9
   ), @Range(
      value = "hvacFreeCool",
      ordinal = 10
   ), @Range(
      value = "hvacIce",
      ordinal = 11
   ), @Range(
      value = "hvacMaxHeat",
      ordinal = 12
   ), @Range(
      value = "hvacEconomy",
      ordinal = 13
   ), @Range(
      value = "hvacDehumid",
      ordinal = 14
   ), @Range(
      value = "hvacCalibrate",
      ordinal = 15
   ), @Range(
      value = "hvacEmergCool",
      ordinal = 16
   ), @Range(
      value = "hvacEmergSteam",
      ordinal = 17
   ), @Range(
      value = "hvacNul",
      ordinal = -1
   )},
   defaultValue = "hvacNul"
)
public final class BLonHvacEnum extends BFrozenEnum {
   public static final int HVAC_AUTO = 0;
   public static final int HVAC_HEAT = 1;
   public static final int HVAC_MRNG_WRMUP = 2;
   public static final int HVAC_COOL = 3;
   public static final int HVAC_NIGHT_PURGE = 4;
   public static final int HVAC_PRE_COOL = 5;
   public static final int HVAC_OFF = 6;
   public static final int HVAC_TEST = 7;
   public static final int HVAC_EMERG_HEAT = 8;
   public static final int HVAC_FAN_ONLY = 9;
   public static final int HVAC_FREE_COOL = 10;
   public static final int HVAC_ICE = 11;
   public static final int HVAC_MAX_HEAT = 12;
   public static final int HVAC_ECONOMY = 13;
   public static final int HVAC_DEHUMID = 14;
   public static final int HVAC_CALIBRATE = 15;
   public static final int HVAC_EMERG_COOL = 16;
   public static final int HVAC_EMERG_STEAM = 17;
   public static final int HVAC_NUL = -1;
   public static final BLonHvacEnum hvacAuto = new BLonHvacEnum(0);
   public static final BLonHvacEnum hvacHeat = new BLonHvacEnum(1);
   public static final BLonHvacEnum hvacMrngWrmup = new BLonHvacEnum(2);
   public static final BLonHvacEnum hvacCool = new BLonHvacEnum(3);
   public static final BLonHvacEnum hvacNightPurge = new BLonHvacEnum(4);
   public static final BLonHvacEnum hvacPreCool = new BLonHvacEnum(5);
   public static final BLonHvacEnum hvacOff = new BLonHvacEnum(6);
   public static final BLonHvacEnum hvacTest = new BLonHvacEnum(7);
   public static final BLonHvacEnum hvacEmergHeat = new BLonHvacEnum(8);
   public static final BLonHvacEnum hvacFanOnly = new BLonHvacEnum(9);
   public static final BLonHvacEnum hvacFreeCool = new BLonHvacEnum(10);
   public static final BLonHvacEnum hvacIce = new BLonHvacEnum(11);
   public static final BLonHvacEnum hvacMaxHeat = new BLonHvacEnum(12);
   public static final BLonHvacEnum hvacEconomy = new BLonHvacEnum(13);
   public static final BLonHvacEnum hvacDehumid = new BLonHvacEnum(14);
   public static final BLonHvacEnum hvacCalibrate = new BLonHvacEnum(15);
   public static final BLonHvacEnum hvacEmergCool = new BLonHvacEnum(16);
   public static final BLonHvacEnum hvacEmergSteam = new BLonHvacEnum(17);
   public static final BLonHvacEnum hvacNul = new BLonHvacEnum(-1);
   public static final BLonHvacEnum DEFAULT = hvacNul;
   public static final Type TYPE = Sys.loadType(BLonHvacEnum.class);

   public static BLonHvacEnum make(int ordinal) {
      return (BLonHvacEnum)hvacAuto.getRange().get(ordinal, false);
   }

   public static BLonHvacEnum make(String tag) {
      return (BLonHvacEnum)hvacAuto.getRange().get(tag);
   }

   private BLonHvacEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
