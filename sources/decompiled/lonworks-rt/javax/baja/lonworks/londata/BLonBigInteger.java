package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonBigInteger extends BLonPrimitive implements BINumeric {
   public static final BLonBigInteger DEFAULT = new BLonBigInteger(BigInteger.ZERO, false);
   public static final Type TYPE = Sys.loadType(BLonBigInteger.class);
   private BigInteger value;
   private boolean invalid;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonBigInteger make(BigInteger value, boolean b) {
      return new BLonBigInteger(value, b);
   }

   private BLonBigInteger(BigInteger value, boolean b) {
      this.value = value;
      this.invalid = b;
   }

   public BigInteger getBigInteger() {
      return this.value;
   }

   public long getLong() {
      return this.value.longValue();
   }

   public float getFloat() {
      return this.value.floatValue();
   }

   public int hashCode() {
      return this.value.intValue();
   }

   public boolean equals(Object obj) {
      return obj instanceof BLonBigInteger ? ((BLonBigInteger)obj).value == this.value : false;
   }

   public int compareTo(Object obj) {
      return this.value.compareTo((BigInteger)obj);
   }

   public String toString(Context context) {
      return this.encodeToString();
   }

   public void encode(DataOutput out) throws IOException {
      out.writeUTF(this.value.toString());
      out.writeBoolean(this.invalid);
   }

   public BObject decode(DataInput in) throws IOException {
      return make(new BigInteger(in.readUTF()), in.readBoolean());
   }

   public String encodeToString() {
      return encodeToString(this.value, this.invalid);
   }

   public static String encodeToString(BigInteger value, boolean invld) {
      return value.toString() + (invld ? "/invalid" : "");
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         int pos = s.indexOf("/");
         return pos < 0 ? make(new BigInteger(s), false) : make(new BigInteger(s.substring(0, pos)), true);
      } catch (Throwable var3) {
         throw new IOException("Invalid BigInteger: " + s);
      }
   }

   public static BigInteger bigIntegerFromString(String s) throws IOException {
      try {
         return new BigInteger(s);
      } catch (Throwable var2) {
         throw new IOException("Invalid BigInteger: " + s);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      BigInteger val = this.value;
      if (!this.invalid && e.getOffset() > 0.0F) {
         val = val.add(BigInteger.valueOf((long)e.getOffset()));
      }

      switch (e.getElemtype().getOrdinal()) {
         case 19:
            out.writeUnsigned64(val);
            return;
         default:
            throw new InvalidTypeException("Invalue id datatype for LonLong.");
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      switch (e.getElemtype().getOrdinal()) {
         case 19:
            BigInteger val = in.readUnsigned64();
            boolean invld = e.isInvalid(val.longValue());
            if (!invld && e.getOffset() > 0.0F) {
               val = val.subtract(BigInteger.valueOf((long)e.getOffset()));
            }

            if (this.value == val) {
               return this;
            }

            return make(val, invld);
         default:
            throw new InvalidTypeException("Invalid datatype for LonLong.");
      }
   }

   @Override
   public double getDataAsDouble() {
      return this.invalid ? Double.NaN : this.value.doubleValue();
   }

   @Override
   public BLonPrimitive makeFromDouble(double value, BLonElementQualifiers e) {
      if (Double.isNaN(value) && e != null && e.hasInvalidValue()) {
         return make(BigInteger.valueOf(e.getInvalidValueL()), true);
      } else {
         DecimalFormat df = new DecimalFormat("#0");
         FieldPosition pos = new FieldPosition(0);
         StringBuffer sb = df.format(value, new StringBuffer(), pos);
         String s = sb.toString();
         BigInteger bi = new BigInteger(s);
         return make(bi, false);
      }
   }

   @Override
   public final boolean isNumeric() {
      return true;
   }

   @Override
   public boolean getDataAsBoolean() {
      return this.value.longValue() != 0L;
   }

   @Override
   public BLonPrimitive makeFromBoolean(boolean v) {
      return make(v ? BigInteger.ONE : BigInteger.ZERO, false);
   }

   @Override
   public String getDataAsString() {
      return this.value.toString();
   }

   @Override
   public BLonPrimitive makeFromString(String stringValue) {
      BigInteger i = BigInteger.ZERO;

      try {
         i = new BigInteger(stringValue);
      } catch (Throwable var4) {
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
