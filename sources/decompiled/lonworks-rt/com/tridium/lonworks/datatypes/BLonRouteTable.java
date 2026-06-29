package com.tridium.lonworks.datatypes;

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
public final class BLonRouteTable extends BSimple {
   public static final BLonRouteTable DEFAULT = new BLonRouteTable();
   public static final Type TYPE = Sys.loadType(BLonRouteTable.class);
   public static final int ROUTE_TABLE_SIZE = 32;
   private byte[] value;

   public Type getType() {
      return TYPE;
   }

   public static BLonRouteTable make() {
      return new BLonRouteTable();
   }

   private BLonRouteTable() {
      this.value = new byte[32];

      for (int i = 0; i < 32; i++) {
         this.value[i] = -1;
      }
   }

   public static BLonRouteTable make(byte[] a) {
      return new BLonRouteTable(a);
   }

   private BLonRouteTable(byte[] a) {
      this.value = a;
   }

   public byte[] getByteArrayCopy() {
      byte[] a = new byte[32];
      System.arraycopy(this.value, 0, a, 0, 32);
      return a;
   }

   public byte[] getSection(int sec) {
      byte[] section = new byte[8];
      System.arraycopy(this.value, 8 * sec, section, 0, 8);
      return section;
   }

   public boolean getFlag(int index) {
      int byteOffset = index / 8;
      if (byteOffset >= 32) {
         return false;
      } else {
         int bitOffset = index % 8;
         return (this.value[byteOffset] & 1 << bitOffset) != 0;
      }
   }

   public BLonRouteTable invert(int[] subs) {
      byte[] a = new byte[32];

      for (int i = 0; i < 32; i++) {
         int inUse = -1;

         for (int n = 0; n < 8; n++) {
            if (subs[i * 8 + n] > 0) {
               inUse &= ~(1 << n);
            }
         }

         a[i] = (byte)(~this.value[i] | inUse);
      }

      a[0] = (byte)(a[0] & 254);
      return make(a);
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BLonRouteTable)) {
         return false;
      } else {
         BLonRouteTable comp = (BLonRouteTable)obj;

         for (int i = 0; i < comp.value.length; i++) {
            if (this.value[i] != comp.value[i]) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean sectionsEqual(BLonRouteTable comp, int sec) {
      int offset = sec * 8;

      for (int n = 0; n < 8; n++) {
         if (this.value[offset] != comp.value[offset++]) {
            return false;
         }
      }

      return true;
   }

   public String toString(Context context) {
      return LonByteArrayUtil.toString(this.value);
   }

   public void encode(DataOutput out) throws IOException {
      out.write(this.value);
   }

   public BObject decode(DataInput in) throws IOException {
      byte[] a = new byte[32];
      in.readFully(a, 0, 32);
      return new BLonRouteTable(a);
   }

   public String encodeToString() throws IOException {
      return this.toString(null);
   }

   public BObject decodeFromString(String s) throws IOException {
      return new BLonRouteTable(LonByteArrayUtil.getBytes(s, 32));
   }
}
