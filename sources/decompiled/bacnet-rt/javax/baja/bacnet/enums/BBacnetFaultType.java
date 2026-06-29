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
   range = {@Range("none"), @Range("faultCharacterstring"), @Range("faultExtended"), @Range("faultLifeSafety"), @Range("faultState"), @Range("faultStatusFlags"), @Range("faultOutOfRange")}
)
public final class BBacnetFaultType extends BFrozenEnum implements BacnetConst {
   public static final int NONE = 0;
   public static final int FAULT_CHARACTERSTRING = 1;
   public static final int FAULT_EXTENDED = 2;
   public static final int FAULT_LIFE_SAFETY = 3;
   public static final int FAULT_STATE = 4;
   public static final int FAULT_STATUS_FLAGS = 5;
   public static final int FAULT_OUT_OF_RANGE = 6;
   public static final BBacnetFaultType none = new BBacnetFaultType(0);
   public static final BBacnetFaultType faultCharacterstring = new BBacnetFaultType(1);
   public static final BBacnetFaultType faultExtended = new BBacnetFaultType(2);
   public static final BBacnetFaultType faultLifeSafety = new BBacnetFaultType(3);
   public static final BBacnetFaultType faultState = new BBacnetFaultType(4);
   public static final BBacnetFaultType faultStatusFlags = new BBacnetFaultType(5);
   public static final BBacnetFaultType faultOutOfRange = new BBacnetFaultType(6);
   public static final BBacnetFaultType DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BBacnetFaultType.class);
   public static final int MAX_ASHRAE_ID = 5;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetFaultType make(int ordinal) {
      return (BBacnetFaultType)none.getRange().get(ordinal, false);
   }

   public static BBacnetFaultType make(String tag) {
      return (BBacnetFaultType)none.getRange().get(tag);
   }

   private BBacnetFaultType(int ordinal) {
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
      return id > 5 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 5;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
