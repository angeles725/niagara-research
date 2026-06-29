package javax.baja.bacnet.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
public final class BBacnetOctetString extends BSimple {
   private static Lexicon lex = Lexicon.make(BBacnetOctetString.class);
   public static final String BACNET_OCTET_STRING = "bacOctetStr";
   public static final BBacnetOctetString DEFAULT = make(null);
   public static final BBacnetOctetString BACNET_WEEK_N_DAY = make(new byte[]{-1, -1, -1});
   public static final Type TYPE = Sys.loadType(BBacnetOctetString.class);
   private byte[] arr = null;
   private int hashCode;
   private static final Logger logger = Logger.getLogger("bacnet.datatypes");

   private BBacnetOctetString(byte[] arr) {
      if (arr == null) {
         this.arr = null;
      } else {
         this.arr = new byte[arr.length];
         System.arraycopy(arr, 0, this.arr, 0, arr.length);
      }

      this.getHashCode();
   }

   public static synchronized BBacnetOctetString make(byte[] arr) {
      return new BBacnetOctetString(arr);
   }

   public boolean isNull() {
      return this.arr == null;
   }

   public boolean equals(Object obj) {
      if (obj instanceof BBacnetOctetString) {
         BBacnetOctetString comp = (BBacnetOctetString)obj;
         return Arrays.equals(comp.arr, this.arr);
      } else {
         return false;
      }
   }

   public void encode(DataOutput out) throws IOException {
      if (this.arr == null) {
         out.writeInt(0);
      } else {
         out.writeInt(this.arr.length);
         out.write(this.arr, 0, this.arr.length);
      }
   }

   public BObject decode(DataInput in) throws IOException {
      byte[] a = null;
      int len = -1;

      try {
         len = in.readInt();
         if (len != 0) {
            a = new byte[len];
            in.readFully(a, 0, len);
         }

         return make(a);
      } catch (OutOfMemoryError var5) {
         throw new IOException("Invalid octet string length: " + len);
      }
   }

   public String encodeToString() throws IOException {
      return encodeToString(this.arr);
   }

   public static String encodeToString(byte[] arr) {
      if (arr != null && arr.length != 0) {
         StringBuilder sb = new StringBuilder();
         sb.append(hexByte(arr[0]));

         for (int i = 1; i < arr.length; i++) {
            sb.append(" ");
            sb.append(hexByte(arr[i]));
         }

         return sb.toString();
      } else {
         return "null";
      }
   }

   private static String hexByte(byte b) {
      String s = TextUtil.byteToHexString(b);
      return s.length() == 1 ? "0" + s : s;
   }

   public BObject decodeFromString(String s) throws IOException {
      byte[] a;
      if (s.equals("null")) {
         a = null;
      } else {
         StringTokenizer st = new StringTokenizer(s, " ");
         a = new byte[st.countTokens()];

         try {
            for (int i = 0; i < a.length; i++) {
               a[i] = (byte)Integer.parseInt(st.nextToken(), 16);
            }
         } catch (Exception var5) {
            logger.log(Level.SEVERE, "Exception occurred in decodeFromString", (Throwable)var5);
            a = null;
         }
      }

      return make(a);
   }

   public int length() {
      return this.arr.length;
   }

   public byte byteAt(int index) {
      return this.arr[index];
   }

   public byte[] getBytes() {
      if (this.arr == null) {
         return null;
      } else {
         byte[] b = new byte[this.arr.length];
         System.arraycopy(this.arr, 0, b, 0, b.length);
         return b;
      }
   }

   public byte[] getAddr() {
      return this.arr == null ? null : this.arr;
   }

   public String toString(Context cx) {
      if (cx != null) {
         if (cx.equals(BacnetConst.nameContext)) {
            return this.toNameString();
         }

         if (cx.equals(BacnetConst.deviceRegistryContext)) {
            return TextUtil.bytesToHexString(this.arr);
         }

         BString osType = (BString)cx.getFacet("bacOctetStr");
         if (osType != null) {
            StringBuilder sb = new StringBuilder();
            String base = osType.getString() + ".b";

            for (int i = 0; i < this.arr.length; i++) {
               String key = base + String.valueOf(i) + "." + this.arr[i];
               String s = lex.get(key);
               if (s == null) {
                  s = hexByte(this.arr[i]);
               }

               sb.append(s).append(':');
            }

            return sb.substring(0, sb.length() - 1);
         }
      }

      try {
         return this.encodeToString();
      } catch (Exception var8) {
         logger.log(Level.SEVERE, "Exception occurred in toString", (Throwable)var8);
         return var8.toString();
      }
   }

   public int hashCode() {
      return this.hashCode;
   }

   private String toNameString() {
      if (this.arr != null && this.arr.length != 0) {
         StringBuilder sb = new StringBuilder();
         sb.append("_").append(TextUtil.byteToHexString(this.arr[0]));

         for (int i = 1; i < this.arr.length; i++) {
            sb.append("_").append(TextUtil.byteToHexString(this.arr[i]));
         }

         return sb.toString();
      } else {
         return "null";
      }
   }

   public static String bytesToString(byte[] b) {
      if (b != null && b.length != 0) {
         StringBuilder sb = new StringBuilder();
         String s = TextUtil.byteToHexString(b[0]);
         if (s.length() == 1) {
            sb.append("0");
         }

         sb.append(s);

         for (int i = 1; i < b.length; i++) {
            sb.append(" ");
            s = TextUtil.byteToHexString(b[i]);
            if (s.length() == 1) {
               sb.append("0");
            }

            sb.append(s);
         }

         return sb.toString();
      } else {
         return "null";
      }
   }

   public static byte[] stringToBytes(String s) {
      byte[] a;
      if (s.equals("null")) {
         a = null;
      } else {
         StringTokenizer st = new StringTokenizer(s, " ");
         a = new byte[st.countTokens()];

         try {
            for (int i = 0; i < a.length; i++) {
               a[i] = (byte)Integer.parseInt(st.nextToken(), 16);
            }
         } catch (Exception var4) {
            logger.log(Level.SEVERE, "Exception occurred in stringToBytes", (Throwable)var4);
            a = null;
         }
      }

      return a;
   }

   private void getHashCode() {
      int result = 0;
      if (this.arr != null) {
         result = 1;

         for (int i = 0; i < this.arr.length; i++) {
            result = 31 * result + this.arr[i];
         }
      }

      this.hashCode = result;
   }

   public Type getType() {
      return TYPE;
   }
}
