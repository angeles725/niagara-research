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
      value = "ftNormal",
      ordinal = 0
   ), @Range(
      value = "ftReset",
      ordinal = 1
   ), @Range(
      value = "ftTest",
      ordinal = 2
   ), @Range(
      value = "ftNotest",
      ordinal = 3
   ), @Range(
      value = "ftNul",
      ordinal = -1
   )}
)
public final class BLonFireTestEnum extends BFrozenEnum {
   public static final int FT_NORMAL = 0;
   public static final int FT_RESET = 1;
   public static final int FT_TEST = 2;
   public static final int FT_NOTEST = 3;
   public static final int FT_NUL = -1;
   public static final BLonFireTestEnum ftNormal = new BLonFireTestEnum(0);
   public static final BLonFireTestEnum ftReset = new BLonFireTestEnum(1);
   public static final BLonFireTestEnum ftTest = new BLonFireTestEnum(2);
   public static final BLonFireTestEnum ftNotest = new BLonFireTestEnum(3);
   public static final BLonFireTestEnum ftNul = new BLonFireTestEnum(-1);
   public static final BLonFireTestEnum DEFAULT = ftNormal;
   public static final Type TYPE = Sys.loadType(BLonFireTestEnum.class);

   public static BLonFireTestEnum make(int ordinal) {
      return (BLonFireTestEnum)ftNormal.getRange().get(ordinal, false);
   }

   public static BLonFireTestEnum make(String tag) {
      return (BLonFireTestEnum)ftNormal.getRange().get(tag);
   }

   private BLonFireTestEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
