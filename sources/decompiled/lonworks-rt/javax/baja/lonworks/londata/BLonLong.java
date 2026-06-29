package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonLong extends BLonPrimitive implements BINumeric {
   public static final BLonLong DEFAULT = new BLonLong(0L, false);
   public static final Type TYPE = Sys.loadType(BLonLong.class);
   private long value;
   private boolean invalid = false;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonLong make(long value, boolean b) {
      return new BLonLong(value, b);
   }

   private BLonLong(long value, boolean b) {
      this.value = value;
      this.invalid = b;
   }

   public long getLong() {
      return this.value;
   }

   public float getFloat() {
      return (float)this.value;
   }

   public int hashCode() {
      return (int)this.value;
   }

   public boolean equals(Object obj) {
      return obj instanceof BLonLong ? ((BLonLong)obj).value == this.value : false;
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
      return this.encodeToString();
   }

   public void encode(DataOutput out) throws IOException {
      out.writeLong(this.value);
      out.writeBoolean(this.invalid);
   }

   public BObject decode(DataInput in) throws IOException {
      return make(in.readLong(), in.readBoolean());
   }

   public String encodeToString() {
      return encodeToString(this.value, this.invalid);
   }

   public static String encodeToString(long val, boolean invld) {
      return Long.toString(val) + (invld ? "/invalid" : "");
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         int pos = s.indexOf("/");
         return pos < 0 ? make(Long.parseLong(s), false) : make(Long.parseLong(s.substring(0, pos)), true);
      } catch (Throwable var3) {
         throw new IOException("Invalid long: " + s);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      long val = this.value;
      if ((!e.hasInvalidValue() || this.value != e.getInvalidValueL()) && e.getOffset() > 0.0F) {
         val = (long)((float)val + e.getOffset());
      }

      switch (e.getElemtype().getOrdinal()) {
         case 5:
            out.writeSigned32((int)val);
            break;
         case 16:
            out.writeUnsigned32(val);
            break;
         case 18:
            out.writeSigned64(val);
            break;
         default:
            throw new InvalidTypeException("Invalue id datatype for LonLong.");
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      long val;
      switch (e.getElemtype().getOrdinal()) {
         case 5:
            val = in.readSigned32();
            break;
         case 16:
            val = in.readUnsigned32();
            break;
         case 18:
            val = in.readSigned64();
            break;
         default:
            throw new InvalidTypeException("Invalid datatype for LonLong.");
      }

      boolean invld = e.isInvalid(val);
      if (!invld && e.getOffset() > 0.0F) {
         val = (long)((float)val - e.getOffset());
      }

      return this.value == val ? this : make(val, invld);
   }

   @Override
   public double getDataAsDouble() {
      return this.invalid ? Double.NaN : this.value;
   }

   @Override
   public BLonPrimitive makeFromDouble(double value, BLonElementQualifiers e) {
      return Double.isNaN(value) && e != null && e.hasInvalidValue() ? make(e.getInvalidValueL(), true) : make((long)value, false);
   }

   @Override
   public final boolean isNumeric() {
      return true;
   }

   @Override
   public boolean getDataAsBoolean() {
      return this.value != 0L;
   }

   @Override
   public BLonPrimitive makeFromBoolean(boolean v) {
      return make(v ? 1L : 0L, false);
   }

   @Override
   public String getDataAsString() {
      return Long.toString(this.value);
   }

   @Override
   public BLonPrimitive makeFromString(String stringValue) {
      long i = 0L;

      try {
         i = Long.parseLong(stringValue);
      } catch (Throwable var5) {
         return null;
      }

      return make(i, false);
   }

   public double getNumeric() {
      return this.getDataAsDouble();
   }

   public BFacets getNumericFacets() {
      return BFacets.NULL;
   }
}
