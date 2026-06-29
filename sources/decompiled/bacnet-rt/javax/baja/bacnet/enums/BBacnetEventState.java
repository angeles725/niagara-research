package javax.baja.bacnet.enums;

import javax.baja.alarm.BSourceState;
import javax.baja.alarm.ext.BAlarmState;
import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("normal"), @Range("fault"), @Range("offnormal"), @Range("highLimit"), @Range("lowLimit"), @Range("lifeSafetyAlarm")}
)
public final class BBacnetEventState extends BFrozenEnum implements BacnetConst {
   public static final int NORMAL = 0;
   public static final int FAULT = 1;
   public static final int OFFNORMAL = 2;
   public static final int HIGH_LIMIT = 3;
   public static final int LOW_LIMIT = 4;
   public static final int LIFE_SAFETY_ALARM = 5;
   public static final BBacnetEventState normal = new BBacnetEventState(0);
   public static final BBacnetEventState fault = new BBacnetEventState(1);
   public static final BBacnetEventState offnormal = new BBacnetEventState(2);
   public static final BBacnetEventState highLimit = new BBacnetEventState(3);
   public static final BBacnetEventState lowLimit = new BBacnetEventState(4);
   public static final BBacnetEventState lifeSafetyAlarm = new BBacnetEventState(5);
   public static final BBacnetEventState DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BBacnetEventState.class);
   public static final int MAX_ASHRAE_ID = 5;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetEventState make(int ordinal) {
      return (BBacnetEventState)normal.getRange().get(ordinal, false);
   }

   public static BBacnetEventState make(String tag) {
      return (BBacnetEventState)normal.getRange().get(tag);
   }

   private BBacnetEventState(int ordinal) {
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

   public static BBacnetEventState make(BAlarmState alarmState) {
      return make(alarmState.getOrdinal());
   }

   public static BBacnetEventState make(BSourceState sourceState) {
      switch (sourceState.getOrdinal()) {
         case 0:
            return normal;
         case 1:
            return offnormal;
         case 2:
            return fault;
         case 3:
         default:
            throw new IllegalArgumentException("Invalid sourceState:" + sourceState);
      }
   }

   public static int fromBAlarmState(BAlarmState alarmState) {
      return alarmState.getOrdinal();
   }

   public BSourceState toSourceState() {
      switch (this.getOrdinal()) {
         case 0:
            return BSourceState.normal;
         case 1:
            return BSourceState.fault;
         case 2:
         case 3:
         case 4:
         case 5:
         default:
            return BSourceState.offnormal;
      }
   }

   public static boolean isNormal(BEnum eventState) {
      return eventState.getOrdinal() == 0;
   }

   public static boolean isFault(BEnum eventState) {
      return eventState.getOrdinal() == 1;
   }

   public static boolean isOffnormal(BEnum eventState) {
      return eventState.getOrdinal() != 0 && eventState.getOrdinal() != 1;
   }

   public static int getEventTransitionBits(BEnum eventState) {
      switch (eventState.getOrdinal()) {
         case 0:
            return 1;
         case 1:
            return 2;
         case 2:
         case 3:
         case 4:
         case 5:
         default:
            return 4;
      }
   }

   public static int getEventTransition(BEnum eventState) {
      switch (eventState.getOrdinal()) {
         case 0:
            return 2;
         case 1:
            return 1;
         case 2:
         case 3:
         case 4:
         case 5:
         default:
            return 0;
      }
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
