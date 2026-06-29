package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonBoolean extends BLonPrimitive implements BIBoolean, BIEnum {
   public static final BLonBoolean TRUE = new BLonBoolean(true);
   public static final BLonBoolean FALSE = new BLonBoolean(false);
   public static final BLonBoolean DEFAULT = FALSE;
   public static final Type TYPE = Sys.loadType(BLonBoolean.class);
   boolean value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonBoolean make(boolean b) {
      return b ? TRUE : FALSE;
   }

   private BLonBoolean(boolean b) {
      this.value = b;
   }

   public boolean equals(Object obj) {
      return !(obj instanceof BLonBoolean) ? false : this.value == ((BLonBoolean)obj).value;
   }

   public String toString(Context context) {
      return this.value ? "true" : "false";
   }

   public String encodeToString() {
      return this.value ? "true" : "false";
   }

   public BObject decodeFromString(String s) throws IOException {
      if (s.equals("true")) {
         return TRUE;
      } else if (s.equals("false")) {
         return FALSE;
      } else {
         throw new IOException("Invalid boolean: " + s);
      }
   }

   public static boolean booleanFromString(String s) throws IOException {
      if (s.equals("true")) {
         return true;
      } else if (s.equals("false")) {
         return false;
      } else {
         throw new IOException("Invalid boolean: " + s);
      }
   }

   public void encode(DataOutput out) throws IOException {
      out.writeUTF(this.encodeToString());
   }

   public BObject decode(DataInput in) throws IOException {
      return this.decodeFromString(in.readUTF());
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      boolean val = this.value;
      switch (e.getElemtype().getOrdinal()) {
         case 6:
            out.writeBoolean(val);
            break;
         case 11:
            out.writeBooleanBit(val, e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         default:
            throw new InvalidTypeException("Invalid datatype for BLonBoolean.");
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      boolean val;
      switch (e.getElemtype().getOrdinal()) {
         case 6:
            val = in.readBoolean();
            break;
         case 11:
            val = in.readBooleanBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         default:
            throw new InvalidTypeException("Invalid datatype for BLonBoolean.");
      }

      return make(val);
   }

   @Override
   public double getDataAsDouble() {
      return this.value ? 1.0 : 0.0;
   }

   @Override
   public BLonPrimitive makeFromDouble(double floatValue, BLonElementQualifiers e) {
      return make(floatValue > 0.0);
   }

   @Override
   public boolean getDataAsBoolean() {
      return this.value;
   }

   @Override
   public BLonPrimitive makeFromBoolean(boolean boolValue) {
      return make(boolValue);
   }

   @Override
   public String getDataAsString() {
      return this.value ? "true" : "false";
   }

   @Override
   public BLonPrimitive makeFromString(String stringValue) {
      boolean b = false;

      try {
         b = Boolean.valueOf(stringValue);
      } catch (Throwable var4) {
         return null;
      }

      return make(b);
   }

   public boolean getBoolean() {
      return this.getDataAsBoolean();
   }

   public BFacets getBooleanFacets() {
      return BFacets.NULL;
   }

   public BEnum getEnum() {
      return this.value ? BBoolean.TRUE : BBoolean.FALSE;
   }

   public BFacets getEnumFacets() {
      return BFacets.makeBoolean();
   }
}
