package javax.baja.lonworks.datatypes;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BAuthenticationKey extends BSimple {
   private static final byte[] defaultKey = new byte[]{-1, -1, -1, -1, -1, -1};
   public static final BAuthenticationKey DEFAULT = new BAuthenticationKey(defaultKey);
   public static final Type TYPE = Sys.loadType(BAuthenticationKey.class);
   public static final int AUTH_KEY_LENGTH = 6;
   private byte[] authKey;

   public Type getType() {
      return TYPE;
   }

   public static BAuthenticationKey make(byte[] b) {
      return new BAuthenticationKey(b);
   }

   public BAuthenticationKey makeFrom(byte[] b) {
      return this.compare(b) ? this : new BAuthenticationKey(b);
   }

   private BAuthenticationKey(byte[] b) {
      if (b.length != 6) {
         throw new RuntimeException("Attempt to create BAuthenticationKey with invalid length");
      } else {
         this.authKey = b;
      }
   }

   public byte[] getByteArray() {
      byte[] a = new byte[6];
      System.arraycopy(this.authKey, 0, a, 0, 6);
      return a;
   }

   public boolean isZero() {
      for (int i = 0; i < this.authKey.length; i++) {
         if (this.authKey[i] != 0) {
            return false;
         }
      }

      return true;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BAuthenticationKey)) {
         return false;
      } else {
         BAuthenticationKey comp = (BAuthenticationKey)obj;
         return this.compare(comp.authKey);
      }
   }

   private boolean compare(byte[] key) {
      if (key.length != 6) {
         return false;
      } else {
         for (int i = 0; i < 6; i++) {
            if (this.authKey[i] != key[i]) {
               return false;
            }
         }

         return true;
      }
   }

   public String toString(Context context) {
      return LonByteArrayUtil.toString(this.authKey);
   }

   public void encode(DataOutput out) throws IOException {
      out.write(this.authKey);
   }

   public BObject decode(DataInput in) throws IOException {
      byte[] aKey = new byte[6];
      in.readFully(aKey, 0, 6);
      return new BAuthenticationKey(aKey);
   }

   public String encodeToString() throws IOException {
      return this.toString();
   }

   public BObject decodeFromString(String s) throws IOException {
      return new BAuthenticationKey(LonByteArrayUtil.getBytes(s, 6));
   }
}
