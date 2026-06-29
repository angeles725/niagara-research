package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=549")
public class BrowsePathResult extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.BrowsePathResult_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.BrowsePathResult_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.BrowsePathResult_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.BrowsePathResult;
   public static final StructureSpecification SPECIFICATION;
   private StatusCode f_statusCode;
   private BrowsePathTarget[] f_targets;

   public BrowsePathResult() {
   }

   public BrowsePathResult(StatusCode var1, BrowsePathTarget[] var2) {
      this.f_statusCode = var1;
      this.f_targets = var2;
   }

   public StatusCode getStatusCode() {
      return this.f_statusCode;
   }

   public void setStatusCode(StatusCode var1) {
      this.f_statusCode = var1;
   }

   public BrowsePathTarget[] getTargets() {
      return this.f_targets;
   }

   public void setTargets(BrowsePathTarget[] var1) {
      this.f_targets = var1;
   }

   public BrowsePathResult clone() {
      BrowsePathResult var1 = (BrowsePathResult)super.clone();
      var1.f_statusCode = (StatusCode)StructureUtils.clone(this.f_statusCode);
      var1.f_targets = (BrowsePathTarget[])StructureUtils.clone(this.f_targets);
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
         BrowsePathResult var2 = (BrowsePathResult)var1;
         return !StructureUtils.scalarOrArrayEquals(this.getStatusCode(), var2.getStatusCode())
            ? false
            : StructureUtils.scalarOrArrayEquals(this.getTargets(), var2.getTargets());
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getStatusCode(), this.getTargets()});
   }

   public void clear() {
      super.clear();
      this.f_statusCode = null;
      this.f_targets = null;
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
      var1.put(BrowsePathResult.Fields.StatusCode, this.getStatusCode());
      var1.put(BrowsePathResult.Fields.Targets, this.getTargets());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowsePathResult.Builder builder() {
      return new BrowsePathResult.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (BrowsePathResult.Fields.StatusCode.equals(var1)) {
         return this.getStatusCode();
      } else if (BrowsePathResult.Fields.Targets.equals(var1)) {
         return this.getTargets();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (BrowsePathResult.Fields.StatusCode.equals(var1)) {
         this.setStatusCode((StatusCode)var2);
      } else if (BrowsePathResult.Fields.Targets.equals(var1)) {
         this.setTargets((BrowsePathTarget[])var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public BrowsePathResult.Builder toBuilder() {
      BrowsePathResult.Builder var1 = builder();
      var1.setStatusCode((StatusCode)StructureUtils.clone(this.getStatusCode()));
      var1.setTargets((BrowsePathTarget[])StructureUtils.clone(this.getTargets()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(BrowsePathResult.Fields.StatusCode);
      var0.addField(BrowsePathResult.Fields.Targets);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("BrowsePathResult");
      var0.setJavaClass(BrowsePathResult.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(BrowsePathResult.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private StatusCode f_statusCode;
      private BrowsePathTarget[] f_targets;

      protected Builder() {
      }

      public StatusCode getStatusCode() {
         return this.f_statusCode;
      }

      public BrowsePathResult.Builder setStatusCode(StatusCode var1) {
         this.f_statusCode = var1;
         return this;
      }

      public BrowsePathTarget[] getTargets() {
         return this.f_targets;
      }

      public BrowsePathResult.Builder setTargets(BrowsePathTarget[] var1) {
         this.f_targets = var1;
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
            BrowsePathResult.Builder var2 = (BrowsePathResult.Builder)var1;
            return !StructureUtils.scalarOrArrayEquals(this.getStatusCode(), var2.getStatusCode())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getTargets(), var2.getTargets());
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getStatusCode(), this.getTargets()});
      }

      public Object get(FieldSpecification var1) {
         if (BrowsePathResult.Fields.StatusCode.equals(var1)) {
            return this.getStatusCode();
         } else if (BrowsePathResult.Fields.Targets.equals(var1)) {
            return this.getTargets();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowsePathResult.Builder set(FieldSpecification var1, Object var2) {
         if (BrowsePathResult.Fields.StatusCode.equals(var1)) {
            this.setStatusCode((StatusCode)var2);
            return this;
         } else if (BrowsePathResult.Fields.Targets.equals(var1)) {
            this.setTargets((BrowsePathTarget[])var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowsePathResult.Builder clear() {
         super.clear();
         this.f_statusCode = null;
         this.f_targets = null;
         return this;
      }

      public StructureSpecification specification() {
         return BrowsePathResult.SPECIFICATION;
      }

      public BrowsePathResult build() {
         return new BrowsePathResult(this.f_statusCode, this.f_targets);
      }
   }

   public static enum Fields implements FieldSpecification {
      StatusCode("StatusCode", StatusCode.class, false, UaIds.StatusCode, -1, null, false),
      Targets("Targets", BrowsePathTarget[].class, false, UaIds.BrowsePathTarget, 1, UaArrayDimensions.valueOf(new long[]{0L}), false);

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
