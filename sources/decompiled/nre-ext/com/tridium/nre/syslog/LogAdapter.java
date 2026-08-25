package com.tridium.nre.syslog;

import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogAdapter {
   private static final String[] MONTH_NAMES = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

   public String adaptPriority(LogRecord logRecord, Facility facility) {
      int code = (facility.getId() << 3) + this.adaptSeverity(logRecord);
      return String.format("<%d>", code);
   }

   public int adaptSeverity(LogRecord logRecord) {
      Level level = logRecord.getLevel();
      if (level.intValue() >= Level.SEVERE.intValue()) {
         return Severity.ERROR.getLevel();
      } else if (level.intValue() >= Level.WARNING.intValue()) {
         return Severity.WARNING.getLevel();
      } else {
         return level.intValue() >= Level.INFO.intValue() ? Severity.INFO.getLevel() : Severity.DEBUG.getLevel();
      }
   }

   public String adaptTimeStamp(LogRecord logRecord) {
      long millis = logRecord.getMillis();
      return getFormattedDateFromMillis(millis);
   }

   public static String getFormattedDateFromMillis(long millis) {
      GregorianCalendar calendar = new GregorianCalendar();
      calendar.setTimeInMillis(millis);
      int month = limit(calendar.get(2), 0, 11);
      String mmm = MONTH_NAMES[month];
      int day = limit(calendar.get(5), 1, 31);
      String dd = indent(String.valueOf(day), 2, ' ');
      int hour = limit(calendar.get(11), 0, 23);
      String hh = indent(String.valueOf(hour), 2, '0');
      int minute = limit(calendar.get(12), 0, 59);
      String mm = indent(String.valueOf(minute), 2, '0');
      int second = limit(calendar.get(13), 0, 59);
      String ss = indent(String.valueOf(second), 2, '0');
      return String.format("%s %s %s:%s:%s", mmm, dd, hh, mm, ss);
   }

   private static String indent(String s, int requiredLength, char identChar) {
      StringBuilder sBuilder = new StringBuilder(s);

      while (sBuilder.length() < requiredLength) {
         sBuilder.insert(0, identChar);
      }

      return sBuilder.toString();
   }

   private static int limit(int value, int min, int max) {
      return value < min ? min : Math.min(value, max);
   }
}
