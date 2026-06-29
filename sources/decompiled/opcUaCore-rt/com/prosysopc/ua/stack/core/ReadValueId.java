package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.utils.AbstractStructure;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import com.prosysopc.ua.types.opcua.Ids;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=626")
public class ReadValueId extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.ReadValueId_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.ReadValueId_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.ReadValueId_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.ReadValueId;
   public static final StructureSpecification SPECIFICATION;
   private NodeId f_nodeId;
   private UnsignedInteger f_attributeId;
   private String f_indexRange;
   private QualifiedName f_dataEncoding;

   public ReadValueId() {
   }

   public ReadValueId(NodeId var1, UnsignedInteger var2, String var3, QualifiedName var4) {
      this.f_nodeId = var1;
      this.f_attributeId = var2;
      this.f_indexRange = var3;
      this.f_dataEncoding = var4;
   }

   public NodeId getNodeId() {
      return this.f_nodeId;
   }

   public void setNodeId(NodeId var1) {
      this.f_nodeId = var1;
   }

   public UnsignedInteger getAttributeId() {
      return this.f_attributeId;
   }

   public void setAttributeId(UnsignedInteger var1) {
      this.f_attributeId = var1;
   }

   public String getIndexRange() {
      return this.f_indexRange;
   }

   public void setIndexRange(String var1) {
      this.f_indexRange = var1;
   }

   public QualifiedName getDataEncoding() {
      return this.f_dataEncoding;
   }

   public void setDataEncoding(QualifiedName var1) {
      this.f_dataEncoding = var1;
   }

   public ReadValueId clone() {
      ReadValueId var1 = (ReadValueId)super.clone();
      var1.f_nodeId = (NodeId)StructureUtils.clone(this.f_nodeId);
      var1.f_attributeId = (UnsignedInteger)StructureUtils.clone(this.f_attributeId);
      var1.f_indexRange = (String)StructureUtils.clone(this.f_indexRange);
      var1.f_dataEncoding = (QualifiedName)StructureUtils.clone(this.f_dataEncoding);
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
         ReadValueId var2 = (ReadValueId)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getNodeId(), var2.getNodeId())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getAttributeId(), var2.getAttributeId())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getIndexRange(), var2.getIndexRange())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getDataEncoding(), var2.getDataEncoding());
         }
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getNodeId(), this.getAttributeId(), this.getIndexRange(), this.getDataEncoding()});
   }

   public void clear() {
      super.clear();
      this.f_nodeId = null;
      this.f_attributeId = null;
      this.f_indexRange = null;
      this.f_dataEncoding = null;
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
      var1.put(ReadValueId.Fields.NodeId, this.getNodeId());
      var1.put(ReadValueId.Fields.AttributeId, this.getAttributeId());
      var1.put(ReadValueId.Fields.IndexRange, this.getIndexRange());
      var1.put(ReadValueId.Fields.DataEncoding, this.getDataEncoding());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static ReadValueId.Builder builder() {
      return new ReadValueId.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (ReadValueId.Fields.NodeId.equals(var1)) {
         return this.getNodeId();
      } else if (ReadValueId.Fields.AttributeId.equals(var1)) {
         return this.getAttributeId();
      } else if (ReadValueId.Fields.IndexRange.equals(var1)) {
         return this.getIndexRange();
      } else if (ReadValueId.Fields.DataEncoding.equals(var1)) {
         return this.getDataEncoding();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (ReadValueId.Fields.NodeId.equals(var1)) {
         this.setNodeId((NodeId)var2);
      } else if (ReadValueId.Fields.AttributeId.equals(var1)) {
         this.setAttributeId((UnsignedInteger)var2);
      } else if (ReadValueId.Fields.IndexRange.equals(var1)) {
         this.setIndexRange((String)var2);
      } else if (ReadValueId.Fields.DataEncoding.equals(var1)) {
         this.setDataEncoding((QualifiedName)var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public ReadValueId.Builder toBuilder() {
      ReadValueId.Builder var1 = builder();
      var1.setNodeId((NodeId)StructureUtils.clone(this.getNodeId()));
      var1.setAttributeId((UnsignedInteger)StructureUtils.clone(this.getAttributeId()));
      var1.setIndexRange((String)StructureUtils.clone(this.getIndexRange()));
      var1.setDataEncoding((QualifiedName)StructureUtils.clone(this.getDataEncoding()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(ReadValueId.Fields.NodeId);
      var0.addField(ReadValueId.Fields.AttributeId);
      var0.addField(ReadValueId.Fields.IndexRange);
      var0.addField(ReadValueId.Fields.DataEncoding);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("ReadValueId");
      var0.setJavaClass(ReadValueId.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(ReadValueId.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private NodeId f_nodeId;
      private UnsignedInteger f_attributeId;
      private String f_indexRange;
      private QualifiedName f_dataEncoding;

      protected Builder() {
      }

      public NodeId getNodeId() {
         return this.f_nodeId;
      }

      public ReadValueId.Builder setNodeId(NodeId var1) {
         this.f_nodeId = var1;
         return this;
      }

      public UnsignedInteger getAttributeId() {
         return this.f_attributeId;
      }

      public ReadValueId.Builder setAttributeId(UnsignedInteger var1) {
         this.f_attributeId = var1;
         return this;
      }

      public String getIndexRange() {
         return this.f_indexRange;
      }

      public ReadValueId.Builder setIndexRange(String var1) {
         this.f_indexRange = var1;
         return this;
      }

      public QualifiedName getDataEncoding() {
         return this.f_dataEncoding;
      }

      public ReadValueId.Builder setDataEncoding(QualifiedName var1) {
         this.f_dataEncoding = var1;
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
            ReadValueId.Builder var2 = (ReadValueId.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getNodeId(), var2.getNodeId())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getAttributeId(), var2.getAttributeId())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getIndexRange(), var2.getIndexRange())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getDataEncoding(), var2.getDataEncoding());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getNodeId(), this.getAttributeId(), this.getIndexRange(), this.getDataEncoding()});
      }

      public Object get(FieldSpecification var1) {
         if (ReadValueId.Fields.NodeId.equals(var1)) {
            return this.getNodeId();
         } else if (ReadValueId.Fields.AttributeId.equals(var1)) {
            return this.getAttributeId();
         } else if (ReadValueId.Fields.IndexRange.equals(var1)) {
            return this.getIndexRange();
         } else if (ReadValueId.Fields.DataEncoding.equals(var1)) {
            return this.getDataEncoding();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ReadValueId.Builder set(FieldSpecification var1, Object var2) {
         if (ReadValueId.Fields.NodeId.equals(var1)) {
            this.setNodeId((NodeId)var2);
            return this;
         } else if (ReadValueId.Fields.AttributeId.equals(var1)) {
            this.setAttributeId((UnsignedInteger)var2);
            return this;
         } else if (ReadValueId.Fields.IndexRange.equals(var1)) {
            this.setIndexRange((String)var2);
            return this;
         } else if (ReadValueId.Fields.DataEncoding.equals(var1)) {
            this.setDataEncoding((QualifiedName)var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public ReadValueId.Builder clear() {
         super.clear();
         this.f_nodeId = null;
         this.f_attributeId = null;
         this.f_indexRange = null;
         this.f_dataEncoding = null;
         return this;
      }

      public StructureSpecification specification() {
         return ReadValueId.SPECIFICATION;
      }

      public ReadValueId build() {
         return new ReadValueId(this.f_nodeId, this.f_attributeId, this.f_indexRange, this.f_dataEncoding);
      }
   }

   public static enum Fields implements FieldSpecification {
      NodeId("NodeId", NodeId.class, false, UaIds.NodeId, -1, null, false),
      AttributeId("AttributeId", UnsignedInteger.class, false, UaIds.IntegerId, -1, null, false),
      IndexRange("IndexRange", String.class, false, UaIds.NumericRange, -1, null, false),
      DataEncoding("DataEncoding", QualifiedName.class, false, UaIds.QualifiedName, -1, null, false);

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
