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
      value = "setOff",
      ordinal = 0
   ), @Range(
      value = "setOn",
      ordinal = 1
   ), @Range(
      value = "setDown",
      ordinal = 2
   ), @Range(
      value = "setUp",
      ordinal = 3
   ), @Range(
      value = "setStop",
      ordinal = 4
   ), @Range(
      value = "setState",
      ordinal = 5
   ), @Range(
      value = "setNul",
      ordinal = -1
   )}
)
public final class BLonSettingEnum extends BFrozenEnum {
   public static final int SET_OFF = 0;
   public static final int SET_ON = 1;
   public static final int SET_DOWN = 2;
   public static final int SET_UP = 3;
   public static final int SET_STOP = 4;
   public static final int SET_STATE = 5;
   public static final int SET_NUL = -1;
   public static final BLonSettingEnum setOff = new BLonSettingEnum(0);
   public static final BLonSettingEnum setOn = new BLonSettingEnum(1);
   public static final BLonSettingEnum setDown = new BLonSettingEnum(2);
   public static final BLonSettingEnum setUp = new BLonSettingEnum(3);
   public static final BLonSettingEnum setStop = new BLonSettingEnum(4);
   public static final BLonSettingEnum setState = new BLonSettingEnum(5);
   public static final BLonSettingEnum setNul = new BLonSettingEnum(-1);
   public static final BLonSettingEnum DEFAULT = setOff;
   public static final Type TYPE = Sys.loadType(BLonSettingEnum.class);

   public static BLonSettingEnum make(int ordinal) {
      return (BLonSettingEnum)setOff.getRange().get(ordinal, false);
   }

   public static BLonSettingEnum make(String tag) {
      return (BLonSettingEnum)setOff.getRange().get(tag);
   }

   private BLonSettingEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
