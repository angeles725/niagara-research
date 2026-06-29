package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.Enumeration;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.EnumerationSpecification.EnumerationBuilderSupplier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=625")
public enum TimestampsToReturn implements Enumeration {
   Source(0),
   Server(1),
   Both(2),
   Neither(3),
   Invalid(4);

   public static final EnumerationSpecification SPECIFICATION;
   public static final EnumSet<TimestampsToReturn> NONE = EnumSet.noneOf(TimestampsToReturn.class);
   public static final EnumSet<TimestampsToReturn> ALL = EnumSet.allOf(TimestampsToReturn.class);
   private static final Map<Integer, TimestampsToReturn> map = new HashMap<>();
   private final int value;

   private TimestampsToReturn(int var3) {
      this.value = var3;
   }

   @Override
   public EnumerationSpecification specification() {
      return SPECIFICATION;
   }

   public static TimestampsToReturn valueOf(int var0) {
      return map.get(var0);
   }

   public static TimestampsToReturn valueOf(Integer var0) {
      return var0 == null ? null : valueOf(var0.intValue());
   }

   public static TimestampsToReturn valueOf(UnsignedInteger var0) {
      return var0 == null ? null : valueOf(var0.intValue());
   }

   public static TimestampsToReturn[] valueOf(int[] var0) {
      TimestampsToReturn[] var1 = new TimestampsToReturn[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static TimestampsToReturn[] valueOf(Integer[] var0) {
      TimestampsToReturn[] var1 = new TimestampsToReturn[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static TimestampsToReturn[] valueOf(UnsignedInteger[] var0) {
      TimestampsToReturn[] var1 = new TimestampsToReturn[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static UnsignedInteger getMask(TimestampsToReturn... var0) {
      int var1 = 0;

      for (TimestampsToReturn var5 : var0) {
         var1 |= var5.value;
      }

      return UnsignedInteger.getFromBits(var1);
   }

   public static UnsignedInteger getMask(Collection<TimestampsToReturn> var0) {
      int var1 = 0;

      for (TimestampsToReturn var3 : var0) {
         var1 |= var3.value;
      }

      return UnsignedInteger.getFromBits(var1);
   }

   public static EnumSet<TimestampsToReturn> getSet(UnsignedInteger var0) {
      return getSet(var0.intValue());
   }

   public static EnumSet<TimestampsToReturn> getSet(int var0) {
      ArrayList var1 = new ArrayList();

      for (TimestampsToReturn var5 : values()) {
         if ((var0 & var5.value) == var5.value) {
            var1.add(var5);
         }
      }

      return EnumSet.copyOf(var1);
   }

   @Override
   public int getValue() {
      return this.value;
   }

   public static TimestampsToReturn.Builder builder() {
      return new TimestampsToReturn.Builder();
   }

   public TimestampsToReturn.Builder toBuilder() {
      TimestampsToReturn.Builder var1 = builder();
      var1.setValue(this.getValue());
      return var1;
   }

   static {
      for (TimestampsToReturn var3 : values()) {
         map.put(var3.value, var3);
      }

      com.prosysopc.ua.typedictionary.EnumerationSpecification.Builder var4 = EnumerationSpecification.builder();
      var4.setName("TimestampsToReturn");
      var4.setJavaClass(TimestampsToReturn.class);
      var4.setTypeId(UaNodeId.fromLocal(ExpandedNodeId.parseExpandedNodeId("nsu=http://opcfoundation.org/UA/;i=625")));
      var4.addMapping(0, "Source");
      var4.addMapping(1, "Server");
      var4.addMapping(2, "Both");
      var4.addMapping(3, "Neither");
      var4.addMapping(4, "Invalid");
      var4.setBuilderSupplier(new EnumerationBuilderSupplier() {
         public Enumeration.Builder get() {
            return TimestampsToReturn.builder();
         }
      });
      SPECIFICATION = var4.build();
   }

   public static class Builder implements Enumeration.Builder {
      private TimestampsToReturn value;

      private Builder() {
      }

      public TimestampsToReturn build() {
         return this.value;
      }

      public TimestampsToReturn.Builder setValue(int var1) {
         this.value = TimestampsToReturn.valueOf(var1);
         if (this.value == null) {
            throw new IllegalArgumentException("Unknown enum TimestampsToReturn int value: " + var1);
         } else {
            return this;
         }
      }
   }
}
