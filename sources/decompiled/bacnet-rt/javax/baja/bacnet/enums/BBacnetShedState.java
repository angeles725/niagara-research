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
   range = {@Range("shedInactive"), @Range("shedRequestPending"), @Range("shedCompliant"), @Range("shedNonCompliant")}
)
public final class BBacnetShedState extends BFrozenEnum implements BacnetConst {
   public static final int SHED_INACTIVE = 0;
   public static final int SHED_REQUEST_PENDING = 1;
   public static final int SHED_COMPLIANT = 2;
   public static final int SHED_NON_COMPLIANT = 3;
   public static final BBacnetShedState shedInactive = new BBacnetShedState(0);
   public static final BBacnetShedState shedRequestPending = new BBacnetShedState(1);
   public static final BBacnetShedState shedCompliant = new BBacnetShedState(2);
   public static final BBacnetShedState shedNonCompliant = new BBacnetShedState(3);
   public static final BBacnetShedState DEFAULT = shedInactive;
   public static final Type TYPE = Sys.loadType(BBacnetShedState.class);
   public static final int MAX_ASHRAE_ID = 3;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetShedState make(int ordinal) {
      return (BBacnetShedState)shedInactive.getRange().get(ordinal, false);
   }

   public static BBacnetShedState make(String tag) {
      return (BBacnetShedState)shedInactive.getRange().get(tag);
   }

   private BBacnetShedState(int ordinal) {
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
      return id > 63 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 3 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 3;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
