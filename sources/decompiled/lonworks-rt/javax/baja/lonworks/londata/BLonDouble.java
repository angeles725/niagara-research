package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
public final class BLonDouble extends BLonPrimitive implements BINumeric {
   public static final BLonDouble DEFAULT = new BLonDouble(0.0);
   public static final Type TYPE = Sys.loadType(BLonDouble.class);
   public static final BLonDouble POSITIVE_INFINITY = new BLonDouble(Double.POSITIVE_INFINITY);
   public static final BLonDouble NEGATIVE_INFINITY = new BLonDouble(Double.NEGATIVE_INFINITY);
   public static final BLonDouble NaN = new BLonDouble(Double.NaN);
   private double value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonDouble make(double value) {
      if (value == 0.0) {
         return DEFAULT;
      } else if (value == Double.NEGATIVE_INFINITY) {
         return NEGATIVE_INFINITY;
      } else if (value == Double.POSITIVE_INFINITY) {
         return POSITIVE_INFINITY;
      } else {
         return Double.isNaN(value) ? NaN : new BLonDouble(value);
      }
   }

   private BLonDouble(double value) {
      this.value = value;
   }

   public double getDouble() {
      return this.value;
   }

   public int getInt() {
      return (int)this.value;
   }

   public static String toString(double value, Context context) {
      BUnit units = null;
      if (context != null) {
         units = (BUnit)context.getFacet("units");
         if (units != null && units.isNull()) {
            units = null;
         }
      }

      String s;
      if (value == Double.POSITIVE_INFINITY) {
         s = "+inf";
      } else if (value == Double.NEGATIVE_INFINITY) {
         s = "-inf";
      } else if (Double.isNaN(value)) {
         s = "nan";
      } else {
         s = Double.toString(value);
      }

      if (units != null) {
         s = s + ' ' + units.getSymbol();
      }

      return s;
   }

   public int hashCode() {
      return (int)(Double.doubleToLongBits(this.value) & -1L);
   }

   public boolean equals(Object obj) {
      if (obj instanceof BLonDouble) {
         double x = ((BLonDouble)obj).value;
         if (x == this.value) {
            return true;
         }

         if (Double.isNaN(x) && Double.isNaN(this.value)) {
            return true;
         }
      }

      return false;
   }

   public static boolean equals(double a, double b) {
      return a == b ? true : Double.isNaN(a) && Double.isNaN(b);
   }

   public int compareTo(Object obj) {
      double a = this.value;
      double b = ((BLonDouble)obj).value;
      if (equals(a, b)) {
         return 0;
      } else {
         return a < b ? -1 : 1;
      }
   }

   public String toString(Context context) {
      return toString(this.value, context);
   }

   public void encode(DataOutput out) throws IOException {
      out.writeDouble(this.value);
   }

   public BObject decode(DataInput in) throws IOException {
      double f = in.readDouble();
      return equals(this.value, f) ? this : new BLonDouble(f);
   }

   public String encodeToString() throws IOException {
      return encodeToString(this.value);
   }

   public static String encodeToString(double value) throws IOException {
      if (value == Double.POSITIVE_INFINITY) {
         return "+inf";
      } else if (value == Double.NEGATIVE_INFINITY) {
         return "-inf";
      } else {
         return Double.isNaN(value) ? "nan" : String.valueOf(value);
      }
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         if (s.equals("+inf")) {
            return POSITIVE_INFINITY;
         } else if (s.equals("-inf")) {
            return NEGATIVE_INFINITY;
         } else {
            return s.equals("nan") ? NaN : new BLonDouble(Double.parseDouble(s));
         }
      } catch (Throwable var3) {
         throw new IOException("Invalid double: " + s);
      }
   }

   public static double doubleFromString(String s) throws IOException {
      try {
         if (s.equalsIgnoreCase("+inf")) {
            return Double.POSITIVE_INFINITY;
         } else if (s.equalsIgnoreCase("-inf")) {
            return Double.NEGATIVE_INFINITY;
         } else {
            return s.equalsIgnoreCase("nan") ? Double.NaN : Double.parseDouble(s);
         }
      } catch (Throwable var2) {
         throw new IOException("Invalid double: " + s);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      double val = this.value;
      boolean invalid = false;
      if (e.hasInvalidValue() && Double.isNaN(val)) {
         val = e.getInvalidValue();
         invalid = true;
      } else {
         if (e.getOffset() != 0.0F) {
            val += e.getOffset();
         }

         if (e.getResolution() != 1.0F) {
            val /= e.getResolution();
         }
      }

      switch (e.getElemtype().getOrdinal()) {
         case 0:
            out.writeUnsigned8((int)val);
            break;
         case 1:
            out.writeSigned8((int)val);
            break;
         case 2:
            out.writeUnsigned8((int)val);
            break;
         case 3:
            out.writeSigned16((int)val);
            break;
         case 4:
            out.writeUnsigned16((int)val);
            break;
         case 5:
            out.writeSigned32((int)val);
            break;
         case 6:
         case 7:
         case 9:
         case 10:
         case 11:
         case 14:
         case 15:
         default:
            throw new InvalidTypeException("Invalid datatype for LonDouble " + e.getElemtype());
         case 8:
            out.writeFloat((float)val);
            break;
         case 12:
            out.writeBit((int)val, e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 13:
            out.writeSignedBit((int)val, e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 16:
            if (invalid) {
               out.writeUnsigned32(e.getInvalidValueL());
            } else {
               out.writeUnsigned32((long)val);
            }
            break;
         case 17:
            out.writeDouble(val);
            break;
         case 18:
            if (invalid) {
               out.writeSigned64(e.getInvalidValueL());
            } else {
               out.writeSigned64((long)val);
            }
            break;
         case 19:
            if (invalid) {
               out.writeUnsigned64(new BigInteger(Long.toString(e.getInvalidValueL())));
            } else {
               out.writeUnsigned64(new BigInteger(Long.toString((long)val)));
            }
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      double val;
      switch (e.getElemtype().getOrdinal()) {
         case 0:
            val = in.readUnsigned8();
            break;
         case 1:
            val = in.readSigned8();
            break;
         case 2:
            val = in.readUnsigned8();
            break;
         case 3:
            val = in.readSigned16();
            break;
         case 4:
            val = in.readUnsigned16();
            break;
         case 5:
            val = in.readSigned32();
            break;
         case 6:
         case 7:
         case 9:
         case 10:
         case 11:
         case 14:
         case 15:
         default:
            throw new InvalidTypeException("Invalid datatype for LonDouble.");
         case 8:
            val = in.readFloat();
            break;
         case 12:
            val = in.readBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 13:
            val = in.readSignedBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 16:
            long lval = in.readUnsigned32();
            if (e.isInvalid(lval)) {
               return make(Double.NaN);
            }

            val = lval;
            break;
         case 17:
            val = in.readDouble();
            break;
         case 18:
            long lval = in.readSigned64();
            if (e.isInvalid(lval)) {
               return make(Double.NaN);
            }

            val = lval;
            break;
         case 19:
            val = in.readUnsigned64().doubleValue();
      }

      if (e.isInvalid(val)) {
         return make(Double.NaN);
      } else {
         if (e.getResolution() != 1.0F) {
            val *= e.getResolution();
         }

         if (e.getOffset() != 0.0F) {
            val -= e.getOffset();
         }

         return val == this.value ? this : make(val);
      }
   }

   @Override
   public double getDataAsDouble() {
      return this.value;
   }

   @Override
   public BLonPrimitive makeFromDouble(double v, BLonElementQualifiers e) {
      return make(v);
   }

   @Override
   public final boolean isNumeric() {
      return true;
   }

   @Override
   public boolean getDataAsBoolean() {
      return this.value > 0.0;
   }

   @Override
   public BLonPrimitive makeFromBoolean(boolean v) {
      return make(v ? 1.0F : 0.0F);
   }

   @Override
   public String getDataAsString() {
      return Double.toString(this.value);
   }

   @Override
   public BLonPrimitive makeFromString(String v) {
      double f = Double.NaN;

      try {
         f = Double.valueOf(v);
      } catch (Throwable var5) {
      }

      return make(f);
   }

   @Override
   public BEnum getDataAsEnum(BEnum en) {
      try {
         BEnumRange enRng = en.getRange();
         return enRng.get((int)this.value);
      } catch (Throwable var3) {
         return null;
      }
   }

   @Override
   public BLonPrimitive makeFromEnum(BEnum v) {
      return new BLonDouble(v.getOrdinal());
   }

   public int getIntValue() {
      return (int)this.value;
   }

   public double getNumeric() {
      return this.value;
   }

   public BFacets getNumericFacets() {
      return BFacets.NULL;
   }
}
