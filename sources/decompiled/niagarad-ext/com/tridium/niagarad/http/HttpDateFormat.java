package com.tridium.niagarad.http;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class HttpDateFormat {
   private static final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);

   public static String format(long dateTime, char dateSep) {
      return formatDate(new Date(dateTime), dateSep);
   }

   public static String format(long dateTime) {
      return formatDate(new Date(dateTime), ' ');
   }

   public static String formatDate(Date date) {
      return formatDate(date, ' ');
   }

   public static String formatDate(Date date, char dateSep) {
      HttpDateFormat.DateTimeInfo dt = new HttpDateFormat.DateTimeInfo();
      synchronized (cal) {
         cal.setTime(date);
         dt.dayOfWeek = cal.get(7);
         dt.dayOfMonth = cal.get(5);
         dt.month = cal.get(2);
         dt.year = cal.get(1);
         dt.hour = cal.get(11);
         dt.minute = cal.get(12);
         dt.second = cal.get(13);
      }

      return formatDateTimeInfo(dt, dateSep);
   }

   private static String formatDateTimeInfo(HttpDateFormat.DateTimeInfo dt, char dateSep) {
      StringBuilder sbuf = new StringBuilder(30);
      sbuf.append(getDayOfWeekString(dt.dayOfWeek));
      sbuf.append(", ");
      if (dt.dayOfMonth < 10) {
         sbuf.append('0');
      }

      sbuf.append(dt.dayOfMonth);
      sbuf.append(dateSep);
      sbuf.append(getMonthString(dt.month));
      sbuf.append(dateSep);
      sbuf.append(dt.year).append(' ');
      if (dt.hour < 10) {
         sbuf.append('0');
      }

      sbuf.append(dt.hour).append(':');
      if (dt.minute < 10) {
         sbuf.append('0');
      }

      sbuf.append(dt.minute).append(':');
      if (dt.second < 10) {
         sbuf.append('0');
      }

      sbuf.append(dt.second);
      sbuf.append(" GMT");
      return sbuf.toString();
   }

   public static String getDayOfWeekString(int dayOfWeek) {
      switch (dayOfWeek) {
         case 1:
            return "Sun";
         case 2:
            return "Mon";
         case 3:
            return "Tue";
         case 4:
            return "Wed";
         case 5:
            return "Thu";
         case 6:
            return "Fri";
         case 7:
            return "Sat";
         default:
            throw new IllegalArgumentException("Invalid weekday index: " + dayOfWeek);
      }
   }

   public static String getMonthString(int month) {
      switch (month) {
         case 0:
            return "Jan";
         case 1:
            return "Feb";
         case 2:
            return "Mar";
         case 3:
            return "Apr";
         case 4:
            return "May";
         case 5:
            return "Jun";
         case 6:
            return "Jul";
         case 7:
            return "Aug";
         case 8:
            return "Sep";
         case 9:
            return "Oct";
         case 10:
            return "Nov";
         case 11:
            return "Dec";
         default:
            throw new IllegalArgumentException("Invalid month index: " + month);
      }
   }

   public static long parse(String dateString) throws IllegalArgumentException {
      HttpDateFormat.DateTimeInfo d = new HttpDateFormat.DateTimeInfo();
      StringTokenizer st = new StringTokenizer(dateString, " ,");
      String token = st.nextToken();
      d.dayOfWeek = getWeekday(token);
      if (d.dayOfWeek != -1) {
         token = st.nextToken(" ,-");
      }

      d.dayOfMonth = getDayOfMonth(token);
      boolean asctime = false;
      if (d.dayOfMonth != -1) {
         parseDate1OrDate2(d, st);
      } else {
         asctime = true;
         d.month = getMonth(token);
         if (d.month == -1) {
            throw new IllegalArgumentException("Invalid date string: " + dateString);
         }

         parseAsctimeDate(d, st);
      }

      parseTime(d, st);
      if (asctime) {
         d.year = Integer.parseInt(st.nextToken());
      }

      synchronized (cal) {
         cal.set(d.year, d.month, d.dayOfMonth, d.hour, d.minute, d.second);
         return cal.getTime().getTime();
      }
   }

   public static int getWeekday(String wdStr) {
      int ch = wdStr.charAt(0);
      switch (ch) {
         case 70:
         case 102:
            return 6;
         case 77:
         case 109:
            return 2;
         case 83:
         case 115:
            int ch2 = wdStr.charAt(1);
            if (ch2 != 117 && ch2 != 85) {
               return 7;
            }

            return 1;
         case 84:
         case 116:
            int ch2 = wdStr.charAt(1);
            if (ch2 != 117 && ch2 != 85) {
               return 5;
            }

            return 3;
         case 87:
         case 119:
            return 4;
         default:
            return -1;
      }
   }

   public static int getDayOfMonth(String dayStr) {
      if (Character.isDigit(dayStr.charAt(0))) {
         try {
            return Integer.parseInt(dayStr);
         } catch (NumberFormatException e) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public static int getMonth(String monthStr) {
      int ch1 = monthStr.charAt(0);
      switch (ch1) {
         case 65:
         case 97:
            int ch2 = monthStr.charAt(1);
            if (ch2 != 112 && ch2 != 80) {
               return 7;
            }

            return 3;
         case 66:
         case 67:
         case 69:
         case 71:
         case 72:
         case 73:
         case 75:
         case 76:
         case 80:
         case 81:
         case 82:
         case 84:
         case 85:
         case 86:
         case 87:
         case 88:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         case 98:
         case 99:
         case 101:
         case 103:
         case 104:
         case 105:
         case 107:
         case 108:
         case 112:
         case 113:
         case 114:
         default:
            return -1;
         case 68:
         case 100:
            return 11;
         case 70:
         case 102:
            return 1;
         case 74:
         case 106:
            int ch2 = monthStr.charAt(1);
            if (ch2 != 97 && ch2 != 65) {
               int ch3 = monthStr.charAt(2);
               if (ch3 != 110 && ch3 != 78) {
                  return 6;
               }

               return 5;
            }

            return 0;
         case 77:
         case 109:
            int ch2 = monthStr.charAt(2);
            if (ch2 != 114 && ch2 != 82) {
               return 4;
            }

            return 2;
         case 78:
         case 110:
            return 10;
         case 79:
         case 111:
            return 9;
         case 83:
         case 115:
            return 8;
      }
   }

   private static void parseDate1OrDate2(HttpDateFormat.DateTimeInfo d, StringTokenizer st) {
      String token = st.nextToken(" -");
      d.month = getMonth(token);
      if (d.month == -1) {
         throw new IllegalArgumentException("Invalid month string: " + token);
      }

      token = st.nextToken();
      d.year = Integer.parseInt(token);
      if (d.year < 100) {
         if (d.year < 60) {
            d.year += 2000;
         } else {
            d.year += 1900;
         }
      }
   }

   private static void parseAsctimeDate(HttpDateFormat.DateTimeInfo d, StringTokenizer st) {
      d.dayOfMonth = Integer.parseInt(st.nextToken());
   }

   private static void parseTime(HttpDateFormat.DateTimeInfo d, StringTokenizer st) {
      d.hour = Integer.parseInt(st.nextToken(" :"));
      d.minute = Integer.parseInt(st.nextToken());
      d.second = Integer.parseInt(st.nextToken());
   }

   private static class DateTimeInfo {
      public int dayOfWeek = -1;
      public int dayOfMonth = -1;
      public int month = -1;
      public int year = -1;
      public int hour = -1;
      public int minute = -1;
      public int second = -1;

      private DateTimeInfo() {
      }

      public void printAttributes() {
         System.out.println("DateTimeInfo:");
         System.out.println("  dayOfWeek : " + this.dayOfWeek);
         System.out.println("  dayOfMonth: " + this.dayOfMonth);
         System.out.println("  month     : " + this.month);
         System.out.println("  year      : " + this.year);
         System.out.println("  hour      : " + this.hour);
         System.out.println("  minute    : " + this.minute);
         System.out.println("  second    : " + this.second);
      }
   }
}
