package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.BitField;
import com.prosysopc.ua.stack.core.Identifiers;
import java.math.BigInteger;

public final class UnsignedLong extends Number implements BitField, Comparable<Number> {
   private static final UnsignedLong[] CACHE = new UnsignedLong[1024];
   private static final long serialVersionUID = 1L;
   @Deprecated
   public static final NodeId ID = Identifiers.UInt64;
   public static final int SIZE = 64;
   private static final long L_MAX_VALUE = Long.MAX_VALUE;
   private static final long L_HI_BIT = Long.MIN_VALUE;
   private static final BigInteger BI_L_MAX_VALUE = new BigInteger(Long.toString(Long.MAX_VALUE));
   private static final BigInteger BI_MAX_VALUE = new BigInteger("2").pow(64).add(new BigInteger("-1"));
   private static final BigInteger BI_MIN_VALUE = new BigInteger("0");
   private static final BigInteger BI_MID_VALUE = new BigInteger("2").pow(63);
   private static final double D_MID_VALUE = BI_MID_VALUE.doubleValue();
   private static final float F_MID_VALUE = BI_MID_VALUE.floatValue();
   public static final UnsignedLong MAX_VALUE = new UnsignedLong(BI_MAX_VALUE);
   public static final UnsignedLong MIN_VALUE = new UnsignedLong(BI_MIN_VALUE);
   public static final UnsignedLong ZERO = MIN_VALUE;
   public static final UnsignedLong ONE = new UnsignedLong(1);
   public static final UnsignedLong[] EMPTY_ARRAY = new UnsignedLong[0];
   private long value;

   public static UnsignedLong[] arrayOf() {
      return EMPTY_ARRAY;
   }

   public static UnsignedLong[] arrayOf(BigInteger... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedLong[] var1 = new UnsignedLong[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedLong[] arrayOf(long... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedLong[] var1 = new UnsignedLong[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedLong[] arrayOf(String... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         UnsignedLong[] var1 = new UnsignedLong[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = valueOf(var0[var2]);
         }

         return var1;
      }
   }

   public static UnsignedLong getFromBits(long var0) {
      if (var0 >= 0L && var0 < CACHE.length) {
         return CACHE[(int)var0];
      } else {
         UnsignedLong var2 = new UnsignedLong(0);
         var2.value = var0;
         return var2;
      }
   }

   public static UnsignedLong parseUnsignedLong(String var0) {
      return new UnsignedLong(new BigInteger(var0));
   }

   public static UnsignedLong parseUnsignedLong(String var0, int var1) throws NumberFormatException, IllegalArgumentException {
      return new UnsignedLong(new BigInteger(var0, var1));
   }

   public static UnsignedLong valueOf(BigInteger var0) {
      return new UnsignedLong(var0);
   }

   public static UnsignedLong valueOf(long var0) {
      return var0 >= 0L && var0 < CACHE.length ? CACHE[(int)var0] : new UnsignedLong(var0);
   }

   public static UnsignedLong valueOf(String var0) {
      return parseUnsignedLong(var0);
   }

   public static UnsignedLong valueOf(String var0, int var1) {
      return parseUnsignedLong(var0, var1);
   }

   @Deprecated
   public UnsignedLong(BigInteger var1) throws IllegalArgumentException {
      if (var1.compareTo(BI_MIN_VALUE) < 0) {
         throw new IllegalArgumentException("Value underflow");
      } else if (var1.compareTo(BI_MAX_VALUE) > 0) {
         throw new IllegalArgumentException("Value overflow");
      } else {
         if (var1.compareTo(BI_L_MAX_VALUE) <= 0) {
            this.value = var1.longValue();
         } else {
            this.value = var1.subtract(BI_MID_VALUE).longValue() | Long.MIN_VALUE;
         }
      }
   }

   @Deprecated
   public UnsignedLong(int var1) {
      if (var1 < 0) {
         throw new IllegalArgumentException("Value underflow");
      } else {
         this.value = var1;
      }
   }

   @Deprecated
   public UnsignedLong(long var1) {
      if (var1 < 0L) {
         throw new IllegalArgumentException("Value underflow");
      } else {
         this.value = var1;
      }
   }

   @Deprecated
   public UnsignedLong(String var1) {
      BigInteger var2 = new BigInteger(var1);
      if (var2.compareTo(BI_MIN_VALUE) < 0) {
         throw new IllegalArgumentException("Value underflow");
      } else if (var2.compareTo(BI_MAX_VALUE) > 0) {
         throw new IllegalArgumentException("Value overflow");
      } else {
         if (var2.compareTo(BI_L_MAX_VALUE) < 0) {
            this.value = var2.longValue();
         } else {
            this.value = var2.subtract(BI_MID_VALUE).longValue() | Long.MIN_VALUE;
         }
      }
   }

   public UnsignedLong add(long var1) {
      long var3 = this.getValue() + var1;
      return var1 <= 0L || this.getValue() >= 0L && var3 >= this.getValue()
         ? valueOf(var3)
         : new UnsignedLong(this.bigIntegerValue().add(BigInteger.valueOf(var1)));
   }

   public UnsignedLong add(UnsignedLong var1) {
      long var2 = this.getValue() + var1.getValue();
      return var1.getValue() > 0L && var2 < this.getValue()
         ? new UnsignedLong(BigInteger.valueOf(this.getValue()).add(BigInteger.valueOf(var1.getValue())))
         : valueOf(var2);
   }

   public BigInteger bigIntegerValue() {
      return (this.value & Long.MIN_VALUE) == Long.MIN_VALUE
         ? BigInteger.valueOf(this.value & Long.MAX_VALUE).add(BI_MID_VALUE)
         : BigInteger.valueOf(this.value);
   }

   public int compareTo(Number var1) {
      if ((this.value & Long.MIN_VALUE) == Long.MIN_VALUE ^ (var1.longValue() & Long.MIN_VALUE) == Long.MIN_VALUE) {
         return (this.value & Long.MIN_VALUE) == Long.MIN_VALUE ? 1 : -1;
      } else {
         long var2 = this.longValue();
         long var4 = var1.longValue();
         return var2 < var4 ? -1 : (var2 == var4 ? 0 : 1);
      }
   }

   public UnsignedLong dec() {
      return valueOf(this.getValue() - 1L);
   }

   public UnsignedLong decOrWrap() {
      return this.equals(MIN_VALUE) ? MAX_VALUE : this.dec();
   }

   public UnsignedLong decOrWrapTo(UnsignedLong var1) {
      return this.equals(MIN_VALUE) ? var1 : this.dec();
   }

   @Override
   public double doubleValue() {
      return (this.value & Long.MIN_VALUE) == Long.MIN_VALUE ? (this.value & Long.MAX_VALUE) + D_MID_VALUE : this.value;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (!var1.getClass().equals(UnsignedLong.class)) {
         return false;
      } else {
         UnsignedLong var2 = (UnsignedLong)var1;
         return this.value == var2.value;
      }
   }

   @Override
   public float floatValue() {
      return (this.value & Long.MIN_VALUE) == Long.MIN_VALUE ? (float)(this.value & Long.MAX_VALUE) + F_MID_VALUE : (float)this.value;
   }

   @Override
   public int hashCode() {
      return (int)this.value | (int)(this.value >> 32);
   }

   public UnsignedLong inc() {
      return valueOf(this.getValue() + 1L);
   }

   public UnsignedLong incOrWrap() {
      return this.equals(MAX_VALUE) ? MIN_VALUE : this.inc();
   }

   public UnsignedLong incOrWrapTo(UnsignedLong var1) {
      return this.equals(MAX_VALUE) ? var1 : this.inc();
   }

   @Override
   public int intValue() {
      return (this.value & Long.MIN_VALUE) == Long.MIN_VALUE ? (int)(this.value & 2147483647L) | -2147483648 : (int)this.value;
   }

   public boolean isBitSet(int var1) {
      return var1 >= 0 && var1 <= 63 ? (this.value >> var1 & 1L) >= 1L : false;
   }

   @Override
   public long longValue() {
      return this.value;
   }

   public UnsignedLong subtract(long var1) {
      if (this.getValue() >= 0L && this.getValue() > var1) {
         return valueOf(this.getValue() - var1);
      } else {
         BigInteger var3 = this.bigIntegerValue();
         var3 = var3.subtract(BigInteger.valueOf(var1));
         return new UnsignedLong(var3);
      }
   }

   public UnsignedLong subtract(UnsignedLong var1) {
      return this.getValue() >= 0L && var1.getValue() >= 0L
         ? valueOf(this.getValue() - var1.getValue())
         : new UnsignedLong(this.bigIntegerValue().subtract(var1.bigIntegerValue()));
   }

   public long toLongBits() {
      return this.value;
   }

   @Override
   public String toString() {
      return (this.value & Long.MIN_VALUE) == Long.MIN_VALUE ? this.bigIntegerValue().toString() : Long.toString(this.value);
   }

   private long getValue() {
      return this.value;
   }

   static {
      CACHE[0] = ZERO;
      CACHE[1] = ONE;

      for (int var0 = 2; var0 < CACHE.length; var0++) {
         CACHE[var0] = new UnsignedLong(var0);
      }
   }
}
