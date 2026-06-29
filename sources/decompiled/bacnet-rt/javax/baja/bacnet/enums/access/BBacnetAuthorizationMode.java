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
   range = {@Range(
      value = "authorize",
      ordinal = 0
   ), @Range(
      value = "grantActive",
      ordinal = 1
   ), @Range(
      value = "denyAll",
      ordinal = 2
   ), @Range(
      value = "verificationRequired",
      ordinal = 3
   ), @Range(
      value = "authorizationDelayed",
      ordinal = 4
   ), @Range(
      value = "none",
      ordinal = 5
   )},
   defaultValue = "denyAll"
)
public final class BBacnetAuthorizationMode extends BFrozenEnum implements BacnetConst {
   public static final int AUTHORIZE = 0;
   public static final int GRANT_ACTIVE = 1;
   public static final int DENY_ALL = 2;
   public static final int VERIFICATION_REQUIRED = 3;
   public static final int AUTHORIZATION_DELAYED = 4;
   public static final int NONE = 5;
   public static final BBacnetAuthorizationMode authorize = new BBacnetAuthorizationMode(0);
   public static final BBacnetAuthorizationMode grantActive = new BBacnetAuthorizationMode(1);
   public static final BBacnetAuthorizationMode denyAll = new BBacnetAuthorizationMode(2);
   public static final BBacnetAuthorizationMode verificationRequired = new BBacnetAuthorizationMode(3);
   public static final BBacnetAuthorizationMode authorizationDelayed = new BBacnetAuthorizationMode(4);
   public static final BBacnetAuthorizationMode none = new BBacnetAuthorizationMode(5);
   public static final BBacnetAuthorizationMode DEFAULT = denyAll;
   public static final Type TYPE = Sys.loadType(BBacnetAuthorizationMode.class);
   public static final int MAX_ASHRAE_ID = 5;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAuthorizationMode make(int ordinal) {
      return (BBacnetAuthorizationMode)authorize.getRange().get(ordinal, false);
   }

   public static BBacnetAuthorizationMode make(String tag) {
      return (BBacnetAuthorizationMode)authorize.getRange().get(tag);
   }

   private BBacnetAuthorizationMode(int ordinal) {
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
}
