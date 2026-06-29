package javax.baja.bacnet.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BBacnetBitString extends BSimple {
   public static final BBacnetBitString DEFAULT = new BBacnetBitString(new boolean[0], BFacets.NULL);
   public static final Type TYPE = Sys.loadType(BBacnetBitString.class);
   private boolean[] bits;
   private BFacets tags;
   private int hashCode;

   private BBacnetBitString(boolean[] bits, BFacets tags) {
      this.bits = bits;
      this.tags = tags;
      this.getHashCode();
   }

   public static BBacnetBitString make(boolean[] bits, BFacets tags) {
      boolean[] mybits = new boolean[bits.length];
      System.arraycopy(bits, 0, mybits, 0, mybits.length);
      return new BBacnetBitString(mybits, tags);
   }

   public static BBacnetBitString make(boolean[] bits) {
      boolean[] mybits = new boolean[bits.length];
      System.arraycopy(bits, 0, mybits, 0, mybits.length);
      return new BBacnetBitString(mybits, BFacets.NULL);
   }

   public static BBacnetBitString make(BBacnetBitString bs, int index, boolean newState) {
      boolean[] newbits = new boolean[bs.bits.length];
      System.arraycopy(bs.bits, 0, newbits, 0, bs.bits.length);
      newbits[index] = newState;
      return new BBacnetBitString(newbits, bs.tags);
   }

   public boolean equals(Object obj) {
      if (obj instanceof BBacnetBitString) {
         boolean[] comp = ((BBacnetBitString)obj).bits;
         if (this.bits.length != comp.length) {
            return false;
         } else {
            for (int i = 0; i < this.bits.length; i++) {
               if (comp[i] != this.bits[i]) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public String toString(Context context) {
      return this.getActiveTags(context);
   }

   public int hashCode() {
      return this.hashCode;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.bits.length);

      for (int i = 0; i < this.bits.length; i++) {
         out.writeBoolean(this.bits[i]);
      }

      this.tags.encode(out);
   }

   public BObject decode(DataInput in) throws IOException {
      int len = in.readInt();
      boolean[] bits = new boolean[len];

      for (int i = 0; i < len; i++) {
         bits[i] = in.readBoolean();
      }

      BFacets tags = (BFacets)BFacets.DEFAULT.decode(in);
      return new BBacnetBitString(bits, tags);
   }

   public String encodeToString() throws IOException {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getCompleteString());
      sb.append(':');
      sb.append(this.tags.encodeToString());
      return sb.toString();
   }

   public BObject decodeFromString(String s) throws IOException {
      BFacets tags = BFacets.NULL;
      int len = s.indexOf(":");
      int tstart = -1;
      if (len < 0) {
         len = s.length();
      } else {
         tstart = len + 1;
      }

      boolean[] bits = new boolean[len];

      for (int i = 0; i < len; i++) {
         bits[i] = s.charAt(i) == '1';
      }

      if (tstart > 0) {
         String t = s.substring(len + 1);
         tags = (BFacets)BFacets.DEFAULT.decodeFromString(t);
      }

      return new BBacnetBitString(bits, tags);
   }

   public int length() {
      return this.bits.length;
   }

   public boolean getBit(int index) {
      try {
         return this.bits[index];
      } catch (Exception var3) {
         throw new IllegalArgumentException("Invalid index (" + index + ") in getBit()!!");
      }
   }

   public boolean[] getBits() {
      boolean[] copy = new boolean[this.bits.length];
      System.arraycopy(this.bits, 0, copy, 0, this.bits.length);
      return copy;
   }

   public String getActiveTags(Context context) {
      Context cx = context;
      if (context == null) {
         cx = this.tags;
      }

      if (cx != null && !cx.equals(BFacets.NULL)) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < this.bits.length; i++) {
            if (this.bits[i]) {
               BObject bitName = cx.getFacet("bit" + String.valueOf(i));
               if (bitName == null) {
                  return this.getCompleteString();
               }

               sb.append(bitName);
               sb.append(";");
            }
         }

         return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "none";
      } else {
         return this.getCompleteString();
      }
   }

   public String getCompleteTagList(Context context) {
      Context cx = context;
      if (context == null) {
         cx = this.tags;
      }

      if (cx != null && !cx.equals(BFacets.NULL)) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < this.bits.length; i++) {
            sb.append(cx.getFacet("bit" + String.valueOf(i)));
            sb.append("=" + this.bits[i] + ";");
         }

         return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "none";
      } else {
         return this.getCompleteString();
      }
   }

   public String getCompleteString() {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < this.bits.length; i++) {
         sb.append((char)(this.bits[i] ? '1' : '0'));
      }

      return sb.toString();
   }

   public static BBacnetBitString emptyBitString(int len) {
      return make(new boolean[len]);
   }

   private void getHashCode() {
      int len = this.bits.length;
      int n = len / 32;

      for (int i = 0; i < n; i++) {
         int hc = 0;

         for (int j = 0; j < 32; j++) {
            int ndx = i * 32 + j;
            if (ndx >= len) {
               break;
            }

            if (this.bits[ndx]) {
               hc |= 1 << j;
            }
         }

         this.hashCode ^= hc;
      }
   }

   public Type getType() {
      return TYPE;
   }
}
