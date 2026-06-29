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
public final class BLonInteger extends BLonPrimitive implements BINumeric {
   public static final BLonInteger DEFAULT = new BLonInteger(0);
   public static final Type TYPE = Sys.loadType(BLonInteger.class);
   private int value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonInteger make(int value) {
      return new BLonInteger(value);
   }

   private BLonInteger(int value) {
      this.value = value;
   }

   public int getInt() {
      return this.value;
   }

   public float getFloat() {
      return this.value;
   }

   public int hashCode() {
      return this.value;
   }

   public boolean equals(Object obj) {
      return obj instanceof BLonInteger ? ((BLonInteger)obj).value == this.value : false;
   }

   public int compareTo(Object obj) {
      int a = this.value;
      int b = ((BNumber)obj).getInt();
      if (a == b) {
         return 0;
      } else {
         return a < b ? -1 : 1;
      }
   }

   public String toString(Context context) {
      return String.valueOf(this.value);
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.value);
   }

   public BObject decode(DataInput in) throws IOException {
      return make(in.readInt());
   }

   public String encodeToString() throws IOException {
      return String.valueOf(this.value);
   }

   public static String encodeToString(int value) {
      return String.valueOf(value);
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         return make(Integer.parseInt(s));
      } catch (Throwable var3) {
         throw new IOException("Invalid integer: " + s);
      }
   }

   public static int intFromString(String s) throws IOException {
      try {
         return Integer.parseInt(s);
      } catch (Throwable var2) {
         throw new IOException("Invalid integer: " + s);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      switch (e.getElemtype().getOrdinal()) {
         case 0:
            out.writeUnsigned8(this.value);
            break;
         case 1:
            out.writeSigned8(this.value);
            break;
         case 2:
            out.writeUnsigned8(this.value);
            break;
         case 3:
            out.writeSigned16(this.value);
            break;
         case 4:
            out.writeUnsigned16(this.value);
            break;
         case 5:
            out.writeSigned32(this.value);
            break;
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         default:
            throw new InvalidTypeException("Invalueid datatype for LonInteger.");
         case 12:
            out.writeBit(this.value, e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 13:
            out.writeSignedBit(this.value, e.getByteOffset(), e.getBitOffset(), e.getSize());
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      int val;
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
         case 8:
         case 9:
         case 10:
         case 11:
         default:
            throw new InvalidTypeException("Invalid datatype for LonInteger.");
         case 12:
            val = in.readBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 13:
            val = in.readSignedBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
      }

      return this.value == val ? this : make(val);
   }

   @Override
   public double getDataAsDouble() {
      return this.value;
   }

   @Override
   public BLonPrimitive makeFromDouble(double value, BLonElementQualifiers e) {
      return make((int)value);
   }

   @Override
   public final boolean isNumeric() {
      return true;
   }

   @Override
   public boolean getDataAsBoolean() {
      return this.value != 0;
   }

   @Override
   public BLonPrimitive makeFromBoolean(boolean v) {
      return make(v ? 1 : 0);
   }

   @Override
   public String getDataAsString() {
      return Integer.toString(this.value);
   }

   @Override
   public BLonPrimitive makeFromString(String stringValue) {
      int i = 0;

      try {
         i = Integer.valueOf(stringValue);
      } catch (Throwable var4) {
         return null;
      }

      return make(i);
   }

   public double getNumeric() {
      return this.getDataAsDouble();
   }

   public BFacets getNumericFacets() {
      return BFacets.NULL;
   }
}
