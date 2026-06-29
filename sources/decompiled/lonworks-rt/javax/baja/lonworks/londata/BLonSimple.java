package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonSimple extends BLonPrimitive {
   public static final BLonSimple DEFAULT = new BLonSimple(null);
   public static final Type TYPE = Sys.loadType(BLonSimple.class);
   BSimple value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonSimple make(BSimple d) {
      return new BLonSimple(d);
   }

   private BLonSimple(BSimple s) {
      this.value = s;
   }

   public boolean equals(Object obj) {
      return obj instanceof BLonSimple ? this.value.equals(((BLonSimple)obj).value) : false;
   }

   public String toString(Context context) {
      if (this.value != null) {
         return this.value.toString(context);
      } else {
         return this.getType() != null ? this.getTypeDisplayName(context) : this.getClass().getName();
      }
   }

   public String encodeToString() throws IOException {
      return this.encodeClass(this.value) + " " + this.value.encodeToString();
   }

   public BObject decodeFromString(String s) throws IOException {
      int typNamLen = s.indexOf(32);
      BSimple d = this.decodeClass(s.substring(0, typNamLen));
      return make((BSimple)d.decodeFromString(s.substring(typNamLen + 1)));
   }

   public void encode(DataOutput out) throws IOException {
      out.writeUTF(this.encodeClass(this.value));
      this.value.encode(out);
   }

   public BObject decode(DataInput in) throws IOException {
      BSimple d = this.decodeClass(in.readUTF());
      return make((BSimple)d.decode(in));
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      BILonNetworkSimple val = (BILonNetworkSimple)this.value;
      val.toOutputStream(out);
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      BILonNetworkSimple val = (BILonNetworkSimple)this.value;
      BSimple newValue = (BSimple)val.fromInputStream(in);
      return newValue.equals(val) ? this : make(newValue);
   }

   @Override
   public String getDataAsString() {
      String s;
      try {
         s = this.value.encodeToString();
      } catch (Throwable var3) {
         s = "";
      }

      return s;
   }

   @Override
   public BLonPrimitive makeFromString(String stringValue) {
      try {
         return make((BSimple)this.value.decodeFromString(stringValue));
      } catch (Throwable var4) {
         return null;
      }
   }
}
