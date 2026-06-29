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

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=517")
public enum BrowseResultMask implements Enumeration {
   None(0),
   ReferenceTypeId(1),
   IsForward(2),
   NodeClass(4),
   BrowseName(8),
   DisplayName(16),
   TypeDefinition(32),
   All(63),
   ReferenceTypeInfo(3),
   TargetInfo(60);

   public static final EnumerationSpecification SPECIFICATION;
   public static final EnumSet<BrowseResultMask> NONE = EnumSet.noneOf(BrowseResultMask.class);
   public static final EnumSet<BrowseResultMask> ALL = EnumSet.allOf(BrowseResultMask.class);
   private static final Map<Integer, BrowseResultMask> map = new HashMap<>();
   private final int value;

   private BrowseResultMask(int var3) {
      this.value = var3;
   }

   @Override
   public EnumerationSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowseResultMask valueOf(int var0) {
      return map.get(var0);
   }

   public static BrowseResultMask valueOf(Integer var0) {
      return var0 == null ? null : valueOf(var0.intValue());
   }

   public static BrowseResultMask valueOf(UnsignedInteger var0) {
      return var0 == null ? null : valueOf(var0.intValue());
   }

   public static BrowseResultMask[] valueOf(int[] var0) {
      BrowseResultMask[] var1 = new BrowseResultMask[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static BrowseResultMask[] valueOf(Integer[] var0) {
      BrowseResultMask[] var1 = new BrowseResultMask[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static BrowseResultMask[] valueOf(UnsignedInteger[] var0) {
      BrowseResultMask[] var1 = new BrowseResultMask[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = valueOf(var0[var2]);
      }

      return var1;
   }

   public static UnsignedInteger getMask(BrowseResultMask... var0) {
      int var1 = 0;

      for (BrowseResultMask var5 : var0) {
         var1 |= var5.value;
      }

      return UnsignedInteger.getFromBits(var1);
   }

   public static UnsignedInteger getMask(Collection<BrowseResultMask> var0) {
      int var1 = 0;

      for (BrowseResultMask var3 : var0) {
         var1 |= var3.value;
      }

      return UnsignedInteger.getFromBits(var1);
   }

   public static EnumSet<BrowseResultMask> getSet(UnsignedInteger var0) {
      return getSet(var0.intValue());
   }

   public static EnumSet<BrowseResultMask> getSet(int var0) {
      ArrayList var1 = new ArrayList();

      for (BrowseResultMask var5 : values()) {
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

   public static BrowseResultMask.Builder builder() {
      return new BrowseResultMask.Builder();
   }

   public BrowseResultMask.Builder toBuilder() {
      BrowseResultMask.Builder var1 = builder();
      var1.setValue(this.getValue());
      return var1;
   }

   static {
      for (BrowseResultMask var3 : values()) {
         map.put(var3.value, var3);
      }

      com.prosysopc.ua.typedictionary.EnumerationSpecification.Builder var4 = EnumerationSpecification.builder();
      var4.setName("BrowseResultMask");
      var4.setJavaClass(BrowseResultMask.class);
      var4.setTypeId(UaNodeId.fromLocal(ExpandedNodeId.parseExpandedNodeId("nsu=http://opcfoundation.org/UA/;i=517")));
      var4.addMapping(0, "None");
      var4.addMapping(1, "ReferenceTypeId");
      var4.addMapping(2, "IsForward");
      var4.addMapping(4, "NodeClass");
      var4.addMapping(8, "BrowseName");
      var4.addMapping(16, "DisplayName");
      var4.addMapping(32, "TypeDefinition");
      var4.addMapping(63, "All");
      var4.addMapping(3, "ReferenceTypeInfo");
      var4.addMapping(60, "TargetInfo");
      var4.setBuilderSupplier(new EnumerationBuilderSupplier() {
         public Enumeration.Builder get() {
            return BrowseResultMask.builder();
         }
      });
      SPECIFICATION = var4.build();
   }

   public static class Builder implements Enumeration.Builder {
      private BrowseResultMask value;

      private Builder() {
      }

      public BrowseResultMask build() {
         return this.value;
      }

      public BrowseResultMask.Builder setValue(int var1) {
         this.value = BrowseResultMask.valueOf(var1);
         if (this.value == null) {
            throw new IllegalArgumentException("Unknown enum BrowseResultMask int value: " + var1);
         } else {
            return this;
         }
      }
   }
}
