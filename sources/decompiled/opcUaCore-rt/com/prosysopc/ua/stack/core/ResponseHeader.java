package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.ExtensionObject;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=392")
public class ResponseHeader extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.ResponseHeader_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.ResponseHeader_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.ResponseHeader_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.ResponseHeader;
   public static final StructureSpecification SPECIFICATION;
   private DateTime f_timestamp;
   private UnsignedInteger f_requestHandle;
   private StatusCode f_serviceResult;
   private DiagnosticInfo f_serviceDiagnostics;
   private String[] f_stringTable;
   private ExtensionObject f_additionalHeader;

   public ResponseHeader() {
   }

   public ResponseHeader(DateTime var1, UnsignedInteger var2, StatusCode var3, DiagnosticInfo var4, String[] var5, ExtensionObject var6) {
      this.f_timestamp = var1;
      this.f_requestHandle = var2;
      this.f_serviceResult = var3;
      this.f_serviceDiagnostics = var4;
      this.f_stringTable = var5;
      this.f_additionalHeader = var6;
   }

   public DateTime getTimestamp() {
      return this.f_timestamp;
   }

   public void setTimestamp(DateTime var1) {
      this.f_timestamp = var1;
   }

   public UnsignedInteger getRequestHandle() {
      return this.f_requestHandle;
   }

   public void setRequestHandle(UnsignedInteger var1) {
      this.f_requestHandle = var1;
   }

   public StatusCode getServiceResult() {
      return this.f_serviceResult;
   }

   public void setServiceResult(StatusCode var1) {
      this.f_serviceResult = var1;
   }

   public DiagnosticInfo getServiceDiagnostics() {
      return this.f_serviceDiagnostics;
   }

   public void setServiceDiagnostics(DiagnosticInfo var1) {
      this.f_serviceDiagnostics = var1;
   }

   public String[] getStringTable() {
      return this.f_stringTable;
   }

   public void setStringTable(String[] var1) {
      this.f_stringTable = var1;
   }

   public ExtensionObject getAdditionalHeader() {
      return this.f_additionalHeader;
   }

   public void setAdditionalHeader(ExtensionObject var1) {
      this.f_additionalHeader = var1;
   }

   public ResponseHeader clone() {
      ResponseHeader var1 = (ResponseHeader)super.clone();
      var1.f_timestamp = (DateTime)StructureUtils.clone(this.f_timestamp);
      var1.f_requestHandle = (UnsignedInteger)StructureUtils.clone(this.f_requestHandle);
      var1.f_serviceResult = (StatusCode)StructureUtils.clone(this.f_serviceResult);
      var1.f_serviceDiagnostics = (DiagnosticInfo)StructureUtils.clone(this.f_serviceDiagnostics);
      var1.f_stringTable = (String[])StructureUtils.clone(this.f_stringTable);
      var1.f_additionalHeader = (ExtensionObject)StructureUtils.clone(this.f_additionalHeader);
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
         ResponseHeader var2 = (ResponseHeader)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getTimestamp(), var2.getTimestamp())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getRequestHandle(), var2.getRequestHandle())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getServiceResult(), var2.getServiceResult())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getServiceDiagnostics(), var2.getServiceDiagnostics())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getStringTable(), var2.getStringTable())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getAdditionalHeader(), var2.getAdditionalHeader());
         }
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(
         new Object[]{
            this.getTimestamp(),
            this.getRequestHandle(),
            this.getServiceResult(),
            this.getServiceDiagnostics(),
            this.getStringTable(),
            this.getAdditionalHeader()
         }
      );
   }

   public void clear() {
      super.clear();
      this.f_timestamp = null;
      this.f_requestHandle = null;
      this.f_serviceResult = null;
      this.f_serviceDiagnostics = null;
      this.f_stringTable = null;
      this.f_additionalHeader = null;
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
      var1.put(ResponseHeader.Fields.Timestamp, this.getTimestamp());
      var1.put(ResponseHeader.Fields.RequestHandle, this.getRequestHandle());
      var1.put(ResponseHeader.Fields.ServiceResult, this.getServiceResult());
      var1.put(ResponseHeader.Fields.ServiceDiagnostics, this.getServiceDiagnostics());
      var1.put(ResponseHeader.Fields.StringTable, this.getStringTable());
      var1.put(ResponseHeader.Fields.AdditionalHeader, this.getAdditionalHeader());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static ResponseHeader.Builder builder() {
      return new ResponseHeader.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (ResponseHeader.Fields.Timestamp.equals(var1)) {
         return this.getTimestamp();
      } else if (ResponseHeader.Fields.RequestHandle.equals(var1)) {
         return this.getRequestHandle();
      } else if (ResponseHeader.Fields.ServiceResult.equals(var1)) {
         return this.getServiceResult();
      } else if (ResponseHeader.Fields.ServiceDiagnostics.equals(var1)) {
         return this.getServiceDiagnostics();
      } else if (ResponseHeader.Fields.StringTable.equals(var1)) {
         return this.getStringTable();
      } else if (ResponseHeader.Fields.AdditionalHeader.equals(var1)) {
         return this.getAdditionalHeader();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (ResponseHeader.Fields.Timestamp.equals(var1)) {
         this.setTimestamp((DateTime)var2);
      } else if (ResponseHeader.Fields.RequestHandle.equals(var1)) {
         this.setRequestHandle((UnsignedInteger)var2);
      } else if (ResponseHeader.Fields.ServiceResult.equals(var1)) {
         this.setServiceResult((StatusCode)var2);
      } else if (ResponseHeader.Fields.ServiceDiagnostics.equals(var1)) {
         this.setServiceDiagnostics((DiagnosticInfo)var2);
      } else if (ResponseHeader.Fields.StringTable.equals(var1)) {
         this.setStringTable((String[])var2);
      } else if (ResponseHeader.Fields.AdditionalHeader.equals(var1)) {
         this.setAdditionalHeader((ExtensionObject)var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public ResponseHeader.Builder toBuilder() {
      ResponseHeader.Builder var1 = builder();
      var1.setTimestamp((DateTime)StructureUtils.clone(this.getTimestamp()));
      var1.setRequestHandle((UnsignedInteger)StructureUtils.clone(this.getRequestHandle()));
      var1.setServiceResult((StatusCode)StructureUtils.clone(this.getServiceResult()));
      var1.setServiceDiagnostics((DiagnosticInfo)StructureUtils.clone(this.getServiceDiagnostics()));
      var1.setStringTable((String[])StructureUtils.clone(this.getStringTable()));
      var1.setAdditionalHeader((ExtensionObject)StructureUtils.clone(this.getAdditionalHeader()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(ResponseHeader.Fields.Timestamp);
      var0.addField(ResponseHeader.Fields.RequestHandle);
      var0.addField(ResponseHeader.Fields.ServiceResult);
      var0.addField(ResponseHeader.Fields.ServiceDiagnostics);
      var0.addField(ResponseHeader.Fields.StringTable);
      var0.addField(ResponseHeader.Fields.AdditionalHeader);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("ResponseHeader");
      var0.setJavaClass(ResponseHeader.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(ResponseHeader.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private DateTime f_timestamp;
      private UnsignedInteger f_requestHandle;
      private StatusCode f_serviceResult;
      private DiagnosticInfo f_serviceDiagnostics;
      private String[] f_stringTable;
      private ExtensionObject f_additionalHeader;

      protected Builder() {
      }

      public DateTime getTimestamp() {
         return this.f_timestamp;
      }

      public ResponseHeader.Builder setTimestamp(DateTime var1) {
         this.f_timestamp = var1;
         return this;
      }

      public UnsignedInteger getRequestHandle() {
         return this.f_requestHandle;
      }

      public ResponseHeader.Builder setRequestHandle(UnsignedInteger var1) {
         this.f_requestHandle = var1;
         return this;
      }

      public StatusCode getServiceResult() {
         return this.f_serviceResult;
      }

      public ResponseHeader.Builder setServiceResult(StatusCode var1) {
         this.f_serviceResult = var1;
         return this;
      }

      public DiagnosticInfo getServiceDiagnostics() {
         return this.f_serviceDiagnostics;
      }

      public ResponseHeader.Builder setServiceDiagnostics(DiagnosticInfo var1) {
         this.f_serviceDiagnostics = var1;
         return this;
      }

      public String[] getStringTable() {
         return this.f_stringTable;
      }

      public ResponseHeader.Builder setStringTable(String[] var1) {
         this.f_stringTable = var1;
         return this;
      }

      public ExtensionObject getAdditionalHeader() {
         return this.f_additionalHeader;
      }

      public ResponseHeader.Builder setAdditionalHeader(ExtensionObject var1) {
         this.f_additionalHeader = var1;
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
            ResponseHeader.Builder var2 = (ResponseHeader.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getTimestamp(), var2.getTimestamp())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getRequestHandle(), var2.getRequestHandle())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getServiceResult(), var2.getServiceResult())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getServiceDiagnostics(), var2.getServiceDiagnostics())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getStringTable(), var2.getStringTable())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getAdditionalHeader(), var2.getAdditionalHeader());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(
            new Object[]{
               this.getTimestamp(),
               this.getRequestHandle(),
               this.getServiceResult(),
               this.getServiceDiagnostics(),
               this.getStringTable(),
               this.getAdditionalHeader()
            }
         );
      }

      public Object get(FieldSpecification var1) {
         if (ResponseHeader.Fields.Timestamp.equals(var1)) {
            return this.getTimestamp();
         } else if (ResponseHeader.Fields.RequestHandle.equals(var1)) {
            return this.getRequestHandle();
         } else if (ResponseHeader.Fields.ServiceResult.equals(var1)) {
            return this.getServiceResult();
         } else if (ResponseHeader.Fields.ServiceDiagnostics.equals(var1)) {
            return this.getServiceDiagnostics();
         } else if (ResponseHeader.Fields.StringTable.equals(var1)) {
            return this.getStringTable();
         } else if (ResponseHeader.Fields.AdditionalHeader.equals(var1)) {
            return this.getAdditionalHeader();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ResponseHeader.Builder set(FieldSpecification var1, Object var2) {
         if (ResponseHeader.Fields.Timestamp.equals(var1)) {
            this.setTimestamp((DateTime)var2);
            return this;
         } else if (ResponseHeader.Fields.RequestHandle.equals(var1)) {
            this.setRequestHandle((UnsignedInteger)var2);
            return this;
         } else if (ResponseHeader.Fields.ServiceResult.equals(var1)) {
            this.setServiceResult((StatusCode)var2);
            return this;
         } else if (ResponseHeader.Fields.ServiceDiagnostics.equals(var1)) {
            this.setServiceDiagnostics((DiagnosticInfo)var2);
            return this;
         } else if (ResponseHeader.Fields.StringTable.equals(var1)) {
            this.setStringTable((String[])var2);
            return this;
         } else if (ResponseHeader.Fields.AdditionalHeader.equals(var1)) {
            this.setAdditionalHeader((ExtensionObject)var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ResponseHeader.Builder clear() {
         super.clear();
         this.f_timestamp = null;
         this.f_requestHandle = null;
         this.f_serviceResult = null;
         this.f_serviceDiagnostics = null;
         this.f_stringTable = null;
         this.f_additionalHeader = null;
         return this;
      }

      public StructureSpecification specification() {
         return ResponseHeader.SPECIFICATION;
      }

      public ResponseHeader build() {
         return new ResponseHeader(
            this.f_timestamp, this.f_requestHandle, this.f_serviceResult, this.f_serviceDiagnostics, this.f_stringTable, this.f_additionalHeader
         );
      }
   }

   public static enum Fields implements FieldSpecification {
      Timestamp("Timestamp", DateTime.class, false, UaIds.UtcTime, -1, null, false),
      RequestHandle("RequestHandle", UnsignedInteger.class, false, UaIds.IntegerId, -1, null, false),
      ServiceResult("ServiceResult", StatusCode.class, false, UaIds.StatusCode, -1, null, false),
      ServiceDiagnostics("ServiceDiagnostics", DiagnosticInfo.class, false, UaIds.DiagnosticInfo, -1, null, false),
      StringTable("StringTable", String[].class, false, UaIds.String, 1, UaArrayDimensions.valueOf(new long[]{0L}), false),
      AdditionalHeader("AdditionalHeader", ExtensionObject.class, false, UaIds.Structure, -1, null, false);

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
