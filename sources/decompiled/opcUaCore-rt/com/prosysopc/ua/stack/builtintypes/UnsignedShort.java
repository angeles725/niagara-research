package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.BitField;
import com.prosysopc.ua.stack.core.Identifiers;

public final class UnsignedShort extends Number implements BitField, Comparable<UnsignedShort> {
   private static final long serialVersionUID = 921127710458932841L;
   private static final UnsignedShort[] CACHE = new UnsignedShort[1024];
   @Deprecated
   public static final NodeId ID = Identifiers.UInt16;
   public static final long L_MAX_VALUE = 65535L;
   public static final long L_MIN_VALUE = 0L;
   public static final UnsignedShort MAX_VALUE = new UnsignedShort(L_MAX_VALUE);
   public static final UnsignedShort MIN_VALUE = UnsignedShort.ZERO;
   public static final UnsignedShort ZERO = new UnsignedShort(0);
   public static final UnsignedShort ONE = new UnsignedShort(1);
   public static final UnsignedShort[] EMPTY_ARRAY = new UnsignedShort[0];
   private int value;

   public static UnsignedShort[] arrayOf() {
      return EMPTY_ARRAY;
   }

   public static UnsignedShort[] arrayOf(long... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedShort[] var1 = new UnsignedShort[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedShort[] arrayOf(String... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedShort[] var1 = new UnsignedShort[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedShort getFromBits(short var0) {
      if (var0 >= 0 && var0 < CACHE.length) {
         return CACHE[var0];
      } else {
         UnsignedShort var1 = new UnsignedShort(0);
         var1.value = var0 & '\uffff';
         return var1;
      }
   }

   public static UnsignedShort max(UnsignedShort var0, UnsignedShort var1) {
      return var0.intValue() < var1.intValue() ? var1 : var0;
   }

   public static UnsignedShort min(UnsignedShort var0, UnsignedShort var1) {
      return var0.intValue() < var1.intValue() ? var0 : var1;
   }

   public static UnsignedShort parseUnsignedShort(String var0) throws NumberFormatException, IllegalArgumentException {
      return valueOf(Integer.parseInt(var0));
   }

   public static UnsignedShort parseUnsignedShort(String var0, int var1) throws NumberFormatException, IllegalArgumentException {
      return valueOf(Integer.parseInt(var0, var1));
   }

   public static UnsignedShort valueOf(int var0) {
      return var0 >= 0 && var0 < CACHE.length ? CACHE[var0] : new UnsignedShort(var0);
   }

   public static UnsignedShort valueOf(long var0) {
      if (var0 >= 0L && var0 <= 2147483647L) {
         return valueOf((int)var0);
      } else {
         throw new IllegalArgumentException("Illegal value");
      }
   }

   public static UnsignedShort valueOf(String var0) {
      return parseUnsignedShort(var0);
   }

   public static UnsignedShort valueOf(String var0, int var1) {
      return parseUnsignedShort(var0, var1);
   }

   @Deprecated
   public UnsignedShort() {
      this.value = 0;
   }

   @Deprecated
   public UnsignedShort(int var1) throws IllegalArgumentException {
      if (var1 >= 0 && var1 < 65536) {
         this.value = var1;
      } else {
         throw new IllegalArgumentException("Illegal value");
      }
   }

   @Deprecated
   public UnsignedShort(Number var1) {
      long var2 = var1.longValue();
      if (var2 >= 0L && var2 < 65536L) {
         this.value = var1.intValue();
      } else {
         throw new IllegalArgumentException("Illegal value");
      }
   }

   @Deprecated
   public UnsignedShort(short var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("Value underflow");
      } else {
         this.value = var1;
      }
   }

   @Deprecated
   public UnsignedShort(String var1) throws IllegalArgumentException {
      int var2 = Integer.parseInt(var1);
      if (var2 >= 0 && var2 < 65536) {
         this.value = var2;
      } else {
         throw new IllegalArgumentException("Illegal value");
      }
   }

   public UnsignedShort add(int var1) {
      return valueOf(this.getValue() + var1);
   }

   public UnsignedShort add(UnsignedShort var1) {
      return valueOf(this.getValue() + var1.getValue());
   }

   @Override
   public byte byteValue() {
      return (byte)(this.value & 0xFF);
   }

   public int compareTo(UnsignedShort var1) {
      return this.value - var1.value;
   }

   public UnsignedShort dec() {
      return valueOf(this.getValue() - 1);
   }

   public UnsignedShort decOrWrap() {
      return this.equals(MIN_VALUE) ? MAX_VALUE : this.dec();
   }

   public UnsignedShort decOrWrapTo(UnsignedShort var1) {
      return this.equals(MIN_VALUE) ? var1 : this.dec();
   }

   @Override
   public double doubleValue() {
      return this.value;
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (!var1.getClass().equals(UnsignedShort.class)) {
         return false;
      } else {
         UnsignedShort var2 = (UnsignedShort)var1;
         return var2.value == this.value;
      }
   }

   @Override
   public float floatValue() {
      return this.value;
   }

   public int getValue() {
      return this.value;
   }

   @Override
   public int hashCode() {
      return this.value;
   }

   public UnsignedShort inc() {
      return valueOf(this.getValue() + 1);
   }

   public UnsignedShort incOrWrap() {
      return this.equals(MAX_VALUE) ? MIN_VALUE : this.inc();
   }

   public UnsignedShort incOrWrapTo(UnsignedShort var1) {
      return this.equals(MAX_VALUE) ? var1 : this.inc();
   }

   @Override
   public int intValue() {
      return this.value;
   }

   public boolean isBitSet(int var1) {
      return var1 >= 0 && var1 <= 15 ? (this.value >> var1 & 1) >= 1 : false;
   }

   @Override
   public long longValue() {
      return this.value;
   }

   @Override
   public short shortValue() {
      return (short)(this.value & 65535);
   }

   public UnsignedShort subtract(int var1) {
      return valueOf(this.getValue() - var1);
   }

   public UnsignedShort subtract(UnsignedShort var1) {
      return valueOf(this.getValue() - var1.getValue());
   }

   public short toShortBits() {
      return (short)(this.value & 65535);
   }

   @Override
   public String toString() {
      return Integer.toString(this.value);
   }

   static {
      CACHE[0] = ZERO;
      CACHE[1] = ONE;

      for (int var0 = 2; var0 < CACHE.length; var0++) {
         CACHE[var0] = new UnsignedShort(var0);
      }
   }
}
