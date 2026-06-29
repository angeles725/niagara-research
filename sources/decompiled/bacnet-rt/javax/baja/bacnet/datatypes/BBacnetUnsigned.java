package javax.baja.bacnet.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BIComparable;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BBacnetUnsigned extends BSimple implements BIComparable {
   private static final BBacnetUnsigned ZERO = new BBacnetUnsigned(0L);
   private static final BBacnetUnsigned ONE = new BBacnetUnsigned(1L);
   private static final BBacnetUnsigned TWO = new BBacnetUnsigned(2L);
   public static final long MAX_UNSIGNED_VALUE = 4294967295L;
   public static final long MIN_UNSIGNED_VALUE = 0L;
   public static final long MAX_UNSIGNED16_VALUE = 65535L;
   public static final BBacnetUnsigned DEFAULT = ZERO;
   public static final BBacnetUnsigned MAX_UNSIGNED = new BBacnetUnsigned(4294967295L);
   public static final Type TYPE = Sys.loadType(BBacnetUnsigned.class);
   private long value;

   public BBacnetUnsigned(long value) {
      if (value > 4294967295L) {
         throw new IllegalArgumentException("" + value);
      } else {
         this.value = value;
      }
   }

   public static BBacnetUnsigned make(long value) {
      switch ((int)value) {
         case 0:
            return ZERO;
         case 1:
            return ONE;
         case 2:
            return TWO;
         default:
            return new BBacnetUnsigned(value);
      }
   }

   public int compareTo(Object obj) {
      int a = (int)this.value;
      int b = ((BNumber)obj).getInt();
      if (a == b) {
         return 0;
      } else {
         return a < b ? -1 : 1;
      }
   }

   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else {
         return obj instanceof BBacnetUnsigned ? ((BBacnetUnsigned)obj).value == this.value : false;
      }
   }

   public String toString(Context context) {
      if (context != null) {
         BEnumRange r = (BEnumRange)context.getFacet("range");
         if (r != null) {
            return SlotPath.unescape(r.getTag((int)this.value));
         }
      }

      return String.valueOf(this.value);
   }

   public int hashCode() {
      return (int)this.value;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeLong(this.value);
   }

   public BObject decode(DataInput in) throws IOException {
      return make(in.readLong());
   }

   public String encodeToString() throws IOException {
      return String.valueOf(this.value);
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         return make(Long.parseLong(s));
      } catch (Exception var3) {
         throw new IOException("Invalid unsigned: " + s);
      }
   }

   public int getInt() {
      return (int)this.value;
   }

   public long getLong() {
      return this.value;
   }

   public long getUnsigned() {
      return this.value;
   }

   public Type getType() {
      return TYPE;
   }
}
