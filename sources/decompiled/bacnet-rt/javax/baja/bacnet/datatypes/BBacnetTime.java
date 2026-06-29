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
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BTime;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BBacnetTime extends BSimple implements BIComparable {
   public static final byte UNSPECIFIED = -1;
   public static final int TEXT_LENGTH = 11;
   public static final BBacnetTime DEFAULT = new BBacnetTime(-1, -1, -1, -1);
   public static final Type TYPE = Sys.loadType(BBacnetTime.class);
   private final byte hour;
   private final byte minute;
   private final byte second;
   private final byte hundredth;
   private final int hashCode;
   private static final Logger logger = Logger.getLogger("bacnet.datatypes");

   private BBacnetTime(int hour, int minute, int second, int hundredth) {
      this.hour = (byte)hour;
      this.minute = (byte)minute;
      this.second = (byte)second;
      this.hundredth = (byte)hundredth;
      this.hashCode = hour << 24 | minute << 16 | second << 8 | hundredth;
   }

   public static BBacnetTime make() {
      return new BBacnetTime(-1, -1, -1, -1);
   }

   public static BBacnetTime make(int hour, int minute, int second, int hundredth) {
      if ((hour < -1 || hour > 23) && hour != 255) {
         throw new IllegalArgumentException("BBacnetTime: invalid hour:" + hour);
      } else if ((minute < -1 || minute > 59) && minute != 255) {
         throw new IllegalArgumentException("BBacnetTime: invalid minute:" + minute);
      } else if ((second < -1 || second > 59) && second != 255) {
         throw new IllegalArgumentException("BBacnetTime: invalid second:" + second);
      } else if ((hundredth < -1 || hundredth > 99) && hundredth != 255) {
         throw new IllegalArgumentException("BBacnetTime: invalid hundredth:" + hundredth);
      } else {
         return new BBacnetTime(hour, minute, second, hundredth);
      }
   }

   public static BBacnetTime make(BAbsTime bt) {
      int h = bt.getHour();
      int n = bt.getMinute();
      int s = bt.getSecond();
      int u = bt.getMillisecond() / 10;
      return new BBacnetTime(h, n, s, u);
   }

   public static BBacnetTime make(BTime bt) {
      int h = bt.getHour();
      int n = bt.getMinute();
      int s = bt.getSecond();
      int u = bt.getMillisecond() / 10;
      return new BBacnetTime(h, n, s, u);
   }

   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else if (!(obj instanceof BBacnetTime)) {
         return false;
      } else {
         BBacnetTime d = (BBacnetTime)obj;
         return this.hour == d.hour && this.minute == d.minute && this.second == d.second && this.hundredth == d.hundredth;
      }
   }

   public int hashCode() {
      return this.hashCode;
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      if (this.hour == -1) {
         sb.append("**:");
      } else {
         sb.append(this.hour < 10 ? "0" + this.hour : String.valueOf(this.hour)).append(':');
      }

      if (this.minute == -1) {
         sb.append("**:");
      } else {
         sb.append(this.minute < 10 ? "0" + this.minute : String.valueOf(this.minute)).append(':');
      }

      if (this.second == -1) {
         sb.append("**.");
      } else {
         sb.append(this.second < 10 ? "0" + this.second : String.valueOf(this.second)).append('.');
      }

      if (this.hundredth == -1) {
         sb.append("**");
      } else {
         sb.append(this.hundredth < 10 ? "0" + this.hundredth : String.valueOf(this.hundredth));
      }

      return sb.toString();
   }

   public void encode(DataOutput out) throws IOException {
      out.writeByte(this.hour);
      out.writeByte(this.minute);
      out.writeByte(this.second);
      out.writeByte(this.hundredth);
   }

   public BObject decode(DataInput in) throws IOException {
      byte h = in.readByte();
      byte m = in.readByte();
      byte s = in.readByte();
      byte u = in.readByte();
      return new BBacnetTime(h, m, s, u);
   }

   public String encodeToString() throws IOException {
      return this.toString(null);
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         StringTokenizer st = new StringTokenizer(s, ":.");
         int h = -1;
         int m = -1;
         int e = -1;
         int u = -1;
         String hs = st.nextToken();
         h = hs.indexOf("*") < 0 ? Integer.parseInt(hs) : -1;
         if (st.hasMoreTokens()) {
            String ms = st.nextToken();
            m = ms.indexOf("*") < 0 ? Integer.parseInt(ms) : -1;
            if (st.hasMoreTokens()) {
               String es = st.nextToken();
               e = es.indexOf("*") < 0 ? Integer.parseInt(es) : -1;
               if (st.hasMoreTokens()) {
                  String us = st.nextToken();
                  u = us.indexOf("*") < 0 ? Integer.parseInt(us) : -1;
               }
            }
         }

         return new BBacnetTime(h, m, e, u);
      } catch (Exception var11) {
         throw new IOException("Error decoding BBacnetTime:" + s);
      }
   }

   public int getHour() {
      return this.hour;
   }

   public int getMinute() {
      return this.minute;
   }

   public int getSecond() {
      return this.second;
   }

   public int getHundredth() {
      return this.hundredth;
   }

   public boolean isHourUnspecified() {
      return this.hour == -1;
   }

   public boolean isMinuteUnspecified() {
      return this.minute == -1;
   }

   public boolean isSecondUnspecified() {
      return this.second == -1;
   }

   public boolean isHundredthUnspecified() {
      return this.hundredth == -1;
   }

   public boolean isAnyUnspecified() {
      return this.hour == -1 || this.minute == -1 || this.second == -1 || this.hundredth == -1;
   }

   public boolean timeEquals(Object obj) {
      return this.compareTo(obj) == 0;
   }

   public int compareTo(Object obj) {
      if (obj == null) {
         throw new ClassCastException();
      } else if (((BObject)obj).getType() == TYPE) {
         BBacnetTime other = (BBacnetTime)obj;
         if (!other.isHourUnspecified() && !this.isHourUnspecified()) {
            if (this.hour < other.hour) {
               return -1;
            }

            if (this.hour > other.hour) {
               return 1;
            }
         }

         if (!other.isMinuteUnspecified() && !this.isMinuteUnspecified()) {
            if (this.minute < other.minute) {
               return -1;
            }

            if (this.minute > other.minute) {
               return 1;
            }
         }

         if (!other.isSecondUnspecified() && !this.isSecondUnspecified()) {
            if (this.second < other.second) {
               return -1;
            }

            if (this.second > other.second) {
               return 1;
            }
         }

         if (!other.isHundredthUnspecified() && !this.isHundredthUnspecified()) {
            if (this.hundredth < other.hundredth) {
               return -1;
            }

            if (this.hundredth > other.hundredth) {
               return 1;
            }
         }

         return 0;
      } else if (((BObject)obj).getType() == BTime.TYPE) {
         BTime otherx = (BTime)obj;
         if (!this.isHourUnspecified()) {
            if (this.hour < otherx.getHour()) {
               return -1;
            }

            if (this.hour > otherx.getHour()) {
               return 1;
            }
         }

         if (!this.isMinuteUnspecified()) {
            if (this.minute < otherx.getMinute()) {
               return -1;
            }

            if (this.minute > otherx.getMinute()) {
               return 1;
            }
         }

         if (!this.isSecondUnspecified()) {
            if (this.second < otherx.getSecond()) {
               return -1;
            }

            if (this.second > otherx.getSecond()) {
               return 1;
            }
         }

         if (!this.isHundredthUnspecified()) {
            if (this.hundredth < otherx.getMillisecond() / 10) {
               return -1;
            }

            if (this.hundredth > otherx.getMillisecond() / 10) {
               return 1;
            }
         }

         return 0;
      } else {
         throw new IllegalArgumentException(obj.toString());
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

   public BTime toBTime() {
      return getBTime(this, true);
   }

   public static BBacnetTime fromString(String s) {
      try {
         return (BBacnetTime)DEFAULT.decodeFromString(s);
      } catch (Exception var2) {
         logger.log(Level.SEVERE, "BBacnetTime.fromString('" + s + "'): error parsing string!!", (Throwable)var2);
         return DEFAULT;
      }
   }

   public static BTime getBTime(BBacnetTime t) {
      return getBTime(t, true);
   }

   public static BTime getBTime(BBacnetTime t, boolean zero) {
      return BTime.make(
         t.isHourUnspecified() ? (zero ? 0 : 23) : t.getHour(),
         t.isMinuteUnspecified() ? (zero ? 0 : 59) : t.getMinute(),
         t.isSecondUnspecified() ? (zero ? 0 : 59) : t.getSecond(),
         t.isHundredthUnspecified() ? (zero ? 0 : 999) : t.getHundredth() * 10
      );
   }

   public Type getType() {
      return TYPE;
   }
}
