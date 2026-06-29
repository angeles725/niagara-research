package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
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

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=514")
public class BrowseDescription extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.BrowseDescription_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.BrowseDescription_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.BrowseDescription_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.BrowseDescription;
   public static final StructureSpecification SPECIFICATION;
   private NodeId f_nodeId;
   private BrowseDirection f_browseDirection;
   private NodeId f_referenceTypeId;
   private Boolean f_includeSubtypes;
   private UnsignedInteger f_nodeClassMask;
   private UnsignedInteger f_resultMask;

   public BrowseDescription() {
   }

   public BrowseDescription(NodeId var1, BrowseDirection var2, NodeId var3, Boolean var4, UnsignedInteger var5, UnsignedInteger var6) {
      this.f_nodeId = var1;
      this.f_browseDirection = var2;
      this.f_referenceTypeId = var3;
      this.f_includeSubtypes = var4;
      this.f_nodeClassMask = var5;
      this.f_resultMask = var6;
   }

   public NodeId getNodeId() {
      return this.f_nodeId;
   }

   public void setNodeId(NodeId var1) {
      this.f_nodeId = var1;
   }

   public BrowseDirection getBrowseDirection() {
      return this.f_browseDirection;
   }

   public void setBrowseDirection(BrowseDirection var1) {
      this.f_browseDirection = var1;
   }

   public NodeId getReferenceTypeId() {
      return this.f_referenceTypeId;
   }

   public void setReferenceTypeId(NodeId var1) {
      this.f_referenceTypeId = var1;
   }

   public Boolean getIncludeSubtypes() {
      return this.f_includeSubtypes;
   }

   public void setIncludeSubtypes(Boolean var1) {
      this.f_includeSubtypes = var1;
   }

   public UnsignedInteger getNodeClassMask() {
      return this.f_nodeClassMask;
   }

   public void setNodeClassMask(UnsignedInteger var1) {
      this.f_nodeClassMask = var1;
   }

   public UnsignedInteger getResultMask() {
      return this.f_resultMask;
   }

   public void setResultMask(UnsignedInteger var1) {
      this.f_resultMask = var1;
   }

   public BrowseDescription clone() {
      BrowseDescription var1 = (BrowseDescription)super.clone();
      var1.f_nodeId = (NodeId)StructureUtils.clone(this.f_nodeId);
      var1.f_browseDirection = (BrowseDirection)StructureUtils.clone(this.f_browseDirection);
      var1.f_referenceTypeId = (NodeId)StructureUtils.clone(this.f_referenceTypeId);
      var1.f_includeSubtypes = (Boolean)StructureUtils.clone(this.f_includeSubtypes);
      var1.f_nodeClassMask = (UnsignedInteger)StructureUtils.clone(this.f_nodeClassMask);
      var1.f_resultMask = (UnsignedInteger)StructureUtils.clone(this.f_resultMask);
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
         BrowseDescription var2 = (BrowseDescription)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getNodeId(), var2.getNodeId())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getBrowseDirection(), var2.getBrowseDirection())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getReferenceTypeId(), var2.getReferenceTypeId())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getIncludeSubtypes(), var2.getIncludeSubtypes())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getNodeClassMask(), var2.getNodeClassMask())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getResultMask(), var2.getResultMask());
         }
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(
         new Object[]{
            this.getNodeId(), this.getBrowseDirection(), this.getReferenceTypeId(), this.getIncludeSubtypes(), this.getNodeClassMask(), this.getResultMask()
         }
      );
   }

   public void clear() {
      super.clear();
      this.f_nodeId = null;
      this.f_browseDirection = null;
      this.f_referenceTypeId = null;
      this.f_includeSubtypes = null;
      this.f_nodeClassMask = null;
      this.f_resultMask = null;
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
      var1.put(BrowseDescription.Fields.NodeId, this.getNodeId());
      var1.put(BrowseDescription.Fields.BrowseDirection, this.getBrowseDirection());
      var1.put(BrowseDescription.Fields.ReferenceTypeId, this.getReferenceTypeId());
      var1.put(BrowseDescription.Fields.IncludeSubtypes, this.getIncludeSubtypes());
      var1.put(BrowseDescription.Fields.NodeClassMask, this.getNodeClassMask());
      var1.put(BrowseDescription.Fields.ResultMask, this.getResultMask());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowseDescription.Builder builder() {
      return new BrowseDescription.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (BrowseDescription.Fields.NodeId.equals(var1)) {
         return this.getNodeId();
      } else if (BrowseDescription.Fields.BrowseDirection.equals(var1)) {
         return this.getBrowseDirection();
      } else if (BrowseDescription.Fields.ReferenceTypeId.equals(var1)) {
         return this.getReferenceTypeId();
      } else if (BrowseDescription.Fields.IncludeSubtypes.equals(var1)) {
         return this.getIncludeSubtypes();
      } else if (BrowseDescription.Fields.NodeClassMask.equals(var1)) {
         return this.getNodeClassMask();
      } else if (BrowseDescription.Fields.ResultMask.equals(var1)) {
         return this.getResultMask();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (BrowseDescription.Fields.NodeId.equals(var1)) {
         this.setNodeId((NodeId)var2);
      } else if (BrowseDescription.Fields.BrowseDirection.equals(var1)) {
         this.setBrowseDirection((BrowseDirection)var2);
      } else if (BrowseDescription.Fields.ReferenceTypeId.equals(var1)) {
         this.setReferenceTypeId((NodeId)var2);
      } else if (BrowseDescription.Fields.IncludeSubtypes.equals(var1)) {
         this.setIncludeSubtypes((Boolean)var2);
      } else if (BrowseDescription.Fields.NodeClassMask.equals(var1)) {
         this.setNodeClassMask((UnsignedInteger)var2);
      } else if (BrowseDescription.Fields.ResultMask.equals(var1)) {
         this.setResultMask((UnsignedInteger)var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public BrowseDescription.Builder toBuilder() {
      BrowseDescription.Builder var1 = builder();
      var1.setNodeId((NodeId)StructureUtils.clone(this.getNodeId()));
      var1.setBrowseDirection((BrowseDirection)StructureUtils.clone(this.getBrowseDirection()));
      var1.setReferenceTypeId((NodeId)StructureUtils.clone(this.getReferenceTypeId()));
      var1.setIncludeSubtypes((Boolean)StructureUtils.clone(this.getIncludeSubtypes()));
      var1.setNodeClassMask((UnsignedInteger)StructureUtils.clone(this.getNodeClassMask()));
      var1.setResultMask((UnsignedInteger)StructureUtils.clone(this.getResultMask()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(BrowseDescription.Fields.NodeId);
      var0.addField(BrowseDescription.Fields.BrowseDirection);
      var0.addField(BrowseDescription.Fields.ReferenceTypeId);
      var0.addField(BrowseDescription.Fields.IncludeSubtypes);
      var0.addField(BrowseDescription.Fields.NodeClassMask);
      var0.addField(BrowseDescription.Fields.ResultMask);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("BrowseDescription");
      var0.setJavaClass(BrowseDescription.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(BrowseDescription.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private NodeId f_nodeId;
      private BrowseDirection f_browseDirection;
      private NodeId f_referenceTypeId;
      private Boolean f_includeSubtypes;
      private UnsignedInteger f_nodeClassMask;
      private UnsignedInteger f_resultMask;

      protected Builder() {
      }

      public NodeId getNodeId() {
         return this.f_nodeId;
      }

      public BrowseDescription.Builder setNodeId(NodeId var1) {
         this.f_nodeId = var1;
         return this;
      }

      public BrowseDirection getBrowseDirection() {
         return this.f_browseDirection;
      }

      public BrowseDescription.Builder setBrowseDirection(BrowseDirection var1) {
         this.f_browseDirection = var1;
         return this;
      }

      public NodeId getReferenceTypeId() {
         return this.f_referenceTypeId;
      }

      public BrowseDescription.Builder setReferenceTypeId(NodeId var1) {
         this.f_referenceTypeId = var1;
         return this;
      }

      public Boolean getIncludeSubtypes() {
         return this.f_includeSubtypes;
      }

      public BrowseDescription.Builder setIncludeSubtypes(Boolean var1) {
         this.f_includeSubtypes = var1;
         return this;
      }

      public UnsignedInteger getNodeClassMask() {
         return this.f_nodeClassMask;
      }

      public BrowseDescription.Builder setNodeClassMask(UnsignedInteger var1) {
         this.f_nodeClassMask = var1;
         return this;
      }

      public UnsignedInteger getResultMask() {
         return this.f_resultMask;
      }

      public BrowseDescription.Builder setResultMask(UnsignedInteger var1) {
         this.f_resultMask = var1;
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
            BrowseDescription.Builder var2 = (BrowseDescription.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getNodeId(), var2.getNodeId())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getBrowseDirection(), var2.getBrowseDirection())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getReferenceTypeId(), var2.getReferenceTypeId())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getIncludeSubtypes(), var2.getIncludeSubtypes())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getNodeClassMask(), var2.getNodeClassMask())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getResultMask(), var2.getResultMask());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(
            new Object[]{
               this.getNodeId(), this.getBrowseDirection(), this.getReferenceTypeId(), this.getIncludeSubtypes(), this.getNodeClassMask(), this.getResultMask()
            }
         );
      }

      public Object get(FieldSpecification var1) {
         if (BrowseDescription.Fields.NodeId.equals(var1)) {
            return this.getNodeId();
         } else if (BrowseDescription.Fields.BrowseDirection.equals(var1)) {
            return this.getBrowseDirection();
         } else if (BrowseDescription.Fields.ReferenceTypeId.equals(var1)) {
            return this.getReferenceTypeId();
         } else if (BrowseDescription.Fields.IncludeSubtypes.equals(var1)) {
            return this.getIncludeSubtypes();
         } else if (BrowseDescription.Fields.NodeClassMask.equals(var1)) {
            return this.getNodeClassMask();
         } else if (BrowseDescription.Fields.ResultMask.equals(var1)) {
            return this.getResultMask();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseDescription.Builder set(FieldSpecification var1, Object var2) {
         if (BrowseDescription.Fields.NodeId.equals(var1)) {
            this.setNodeId((NodeId)var2);
            return this;
         } else if (BrowseDescription.Fields.BrowseDirection.equals(var1)) {
            this.setBrowseDirection((BrowseDirection)var2);
            return this;
         } else if (BrowseDescription.Fields.ReferenceTypeId.equals(var1)) {
            this.setReferenceTypeId((NodeId)var2);
            return this;
         } else if (BrowseDescription.Fields.IncludeSubtypes.equals(var1)) {
            this.setIncludeSubtypes((Boolean)var2);
            return this;
         } else if (BrowseDescription.Fields.NodeClassMask.equals(var1)) {
            this.setNodeClassMask((UnsignedInteger)var2);
            return this;
         } else if (BrowseDescription.Fields.ResultMask.equals(var1)) {
            this.setResultMask((UnsignedInteger)var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowseDescription.Builder clear() {
         super.clear();
         this.f_nodeId = null;
         this.f_browseDirection = null;
         this.f_referenceTypeId = null;
         this.f_includeSubtypes = null;
         this.f_nodeClassMask = null;
         this.f_resultMask = null;
         return this;
      }

      public StructureSpecification specification() {
         return BrowseDescription.SPECIFICATION;
      }

      public BrowseDescription build() {
         return new BrowseDescription(
            this.f_nodeId, this.f_browseDirection, this.f_referenceTypeId, this.f_includeSubtypes, this.f_nodeClassMask, this.f_resultMask
         );
      }
   }

   public static enum Fields implements FieldSpecification {
      NodeId("NodeId", NodeId.class, false, UaIds.NodeId, -1, null, false),
      BrowseDirection("BrowseDirection", BrowseDirection.class, false, UaIds.BrowseDirection, -1, null, false),
      ReferenceTypeId("ReferenceTypeId", NodeId.class, false, UaIds.NodeId, -1, null, false),
      IncludeSubtypes("IncludeSubtypes", Boolean.class, false, UaIds.Boolean, -1, null, false),
      NodeClassMask("NodeClassMask", UnsignedInteger.class, false, UaIds.UInt32, -1, null, false),
      ResultMask("ResultMask", UnsignedInteger.class, false, UaIds.UInt32, -1, null, false);

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
