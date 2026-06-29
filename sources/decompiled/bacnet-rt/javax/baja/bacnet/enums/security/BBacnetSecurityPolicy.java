package javax.baja.bacnet.enums.security;

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
   range = {@Range("plainNonTrusted"), @Range("plainTrusted"), @Range("signedTrusted"), @Range("encryptedTrusted")}
)
public final class BBacnetSecurityPolicy extends BFrozenEnum implements BacnetConst {
   public static final int PLAIN_NON_TRUSTED = 0;
   public static final int PLAIN_TRUSTED = 1;
   public static final int SIGNED_TRUSTED = 2;
   public static final int ENCRYPTED_TRUSTED = 3;
   public static final BBacnetSecurityPolicy plainNonTrusted = new BBacnetSecurityPolicy(0);
   public static final BBacnetSecurityPolicy plainTrusted = new BBacnetSecurityPolicy(1);
   public static final BBacnetSecurityPolicy signedTrusted = new BBacnetSecurityPolicy(2);
   public static final BBacnetSecurityPolicy encryptedTrusted = new BBacnetSecurityPolicy(3);
   public static final BBacnetSecurityPolicy DEFAULT = plainNonTrusted;
   public static final Type TYPE = Sys.loadType(BBacnetSecurityPolicy.class);
   public static final int MAX_ASHRAE_ID = 3;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetSecurityPolicy make(int ordinal) {
      return (BBacnetSecurityPolicy)plainNonTrusted.getRange().get(ordinal, false);
   }

   public static BBacnetSecurityPolicy make(String tag) {
      return (BBacnetSecurityPolicy)plainNonTrusted.getRange().get(tag);
   }

   private BBacnetSecurityPolicy(int ordinal) {
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
