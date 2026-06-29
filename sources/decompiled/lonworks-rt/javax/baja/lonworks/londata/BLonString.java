package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonString extends BLonPrimitive {
   public static final BLonString DEFAULT = new BLonString("");
   public static final Type TYPE = Sys.loadType(BLonString.class);
   private String value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonString make(String value) {
      return new BLonString(value);
   }

   private BLonString(String value) {
      if (value == null) {
         value = "";
      } else {
         this.value = value;
      }
   }

   public String getString() {
      return this.value;
   }

   public int hashCode() {
      return this.value.hashCode();
   }

   public boolean equals(Object obj) {
      return obj instanceof BLonString ? this.value.equals(((BLonString)obj).value) : false;
   }

   public int compareTo(Object obj) {
      return this.value.compareTo(((BLonString)obj).getString());
   }

   public String toString(Context context) {
      return this.value;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeUTF(this.value);
   }

   public BObject decode(DataInput in) throws IOException {
      String s = in.readUTF();
      return s.equals("") ? DEFAULT : new BLonString(s);
   }

   public String encodeToString() throws IOException {
      return this.value;
   }

   public BObject decodeFromString(String s) throws IOException {
      return s.equals("") ? DEFAULT : new BLonString(s);
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      switch (e.getElemtype().getOrdinal()) {
         case 14:
            out.writeCharArray(this.value, e.getLength());
            return;
         default:
            throw new InvalidTypeException("Invalid datatype for LonString.");
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      switch (e.getElemtype().getOrdinal()) {
         case 14:
            String val = in.readCharArray(e.getLength());
            if (this.value.equals(val)) {
               return this;
            }

            return make(val);
         default:
            throw new InvalidTypeException("Invalid datatype for LonString.");
      }
   }

   @Override
   public double getDataAsDouble() {
      double f = Double.NaN;

      try {
         f = Double.valueOf(this.value);
      } catch (Throwable var4) {
      }

      return f;
   }

   @Override
   public BLonPrimitive makeFromDouble(double value, BLonElementQualifiers e) {
      return make(Double.toString(value));
   }

   @Override
   public boolean getDataAsBoolean() {
      return Boolean.valueOf(this.value);
   }

   @Override
   public BLonPrimitive makeFromBoolean(boolean boolValue) {
      return make(boolValue ? "true" : "false");
   }

   @Override
   public String getDataAsString() {
      return this.value;
   }

   @Override
   public BLonPrimitive makeFromString(String stringValue) {
      return make(stringValue);
   }

   @Override
   public BEnum getDataAsEnum(BEnum en) {
      BEnumRange enRng = en.getRange();
      if (!enRng.isTag(this.value)) {
         String s = "";

         try {
            s = en.encodeToString();
         } catch (Throwable var5) {
         }

         throw new BajaRuntimeException("Cannot map tag (" + this.value + ") to enum.\n" + s);
      } else {
         return enRng.get(this.value);
      }
   }

   @Override
   public BLonPrimitive makeFromEnum(BEnum v) {
      return new BLonString(v.getTag());
   }
}
