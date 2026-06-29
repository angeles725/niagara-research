package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.ServiceRequest;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=525")
public class BrowseRequest extends AbstractStructure implements ServiceRequest<BrowseResponse> {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.BrowseRequest_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.BrowseRequest_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.BrowseRequest_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.BrowseRequest;
   public static final StructureSpecification SPECIFICATION;
   private RequestHeader f_requestHeader;
   private ViewDescription f_view;
   private UnsignedInteger f_requestedMaxReferencesPerNode;
   private BrowseDescription[] f_nodesToBrowse;

   public BrowseRequest() {
   }

   public BrowseRequest(RequestHeader var1, ViewDescription var2, UnsignedInteger var3, BrowseDescription[] var4) {
      this.f_requestHeader = var1;
      this.f_view = var2;
      this.f_requestedMaxReferencesPerNode = var3;
      this.f_nodesToBrowse = var4;
   }

   @Override
   public RequestHeader getRequestHeader() {
      return this.f_requestHeader;
   }

   @Override
   public void setRequestHeader(RequestHeader var1) {
      this.f_requestHeader = var1;
   }

   public ViewDescription getView() {
      return this.f_view;
   }

   public void setView(ViewDescription var1) {
      this.f_view = var1;
   }

   public UnsignedInteger getRequestedMaxReferencesPerNode() {
      return this.f_requestedMaxReferencesPerNode;
   }

   public void setRequestedMaxReferencesPerNode(UnsignedInteger var1) {
      this.f_requestedMaxReferencesPerNode = var1;
   }

   public BrowseDescription[] getNodesToBrowse() {
      return this.f_nodesToBrowse;
   }

   public void setNodesToBrowse(BrowseDescription[] var1) {
      this.f_nodesToBrowse = var1;
   }

   public BrowseRequest clone() {
      BrowseRequest var1 = (BrowseRequest)super.clone();
      var1.f_requestHeader = (RequestHeader)StructureUtils.clone(this.f_requestHeader);
      var1.f_view = (ViewDescription)StructureUtils.clone(this.f_view);
      var1.f_requestedMaxReferencesPerNode = (UnsignedInteger)StructureUtils.clone(this.f_requestedMaxReferencesPerNode);
      var1.f_nodesToBrowse = (BrowseDescription[])StructureUtils.clone(this.f_nodesToBrowse);
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
         BrowseRequest var2 = (BrowseRequest)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getView(), var2.getView())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getRequestedMaxReferencesPerNode(), var2.getRequestedMaxReferencesPerNode())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getNodesToBrowse(), var2.getNodesToBrowse());
         }
      }
   }

   @Override
   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getView(), this.getRequestedMaxReferencesPerNode(), this.getNodesToBrowse()});
   }

   @Override
   public void clear() {
      super.clear();
      this.f_requestHeader = null;
      this.f_view = null;
      this.f_requestedMaxReferencesPerNode = null;
      this.f_nodesToBrowse = null;
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
      var1.put(BrowseRequest.Fields.RequestHeader, this.getRequestHeader());
      var1.put(BrowseRequest.Fields.View, this.getView());
      var1.put(BrowseRequest.Fields.RequestedMaxReferencesPerNode, this.getRequestedMaxReferencesPerNode());
      var1.put(BrowseRequest.Fields.NodesToBrowse, this.getNodesToBrowse());
      return Collections.unmodifiableMap(var1);
   }

   @Override
   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowseRequest.Builder builder() {
      return new BrowseRequest.Builder();
   }

   @Override
   public Object get(FieldSpecification var1) {
      if (BrowseRequest.Fields.RequestHeader.equals(var1)) {
         return this.getRequestHeader();
      } else if (BrowseRequest.Fields.View.equals(var1)) {
         return this.getView();
      } else if (BrowseRequest.Fields.RequestedMaxReferencesPerNode.equals(var1)) {
         return this.getRequestedMaxReferencesPerNode();
      } else if (BrowseRequest.Fields.NodesToBrowse.equals(var1)) {
         return this.getNodesToBrowse();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   @Override
   public void set(FieldSpecification var1, Object var2) {
      if (BrowseRequest.Fields.RequestHeader.equals(var1)) {
         this.setRequestHeader((RequestHeader)var2);
      } else if (BrowseRequest.Fields.View.equals(var1)) {
         this.setView((ViewDescription)var2);
      } else if (BrowseRequest.Fields.RequestedMaxReferencesPerNode.equals(var1)) {
         this.setRequestedMaxReferencesPerNode((UnsignedInteger)var2);
      } else if (BrowseRequest.Fields.NodesToBrowse.equals(var1)) {
         this.setNodesToBrowse((BrowseDescription[])var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public BrowseRequest.Builder toBuilder() {
      BrowseRequest.Builder var1 = builder();
      var1.setRequestHeader((RequestHeader)StructureUtils.clone(this.getRequestHeader()));
      var1.setView((ViewDescription)StructureUtils.clone(this.getView()));
      var1.setRequestedMaxReferencesPerNode((UnsignedInteger)StructureUtils.clone(this.getRequestedMaxReferencesPerNode()));
      var1.setNodesToBrowse((BrowseDescription[])StructureUtils.clone(this.getNodesToBrowse()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(BrowseRequest.Fields.RequestHeader);
      var0.addField(BrowseRequest.Fields.View);
      var0.addField(BrowseRequest.Fields.RequestedMaxReferencesPerNode);
      var0.addField(BrowseRequest.Fields.NodesToBrowse);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("BrowseRequest");
      var0.setJavaClass(BrowseRequest.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(BrowseRequest.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private RequestHeader f_requestHeader;
      private ViewDescription f_view;
      private UnsignedInteger f_requestedMaxReferencesPerNode;
      private BrowseDescription[] f_nodesToBrowse;

      protected Builder() {
      }

      public RequestHeader getRequestHeader() {
         return this.f_requestHeader;
      }

      public BrowseRequest.Builder setRequestHeader(RequestHeader var1) {
         this.f_requestHeader = var1;
         return this;
      }

      public ViewDescription getView() {
         return this.f_view;
      }

      public BrowseRequest.Builder setView(ViewDescription var1) {
         this.f_view = var1;
         return this;
      }

      public UnsignedInteger getRequestedMaxReferencesPerNode() {
         return this.f_requestedMaxReferencesPerNode;
      }

      public BrowseRequest.Builder setRequestedMaxReferencesPerNode(UnsignedInteger var1) {
         this.f_requestedMaxReferencesPerNode = var1;
         return this;
      }

      public BrowseDescription[] getNodesToBrowse() {
         return this.f_nodesToBrowse;
      }

      public BrowseRequest.Builder setNodesToBrowse(BrowseDescription[] var1) {
         this.f_nodesToBrowse = var1;
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
            BrowseRequest.Builder var2 = (BrowseRequest.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getRequestHeader(), var2.getRequestHeader())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getView(), var2.getView())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getRequestedMaxReferencesPerNode(), var2.getRequestedMaxReferencesPerNode())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getNodesToBrowse(), var2.getNodesToBrowse());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getRequestHeader(), this.getView(), this.getRequestedMaxReferencesPerNode(), this.getNodesToBrowse()});
      }

      public Object get(FieldSpecification var1) {
         if (BrowseRequest.Fields.RequestHeader.equals(var1)) {
            return this.getRequestHeader();
         } else if (BrowseRequest.Fields.View.equals(var1)) {
            return this.getView();
         } else if (BrowseRequest.Fields.RequestedMaxReferencesPerNode.equals(var1)) {
            return this.getRequestedMaxReferencesPerNode();
         } else if (BrowseRequest.Fields.NodesToBrowse.equals(var1)) {
            return this.getNodesToBrowse();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseRequest.Builder set(FieldSpecification var1, Object var2) {
         if (BrowseRequest.Fields.RequestHeader.equals(var1)) {
            this.setRequestHeader((RequestHeader)var2);
            return this;
         } else if (BrowseRequest.Fields.View.equals(var1)) {
            this.setView((ViewDescription)var2);
            return this;
         } else if (BrowseRequest.Fields.RequestedMaxReferencesPerNode.equals(var1)) {
            this.setRequestedMaxReferencesPerNode((UnsignedInteger)var2);
            return this;
         } else if (BrowseRequest.Fields.NodesToBrowse.equals(var1)) {
            this.setNodesToBrowse((BrowseDescription[])var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseRequest.Builder clear() {
         super.clear();
         this.f_requestHeader = null;
         this.f_view = null;
         this.f_requestedMaxReferencesPerNode = null;
         this.f_nodesToBrowse = null;
         return this;
      }

      public StructureSpecification specification() {
         return BrowseRequest.SPECIFICATION;
      }

      public BrowseRequest build() {
         return new BrowseRequest(this.f_requestHeader, this.f_view, this.f_requestedMaxReferencesPerNode, this.f_nodesToBrowse);
      }
   }

   public static enum Fields implements FieldSpecification {
      RequestHeader("RequestHeader", RequestHeader.class, false, UaIds.RequestHeader, -1, null, false),
      View("View", ViewDescription.class, false, UaIds.ViewDescription, -1, null, false),
      RequestedMaxReferencesPerNode("RequestedMaxReferencesPerNode", UnsignedInteger.class, false, UaIds.Counter, -1, null, false),
      NodesToBrowse("NodesToBrowse", BrowseDescription[].class, false, UaIds.BrowseDescription, 1, UaArrayDimensions.valueOf(new long[]{0L}), false);

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
