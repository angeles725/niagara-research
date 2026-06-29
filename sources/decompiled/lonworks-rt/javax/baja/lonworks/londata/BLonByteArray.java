package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonByteArray extends BLonPrimitive {
   public static final BLonByteArray DEFAULT = new BLonByteArray(1);
   public static final Type TYPE = Sys.loadType(BLonByteArray.class);
   private static final String DELIMITER = " ";
   private static final int RADIX = 16;
   private byte[] value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonByteArray make(int size) {
      return new BLonByteArray(size);
   }

   public static BLonByteArray make(byte[] v) {
      return new BLonByteArray(v);
   }

   private BLonByteArray(byte[] v) {
      this.value = new byte[v.length];
      System.arraycopy(v, 0, this.value, 0, this.value.length);
   }

   private BLonByteArray(int len) {
      this.value = new byte[len];
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BLonByteArray)) {
         return false;
      } else {
         BLonByteArray comp = (BLonByteArray)obj;
         return this.compare(comp.value);
      }
   }

   private boolean compare(byte[] a) {
      if (a.length != this.value.length) {
         return false;
      } else {
         for (int i = 0; i < a.length; i++) {
            if (a[i] != this.value[i]) {
               return false;
            }
         }

         return true;
      }
   }

   public String toString(Context context) {
      int len = this.value.length;
      if (len == 0) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < len; i++) {
            if (i > 0) {
               sb.append(" ");
            }

            sb.append(Integer.toString(this.value[i] & 255, 16));
         }

         return sb.toString();
      }
   }

   public void encode(DataOutput out) throws IOException {
      int len = this.value.length;
      out.writeInt(len);
      if (len > 0) {
         out.write(this.value);
      }
   }

   public BObject decode(DataInput in) throws IOException {
      int len = in.readInt();
      byte[] a = new byte[len];
      if (len > 0) {
         in.readFully(a);
      }

      return new BLonByteArray(a);
   }

   public String encodeToString() {
      return this.toString(null);
   }

   public BObject decodeFromString(String s) {
      StringTokenizer st = new StringTokenizer(s, " ");
      int tokCnt = st.countTokens();
      byte[] b = new byte[tokCnt];
      if (tokCnt > 0) {
         for (int i = 0; i < tokCnt; i++) {
            b[i] = (byte)Integer.parseInt(st.nextToken(), 16);
         }
      }

      return this.compare(b) ? this : new BLonByteArray(b);
   }

   public byte[] getBytes() {
      return this.value;
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      switch (e.getElemtype().getOrdinal()) {
         case 15:
            out.writeByteArray(this.value, e.getSize());
            return;
         default:
            throw new InvalidTypeException("Invalid datatype for LonString.");
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      switch (e.getElemtype().getOrdinal()) {
         case 15:
            return new BLonByteArray(in.readByteArray(e.getSize()));
         default:
            throw new InvalidTypeException("Invalid datatype for LonString.");
      }
   }

   @Override
   public String getDataAsString() {
      return this.encodeToString();
   }

   @Override
   public BLonPrimitive makeFromString(String stringValue) {
      return (BLonPrimitive)this.decodeFromString(stringValue);
   }
}
