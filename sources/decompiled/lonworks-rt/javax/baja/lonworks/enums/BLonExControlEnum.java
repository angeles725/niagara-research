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
      value = "exControlNone",
      ordinal = 0
   ), @Range(
      value = "exControlOther",
      ordinal = 1
   ), @Range(
      value = "exControlThisAddr",
      ordinal = 2
   ), @Range(
      value = "exControlNul",
      ordinal = -1
   )}
)
public final class BLonExControlEnum extends BFrozenEnum {
   public static final int EX_CONTROL_NONE = 0;
   public static final int EX_CONTROL_OTHER = 1;
   public static final int EX_CONTROL_THIS_ADDR = 2;
   public static final int EX_CONTROL_NUL = -1;
   public static final BLonExControlEnum exControlNone = new BLonExControlEnum(0);
   public static final BLonExControlEnum exControlOther = new BLonExControlEnum(1);
   public static final BLonExControlEnum exControlThisAddr = new BLonExControlEnum(2);
   public static final BLonExControlEnum exControlNul = new BLonExControlEnum(-1);
   public static final BLonExControlEnum DEFAULT = exControlNone;
   public static final Type TYPE = Sys.loadType(BLonExControlEnum.class);

   public static BLonExControlEnum make(int ordinal) {
      return (BLonExControlEnum)exControlNone.getRange().get(ordinal, false);
   }

   public static BLonExControlEnum make(String tag) {
      return (BLonExControlEnum)exControlNone.getRange().get(tag);
   }

   private BLonExControlEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
