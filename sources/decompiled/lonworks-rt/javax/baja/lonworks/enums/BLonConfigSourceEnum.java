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
      value = "cfgLocal",
      ordinal = 0
   ), @Range(
      value = "cfgExternal",
      ordinal = 1
   ), @Range(
      value = "cfgNul",
      ordinal = -1
   )}
)
public final class BLonConfigSourceEnum extends BFrozenEnum {
   public static final int CFG_LOCAL = 0;
   public static final int CFG_EXTERNAL = 1;
   public static final int CFG_NUL = -1;
   public static final BLonConfigSourceEnum cfgLocal = new BLonConfigSourceEnum(0);
   public static final BLonConfigSourceEnum cfgExternal = new BLonConfigSourceEnum(1);
   public static final BLonConfigSourceEnum cfgNul = new BLonConfigSourceEnum(-1);
   public static final BLonConfigSourceEnum DEFAULT = cfgLocal;
   public static final Type TYPE = Sys.loadType(BLonConfigSourceEnum.class);

   public static BLonConfigSourceEnum make(int ordinal) {
      return (BLonConfigSourceEnum)cfgLocal.getRange().get(ordinal, false);
   }

   public static BLonConfigSourceEnum make(String tag) {
      return (BLonConfigSourceEnum)cfgLocal.getRange().get(tag);
   }

   private BLonConfigSourceEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
