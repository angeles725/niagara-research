package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
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
public final class BLonFloat extends BLonPrimitive implements BINumeric {
   public static final BLonFloat DEFAULT = new BLonFloat(0.0F);
   public static final Type TYPE = Sys.loadType(BLonFloat.class);
   public static final BLonFloat POSITIVE_INFINITY = new BLonFloat(Float.POSITIVE_INFINITY);
   public static final BLonFloat NEGATIVE_INFINITY = new BLonFloat(Float.NEGATIVE_INFINITY);
   public static final BLonFloat NaN = new BLonFloat(Float.NaN);
   private float value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonFloat make(float value) {
      if (value == 0.0F) {
         return DEFAULT;
      } else if (value == Float.NEGATIVE_INFINITY) {
         return NEGATIVE_INFINITY;
      } else if (value == Float.POSITIVE_INFINITY) {
         return POSITIVE_INFINITY;
      } else {
         return Float.isNaN(value) ? NaN : new BLonFloat(value);
      }
   }

   private BLonFloat(float value) {
      this.value = value;
   }

   public float getFloat() {
      return this.value;
   }

   public int getInt() {
      return (int)this.value;
   }

   public static String toString(float value, Context context) {
      BUnit units = null;
      if (context != null) {
         units = (BUnit)context.getFacet("units");
         if (units != null && units.isNull()) {
            units = null;
         }
      }

      String s;
      if (value == Float.POSITIVE_INFINITY) {
         s = "+inf";
      } else if (value == Float.NEGATIVE_INFINITY) {
         s = "-inf";
      } else if (Float.isNaN(value)) {
         s = "nan";
      } else {
         s = Float.toString(value);
      }

      if (units != null) {
         s = s + ' ' + units.getSymbol();
      }

      return s;
   }

   public int hashCode() {
      return Float.floatToIntBits(this.value);
   }

   public boolean equals(Object obj) {
      if (obj instanceof BLonFloat) {
         float x = ((BLonFloat)obj).value;
         if (x == this.value) {
            return true;
         }

         if (Float.isNaN(x) && Float.isNaN(this.value)) {
            return true;
         }
      }

      return false;
   }

   public static boolean equals(float a, float b) {
      return a == b ? true : Float.isNaN(a) && Float.isNaN(b);
   }

   public int compareTo(Object obj) {
      float a = this.value;
      float b = ((BLonFloat)obj).value;
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
      out.writeFloat(this.value);
   }

   public BObject decode(DataInput in) throws IOException {
      float f = in.readFloat();
      return equals(this.value, f) ? this : new BLonFloat(f);
   }

   public String encodeToString() throws IOException {
      return encodeToString(this.value);
   }

   public static String encodeToString(float value) throws IOException {
      if (value == Float.POSITIVE_INFINITY) {
         return "+inf";
      } else if (value == Float.NEGATIVE_INFINITY) {
         return "-inf";
      } else {
         return Float.isNaN(value) ? "nan" : String.valueOf((double)value);
      }
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         if (s.equals("+inf")) {
            return POSITIVE_INFINITY;
         } else if (s.equals("-inf")) {
            return NEGATIVE_INFINITY;
         } else {
            return s.equals("nan") ? NaN : new BLonFloat((float)Double.parseDouble(s));
         }
      } catch (Throwable var3) {
         throw new IOException("Invalid float: " + s);
      }
   }

   public static float floatFromString(String s) throws IOException {
      try {
         if (s.equalsIgnoreCase("+inf")) {
            return Float.POSITIVE_INFINITY;
         } else if (s.equalsIgnoreCase("-inf")) {
            return Float.NEGATIVE_INFINITY;
         } else {
            return s.equalsIgnoreCase("nan") ? Float.NaN : (float)Double.parseDouble(s);
         }
      } catch (Throwable var2) {
         throw new IOException("Invalid float: " + s);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      float val = this.value;
      if (e.hasInvalidValue() && Float.isNaN(val)) {
         val = e.getInvalidValue();
      } else {
         if (e.getOffset() != 0.0F) {
            val += e.getOffset();
         }

         if (e.getResolution() != 1.0F) {
            val /= e.getResolution();
         }
      }

      int ival = (int)val;
      switch (e.getElemtype().getOrdinal()) {
         case 0:
            out.writeUnsigned8(ival);
            break;
         case 1:
            out.writeSigned8(ival);
            break;
         case 2:
            out.writeUnsigned8(ival);
            break;
         case 3:
            out.writeSigned16(ival);
            break;
         case 4:
            out.writeUnsigned16(ival);
            break;
         case 5:
            out.writeSigned32(ival);
            break;
         case 6:
         case 7:
         case 9:
         case 10:
         case 11:
         default:
            throw new InvalidTypeException("Invalid datatype for LonFloat " + e.getElemtype());
         case 8:
            out.writeFloat(val);
            break;
         case 12:
            out.writeBit(ival, e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 13:
            out.writeSignedBit(ival, e.getByteOffset(), e.getBitOffset(), e.getSize());
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
         default:
            throw new InvalidTypeException("Invalid datatype for LonFloat.");
         case 8:
            val = in.readFloat();
            break;
         case 12:
            val = in.readBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 13:
            val = in.readSignedBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
      }

      if (e.hasInvalidValue() && val == e.getInvalidValueL()) {
         return make(Float.NaN);
      } else {
         if (e.getResolution() != 1.0F) {
            val *= e.getResolution();
         }

         if (e.getOffset() != 0.0F) {
            val -= e.getOffset();
         }

         return val == this.value ? this : make((float)val);
      }
   }

   @Override
   public double getDataAsDouble() {
      return this.value;
   }

   @Override
   public BLonPrimitive makeFromDouble(double v, BLonElementQualifiers e) {
      return make((float)v);
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
      return String.valueOf((double)this.value);
   }

   @Override
   public BLonPrimitive makeFromString(String v) {
      float f = Float.NaN;

      try {
         f = Float.valueOf(v);
      } catch (Throwable var4) {
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
      return new BLonFloat(v.getOrdinal());
   }

   public int getIntValue() {
      return (int)this.value;
   }

   public double getNumeric() {
      return this.getDataAsDouble();
   }

   public BFacets getNumericFacets() {
      return BFacets.NULL;
   }
}
