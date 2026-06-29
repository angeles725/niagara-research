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
   range = {@Range("secured"), @Range("unsecured"), @Range("unknown")}
)
public final class BBacnetDoorSecuredStatus extends BFrozenEnum implements BacnetConst {
   public static final int SECURED = 0;
   public static final int UNSECURED = 1;
   public static final int UNKNOWN = 2;
   public static final BBacnetDoorSecuredStatus secured = new BBacnetDoorSecuredStatus(0);
   public static final BBacnetDoorSecuredStatus unsecured = new BBacnetDoorSecuredStatus(1);
   public static final BBacnetDoorSecuredStatus unknown = new BBacnetDoorSecuredStatus(2);
   public static final BBacnetDoorSecuredStatus DEFAULT = secured;
   public static final Type TYPE = Sys.loadType(BBacnetDoorSecuredStatus.class);
   public static final int MAX_ASHRAE_ID = 2;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetDoorSecuredStatus make(int ordinal) {
      return (BBacnetDoorSecuredStatus)secured.getRange().get(ordinal, false);
   }

   public static BBacnetDoorSecuredStatus make(String tag) {
      return (BBacnetDoorSecuredStatus)secured.getRange().get(tag);
   }

   private BBacnetDoorSecuredStatus(int ordinal) {
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
      return id > 2 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 2;
   }
}
