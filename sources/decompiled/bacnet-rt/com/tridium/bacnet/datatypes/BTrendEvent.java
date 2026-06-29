package com.tridium.bacnet.datatypes;

import com.tridium.bacnet.asn.NErrorType;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetLogRecord;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BTrendEvent extends BSimple {
   public static final long NO_EVENT = 0L;
   public static final long LOG_STATUS_EVENT = 4294967296L;
   public static final long FAILURE_EVENT = 8589934592L;
   public static final long TIME_CHANGE_EVENT = 17179869184L;
   public static final long INVALID_EVENT = 34359738368L;
   private static final long LOG_DISABLED_BIT = 1L;
   private static final long BUFFER_PURGED_BIT = 2L;
   private static final long LOG_INTERRUPTED_BIT = 4L;
   private static final BTrendEvent ZERO = new BTrendEvent(0L);
   public static final BTrendEvent DEFAULT = ZERO;
   public static final Type TYPE = Sys.loadType(BTrendEvent.class);
   private static final boolean[] LS_ENABLE = new boolean[]{false, false, false};
   private static final boolean[] LS_DISABLE = new boolean[]{true, false, false};
   private static final boolean[] LS_ENABLE_PURGE = new boolean[]{false, true, false};
   private static final boolean[] LS_DISABLE_PURGE = new boolean[]{true, true, false};
   private static final boolean[] LS_INTERRUPTED = new boolean[]{false, false, true};
   public static final BTrendEvent LOG_STATUS_ENABLED = makeLogStatus(BBacnetBitString.make(LS_ENABLE));
   public static final BTrendEvent LOG_STATUS_DISABLED = makeLogStatus(BBacnetBitString.make(LS_DISABLE));
   public static final BTrendEvent LOG_STATUS_ENABLED_BUFFER_PURGED = makeLogStatus(BBacnetBitString.make(LS_ENABLE_PURGE));
   public static final BTrendEvent LOG_STATUS_DISABLED_BUFFER_PURGED = makeLogStatus(BBacnetBitString.make(LS_DISABLE_PURGE));
   public static final BTrendEvent LOG_STATUS_INTERRUPTED = makeLogStatus(BBacnetBitString.make(LS_INTERRUPTED));
   private long value;

   public static BTrendEvent make() {
      return make(0L);
   }

   public static BTrendEvent makeLogStatus(BBacnetBitString val) {
      long value = 4294967296L;
      if (val == null) {
         return make(value | 34359738368L);
      } else {
         boolean[] bits = val.getBits();
         if (bits.length < 2) {
            value |= 34359738368L;
         }

         for (int i = 0; i < bits.length; i++) {
            if (i >= 32) {
               value |= 34359738368L;
               break;
            }

            if (bits[i]) {
               value |= 1L << i;
            }
         }

         return make(value);
      }
   }

   public static BTrendEvent makeFailure(NErrorType val) {
      long value = 8589934592L;
      if (val == null) {
         return make(value | 34359738368L);
      } else {
         long errorClass = 65535 & val.getErrorClass();
         long errorCode = 65535 & val.getErrorCode();
         long temp = 0L;
         temp |= errorClass;
         temp <<= 16;
         temp |= errorCode;
         value |= temp;
         return make(value);
      }
   }

   public static BTrendEvent makeTimeChange(long val) {
      long value = 17179869184L;
      long seconds = 4294967295L & val;
      value |= seconds;
      return make(value);
   }

   public static BTrendEvent make(long value) {
      return value == 0L ? ZERO : new BTrendEvent(value);
   }

   public static BTrendEvent make(String value) {
      return make(decode(value));
   }

   private BTrendEvent(long value) {
      this.value = value;
   }

   public boolean getBit(long event) {
      return (this.value & event) != 0L;
   }

   public static boolean getBit(long event, long val) {
      return (val & event) != 0L;
   }

   public long getLong() {
      return this.value;
   }

   public int getInt() {
      return (int)this.value;
   }

   public float getFloat() {
      return (float)this.value;
   }

   public double getDouble() {
      return this.value;
   }

   public int hashCode() {
      return (int)(this.value ^ this.value >>> 32);
   }

   public boolean equals(Object obj) {
      return obj instanceof BTrendEvent ? ((BTrendEvent)obj).value == this.value : false;
   }

   public int compareTo(Object obj) {
      long a = this.value;
      long b = ((BNumber)obj).getLong();
      if (a == b) {
         return 0;
      } else {
         return a < b ? -1 : 1;
      }
   }

   public String toString(Context context) {
      return encode(this.value);
   }

   public void encode(DataOutput out) throws IOException {
      out.writeLong(this.value);
   }

   public BObject decode(DataInput in) throws IOException {
      return make(in.readLong());
   }

   public String encodeToString() throws IOException {
      return encode(this.value);
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         return make(decode(s));
      } catch (RuntimeException var3) {
         throw new IOException("Invalid BTrendEvent: " + s);
      }
   }

   public static long decode(String s) {
      if (s == null) {
         return 0L;
      } else {
         int endSearch = s.lastIndexOf(58);
         return endSearch <= 0 ? 0L : Long.parseLong(s.substring(endSearch + 1));
      }
   }

   public static String encode(long x) {
      if (x == 0L) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();
         int eventType = 0;
         if (getBit(34359738368L, x)) {
            sb.append(BBacnetLogRecord.INVALID_STRING + " ");
            eventType = -1;
         }

         if (getBit(4294967296L, x)) {
            sb.append(BBacnetLogRecord.LOG_STATUS_STRING + " ");
            if (eventType >= 0) {
               eventType = 1;
            } else {
               sb.append(BBacnetLogRecord.EVENT_STRING);
            }
         } else if (getBit(8589934592L, x)) {
            sb.append(BBacnetLogRecord.FAILURE_STRING + " ");
            if (eventType >= 0) {
               eventType = 2;
            } else {
               sb.append(BBacnetLogRecord.EVENT_STRING);
            }
         } else if (getBit(17179869184L, x)) {
            sb.append(BBacnetLogRecord.TIME_CHANGE_STRING + " ");
            if (eventType >= 0) {
               eventType = 3;
            } else {
               sb.append(BBacnetLogRecord.EVENT_STRING);
            }
         } else {
            sb.append(BBacnetLogRecord.UNKNOWN_STRING + BBacnetLogRecord.EVENT_STRING);
         }

         switch (eventType) {
            case 1:
               if (getBit(1L, x)) {
                  sb.append(BBacnetLogRecord.LOG_DISABLED_STRING + " ");
               } else {
                  sb.append(BBacnetLogRecord.LOG_ENABLED_STRING + " ");
               }

               if (getBit(2L, x)) {
                  sb.append(BBacnetLogRecord.LOG_BUFFER_PURGED_STRING + " ");
               }

               if (getBit(4L, x)) {
                  sb.append(BBacnetLogRecord.LOG_INTERRUPTED_STRING);
               }
               break;
            case 2:
               int errorCode = (int)(65535L & x);
               int errorClass = (int)(-65536L & x) >> 16;
               sb.append("(" + BBacnetErrorClass.tag(errorClass) + ", " + BBacnetErrorCode.tag(errorCode) + ")");
               break;
            case 3:
               int seconds = (int)(-1L & x);
               sb.append(seconds + " " + BBacnetLogRecord.SECONDS_STRING);
         }

         sb.append(':' + String.valueOf(x));
         return sb.toString();
      }
   }

   public boolean isLogStatus() {
      return (this.value & 4294967296L) != 0L;
   }

   public boolean isFailure() {
      return (this.value & 8589934592L) != 0L;
   }

   public boolean isTimeChange() {
      return (this.value & 17179869184L) != 0L;
   }

   public BBacnetBitString getLogStatus() {
      boolean[] logStatusFlags = new boolean[]{this.getBit(1L), this.getBit(2L), this.getBit(4L)};
      return BBacnetBitString.make(logStatusFlags);
   }

   public boolean isLogDisabled() {
      return this.isLogStatus() && this.getBit(1L);
   }

   public NErrorType getFailure() {
      int errorCode = (int)(65535L & this.value);
      int errorClass = (int)(-65536L & this.value) >> 16;
      return new NErrorType(errorClass, errorCode);
   }

   public float getTimeChange() {
      return (float)(-1L & this.value);
   }

   public static BBacnetBitString getLogStatus(long event) {
      boolean[] logStatusFlags = new boolean[]{(event & 1L) != 0L, (event & 2L) != 0L, (event & 4L) != 0L};
      return BBacnetBitString.make(logStatusFlags);
   }

   public static NErrorType getFailure(long event) {
      int errorCode = (int)(65535L & event);
      int errorClass = (int)(-65536L & event) >> 16;
      return new NErrorType(errorClass, errorCode);
   }

   public static float getTimeChange(long event) {
      return (float)(4294967295L & event);
   }

   public Type getType() {
      return TYPE;
   }
}
