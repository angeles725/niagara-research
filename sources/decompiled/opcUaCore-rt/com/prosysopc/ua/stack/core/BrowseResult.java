package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ByteString;
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

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=522")
public class BrowseResult extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.BrowseResult_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.BrowseResult_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.BrowseResult_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.BrowseResult;
   public static final StructureSpecification SPECIFICATION;
   private StatusCode f_statusCode;
   private ByteString f_continuationPoint;
   private ReferenceDescription[] f_references;

   public BrowseResult() {
   }

   public BrowseResult(StatusCode var1, ByteString var2, ReferenceDescription[] var3) {
      this.f_statusCode = var1;
      this.f_continuationPoint = var2;
      this.f_references = var3;
   }

   public StatusCode getStatusCode() {
      return this.f_statusCode;
   }

   public void setStatusCode(StatusCode var1) {
      this.f_statusCode = var1;
   }

   public ByteString getContinuationPoint() {
      return this.f_continuationPoint;
   }

   public void setContinuationPoint(ByteString var1) {
      this.f_continuationPoint = var1;
   }

   public ReferenceDescription[] getReferences() {
      return this.f_references;
   }

   public void setReferences(ReferenceDescription[] var1) {
      this.f_references = var1;
   }

   public BrowseResult clone() {
      BrowseResult var1 = (BrowseResult)super.clone();
      var1.f_statusCode = (StatusCode)StructureUtils.clone(this.f_statusCode);
      var1.f_continuationPoint = (ByteString)StructureUtils.clone(this.f_continuationPoint);
      var1.f_references = (ReferenceDescription[])StructureUtils.clone(this.f_references);
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
         BrowseResult var2 = (BrowseResult)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getStatusCode(), var2.getStatusCode())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getContinuationPoint(), var2.getContinuationPoint())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getReferences(), var2.getReferences());
         }
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getStatusCode(), this.getContinuationPoint(), this.getReferences()});
   }

   public void clear() {
      super.clear();
      this.f_statusCode = null;
      this.f_continuationPoint = null;
      this.f_references = null;
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
      var1.put(BrowseResult.Fields.StatusCode, this.getStatusCode());
      var1.put(BrowseResult.Fields.ContinuationPoint, this.getContinuationPoint());
      var1.put(BrowseResult.Fields.References, this.getReferences());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowseResult.Builder builder() {
      return new BrowseResult.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (BrowseResult.Fields.StatusCode.equals(var1)) {
         return this.getStatusCode();
      } else if (BrowseResult.Fields.ContinuationPoint.equals(var1)) {
         return this.getContinuationPoint();
      } else if (BrowseResult.Fields.References.equals(var1)) {
         return this.getReferences();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (BrowseResult.Fields.StatusCode.equals(var1)) {
         this.setStatusCode((StatusCode)var2);
      } else if (BrowseResult.Fields.ContinuationPoint.equals(var1)) {
         this.setContinuationPoint((ByteString)var2);
      } else if (BrowseResult.Fields.References.equals(var1)) {
         this.setReferences((ReferenceDescription[])var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public BrowseResult.Builder toBuilder() {
      BrowseResult.Builder var1 = builder();
      var1.setStatusCode((StatusCode)StructureUtils.clone(this.getStatusCode()));
      var1.setContinuationPoint((ByteString)StructureUtils.clone(this.getContinuationPoint()));
      var1.setReferences((ReferenceDescription[])StructureUtils.clone(this.getReferences()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(BrowseResult.Fields.StatusCode);
      var0.addField(BrowseResult.Fields.ContinuationPoint);
      var0.addField(BrowseResult.Fields.References);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("BrowseResult");
      var0.setJavaClass(BrowseResult.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(BrowseResult.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private StatusCode f_statusCode;
      private ByteString f_continuationPoint;
      private ReferenceDescription[] f_references;

      protected Builder() {
      }

      public StatusCode getStatusCode() {
         return this.f_statusCode;
      }

      public BrowseResult.Builder setStatusCode(StatusCode var1) {
         this.f_statusCode = var1;
         return this;
      }

      public ByteString getContinuationPoint() {
         return this.f_continuationPoint;
      }

      public BrowseResult.Builder setContinuationPoint(ByteString var1) {
         this.f_continuationPoint = var1;
         return this;
      }

      public ReferenceDescription[] getReferences() {
         return this.f_references;
      }

      public BrowseResult.Builder setReferences(ReferenceDescription[] var1) {
         this.f_references = var1;
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
            BrowseResult.Builder var2 = (BrowseResult.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getStatusCode(), var2.getStatusCode())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getContinuationPoint(), var2.getContinuationPoint())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getReferences(), var2.getReferences());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getStatusCode(), this.getContinuationPoint(), this.getReferences()});
      }

      public Object get(FieldSpecification var1) {
         if (BrowseResult.Fields.StatusCode.equals(var1)) {
            return this.getStatusCode();
         } else if (BrowseResult.Fields.ContinuationPoint.equals(var1)) {
            return this.getContinuationPoint();
         } else if (BrowseResult.Fields.References.equals(var1)) {
            return this.getReferences();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseResult.Builder set(FieldSpecification var1, Object var2) {
         if (BrowseResult.Fields.StatusCode.equals(var1)) {
            this.setStatusCode((StatusCode)var2);
            return this;
         } else if (BrowseResult.Fields.ContinuationPoint.equals(var1)) {
            this.setContinuationPoint((ByteString)var2);
            return this;
         } else if (BrowseResult.Fields.References.equals(var1)) {
            this.setReferences((ReferenceDescription[])var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseResult.Builder clear() {
         super.clear();
         this.f_statusCode = null;
         this.f_continuationPoint = null;
         this.f_references = null;
         return this;
      }

      public StructureSpecification specification() {
         return BrowseResult.SPECIFICATION;
      }

      public BrowseResult build() {
         return new BrowseResult(this.f_statusCode, this.f_continuationPoint, this.f_references);
      }
   }

   public static enum Fields implements FieldSpecification {
      StatusCode("StatusCode", StatusCode.class, false, UaIds.StatusCode, -1, null, false),
      ContinuationPoint("ContinuationPoint", ByteString.class, false, UaIds.ContinuationPoint, -1, null, false),
      References("References", ReferenceDescription[].class, false, UaIds.ReferenceDescription, 1, UaArrayDimensions.valueOf(new long[]{0L}), false);

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
