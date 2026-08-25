package com.tridium.niagarad.license;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import javax.baja.nre.util.TextUtil;

public final class LicenseUtil {
   public static final long INVALID_LICENSE_TIME_MILLIS_FLOOR = 1420070400000L;
   public static final String INVALID_LICENSE_TIME_DATE_FLOOR = "2015-01-01";
   public static final String TRIDIUM_VENDOR = "tridium";

   private LicenseUtil() {
   }

   public static String toKey(String vendor, String feature) {
      return TextUtil.toLowerCase(vendor) + ':' + TextUtil.toLowerCase(feature);
   }

   public static String[] parseList(String value) {
      ArrayList<String> list = new ArrayList<>();
      StringTokenizer st = new StringTokenizer(value, ";");

      while (st.hasMoreTokens()) {
         list.add(st.nextToken().trim());
      }

      return list.toArray(new String[0]);
   }

   public static String formatDate(long millis) {
      return millis == Long.MAX_VALUE ? "never" : new SimpleDateFormat("yyyy-MM-dd").format(new Date(millis));
   }

   public static long parseDate(String date, boolean startOfDay) {
      if (date.equalsIgnoreCase("never")) {
         return Long.MAX_VALUE;
      }

      try {
         StringTokenizer st = new StringTokenizer(date, "- ");
         int year = Integer.parseInt(st.nextToken()) - 1900;
         int month = Integer.parseInt(st.nextToken()) - 1;
         int dayOfMonth = Integer.parseInt(st.nextToken());
         int hourOfDay = startOfDay ? 0 : 23;
         int minute = startOfDay ? 0 : 59;
         int second = startOfDay ? 0 : 59;
         Date d = new GregorianCalendar(year + 1900, month, dayOfMonth, hourOfDay, minute, second).getTime();
         return d.getTime();
      } catch (Exception e) {
         throw new RuntimeException("Invalid license date format yyyy-MM-dd: " + date);
      }
   }
}
