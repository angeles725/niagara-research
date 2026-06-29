package javax.baja.bacnet.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BIComparable;
import javax.baja.sys.BMonth;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BWeekday;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconModule;

@NiagaraType
public final class BBacnetDate extends BSimple implements BIComparable {
   public static final byte UNSPECIFIED = -1;
   public static final byte ODD_MONTHS = 13;
   public static final byte EVEN_MONTHS = 14;
   public static final byte BAJA_ODD_MONTHS = 12;
   public static final byte BAJA_EVEN_MONTHS = 13;
   public static final byte LAST_DAY_OF_MONTH = 32;
   public static final byte ODD_DAYS_OF_MONTH = 33;
   public static final byte EVEN_DAYS_OF_MONTH = 34;
   public static final byte BAJA_ALWAYS_EFFECTIVE = -1;
   public static final byte BAJA_LAST_7_DAYS_OF_MONTH = 33;
   public static final byte BAJA_ODD_DAYS_OF_MONTH = 34;
   public static final byte BAJA_EVEN_DAYS_OF_MONTH = 35;
   private static LexiconModule lex = LexiconModule.make("baja");
   private static final String[] WEEKDAYS_LEXKEYS = new String[]{
      "monday.short", "tuesday.short", "wednesday.short", "thursday.short", "friday.short", "saturday.short", "sunday.short"
   };
   private static final String[] WEEKDAYS = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
   static final int TEXT_LENGTH = 14;
   public static final BBacnetDate DEFAULT = new BBacnetDate(-1, -1, -1, -1);
   public static final Type TYPE = Sys.loadType(BBacnetDate.class);
   private final byte year;
   private final byte month;
   private final byte dayOfMonth;
   private final byte dayOfWeek;
   private final int hashCode;
   private static final Logger logger = Logger.getLogger("bacnet.datatypes");

   private BBacnetDate(int year, int month, int dayOfMonth, int dayOfWeek) {
      if (year < 0) {
         year = -1;
      }

      if (year >= 1900) {
         year -= 1900;
      }

      if (month < 1 || month > 14) {
         month = -1;
      }

      if (dayOfMonth < 1 || dayOfMonth > 34) {
         dayOfMonth = -1;
      }

      if (dayOfWeek < 1 || dayOfWeek > 7) {
         dayOfWeek = -1;
      }

      this.year = (byte)year;
      this.month = (byte)month;
      this.dayOfMonth = (byte)dayOfMonth;
      this.dayOfWeek = (byte)dayOfWeek;
      this.hashCode = year << 24 | month << 16 | dayOfMonth << 8 | dayOfWeek;
   }

   public static BBacnetDate make() {
      return new BBacnetDate(-1, -1, -1, -1);
   }

   public static BBacnetDate make(int year, int month, int dayOfMonth, int dayOfWeek) {
      return new BBacnetDate(year, month, dayOfMonth, dayOfWeek);
   }

   public static BBacnetDate make(int year, int month, int dayOfMonth) {
      if (year != -1 && month >= 1 && month <= 12 && dayOfMonth >= 1 && dayOfMonth <= 31) {
         BAbsTime newDate = BAbsTime.make(year + 1900, BMonth.make(month - 1), dayOfMonth);
         return make(newDate);
      } else {
         return make(year, month, dayOfMonth, -1);
      }
   }

   public static BBacnetDate make(BAbsTime bt) {
      int y = bt.getYear() - 1900;
      int m = bt.getMonth().getOrdinal() + 1;
      int d = bt.getDay();
      int w = bt.getWeekday().getOrdinal();
      if (w == 0) {
         w = 7;
      }

      return new BBacnetDate(y, m, d, w);
   }

   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else if (!(obj instanceof BBacnetDate)) {
         return false;
      } else {
         BBacnetDate d = (BBacnetDate)obj;
         return this.year == d.year && this.month == d.month && this.dayOfMonth == d.dayOfMonth && this.dayOfWeek == d.dayOfWeek;
      }
   }

   public int hashCode() {
      return this.hashCode;
   }

   public String toString(Context context) {
      return this.toString(context, true);
   }

   public String toString(Context context, boolean lexiconize) {
      try {
         StringBuilder sb = new StringBuilder();
         if (this.year == -1) {
            sb.append("****-");
         } else {
            sb.append((this.year & 255) + 1900).append('-');
         }

         if (this.month == -1) {
            sb.append("**-");
         } else if (this.month == 14) {
            sb.append("EV-");
         } else if (this.month == 13) {
            sb.append("OD-");
         } else {
            sb.append(this.month < 10 ? "0" + this.month : String.valueOf(this.month)).append('-');
         }

         if (this.dayOfMonth == -1) {
            sb.append("**-");
         } else if (this.dayOfMonth == 32) {
            sb.append("LD-");
         } else if (this.dayOfMonth == 33) {
            sb.append("OD-");
         } else if (this.dayOfMonth == 34) {
            sb.append("ED-");
         } else {
            sb.append(this.dayOfMonth < 10 ? "0" + this.dayOfMonth : String.valueOf(this.dayOfMonth)).append('-');
         }

         if (this.dayOfWeek == -1) {
            sb.append("***");
         } else if (lexiconize) {
            sb.append(lex.getText(WEEKDAYS_LEXKEYS[this.dayOfWeek - 1], context));
         } else {
            sb.append(WEEKDAYS[this.dayOfWeek - 1]);
         }

         return sb.toString();
      } catch (RuntimeException var4) {
         logger.log(
            Level.SEVERE,
            "BBacnetDate toString() error: year=" + this.year + " month=" + this.month + " dayOfMonth=" + this.dayOfMonth + " dayOfWeek=" + this.dayOfWeek,
            (Throwable)var4
         );
         throw var4;
      }
   }

   public void encode(DataOutput out) throws IOException {
      out.writeByte(this.year);
      out.writeByte(this.month);
      out.writeByte(this.dayOfMonth);
      out.writeByte(this.dayOfWeek);
   }

   public BObject decode(DataInput in) throws IOException {
      byte y = in.readByte();
      byte m = in.readByte();
      byte d = in.readByte();
      byte w = in.readByte();
      return new BBacnetDate(y, m, d, w);
   }

   public String encodeToString() throws IOException {
      return this.toString(null, false);
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         StringTokenizer st = new StringTokenizer(s, "-");
         int y = -1;
         int m = -1;
         int d = -1;
         int w = -1;
         String ys = st.nextToken();
         y = ys.indexOf("*") < 0 ? Integer.parseInt(ys) - 1900 : -1;
         if (st.hasMoreTokens()) {
            String ms = st.nextToken();
            if (ms.indexOf("*") != -1) {
               m = -1;
            } else if (ms.indexOf("EV") != -1) {
               m = 14;
            } else if (ms.indexOf("OD") != -1) {
               m = 13;
            } else {
               m = Integer.parseInt(ms);
            }

            if (st.hasMoreTokens()) {
               String ds = st.nextToken();
               if (ds.indexOf("*") != -1) {
                  d = -1;
               } else if (ds.indexOf("LD") != -1) {
                  d = 32;
               } else if (ds.indexOf("OD") != -1) {
                  d = 33;
               } else if (ds.indexOf("ED") != -1) {
                  d = 34;
               } else {
                  d = Integer.parseInt(ds);
               }

               if (st.hasMoreTokens()) {
                  String ws = st.nextToken();
                  w = ws.indexOf("*") < 0 ? this.weekday(ws) : -1;
               }
            }
         }

         return new BBacnetDate(y, m, d, w);
      } catch (Exception var11) {
         throw new IOException("Error decoding BBacnetDate:" + s + "; exc=" + var11.toString());
      }
   }

   public int getYear() {
      return this.isYearUnspecified() ? -1 : (this.year & 0xFF) + 1900;
   }

   public int getRawYear() {
      return this.year & 0xFF;
   }

   public int getMonth() {
      return this.month;
   }

   public BMonth getBMonth() {
      if (this.isMonthUnspecified()) {
         throw new IllegalStateException("Month is unspecified!");
      } else if (this.isEvenMonths()) {
         throw new IllegalStateException("Month is EVEN MONTHS");
      } else if (this.isOddMonths()) {
         throw new IllegalStateException("Month is ODD MONTHS");
      } else {
         return BMonth.make(this.month - 1);
      }
   }

   public int getDayOfMonth() {
      return this.dayOfMonth;
   }

   public int getDayOfWeek() {
      return this.dayOfWeek;
   }

   public BWeekday getBWeekday() {
      if (this.isDayOfWeekUnspecified()) {
         throw new IllegalStateException("DayOfWeek is unspecified!");
      } else {
         return this.dayOfWeek == 7 ? BWeekday.make(0) : BWeekday.make(this.dayOfWeek);
      }
   }

   public boolean isYearUnspecified() {
      return this.year == -1;
   }

   public boolean isMonthUnspecified() {
      return this.month == -1;
   }

   public boolean isMonthSpecial() {
      return this.isOddMonths() || this.isEvenMonths();
   }

   public boolean isOddMonths() {
      return this.month == 13;
   }

   public boolean isEvenMonths() {
      return this.month == 14;
   }

   public boolean isDayOfMonthUnspecified() {
      return this.dayOfMonth == -1;
   }

   public boolean isDayOfMonthSpecial() {
      return this.isLastDayOfMonth() || this.isOddDaysOfMonth() || this.isEvenDaysOfMonth();
   }

   public boolean isLastDayOfMonth() {
      return this.dayOfMonth == 32;
   }

   public boolean isOddDaysOfMonth() {
      return this.dayOfMonth == 33;
   }

   public boolean isEvenDaysOfMonth() {
      return this.dayOfMonth == 34;
   }

   public boolean isDayOfWeekUnspecified() {
      return this.dayOfWeek == -1;
   }

   public boolean isAnyUnspecified() {
      return this.year == -1 || this.month == -1 || this.dayOfMonth == -1 || this.dayOfWeek == -1;
   }

   public boolean dateEquals(Object obj) {
      return this.compareTo(obj) == 0;
   }

   public int compareTo(Object obj) {
      if (obj == null) {
         throw new ClassCastException();
      } else {
         BBacnetDate other = (BBacnetDate)obj;
         if (!other.isYearUnspecified() && !this.isYearUnspecified()) {
            int y1 = this.year & 255;
            int y2 = other.year & 255;
            if (y1 < y2) {
               return -1;
            }

            if (y1 > y2) {
               return 1;
            }
         }

         if (!other.isMonthUnspecified()
            && !this.isMonthUnspecified()
            && !other.isOddMonths()
            && !this.isOddMonths()
            && !other.isEvenMonths()
            && !this.isEvenMonths()) {
            if (this.month < other.month) {
               return -1;
            }

            if (this.month > other.month) {
               return 1;
            }
         }

         if (!other.isDayOfMonthUnspecified()
            && !this.isDayOfMonthUnspecified()
            && !other.isLastDayOfMonth()
            && !this.isLastDayOfMonth()
            && !other.isEvenDaysOfMonth()
            && !this.isEvenDaysOfMonth()
            && !other.isOddDaysOfMonth()
            && !this.isOddDaysOfMonth()) {
            if (this.dayOfMonth < other.dayOfMonth) {
               return -1;
            }

            if (this.dayOfMonth > other.dayOfMonth) {
               return 1;
            }
         }

         if (!other.isDayOfWeekUnspecified() && !this.isDayOfWeekUnspecified()) {
            if (this.dayOfWeek < other.dayOfWeek) {
               return -1;
            }

            if (this.dayOfWeek > other.dayOfWeek) {
               return 1;
            }
         }

         return 0;
      }
   }

   public boolean isBefore(Object x) {
      return this.compareTo(x) < 0;
   }

   public boolean isAfter(Object x) {
      return this.compareTo(x) > 0;
   }

   public boolean isNotBefore(Object x) {
      return this.compareTo(x) >= 0;
   }

   public boolean isNotAfter(Object x) {
      return this.compareTo(x) <= 0;
   }

   public BAbsTime makeBAbsTime(BAbsTime date) {
      int y = this.isYearUnspecified() ? date.getYear() : this.getYear();
      BMonth m = !this.isMonthUnspecified() && !this.isOddMonths() && !this.isEvenMonths() ? this.getBMonth() : date.getMonth();
      int d = !this.isDayOfMonthUnspecified() && !this.isLastDayOfMonth() ? this.getDayOfMonth() : date.getDay();
      return BAbsTime.make(y, m, d, date.getHour(), date.getMinute(), date.getSecond(), date.getMillisecond(), date.getTimeZone());
   }

   public static BBacnetDate fromString(String s) {
      try {
         return (BBacnetDate)DEFAULT.decodeFromString(s);
      } catch (Exception var2) {
         logger.log(Level.SEVERE, "BBacnetDate.fromString('" + s + "'): error parsing string!!", (Throwable)var2);
         return DEFAULT;
      }
   }

   private int weekday(String wkdayStr) {
      for (int i = 0; i < WEEKDAYS.length; i++) {
         if (wkdayStr.equals(WEEKDAYS[i])) {
            return i + 1;
         }
      }

      for (int ix = 0; ix < WEEKDAYS_LEXKEYS.length; ix++) {
         if (wkdayStr.equals(lex.getText(WEEKDAYS_LEXKEYS[ix], null))) {
            return ix + 1;
         }
      }

      throw new IllegalArgumentException("Invalid weekday:" + wkdayStr);
   }

   public Type getType() {
      return TYPE;
   }
}
