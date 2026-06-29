package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=543")
public class BrowsePath extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.BrowsePath_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.BrowsePath_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.BrowsePath_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.BrowsePath;
   public static final StructureSpecification SPECIFICATION;
   private NodeId f_startingNode;
   private RelativePath f_relativePath;

   public BrowsePath() {
   }

   public BrowsePath(NodeId var1, RelativePath var2) {
      this.f_startingNode = var1;
      this.f_relativePath = var2;
   }

   public NodeId getStartingNode() {
      return this.f_startingNode;
   }

   public void setStartingNode(NodeId var1) {
      this.f_startingNode = var1;
   }

   public RelativePath getRelativePath() {
      return this.f_relativePath;
   }

   public void setRelativePath(RelativePath var1) {
      this.f_relativePath = var1;
   }

   public BrowsePath clone() {
      BrowsePath var1 = (BrowsePath)super.clone();
      var1.f_startingNode = (NodeId)StructureUtils.clone(this.f_startingNode);
      var1.f_relativePath = (RelativePath)StructureUtils.clone(this.f_relativePath);
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
         BrowsePath var2 = (BrowsePath)var1;
         return !StructureUtils.scalarOrArrayEquals(this.getStartingNode(), var2.getStartingNode())
            ? false
            : StructureUtils.scalarOrArrayEquals(this.getRelativePath(), var2.getRelativePath());
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getStartingNode(), this.getRelativePath()});
   }

   public void clear() {
      super.clear();
      this.f_startingNode = null;
      this.f_relativePath = null;
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
      var1.put(BrowsePath.Fields.StartingNode, this.getStartingNode());
      var1.put(BrowsePath.Fields.RelativePath, this.getRelativePath());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static BrowsePath.Builder builder() {
      return new BrowsePath.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (BrowsePath.Fields.StartingNode.equals(var1)) {
         return this.getStartingNode();
      } else if (BrowsePath.Fields.RelativePath.equals(var1)) {
         return this.getRelativePath();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (BrowsePath.Fields.StartingNode.equals(var1)) {
         this.setStartingNode((NodeId)var2);
      } else if (BrowsePath.Fields.RelativePath.equals(var1)) {
         this.setRelativePath((RelativePath)var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public BrowsePath.Builder toBuilder() {
      BrowsePath.Builder var1 = builder();
      var1.setStartingNode((NodeId)StructureUtils.clone(this.getStartingNode()));
      var1.setRelativePath((RelativePath)StructureUtils.clone(this.getRelativePath()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(BrowsePath.Fields.StartingNode);
      var0.addField(BrowsePath.Fields.RelativePath);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("BrowsePath");
      var0.setJavaClass(BrowsePath.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(BrowsePath.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private NodeId f_startingNode;
      private RelativePath f_relativePath;

      protected Builder() {
      }

      public NodeId getStartingNode() {
         return this.f_startingNode;
      }

      public BrowsePath.Builder setStartingNode(NodeId var1) {
         this.f_startingNode = var1;
         return this;
      }

      public RelativePath getRelativePath() {
         return this.f_relativePath;
      }

      public BrowsePath.Builder setRelativePath(RelativePath var1) {
         this.f_relativePath = var1;
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
            BrowsePath.Builder var2 = (BrowsePath.Builder)var1;
            return !StructureUtils.scalarOrArrayEquals(this.getStartingNode(), var2.getStartingNode())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getRelativePath(), var2.getRelativePath());
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getStartingNode(), this.getRelativePath()});
      }

      public Object get(FieldSpecification var1) {
         if (BrowsePath.Fields.StartingNode.equals(var1)) {
            return this.getStartingNode();
         } else if (BrowsePath.Fields.RelativePath.equals(var1)) {
            return this.getRelativePath();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowsePath.Builder set(FieldSpecification var1, Object var2) {
         if (BrowsePath.Fields.StartingNode.equals(var1)) {
            this.setStartingNode((NodeId)var2);
            return this;
         } else if (BrowsePath.Fields.RelativePath.equals(var1)) {
            this.setRelativePath((RelativePath)var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public BrowsePath.Builder clear() {
         super.clear();
         this.f_startingNode = null;
         this.f_relativePath = null;
         return this;
      }

      public StructureSpecification specification() {
         return BrowsePath.SPECIFICATION;
      }

      public BrowsePath build() {
         return new BrowsePath(this.f_startingNode, this.f_relativePath);
      }
   }

   public static enum Fields implements FieldSpecification {
      StartingNode("StartingNode", NodeId.class, false, UaIds.NodeId, -1, null, false),
      RelativePath("RelativePath", RelativePath.class, false, UaIds.RelativePath, -1, null, false);

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
