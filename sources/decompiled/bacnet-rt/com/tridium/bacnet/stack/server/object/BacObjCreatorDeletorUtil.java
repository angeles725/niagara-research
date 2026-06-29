package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.schedule.ScheduleType;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.util.PropertyInfo;

public class BacObjCreatorDeletorUtil {
   public static final String SCHEDULE_DESC_NAME = "schedule_";
   public static final String SCHEDULE_NAME = "DynamicSchedule";
   public static final String CALENDAR_DESC_NAME = "calendar_";
   public static final String CALENDAR_NAME = "DynamicCalendar";
   public static final String DYNAMIC_SLOT = "dynamic";
   public static final String NOTIFICATION_CLASS_DESC_NAME = "notificationClass_";
   public static final String NOTIFICATION_CLASS_NAME = "DynamicAlarmClass";
   public static final int SCHEDULE_TYPE_NUMERIC = 0;
   public static final int SCHEDULE_TYPE_STRING = 1;
   public static final int SCHEDULE_TYPE_ENUMERATED = 2;
   public static final int SCHEDULE_TYPE_BOOLEAN = 3;
   public static final int SCHEDULE_TYPE_UNKNOWN = -1;
   public static final String DAY_SCHEDULE_OPENING_TAG = "0f";
   public static final String EXCEPTION_SCHEDULE_TAGS_REGEX = "2e[\\s\\S]*?2f";
   public static final Logger logger = Logger.getLogger("bacnet.server.object");

   public static int getScheduleType(int inputDataType) {
      switch (inputDataType) {
         case 0:
         case 6:
         case 7:
         case 8:
         case 10:
         case 11:
         case 12:
            return 1;
         case 1:
            return 3;
         case 2:
         case 3:
         case 9:
            return 2;
         case 4:
         case 5:
            return 0;
         default:
            return -1;
      }
   }

   public static int scheduleTypeFromRef(int objectType, int propertyId) {
      PropertyInfo info = BBacnetNetwork.localDevice().getPropertyInfo(objectType, propertyId);
      return info != null ? getScheduleType(info.getAsnType()) : -1;
   }

   public static int getScheduleType(ScheduleType st) {
      String var1 = st.getScheduleType();
      switch (var1) {
         case "schedule:NumericSchedule":
            return 0;
         case "schedule:BooleanSchedule":
            return 3;
         case "schedule:EnumSchedule":
            return 2;
         case "schedule:StringSchedule":
            return 1;
         default:
            return -1;
      }
   }
}
