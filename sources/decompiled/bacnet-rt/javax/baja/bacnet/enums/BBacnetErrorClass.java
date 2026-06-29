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
   range = {@Range("device"), @Range("object"), @Range("property"), @Range("resources"), @Range("security"), @Range("services"), @Range("vt"), @Range("communication")}
)
public final class BBacnetErrorClass extends BFrozenEnum implements BacnetConst {
   public static final int DEVICE = 0;
   public static final int OBJECT = 1;
   public static final int PROPERTY = 2;
   public static final int RESOURCES = 3;
   public static final int SECURITY = 4;
   public static final int SERVICES = 5;
   public static final int VT = 6;
   public static final int COMMUNICATION = 7;
   public static final BBacnetErrorClass device = new BBacnetErrorClass(0);
   public static final BBacnetErrorClass object = new BBacnetErrorClass(1);
   public static final BBacnetErrorClass property = new BBacnetErrorClass(2);
   public static final BBacnetErrorClass resources = new BBacnetErrorClass(3);
   public static final BBacnetErrorClass security = new BBacnetErrorClass(4);
   public static final BBacnetErrorClass services = new BBacnetErrorClass(5);
   public static final BBacnetErrorClass vt = new BBacnetErrorClass(6);
   public static final BBacnetErrorClass communication = new BBacnetErrorClass(7);
   public static final BBacnetErrorClass DEFAULT = device;
   public static final Type TYPE = Sys.loadType(BBacnetErrorClass.class);
   public static final int MAX_ASHRAE_ID = 7;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetErrorClass make(int ordinal) {
      return (BBacnetErrorClass)device.getRange().get(ordinal, false);
   }

   public static BBacnetErrorClass make(String tag) {
      return (BBacnetErrorClass)device.getRange().get(tag);
   }

   private BBacnetErrorClass(int ordinal) {
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
      return id > 7 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 7;
   }
}
