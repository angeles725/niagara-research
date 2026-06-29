package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("unknown"), @Range("coldstart"), @Range("warmstart"), @Range("detectedPowerLost"), @Range("detectedPowerOff"), @Range("hardwareWatchdog"), @Range("softwareWatchdog"), @Range("suspended")}
)
public final class BBacnetRestartReason extends BFrozenEnum implements BacnetConst {
   public static final int UNKNOWN = 0;
   public static final int COLDSTART = 1;
   public static final int WARMSTART = 2;
   public static final int DETECTED_POWER_LOST = 3;
   public static final int DETECTED_POWER_OFF = 4;
   public static final int HARDWARE_WATCHDOG = 5;
   public static final int SOFTWARE_WATCHDOG = 6;
   public static final int SUSPENDED = 7;
   public static final BBacnetRestartReason unknown = new BBacnetRestartReason(0);
   public static final BBacnetRestartReason coldstart = new BBacnetRestartReason(1);
   public static final BBacnetRestartReason warmstart = new BBacnetRestartReason(2);
   public static final BBacnetRestartReason detectedPowerLost = new BBacnetRestartReason(3);
   public static final BBacnetRestartReason detectedPowerOff = new BBacnetRestartReason(4);
   public static final BBacnetRestartReason hardwareWatchdog = new BBacnetRestartReason(5);
   public static final BBacnetRestartReason softwareWatchdog = new BBacnetRestartReason(6);
   public static final BBacnetRestartReason suspended = new BBacnetRestartReason(7);
   public static final BBacnetRestartReason DEFAULT = unknown;
   public static final Type TYPE = Sys.loadType(BBacnetRestartReason.class);
   public static final int MAX_ASHRAE_ID = 7;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 255;

   public static BBacnetRestartReason make(int ordinal) {
      return (BBacnetRestartReason)unknown.getRange().get(ordinal, false);
   }

   public static BBacnetRestartReason make(String tag) {
      return (BBacnetRestartReason)unknown.getRange().get(tag);
   }

   private BBacnetRestartReason(int ordinal) {
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
      return id > 63 && id <= 255;
   }

   public static boolean isAshrae(int id) {
      return id > 7 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 255;
   }

   public static boolean isFixed(int id) {
      return id <= 7;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
