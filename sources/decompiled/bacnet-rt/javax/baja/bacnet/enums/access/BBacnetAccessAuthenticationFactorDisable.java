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
   range = {@Range("none"), @Range("disabled"), @Range("disabledLost"), @Range("disabledStolen"), @Range("disabledDamaged"), @Range("disabledDestroyed")}
)
public final class BBacnetAccessAuthenticationFactorDisable extends BFrozenEnum implements BacnetConst {
   public static final int NONE = 0;
   public static final int DISABLED = 1;
   public static final int DISABLED_LOST = 2;
   public static final int DISABLED_STOLEN = 3;
   public static final int DISABLED_DAMAGED = 4;
   public static final int DISABLED_DESTROYED = 5;
   public static final BBacnetAccessAuthenticationFactorDisable none = new BBacnetAccessAuthenticationFactorDisable(0);
   public static final BBacnetAccessAuthenticationFactorDisable disabled = new BBacnetAccessAuthenticationFactorDisable(1);
   public static final BBacnetAccessAuthenticationFactorDisable disabledLost = new BBacnetAccessAuthenticationFactorDisable(2);
   public static final BBacnetAccessAuthenticationFactorDisable disabledStolen = new BBacnetAccessAuthenticationFactorDisable(3);
   public static final BBacnetAccessAuthenticationFactorDisable disabledDamaged = new BBacnetAccessAuthenticationFactorDisable(4);
   public static final BBacnetAccessAuthenticationFactorDisable disabledDestroyed = new BBacnetAccessAuthenticationFactorDisable(5);
   public static final BBacnetAccessAuthenticationFactorDisable DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BBacnetAccessAuthenticationFactorDisable.class);
   public static final int MAX_ASHRAE_ID = 5;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAccessAuthenticationFactorDisable make(int ordinal) {
      return (BBacnetAccessAuthenticationFactorDisable)none.getRange().get(ordinal, false);
   }

   public static BBacnetAccessAuthenticationFactorDisable make(String tag) {
      return (BBacnetAccessAuthenticationFactorDisable)none.getRange().get(tag);
   }

   private BBacnetAccessAuthenticationFactorDisable(int ordinal) {
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
