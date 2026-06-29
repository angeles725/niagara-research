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
public final class BDomainId extends BSimple {
   public static final BDomainId DEFAULT = new BDomainId();
   public static final Type TYPE = Sys.loadType(BDomainId.class);
   public static final int MAX_DOMAIN_LENGTH = 6;
   private byte[] domainId = new byte[]{0, 0, 0, 0, 0, 0};
   private int domainLength = 0;

   public Type getType() {
      return TYPE;
   }

   public static BDomainId make(int len, byte[] b) {
      return new BDomainId(len, b);
   }

   public BDomainId makeFrom(int len, byte[] b) {
      return this.compare(len, b) ? this : new BDomainId(len, b);
   }

   private BDomainId(int len, byte[] b) {
      if (!this.isValidLength(len)) {
         throw new RuntimeException("Invalid DomainId length.");
      } else {
         this.domainLength = len;
         System.arraycopy(b, 0, this.domainId, 0, len);
      }
   }

   private BDomainId() {
   }

   public byte[] getDomainId() {
      byte[] a = new byte[this.domainLength];
      System.arraycopy(this.domainId, 0, a, 0, this.domainLength);
      return a;
   }

   public int getLength() {
      return this.domainLength;
   }

   public boolean isZeroLength() {
      return this.domainLength == 0;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BDomainId)) {
         return false;
      } else {
         BDomainId comp = (BDomainId)obj;
         return this.compare(comp.domainLength, comp.domainId);
      }
   }

   private boolean compare(int len, byte[] b) {
      if (this.domainLength != len) {
         return false;
      } else {
         for (int i = 0; i < len; i++) {
            if (this.domainId[i] != b[i]) {
               return false;
            }
         }

         return true;
      }
   }

   public String toString(Context context) {
      return "len=" + this.domainLength + ":" + LonByteArrayUtil.toString(this.domainId, '.', this.domainLength);
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.domainLength);

      for (int i = 0; i < this.domainLength; i++) {
         out.writeInt(this.domainId[i]);
      }
   }

   public BObject decode(DataInput in) throws IOException {
      int len = in.readInt();
      byte[] id = new byte[6];

      for (int i = 0; i < len; i++) {
         id[i] = (byte)in.readInt();
      }

      return new BDomainId(len, id);
   }

   public String encodeToString() throws IOException {
      return this.toString();
   }

   public BObject decodeFromString(String s) throws IOException {
      if (s.length() == 0) {
         return DEFAULT;
      } else {
         String slen = s.substring(s.indexOf(61) + 1, s.indexOf(58)).trim();
         int len = Integer.parseInt(slen);
         if (len == 0) {
            return DEFAULT;
         } else if (!this.isValidLength(len)) {
            throw new RuntimeException("Invalid DomainId length.");
         } else {
            String id = s.substring(s.indexOf(58) + 1);
            byte[] domId = LonByteArrayUtil.getBytes(id, len);
            return new BDomainId(len, domId);
         }
      }
   }

   private boolean isValidLength(int len) {
      switch (len) {
         case 0:
         case 1:
         case 3:
         case 6:
            return true;
         case 2:
         case 4:
         case 5:
         default:
            return false;
      }
   }
}
