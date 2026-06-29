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
      value = "grpNormal",
      ordinal = 0
   ), @Range(
      value = "grpOutputOnly",
      ordinal = 1
   ), @Range(
      value = "grpInputNoAck",
      ordinal = 2
   )}
)
public final class BLonGroupRestrictionEnum extends BFrozenEnum {
   public static final int GRP_NORMAL = 0;
   public static final int GRP_OUTPUT_ONLY = 1;
   public static final int GRP_INPUT_NO_ACK = 2;
   public static final BLonGroupRestrictionEnum grpNormal = new BLonGroupRestrictionEnum(0);
   public static final BLonGroupRestrictionEnum grpOutputOnly = new BLonGroupRestrictionEnum(1);
   public static final BLonGroupRestrictionEnum grpInputNoAck = new BLonGroupRestrictionEnum(2);
   public static final BLonGroupRestrictionEnum DEFAULT = grpNormal;
   public static final Type TYPE = Sys.loadType(BLonGroupRestrictionEnum.class);

   public static BLonGroupRestrictionEnum make(int ordinal) {
      return (BLonGroupRestrictionEnum)grpNormal.getRange().get(ordinal, false);
   }

   public static BLonGroupRestrictionEnum make(String tag) {
      return (BLonGroupRestrictionEnum)grpNormal.getRange().get(tag);
   }

   private BLonGroupRestrictionEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
