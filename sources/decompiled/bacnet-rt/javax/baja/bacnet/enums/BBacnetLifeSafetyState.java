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
   range = {@Range("quiet"), @Range("preAlarm"), @Range("alarm"), @Range("fault"), @Range("faultPreAlarm"), @Range("faultAlarm"), @Range("notReady"), @Range("active"), @Range("tamper"), @Range("testAlarm"), @Range("testActive"), @Range("testFault"), @Range("testFaultAlarm"), @Range("holdup"), @Range("duress"), @Range("tamperAlarm"), @Range("abnormal"), @Range("emergencyPower"), @Range("delayed"), @Range("blocked"), @Range("localAlarm"), @Range("generalAlarm"), @Range("supervisory"), @Range("testSupervisory")}
)
public final class BBacnetLifeSafetyState extends BFrozenEnum implements BacnetConst {
   public static final int QUIET = 0;
   public static final int PRE_ALARM = 1;
   public static final int ALARM = 2;
   public static final int FAULT = 3;
   public static final int FAULT_PRE_ALARM = 4;
   public static final int FAULT_ALARM = 5;
   public static final int NOT_READY = 6;
   public static final int ACTIVE = 7;
   public static final int TAMPER = 8;
   public static final int TEST_ALARM = 9;
   public static final int TEST_ACTIVE = 10;
   public static final int TEST_FAULT = 11;
   public static final int TEST_FAULT_ALARM = 12;
   public static final int HOLDUP = 13;
   public static final int DURESS = 14;
   public static final int TAMPER_ALARM = 15;
   public static final int ABNORMAL = 16;
   public static final int EMERGENCY_POWER = 17;
   public static final int DELAYED = 18;
   public static final int BLOCKED = 19;
   public static final int LOCAL_ALARM = 20;
   public static final int GENERAL_ALARM = 21;
   public static final int SUPERVISORY = 22;
   public static final int TEST_SUPERVISORY = 23;
   public static final BBacnetLifeSafetyState quiet = new BBacnetLifeSafetyState(0);
   public static final BBacnetLifeSafetyState preAlarm = new BBacnetLifeSafetyState(1);
   public static final BBacnetLifeSafetyState alarm = new BBacnetLifeSafetyState(2);
   public static final BBacnetLifeSafetyState fault = new BBacnetLifeSafetyState(3);
   public static final BBacnetLifeSafetyState faultPreAlarm = new BBacnetLifeSafetyState(4);
   public static final BBacnetLifeSafetyState faultAlarm = new BBacnetLifeSafetyState(5);
   public static final BBacnetLifeSafetyState notReady = new BBacnetLifeSafetyState(6);
   public static final BBacnetLifeSafetyState active = new BBacnetLifeSafetyState(7);
   public static final BBacnetLifeSafetyState tamper = new BBacnetLifeSafetyState(8);
   public static final BBacnetLifeSafetyState testAlarm = new BBacnetLifeSafetyState(9);
   public static final BBacnetLifeSafetyState testActive = new BBacnetLifeSafetyState(10);
   public static final BBacnetLifeSafetyState testFault = new BBacnetLifeSafetyState(11);
   public static final BBacnetLifeSafetyState testFaultAlarm = new BBacnetLifeSafetyState(12);
   public static final BBacnetLifeSafetyState holdup = new BBacnetLifeSafetyState(13);
   public static final BBacnetLifeSafetyState duress = new BBacnetLifeSafetyState(14);
   public static final BBacnetLifeSafetyState tamperAlarm = new BBacnetLifeSafetyState(15);
   public static final BBacnetLifeSafetyState abnormal = new BBacnetLifeSafetyState(16);
   public static final BBacnetLifeSafetyState emergencyPower = new BBacnetLifeSafetyState(17);
   public static final BBacnetLifeSafetyState delayed = new BBacnetLifeSafetyState(18);
   public static final BBacnetLifeSafetyState blocked = new BBacnetLifeSafetyState(19);
   public static final BBacnetLifeSafetyState localAlarm = new BBacnetLifeSafetyState(20);
   public static final BBacnetLifeSafetyState generalAlarm = new BBacnetLifeSafetyState(21);
   public static final BBacnetLifeSafetyState supervisory = new BBacnetLifeSafetyState(22);
   public static final BBacnetLifeSafetyState testSupervisory = new BBacnetLifeSafetyState(23);
   public static final BBacnetLifeSafetyState DEFAULT = quiet;
   public static final Type TYPE = Sys.loadType(BBacnetLifeSafetyState.class);
   public static final int MAX_ASHRAE_ID = 23;
   public static final int MAX_RESERVED_ID = 255;
   public static final int MAX_ID = 65535;

   public static BBacnetLifeSafetyState make(int ordinal) {
      return (BBacnetLifeSafetyState)quiet.getRange().get(ordinal, false);
   }

   public static BBacnetLifeSafetyState make(String tag) {
      return (BBacnetLifeSafetyState)quiet.getRange().get(tag);
   }

   private BBacnetLifeSafetyState(int ordinal) {
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
      return id > 23 && id <= 255;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 23;
   }
}
