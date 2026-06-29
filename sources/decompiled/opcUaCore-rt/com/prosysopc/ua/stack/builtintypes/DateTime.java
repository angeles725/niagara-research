package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.stack.core.Identifiers;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateTime implements Comparable<DateTime> {
   public static final String DEFAULT_STR_FORMAT = "%TD %TT.%07d %TZ";
   public static final String ISO_8601_STR_FORMAT = "%TF %TT.%07d %TZ";
   private static String strFormat = "%TD %TT.%07d %TZ";
   private static boolean useLocalTimeInToString = false;
   public static final DateTime[] EMPTY_ARRAY = new DateTime[0];
   private static final long MIN_VALUE_UA_EPOCH_100_NANOSECONDS = 0L;
   private static final long MAX_VALUE_UA_EPOCH_100_NANOSECONDS = 2650153247990000000L;
   @Deprecated
   public static final NodeId ID = Identifiers.DateTime;
   private static final TimeZone UTC = TimeZone.getTimeZone("GMT");
   public static final long OffsetToGregorianCalendarZero = 116444736000000000L;
   public static final DateTime MIN_VALUE = valueOf(0L);
   public static final DateTime MAX_VALUE = valueOf(2650153247990000000L);
   public static final Instant MIN = MIN_VALUE.toInstant();
   public static final Instant MAX = MAX_VALUE.toInstant();
   private final long value;
   private final Instant instant;

   public static DateTime currentTime() {
      return new DateTime();
   }

   public static DateTime fromInstant(Instant var0) {
      if (var0 == null) {
         return null;
      } else if (var0.isBefore(MIN)) {
         return MIN_VALUE;
      } else if (var0.isAfter(MAX)) {
         return MAX_VALUE;
      } else {
         long var1 = instantToOpcUaEpoch100NanosecondsClampMinMax(var0);
         return new DateTime(var1, uaEpoch100NanosecondsToInstant(var1));
      }
   }

   public static DateTime fromMillis(long var0) {
      return new DateTime(var0 * 10000L + 116444736000000000L);
   }

   public static String getStrFormat() {
      return strFormat;
   }

   public static boolean isUseLocalTimeInToString() {
      return useLocalTimeInToString;
   }

   public static DateTime parseDateTime(String var0) throws ParseException {
      int var1 = var0.length();
      if (var1 < 10) {
         throw new ParseException(
            "Cannot parse DateTime from " + var0 + " expecting format 'yyyy-MM-dd'T'hh:mm:ssZ', for example '2011-04-13T11:47:12Z' for UTC timezone", 0
         );
      } else {
         int var2 = Integer.parseInt(var0.substring(0, 4));
         int var3 = Integer.parseInt(var0.substring(5, 7));
         int var4 = Integer.parseInt(var0.substring(8, 10));
         int var5 = var1 < 13 ? 0 : Integer.parseInt(var0.substring(11, 13));
         int var6 = var1 < 16 ? 0 : Integer.parseInt(var0.substring(14, 16));
         int var7 = var1 < 19 ? 0 : Integer.parseInt(var0.substring(17, 19));
         TimeZone var8 = UTC;
         if (var1 > 19) {
            String var9 = var0.substring(19);
            if (var9.charAt(0) == '+' || var9.charAt(0) == '-') {
               var9 = "GMT" + var9;
            }

            var8 = TimeZone.getTimeZone(var9);
         }

         return valueOf(new DateTime(var2, var3 - 1, var4, var5, var6, var7, 0, var8).getValue());
      }
   }

   public static void setStrFormat(String var0) {
      strFormat = var0;
   }

   public static void setUseLocalTimeInToString(boolean var0) {
      useLocalTimeInToString = var0;
   }

   public static DateTime valueOf(long var0) {
      if (var0 < 0L) {
         var0 = 0L;
      }

      if (var0 > 2650153247990000000L) {
         var0 = 2650153247990000000L;
      }

      return new DateTime(var0, uaEpoch100NanosecondsToInstant(var0));
   }

   private static long instantToOpcUaEpoch100NanosecondsClampMinMax(Instant var0) {
      if (var0.isBefore(MIN)) {
         return MIN_VALUE.getValue();
      } else if (var0.isAfter(MAX)) {
         return MAX_VALUE.getValue();
      } else {
         long var1 = var0.getEpochSecond();
         long var3 = var0.getNano();
         long var5 = var3 / 100L;
         long var7 = var1 * 1000L * 10000L + 116444736000000000L;
         return var7 + var5;
      }
   }

   private static Instant uaEpoch100NanosecondsToInstant(long var0) {
      long var2 = (var0 - 116444736000000000L) / 10000000L;
      long var4 = (var0 - 116444736000000000L) % 10000000L;
      long var6 = var4 * 100L;
      return Instant.ofEpochSecond(var2, var6);
   }

   @Deprecated
   public DateTime() {
      this(System.currentTimeMillis() * 10000L + 116444736000000000L);
   }

   @Deprecated
   public DateTime(Calendar var1) {
      this.value = var1.getTimeInMillis() * 10000L + 116444736000000000L;
      this.instant = uaEpoch100NanosecondsToInstant(this.value);
   }

   @Deprecated
   public DateTime(int var1, int var2, int var3, int var4, int var5, int var6) {
      this(var1, var2, var3, var4, var5, var6, 0, UTC);
   }

   @Deprecated
   public DateTime(int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      this(var1, var2, var3, var4, var5, var6, var7, UTC);
   }

   @Deprecated
   public DateTime(int var1, int var2, int var3, int var4, int var5, int var6, int var7, TimeZone var8) {
      GregorianCalendar var9 = new GregorianCalendar(var1, var2, var3, var4, var5, var6);
      var9.setTimeZone(var8);
      this.value = var7 / 100 + var9.getTimeInMillis() * 10000L + 116444736000000000L;
      this.instant = uaEpoch100NanosecondsToInstant(this.value);
   }

   @Deprecated
   public DateTime(long var1) {
      this(var1, uaEpoch100NanosecondsToInstant(var1));
   }

   private DateTime(long var1, Instant var3) {
      this.value = var1;
      if (var3 == null) {
         throw new IllegalStateException("The instant was null");
      } else {
         this.instant = var3;
      }
   }

   public int compareTo(DateTime var1) {
      if (this.value < var1.value) {
         return -1;
      } else {
         return this.value > var1.value ? 1 : 0;
      }
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         DateTime var2 = (DateTime)var1;
         return this.value == var2.value;
      }
   }

   @Deprecated
   public GregorianCalendar getCalendar(TimeZone var1) {
      GregorianCalendar var2 = new GregorianCalendar(var1);
      var2.setTimeInMillis(this.getTimeInMillis());
      return var2;
   }

   @Deprecated
   public GregorianCalendar getLocalCalendar() {
      return this.getCalendar(TimeZone.getDefault());
   }

   public long getTimeInMillis() {
      return this.instant.toEpochMilli();
   }

   @Deprecated
   public GregorianCalendar getUtcCalendar() {
      return this.getCalendar(UTC);
   }

   public long getValue() {
      return this.value;
   }

   @Override
   public int hashCode() {
      return (int)(this.value ^ this.value >> 32);
   }

   public DateTime minus(long var1, TemporalUnit var3) {
      return this.plus(-var1, var3);
   }

   public DateTime minus(long var1, TimeUnit var3) {
      return this.plus(-var1, var3);
   }

   public DateTime minus(TemporalAmount var1) {
      return this.plus(var1);
   }

   public DateTime minusMillis(double var1) {
      return this.plusMillis(-var1);
   }

   public DateTime minusMillis(long var1) {
      return this.plusMillis(-var1);
   }

   public DateTime minusNanos(long var1) {
      return this.plusNanos(-var1);
   }

   public DateTime plus(long var1, TemporalUnit var3) {
      return fromInstant(this.toInstant().plus(var1, var3));
   }

   public DateTime plus(long var1, TimeUnit var3) {
      switch (var3) {
         case MICROSECONDS:
         case NANOSECONDS:
            return this.plusNanos(var3.toNanos(var1));
         default:
            return this.plusMillis(var3.toMillis(var1));
      }
   }

   public DateTime plus(TemporalAmount var1) {
      return fromInstant(this.toInstant().plus(var1));
   }

   public DateTime plusMillis(double var1) {
      return valueOf(this.getValue() + Math.round(var1 * 10000.0));
   }

   public DateTime plusMillis(long var1) {
      return valueOf(this.getValue() + var1 * 10000L);
   }

   public DateTime plusNanos(long var1) {
      return valueOf(this.getValue() + var1 / 100L);
   }

   public Instant toInstant() {
      return this.instant;
   }

   @Override
   public String toString() {
      GregorianCalendar var1 = useLocalTimeInToString ? this.getLocalCalendar() : this.getUtcCalendar();
      long var2 = this.value % 10000000L;
      return String.format(Locale.ROOT, strFormat, var1, var1, var2, var1);
   }
}
