package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("input"), @Range("output")}
)
public final class BLonNvDirection extends BFrozenEnum {
   public static final int INPUT = 0;
   public static final int OUTPUT = 1;
   public static final BLonNvDirection input = new BLonNvDirection(0);
   public static final BLonNvDirection output = new BLonNvDirection(1);
   public static final BLonNvDirection DEFAULT = input;
   public static final Type TYPE = Sys.loadType(BLonNvDirection.class);

   public static BLonNvDirection make(int ordinal) {
      return (BLonNvDirection)input.getRange().get(ordinal, false);
   }

   public static BLonNvDirection make(String tag) {
      return (BLonNvDirection)input.getRange().get(tag);
   }

   private BLonNvDirection(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public BLonNvDirection reverse() {
      return this == input ? output : input;
   }
}
