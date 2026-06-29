package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("nil")}
)
public final class BLonNilEnum extends BFrozenEnum {
   public static final int NIL = 0;
   public static final BLonNilEnum nil = new BLonNilEnum(0);
   public static final BLonNilEnum DEFAULT = nil;
   public static final Type TYPE = Sys.loadType(BLonNilEnum.class);

   public static BLonNilEnum make(int ordinal) {
      return (BLonNilEnum)nil.getRange().get(ordinal, false);
   }

   public static BLonNilEnum make(String tag) {
      return (BLonNilEnum)nil.getRange().get(tag);
   }

   private BLonNilEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
