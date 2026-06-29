package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("direct"), @Range("reverse")}
)
public final class BBacnetAction extends BFrozenEnum {
   public static final int DIRECT = 0;
   public static final int REVERSE = 1;
   public static final BBacnetAction direct = new BBacnetAction(0);
   public static final BBacnetAction reverse = new BBacnetAction(1);
   public static final BBacnetAction DEFAULT = direct;
   public static final Type TYPE = Sys.loadType(BBacnetAction.class);

   public static BBacnetAction make(int ordinal) {
      return (BBacnetAction)direct.getRange().get(ordinal, false);
   }

   public static BBacnetAction make(String tag) {
      return (BBacnetAction)direct.getRange().get(tag);
   }

   private BBacnetAction(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }

   public String getTag(Context cx) {
      if (cx != null) {
         BString tag = (BString)cx.getFacet(this.isActive() ? "trueText" : "falseText");
         return tag != null ? tag.getString() : this.getTag();
      } else {
         return this.getTag();
      }
   }

   public static final BBacnetAction make(boolean value) {
      return value ? reverse : direct;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
