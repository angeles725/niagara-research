package javax.baja.bacnet.enums.access;

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
   range = {@Range("closed"), @Range("open"), @Range("unknown"), @Range("doorFault"), @Range("unused")}
)
public final class BBacnetDoorStatus extends BFrozenEnum implements BacnetConst {
   public static final int CLOSED = 0;
   public static final int OPEN = 1;
   public static final int UNKNOWN = 2;
   public static final int DOOR_FAULT = 3;
   public static final int UNUSED = 4;
   public static final BBacnetDoorStatus closed = new BBacnetDoorStatus(0);
   public static final BBacnetDoorStatus open = new BBacnetDoorStatus(1);
   public static final BBacnetDoorStatus unknown = new BBacnetDoorStatus(2);
   public static final BBacnetDoorStatus doorFault = new BBacnetDoorStatus(3);
   public static final BBacnetDoorStatus unused = new BBacnetDoorStatus(4);
   public static final BBacnetDoorStatus DEFAULT = closed;
   public static final Type TYPE = Sys.loadType(BBacnetDoorStatus.class);
   public static final int MAX_ASHRAE_ID = 4;
   public static final int MAX_RESERVED_ID = 1023;
   public static final int MAX_ID = 65535;

   public static BBacnetDoorStatus make(int ordinal) {
      return (BBacnetDoorStatus)closed.getRange().get(ordinal, false);
   }

   public static BBacnetDoorStatus make(String tag) {
      return (BBacnetDoorStatus)closed.getRange().get(tag);
   }

   private BBacnetDoorStatus(int ordinal) {
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
      return id > 1023 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 4 && id <= 1023;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 4;
   }
}
