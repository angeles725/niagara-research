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
   range = {@Range("locked"), @Range("unlocked"), @Range("lockFault"), @Range("unused"), @Range("unknown")}
)
public final class BBacnetLockStatus extends BFrozenEnum implements BacnetConst {
   public static final int LOCKED = 0;
   public static final int UNLOCKED = 1;
   public static final int LOCK_FAULT = 2;
   public static final int UNUSED = 3;
   public static final int UNKNOWN = 4;
   public static final BBacnetLockStatus locked = new BBacnetLockStatus(0);
   public static final BBacnetLockStatus unlocked = new BBacnetLockStatus(1);
   public static final BBacnetLockStatus lockFault = new BBacnetLockStatus(2);
   public static final BBacnetLockStatus unused = new BBacnetLockStatus(3);
   public static final BBacnetLockStatus unknown = new BBacnetLockStatus(4);
   public static final BBacnetLockStatus DEFAULT = locked;
   public static final Type TYPE = Sys.loadType(BBacnetLockStatus.class);
   public static final int MAX_ASHRAE_ID = 4;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetLockStatus make(int ordinal) {
      return (BBacnetLockStatus)locked.getRange().get(ordinal, false);
   }

   public static BBacnetLockStatus make(String tag) {
      return (BBacnetLockStatus)locked.getRange().get(tag);
   }

   private BBacnetLockStatus(int ordinal) {
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
      return id > 4 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 4;
   }
}
