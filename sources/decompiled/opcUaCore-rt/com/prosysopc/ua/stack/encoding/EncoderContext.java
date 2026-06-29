package com.prosysopc.ua.stack.encoding;

import com.prosysopc.ua.CommonCodegenModel;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ExtensionObject;
import com.prosysopc.ua.stack.builtintypes.Structure;
import com.prosysopc.ua.stack.common.NamespaceTable;
import com.prosysopc.ua.stack.common.ServerTable;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.SimpleTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.types.opcua.CommonInformationModel;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncoderContext {
   private static final Logger logger = LoggerFactory.getLogger(EncoderContext.class);
   private NamespaceTable iz;
   private ServerTable sN;
   private int maxMessageSize = 0;
   private int sO = 0;
   private int sP = 0;
   private int sQ = 0;
   private final Map<UaNodeId, UaDataTypeSpecification> specifications = new ConcurrentHashMap<>();
   private volatile Function<UaNodeId, UaDataTypeSpecification> sR;

   @Deprecated
   public static EncoderContext getDefaultInstance() {
      return EncoderContext.a.sS;
   }

   public EncoderContext(NamespaceTable var1, ServerTable var2) {
      this.iz = var1;
      this.sN = var2;
      this.registerModel(CommonInformationModel.MODEL);
   }

   public EncoderContext(NamespaceTable var1, ServerTable var2, int var3) {
      this.iz = var1;
      this.sN = var2;
      this.maxMessageSize = var3;
      this.registerModel(CommonInformationModel.MODEL);
   }

   public void addDataTypeSpecification(UaDataTypeSpecification var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("The given specification cannot be null");
      } else {
         this.specifications.put(var1.getTypeId(), var1);
         if (var1 instanceof StructureSpecification) {
            StructureSpecification var2 = (StructureSpecification)var1;
            if (var2.getBinaryEncodeId() != null) {
               this.specifications.put(var2.getBinaryEncodeId(), var1);
            }

            if (var2.getXmlEncodeId() != null) {
               this.specifications.put(var2.getXmlEncodeId(), var1);
            }

            if (var2.getJsonEncodeId() != null) {
               this.specifications.put(var2.getJsonEncodeId(), var1);
            }
         }
      }
   }

   @Deprecated
   public void addEnumerationSpecification(EnumerationSpecification var1) {
      this.addDataTypeSpecification(var1);
   }

   @Deprecated
   public void addStructureSpecification(StructureSpecification var1) {
      this.addDataTypeSpecification(var1);
   }

   public Object decode(ExtensionObject[] var1) throws DecodingException {
      return this.decode(var1, null);
   }

   public Object decode(ExtensionObject[] var1, NamespaceTable var2) throws DecodingException {
      int var4 = var1.length;
      Structure[] var5 = new Structure[var4];

      for (int var6 = 0; var6 < var4; var6++) {
         ExtensionObject var7 = var1[var6];
         if (var7 != null) {
            var5[var6] = var7.decode(this, var2);
         }
      }

      Object var3 = var5;
      if (var4 > 0) {
         Class var9 = null;

         for (int var10 = 0; var10 < var4; var10++) {
            if (var5[var10] != null) {
               Class var8 = var5[var10].getClass();
               if (var9 == null) {
                  var9 = var8;
               } else if (!var8.isAssignableFrom(var9)) {
                  if (!var9.isAssignableFrom(var8)) {
                     var9 = null;
                     break;
                  }

                  var9 = var8;
               }
            }
         }

         if (var9 != null) {
            var3 = Arrays.copyOf(var5, var4, ((Structure[])Array.newInstance(var9, 0)).getClass());
         }
      }

      return var3;
   }

   public UaDataTypeSpecification getDataTypeSpecification(UaNodeId var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("The given id cannot be null");
      } else {
         UaDataTypeSpecification var2 = this.specifications.get(var1);
         if (var2 != null) {
            return var2;
         } else {
            Function var3 = this.sR;
            return var3 != null ? (UaDataTypeSpecification)var3.apply(var1) : null;
         }
      }
   }

   public Map<UaNodeId, UaDataTypeSpecification> getDataTypeSpecifications() {
      return Collections.unmodifiableMap(this.specifications);
   }

   public EnumerationSpecification getEnumerationSpecification(UaNodeId var1) {
      UaDataTypeSpecification var2 = this.getDataTypeSpecification(var1);
      return var2 instanceof EnumerationSpecification ? (EnumerationSpecification)var2 : null;
   }

   public int getMaxArrayLength() {
      return this.sQ;
   }

   public int getMaxByteStringLength() {
      return this.sP;
   }

   public int getMaxMessageSize() {
      return this.maxMessageSize;
   }

   public int getMaxStringLength() {
      return this.sO;
   }

   public NamespaceTable getNamespaceTable() {
      return this.iz;
   }

   public OptionSetSpecification getOptionSetSpecification(UaNodeId var1) {
      UaDataTypeSpecification var2 = this.getDataTypeSpecification(var1);
      return var2 instanceof OptionSetSpecification ? (OptionSetSpecification)var2 : null;
   }

   public ServerTable getServerTable() {
      return this.sN;
   }

   public SimpleTypeSpecification getSimpleTypeSpecification(UaNodeId var1) {
      UaDataTypeSpecification var2 = this.getDataTypeSpecification(var1);
      return var2 instanceof SimpleTypeSpecification ? (SimpleTypeSpecification)var2 : null;
   }

   public StructureSpecification getStructureSpecification(UaNodeId var1) {
      UaDataTypeSpecification var2 = this.getDataTypeSpecification(var1);
      return var2 instanceof StructureSpecification ? (StructureSpecification)var2 : null;
   }

   public void registerModel(CommonCodegenModel var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("The given model cannot be null");
      } else {
         var1.getSpecifications().values().forEach(this::addDataTypeSpecification);
      }
   }

   public void setDynamicDataTypeSpecificationProvider(Function<UaNodeId, UaDataTypeSpecification> var1) {
      this.sR = var1;
   }

   public void setMaxArrayLength(int var1) {
      this.sQ = var1;
   }

   public void setMaxByteStringLength(int var1) {
      this.sP = var1;
   }

   public void setMaxMessageSize(int var1) {
      this.maxMessageSize = var1;
   }

   public void setMaxStringLength(int var1) {
      this.sO = var1;
   }

   public void setNamespaceTable(NamespaceTable var1) {
      this.iz = var1;
   }

   public void setServerTable(ServerTable var1) {
      this.sN = var1;
   }

   public EncoderContext shallowCopy() {
      EncoderContext var1 = new EncoderContext(this.getNamespaceTable(), this.getServerTable());
      var1.setMaxArrayLength(this.getMaxArrayLength());
      var1.setMaxByteStringLength(this.getMaxByteStringLength());
      var1.setMaxMessageSize(this.getMaxMessageSize());
      var1.setMaxStringLength(this.getMaxStringLength());
      var1.specifications.putAll(this.specifications);
      var1.sR = this.sR;
      return var1;
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append("   namespaceTable = " + this.iz + "\n");
      var1.append("   serverTable = " + this.sN + "\n");
      var1.append("   maxMessageSize = " + this.maxMessageSize + "\n");
      var1.append("   maxStringLength = " + this.sO + "\n");
      var1.append("   maxByteStringLength = " + this.sP + "\n");
      var1.append("   maxArrayLength = " + this.sQ + "\n");
      return var1.toString();
   }

   private static final class a {
      static final EncoderContext sS = new EncoderContext(NamespaceTable.getDefaultInstance(), null);
   }
}
