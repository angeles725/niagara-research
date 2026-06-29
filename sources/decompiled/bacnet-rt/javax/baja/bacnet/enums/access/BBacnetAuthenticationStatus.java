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
      value = "notReady",
      ordinal = 0
   ), @Range(
      value = "ready",
      ordinal = 1
   ), @Range(
      value = "disabled",
      ordinal = 2
   ), @Range(
      value = "waitingForAuthenticationFactor",
      ordinal = 3
   ), @Range(
      value = "waitingForAccompaniment",
      ordinal = 4
   ), @Range(
      value = "waitingForVerification",
      ordinal = 5
   ), @Range(
      value = "inProgress",
      ordinal = 6
   )}
)
public final class BBacnetAuthenticationStatus extends BFrozenEnum implements BacnetConst {
   public static final int NOT_READY = 0;
   public static final int READY = 1;
   public static final int DISABLED = 2;
   public static final int WAITING_FOR_AUTHENTICATION_FACTOR = 3;
   public static final int WAITING_FOR_ACCOMPANIMENT = 4;
   public static final int WAITING_FOR_VERIFICATION = 5;
   public static final int IN_PROGRESS = 6;
   public static final BBacnetAuthenticationStatus notReady = new BBacnetAuthenticationStatus(0);
   public static final BBacnetAuthenticationStatus ready = new BBacnetAuthenticationStatus(1);
   public static final BBacnetAuthenticationStatus disabled = new BBacnetAuthenticationStatus(2);
   public static final BBacnetAuthenticationStatus waitingForAuthenticationFactor = new BBacnetAuthenticationStatus(3);
   public static final BBacnetAuthenticationStatus waitingForAccompaniment = new BBacnetAuthenticationStatus(4);
   public static final BBacnetAuthenticationStatus waitingForVerification = new BBacnetAuthenticationStatus(5);
   public static final BBacnetAuthenticationStatus inProgress = new BBacnetAuthenticationStatus(6);
   public static final BBacnetAuthenticationStatus DEFAULT = notReady;
   public static final Type TYPE = Sys.loadType(BBacnetAuthenticationStatus.class);
   public static final int MAX_ASHRAE_ID = 6;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAuthenticationStatus make(int ordinal) {
      return (BBacnetAuthenticationStatus)notReady.getRange().get(ordinal, false);
   }

   public static BBacnetAuthenticationStatus make(String tag) {
      return (BBacnetAuthenticationStatus)notReady.getRange().get(tag);
   }

   private BBacnetAuthenticationStatus(int ordinal) {
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
