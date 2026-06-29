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
   range = {@Range("inactive"), @Range("active")}
)
public final class BBacnetBinaryPv extends BFrozenEnum {
   public static final int INACTIVE = 0;
   public static final int ACTIVE = 1;
   public static final BBacnetBinaryPv inactive = new BBacnetBinaryPv(0);
   public static final BBacnetBinaryPv active = new BBacnetBinaryPv(1);
   public static final BBacnetBinaryPv DEFAULT = inactive;
   public static final Type TYPE = Sys.loadType(BBacnetBinaryPv.class);

   public static BBacnetBinaryPv make(int ordinal) {
      return (BBacnetBinaryPv)inactive.getRange().get(ordinal, false);
   }

   public static BBacnetBinaryPv make(String tag) {
      return (BBacnetBinaryPv)inactive.getRange().get(tag);
   }

   private BBacnetBinaryPv(int ordinal) {
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

   public static final BBacnetBinaryPv make(boolean value) {
      return value ? active : inactive;
   }

   public String toString(Context context) {
      if (context != null) {
         if (context.equals(BacnetConst.facetsContext)) {
            return this.getTag();
         }

         if (this.isActive()) {
            BString s = (BString)context.getFacet("trueText");
            if (s != null) {
               return s.getString();
            }
         } else {
            BString s = (BString)context.getFacet("falseText");
            if (s != null) {
               return s.getString();
            }
         }
      }

      return this.getDisplayTag(context);
   }

   public boolean isActive() {
      return this.getOrdinal() == 1;
   }
}
