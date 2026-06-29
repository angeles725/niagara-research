package javax.baja.bacnet.enums.lighting;

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
   range = {@Range("none"), @Range("fadeTo"), @Range("rampTo"), @Range("stepUp"), @Range("stepDown"), @Range("stepOn"), @Range("stepOff"), @Range("warn"), @Range("warnOff"), @Range("warnRelinquish"), @Range("stop")}
)
public final class BBacnetLightingOperation extends BFrozenEnum implements BacnetConst {
   public static final int NONE = 0;
   public static final int FADE_TO = 1;
   public static final int RAMP_TO = 2;
   public static final int STEP_UP = 3;
   public static final int STEP_DOWN = 4;
   public static final int STEP_ON = 5;
   public static final int STEP_OFF = 6;
   public static final int WARN = 7;
   public static final int WARN_OFF = 8;
   public static final int WARN_RELINQUISH = 9;
   public static final int STOP = 10;
   public static final BBacnetLightingOperation none = new BBacnetLightingOperation(0);
   public static final BBacnetLightingOperation fadeTo = new BBacnetLightingOperation(1);
   public static final BBacnetLightingOperation rampTo = new BBacnetLightingOperation(2);
   public static final BBacnetLightingOperation stepUp = new BBacnetLightingOperation(3);
   public static final BBacnetLightingOperation stepDown = new BBacnetLightingOperation(4);
   public static final BBacnetLightingOperation stepOn = new BBacnetLightingOperation(5);
   public static final BBacnetLightingOperation stepOff = new BBacnetLightingOperation(6);
   public static final BBacnetLightingOperation warn = new BBacnetLightingOperation(7);
   public static final BBacnetLightingOperation warnOff = new BBacnetLightingOperation(8);
   public static final BBacnetLightingOperation warnRelinquish = new BBacnetLightingOperation(9);
   public static final BBacnetLightingOperation stop = new BBacnetLightingOperation(10);
   public static final BBacnetLightingOperation DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BBacnetLightingOperation.class);
   public static final int MAX_ASHRAE_ID = 10;
   public static final int MAX_RESERVED_ID = 255;
   public static final int MAX_ID = 65535;

   public static BBacnetLightingOperation make(int ordinal) {
      return (BBacnetLightingOperation)none.getRange().get(ordinal, false);
   }

   public static BBacnetLightingOperation make(String tag) {
      return (BBacnetLightingOperation)none.getRange().get(tag);
   }

   private BBacnetLightingOperation(int ordinal) {
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
      return id > 10 && id <= 255;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 10;
   }
}
