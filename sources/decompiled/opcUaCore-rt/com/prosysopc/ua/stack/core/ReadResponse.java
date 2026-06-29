package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.ServiceResponse;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=632")
public class ReadResponse extends AbstractStructure implements ServiceResponse {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.ReadResponse_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.ReadResponse_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.ReadResponse_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.ReadResponse;
   public static final StructureSpecification SPECIFICATION;
   private ResponseHeader f_responseHeader;
   private DataValue[] f_results;
   private DiagnosticInfo[] f_diagnosticInfos;

   public ReadResponse() {
   }

   public ReadResponse(ResponseHeader var1, DataValue[] var2, DiagnosticInfo[] var3) {
      this.f_responseHeader = var1;
      this.f_results = var2;
      this.f_diagnosticInfos = var3;
   }

   @Override
   public ResponseHeader getResponseHeader() {
      return this.f_responseHeader;
   }

   @Override
   public void setResponseHeader(ResponseHeader var1) {
      this.f_responseHeader = var1;
   }

   public DataValue[] getResults() {
      return this.f_results;
   }

   public void setResults(DataValue[] var1) {
      this.f_results = var1;
   }

   public DiagnosticInfo[] getDiagnosticInfos() {
      return this.f_diagnosticInfos;
   }

   public void setDiagnosticInfos(DiagnosticInfo[] var1) {
      this.f_diagnosticInfos = var1;
   }

   public ReadResponse clone() {
      ReadResponse var1 = (ReadResponse)super.clone();
      var1.f_responseHeader = (ResponseHeader)StructureUtils.clone(this.f_responseHeader);
      var1.f_results = (DataValue[])StructureUtils.clone(this.f_results);
      var1.f_diagnosticInfos = (DiagnosticInfo[])StructureUtils.clone(this.f_diagnosticInfos);
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
         ReadResponse var2 = (ReadResponse)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getResponseHeader(), var2.getResponseHeader())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getResults(), var2.getResults())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getDiagnosticInfos(), var2.getDiagnosticInfos());
         }
      }
   }

   @Override
   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getResponseHeader(), this.getResults(), this.getDiagnosticInfos()});
   }

   @Override
   public void clear() {
      super.clear();
      this.f_responseHeader = null;
      this.f_results = null;
      this.f_diagnosticInfos = null;
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
      var1.put(ReadResponse.Fields.ResponseHeader, this.getResponseHeader());
      var1.put(ReadResponse.Fields.Results, this.getResults());
      var1.put(ReadResponse.Fields.DiagnosticInfos, this.getDiagnosticInfos());
      return Collections.unmodifiableMap(var1);
   }

   @Override
   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static ReadResponse.Builder builder() {
      return new ReadResponse.Builder();
   }

   @Override
   public Object get(FieldSpecification var1) {
      if (ReadResponse.Fields.ResponseHeader.equals(var1)) {
         return this.getResponseHeader();
      } else if (ReadResponse.Fields.Results.equals(var1)) {
         return this.getResults();
      } else if (ReadResponse.Fields.DiagnosticInfos.equals(var1)) {
         return this.getDiagnosticInfos();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   @Override
   public void set(FieldSpecification var1, Object var2) {
      if (ReadResponse.Fields.ResponseHeader.equals(var1)) {
         this.setResponseHeader((ResponseHeader)var2);
      } else if (ReadResponse.Fields.Results.equals(var1)) {
         this.setResults((DataValue[])var2);
      } else if (ReadResponse.Fields.DiagnosticInfos.equals(var1)) {
         this.setDiagnosticInfos((DiagnosticInfo[])var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public ReadResponse.Builder toBuilder() {
      ReadResponse.Builder var1 = builder();
      var1.setResponseHeader((ResponseHeader)StructureUtils.clone(this.getResponseHeader()));
      var1.setResults((DataValue[])StructureUtils.clone(this.getResults()));
      var1.setDiagnosticInfos((DiagnosticInfo[])StructureUtils.clone(this.getDiagnosticInfos()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(ReadResponse.Fields.ResponseHeader);
      var0.addField(ReadResponse.Fields.Results);
      var0.addField(ReadResponse.Fields.DiagnosticInfos);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("ReadResponse");
      var0.setJavaClass(ReadResponse.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(ReadResponse.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private ResponseHeader f_responseHeader;
      private DataValue[] f_results;
      private DiagnosticInfo[] f_diagnosticInfos;

      protected Builder() {
      }

      public ResponseHeader getResponseHeader() {
         return this.f_responseHeader;
      }

      public ReadResponse.Builder setResponseHeader(ResponseHeader var1) {
         this.f_responseHeader = var1;
         return this;
      }

      public DataValue[] getResults() {
         return this.f_results;
      }

      public ReadResponse.Builder setResults(DataValue[] var1) {
         this.f_results = var1;
         return this;
      }

      public DiagnosticInfo[] getDiagnosticInfos() {
         return this.f_diagnosticInfos;
      }

      public ReadResponse.Builder setDiagnosticInfos(DiagnosticInfo[] var1) {
         this.f_diagnosticInfos = var1;
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
            ReadResponse.Builder var2 = (ReadResponse.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getResponseHeader(), var2.getResponseHeader())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getResults(), var2.getResults())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getDiagnosticInfos(), var2.getDiagnosticInfos());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getResponseHeader(), this.getResults(), this.getDiagnosticInfos()});
      }

      public Object get(FieldSpecification var1) {
         if (ReadResponse.Fields.ResponseHeader.equals(var1)) {
            return this.getResponseHeader();
         } else if (ReadResponse.Fields.Results.equals(var1)) {
            return this.getResults();
         } else if (ReadResponse.Fields.DiagnosticInfos.equals(var1)) {
            return this.getDiagnosticInfos();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ReadResponse.Builder set(FieldSpecification var1, Object var2) {
         if (ReadResponse.Fields.ResponseHeader.equals(var1)) {
            this.setResponseHeader((ResponseHeader)var2);
            return this;
         } else if (ReadResponse.Fields.Results.equals(var1)) {
            this.setResults((DataValue[])var2);
            return this;
         } else if (ReadResponse.Fields.DiagnosticInfos.equals(var1)) {
            this.setDiagnosticInfos((DiagnosticInfo[])var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ReadResponse.Builder clear() {
         super.clear();
         this.f_responseHeader = null;
         this.f_results = null;
         this.f_diagnosticInfos = null;
         return this;
      }

      public StructureSpecification specification() {
         return ReadResponse.SPECIFICATION;
      }

      public ReadResponse build() {
         return new ReadResponse(this.f_responseHeader, this.f_results, this.f_diagnosticInfos);
      }
   }

   public static enum Fields implements FieldSpecification {
      ResponseHeader("ResponseHeader", ResponseHeader.class, false, UaIds.ResponseHeader, -1, null, false),
      Results("Results", DataValue[].class, false, UaIds.DataValue, 1, UaArrayDimensions.valueOf(new long[]{0L}), false),
      DiagnosticInfos("DiagnosticInfos", DiagnosticInfo[].class, false, UaIds.DiagnosticInfo, 1, UaArrayDimensions.valueOf(new long[]{0L}), false);

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
