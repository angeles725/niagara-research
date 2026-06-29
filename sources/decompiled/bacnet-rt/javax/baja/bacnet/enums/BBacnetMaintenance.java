package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("none"), @Range("periodicTest"), @Range("needServiceOperational"), @Range("needServiceInoperative")}
)
public final class BBacnetMaintenance extends BFrozenEnum implements BacnetConst {
   public static final int NONE = 0;
   public static final int PERIODIC_TEST = 1;
   public static final int NEED_SERVICE_OPERATIONAL = 2;
   public static final int NEED_SERVICE_INOPERATIVE = 3;
   public static final BBacnetMaintenance none = new BBacnetMaintenance(0);
   public static final BBacnetMaintenance periodicTest = new BBacnetMaintenance(1);
   public static final BBacnetMaintenance needServiceOperational = new BBacnetMaintenance(2);
   public static final BBacnetMaintenance needServiceInoperative = new BBacnetMaintenance(3);
   public static final BBacnetMaintenance DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BBacnetMaintenance.class);
   public static final int MAX_ASHRAE_ID = 23;
   public static final int MAX_RESERVED_ID = 255;
   public static final int MAX_ID = 65535;

   public static BBacnetMaintenance make(int ordinal) {
      return (BBacnetMaintenance)none.getRange().get(ordinal, false);
   }

   public static BBacnetMaintenance make(String tag) {
      return (BBacnetMaintenance)none.getRange().get(tag);
   }

   private BBacnetMaintenance(int ordinal) {
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
      return id > 255 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 23 && id <= 255;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 23;
   }
}
