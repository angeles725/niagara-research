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
      value = "chlrOff",
      ordinal = 0
   ), @Range(
      value = "chlrStart",
      ordinal = 1
   ), @Range(
      value = "chlrRun",
      ordinal = 2
   ), @Range(
      value = "chlrPreshutdn",
      ordinal = 3
   ), @Range(
      value = "chlrService",
      ordinal = 4
   ), @Range(
      value = "chlrNull",
      ordinal = -1
   )}
)
public final class BLonChillerEnum extends BFrozenEnum {
   public static final int CHLR_OFF = 0;
   public static final int CHLR_START = 1;
   public static final int CHLR_RUN = 2;
   public static final int CHLR_PRESHUTDN = 3;
   public static final int CHLR_SERVICE = 4;
   public static final int CHLR_NULL = -1;
   public static final BLonChillerEnum chlrOff = new BLonChillerEnum(0);
   public static final BLonChillerEnum chlrStart = new BLonChillerEnum(1);
   public static final BLonChillerEnum chlrRun = new BLonChillerEnum(2);
   public static final BLonChillerEnum chlrPreshutdn = new BLonChillerEnum(3);
   public static final BLonChillerEnum chlrService = new BLonChillerEnum(4);
   public static final BLonChillerEnum chlrNull = new BLonChillerEnum(-1);
   public static final BLonChillerEnum DEFAULT = chlrOff;
   public static final Type TYPE = Sys.loadType(BLonChillerEnum.class);

   public static BLonChillerEnum make(int ordinal) {
      return (BLonChillerEnum)chlrOff.getRange().get(ordinal, false);
   }

   public static BLonChillerEnum make(String tag) {
      return (BLonChillerEnum)chlrOff.getRange().get(tag);
   }

   private BLonChillerEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
