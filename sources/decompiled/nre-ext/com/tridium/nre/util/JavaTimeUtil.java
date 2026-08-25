package com.tridium.nre.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;

public class JavaTimeUtil {
   public static final String[] _TIMEZONE_MONTHS = new String[]{
      "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"
   };
   public static final String[] _DAYS_OF_WEEK = new String[]{null, "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
   public static final int DOM_MODE;
   public static final int DOW_IN_MONTH_MODE;
   public static final int DOW_GE_DOM_MODE;
   public static final int DOW_LE_DOM_MODE;
   public static final int WALL_TIME = 0;
   public static final int STANDARD_TIME = 1;
   public static final int UTC_TIME = 2;

   public static String convertOlsonIdToTimeZoneXml(String olsonId) {
      String timezoneXml = null;
      if (olsonId != null) {
         TimeZone javaTimeZone = TimeZone.getTimeZone(olsonId);
         if (javaTimeZone != null && javaTimeZone.getID().equals(olsonId)) {
            try {
               ByteBuffer buffer = new ByteBuffer();
               XWriter content = new XWriter(buffer.getOutputStream());
               XElem zoneElem = new XElem();
               zoneElem.setName("zone");
               zoneElem.setAttr("id", javaTimeZone.getID());
               zoneElem.setAttr("utcOffset", String.valueOf(javaTimeZone.getRawOffset()));
               if (javaTimeZone.useDaylightTime()) {
                  XElem dstElem = new XElem();
                  dstElem.setName("dst");
                  double dstHourSavings = javaTimeZone.getDSTSavings() / 3600000.0;
                  dstElem.setAttr("savings", dstHourSavings + "h");

                  Class<?> zoneInfoClass;
                  try {
                     zoneInfoClass = Class.forName("sun.util.calendar.ZoneInfo");
                  } catch (ClassNotFoundException exception) {
                     try {
                        content.close();
                     } catch (Exception var14) {
                     }

                     return null;
                  }

                  SimpleTimeZone currentZoneRules;
                  if (zoneInfoClass.isInstance(javaTimeZone)) {
                     Method getLastRuleInstanceMethod = zoneInfoClass.getMethod("getLastRuleInstance");
                     currentZoneRules = (SimpleTimeZone)getLastRuleInstanceMethod.invoke(javaTimeZone, (Object[])null);
                  } else {
                     if (!(javaTimeZone instanceof SimpleTimeZone)) {
                        try {
                           content.close();
                        } catch (Exception var13) {
                        }

                        throw new Exception("Unknown Java Time Zone subclass");
                     }

                     currentZoneRules = (SimpleTimeZone)javaTimeZone;
                  }

                  dstElem.addContent(convertOlsonRuleToTimeZoneXml(currentZoneRules, true));
                  dstElem.addContent(convertOlsonRuleToTimeZoneXml(currentZoneRules, false));
                  zoneElem.addContent(dstElem);
               }

               zoneElem.write(content);
               content.flush();
               content.close();
               timezoneXml = new String(buffer.toByteArray());
            } catch (Exception e) {
               e.printStackTrace();
               timezoneXml = null;
            }
         } else {
            timezoneXml = null;
         }
      }

      return timezoneXml;
   }

   public static XElem convertOlsonRuleToTimeZoneXml(SimpleTimeZone rules, boolean start) throws NoSuchFieldException, IllegalAccessException {
      Class<?> clazz = SimpleTimeZone.class;
      Field modeField = clazz.getDeclaredField(start ? "startMode" : "endMode");
      Field monthField = clazz.getDeclaredField(start ? "startMonth" : "endMonth");
      Field dayField = clazz.getDeclaredField(start ? "startDay" : "endDay");
      Field dayOfWeekField = clazz.getDeclaredField(start ? "startDayOfWeek" : "endDayOfWeek");
      Field timeField = clazz.getDeclaredField(start ? "startTime" : "endTime");
      Field timeModeField = clazz.getDeclaredField(start ? "startTimeMode" : "endTimeMode");
      modeField.setAccessible(true);
      monthField.setAccessible(true);
      dayField.setAccessible(true);
      dayOfWeekField.setAccessible(true);
      timeField.setAccessible(true);
      timeModeField.setAccessible(true);
      int mode = modeField.getInt(rules);
      int month = monthField.getInt(rules);
      int day = dayField.getInt(rules);
      int dayOfWeek = dayOfWeekField.getInt(rules);
      int time = timeField.getInt(rules);
      int timeMode = timeModeField.getInt(rules);
      XElem dstElem = new XElem();
      dstElem.setName(start ? "start" : "end");
      long hours = time / 3600000L;
      long minutes = time % 3600000L / 60000L;
      String timeString = hours + ":" + TextUtil.padZeros(String.valueOf(minutes), 2);
      if (timeMode == 2) {
         timeString = timeString + " utc";
      } else if (timeMode == 1) {
         timeString = timeString + " standard";
      }

      dstElem.setAttr("time", timeString);
      dstElem.setAttr("month", _TIMEZONE_MONTHS[month]);
      if (mode == DOM_MODE) {
         dstElem.setAttr("day", String.valueOf(day));
      } else if (mode == DOW_IN_MONTH_MODE) {
         String week = null;
         if (day == 1) {
            week = "first";
         } else if (day == 2) {
            week = "second";
         } else if (day == 3) {
            week = "third";
         } else if (day == 4) {
            week = "fourth";
         } else if (day == 5) {
            week = "fifth";
         } else if (day == -1) {
            week = "last";
         }

         dstElem.setAttr("week", String.valueOf(week));
         dstElem.setAttr("weekday", _DAYS_OF_WEEK[dayOfWeek]);
      } else if (mode == DOW_GE_DOM_MODE) {
         dstElem.setAttr("day", day + "...");
         dstElem.setAttr("weekday", _DAYS_OF_WEEK[dayOfWeek]);
      } else if (mode == DOW_LE_DOM_MODE) {
         dstElem.setAttr("day", "..." + day);
         dstElem.setAttr("weekday", _DAYS_OF_WEEK[dayOfWeek]);
      }

      return dstElem;
   }

   static {
      int domModeValue = 1;
      int dowInMonthModeValue = 2;
      int dowGeDomModeValue = 3;
      int dowLeDomModeValue = 4;

      try {
         Field DOM_MODE_FIELD = SimpleTimeZone.class.getDeclaredField("DOM_MODE");
         Field DOW_IN_MONTH_MODE = SimpleTimeZone.class.getDeclaredField("DOW_IN_MONTH_MODE");
         Field DOW_GE_DOM_MODE = SimpleTimeZone.class.getDeclaredField("DOW_GE_DOM_MODE");
         Field DOW_LE_DOM_MODE = SimpleTimeZone.class.getDeclaredField("DOW_LE_DOM_MODE");
         DOM_MODE_FIELD.setAccessible(true);
         DOW_IN_MONTH_MODE.setAccessible(true);
         DOW_GE_DOM_MODE.setAccessible(true);
         DOW_LE_DOM_MODE.setAccessible(true);
         domModeValue = DOM_MODE_FIELD.getInt(null);
         dowInMonthModeValue = DOW_IN_MONTH_MODE.getInt(null);
         dowGeDomModeValue = DOW_GE_DOM_MODE.getInt(null);
         dowLeDomModeValue = DOW_LE_DOM_MODE.getInt(null);
      } catch (Exception e) {
         domModeValue = 1;
         dowInMonthModeValue = 2;
         dowGeDomModeValue = 3;
         dowLeDomModeValue = 4;
      } finally {
         DOM_MODE = domModeValue;
         DOW_IN_MONTH_MODE = dowInMonthModeValue;
         DOW_GE_DOM_MODE = dowGeDomModeValue;
         DOW_LE_DOM_MODE = dowLeDomModeValue;
      }
   }
}
