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
   range = {@Range("passback"), @Range("occupancyCheck"), @Range("accessRights"), @Range("lockout"), @Range("deny"), @Range("verification"), @Range("authorizationDelay")}
)
public final class BBacnetAuthorizationExemption extends BFrozenEnum implements BacnetConst {
   public static final int PASSBACK = 0;
   public static final int OCCUPANCY_CHECK = 1;
   public static final int ACCESS_RIGHTS = 2;
   public static final int LOCKOUT = 3;
   public static final int DENY = 4;
   public static final int VERIFICATION = 5;
   public static final int AUTHORIZATION_DELAY = 6;
   public static final BBacnetAuthorizationExemption passback = new BBacnetAuthorizationExemption(0);
   public static final BBacnetAuthorizationExemption occupancyCheck = new BBacnetAuthorizationExemption(1);
   public static final BBacnetAuthorizationExemption accessRights = new BBacnetAuthorizationExemption(2);
   public static final BBacnetAuthorizationExemption lockout = new BBacnetAuthorizationExemption(3);
   public static final BBacnetAuthorizationExemption deny = new BBacnetAuthorizationExemption(4);
   public static final BBacnetAuthorizationExemption verification = new BBacnetAuthorizationExemption(5);
   public static final BBacnetAuthorizationExemption authorizationDelay = new BBacnetAuthorizationExemption(6);
   public static final BBacnetAuthorizationExemption DEFAULT = passback;
   public static final Type TYPE = Sys.loadType(BBacnetAuthorizationExemption.class);
   public static final int MAX_ASHRAE_ID = 6;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAuthorizationExemption make(int ordinal) {
      return (BBacnetAuthorizationExemption)passback.getRange().get(ordinal, false);
   }

   public static BBacnetAuthorizationExemption make(String tag) {
      return (BBacnetAuthorizationExemption)passback.getRange().get(tag);
   }

   private BBacnetAuthorizationExemption(int ordinal) {
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
      return id > 6 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 6;
   }
}
