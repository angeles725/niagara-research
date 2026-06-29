package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("normal"), @Range("reverse")}
)
public final class BBacnetPolarity extends BFrozenEnum {
   public static final int NORMAL = 0;
   public static final int REVERSE = 1;
   public static final BBacnetPolarity normal = new BBacnetPolarity(0);
   public static final BBacnetPolarity reverse = new BBacnetPolarity(1);
   public static final BBacnetPolarity DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BBacnetPolarity.class);

   public static BBacnetPolarity make(int ordinal) {
      return (BBacnetPolarity)normal.getRange().get(ordinal, false);
   }

   public static BBacnetPolarity make(String tag) {
      return (BBacnetPolarity)normal.getRange().get(tag);
   }

   private BBacnetPolarity(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }

   public static final BBacnetPolarity make(boolean value) {
      return value ? reverse : normal;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
