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
   range = {@Range("incapable"), @Range("plain"), @Range("signed"), @Range("encrypted"), @Range("signedEndToEnd"), @Range("encryptedEndToEnd")}
)
public final class BBacnetSecurityLevel extends BFrozenEnum implements BacnetConst {
   public static final int INCAPABLE = 0;
   public static final int PLAIN = 1;
   public static final int SIGNED = 2;
   public static final int ENCRYPTED = 3;
   public static final int SIGNED_END_TO_END = 4;
   public static final int ENCRYPTED_END_TO_END = 5;
   public static final BBacnetSecurityLevel incapable = new BBacnetSecurityLevel(0);
   public static final BBacnetSecurityLevel plain = new BBacnetSecurityLevel(1);
   public static final BBacnetSecurityLevel signed = new BBacnetSecurityLevel(2);
   public static final BBacnetSecurityLevel encrypted = new BBacnetSecurityLevel(3);
   public static final BBacnetSecurityLevel signedEndToEnd = new BBacnetSecurityLevel(4);
   public static final BBacnetSecurityLevel encryptedEndToEnd = new BBacnetSecurityLevel(5);
   public static final BBacnetSecurityLevel DEFAULT = incapable;
   public static final Type TYPE = Sys.loadType(BBacnetSecurityLevel.class);
   public static final int MAX_ASHRAE_ID = 5;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetSecurityLevel make(int ordinal) {
      return (BBacnetSecurityLevel)incapable.getRange().get(ordinal, false);
   }

   public static BBacnetSecurityLevel make(String tag) {
      return (BBacnetSecurityLevel)incapable.getRange().get(tag);
   }

   private BBacnetSecurityLevel(int ordinal) {
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
