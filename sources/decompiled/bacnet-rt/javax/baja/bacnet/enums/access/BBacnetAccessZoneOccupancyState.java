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
      value = "normal",
      ordinal = 0
   ), @Range(
      value = "belowLowerLimit",
      ordinal = 1
   ), @Range(
      value = "atLowerLimit",
      ordinal = 2
   ), @Range(
      value = "atUpperLimit",
      ordinal = 3
   ), @Range(
      value = "aboveUpperLimit",
      ordinal = 4
   ), @Range(
      value = "disabled",
      ordinal = 5
   ), @Range(
      value = "notSupported",
      ordinal = 6
   )}
)
public final class BBacnetAccessZoneOccupancyState extends BFrozenEnum implements BacnetConst {
   public static final int NORMAL = 0;
   public static final int BELOW_LOWER_LIMIT = 1;
   public static final int AT_LOWER_LIMIT = 2;
   public static final int AT_UPPER_LIMIT = 3;
   public static final int ABOVE_UPPER_LIMIT = 4;
   public static final int DISABLED = 5;
   public static final int NOT_SUPPORTED = 6;
   public static final BBacnetAccessZoneOccupancyState normal = new BBacnetAccessZoneOccupancyState(0);
   public static final BBacnetAccessZoneOccupancyState belowLowerLimit = new BBacnetAccessZoneOccupancyState(1);
   public static final BBacnetAccessZoneOccupancyState atLowerLimit = new BBacnetAccessZoneOccupancyState(2);
   public static final BBacnetAccessZoneOccupancyState atUpperLimit = new BBacnetAccessZoneOccupancyState(3);
   public static final BBacnetAccessZoneOccupancyState aboveUpperLimit = new BBacnetAccessZoneOccupancyState(4);
   public static final BBacnetAccessZoneOccupancyState disabled = new BBacnetAccessZoneOccupancyState(5);
   public static final BBacnetAccessZoneOccupancyState notSupported = new BBacnetAccessZoneOccupancyState(6);
   public static final BBacnetAccessZoneOccupancyState DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BBacnetAccessZoneOccupancyState.class);
   public static final int MAX_ASHRAE_ID = 6;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAccessZoneOccupancyState make(int ordinal) {
      return (BBacnetAccessZoneOccupancyState)normal.getRange().get(ordinal, false);
   }

   public static BBacnetAccessZoneOccupancyState make(String tag) {
      return (BBacnetAccessZoneOccupancyState)normal.getRange().get(tag);
   }

   private BBacnetAccessZoneOccupancyState(int ordinal) {
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
