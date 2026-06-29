package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=546")
public class BrowsePathTarget extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.BrowsePathTarget_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.BrowsePathTarget_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.BrowsePathTarget_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.BrowsePathTarget;
   public static final StructureSpecification SPECIFICATION;
   private ExpandedNodeId f_targetId;
   private UnsignedInteger f_remainingPathIndex;

   public BrowsePathTarget() {
   }

   public BrowsePathTarget(ExpandedNodeId var1, UnsignedInteger var2) {
      this.f_targetId = var1;
      this.f_remainingPathIndex = var2;
   }

   public ExpandedNodeId getTargetId() {
      return this.f_targetId;
   }

   public void setTargetId(ExpandedNodeId var1) {
      this.f_targetId = var1;
   }

   public UnsignedInteger getRemainingPathIndex() {
      return this.f_remainingPathIndex;
   }

   public void setRemainingPathIndex(UnsignedInteger var1) {
      this.f_remainingPathIndex = var1;
   }

   public BrowsePathTarget clone() {
      BrowsePathTarget var1 = (BrowsePathTarget)super.clone();
      var1.f_targetId = (ExpandedNodeId)StructureUtils.clone(this.f_targetId);
      var1.f_remainingPathIndex = (UnsignedInteger)StructureUtils.clone(this.f_remainingPathIndex);
      return var1;
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         BrowsePathTarget var2 = (BrowsePathTarget)var1;
         return !StructureUtils.scalarOrArrayEquals(this.getTargetId(), var2.getTargetId())
            ? false
            : StructureUtils.scalarOrArrayEquals(this.getRemainingPathIndex(), var2.getRemainingPathIndex());
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getTargetId(), this.getRemainingPathIndex()});
   }

   public void clear() {
      super.clear();
      this.f_targetId = null;
      this.f_remainingPathIndex = null;
   }

   @Deprecated
   public ExpandedNodeId getBinaryEncodeId() {
      return BINARY;
   }

   @Deprecated
   public ExpandedNodeId getXmlEncodeId() {
      return XML;
   }

   @Deprecated
   public ExpandedNodeId getJsonEncodeId() {
      return JSON;
   }

   @Deprecated
   public ExpandedNodeId getTypeId() {
      return ID;
   }

   public Map<FieldSpecification, Object> toFieldsMap() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put(BrowsePathTarget.Fields.TargetId, this.getTargetId());
      var1.put(BrowsePathTarget.Fields.RemainingPathIndex, this.getRemainingPathIndex());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowsePathTarget.Builder builder() {
      return new BrowsePathTarget.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (BrowsePathTarget.Fields.TargetId.equals(var1)) {
         return this.getTargetId();
      } else if (BrowsePathTarget.Fields.RemainingPathIndex.equals(var1)) {
         return this.getRemainingPathIndex();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (BrowsePathTarget.Fields.TargetId.equals(var1)) {
         this.setTargetId((ExpandedNodeId)var2);
      } else if (BrowsePathTarget.Fields.RemainingPathIndex.equals(var1)) {
         this.setRemainingPathIndex((UnsignedInteger)var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public BrowsePathTarget.Builder toBuilder() {
      BrowsePathTarget.Builder var1 = builder();
      var1.setTargetId((ExpandedNodeId)StructureUtils.clone(this.getTargetId()));
      var1.setRemainingPathIndex((UnsignedInteger)StructureUtils.clone(this.getRemainingPathIndex()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(BrowsePathTarget.Fields.TargetId);
      var0.addField(BrowsePathTarget.Fields.RemainingPathIndex);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("BrowsePathTarget");
      var0.setJavaClass(BrowsePathTarget.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(BrowsePathTarget.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private ExpandedNodeId f_targetId;
      private UnsignedInteger f_remainingPathIndex;

      protected Builder() {
      }

      public ExpandedNodeId getTargetId() {
         return this.f_targetId;
      }

      public BrowsePathTarget.Builder setTargetId(ExpandedNodeId var1) {
         this.f_targetId = var1;
         return this;
      }

      public UnsignedInteger getRemainingPathIndex() {
         return this.f_remainingPathIndex;
      }

      public BrowsePathTarget.Builder setRemainingPathIndex(UnsignedInteger var1) {
         this.f_remainingPathIndex = var1;
         return this;
      }

      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 == null) {
            return false;
         } else if (this.getClass() != var1.getClass()) {
            return false;
         } else {
            BrowsePathTarget.Builder var2 = (BrowsePathTarget.Builder)var1;
            return !StructureUtils.scalarOrArrayEquals(this.getTargetId(), var2.getTargetId())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getRemainingPathIndex(), var2.getRemainingPathIndex());
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getTargetId(), this.getRemainingPathIndex()});
      }

      public Object get(FieldSpecification var1) {
         if (BrowsePathTarget.Fields.TargetId.equals(var1)) {
            return this.getTargetId();
         } else if (BrowsePathTarget.Fields.RemainingPathIndex.equals(var1)) {
            return this.getRemainingPathIndex();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowsePathTarget.Builder set(FieldSpecification var1, Object var2) {
         if (BrowsePathTarget.Fields.TargetId.equals(var1)) {
            this.setTargetId((ExpandedNodeId)var2);
            return this;
         } else if (BrowsePathTarget.Fields.RemainingPathIndex.equals(var1)) {
            this.setRemainingPathIndex((UnsignedInteger)var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowsePathTarget.Builder clear() {
         super.clear();
         this.f_targetId = null;
         this.f_remainingPathIndex = null;
         return this;
      }

      public StructureSpecification specification() {
         return BrowsePathTarget.SPECIFICATION;
      }

      public BrowsePathTarget build() {
         return new BrowsePathTarget(this.f_targetId, this.f_remainingPathIndex);
      }
   }

   public static enum Fields implements FieldSpecification {
      TargetId("TargetId", ExpandedNodeId.class, false, UaIds.ExpandedNodeId, -1, null, false),
      RemainingPathIndex("RemainingPathIndex", UnsignedInteger.class, false, UaIds.Index, -1, null, false);

      private final FieldSpecification delegate;

      private Fields(String var3, Class<?> var4, boolean var5, UaNodeId var6, int var7, UaArrayDimensions var8, boolean var9) {
         com.prosysopc.ua.typedictionary.FieldSpecification.Builder var10 = FieldSpecification.builder();
         var10.setName(var3);
         var10.setJavaClass(var4);
         var10.setIsOptional(var5);
         var10.setDataTypeId(var6);
         var10.setValueRank(var7);
         var10.setArrayDimensions(var8);
         var10.setAllowSubTypes(var9);
         this.delegate = var10.build();
      }

      @Deprecated
      public FieldSpecification getSpecification() {
         return this;
      }

      public UaArrayDimensions getArrayDimensions() {
         return this.delegate.getArrayDimensions();
      }

      public UaNodeId getDataTypeId() {
         return this.delegate.getDataTypeId();
      }

      public String getDescription() {
         return this.delegate.getDescription();
      }

      public Class<?> getJavaClass() {
         return this.delegate.getJavaClass();
      }

      public int getMaxStringLength() {
         return this.delegate.getMaxStringLength();
      }

      public String getName() {
         return this.delegate.getName();
      }

      public int getValueRank() {
         return this.delegate.getValueRank();
      }

      public boolean isAllowSubTypes() {
         return this.delegate.isAllowSubTypes();
      }

      public boolean isArray() {
         return this.delegate.isArray();
      }

      public boolean isOptional() {
         return this.delegate.isOptional();
      }
   }
}
