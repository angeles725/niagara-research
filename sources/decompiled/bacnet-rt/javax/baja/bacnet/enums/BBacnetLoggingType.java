package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("polled"), @Range("cov"), @Range("triggered")}
)
public final class BBacnetLoggingType extends BFrozenEnum implements BacnetConst {
   public static final int POLLED = 0;
   public static final int COV = 1;
   public static final int TRIGGERED = 2;
   public static final BBacnetLoggingType polled = new BBacnetLoggingType(0);
   public static final BBacnetLoggingType cov = new BBacnetLoggingType(1);
   public static final BBacnetLoggingType triggered = new BBacnetLoggingType(2);
   public static final BBacnetLoggingType DEFAULT = polled;
   public static final Type TYPE = Sys.loadType(BBacnetLoggingType.class);
   public static final int MAX_ASHRAE_ID = 3;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 255;

   public static BBacnetLoggingType make(int ordinal) {
      return (BBacnetLoggingType)polled.getRange().get(ordinal, false);
   }

   public static BBacnetLoggingType make(String tag) {
      return (BBacnetLoggingType)polled.getRange().get(tag);
   }

   private BBacnetLoggingType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      if (DEFAULT.getRange().isOrdinal(id)) {
         return DEFAULT.getRange().getTag(id);
      } else if (isAshrae(id)) {
         return ASHRAE_PREFIX + id;
      } else if (isProprietary(id)) {
         return PROPRIETARY_PREFIX + id;
      } else {
         throw new InvalidEnumException(id);
      }
   }

   public static int ordinal(String tag) {
      try {
         return DEFAULT.getRange().tagToOrdinal(tag);
      } catch (InvalidEnumException var2) {
         if (tag.startsWith(ASHRAE_PREFIX)) {
            return Integer.parseInt(tag.substring(ASHRAE_PREFIX_LENGTH));
         } else if (tag.startsWith(PROPRIETARY_PREFIX)) {
            return Integer.parseInt(tag.substring(PROPRIETARY_PREFIX_LENGTH));
         } else {
            throw var2;
         }
      }
   }

   public static boolean isProprietary(int id) {
      return id > 63 && id <= 255;
   }

   public static boolean isAshrae(int id) {
      return id > 3 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 255;
   }

   public static boolean isFixed(int id) {
      return id <= 3;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
