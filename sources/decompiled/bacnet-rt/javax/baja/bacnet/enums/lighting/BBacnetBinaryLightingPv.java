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
   range = {@Range("off"), @Range("on"), @Range("warn"), @Range("warnOff"), @Range("warnRelinquish"), @Range("stop")}
)
public final class BBacnetBinaryLightingPv extends BFrozenEnum {
   public static final int OFF = 0;
   public static final int ON = 1;
   public static final int WARN = 2;
   public static final int WARN_OFF = 3;
   public static final int WARN_RELINQUISH = 4;
   public static final int STOP = 5;
   public static final BBacnetBinaryLightingPv off = new BBacnetBinaryLightingPv(0);
   public static final BBacnetBinaryLightingPv on = new BBacnetBinaryLightingPv(1);
   public static final BBacnetBinaryLightingPv warn = new BBacnetBinaryLightingPv(2);
   public static final BBacnetBinaryLightingPv warnOff = new BBacnetBinaryLightingPv(3);
   public static final BBacnetBinaryLightingPv warnRelinquish = new BBacnetBinaryLightingPv(4);
   public static final BBacnetBinaryLightingPv stop = new BBacnetBinaryLightingPv(5);
   public static final BBacnetBinaryLightingPv DEFAULT = off;
   public static final Type TYPE = Sys.loadType(BBacnetBinaryLightingPv.class);
   public static final int MAX_ASHRAE_ID = 5;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 255;

   public static BBacnetBinaryLightingPv make(int ordinal) {
      return (BBacnetBinaryLightingPv)off.getRange().get(ordinal, false);
   }

   public static BBacnetBinaryLightingPv make(String tag) {
      return (BBacnetBinaryLightingPv)off.getRange().get(tag);
   }

   private BBacnetBinaryLightingPv(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      if (DEFAULT.getRange().isOrdinal(id)) {
         return DEFAULT.getRange().getTag(id);
      } else if (isAshrae(id)) {
         return BacnetConst.ASHRAE_PREFIX + id;
      } else if (isProprietary(id)) {
         return BacnetConst.PROPRIETARY_PREFIX + id;
      } else {
         throw new InvalidEnumException(id);
      }
   }

   public static int ordinal(String tag) {
      try {
         return DEFAULT.getRange().tagToOrdinal(tag);
      } catch (InvalidEnumException var2) {
         if (tag.startsWith(BacnetConst.ASHRAE_PREFIX)) {
            return Integer.parseInt(tag.substring(BacnetConst.ASHRAE_PREFIX_LENGTH));
         } else if (tag.startsWith(BacnetConst.PROPRIETARY_PREFIX)) {
            return Integer.parseInt(tag.substring(BacnetConst.PROPRIETARY_PREFIX_LENGTH));
         } else {
            throw var2;
         }
      }
   }

   public static boolean isProprietary(int id) {
      return id > 63 && id <= 255;
   }

   public static boolean isAshrae(int id) {
      return id > 5 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 255;
   }

   public static boolean isFixed(int id) {
      return id <= 5;
   }
}
