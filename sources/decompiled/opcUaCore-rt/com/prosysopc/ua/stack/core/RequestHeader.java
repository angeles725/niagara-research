package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.ExtensionObject;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=389")
public class RequestHeader extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.RequestHeader_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.RequestHeader_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.RequestHeader_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.RequestHeader;
   public static final StructureSpecification SPECIFICATION;
   private NodeId f_authenticationToken;
   private DateTime f_timestamp;
   private UnsignedInteger f_requestHandle;
   private UnsignedInteger f_returnDiagnostics;
   private String f_auditEntryId;
   private UnsignedInteger f_timeoutHint;
   private ExtensionObject f_additionalHeader;

   public RequestHeader() {
   }

   public RequestHeader(NodeId var1, DateTime var2, UnsignedInteger var3, UnsignedInteger var4, String var5, UnsignedInteger var6, ExtensionObject var7) {
      this.f_authenticationToken = var1;
      this.f_timestamp = var2;
      this.f_requestHandle = var3;
      this.f_returnDiagnostics = var4;
      this.f_auditEntryId = var5;
      this.f_timeoutHint = var6;
      this.f_additionalHeader = var7;
   }

   public NodeId getAuthenticationToken() {
      return this.f_authenticationToken;
   }

   public void setAuthenticationToken(NodeId var1) {
      this.f_authenticationToken = var1;
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

   public UnsignedInteger getReturnDiagnostics() {
      return this.f_returnDiagnostics;
   }

   public void setReturnDiagnostics(UnsignedInteger var1) {
      this.f_returnDiagnostics = var1;
   }

   public String getAuditEntryId() {
      return this.f_auditEntryId;
   }

   public void setAuditEntryId(String var1) {
      this.f_auditEntryId = var1;
   }

   public UnsignedInteger getTimeoutHint() {
      return this.f_timeoutHint;
   }

   public void setTimeoutHint(UnsignedInteger var1) {
      this.f_timeoutHint = var1;
   }

   public ExtensionObject getAdditionalHeader() {
      return this.f_additionalHeader;
   }

   public void setAdditionalHeader(ExtensionObject var1) {
      this.f_additionalHeader = var1;
   }

   public RequestHeader clone() {
      RequestHeader var1 = (RequestHeader)super.clone();
      var1.f_authenticationToken = (NodeId)StructureUtils.clone(this.f_authenticationToken);
      var1.f_timestamp = (DateTime)StructureUtils.clone(this.f_timestamp);
      var1.f_requestHandle = (UnsignedInteger)StructureUtils.clone(this.f_requestHandle);
      var1.f_returnDiagnostics = (UnsignedInteger)StructureUtils.clone(this.f_returnDiagnostics);
      var1.f_auditEntryId = (String)StructureUtils.clone(this.f_auditEntryId);
      var1.f_timeoutHint = (UnsignedInteger)StructureUtils.clone(this.f_timeoutHint);
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
         RequestHeader var2 = (RequestHeader)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getAuthenticationToken(), var2.getAuthenticationToken())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getTimestamp(), var2.getTimestamp())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getRequestHandle(), var2.getRequestHandle())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getReturnDiagnostics(), var2.getReturnDiagnostics())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getAuditEntryId(), var2.getAuditEntryId())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getTimeoutHint(), var2.getTimeoutHint())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getAdditionalHeader(), var2.getAdditionalHeader());
         }
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(
         new Object[]{
            this.getAuthenticationToken(),
            this.getTimestamp(),
            this.getRequestHandle(),
            this.getReturnDiagnostics(),
            this.getAuditEntryId(),
            this.getTimeoutHint(),
            this.getAdditionalHeader()
         }
      );
   }

   public void clear() {
      super.clear();
      this.f_authenticationToken = null;
      this.f_timestamp = null;
      this.f_requestHandle = null;
      this.f_returnDiagnostics = null;
      this.f_auditEntryId = null;
      this.f_timeoutHint = null;
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
      var1.put(RequestHeader.Fields.AuthenticationToken, this.getAuthenticationToken());
      var1.put(RequestHeader.Fields.Timestamp, this.getTimestamp());
      var1.put(RequestHeader.Fields.RequestHandle, this.getRequestHandle());
      var1.put(RequestHeader.Fields.ReturnDiagnostics, this.getReturnDiagnostics());
      var1.put(RequestHeader.Fields.AuditEntryId, this.getAuditEntryId());
      var1.put(RequestHeader.Fields.TimeoutHint, this.getTimeoutHint());
      var1.put(RequestHeader.Fields.AdditionalHeader, this.getAdditionalHeader());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static RequestHeader.Builder builder() {
      return new RequestHeader.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (RequestHeader.Fields.AuthenticationToken.equals(var1)) {
         return this.getAuthenticationToken();
      } else if (RequestHeader.Fields.Timestamp.equals(var1)) {
         return this.getTimestamp();
      } else if (RequestHeader.Fields.RequestHandle.equals(var1)) {
         return this.getRequestHandle();
      } else if (RequestHeader.Fields.ReturnDiagnostics.equals(var1)) {
         return this.getReturnDiagnostics();
      } else if (RequestHeader.Fields.AuditEntryId.equals(var1)) {
         return this.getAuditEntryId();
      } else if (RequestHeader.Fields.TimeoutHint.equals(var1)) {
         return this.getTimeoutHint();
      } else if (RequestHeader.Fields.AdditionalHeader.equals(var1)) {
         return this.getAdditionalHeader();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (RequestHeader.Fields.AuthenticationToken.equals(var1)) {
         this.setAuthenticationToken((NodeId)var2);
      } else if (RequestHeader.Fields.Timestamp.equals(var1)) {
         this.setTimestamp((DateTime)var2);
      } else if (RequestHeader.Fields.RequestHandle.equals(var1)) {
         this.setRequestHandle((UnsignedInteger)var2);
      } else if (RequestHeader.Fields.ReturnDiagnostics.equals(var1)) {
         this.setReturnDiagnostics((UnsignedInteger)var2);
      } else if (RequestHeader.Fields.AuditEntryId.equals(var1)) {
         this.setAuditEntryId((String)var2);
      } else if (RequestHeader.Fields.TimeoutHint.equals(var1)) {
         this.setTimeoutHint((UnsignedInteger)var2);
      } else if (RequestHeader.Fields.AdditionalHeader.equals(var1)) {
         this.setAdditionalHeader((ExtensionObject)var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public RequestHeader.Builder toBuilder() {
      RequestHeader.Builder var1 = builder();
      var1.setAuthenticationToken((NodeId)StructureUtils.clone(this.getAuthenticationToken()));
      var1.setTimestamp((DateTime)StructureUtils.clone(this.getTimestamp()));
      var1.setRequestHandle((UnsignedInteger)StructureUtils.clone(this.getRequestHandle()));
      var1.setReturnDiagnostics((UnsignedInteger)StructureUtils.clone(this.getReturnDiagnostics()));
      var1.setAuditEntryId((String)StructureUtils.clone(this.getAuditEntryId()));
      var1.setTimeoutHint((UnsignedInteger)StructureUtils.clone(this.getTimeoutHint()));
      var1.setAdditionalHeader((ExtensionObject)StructureUtils.clone(this.getAdditionalHeader()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(RequestHeader.Fields.AuthenticationToken);
      var0.addField(RequestHeader.Fields.Timestamp);
      var0.addField(RequestHeader.Fields.RequestHandle);
      var0.addField(RequestHeader.Fields.ReturnDiagnostics);
      var0.addField(RequestHeader.Fields.AuditEntryId);
      var0.addField(RequestHeader.Fields.TimeoutHint);
      var0.addField(RequestHeader.Fields.AdditionalHeader);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("RequestHeader");
      var0.setJavaClass(RequestHeader.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(RequestHeader.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private NodeId f_authenticationToken;
      private DateTime f_timestamp;
      private UnsignedInteger f_requestHandle;
      private UnsignedInteger f_returnDiagnostics;
      private String f_auditEntryId;
      private UnsignedInteger f_timeoutHint;
      private ExtensionObject f_additionalHeader;

      protected Builder() {
      }

      public NodeId getAuthenticationToken() {
         return this.f_authenticationToken;
      }

      public RequestHeader.Builder setAuthenticationToken(NodeId var1) {
         this.f_authenticationToken = var1;
         return this;
      }

      public DateTime getTimestamp() {
         return this.f_timestamp;
      }

      public RequestHeader.Builder setTimestamp(DateTime var1) {
         this.f_timestamp = var1;
         return this;
      }

      public UnsignedInteger getRequestHandle() {
         return this.f_requestHandle;
      }

      public RequestHeader.Builder setRequestHandle(UnsignedInteger var1) {
         this.f_requestHandle = var1;
         return this;
      }

      public UnsignedInteger getReturnDiagnostics() {
         return this.f_returnDiagnostics;
      }

      public RequestHeader.Builder setReturnDiagnostics(UnsignedInteger var1) {
         this.f_returnDiagnostics = var1;
         return this;
      }

      public String getAuditEntryId() {
         return this.f_auditEntryId;
      }

      public RequestHeader.Builder setAuditEntryId(String var1) {
         this.f_auditEntryId = var1;
         return this;
      }

      public UnsignedInteger getTimeoutHint() {
         return this.f_timeoutHint;
      }

      public RequestHeader.Builder setTimeoutHint(UnsignedInteger var1) {
         this.f_timeoutHint = var1;
         return this;
      }

      public ExtensionObject getAdditionalHeader() {
         return this.f_additionalHeader;
      }

      public RequestHeader.Builder setAdditionalHeader(ExtensionObject var1) {
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
            RequestHeader.Builder var2 = (RequestHeader.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getAuthenticationToken(), var2.getAuthenticationToken())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getTimestamp(), var2.getTimestamp())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getRequestHandle(), var2.getRequestHandle())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getReturnDiagnostics(), var2.getReturnDiagnostics())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getAuditEntryId(), var2.getAuditEntryId())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getTimeoutHint(), var2.getTimeoutHint())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getAdditionalHeader(), var2.getAdditionalHeader());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(
            new Object[]{
               this.getAuthenticationToken(),
               this.getTimestamp(),
               this.getRequestHandle(),
               this.getReturnDiagnostics(),
               this.getAuditEntryId(),
               this.getTimeoutHint(),
               this.getAdditionalHeader()
            }
         );
      }

      public Object get(FieldSpecification var1) {
         if (RequestHeader.Fields.AuthenticationToken.equals(var1)) {
            return this.getAuthenticationToken();
         } else if (RequestHeader.Fields.Timestamp.equals(var1)) {
            return this.getTimestamp();
         } else if (RequestHeader.Fields.RequestHandle.equals(var1)) {
            return this.getRequestHandle();
         } else if (RequestHeader.Fields.ReturnDiagnostics.equals(var1)) {
            return this.getReturnDiagnostics();
         } else if (RequestHeader.Fields.AuditEntryId.equals(var1)) {
            return this.getAuditEntryId();
         } else if (RequestHeader.Fields.TimeoutHint.equals(var1)) {
            return this.getTimeoutHint();
         } else if (RequestHeader.Fields.AdditionalHeader.equals(var1)) {
            return this.getAdditionalHeader();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public RequestHeader.Builder set(FieldSpecification var1, Object var2) {
         if (RequestHeader.Fields.AuthenticationToken.equals(var1)) {
            this.setAuthenticationToken((NodeId)var2);
            return this;
         } else if (RequestHeader.Fields.Timestamp.equals(var1)) {
            this.setTimestamp((DateTime)var2);
            return this;
         } else if (RequestHeader.Fields.RequestHandle.equals(var1)) {
            this.setRequestHandle((UnsignedInteger)var2);
            return this;
         } else if (RequestHeader.Fields.ReturnDiagnostics.equals(var1)) {
            this.setReturnDiagnostics((UnsignedInteger)var2);
            return this;
         } else if (RequestHeader.Fields.AuditEntryId.equals(var1)) {
            this.setAuditEntryId((String)var2);
            return this;
         } else if (RequestHeader.Fields.TimeoutHint.equals(var1)) {
            this.setTimeoutHint((UnsignedInteger)var2);
            return this;
         } else if (RequestHeader.Fields.AdditionalHeader.equals(var1)) {
            this.setAdditionalHeader((ExtensionObject)var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public RequestHeader.Builder clear() {
         super.clear();
         this.f_authenticationToken = null;
         this.f_timestamp = null;
         this.f_requestHandle = null;
         this.f_returnDiagnostics = null;
         this.f_auditEntryId = null;
         this.f_timeoutHint = null;
         this.f_additionalHeader = null;
         return this;
      }

      public StructureSpecification specification() {
         return RequestHeader.SPECIFICATION;
      }

      public RequestHeader build() {
         return new RequestHeader(
            this.f_authenticationToken,
            this.f_timestamp,
            this.f_requestHandle,
            this.f_returnDiagnostics,
            this.f_auditEntryId,
            this.f_timeoutHint,
            this.f_additionalHeader
         );
      }
   }

   public static enum Fields implements FieldSpecification {
      AuthenticationToken("AuthenticationToken", NodeId.class, false, UaIds.SessionAuthenticationToken, -1, null, false),
      Timestamp("Timestamp", DateTime.class, false, UaIds.UtcTime, -1, null, false),
      RequestHandle("RequestHandle", UnsignedInteger.class, false, UaIds.IntegerId, -1, null, false),
      ReturnDiagnostics("ReturnDiagnostics", UnsignedInteger.class, false, UaIds.UInt32, -1, null, false),
      AuditEntryId("AuditEntryId", String.class, false, UaIds.String, -1, null, false),
      TimeoutHint("TimeoutHint", UnsignedInteger.class, false, UaIds.UInt32, -1, null, false),
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
