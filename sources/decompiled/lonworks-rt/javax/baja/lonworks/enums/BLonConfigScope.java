package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("node"), @Range("object"), @Range("nv")}
)
public final class BLonConfigScope extends BFrozenEnum {
   public static final int NODE = 0;
   public static final int OBJECT = 1;
   public static final int NV = 2;
   public static final BLonConfigScope node = new BLonConfigScope(0);
   public static final BLonConfigScope object = new BLonConfigScope(1);
   public static final BLonConfigScope nv = new BLonConfigScope(2);
   public static final BLonConfigScope DEFAULT = node;
   public static final Type TYPE = Sys.loadType(BLonConfigScope.class);

   public static BLonConfigScope make(int ordinal) {
      return (BLonConfigScope)node.getRange().get(ordinal, false);
   }

   public static BLonConfigScope make(String tag) {
      return (BLonConfigScope)node.getRange().get(tag);
   }

   private BLonConfigScope(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
