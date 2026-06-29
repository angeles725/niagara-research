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
      value = "ovRetain",
      ordinal = 0
   ), @Range(
      value = "ovSpecified",
      ordinal = 1
   ), @Range(
      value = "ovDefault",
      ordinal = 2
   ), @Range(
      value = "ovNul",
      ordinal = -1
   )}
)
public final class BLonOverrideEnum extends BFrozenEnum {
   public static final int OV_RETAIN = 0;
   public static final int OV_SPECIFIED = 1;
   public static final int OV_DEFAULT = 2;
   public static final int OV_NUL = -1;
   public static final BLonOverrideEnum ovRetain = new BLonOverrideEnum(0);
   public static final BLonOverrideEnum ovSpecified = new BLonOverrideEnum(1);
   public static final BLonOverrideEnum ovDefault = new BLonOverrideEnum(2);
   public static final BLonOverrideEnum ovNul = new BLonOverrideEnum(-1);
   public static final BLonOverrideEnum DEFAULT = ovRetain;
   public static final Type TYPE = Sys.loadType(BLonOverrideEnum.class);

   public static BLonOverrideEnum make(int ordinal) {
      return (BLonOverrideEnum)ovRetain.getRange().get(ordinal, false);
   }

   public static BLonOverrideEnum make(String tag) {
      return (BLonOverrideEnum)ovRetain.getRange().get(tag);
   }

   private BLonOverrideEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
