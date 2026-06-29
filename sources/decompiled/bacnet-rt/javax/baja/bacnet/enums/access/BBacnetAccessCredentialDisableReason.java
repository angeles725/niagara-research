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
   range = {@Range("disabled"), @Range("disabledNeedsProvisioning"), @Range("disabledUnassigned"), @Range("disabledNotYetActive"), @Range("disabledExpired"), @Range("disabledLockout"), @Range("disabledMaxDays"), @Range("disabledMaxUses"), @Range("disabledInactivity"), @Range("disabledManual")}
)
public final class BBacnetAccessCredentialDisableReason extends BFrozenEnum implements BacnetConst {
   public static final int DISABLED = 0;
   public static final int DISABLED_NEEDS_PROVISIONING = 1;
   public static final int DISABLED_UNASSIGNED = 2;
   public static final int DISABLED_NOT_YET_ACTIVE = 3;
   public static final int DISABLED_EXPIRED = 4;
   public static final int DISABLED_LOCKOUT = 5;
   public static final int DISABLED_MAX_DAYS = 6;
   public static final int DISABLED_MAX_USES = 7;
   public static final int DISABLED_INACTIVITY = 8;
   public static final int DISABLED_MANUAL = 9;
   public static final BBacnetAccessCredentialDisableReason disabled = new BBacnetAccessCredentialDisableReason(0);
   public static final BBacnetAccessCredentialDisableReason disabledNeedsProvisioning = new BBacnetAccessCredentialDisableReason(1);
   public static final BBacnetAccessCredentialDisableReason disabledUnassigned = new BBacnetAccessCredentialDisableReason(2);
   public static final BBacnetAccessCredentialDisableReason disabledNotYetActive = new BBacnetAccessCredentialDisableReason(3);
   public static final BBacnetAccessCredentialDisableReason disabledExpired = new BBacnetAccessCredentialDisableReason(4);
   public static final BBacnetAccessCredentialDisableReason disabledLockout = new BBacnetAccessCredentialDisableReason(5);
   public static final BBacnetAccessCredentialDisableReason disabledMaxDays = new BBacnetAccessCredentialDisableReason(6);
   public static final BBacnetAccessCredentialDisableReason disabledMaxUses = new BBacnetAccessCredentialDisableReason(7);
   public static final BBacnetAccessCredentialDisableReason disabledInactivity = new BBacnetAccessCredentialDisableReason(8);
   public static final BBacnetAccessCredentialDisableReason disabledManual = new BBacnetAccessCredentialDisableReason(9);
   public static final BBacnetAccessCredentialDisableReason DEFAULT = disabled;
   public static final Type TYPE = Sys.loadType(BBacnetAccessCredentialDisableReason.class);
   public static final int MAX_ASHRAE_ID = 9;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAccessCredentialDisableReason make(int ordinal) {
      return (BBacnetAccessCredentialDisableReason)disabled.getRange().get(ordinal, false);
   }

   public static BBacnetAccessCredentialDisableReason make(String tag) {
      return (BBacnetAccessCredentialDisableReason)disabled.getRange().get(tag);
   }

   private BBacnetAccessCredentialDisableReason(int ordinal) {
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
      return id > 9 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 9;
   }
}
