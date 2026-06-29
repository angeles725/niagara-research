package com.prosysopc.ua.stack.encoding.json;

import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.UaOptionSet;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.Enumeration;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.ExtensionObject;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.Structure;
import com.prosysopc.ua.stack.builtintypes.UnsignedByte;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.UnsignedLong;
import com.prosysopc.ua.stack.builtintypes.UnsignedShort;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.builtintypes.XmlElement;
import com.prosysopc.ua.stack.encoding.DecodingException;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.IDecoder;
import com.prosysopc.ua.stack.utils.BijectionMap;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.SimpleTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDecoder implements IDecoder {
   private static final Logger logger = LoggerFactory.getLogger(JsonDecoder.class);
   private static final Map<Class<?>, JsonDecoder.a<?>> sU = new HashMap<>();
   private static final Map<UaNodeId, JsonDecoder.a<?>> sV = new HashMap<>();
   private static final BijectionMap<UaNodeId, Class<?>> sW = new BijectionMap();
   private static final JsonDecoder.a<DateTime> ty = (var0, var1, var2, var3) -> var0.e(var1);
   private static final JsonDecoder.a<ExtensionObject> tz = (var0, var1, var2, var3) -> var0.j(var1);
   private static final JsonDecoder.a<Structure> tA = (var0, var1, var2, var3) -> {
      if (var3 == null) {
         throw new DecodingException("DataTypeId is null, class: " + var2);
      } else {
         StructureSpecification var4 = var0.getEncoderContext().getStructureSpecification(var3);
         if (var4 == null) {
            throw new DecodingException("Cannot find StructureSpecification for DataType: " + var3);
         } else {
            return var0.a(var1, var4);
         }
      }
   };
   private static final JsonDecoder.a<DataValue> tB = (var0, var1, var2, var3) -> var0.d(var1);
   private static final JsonDecoder.a<Variant> tC = (var0, var1, var2, var3) -> var0.z(var1);
   private static final JsonDecoder.a<DiagnosticInfo> tD = (var0, var1, var2, var3) -> var0.g(var1);
   private static final JsonDecoder.a<Enumeration> tE = (var0, var1, var2, var3) -> var0.a(var1, var2, var3);
   private static final JsonDecoder.a<BigDecimal> tF = (var0, var1, var2, var3) -> var0.f(var1);
   private static final JsonDecoder.a<UaOptionSet> tG = (var0, var1, var2, var3) -> var0.a(var1, var3);
   private EncoderContext ti;
   private InputStream in;
   private InputStreamReader tH;
   private com.prosysopc.ua.stack.encoding.json.a tI;
   private Stack<com.prosysopc.ua.stack.encoding.json.a> tJ = new Stack<>();
   private boolean tK = false;
   private char tL;
   private boolean tM = false;

   private static <T> void a(UaNodeId var0, Class<T> var1, JsonDecoder.a<T> var2) throws Error {
      if (var0 != null) {
         if (sV.put(var0, var2) != null) {
            throw new Error("DataType " + var0 + " already has a serializer defined");
         }

         if (sW.containsLeft(var0)) {
            throw new Error("DataType " + var0 + " already mapped to a class");
         }

         if (sW.containsRight(var1)) {
            throw new Error("Class " + var1 + " already mapped to a DataTypeId");
         }

         sW.map(var0, var1);
      }
   }

   private static <T> void b(UaNodeId var0, Class<T> var1, JsonDecoder.a<T> var2) {
      if (!Object.class.equals(var1) && !Modifier.isFinal(var1.getModifiers())) {
         throw new Error("Class " + var1 + " is not final, and cannot be put to known final classes serializtion helper");
      } else {
         JsonDecoder.a var3 = sU.put(var1, var2);
         if (var3 != null) {
            throw new Error("Class " + var1 + " already has a serializer defined");
         } else {
            a(var0, var1, var2);
         }
      }
   }

   private static <T> JsonDecoder.a<T> h(Class<?> var0) throws DecodingException {
      JsonDecoder.a var1 = sU.get(var0);
      if (var1 != null) {
         return var1;
      } else if (ExtensionObject.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tz;
      } else if (Structure.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tA;
      } else if (DataValue.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tB;
      } else if (Variant.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tC;
      } else if (DiagnosticInfo.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tD;
      } else if (Enumeration.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tE;
      } else if (DateTime.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)ty;
      } else if (BigDecimal.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tF;
      } else if (UaOptionSet.class.isAssignableFrom(var0)) {
         return (JsonDecoder.a<T>)tG;
      } else {
         throw new DecodingException("Cannot decode class: " + var0);
      }
   }

   private static JsonDecoder.a<Object> c(UaNodeId var0, EncoderContext var1, AtomicReference<Class<?>> var2) throws DecodingException {
      if (var0 == null) {
         throw new IllegalArgumentException("The given DataTypeId cannot be null");
      } else if (UaIds.BaseDataType.equals(var0)) {
         var2.set(Object.class);
         return h(Object.class);
      } else if (UaIds.Structure.equals(var0)) {
         var2.set(ExtensionObject.class);
         return h(ExtensionObject.class);
      } else {
         JsonDecoder.a var3 = sV.get(var0);
         if (var3 == null) {
            UaDataTypeSpecification var4 = var1.getDataTypeSpecification(var0);
            if (var4 == null) {
               logger.warn("Cannot find UaDataTypeSpecification for DataTypeId: {}", var0);
            } else if (var4 instanceof SimpleTypeSpecification) {
               var3 = sV.get(((SimpleTypeSpecification)var4).getBaseTypeId());
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof StructureSpecification) {
               var3 = tA;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof OptionSetSpecification) {
               var3 = tG;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof EnumerationSpecification) {
               var3 = tE;
               var2.set(var4.getJavaClass());
            }
         } else {
            var2.set(sW.getRight(var0));
         }

         if (var3 == null) {
            throw new DecodingException("Cannot find serializer for DataTypeId: " + var0);
         } else if (var2.get() == null) {
            throw new DecodingException("Cannot find the class mapping for DataTypeId: " + var0);
         } else {
            return var3;
         }
      }
   }

   @Override
   public <T> T get(String var1, UaNodeId var2, int var3) throws DecodingException {
      if (var2 == null) {
         throw new IllegalArgumentException("The given DataTypeId cannot be null");
      } else {
         Class<Object> var4;
         if (UaIds.BaseDataType.equals(var2)) {
            var4 = Object.class;
         } else if (UaIds.Structure.equals(var2)) {
            var4 = ExtensionObject.class;
         } else {
            UaDataTypeSpecification var5 = this.getEncoderContext().getDataTypeSpecification(var2);
            if (var5 == null) {
               throw new DecodingException("Cannot find UaDataTypeSpecification, cannot decode DataType: " + var2);
            }

            var4 = var5.getJavaClass();
         }

         com.prosysopc.ua.stack.encoding.json.a var7 = this.I(var1);
         if (var7 == null) {
            return null;
         } else if (var3 > 0 && !var7.esy()) {
            throw new DecodingException("JsonDecoder: field '" + var1 + "' is not an array as expected");
         } else {
            return this.a(var7, (Class<T>)var4, var2, var3);
         }
      }
   }

   @Override
   public Boolean getBoolean(String var1) throws DecodingException {
      return this.a(var1, Boolean.class);
   }

   @Override
   public Boolean[] getBooleanArray(String var1) throws DecodingException {
      return this.a(var1, Boolean[].class);
   }

   @Override
   public UnsignedByte getByte(String var1) throws DecodingException {
      return this.a(var1, UnsignedByte.class);
   }

   @Override
   public UnsignedByte[] getByteArray(String var1) throws DecodingException {
      return this.a(var1, UnsignedByte[].class);
   }

   @Override
   public ByteString getByteString(String var1) throws DecodingException {
      return this.a(var1, ByteString.class);
   }

   @Override
   public ByteString[] getByteStringArray(String var1) throws DecodingException {
      return this.a(var1, ByteString[].class);
   }

   @Override
   public DataValue getDataValue(String var1) throws DecodingException {
      return this.a(var1, DataValue.class);
   }

   @Override
   public DataValue[] getDataValueArray(String var1) throws DecodingException {
      return this.a(var1, DataValue[].class);
   }

   @Override
   public DateTime getDateTime(String var1) throws DecodingException {
      return this.a(var1, DateTime.class);
   }

   @Override
   public DateTime[] getDateTimeArray(String var1) throws DecodingException {
      return this.a(var1, DateTime[].class);
   }

   @Override
   public DiagnosticInfo getDiagnosticInfo(String var1) throws DecodingException {
      return this.a(var1, DiagnosticInfo.class);
   }

   @Override
   public DiagnosticInfo[] getDiagnosticInfoArray(String var1) throws DecodingException {
      return this.a(var1, DiagnosticInfo[].class);
   }

   @Override
   public Double getDouble(String var1) throws DecodingException {
      return this.a(var1, Double.class);
   }

   @Override
   public Double[] getDoubleArray(String var1) throws DecodingException {
      return this.a(var1, Double[].class);
   }

   @Override
   public EncoderContext getEncoderContext() {
      return this.ti;
   }

   @Override
   public ExpandedNodeId getExpandedNodeId(String var1) throws DecodingException {
      return this.a(var1, ExpandedNodeId.class);
   }

   @Override
   public ExpandedNodeId[] getExpandedNodeIdArray(String var1) throws DecodingException {
      return this.a(var1, ExpandedNodeId[].class);
   }

   @Override
   public ExtensionObject getExtensionObject(String var1) throws DecodingException {
      return this.a(var1, ExtensionObject.class);
   }

   @Override
   public ExtensionObject[] getExtensionObjectArray(String var1) throws DecodingException {
      return this.a(var1, ExtensionObject[].class);
   }

   public Set<String> getFieldNames() throws DecodingException {
      com.prosysopc.ua.stack.encoding.json.a var1 = this.eso();
      return (Set<String>)(!var1.esz() ? new HashSet<>() : var1.esx().getFieldNames());
   }

   @Override
   public Float getFloat(String var1) throws DecodingException {
      return this.a(var1, Float.class);
   }

   @Override
   public Float[] getFloatArray(String var1) throws DecodingException {
      return this.a(var1, Float[].class);
   }

   @Override
   public UUID getGuid(String var1) throws DecodingException {
      return this.a(var1, UUID.class);
   }

   @Override
   public UUID[] getGuidArray(String var1) throws DecodingException {
      return this.a(var1, UUID[].class);
   }

   @Override
   public Short getInt16(String var1) throws DecodingException {
      return this.a(var1, Short.class);
   }

   @Override
   public Short[] getInt16Array(String var1) throws DecodingException {
      return this.a(var1, Short[].class);
   }

   @Override
   public Integer getInt32(String var1) throws DecodingException {
      return this.a(var1, Integer.class);
   }

   @Override
   public Integer[] getInt32Array(String var1) throws DecodingException {
      return this.a(var1, Integer[].class);
   }

   @Override
   public int[] getInt32Array_(String var1) throws DecodingException {
      Integer[] var2 = this.getInt32Array(var1);
      int[] var3 = new int[var2.length];

      for (int var4 = 0; var4 < var2.length; var4++) {
         var3[var4] = var2[var4];
      }

      return var3;
   }

   @Override
   public Long getInt64(String var1) throws DecodingException {
      return this.a(var1, Long.class);
   }

   @Override
   public Long[] getInt64Array(String var1) throws DecodingException {
      return this.a(var1, Long[].class);
   }

   public String getJson(String var1) throws DecodingException {
      com.prosysopc.ua.stack.encoding.json.a var2 = this.I(var1);
      return var2 == null ? null : var2.esC();
   }

   public int getJsonArraySize() throws DecodingException {
      com.prosysopc.ua.stack.encoding.json.a var1 = this.eso();
      return !var1.esy() ? -1 : var1.esw().size();
   }

   @Override
   public LocalizedText getLocalizedText(String var1) throws DecodingException {
      return this.a(var1, LocalizedText.class);
   }

   @Override
   public LocalizedText[] getLocalizedTextArray(String var1) throws DecodingException {
      return this.a(var1, LocalizedText[].class);
   }

   @Override
   public NodeId getNodeId(String var1) throws DecodingException {
      return this.a(var1, NodeId.class);
   }

   @Override
   public NodeId[] getNodeIdArray(String var1) throws DecodingException {
      return this.a(var1, NodeId[].class);
   }

   @Override
   public QualifiedName getQualifiedName(String var1) throws DecodingException {
      return this.a(var1, QualifiedName.class);
   }

   @Override
   public QualifiedName[] getQualifiedNameArray(String var1) throws DecodingException {
      return this.a(var1, QualifiedName[].class);
   }

   @Override
   public Byte getSByte(String var1) throws DecodingException {
      return this.a(var1, Byte.class);
   }

   @Override
   public Byte[] getSByteArray(String var1) throws DecodingException {
      return this.a(var1, Byte[].class);
   }

   @Override
   public StatusCode getStatusCode(String var1) throws DecodingException {
      return this.a(var1, StatusCode.class);
   }

   @Override
   public StatusCode[] getStatusCodeArray(String var1) throws DecodingException {
      return this.a(var1, StatusCode[].class);
   }

   @Override
   public String getString(String var1) throws DecodingException {
      return this.a(var1, String.class);
   }

   @Override
   public String[] getStringArray(String var1) throws DecodingException {
      return this.a(var1, String[].class);
   }

   @Override
   public UnsignedShort getUInt16(String var1) throws DecodingException {
      return this.a(var1, UnsignedShort.class);
   }

   @Override
   public UnsignedShort[] getUInt16Array(String var1) throws DecodingException {
      return this.a(var1, UnsignedShort[].class);
   }

   @Override
   public UnsignedInteger getUInt32(String var1) throws DecodingException {
      return this.a(var1, UnsignedInteger.class);
   }

   @Override
   public UnsignedInteger[] getUInt32Array(String var1) throws DecodingException {
      return this.a(var1, UnsignedInteger[].class);
   }

   @Override
   public UnsignedLong getUInt64(String var1) throws DecodingException {
      return this.a(var1, UnsignedLong.class);
   }

   @Override
   public UnsignedLong[] getUInt64Array(String var1) throws DecodingException {
      return this.a(var1, UnsignedLong[].class);
   }

   @Override
   public Variant getVariant(String var1) throws DecodingException {
      return this.a(var1, Variant.class);
   }

   @Override
   public Variant[] getVariantArray(String var1) throws DecodingException {
      return this.a(var1, Variant[].class);
   }

   @Override
   public XmlElement getXmlElement(String var1) throws DecodingException {
      return this.a(var1, XmlElement.class);
   }

   @Override
   public XmlElement[] getXmlElementArray(String var1) throws DecodingException {
      return this.a(var1, XmlElement[].class);
   }

   public void popElementStack() {
      if (!this.tJ.isEmpty()) {
         this.tJ.pop();
      }
   }

   public void pushElementStack(int var1) throws DecodingException {
      ArrayList var2 = this.eso().esw();
      this.tJ.push((com.prosysopc.ua.stack.encoding.json.a)var2.get(var1));
   }

   public void pushElementStack(String var1) throws DecodingException {
      this.tJ.push(this.I(var1));
   }

   public void setEncoderContext(EncoderContext var1) {
      this.ti = var1;
   }

   public void setReadable(InputStream var1) {
      this.in = var1;
      this.tH = new InputStreamReader(var1, StandardCharsets.UTF_8);
      this.tK = false;
      this.tJ.clear();
   }

   private Object a(com.prosysopc.ua.stack.encoding.json.a var1, int var2) throws DecodingException {
      Class<Boolean> var3;
      switch (var2) {
         case 1:
            var3 = Boolean.class;
            break;
         case 2:
            var3 = Byte.class;
            break;
         case 3:
            var3 = UnsignedByte.class;
            break;
         case 4:
            var3 = Short.class;
            break;
         case 5:
            var3 = UnsignedShort.class;
            break;
         case 6:
            var3 = Integer.class;
            break;
         case 7:
            var3 = UnsignedInteger.class;
            break;
         case 8:
            var3 = Long.class;
            break;
         case 9:
            var3 = UnsignedLong.class;
            break;
         case 10:
            var3 = Float.class;
            break;
         case 11:
            var3 = Double.class;
            break;
         case 12:
            var3 = String.class;
            break;
         case 13:
            var3 = DateTime.class;
            break;
         case 14:
            var3 = UUID.class;
            break;
         case 15:
            var3 = ByteString.class;
            break;
         case 16:
            var3 = XmlElement.class;
            break;
         case 17:
            var3 = NodeId.class;
            break;
         case 18:
            var3 = ExpandedNodeId.class;
            break;
         case 19:
            var3 = StatusCode.class;
            break;
         case 20:
            var3 = QualifiedName.class;
            break;
         case 21:
            var3 = LocalizedText.class;
            break;
         case 22:
            var3 = ExtensionObject.class;
            break;
         case 23:
            var3 = DataValue.class;
            break;
         case 24:
            var3 = Variant.class;
            break;
         case 25:
            var3 = DiagnosticInfo.class;
            break;
         default:
            throw new DecodingException("Cannot decode builtin type id " + var2);
      }

      this.tJ.push(var1);
      Object var4 = this.a("Body", MultiDimensionArrayUtils.arrayClassOf(var3, 1));
      this.tJ.pop();
      return var4;
   }

   private Boolean a(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? Boolean.parseBoolean(var1.getString()) : null;
   }

   private UnsignedByte b(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 != null && !var1.isNull()) {
         String var2 = var1.getString();
         return var2.isEmpty() ? null : UnsignedByte.valueOf(var2);
      } else {
         return null;
      }
   }

   private ByteString c(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? ByteString.valueOf(CryptoUtil.base64Decode(var1.getString())) : null;
   }

   private DataValue d(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 == null) {
         return null;
      } else if (var1.esz()) {
         b var2 = var1.esx();
         Variant var3 = this.z(var2.M("Value"));
         DateTime var4 = this.e(var2.M("SourceTimestamp"));
         UnsignedShort var5 = this.w(var2.M("SourcePicoSeconds"));
         DateTime var6 = this.e(var2.M("ServerTimestamp"));
         UnsignedShort var7 = this.w(var2.M("ServerPicoSeconds"));
         StatusCode var8 = this.u(var2.M("Status"));
         return new DataValue(var3, var8, var4, var5, var6, var7);
      } else {
         return null;
      }
   }

   private DateTime e(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 != null && !var1.isNull()) {
         try {
            return DateTime.parseDateTime(var1.getString());
         } catch (Exception var3) {
            throw new DecodingException(var3);
         }
      } else {
         return null;
      }
   }

   private BigDecimal f(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 != null && !var1.isNull()) {
         b var2 = var1.esx();
         Integer var3 = this.n(var2.M("Scale"));
         Long var4 = Long.valueOf(var2.M("Value").getString());
         return BigDecimal.valueOf(var4, var3);
      } else {
         return null;
      }
   }

   private DiagnosticInfo g(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 != null && !var1.isNull()) {
         b var2 = var1.esx();
         String var3 = this.v(var2.M("Additional Info"));
         DiagnosticInfo var4 = this.g(var2.M("Inner DiagnosticInfo"));
         StatusCode var5 = this.u(var2.M("Inner StatusCode"));
         if (var5 == null) {
            var5 = StatusCode.GOOD;
         }

         Integer var6 = this.n(var2.M("Locale"));
         Integer var7 = this.n(var2.M("LocalizedText"));
         Integer var8 = this.n(var2.M("NamespaceUri"));
         Integer var9 = this.n(var2.M("SymbolicId"));
         return new DiagnosticInfo(var3, var4, var5, var6, var7, var8, var9);
      } else {
         return null;
      }
   }

   private Double h(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? this.B(var1) : null;
   }

   private <T extends Enumeration> T a(com.prosysopc.ua.stack.encoding.json.a var1, Class<T> var2, UaNodeId var3) throws DecodingException {
      if (var1 != null && var1.esA()) {
         try {
            Method var4 = var2.getMethod("valueOf", int.class);
            if (var1.esB()) {
               EnumerationSpecification var9 = this.getEncoderContext().getEnumerationSpecification(var3);
               return (T)(var9 != null ? var9.getByValue(this.C(var1)) : var4.invoke(null, this.C(var1)));
            } else {
               String[] var5 = var1.getString().split("_");
               int var6 = Integer.parseInt(var5[var5.length - 1]);
               EnumerationSpecification var7 = this.getEncoderContext().getEnumerationSpecification(var3);
               return (T)(var7 != null ? var7.getByValue(var6) : var4.invoke(null, var6));
            }
         } catch (Exception var8) {
            throw new DecodingException(var8, "cannot decode" + var2);
         }
      } else {
         return null;
      }
   }

   private ExpandedNodeId i(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 != null && var1.esz()) {
         b var2 = var1.esx();
         Object var3 = null;
         Integer var4 = this.n(var2.M("IdType"));
         if (var4 != null && var4 != 0) {
            switch (var4) {
               case 1:
                  var3 = this.v(var2.M("Id"));
                  break;
               case 2:
                  var3 = this.l(var2.M("Id"));
                  break;
               case 3:
                  var3 = this.c(var2.M("Id"));
            }
         } else {
            var3 = this.x(var2.M("Id"));
         }

         UnsignedInteger var5 = this.x(var2.M("ServerUri"));
         com.prosysopc.ua.stack.encoding.json.a var6 = var2.M("Namespace");
         if (var6 == null) {
            return new ExpandedNodeId(var5, 0, var3);
         } else {
            return var6.esB() ? new ExpandedNodeId(var5, this.C(var6), var3) : new ExpandedNodeId(var5, var6.getString(), var3);
         }
      } else {
         return null;
      }
   }

   private ExtensionObject j(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 != null && var1.esz()) {
         b var2 = var1.esx();
         ExpandedNodeId var3 = this.i(var2.M("TypeId"));
         UaNodeId var4 = UaNodeId.fromLocal(var3, this.ti.getNamespaceTable());
         var3 = var4.asExpandedNodeId();
         com.prosysopc.ua.stack.encoding.json.a var5 = var2.M("Encoding");
         Integer var6 = var5 != null && var5.esB() ? this.C(var5) : 0;
         ExtensionObject var7;
         if (var5 != null && !var5.isNull() && var6 != 0) {
            if (var6 == 1) {
               var7 = new ExtensionObject(var3, this.c(var2.M("Body")));
            } else {
               if (var6 != 2) {
                  throw new DecodingException("Unknown encoding byte: " + var6);
               }

               var7 = new ExtensionObject(var3, this.A(var2.M("Body")));
            }
         } else {
            com.prosysopc.ua.stack.encoding.json.a var8 = var2.M("Body");
            var7 = new ExtensionObject(var3, var8 == null ? null : var8.esC());
         }

         try {
            Structure var11 = var7.decode(this.getEncoderContext());
            return new ExtensionObject(var11);
         } catch (DecodingException var9) {
            return var7;
         }
      } else {
         return null;
      }
   }

   private Float k(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? Float.parseFloat(var1.getString()) : null;
   }

   private UUID l(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? UUID.fromString(var1.getString()) : null;
   }

   private Short m(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? Short.decode(var1.getString()) : null;
   }

   private Integer n(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? this.C(var1) : null;
   }

   private Long o(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? Long.parseLong(var1.getString()) : null;
   }

   private LocalizedText p(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 == null) {
         return null;
      } else if (var1.esz()) {
         b var2 = var1.esx();
         com.prosysopc.ua.stack.encoding.json.a var3 = var2.M("Text");
         String var4 = var3 == null ? null : var3.getString();
         com.prosysopc.ua.stack.encoding.json.a var5 = var2.M("Locale");
         return var5 == null
            ? LocalizedText.builder().setText(var4, LocalizedText.NO_LOCALE).build()
            : LocalizedText.builder().setText(var4, var5.getString()).build();
      } else {
         return var1.esA() ? LocalizedText.builder().setText(var1.getString(), LocalizedText.NO_LOCALE).build() : null;
      }
   }

   private NodeId q(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 != null && var1.esz()) {
         b var2 = var1.esx();
         Integer var3 = 0;
         com.prosysopc.ua.stack.encoding.json.a var4 = var2.M("Namespace");
         if (var4 != null) {
            if (var4.esB()) {
               var3 = this.C(var4);
            } else {
               var3 = this.ti.getNamespaceTable().getIndex(var4.getString());
            }
         }

         Integer var5 = this.n(var2.M("IdType"));
         if (var5 != null && var5 != 0) {
            com.prosysopc.ua.stack.encoding.json.a var6 = var2.M("Id");
            switch (var5) {
               case 1:
                  return new NodeId(var3, this.v(var6));
               case 2:
                  return new NodeId(var3, this.l(var6));
               case 3:
                  return new NodeId(var3, this.c(var6));
               default:
                  return null;
            }
         } else {
            return new NodeId(var3, this.x(var2.M("Id")));
         }
      } else {
         return null;
      }
   }

   private UaOptionSet a(com.prosysopc.ua.stack.encoding.json.a var1, UaNodeId var2) throws DecodingException {
      if (var1 != null && !var1.isNull()) {
         OptionSetSpecification var3 = this.getEncoderContext().getOptionSetSpecification(var2);
         if (var3 == null) {
            throw new DecodingException("Cannot resolve OptionSetSpecification for: " + var2);
         } else {
            Object var4 = this.a(var1, var3.getBaseTypeJavaClass(), var3.getBaseTypeId(), 0);

            try {
               return (UaOptionSet)new Variant(var4).asOptionSet(var3);
            } catch (Exception var6) {
               throw new DecodingException(var6, "Could not resolve the value as UaOptionSet");
            }
         }
      } else {
         return null;
      }
   }

   private QualifiedName r(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      if (var1 == null) {
         return null;
      } else {
         if (var1.esz()) {
            b var2 = var1.esx();
            String var3 = var2.M("Name").getString();
            com.prosysopc.ua.stack.encoding.json.a var4 = var2.M("Uri");
            if (var4 == null) {
               return new QualifiedName(0, var3);
            }

            if (var4.esB()) {
               return new QualifiedName(this.C(var4), var3);
            }

            if (var4.esA()) {
               return new QualifiedName(this.ti.getNamespaceTable().getIndex(var4.getString()), var3);
            }
         }

         return null;
      }
   }

   private com.prosysopc.ua.stack.encoding.json.a eso() throws DecodingException {
      if (!this.tJ.isEmpty()) {
         return this.tJ.peek();
      } else {
         this.ess();
         return this.tI;
      }
   }

   private com.prosysopc.ua.stack.encoding.json.a I(String var1) throws DecodingException {
      com.prosysopc.ua.stack.encoding.json.a var2 = this.eso();
      if (var1 == null) {
         return var2;
      } else if (!var2.esz()) {
         throw new DecodingException("JsonDecoder: Root Json is not an object");
      } else {
         return var2.esx().M(var1);
      }
   }

   private Byte s(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? Byte.parseByte(var1.getString()) : null;
   }

   private Object t(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      b var2 = var1.esx();
      int var3 = this.C(var2.M("Type"));
      com.prosysopc.ua.stack.encoding.json.a var4 = var2.M("Body");

      try {
         switch (var3) {
            case 1:
               return this.a(var4);
            case 2:
               return this.s(var4);
            case 3:
               return this.b(var4);
            case 4:
               return this.m(var4);
            case 5:
               return this.w(var4);
            case 6:
               return this.n(var4);
            case 7:
               return this.x(var4);
            case 8:
               return this.o(var4);
            case 9:
               return this.y(var4);
            case 10:
               return this.k(var4);
            case 11:
               return this.h(var4);
            case 12:
               return this.v(var4);
            case 13:
               return this.e(var4);
            case 14:
               return this.l(var4);
            case 15:
               return this.c(var4);
            case 16:
               return this.A(var4);
            case 17:
               return this.q(var4);
            case 18:
               return this.i(var4);
            case 19:
               return this.u(var4);
            case 20:
               return this.r(var4);
            case 21:
               return this.p(var4);
            case 22:
               return this.j(var4);
            case 23:
               return this.d(var4);
            case 24:
               return this.z(var4);
            case 25:
               return this.g(var4);
         }
      } catch (Exception var6) {
         throw new DecodingException(var6);
      }

      throw new DecodingException("Cannot decode builtin type id " + var3);
   }

   private StatusCode u(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      Integer var2 = 0;
      if (var1 != null) {
         if (var1.esB()) {
            var2 = this.C(var1);
         } else if (var1.esz()) {
            com.prosysopc.ua.stack.encoding.json.a var3 = var1.esx().M("Code");
            if (var3 != null) {
               var2 = this.C(var3);
            }
         }
      }

      return StatusCode.getFromBits(var2);
   }

   private String v(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? var1.getString() : null;
   }

   private UnsignedShort w(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? UnsignedShort.parseUnsignedShort(var1.getString()) : null;
   }

   private UnsignedInteger x(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? UnsignedInteger.parseUnsignedInteger(var1.getString()) : null;
   }

   private UnsignedLong y(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? UnsignedLong.parseUnsignedLong(var1.getString()) : null;
   }

   private Variant z(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      Object var2 = null;
      if (var1 != null && !var1.isNull()) {
         if (var1.esz()) {
            b var3 = var1.esx();
            com.prosysopc.ua.stack.encoding.json.a var4 = var3.M("Type");
            com.prosysopc.ua.stack.encoding.json.a var5 = var3.M("Body");
            if (var5 == null || var4 == null) {
               var2 = var1.esC();
            } else if (var5.esy()) {
               int var6 = this.C(var4);
               var2 = this.a(var1, var6);
               com.prosysopc.ua.stack.encoding.json.a var7 = var3.M("Dimensions");
               if (var7 != null && var7.esy()) {
                  this.tJ.push(var1);
                  int[] var8 = this.getInt32Array_("Dimensions");
                  this.tJ.pop();
                  var2 = MultiDimensionArrayUtils.demuxArray(var2, var8);
               }
            } else {
               var2 = this.t(var1);
            }
         } else if (var1.esy()) {
            var2 = var1.esC();
         } else if (var1.isBoolean()) {
            var2 = this.a(var1);
         } else if (var1.esB()) {
            var2 = this.h(var1);
         } else if (var1.esA()) {
            var2 = this.v(var1);
         }

         return new Variant(var2);
      } else {
         return null;
      }
   }

   private XmlElement A(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      return var1 != null && !var1.isNull() ? new XmlElement(var1.getString()) : null;
   }

   private void esp() throws DecodingException {
      if (this.tM) {
         throw new DecodingException("JsonDecoder: goBack called twice in succession");
      } else {
         this.tM = true;
      }
   }

   private <T> T a(com.prosysopc.ua.stack.encoding.json.a var1, Class<T> var2, UaNodeId var3, int var4) throws DecodingException {
      if (var4 < 0) {
         throw new DecodingException("The given dimensions cannot be negative");
      } else if (var1 == null) {
         return null;
      } else {
         JsonDecoder.a var5 = null;
         UaNodeId var6 = var3;
         if (var3 == null && var2 != null) {
            var6 = (UaNodeId)sW.getLeft(MultiDimensionArrayUtils.getComponentType(var2));
         }

         Class var7;
         if (var6 != null) {
            AtomicReference var8 = new AtomicReference();
            var5 = c(var6, this.getEncoderContext(), var8);
            var7 = (Class)var8.get();
         } else {
            var7 = MultiDimensionArrayUtils.getComponentType(var2);
            var5 = h(var7);
         }

         if (var4 == 0) {
            return (T)var5.get(this, var1, var2, var3);
         } else {
            ArrayList var17 = var1.esw();
            if (var4 == 1) {
               Object[] var18 = (Object[])Array.newInstance(var7, var17.size());

               for (int var19 = 0; var19 < var17.size(); var19++) {
                  var18[var19] = var5.get(this, (com.prosysopc.ua.stack.encoding.json.a)var17.get(var19), var7, var3);
               }

               return (T)var18;
            } else {
               ArrayList var9 = new ArrayList();
               ArrayDeque var10 = new ArrayDeque();
               int[] var11 = new int[var4];
               int var12 = 0;
               var10.add(var17);

               while (!var10.isEmpty()) {
                  List var13 = (List)var10.poll();
                  if (var12 < var4) {
                     var11[var12++] = var13.size();
                  }

                  com.prosysopc.ua.stack.encoding.json.a var14 = (com.prosysopc.ua.stack.encoding.json.a)var13.get(0);
                  if (var14.esy()) {
                     for (int var22 = 0; var22 < var13.size(); var22++) {
                        var10.add(((com.prosysopc.ua.stack.encoding.json.a)var13.get(var22)).esw());
                     }
                  } else {
                     for (int var15 = 0; var15 < var13.size(); var15++) {
                        var9.add(var13.get(var15));
                     }
                  }
               }

               int var20 = var9.size();
               Object[] var21 = (Object[])Array.newInstance(var7, var20);

               for (int var23 = 0; var23 < var20; var23++) {
                  var21[var23] = var5.get(this, (com.prosysopc.ua.stack.encoding.json.a)var9.get(var23), var7, var3);
               }

               return (T)MultiDimensionArrayUtils.demuxArray(var21, var11, var7);
            }
         }
      }
   }

   private Structure a(com.prosysopc.ua.stack.encoding.json.a var1, StructureSpecification var2) throws DecodingException {
      UnsignedLong var3 = null;
      Structure.Builder var4 = var2.toInstanceBuilder();
      if (var1 != null && !var1.isNull()) {
         if (StructureType.OPTIONAL == var2.getStructureType()) {
            var3 = this.y(var1.esx().M("EncodingMask"));
         }

         if (StructureType.UNION != var2.getStructureType() && StructureType.UNION_SUBTYPES != var2.getStructureType()) {
            int var13 = -1;

            for (FieldSpecification var14 : var2.getFields()) {
               com.prosysopc.ua.stack.encoding.json.a var16 = var1.esx().M(var14.getName());
               if (var14.isOptional()) {
                  if ((var3.longValue() & 1 << ++var13) != 0L) {
                     var4.set(var14, this.a(var16, var14));
                  }
               } else {
                  var4.set(var14, this.a(var16, var14));
               }
            }

            return var4.build();
         } else {
            long var5 = 0L;
            com.prosysopc.ua.stack.encoding.json.a var7 = var1.esx().M("SwitchField");
            if (var7 != null) {
               var5 = this.x(var7).longValue();
            }

            if (var5 < 0L) {
               throw new DecodingException("Union SwitchField must be >= 0");
            } else if (var5 == 0L) {
               return var4.build();
            } else {
               long var8 = 0L;

               for (FieldSpecification var11 : var2.getFields()) {
                  if (var5 == ++var8) {
                     com.prosysopc.ua.stack.encoding.json.a var12 = var1.esx().M(var11.getName());
                     var4.set(var11, this.a(var12, var11));
                     return var4.build();
                  }
               }

               throw new DecodingException("Union SwitchField overflow: " + ++var8);
            }
         }
      } else {
         return null;
      }
   }

   private Object a(com.prosysopc.ua.stack.encoding.json.a var1, FieldSpecification var2) throws DecodingException {
      Class var3 = var2.getJavaClass();
      int var4 = MultiDimensionArrayUtils.getClassDimensions(var3);
      if (var2.isAllowSubTypes()) {
         Class var5 = MultiDimensionArrayUtils.getComponentType(var3);
         return ExtensionObject.class.isAssignableFrom(var5)
            ? this.a(var1, MultiDimensionArrayUtils.arrayClassOf(ExtensionObject.class, var4), UaIds.Structure, var4)
            : this.a(var1, MultiDimensionArrayUtils.arrayClassOf(Variant.class, var4), UaIds.BaseDataType, var4);
      } else {
         return this.a(var1, var3, var2.getDataTypeId(), var4);
      }
   }

   private char esq() throws DecodingException {
      char var1;
      do {
         var1 = this.esv();
      } while (Character.isWhitespace(var1));

      return var1;
   }

   private String a(char var1) throws DecodingException {
      char var2 = this.esv();

      StringBuilder var3;
      for (var3 = new StringBuilder(); var2 != var1; var2 = this.esv()) {
         switch (var2) {
            case '\n':
            case '\r':
               throw new DecodingException("JsonDecoder: Unterminated string in Json");
            case '\\':
               var2 = this.esv();
               switch (var2) {
                  case '"':
                  case '\'':
                  case '/':
                  case '\\':
                     var3.append(var2);
                     continue;
                  case 'b':
                     var3.append('\b');
                     continue;
                  case 'f':
                     var3.append('\f');
                     continue;
                  case 'n':
                     var3.append('\n');
                     continue;
                  case 'r':
                     var3.append('\r');
                     continue;
                  case 't':
                     var3.append('\t');
                     continue;
                  case 'u':
                     try {
                        var3.append((char)Integer.parseInt(this.r(4), 16));
                        continue;
                     } catch (NumberFormatException var5) {
                        throw new DecodingException("JsonDecoder: Illegal escape in Json", var5);
                     }
                  default:
                     throw new DecodingException("JsonDecoder: Illegal escape in Json");
               }
            default:
               var3.append(var2);
         }
      }

      return var3.toString();
   }

   private com.prosysopc.ua.stack.encoding.json.a esr() throws DecodingException {
      char var1 = this.esq();
      switch (var1) {
         case '"':
         case '\'':
            return new com.prosysopc.ua.stack.encoding.json.a(this.a(var1));
         case '[':
            this.esp();
            return new com.prosysopc.ua.stack.encoding.json.a(this.est());
         case '{':
            this.esp();
            return new com.prosysopc.ua.stack.encoding.json.a(this.esu());
         default:
            StringBuilder var2 = new StringBuilder();
            this.esp();

            while (true) {
               var1 = this.esv();
               if (var1 == 0) {
                  throw new DecodingException("JsonDecoder: Unexpected end of Json");
               }

               if (Character.isWhitespace(var1) || ",]}".indexOf(var1) > -1) {
                  this.esp();
                  String var3 = var2.toString().trim();
                  if (var3.isEmpty()) {
                     throw new DecodingException("JsonDecoder: Missing value in Json");
                  } else {
                     return "null".equalsIgnoreCase(var3)
                        ? new com.prosysopc.ua.stack.encoding.json.a()
                        : new com.prosysopc.ua.stack.encoding.json.a(var3, true);
                  }
               }

               var2.append(var1);
            }
      }
   }

   private void ess() throws DecodingException {
      if (!this.tK) {
         this.tK = true;
         char var1 = this.esq();
         if (var1 == '{') {
            this.esp();
            this.tI = new com.prosysopc.ua.stack.encoding.json.a(this.esu());
         } else if (var1 == '[') {
            this.esp();
            this.tI = new com.prosysopc.ua.stack.encoding.json.a(this.est());
         } else {
            throw new DecodingException("JsonDecoder: Json text should start with '{' or '[': '" + var1 + "' found");
         }
      }
   }

   private ArrayList<com.prosysopc.ua.stack.encoding.json.a> est() throws DecodingException {
      if (this.esq() != '[') {
         throw new DecodingException("JsonDecoder: Json array must start with '['");
      } else {
         ArrayList var1 = new ArrayList();
         boolean var3 = false;

         while (!var3) {
            char var2 = this.esq();
            switch (var2) {
               case '\u0000':
                  throw new DecodingException("JsonDecoder: Unexpected end of Json");
               case ',':
                  if (var1.isEmpty()) {
                     throw new DecodingException("JsonDecoder: Missing first value in Json array");
                  }
                  break;
               case ']':
                  var3 = true;
                  break;
               default:
                  this.esp();
                  var1.add(this.esr());
            }
         }

         return var1;
      }
   }

   private Double B(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      try {
         return Double.valueOf(var1.getString());
      } catch (Exception var3) {
         throw new DecodingException(var3);
      }
   }

   private int C(com.prosysopc.ua.stack.encoding.json.a var1) throws DecodingException {
      try {
         BigInteger var2 = new BigInteger(var1.getString());
         return var2.intValue();
      } catch (Exception var3) {
         throw new DecodingException(var3);
      }
   }

   private b esu() throws DecodingException {
      if (this.esq() != '{') {
         throw new DecodingException("JsonDecoder: Json object must start with '{");
      } else {
         HashMap var1 = new HashMap();

         while (true) {
            char var2 = this.esq();
            switch (var2) {
               case '\u0000':
                  throw new DecodingException("JsonDecoder: Unexpected end of Json");
               case '}':
                  return new b(var1);
               default:
                  this.esp();
                  String var3 = this.esr().getString();
                  if (this.esq() != ':') {
                     throw new DecodingException("JsonDecoder: Expected ':' after field name");
                  }

                  if (var3 != null) {
                     if (var1.containsKey(var3)) {
                        throw new DecodingException("JsonDecoder: Duplicate field name: " + var3);
                     }

                     com.prosysopc.ua.stack.encoding.json.a var4 = this.esr();
                     var1.put(var3, var4);
                  }

                  switch (this.esq()) {
                     case ',':
                        break;
                     case '}':
                        return new b(var1);
                     default:
                        throw new DecodingException("JsonDecoder: Expected a ',' or '}'");
                  }
            }
         }
      }
   }

   private char esv() throws DecodingException {
      if (this.tM) {
         this.tM = false;
         return this.tL;
      } else {
         int var1;
         try {
            var1 = this.tH.read();
         } catch (IOException var3) {
            throw new DecodingException((Exception)var3);
         }

         if (var1 <= 0) {
            return '\u0000';
         } else {
            this.tL = (char)var1;
            return this.tL;
         }
      }
   }

   private String r(int var1) throws DecodingException {
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var1; var3++) {
         char var4 = this.esv();
         if (var4 == 0) {
            break;
         }

         var2.append(var4);
      }

      return var2.toString();
   }

   @Deprecated
   <T> T a(String var1, Class<T> var2) throws DecodingException {
      int var3 = MultiDimensionArrayUtils.getClassDimensions(var2);
      com.prosysopc.ua.stack.encoding.json.a var4 = this.I(var1);
      if (var4 == null) {
         return null;
      } else if (var3 > 0 && !var4.esy()) {
         throw new DecodingException("JsonDecoder: field '" + var1 + "' is not an array as expected");
      } else {
         return this.a(var4, var2, null, var3);
      }
   }

   static {
      b(UaIds.Boolean, Boolean.class, (var0, var1, var2, var3) -> var0.a(var1));
      b(UaIds.SByte, Byte.class, (var0, var1, var2, var3) -> var0.s(var1));
      b(UaIds.Byte, UnsignedByte.class, (var0, var1, var2, var3) -> var0.b(var1));
      b(UaIds.Int16, Short.class, (var0, var1, var2, var3) -> var0.m(var1));
      b(UaIds.UInt16, UnsignedShort.class, (var0, var1, var2, var3) -> var0.w(var1));
      b(UaIds.Int32, Integer.class, (var0, var1, var2, var3) -> var0.n(var1));
      b(UaIds.UInt32, UnsignedInteger.class, (var0, var1, var2, var3) -> var0.x(var1));
      b(UaIds.Int64, Long.class, (var0, var1, var2, var3) -> var0.o(var1));
      b(UaIds.UInt64, UnsignedLong.class, (var0, var1, var2, var3) -> var0.y(var1));
      b(UaIds.Float, Float.class, (var0, var1, var2, var3) -> var0.k(var1));
      b(UaIds.Double, Double.class, (var0, var1, var2, var3) -> var0.h(var1));
      b(UaIds.String, String.class, (var0, var1, var2, var3) -> var0.v(var1));
      b(UaIds.Guid, UUID.class, (var0, var1, var2, var3) -> var0.l(var1));
      b(UaIds.ByteString, ByteString.class, (var0, var1, var2, var3) -> var0.c(var1));
      b(UaIds.XmlElement, XmlElement.class, (var0, var1, var2, var3) -> var0.A(var1));
      b(UaIds.NodeId, NodeId.class, (var0, var1, var2, var3) -> var0.q(var1));
      b(UaIds.ExpandedNodeId, ExpandedNodeId.class, (var0, var1, var2, var3) -> var0.i(var1));
      b(UaIds.StatusCode, StatusCode.class, (var0, var1, var2, var3) -> var0.u(var1));
      b(UaIds.QualifiedName, QualifiedName.class, (var0, var1, var2, var3) -> var0.r(var1));
      b(UaIds.LocalizedText, LocalizedText.class, (var0, var1, var2, var3) -> var0.p(var1));
      a(UaIds.DateTime, DateTime.class, ty);
      a(UaIds.Structure, ExtensionObject.class, tz);
      a(UaIds.DataValue, DataValue.class, tB);
      a(UaIds.DiagnosticInfo, DiagnosticInfo.class, tD);
      a(UaIds.Decimal, BigDecimal.class, tF);
      b(null, Object.class, (var0, var1, var2, var3) -> tC.get(var0, var1, Variant.class, var3).getValue());
   }

   private interface a<T> {
      T get(JsonDecoder var1, com.prosysopc.ua.stack.encoding.json.a var2, Class<? extends T> var3, UaNodeId var4) throws DecodingException;
   }
}
