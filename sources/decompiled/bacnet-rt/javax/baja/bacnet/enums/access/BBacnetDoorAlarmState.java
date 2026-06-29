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
   range = {@Range("normal"), @Range("alarm"), @Range("doorOpenForTooLong"), @Range("forcedOpen"), @Range("tamper"), @Range("doorFault"), @Range("lockDown"), @Range("freeAccess"), @Range("egressOpen")}
)
public final class BBacnetDoorAlarmState extends BFrozenEnum implements BacnetConst {
   public static final int NORMAL = 0;
   public static final int ALARM = 1;
   public static final int DOOR_OPEN_FOR_TOO_LONG = 2;
   public static final int FORCED_OPEN = 3;
   public static final int TAMPER = 4;
   public static final int DOOR_FAULT = 5;
   public static final int LOCK_DOWN = 6;
   public static final int FREE_ACCESS = 7;
   public static final int EGRESS_OPEN = 8;
   public static final BBacnetDoorAlarmState normal = new BBacnetDoorAlarmState(0);
   public static final BBacnetDoorAlarmState alarm = new BBacnetDoorAlarmState(1);
   public static final BBacnetDoorAlarmState doorOpenForTooLong = new BBacnetDoorAlarmState(2);
   public static final BBacnetDoorAlarmState forcedOpen = new BBacnetDoorAlarmState(3);
   public static final BBacnetDoorAlarmState tamper = new BBacnetDoorAlarmState(4);
   public static final BBacnetDoorAlarmState doorFault = new BBacnetDoorAlarmState(5);
   public static final BBacnetDoorAlarmState lockDown = new BBacnetDoorAlarmState(6);
   public static final BBacnetDoorAlarmState freeAccess = new BBacnetDoorAlarmState(7);
   public static final BBacnetDoorAlarmState egressOpen = new BBacnetDoorAlarmState(8);
   public static final BBacnetDoorAlarmState DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BBacnetDoorAlarmState.class);
   public static final int MAX_ASHRAE_ID = 8;
   public static final int MAX_RESERVED_ID = 255;
   public static final int MAX_ID = 65535;

   public static BBacnetDoorAlarmState make(int ordinal) {
      return (BBacnetDoorAlarmState)normal.getRange().get(ordinal, false);
   }

   public static BBacnetDoorAlarmState make(String tag) {
      return (BBacnetDoorAlarmState)normal.getRange().get(tag);
   }

   private BBacnetDoorAlarmState(int ordinal) {
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
      return id > 8 && id <= 255;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 8;
   }
}
