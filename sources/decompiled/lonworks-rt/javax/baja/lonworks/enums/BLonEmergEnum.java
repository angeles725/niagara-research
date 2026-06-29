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
      value = "emergNormal",
      ordinal = 0
   ), @Range(
      value = "emergPressurize",
      ordinal = 1
   ), @Range(
      value = "emergDepressurize",
      ordinal = 2
   ), @Range(
      value = "emergPurge",
      ordinal = 3
   ), @Range(
      value = "emergShutdown",
      ordinal = 4
   ), @Range(
      value = "emergFire",
      ordinal = 5
   ), @Range(
      value = "emergNul",
      ordinal = -1
   )}
)
public final class BLonEmergEnum extends BFrozenEnum {
   public static final int EMERG_NORMAL = 0;
   public static final int EMERG_PRESSURIZE = 1;
   public static final int EMERG_DEPRESSURIZE = 2;
   public static final int EMERG_PURGE = 3;
   public static final int EMERG_SHUTDOWN = 4;
   public static final int EMERG_FIRE = 5;
   public static final int EMERG_NUL = -1;
   public static final BLonEmergEnum emergNormal = new BLonEmergEnum(0);
   public static final BLonEmergEnum emergPressurize = new BLonEmergEnum(1);
   public static final BLonEmergEnum emergDepressurize = new BLonEmergEnum(2);
   public static final BLonEmergEnum emergPurge = new BLonEmergEnum(3);
   public static final BLonEmergEnum emergShutdown = new BLonEmergEnum(4);
   public static final BLonEmergEnum emergFire = new BLonEmergEnum(5);
   public static final BLonEmergEnum emergNul = new BLonEmergEnum(-1);
   public static final BLonEmergEnum DEFAULT = emergNormal;
   public static final Type TYPE = Sys.loadType(BLonEmergEnum.class);

   public static BLonEmergEnum make(int ordinal) {
      return (BLonEmergEnum)emergNormal.getRange().get(ordinal, false);
   }

   public static BLonEmergEnum make(String tag) {
      return (BLonEmergEnum)emergNormal.getRange().get(tag);
   }

   private BLonEmergEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
