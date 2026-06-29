package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.BitField;
import java.util.Objects;

public final class UnsignedInteger extends Number implements BitField, Comparable<Number> {
   private static final UnsignedInteger[] CACHE = new UnsignedInteger[1024];
   private static final long serialVersionUID = 8818590379317818155L;
   public static final long L_MAX_VALUE = 4294967295L;
   public static final long L_MIN_VALUE = 0L;
   public static final UnsignedInteger MAX_VALUE = new UnsignedInteger(4294967295L);
   public static final UnsignedInteger MIN_VALUE = new UnsignedInteger(0L);
   public static final UnsignedInteger ZERO = MIN_VALUE;
   public static final UnsignedInteger ONE = new UnsignedInteger(1);
   public static final UnsignedInteger[] EMPTY_ARRAY = new UnsignedInteger[0];
   private int value;

   public static UnsignedInteger[] arrayOf() {
      return EMPTY_ARRAY;
   }

   public static UnsignedInteger[] arrayOf(long... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedInteger[] var1 = new UnsignedInteger[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedInteger[] arrayOf(String... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedInteger[] var1 = new UnsignedInteger[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedInteger clamp(UnsignedInteger var0, UnsignedInteger var1, UnsignedInteger var2) {
      var0 = var0 == null ? ZERO : var0;
      var1 = var1 == null ? ZERO : var1;
      var2 = var2 == null ? ZERO : var2;
      if (var0.longValue() < var1.longValue()) {
         return var1;
      } else {
         return var0.longValue() > var2.longValue() ? var2 : var0;
      }
   }

   public static UnsignedInteger getFromBits(int var0) {
      if (var0 >= 0 && var0 < CACHE.length) {
         return CACHE[var0];
      } else {
         UnsignedInteger var1 = new UnsignedInteger();
         var1.value = var0;
         return var1;
      }
   }

   public static UnsignedInteger max(UnsignedInteger var0, UnsignedInteger var1) {
      return var0.longValue() < var1.longValue() ? var1 : var0;
   }

   public static UnsignedInteger min(UnsignedInteger var0, UnsignedInteger var1) {
      return var0.longValue() < var1.longValue() ? var0 : var1;
   }

   public static UnsignedInteger parseUnsignedInteger(String var0) throws NumberFormatException, IllegalArgumentException {
      return valueOf(Long.parseLong(var0));
   }

   public static UnsignedInteger parseUnsignedInteger(String var0, int var1) throws NumberFormatException, IllegalArgumentException {
      return valueOf(Long.parseLong(var0, var1));
   }

   public static UnsignedInteger replaceIfNullOrEqual(UnsignedInteger var0, UnsignedInteger var1, UnsignedInteger var2) {
      return var0 != null && !Objects.equals(var0, var1) ? var0 : var2;
   }

   public static UnsignedInteger replaceIfNullOrZero(UnsignedInteger var0, UnsignedInteger var1) {
      return replaceIfNullOrEqual(var0, ZERO, var1);
   }

   public static UnsignedInteger valueOf(long var0) {
      return var0 >= 0L && var0 < CACHE.length ? CACHE[(int)var0] : new UnsignedInteger(var0);
   }

   public static UnsignedInteger valueOf(String var0) {
      return parseUnsignedInteger(var0);
   }

   public static UnsignedInteger valueOf(String var0, int var1) {
      return parseUnsignedInteger(var0, var1);
   }

   public static UnsignedInteger valueOf(UnsignedByte var0) {
      return UnsignedByte.ZERO.equals(var0) ? ZERO : valueOf(var0.getValue());
   }

   @Deprecated
   public UnsignedInteger() {
      this.value = 0;
   }

   @Deprecated
   public UnsignedInteger(int var1) throws IllegalArgumentException {
      if (var1 < 0) {
         throw new IllegalArgumentException("Value underflow");
      } else {
         this.value = var1;
      }
   }

   @Deprecated
   public UnsignedInteger(long var1) throws IllegalArgumentException {
      if (var1 >= 0L && var1 <= 4294967295L) {
         this.value = (int)var1;
      } else {
         throw new IllegalArgumentException("Value overflow");
      }
   }

   @Deprecated
   public UnsignedInteger(String var1) throws IllegalArgumentException {
      long var2 = Long.parseLong(var1);
      if (var2 >= 0L && var2 <= 4294967295L) {
         this.value = (int)var2;
      } else {
         throw new IllegalArgumentException("Value overflow");
      }
   }

   @Deprecated
   public UnsignedInteger(UnsignedByte var1) {
      this.value = var1.getValue();
   }

   @Deprecated
   public UnsignedInteger(UnsignedInteger var1) {
      this.value = var1.value;
   }

   public UnsignedInteger add(int var1) {
      return valueOf(this.getValue() + var1);
   }

   public UnsignedInteger add(long var1) {
      return valueOf(this.getValue() + var1);
   }

   public UnsignedInteger add(UnsignedInteger var1) {
      return valueOf(this.getValue() + var1.getValue());
   }

   public UnsignedInteger and(int var1) {
      return getFromBits(this.value & var1);
   }

   public UnsignedInteger and(long var1) {
      return new UnsignedInteger(this.value & var1);
   }

   public UnsignedInteger and(UnsignedInteger var1) {
      return getFromBits(var1.value & this.value);
   }

   @Override
   public byte byteValue() {
      return (byte)(this.value & 0xFF);
   }

   public int compareTo(Number var1) {
      long var2 = this.longValue();
      long var4 = var1.longValue();
      return var2 < var4 ? -1 : (var2 == var4 ? 0 : 1);
   }

   public UnsignedInteger dec() {
      return valueOf(this.getValue() - 1L);
   }

   public UnsignedInteger decOrWrap() {
      return this.equals(MIN_VALUE) ? MAX_VALUE : this.dec();
   }

   public UnsignedInteger decOrWrapTo(UnsignedInteger var1) {
      return this.equals(MIN_VALUE) ? var1 : this.dec();
   }

   @Override
   public double doubleValue() {
      return this.getValue();
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (!var1.getClass().equals(UnsignedInteger.class)) {
         return false;
      } else {
         UnsignedInteger var2 = (UnsignedInteger)var1;
         return this.value == var2.value;
      }
   }

   @Override
   public float floatValue() {
      return (float)this.getValue();
   }

   public long getValue() {
      return this.value & 4294967295L;
   }

   @Override
   public int hashCode() {
      return this.value;
   }

   public UnsignedInteger inc() {
      return valueOf(this.getValue() + 1L);
   }

   public UnsignedInteger incOrWrap() {
      return this.equals(MAX_VALUE) ? MIN_VALUE : this.inc();
   }

   public UnsignedInteger incOrWrapTo(UnsignedInteger var1) {
      return this.equals(MAX_VALUE) ? var1 : this.inc();
   }

   @Override
   public int intValue() {
      return this.value;
   }

   public boolean isBitSet(int var1) {
      return var1 >= 0 && var1 <= 31 ? (this.value >> var1 & 1) >= 1 : false;
   }

   @Override
   public long longValue() {
      return this.value & 4294967295L;
   }

   public UnsignedInteger or(int var1) {
      return getFromBits(this.value | var1);
   }

   public UnsignedInteger or(long var1) {
      return new UnsignedInteger(this.value | var1);
   }

   public UnsignedInteger or(UnsignedInteger var1) {
      return getFromBits(var1.value | this.value);
   }

   public UnsignedInteger subtract(int var1) {
      return valueOf(this.getValue() - var1);
   }

   public UnsignedInteger subtract(long var1) {
      return valueOf(this.getValue() - var1);
   }

   public UnsignedInteger subtract(UnsignedInteger var1) {
      return valueOf(this.getValue() - var1.getValue());
   }

   public int toIntBits() {
      return this.value;
   }

   @Override
   public String toString() {
      return Long.toString(this.value & 4294967295L);
   }

   static {
      CACHE[0] = ZERO;
      CACHE[1] = ONE;

      for (int var0 = 2; var0 < CACHE.length; var0++) {
         CACHE[var0] = new UnsignedInteger(var0);
      }

      CACHE[0] = ZERO;
      CACHE[1] = ONE;

      for (int var1 = 2; var1 < CACHE.length; var1++) {
         CACHE[var1] = new UnsignedInteger(var1);
      }
   }
}
