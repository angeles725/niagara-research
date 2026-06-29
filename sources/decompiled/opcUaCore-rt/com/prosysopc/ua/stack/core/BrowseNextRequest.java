package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.ServiceRequest;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=531")
public class BrowseNextRequest extends AbstractStructure implements ServiceRequest<BrowseNextResponse> {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.BrowseNextRequest_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.BrowseNextRequest_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.BrowseNextRequest_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.BrowseNextRequest;
   public static final StructureSpecification SPECIFICATION;
   private RequestHeader f_requestHeader;
   private Boolean f_releaseContinuationPoints;
   private ByteString[] f_continuationPoints;

   public BrowseNextRequest() {
   }

   public BrowseNextRequest(RequestHeader var1, Boolean var2, ByteString[] var3) {
      this.f_requestHeader = var1;
      this.f_releaseContinuationPoints = var2;
      this.f_continuationPoints = var3;
   }

   @Override
   public RequestHeader getRequestHeader() {
      return this.f_requestHeader;
   }

   @Override
   public void setRequestHeader(RequestHeader var1) {
      this.f_requestHeader = var1;
   }

   public Boolean getReleaseContinuationPoints() {
      return this.f_releaseContinuationPoints;
   }

   public void setReleaseContinuationPoints(Boolean var1) {
      this.f_releaseContinuationPoints = var1;
   }

   public ByteString[] getContinuationPoints() {
      return this.f_continuationPoints;
   }

   public void setContinuationPoints(ByteString[] var1) {
      this.f_continuationPoints = var1;
   }

   public BrowseNextRequest clone() {
      BrowseNextRequest var1 = (BrowseNextRequest)super.clone();
      var1.f_requestHeader = (RequestHeader)StructureUtils.clone(this.f_requestHeader);
      var1.f_releaseContinuationPoints = (Boolean)StructureUtils.clone(this.f_releaseContinuationPoints);
      var1.f_continuationPoints = (ByteString[])StructureUtils.clone(this.f_continuationPoints);
      return var1;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         BrowseNextRequest var2 = (BrowseNextRequest)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getReleaseContinuationPoints(), var2.getReleaseContinuationPoints())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getContinuationPoints(), var2.getContinuationPoints());
         }
      }
   }

   @Override
   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getReleaseContinuationPoints(), this.getContinuationPoints()});
   }

   @Override
   public void clear() {
      super.clear();
      this.f_requestHeader = null;
      this.f_releaseContinuationPoints = null;
      this.f_continuationPoints = null;
   }

   @Deprecated
   @Override
   public ExpandedNodeId getBinaryEncodeId() {
      return BINARY;
   }

   @Deprecated
   @Override
   public ExpandedNodeId getXmlEncodeId() {
      return XML;
   }

   @Deprecated
   @Override
   public ExpandedNodeId getJsonEncodeId() {
      return JSON;
   }

   @Deprecated
   @Override
   public ExpandedNodeId getTypeId() {
      return ID;
   }

   @Override
   public Map<FieldSpecification, Object> toFieldsMap() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put(BrowseNextRequest.Fields.RequestHeader, this.getRequestHeader());
      var1.put(BrowseNextRequest.Fields.ReleaseContinuationPoints, this.getReleaseContinuationPoints());
      var1.put(BrowseNextRequest.Fields.ContinuationPoints, this.getContinuationPoints());
      return Collections.unmodifiableMap(var1);
   }

   @Override
   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowseNextRequest.Builder builder() {
      return new BrowseNextRequest.Builder();
   }

   @Override
   public Object get(FieldSpecification var1) {
      if (BrowseNextRequest.Fields.RequestHeader.equals(var1)) {
         return this.getRequestHeader();
      } else if (BrowseNextRequest.Fields.ReleaseContinuationPoints.equals(var1)) {
         return this.getReleaseContinuationPoints();
      } else if (BrowseNextRequest.Fields.ContinuationPoints.equals(var1)) {
         return this.getContinuationPoints();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   @Override
   public void set(FieldSpecification var1, Object var2) {
      if (BrowseNextRequest.Fields.RequestHeader.equals(var1)) {
         this.setRequestHeader((RequestHeader)var2);
      } else if (BrowseNextRequest.Fields.ReleaseContinuationPoints.equals(var1)) {
         this.setReleaseContinuationPoints((Boolean)var2);
      } else if (BrowseNextRequest.Fields.ContinuationPoints.equals(var1)) {
         this.setContinuationPoints((ByteString[])var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public BrowseNextRequest.Builder toBuilder() {
      BrowseNextRequest.Builder var1 = builder();
      var1.setRequestHeader((RequestHeader)StructureUtils.clone(this.getRequestHeader()));
      var1.setReleaseContinuationPoints((Boolean)StructureUtils.clone(this.getReleaseContinuationPoints()));
      var1.setContinuationPoints((ByteString[])StructureUtils.clone(this.getContinuationPoints()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(BrowseNextRequest.Fields.RequestHeader);
      var0.addField(BrowseNextRequest.Fields.ReleaseContinuationPoints);
      var0.addField(BrowseNextRequest.Fields.ContinuationPoints);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("BrowseNextRequest");
      var0.setJavaClass(BrowseNextRequest.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(BrowseNextRequest.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private RequestHeader f_requestHeader;
      private Boolean f_releaseContinuationPoints;
      private ByteString[] f_continuationPoints;

      protected Builder() {
      }

      public RequestHeader getRequestHeader() {
         return this.f_requestHeader;
      }

      public BrowseNextRequest.Builder setRequestHeader(RequestHeader var1) {
         this.f_requestHeader = var1;
         return this;
      }

      public Boolean getReleaseContinuationPoints() {
         return this.f_releaseContinuationPoints;
      }

      public BrowseNextRequest.Builder setReleaseContinuationPoints(Boolean var1) {
         this.f_releaseContinuationPoints = var1;
         return this;
      }

      public ByteString[] getContinuationPoints() {
         return this.f_continuationPoints;
      }

      public BrowseNextRequest.Builder setContinuationPoints(ByteString[] var1) {
         this.f_continuationPoints = var1;
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
            BrowseNextRequest.Builder var2 = (BrowseNextRequest.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getReleaseContinuationPoints(), var2.getReleaseContinuationPoints())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getContinuationPoints(), var2.getContinuationPoints());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getReleaseContinuationPoints(), this.getContinuationPoints()});
      }

      public Object get(FieldSpecification var1) {
         if (BrowseNextRequest.Fields.RequestHeader.equals(var1)) {
            return this.getRequestHeader();
         } else if (BrowseNextRequest.Fields.ReleaseContinuationPoints.equals(var1)) {
            return this.getReleaseContinuationPoints();
         } else if (BrowseNextRequest.Fields.ContinuationPoints.equals(var1)) {
            return this.getContinuationPoints();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseNextRequest.Builder set(FieldSpecification var1, Object var2) {
         if (BrowseNextRequest.Fields.RequestHeader.equals(var1)) {
            this.setRequestHeader((RequestHeader)var2);
            return this;
         } else if (BrowseNextRequest.Fields.ReleaseContinuationPoints.equals(var1)) {
            this.setReleaseContinuationPoints((Boolean)var2);
            return this;
         } else if (BrowseNextRequest.Fields.ContinuationPoints.equals(var1)) {
            this.setContinuationPoints((ByteString[])var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseNextRequest.Builder clear() {
         super.clear();
         this.f_requestHeader = null;
         this.f_releaseContinuationPoints = null;
         this.f_continuationPoints = null;
         return this;
      }

      public StructureSpecification specification() {
         return BrowseNextRequest.SPECIFICATION;
      }

      public BrowseNextRequest build() {
         return new BrowseNextRequest(this.f_requestHeader, this.f_releaseContinuationPoints, this.f_continuationPoints);
      }
   }

   public static enum Fields implements FieldSpecification {
      RequestHeader("RequestHeader", RequestHeader.class, false, UaIds.RequestHeader, -1, null, false),
      ReleaseContinuationPoints("ReleaseContinuationPoints", Boolean.class, false, UaIds.Boolean, -1, null, false),
      ContinuationPoints("ContinuationPoints", ByteString[].class, false, UaIds.ContinuationPoint, 1, UaArrayDimensions.valueOf(new long[]{0L}), false);

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
