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
   range = {@Range("lock"), @Range("unlock"), @Range("pulseUnlock"), @Range("extendedPulseUnlock")}
)
public final class BBacnetDoorValue extends BFrozenEnum implements BacnetConst {
   public static final int LOCK = 0;
   public static final int UNLOCK = 1;
   public static final int PULSE_UNLOCK = 2;
   public static final int EXTENDED_PULSE_UNLOCK = 3;
   public static final BBacnetDoorValue lock = new BBacnetDoorValue(0);
   public static final BBacnetDoorValue unlock = new BBacnetDoorValue(1);
   public static final BBacnetDoorValue pulseUnlock = new BBacnetDoorValue(2);
   public static final BBacnetDoorValue extendedPulseUnlock = new BBacnetDoorValue(3);
   public static final BBacnetDoorValue DEFAULT = lock;
   public static final Type TYPE = Sys.loadType(BBacnetDoorValue.class);
   public static final int MAX_ASHRAE_ID = 3;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetDoorValue make(int ordinal) {
      return (BBacnetDoorValue)lock.getRange().get(ordinal, false);
   }

   public static BBacnetDoorValue make(String tag) {
      return (BBacnetDoorValue)lock.getRange().get(tag);
   }

   private BBacnetDoorValue(int ordinal) {
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
}
