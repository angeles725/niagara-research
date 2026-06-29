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
   range = {@Range("none"), @Range("disabled"), @Range("disableManual"), @Range("disableLockout")}
)
public final class BBacnetAccessCredentialDisable extends BFrozenEnum implements BacnetConst {
   public static final int NONE = 0;
   public static final int DISABLED = 1;
   public static final int DISABLE_MANUAL = 2;
   public static final int DISABLE_LOCKOUT = 3;
   public static final BBacnetAccessCredentialDisable none = new BBacnetAccessCredentialDisable(0);
   public static final BBacnetAccessCredentialDisable disabled = new BBacnetAccessCredentialDisable(1);
   public static final BBacnetAccessCredentialDisable disableManual = new BBacnetAccessCredentialDisable(2);
   public static final BBacnetAccessCredentialDisable disableLockout = new BBacnetAccessCredentialDisable(3);
   public static final BBacnetAccessCredentialDisable DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BBacnetAccessCredentialDisable.class);
   public static final int MAX_ASHRAE_ID = 3;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAccessCredentialDisable make(int ordinal) {
      return (BBacnetAccessCredentialDisable)none.getRange().get(ordinal, false);
   }

   public static BBacnetAccessCredentialDisable make(String tag) {
      return (BBacnetAccessCredentialDisable)none.getRange().get(tag);
   }

   private BBacnetAccessCredentialDisable(int ordinal) {
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
