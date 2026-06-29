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

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=510")
public enum BrowseDirection implements Enumeration {
   Forward(0),
   Inverse(1),
   Both(2),
   Invalid(3);

   public static final EnumerationSpecification SPECIFICATION;
   public static final EnumSet<BrowseDirection> NONE = EnumSet.noneOf(BrowseDirection.class);
   public static final EnumSet<BrowseDirection> ALL = EnumSet.allOf(BrowseDirection.class);
   private static final Map<Integer, BrowseDirection> map = new HashMap<>();
   private final int value;

   private BrowseDirection(int var3) {
      this.value = var3;
   }

   @Override
   public EnumerationSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowseDirection valueOf(int var0) {
      return map.get(var0);
   }

   public static BrowseDirection valueOf(Integer var0) {
      return var0 == null ? null : valueOf(var0.intValue());
   }

   public static BrowseDirection valueOf(UnsignedInteger var0) {
      return var0 == null ? null : valueOf(var0.intValue());
   }

   public static BrowseDirection[] valueOf(int[] var0) {
      BrowseDirection[] var1 = new BrowseDirection[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static BrowseDirection[] valueOf(Integer[] var0) {
      BrowseDirection[] var1 = new BrowseDirection[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static BrowseDirection[] valueOf(UnsignedInteger[] var0) {
      BrowseDirection[] var1 = new BrowseDirection[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static UnsignedInteger getMask(BrowseDirection... var0) {
      int var1 = 0;

      for (BrowseDirection var5 : var0) {
         var1 |= var5.value;
      }

      return UnsignedInteger.getFromBits(var1);
   }

   public static UnsignedInteger getMask(Collection<BrowseDirection> var0) {
      int var1 = 0;

      for (BrowseDirection var3 : var0) {
         var1 |= var3.value;
      }

      return UnsignedInteger.getFromBits(var1);
   }

   public static EnumSet<BrowseDirection> getSet(UnsignedInteger var0) {
      return getSet(var0.intValue());
   }

   public static EnumSet<BrowseDirection> getSet(int var0) {
      ArrayList var1 = new ArrayList();

      for (BrowseDirection var5 : values()) {
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

   public static BrowseDirection.Builder builder() {
      return new BrowseDirection.Builder();
   }

   public BrowseDirection.Builder toBuilder() {
      BrowseDirection.Builder var1 = builder();
      var1.setValue(this.getValue());
      return var1;
   }

   static {
      for (BrowseDirection var3 : values()) {
         map.put(var3.value, var3);
      }

      com.prosysopc.ua.typedictionary.EnumerationSpecification.Builder var4 = EnumerationSpecification.builder();
      var4.setName("BrowseDirection");
      var4.setJavaClass(BrowseDirection.class);
      var4.setTypeId(UaNodeId.fromLocal(ExpandedNodeId.parseExpandedNodeId("nsu=http://opcfoundation.org/UA/;i=510")));
      var4.addMapping(0, "Forward");
      var4.addMapping(1, "Inverse");
      var4.addMapping(2, "Both");
      var4.addMapping(3, "Invalid");
      var4.setBuilderSupplier(new EnumerationBuilderSupplier() {
         public Enumeration.Builder get() {
            return BrowseDirection.builder();
         }
      });
      SPECIFICATION = var4.build();
   }

   public static class Builder implements Enumeration.Builder {
      private BrowseDirection value;

      private Builder() {
      }

      public BrowseDirection build() {
         return this.value;
      }

      public BrowseDirection.Builder setValue(int var1) {
         this.value = BrowseDirection.valueOf(var1);
         if (this.value == null) {
            throw new IllegalArgumentException("Unknown enum BrowseDirection int value: " + var1);
         } else {
            return this;
         }
      }
   }
}
