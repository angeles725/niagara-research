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
      value = "dfmModeAmbient",
      ordinal = 0
   ), @Range(
      value = "dfmModeForced",
      ordinal = 1
   ), @Range(
      value = "dfmModeSync",
      ordinal = 2
   ), @Range(
      value = "dfmNul",
      ordinal = -1
   )}
)
public final class BLonDefrostModeEnum extends BFrozenEnum {
   public static final int DFM_MODE_AMBIENT = 0;
   public static final int DFM_MODE_FORCED = 1;
   public static final int DFM_MODE_SYNC = 2;
   public static final int DFM_NUL = -1;
   public static final BLonDefrostModeEnum dfmModeAmbient = new BLonDefrostModeEnum(0);
   public static final BLonDefrostModeEnum dfmModeForced = new BLonDefrostModeEnum(1);
   public static final BLonDefrostModeEnum dfmModeSync = new BLonDefrostModeEnum(2);
   public static final BLonDefrostModeEnum dfmNul = new BLonDefrostModeEnum(-1);
   public static final BLonDefrostModeEnum DEFAULT = dfmModeAmbient;
   public static final Type TYPE = Sys.loadType(BLonDefrostModeEnum.class);

   public static BLonDefrostModeEnum make(int ordinal) {
      return (BLonDefrostModeEnum)dfmModeAmbient.getRange().get(ordinal, false);
   }

   public static BLonDefrostModeEnum make(String tag) {
      return (BLonDefrostModeEnum)dfmModeAmbient.getRange().get(tag);
   }

   private BLonDefrostModeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
