package com.prosysopc.ua.stack.core;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.UaArrayDimensions;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.DataValue;
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

@TypeDefinitionId("nsu=http://opcfoundation.org/UA/;i=668")
public class WriteValue extends AbstractStructure {
   @Deprecated
   public static final ExpandedNodeId BINARY = Ids.WriteValue_DefaultBinary;
   @Deprecated
   public static final ExpandedNodeId XML = Ids.WriteValue_DefaultXml;
   @Deprecated
   public static final ExpandedNodeId JSON = Ids.WriteValue_DefaultJson;
   @Deprecated
   public static final ExpandedNodeId ID = Ids.WriteValue;
   public static final StructureSpecification SPECIFICATION;
   private NodeId f_nodeId;
   private UnsignedInteger f_attributeId;
   private String f_indexRange;
   private DataValue f_value;

   public WriteValue() {
   }

   public WriteValue(NodeId var1, UnsignedInteger var2, String var3, DataValue var4) {
      this.f_nodeId = var1;
      this.f_attributeId = var2;
      this.f_indexRange = var3;
      this.f_value = var4;
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

   public DataValue getValue() {
      return this.f_value;
   }

   public void setValue(DataValue var1) {
      this.f_value = var1;
   }

   public WriteValue clone() {
      WriteValue var1 = (WriteValue)super.clone();
      var1.f_nodeId = (NodeId)StructureUtils.clone(this.f_nodeId);
      var1.f_attributeId = (UnsignedInteger)StructureUtils.clone(this.f_attributeId);
      var1.f_indexRange = (String)StructureUtils.clone(this.f_indexRange);
      var1.f_value = (DataValue)StructureUtils.clone(this.f_value);
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
         WriteValue var2 = (WriteValue)var1;
         if (!StructureUtils.scalarOrArrayEquals(this.getNodeId(), var2.getNodeId())) {
            return false;
         } else if (!StructureUtils.scalarOrArrayEquals(this.getAttributeId(), var2.getAttributeId())) {
            return false;
         } else {
            return !StructureUtils.scalarOrArrayEquals(this.getIndexRange(), var2.getIndexRange())
               ? false
               : StructureUtils.scalarOrArrayEquals(this.getValue(), var2.getValue());
         }
      }
   }

   public int hashCode() {
      return StructureUtils.hashCode(new Object[]{this.getNodeId(), this.getAttributeId(), this.getIndexRange(), this.getValue()});
   }

   public void clear() {
      super.clear();
      this.f_nodeId = null;
      this.f_attributeId = null;
      this.f_indexRange = null;
      this.f_value = null;
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
      var1.put(WriteValue.Fields.NodeId, this.getNodeId());
      var1.put(WriteValue.Fields.AttributeId, this.getAttributeId());
      var1.put(WriteValue.Fields.IndexRange, this.getIndexRange());
      var1.put(WriteValue.Fields.Value, this.getValue());
      return Collections.unmodifiableMap(var1);
   }

   public StructureSpecification specification() {
      return SPECIFICATION;
   }

   public static WriteValue.Builder builder() {
      return new WriteValue.Builder();
   }

   public Object get(FieldSpecification var1) {
      if (WriteValue.Fields.NodeId.equals(var1)) {
         return this.getNodeId();
      } else if (WriteValue.Fields.AttributeId.equals(var1)) {
         return this.getAttributeId();
      } else if (WriteValue.Fields.IndexRange.equals(var1)) {
         return this.getIndexRange();
      } else if (WriteValue.Fields.Value.equals(var1)) {
         return this.getValue();
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public void set(FieldSpecification var1, Object var2) {
      if (WriteValue.Fields.NodeId.equals(var1)) {
         this.setNodeId((NodeId)var2);
      } else if (WriteValue.Fields.AttributeId.equals(var1)) {
         this.setAttributeId((UnsignedInteger)var2);
      } else if (WriteValue.Fields.IndexRange.equals(var1)) {
         this.setIndexRange((String)var2);
      } else if (WriteValue.Fields.Value.equals(var1)) {
         this.setValue((DataValue)var2);
      } else {
         throw new IllegalArgumentException("Unknown field: " + var1);
      }
   }

   public WriteValue.Builder toBuilder() {
      WriteValue.Builder var1 = builder();
      var1.setNodeId((NodeId)StructureUtils.clone(this.getNodeId()));
      var1.setAttributeId((UnsignedInteger)StructureUtils.clone(this.getAttributeId()));
      var1.setIndexRange((String)StructureUtils.clone(this.getIndexRange()));
      var1.setValue((DataValue)StructureUtils.clone(this.getValue()));
      return var1;
   }

   static {
      com.prosysopc.ua.typedictionary.StructureSpecification.Builder var0 = StructureSpecification.builder();
      var0.addField(WriteValue.Fields.NodeId);
      var0.addField(WriteValue.Fields.AttributeId);
      var0.addField(WriteValue.Fields.IndexRange);
      var0.addField(WriteValue.Fields.Value);
      var0.setBinaryEncodeId(UaNodeId.fromLocal(BINARY));
      var0.setXmlEncodeId(UaNodeId.fromLocal(XML));
      var0.setJsonEncodeId(UaNodeId.fromLocal(JSON));
      var0.setTypeId(UaNodeId.fromLocal(ID));
      var0.addSuperTypeId(UaIds.Structure);
      var0.setName("WriteValue");
      var0.setJavaClass(WriteValue.class);
      var0.setStructureType(StructureType.NORMAL);
      var0.setBuilderSupplier(WriteValue.Builder::new);
      SPECIFICATION = var0.build();
   }

   public static class Builder extends com.prosysopc.ua.stack.utils.AbstractStructure.Builder {
      private NodeId f_nodeId;
      private UnsignedInteger f_attributeId;
      private String f_indexRange;
      private DataValue f_value;

      protected Builder() {
      }

      public NodeId getNodeId() {
         return this.f_nodeId;
      }

      public WriteValue.Builder setNodeId(NodeId var1) {
         this.f_nodeId = var1;
         return this;
      }

      public UnsignedInteger getAttributeId() {
         return this.f_attributeId;
      }

      public WriteValue.Builder setAttributeId(UnsignedInteger var1) {
         this.f_attributeId = var1;
         return this;
      }

      public String getIndexRange() {
         return this.f_indexRange;
      }

      public WriteValue.Builder setIndexRange(String var1) {
         this.f_indexRange = var1;
         return this;
      }

      public DataValue getValue() {
         return this.f_value;
      }

      public WriteValue.Builder setValue(DataValue var1) {
         this.f_value = var1;
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
            WriteValue.Builder var2 = (WriteValue.Builder)var1;
            if (!StructureUtils.scalarOrArrayEquals(this.getNodeId(), var2.getNodeId())) {
               return false;
            } else if (!StructureUtils.scalarOrArrayEquals(this.getAttributeId(), var2.getAttributeId())) {
               return false;
            } else {
               return !StructureUtils.scalarOrArrayEquals(this.getIndexRange(), var2.getIndexRange())
                  ? false
                  : StructureUtils.scalarOrArrayEquals(this.getValue(), var2.getValue());
            }
         }
      }

      public int hashCode() {
         return StructureUtils.hashCode(new Object[]{this.getNodeId(), this.getAttributeId(), this.getIndexRange(), this.getValue()});
      }

      public Object get(FieldSpecification var1) {
         if (WriteValue.Fields.NodeId.equals(var1)) {
            return this.getNodeId();
         } else if (WriteValue.Fields.AttributeId.equals(var1)) {
            return this.getAttributeId();
         } else if (WriteValue.Fields.IndexRange.equals(var1)) {
            return this.getIndexRange();
         } else if (WriteValue.Fields.Value.equals(var1)) {
            return this.getValue();
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public WriteValue.Builder set(FieldSpecification var1, Object var2) {
         if (WriteValue.Fields.NodeId.equals(var1)) {
            this.setNodeId((NodeId)var2);
            return this;
         } else if (WriteValue.Fields.AttributeId.equals(var1)) {
            this.setAttributeId((UnsignedInteger)var2);
            return this;
         } else if (WriteValue.Fields.IndexRange.equals(var1)) {
            this.setIndexRange((String)var2);
            return this;
         } else if (WriteValue.Fields.Value.equals(var1)) {
            this.setValue((DataValue)var2);
            return this;
         } else {
            throw new IllegalArgumentException("Unknown field: " + var1);
         }
      }

      public WriteValue.Builder clear() {
         super.clear();
         this.f_nodeId = null;
         this.f_attributeId = null;
         this.f_indexRange = null;
         this.f_value = null;
         return this;
      }

      public StructureSpecification specification() {
         return WriteValue.SPECIFICATION;
      }

      public WriteValue build() {
         return new WriteValue(this.f_nodeId, this.f_attributeId, this.f_indexRange, this.f_value);
      }
   }

   public static enum Fields implements FieldSpecification {
      NodeId("NodeId", NodeId.class, false, UaIds.NodeId, -1, null, false),
      AttributeId("AttributeId", UnsignedInteger.class, false, UaIds.IntegerId, -1, null, false),
      IndexRange("IndexRange", String.class, false, UaIds.NumericRange, -1, null, false),
      Value("Value", DataValue.class, false, UaIds.DataValue, -1, null, false);

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
