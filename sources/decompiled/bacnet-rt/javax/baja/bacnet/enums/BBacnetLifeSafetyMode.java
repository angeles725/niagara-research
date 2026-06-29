package javax.baja.bacnet.enums;

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
   range = {@Range("off"), @Range("on"), @Range("test"), @Range("manned"), @Range("unmanned"), @Range("armed"), @Range("disarmed"), @Range("prearmed"), @Range("slow"), @Range("fast"), @Range("disconnected"), @Range("enabled"), @Range("disabled"), @Range("automaticReleaseDisabled"), @Range("defaultMode")}
)
public final class BBacnetLifeSafetyMode extends BFrozenEnum implements BacnetConst {
   public static final int OFF = 0;
   public static final int ON = 1;
   public static final int TEST = 2;
   public static final int MANNED = 3;
   public static final int UNMANNED = 4;
   public static final int ARMED = 5;
   public static final int DISARMED = 6;
   public static final int PREARMED = 7;
   public static final int SLOW = 8;
   public static final int FAST = 9;
   public static final int DISCONNECTED = 10;
   public static final int ENABLED = 11;
   public static final int DISABLED = 12;
   public static final int AUTOMATIC_RELEASE_DISABLED = 13;
   public static final int DEFAULT_MODE = 14;
   public static final BBacnetLifeSafetyMode off = new BBacnetLifeSafetyMode(0);
   public static final BBacnetLifeSafetyMode on = new BBacnetLifeSafetyMode(1);
   public static final BBacnetLifeSafetyMode test = new BBacnetLifeSafetyMode(2);
   public static final BBacnetLifeSafetyMode manned = new BBacnetLifeSafetyMode(3);
   public static final BBacnetLifeSafetyMode unmanned = new BBacnetLifeSafetyMode(4);
   public static final BBacnetLifeSafetyMode armed = new BBacnetLifeSafetyMode(5);
   public static final BBacnetLifeSafetyMode disarmed = new BBacnetLifeSafetyMode(6);
   public static final BBacnetLifeSafetyMode prearmed = new BBacnetLifeSafetyMode(7);
   public static final BBacnetLifeSafetyMode slow = new BBacnetLifeSafetyMode(8);
   public static final BBacnetLifeSafetyMode fast = new BBacnetLifeSafetyMode(9);
   public static final BBacnetLifeSafetyMode disconnected = new BBacnetLifeSafetyMode(10);
   public static final BBacnetLifeSafetyMode enabled = new BBacnetLifeSafetyMode(11);
   public static final BBacnetLifeSafetyMode disabled = new BBacnetLifeSafetyMode(12);
   public static final BBacnetLifeSafetyMode automaticReleaseDisabled = new BBacnetLifeSafetyMode(13);
   public static final BBacnetLifeSafetyMode defaultMode = new BBacnetLifeSafetyMode(14);
   public static final BBacnetLifeSafetyMode DEFAULT = off;
   public static final Type TYPE = Sys.loadType(BBacnetLifeSafetyMode.class);
   public static final int MAX_ASHRAE_ID = 14;
   public static final int MAX_RESERVED_ID = 255;
   public static final int MAX_ID = 65535;

   public static BBacnetLifeSafetyMode make(int ordinal) {
      return (BBacnetLifeSafetyMode)off.getRange().get(ordinal, false);
   }

   public static BBacnetLifeSafetyMode make(String tag) {
      return (BBacnetLifeSafetyMode)off.getRange().get(tag);
   }

   private BBacnetLifeSafetyMode(int ordinal) {
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
      return id > 255 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 14 && id <= 255;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 14;
   }
}
