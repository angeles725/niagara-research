package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.BitField;
import com.prosysopc.ua.stack.core.Identifiers;

public final class UnsignedByte extends Number implements BitField, Comparable<UnsignedByte> {
   @Deprecated
   public static final NodeId ID = Identifiers.Byte;
   private static final long serialVersionUID = 4691302796477290208L;
   private static final UnsignedByte[] CACHE = new UnsignedByte[256];
   public static final long L_MAX_VALUE = 255L;
   public static final long L_MIN_VALUE = 0L;
   public static final UnsignedByte MAX_VALUE;
   public static final UnsignedByte MIN_VALUE;
   public static final UnsignedByte ZERO = new UnsignedByte(0);
   public static final UnsignedByte ONE = new UnsignedByte(1);
   public static final UnsignedByte[] EMPTY_ARRAY = new UnsignedByte[0];
   private final int value;

   public static UnsignedByte[] arrayOf() {
      return EMPTY_ARRAY;
   }

   public static UnsignedByte[] arrayOf(long... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedByte[] var1 = new UnsignedByte[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedByte[] arrayOf(String... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedByte[] var1 = new UnsignedByte[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedByte getFromBits(byte var0) {
      return CACHE[var0 & 255];
   }

   public static UnsignedByte max(UnsignedByte var0, UnsignedByte var1) {
      return var0.intValue() < var1.intValue() ? var1 : var0;
   }

   public static UnsignedByte min(UnsignedByte var0, UnsignedByte var1) {
      return var0.intValue() < var1.intValue() ? var0 : var1;
   }

   public static UnsignedByte parseUnsignedByte(String var0) {
      return valueOf(Integer.parseInt(var0));
   }

   public static UnsignedByte parseUnsignedByte(String var0, int var1) {
      return valueOf(Integer.parseInt(var0, var1));
   }

   public static UnsignedByte valueOf(int var0) {
      assertValueInRange(var0);
      return CACHE[var0];
   }

   public static UnsignedByte valueOf(long var0) {
      assertValueInRange(var0);
      return CACHE[(int)var0];
   }

   public static UnsignedByte valueOf(String var0) {
      return parseUnsignedByte(var0);
   }

   public static UnsignedByte valueOf(String var0, int var1) {
      return parseUnsignedByte(var0, var1);
   }

   private static void assertValueInRange(int var0) {
      if (var0 < 0) {
         throw new IllegalArgumentException("Data value underflow, value less than 0, was: " + var0);
      } else if (var0 > 255) {
         throw new IllegalArgumentException("Data value overflow, value over 255, was: " + var0);
      }
   }

   private static void assertValueInRange(long var0) {
      if (var0 < 0L) {
         throw new IllegalArgumentException("Data value underflow!");
      } else if (var0 > 255L) {
         throw new IllegalArgumentException("Data value overflow!");
      }
   }

   @Deprecated
   public UnsignedByte() {
      this.value = 0;
   }

   @Deprecated
   public UnsignedByte(byte var1) throws IllegalArgumentException {
      assertValueInRange((int)var1);
      this.value = var1;
   }

   @Deprecated
   public UnsignedByte(int var1) throws IllegalArgumentException {
      assertValueInRange(var1);
      this.value = var1;
   }

   @Deprecated
   public UnsignedByte(long var1) throws IllegalArgumentException {
      assertValueInRange(var1);
      this.value = (int)var1;
   }

   @Deprecated
   public UnsignedByte(String var1) throws IllegalArgumentException {
      short var2 = Short.parseShort(var1);
      if (var2 >= MIN_VALUE.getValue() && var2 <= MAX_VALUE.getValue()) {
         this.value = var2;
         assertValueInRange(this.value);
      } else {
         throw new IllegalArgumentException("Value out of bounds!");
      }
   }

   public UnsignedByte add(int var1) {
      return valueOf(this.getValue() + var1);
   }

   public UnsignedByte add(UnsignedByte var1) {
      return valueOf(this.getValue() + var1.getValue());
   }

   @Override
   public byte byteValue() {
      return (byte)(this.value & 0xFF);
   }

   public int compareTo(UnsignedByte var1) {
      return this.value - var1.getValue();
   }

   public UnsignedByte dec() {
      return valueOf(this.getValue() - 1);
   }

   public UnsignedByte decOrWrap() {
      return this.equals(MIN_VALUE) ? MAX_VALUE : this.dec();
   }

   public UnsignedByte decOrWrapTo(UnsignedByte var1) {
      return this.equals(MIN_VALUE) ? var1 : this.dec();
   }

   @Override
   public double doubleValue() {
      return this.value;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (!var1.getClass().equals(UnsignedByte.class)) {
         return false;
      } else {
         UnsignedByte var2 = (UnsignedByte)var1;
         return this.value == var2.value;
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

   public UnsignedByte inc() {
      return valueOf(this.getValue() + 1);
   }

   public UnsignedByte incOrWrap() {
      return this.equals(MAX_VALUE) ? MIN_VALUE : this.inc();
   }

   public UnsignedByte incOrWrapTo(UnsignedByte var1) {
      return this.equals(MAX_VALUE) ? var1 : this.inc();
   }

   @Override
   public int intValue() {
      return this.value;
   }

   public boolean isBitSet(int var1) {
      return var1 >= 0 && var1 <= 7 ? (this.value >> var1 & 1) >= 1 : false;
   }

   @Override
   public long longValue() {
      return this.value;
   }

   public UnsignedByte subtract(int var1) {
      return valueOf(this.getValue() - var1);
   }

   public UnsignedByte subtract(UnsignedByte var1) {
      return valueOf(this.getValue() - var1.getValue());
   }

   public byte toByteBits() {
      return (byte)(this.value & 0xFF);
   }

   @Override
   public String toString() {
      return Integer.toString(this.value);
   }

   static {
      CACHE[0] = ZERO;
      CACHE[1] = ONE;

      for (int var0 = 2; var0 < CACHE.length; var0++) {
         CACHE[var0] = new UnsignedByte(var0);
      }

      MIN_VALUE = CACHE[0];
      MAX_VALUE = CACHE[255];
   }
}
