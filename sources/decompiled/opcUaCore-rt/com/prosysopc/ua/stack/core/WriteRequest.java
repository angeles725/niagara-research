package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
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

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=671")
public class WriteRequest extends AbstractStructure implements ServiceRequest<WriteResponse> {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.WriteRequest_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.WriteRequest_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.WriteRequest_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.WriteRequest;
   public static final StructureSpecification SPECIFICATION;
   private RequestHeader f_requestHeader;
   private WriteValue[] f_nodesToWrite;

   public WriteRequest() {
   }

   public WriteRequest(RequestHeader var1, WriteValue[] var2) {
      this.f_requestHeader = var1;
      this.f_nodesToWrite = var2;
   }

   @Override
   public RequestHeader getRequestHeader() {
      return this.f_requestHeader;
   }

   @Override
   public void setRequestHeader(RequestHeader var1) {
      this.f_requestHeader = var1;
   }

   public WriteValue[] getNodesToWrite() {
      return this.f_nodesToWrite;
   }

   public void setNodesToWrite(WriteValue[] var1) {
      this.f_nodesToWrite = var1;
   }

   public WriteRequest clone() {
      WriteRequest var1 = (WriteRequest)super.clone();
      var1.f_requestHeader = (RequestHeader)StructureUtils.clone(this.f_requestHeader);
      var1.f_nodesToWrite = (WriteValue[])StructureUtils.clone(this.f_nodesToWrite);
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
         WriteRequest var2 = (WriteRequest)var1;
         return !StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())
            ? false
            : StructureUtils.scalarOrArrayEquals(this.getNodesToWrite(), var2.getNodesToWrite());
      }
   }

   @Override
   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getNodesToWrite()});
   }

   @Override
   public void clear() {
      super.clear();
      this.f_requestHeader = null;
      this.f_nodesToWrite = null;
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
      var1.put(WriteRequest.Fields.RequestHeader, this.getRequestHeader());
      var1.put(WriteRequest.Fields.NodesToWrite, this.getNodesToWrite());
      return Collections.unmodifiableMap(var1);
   }

   @Override
   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static WriteRequest.Builder builder() {
      return new WriteRequest.Builder();
   }

   @Override
   public Object get(FieldSpecification var1) {
      if (WriteRequest.Fields.RequestHeader.equals(var1)) {
         return this.getRequestHeader();
      } else if (WriteRequest.Fields.NodesToWrite.equals(var1)) {
         return this.getNodesToWrite();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   @Override
   public void set(FieldSpecification var1, Object var2) {
      if (WriteRequest.Fields.RequestHeader.equals(var1)) {
         this.setRequestHeader((RequestHeader)var2);
      } else if (WriteRequest.Fields.NodesToWrite.equals(var1)) {
         this.setNodesToWrite((WriteValue[])var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public WriteRequest.Builder toBuilder() {
      WriteRequest.Builder var1 = builder();
      var1.setRequestHeader((RequestHeader)StructureUtils.clone(this.getRequestHeader()));
      var1.setNodesToWrite((WriteValue[])StructureUtils.clone(this.getNodesToWrite()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(WriteRequest.Fields.RequestHeader);
      var0.addField(WriteRequest.Fields.NodesToWrite);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("WriteRequest");
      var0.setJavaClass(WriteRequest.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(WriteRequest.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private RequestHeader f_requestHeader;
      private WriteValue[] f_nodesToWrite;

      protected Builder() {
      }

      public RequestHeader getRequestHeader() {
         return this.f_requestHeader;
      }

      public WriteRequest.Builder setRequestHeader(RequestHeader var1) {
         this.f_requestHeader = var1;
         return this;
      }

      public WriteValue[] getNodesToWrite() {
         return this.f_nodesToWrite;
      }

      public WriteRequest.Builder setNodesToWrite(WriteValue[] var1) {
         this.f_nodesToWrite = var1;
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
            WriteRequest.Builder var2 = (WriteRequest.Builder)var1;
            return !StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getNodesToWrite(), var2.getNodesToWrite());
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getNodesToWrite()});
      }

      public Object get(FieldSpecification var1) {
         if (WriteRequest.Fields.RequestHeader.equals(var1)) {
            return this.getRequestHeader();
         } else if (WriteRequest.Fields.NodesToWrite.equals(var1)) {
            return this.getNodesToWrite();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public WriteRequest.Builder set(FieldSpecification var1, Object var2) {
         if (WriteRequest.Fields.RequestHeader.equals(var1)) {
            this.setRequestHeader((RequestHeader)var2);
            return this;
         } else if (WriteRequest.Fields.NodesToWrite.equals(var1)) {
            this.setNodesToWrite((WriteValue[])var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public WriteRequest.Builder clear() {
         super.clear();
         this.f_requestHeader = null;
         this.f_nodesToWrite = null;
         return this;
      }

      public StructureSpecification specification() {
         return WriteRequest.SPECIFICATION;
      }

      public WriteRequest build() {
         return new WriteRequest(this.f_requestHeader, this.f_nodesToWrite);
      }
   }

   public static enum Fields implements FieldSpecification {
      RequestHeader("RequestHeader", RequestHeader.class, false, UaIds.RequestHeader, -1, null, false),
      NodesToWrite("NodesToWrite", WriteValue[].class, false, UaIds.WriteValue, 1, UaArrayDimensions.valueOf(new long[]{0L}), false);

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
