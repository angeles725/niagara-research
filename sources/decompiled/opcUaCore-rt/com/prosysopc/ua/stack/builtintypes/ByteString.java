package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.BitField;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.binary.BinaryEncoder;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

public final class ByteString implements BitField, Comparable<ByteString> {
   public static final ByteString EMPTY = new ByteString(new byte[0], false);
   public static final ByteString[] EMPTY_ARRAY = new ByteString[0];
   private final byte[] value;

   public static byte[] asByteArray(ByteString var0) {
      return var0 == null ? null : var0.getValue();
   }

   public static ByteString from(ByteArrayOutputStream var0) {
      if (var0 == null) {
         return null;
      } else {
         byte[] var1 = var0.toByteArray();
         return var1.length == 0 ? EMPTY : new ByteString(var1, false);
      }
   }

   public static ByteString fromBitSet(BitSet var0) {
      if (var0 == null) {
         return null;
      } else {
         byte[] var1 = new byte[(var0.length() + 7) / 8];

         for (int var2 = 0; var2 < var0.length(); var2++) {
            if (var0.get(var2)) {
               var1[var2 / 8] = (byte)(var1[var2 / 8] | 1 << var2 % 8);
            }
         }

         return valueOf(var1);
      }
   }

   public static ByteString fromBitSet(BitSet var0, int var1) {
      ByteString var2 = fromBitSet(var0);
      return var2.getLength() < var1 ? var2.append(new byte[var1 - var2.getLength()]) : var2;
   }

   public static ByteString fromHex(String var0) {
      return valueOf(CryptoUtil.hexToBytes(var0));
   }

   public static ByteString fromLong(long var0) {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      BinaryEncoder var3 = new BinaryEncoder(var2);

      try {
         var3.putInt64(null, var0);
      } catch (EncodingException var5) {
         throw new IllegalStateException("Encoding of Int64 failed, should not happen", var5);
      }

      return from(var2);
   }

   public static ByteString fromUUID(UUID var0) {
      ByteArrayOutputStream var1 = new ByteArrayOutputStream();
      BinaryEncoder var2 = new BinaryEncoder(var1);

      try {
         var2.putGuid(null, var0);
      } catch (EncodingException var4) {
         throw new IllegalStateException("Encoding of UUID failed, should not happen", var4);
      }

      return from(var1);
   }

   public static ByteString valueOf(byte... var0) {
      if (var0 == null) {
         return null;
      } else {
         return var0.length == 0 ? EMPTY : new ByteString(var0, true);
      }
   }

   static boolean a(byte var0, int var1) {
      if (var1 >= 0 && var1 <= 7) {
         return (var0 & 1 << var1) != 0;
      } else {
         throw new IllegalArgumentException("internal failure in resolving set bits, position out of range 0-7, was: " + var1);
      }
   }

   private ByteString(byte[] var1, boolean var2) {
      if (var2) {
         this.value = Arrays.copyOf(var1, var1.length);
      } else {
         this.value = var1;
      }
   }

   public ByteString append(byte[] var1) {
      if (var1 == null) {
         return this;
      } else {
         ByteArrayOutputStream var2 = new ByteArrayOutputStream();
         var2.write(this.value, 0, this.value.length);
         var2.write(var1, 0, var1.length);
         return from(var2);
      }
   }

   public ByteString append(ByteString var1) {
      return var1 == null ? this : this.append(var1.value);
   }

   public int compareTo(ByteString var1) {
      return this.equals(var1) ? 0 : this.toString().compareTo(var1.toString());
   }

   public void copyTo(ByteArrayOutputStream var1) {
      var1.write(this.value, 0, this.value.length);
   }

   public void copyTo(ByteArrayOutputStream var1, int var2, int var3) {
      var1.write(this.value, var2, var3);
   }

   public void copyTo(OutputStream var1) throws IOException {
      var1.write(this.value, 0, this.value.length);
   }

   public void copyTo(OutputStream var1, int var2, int var3) throws IOException {
      var1.write(this.value, var2, var3);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         ByteString var2 = (ByteString)var1;
         return Arrays.equals(this.value, var2.value);
      }
   }

   public int getLength() {
      return this.value.length;
   }

   public byte[] getValue() {
      return Arrays.copyOf(this.value, this.value.length);
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      byte var2 = 1;
      return 31 * var2 + Arrays.hashCode(this.value);
   }

   public boolean isBitSet(int var1) {
      int var2 = var1 / 8;
      int var3 = var1 % 8;
      return var2 >= this.value.length ? false : a(this.value[var2], var3);
   }

   public BitSet toBitSet() {
      BitSet var1 = new BitSet(this.getLength() * 8);

      for (int var2 = 0; var2 < this.getLength() * 8; var2++) {
         if (this.isBitSet(var2)) {
            var1.set(var2);
         }
      }

      return var1;
   }

   public String toHex() {
      return CryptoUtil.toHex(this.value, 0, false);
   }

   @Override
   public String toString() {
      return CryptoUtil.toHex(this.value, 0);
   }
}
