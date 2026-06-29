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
   range = {@Range("idle"), @Range("fadeActive"), @Range("rampActive"), @Range("notControlled"), @Range("other")}
)
public final class BBacnetLightingInProgress extends BFrozenEnum implements BacnetConst {
   public static final int IDLE = 0;
   public static final int FADE_ACTIVE = 1;
   public static final int RAMP_ACTIVE = 2;
   public static final int NOT_CONTROLLED = 3;
   public static final int OTHER = 4;
   public static final BBacnetLightingInProgress idle = new BBacnetLightingInProgress(0);
   public static final BBacnetLightingInProgress fadeActive = new BBacnetLightingInProgress(1);
   public static final BBacnetLightingInProgress rampActive = new BBacnetLightingInProgress(2);
   public static final BBacnetLightingInProgress notControlled = new BBacnetLightingInProgress(3);
   public static final BBacnetLightingInProgress other = new BBacnetLightingInProgress(4);
   public static final BBacnetLightingInProgress DEFAULT = idle;
   public static final Type TYPE = Sys.loadType(BBacnetLightingInProgress.class);
   public static final int MAX_ASHRAE_ID = 4;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetLightingInProgress make(int ordinal) {
      return (BBacnetLightingInProgress)idle.getRange().get(ordinal, false);
   }

   public static BBacnetLightingInProgress make(String tag) {
      return (BBacnetLightingInProgress)idle.getRange().get(tag);
   }

   private BBacnetLightingInProgress(int ordinal) {
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
      return id > 4 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 4;
   }
}
