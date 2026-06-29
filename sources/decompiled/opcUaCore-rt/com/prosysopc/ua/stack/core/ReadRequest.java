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

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=629")
public class ReadRequest extends AbstractStructure implements ServiceRequest<ReadResponse> {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.ReadRequest_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.ReadRequest_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.ReadRequest_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.ReadRequest;
   public static final StructureSpecification SPECIFICATION;
   private RequestHeader f_requestHeader;
   private Double f_maxAge;
   private TimestampsToReturn f_timestampsToReturn;
   private ReadValueId[] f_nodesToRead;

   public ReadRequest() {
   }

   public ReadRequest(RequestHeader var1, Double var2, TimestampsToReturn var3, ReadValueId[] var4) {
      this.f_requestHeader = var1;
      this.f_maxAge = var2;
      this.f_timestampsToReturn = var3;
      this.f_nodesToRead = var4;
   }

   @Override
   public RequestHeader getRequestHeader() {
      return this.f_requestHeader;
   }

   @Override
   public void setRequestHeader(RequestHeader var1) {
      this.f_requestHeader = var1;
   }

   public Double getMaxAge() {
      return this.f_maxAge;
   }

   public void setMaxAge(Double var1) {
      this.f_maxAge = var1;
   }

   public TimestampsToReturn getTimestampsToReturn() {
      return this.f_timestampsToReturn;
   }

   public void setTimestampsToReturn(TimestampsToReturn var1) {
      this.f_timestampsToReturn = var1;
   }

   public ReadValueId[] getNodesToRead() {
      return this.f_nodesToRead;
   }

   public void setNodesToRead(ReadValueId[] var1) {
      this.f_nodesToRead = var1;
   }

   public ReadRequest clone() {
      ReadRequest var1 = (ReadRequest)super.clone();
      var1.f_requestHeader = (RequestHeader)StructureUtils.clone(this.f_requestHeader);
      var1.f_maxAge = (Double)StructureUtils.clone(this.f_maxAge);
      var1.f_timestampsToReturn = (TimestampsToReturn)StructureUtils.clone(this.f_timestampsToReturn);
      var1.f_nodesToRead = (ReadValueId[])StructureUtils.clone(this.f_nodesToRead);
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
         ReadRequest var2 = (ReadRequest)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getMaxAge(), var2.getMaxAge())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getTimestampsToReturn(), var2.getTimestampsToReturn())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getNodesToRead(), var2.getNodesToRead());
         }
      }
   }

   @Override
   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getMaxAge(), this.getTimestampsToReturn(), this.getNodesToRead()});
   }

   @Override
   public void clear() {
      super.clear();
      this.f_requestHeader = null;
      this.f_maxAge = null;
      this.f_timestampsToReturn = null;
      this.f_nodesToRead = null;
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
      var1.put(ReadRequest.Fields.RequestHeader, this.getRequestHeader());
      var1.put(ReadRequest.Fields.MaxAge, this.getMaxAge());
      var1.put(ReadRequest.Fields.TimestampsToReturn, this.getTimestampsToReturn());
      var1.put(ReadRequest.Fields.NodesToRead, this.getNodesToRead());
      return Collections.unmodifiableMap(var1);
   }

   @Override
   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static ReadRequest.Builder builder() {
      return new ReadRequest.Builder();
   }

   @Override
   public Object get(FieldSpecification var1) {
      if (ReadRequest.Fields.RequestHeader.equals(var1)) {
         return this.getRequestHeader();
      } else if (ReadRequest.Fields.MaxAge.equals(var1)) {
         return this.getMaxAge();
      } else if (ReadRequest.Fields.TimestampsToReturn.equals(var1)) {
         return this.getTimestampsToReturn();
      } else if (ReadRequest.Fields.NodesToRead.equals(var1)) {
         return this.getNodesToRead();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   @Override
   public void set(FieldSpecification var1, Object var2) {
      if (ReadRequest.Fields.RequestHeader.equals(var1)) {
         this.setRequestHeader((RequestHeader)var2);
      } else if (ReadRequest.Fields.MaxAge.equals(var1)) {
         this.setMaxAge((Double)var2);
      } else if (ReadRequest.Fields.TimestampsToReturn.equals(var1)) {
         this.setTimestampsToReturn((TimestampsToReturn)var2);
      } else if (ReadRequest.Fields.NodesToRead.equals(var1)) {
         this.setNodesToRead((ReadValueId[])var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public ReadRequest.Builder toBuilder() {
      ReadRequest.Builder var1 = builder();
      var1.setRequestHeader((RequestHeader)StructureUtils.clone(this.getRequestHeader()));
      var1.setMaxAge((Double)StructureUtils.clone(this.getMaxAge()));
      var1.setTimestampsToReturn((TimestampsToReturn)StructureUtils.clone(this.getTimestampsToReturn()));
      var1.setNodesToRead((ReadValueId[])StructureUtils.clone(this.getNodesToRead()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(ReadRequest.Fields.RequestHeader);
      var0.addField(ReadRequest.Fields.MaxAge);
      var0.addField(ReadRequest.Fields.TimestampsToReturn);
      var0.addField(ReadRequest.Fields.NodesToRead);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("ReadRequest");
      var0.setJavaClass(ReadRequest.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(ReadRequest.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private RequestHeader f_requestHeader;
      private Double f_maxAge;
      private TimestampsToReturn f_timestampsToReturn;
      private ReadValueId[] f_nodesToRead;

      protected Builder() {
      }

      public RequestHeader getRequestHeader() {
         return this.f_requestHeader;
      }

      public ReadRequest.Builder setRequestHeader(RequestHeader var1) {
         this.f_requestHeader = var1;
         return this;
      }

      public Double getMaxAge() {
         return this.f_maxAge;
      }

      public ReadRequest.Builder setMaxAge(Double var1) {
         this.f_maxAge = var1;
         return this;
      }

      public TimestampsToReturn getTimestampsToReturn() {
         return this.f_timestampsToReturn;
      }

      public ReadRequest.Builder setTimestampsToReturn(TimestampsToReturn var1) {
         this.f_timestampsToReturn = var1;
         return this;
      }

      public ReadValueId[] getNodesToRead() {
         return this.f_nodesToRead;
      }

      public ReadRequest.Builder setNodesToRead(ReadValueId[] var1) {
         this.f_nodesToRead = var1;
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
            ReadRequest.Builder var2 = (ReadRequest.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getMaxAge(), var2.getMaxAge())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getTimestampsToReturn(), var2.getTimestampsToReturn())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getNodesToRead(), var2.getNodesToRead());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getMaxAge(), this.getTimestampsToReturn(), this.getNodesToRead()});
      }

      public Object get(FieldSpecification var1) {
         if (ReadRequest.Fields.RequestHeader.equals(var1)) {
            return this.getRequestHeader();
         } else if (ReadRequest.Fields.MaxAge.equals(var1)) {
            return this.getMaxAge();
         } else if (ReadRequest.Fields.TimestampsToReturn.equals(var1)) {
            return this.getTimestampsToReturn();
         } else if (ReadRequest.Fields.NodesToRead.equals(var1)) {
            return this.getNodesToRead();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ReadRequest.Builder set(FieldSpecification var1, Object var2) {
         if (ReadRequest.Fields.RequestHeader.equals(var1)) {
            this.setRequestHeader((RequestHeader)var2);
            return this;
         } else if (ReadRequest.Fields.MaxAge.equals(var1)) {
            this.setMaxAge((Double)var2);
            return this;
         } else if (ReadRequest.Fields.TimestampsToReturn.equals(var1)) {
            this.setTimestampsToReturn((TimestampsToReturn)var2);
            return this;
         } else if (ReadRequest.Fields.NodesToRead.equals(var1)) {
            this.setNodesToRead((ReadValueId[])var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ReadRequest.Builder clear() {
         super.clear();
         this.f_requestHeader = null;
         this.f_maxAge = null;
         this.f_timestampsToReturn = null;
         this.f_nodesToRead = null;
         return this;
      }

      public StructureSpecification specification() {
         return ReadRequest.SPECIFICATION;
      }

      public ReadRequest build() {
         return new ReadRequest(this.f_requestHeader, this.f_maxAge, this.f_timestampsToReturn, this.f_nodesToRead);
      }
   }

   public static enum Fields implements FieldSpecification {
      RequestHeader("RequestHeader", RequestHeader.class, false, UaIds.RequestHeader, -1, null, false),
      MaxAge("MaxAge", Double.class, false, UaIds.Duration, -1, null, false),
      TimestampsToReturn("TimestampsToReturn", TimestampsToReturn.class, false, UaIds.TimestampsToReturn, -1, null, false),
      NodesToRead("NodesToRead", ReadValueId[].class, false, UaIds.ReadValueId, 1, UaArrayDimensions.valueOf(new long[]{0L}), false);

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
