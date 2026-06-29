package com.prosysopc.ua.stack.encoding.json;

import com.prosysopc.ua.InternalHasDataTypeId;
import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.UaOptionSet;
import com.prosysopc.ua.stack.builtintypes.BuiltinsMap;
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
import com.prosysopc.ua.stack.core.DataSetFieldContentMask;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.core.DataSetFieldContentMask.Options;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.IEncoder;
import com.prosysopc.ua.stack.utils.BijectionMap;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.OptionSpecification;
import com.prosysopc.ua.typedictionary.SimpleTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonEncoder implements IEncoder {
   private static final Logger logger = LoggerFactory.getLogger(JsonEncoder.class);
   private static final String tO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
   private static DateTimeFormatter tP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
   private static final Map<UaNodeId, JsonEncoder.a<?>> sV = new HashMap<>();
   private static final BijectionMap<UaNodeId, Class<?>> sW = new BijectionMap();
   private static final String tQ = "0001-01-01T00:00:00Z";
   private static final String tR = "9999-12-31T23:59:59Z";
   private static final Map<Class<?>, JsonEncoder.a<?>> sU = new HashMap<>();
   private static final JsonEncoder.a<DateTime> tS = (var0, var1, var2, var3) -> var0.putDateTime(var1, var2);
   private static final JsonEncoder.a<ExtensionObject> tT = (var0, var1, var2, var3) -> var0.putExtensionObject(var1, var2);
   private static final JsonEncoder.a<Structure> tU = (var0, var1, var2, var3) -> {
      StructureSpecification var4 = null;
      if (var2 != null) {
         var4 = var2.specification();
      }

      var0.a(var1, var2, var4);
   };
   private static final JsonEncoder.a<DataValue> tV = (var0, var1, var2, var3) -> var0.putDataValue(var1, var2);
   private static final JsonEncoder.a<Variant> tW = (var0, var1, var2, var3) -> var0.putVariant(var1, var2);
   private static final JsonEncoder.a<DiagnosticInfo> tX = (var0, var1, var2, var3) -> var0.putDiagnosticInfo(var1, var2);
   private static final JsonEncoder.a<Enumeration> tY = (var0, var1, var2, var3) -> var0.putObject(var1, var2);
   private static final JsonEncoder.a<BigDecimal> tZ = (var0, var1, var2, var3) -> var0.a(var1, var2);
   private static final JsonEncoder.a<UaOptionSet> ua = (var0, var1, var2, var3) -> var0.a(var1, var2, var3);
   private EncoderContext ti;
   private List<Integer> ub = new ArrayList<>();
   private boolean uc = true;
   private final Writer writer;
   private List<Integer> ud = new ArrayList<>();
   private int ue;
   private int uf;

   private static <T> void a(UaNodeId var0, Class<T> var1, JsonEncoder.a<T> var2) throws Error {
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

   private static <T> void b(UaNodeId var0, Class<T> var1, JsonEncoder.a<T> var2) {
      if (!Object.class.equals(var1) && !Modifier.isFinal(var1.getModifiers())) {
         throw new Error("Class " + var1 + " is not final, and cannot be put to known final classes serialization helper");
      } else {
         JsonEncoder.a var3 = sU.put(var1, var2);
         if (var3 != null) {
            throw new Error("Class " + var1 + " already has a serializer defined");
         } else {
            a(var0, var1, var2);
         }
      }
   }

   private static <T> JsonEncoder.a<T> i(Class<?> var0) throws EncodingException {
      JsonEncoder.a var1 = sU.get(var0);
      if (var1 != null) {
         return var1;
      } else if (ExtensionObject.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tT;
      } else if (Structure.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tU;
      } else if (DataValue.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tV;
      } else if (Variant.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tW;
      } else if (DiagnosticInfo.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tX;
      } else if (Enumeration.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tY;
      } else if (DateTime.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tS;
      } else if (BigDecimal.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)tZ;
      } else if (UaOptionSet.class.isAssignableFrom(var0)) {
         return (JsonEncoder.a<T>)ua;
      } else {
         throw new EncodingException("Cannot encode class: " + var0);
      }
   }

   private static JsonEncoder.a<Object> d(UaNodeId var0, EncoderContext var1, AtomicReference<Class<?>> var2) throws EncodingException {
      if (var0 == null) {
         throw new IllegalArgumentException("The given DataTypeId cannot be null");
      } else if (UaIds.BaseDataType.equals(var0)) {
         var2.set(Object.class);
         return i(Object.class);
      } else if (UaIds.Structure.equals(var0)) {
         var2.set(ExtensionObject.class);
         return i(ExtensionObject.class);
      } else {
         JsonEncoder.a var3 = sV.get(var0);
         if (var3 == null) {
            UaDataTypeSpecification var4 = var1.getDataTypeSpecification(var0);
            if (var4 == null) {
               logger.warn("Cannot find UaDataTypeSpecification for DataTypeId: {}", var0);
            } else if (var4 instanceof SimpleTypeSpecification) {
               var3 = sV.get(((SimpleTypeSpecification)var4).getBaseTypeId());
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof StructureSpecification) {
               var3 = tU;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof OptionSetSpecification) {
               var3 = ua;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof EnumerationSpecification) {
               var3 = tY;
               var2.set(var4.getJavaClass());
            }
         } else {
            var2.set(sW.getRight(var0));
         }

         if (var3 == null) {
            throw new EncodingException("Cannot find serializer for DataTypeId: " + var0);
         } else if (var2.get() == null) {
            throw new EncodingException("Cannot find the class mapping for DataTypeId: " + var0);
         } else {
            return var3;
         }
      }
   }

   private static void a(Throwable var0, String var1, Object var2) throws EncodingException {
      String var3 = String.format(Locale.ROOT, "fieldname=%s, value=%s", var1, var2);
      if (var0 instanceof ClosedChannelException) {
         throw new EncodingException(StatusCodes.Bad_ConnectionClosed, var0, var3);
      } else if (var0 instanceof EOFException) {
         throw new EncodingException(StatusCodes.Bad_EndOfStream, var0, var3);
      } else if (var0 instanceof ConnectException) {
         throw new EncodingException(StatusCodes.Bad_ConnectionRejected, var0, var3);
      } else if (var0 instanceof SocketException) {
         throw new EncodingException(StatusCodes.Bad_CommunicationError, var0, var3);
      } else {
         throw new EncodingException(StatusCodes.Bad_UnexpectedError, var0, var3);
      }
   }

   public JsonEncoder(Writer var1) {
      this.writer = var1;
      this.ue = -1;
      this.uf = -1;
   }

   public void beginArray() throws EncodingException {
      try {
         this.writer.write("[");
      } catch (IOException var2) {
         a(var2, "beginArray", null);
      }

      this.uf++;
      this.ud.add(0);
   }

   public void beginArray(String var1) throws EncodingException {
      this.K(var1);
      this.beginArray();
   }

   public void beginObject() throws EncodingException {
      try {
         this.writer.write(123);
      } catch (IOException var2) {
         a(var2, "beginObject", null);
      }

      this.ue++;
      this.ub.add(0);
   }

   public void beginObject(String var1) throws EncodingException {
      this.K(var1);
      this.beginObject();
   }

   public void close() throws EncodingException {
      if (this.ue > 0) {
         throw new EncodingException("JsonEncoder: close called without matching beginObject/endObject calls");
      } else if (this.uf > 0) {
         throw new EncodingException("JsonEncoder: close called without matching beginArray/endArray calls");
      } else {
         if (this.ue == 0) {
            this.endObject();
         }

         if (this.uf == 0) {
            this.endArray();
         }

         try {
            this.writer.flush();
         } catch (IOException var2) {
            a(var2, "close", null);
         }
      }
   }

   public void endArray() throws EncodingException {
      if (this.ud.size() > this.uf) {
         if (this.ud.size() > this.uf + 1) {
            throw new EncodingException("JsonEncoder: endArray called with invalid arrayIndex list");
         }

         this.ud.remove(this.uf);
      }

      if (this.uf-- < 0) {
         throw new EncodingException("JsonEncoder: endArray called without beginArray");
      } else {
         try {
            this.writer.write("]");
         } catch (IOException var2) {
            a(var2, "endArray", null);
         }
      }
   }

   public void endObject() throws EncodingException {
      if (this.ub.size() > this.ue) {
         if (this.ub.size() > this.ue + 1) {
            throw new EncodingException("JsonEncoder: endObject called with invalid fieldIndex list");
         }

         this.ub.remove(this.ue);
      }

      if (this.ue-- < 0) {
         throw new EncodingException("JsonEncoder: endObject called without beginObject");
      } else {
         try {
            this.writer.write(125);
         } catch (IOException var2) {
            a(var2, "endObject", this.ue);
         }
      }
   }

   @Override
   public EncoderContext getEncoderContext() {
      return this.ti;
   }

   @Override
   public List<Locale> getLocales() {
      return new ArrayList<>();
   }

   public boolean getReversibleEncoding() {
      return this.uc;
   }

   @Override
   public void put(String var1, Object var2, UaNodeId var3, int var4) throws EncodingException {
      this.a(var1, var2, null, var3, var4);
   }

   public void putArrayElement(Object var1) throws EncodingException {
      this.esH();
      this.g(var1);
   }

   @Override
   public void putBoolean(String var1, Boolean var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putBooleanArray(String var1, Boolean[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putBooleanArray(String var1, Collection<Boolean> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putByte(String var1, UnsignedByte var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putByteArray(String var1, Collection<UnsignedByte> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putByteArray(String var1, UnsignedByte[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putByteString(String var1, ByteString var2) throws EncodingException {
      if (var2 != null) {
         this.putObject(var1, var2.getValue());
      }
   }

   @Override
   public void putByteStringArray(String var1, ByteString[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putByteStringArray(String var1, Collection<ByteString> var2) throws EncodingException {
      this.a(var1, var2);
   }

   public void putComma() throws EncodingException {
      try {
         this.writer.write(",");
      } catch (IOException var2) {
         a(var2, "", "comma");
      }
   }

   @Override
   public void putDataValue(String var1, DataValue var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         if (var2.getValue().getValue() != null) {
            this.putVariant("Value", var2.getValue());
         }

         if (var2.getStatusCode() != StatusCode.GOOD) {
            this.putStatusCode("Status", var2.getStatusCode());
         }

         if (var2.getSourceTimestamp() != null) {
            this.putObject("SourceTimestamp", var2.getSourceTimestamp());
         }

         if (var2.getSourcePicoseconds() != null && var2.getSourcePicoseconds().intValue() != 0) {
            this.putObject("SourcePicoSeconds", var2.getSourcePicoseconds());
         }

         if (var2.getServerTimestamp() != null) {
            this.putObject("ServerTimestamp", var2.getServerTimestamp());
         }

         if (var2.getServerPicoseconds() != null && var2.getServerPicoseconds().intValue() != 0) {
            this.putObject("ServerPicoSeconds", var2.getServerPicoseconds());
         }

         this.endObject();
      }
   }

   public void putDataValue(String var1, DataValue var2, DataSetFieldContentMask var3) throws EncodingException {
      if (var2 != null) {
         boolean var4 = var3.contains(new OptionSpecification[]{Options.RawData});
         boolean var5 = var4 || ((UnsignedInteger)var3.asBuiltInType()).getValue() == 0L;
         if (!var4 && !var5) {
            this.K(var1);
            this.beginObject();
            this.putVariant("Value", var2.getValue());
            if (var3.contains(new OptionSpecification[]{Options.StatusCode}) && var2.getStatusCode() != StatusCode.GOOD) {
               this.putStatusCode("Status", var2.getStatusCode());
            }

            if (var3.contains(new OptionSpecification[]{Options.SourceTimestamp})) {
               this.putObject("SourceTimestamp", var2.getSourceTimestamp());
               if (var2.getSourcePicoseconds() != null && var2.getSourcePicoseconds().intValue() != 0) {
                  this.putObject("SourcePicoSeconds", var2.getSourcePicoseconds());
               }
            }

            if (var3.contains(new OptionSpecification[]{Options.ServerTimestamp})) {
               this.putObject("ServerTimestamp", var2.getServerTimestamp());
               if (var2.getServerPicoseconds() != null && var2.getServerPicoseconds().intValue() != 0) {
                  this.putObject("ServerPicoSeconds", var2.getServerPicoseconds());
               }
            }

            this.endObject();
         } else {
            this.putVariant(var1, var2.getValue());
         }
      }
   }

   @Override
   public void putDataValueArray(String var1, Collection<DataValue> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (DataValue var4 : var2) {
            this.esH();
            if (var4 == null) {
               this.putNull();
            } else {
               this.putDataValue(null, var4);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putDataValueArray(String var1, DataValue[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (DataValue var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putDataValue(null, var6);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putDateTime(String var1, DateTime var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putDateTimeArray(String var1, Collection<DateTime> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putDateTimeArray(String var1, DateTime[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putDiagnosticInfo(String var1, DiagnosticInfo var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         this.putObject("SymbolicId", var2.getSymbolicId());
         this.putObject("NamespaceUri", var2.getNamespaceUri());
         this.putObject("Locale", var2.getLocale());
         this.putObject("LocalizedText", var2.getLocalizedText());
         this.putObject("Additional Info", var2.getAdditionalInfo());
         if (var2.getInnerStatusCode() != null && var2.getInnerStatusCode().isNotGood()) {
            if (this.uc) {
               this.putObject("Inner StatusCode", var2.getInnerStatusCode().getValue());
            } else {
               this.putStatusCode(null, var2.getInnerStatusCode());
            }
         }

         if (var2.getInnerDiagnosticInfo() != null) {
            this.putDiagnosticInfo("Inner DiagnosticInfo", var2.getInnerDiagnosticInfo());
         }

         this.endObject();
      }
   }

   @Override
   public void putDiagnosticInfoArray(String var1, Collection<DiagnosticInfo> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (DiagnosticInfo var4 : var2) {
            this.esH();
            if (var4 == null) {
               this.putNull();
            } else {
               this.putDiagnosticInfo(null, var4);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putDiagnosticInfoArray(String var1, DiagnosticInfo[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (DiagnosticInfo var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putDiagnosticInfo(null, var6);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putDouble(String var1, double var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putDouble(String var1, Double var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putDoubleArray(String var1, Collection<Double> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putDoubleArray(String var1, Double[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putExpandedNodeId(String var1, ExpandedNodeId var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         this.s(var2.getIdType().getValue());
         this.putObject("Id", var2.getValue());
         if (var2.getNamespaceUri() != null) {
            this.a("Namespace", this.ti.getNamespaceTable().getIndex(var2.getNamespaceUri()));
         } else {
            this.a("Namespace", var2.getNamespaceIndex());
         }

         this.k(var2.getServerIndex());
         this.endObject();
      }
   }

   @Override
   public void putExpandedNodeIdArray(String var1, Collection<ExpandedNodeId> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (ExpandedNodeId var4 : var2) {
            this.esH();
            if (var4 == null) {
               this.putNull();
            } else {
               this.putExpandedNodeId(null, var4);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putExpandedNodeIdArray(String var1, ExpandedNodeId[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (ExpandedNodeId var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putExpandedNodeId(null, var6);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putExtensionObject(String var1, ExtensionObject var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         if (var2.getTypeId() != null && this.getReversibleEncoding()) {
            this.putExpandedNodeId("TypeId", var2.getTypeId());
         } else if (this.getReversibleEncoding() && var2.getObject() instanceof Structure) {
            StructureSpecification var3 = ((Structure)var2.getObject()).specification();
            UaNodeId var4 = var3.getJsonEncodeId();
            if (var4 != null) {
               this.putExpandedNodeId("TypeId", var4.asExpandedNodeId());
            }
         }

         Object var5 = var2.getObject();
         if (var5 != null) {
            if (var5 instanceof ByteString) {
               if (this.getReversibleEncoding()) {
                  this.putObject("Encoding", 1);
               }

               this.putByteString("Body", (ByteString)var5);
            } else if (var5 instanceof XmlElement) {
               if (this.getReversibleEncoding()) {
                  this.putObject("Encoding", 2);
               }

               this.putXmlElement("Body", (XmlElement)var5);
            } else {
               this.putStructure("Body", (Structure)var2.getObject());
            }
         }

         this.endObject();
      }
   }

   @Override
   public void putExtensionObjectArray(String var1, Collection<ExtensionObject> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (ExtensionObject var4 : var2) {
            this.esH();
            if (var4 == null) {
               this.putNull();
            } else {
               this.putExtensionObject(null, var4);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putExtensionObjectArray(String var1, ExtensionObject[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (ExtensionObject var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putExtensionObject(null, var6);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putFloat(String var1, float var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putFloat(String var1, Float var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putFloatArray(String var1, Collection<Float> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putFloatArray(String var1, Float[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putGuid(String var1, UUID var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putGuidArray(String var1, Collection<UUID> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putGuidArray(String var1, UUID[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putInt16(String var1, short var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putInt16(String var1, Short var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putInt16Array(String var1, Collection<Short> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putInt16Array(String var1, Short[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putInt32(String var1, int var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putInt32(String var1, Integer var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putInt32Array(String var1, Collection<Integer> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putInt32Array(String var1, int[] var2) throws EncodingException {
      if (var2 != null) {
         this.p(var2.length);
         this.K(var1);
         this.beginArray();

         for (int var6 : var2) {
            this.putArrayElement(var6);
         }

         this.endArray();
      }
   }

   @Override
   public void putInt32Array(String var1, Integer[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putInt64(String var1, long var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putInt64(String var1, Long var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putInt64Array(String var1, Collection<Long> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putInt64Array(String var1, Long[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putLocalizedText(String var1, LocalizedText var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         var2 = var2.asSingleLocale(this.getLocales());
         String var3 = var2.getLocaleId();
         String var4 = var2.getText();
         if (this.uc) {
            this.beginObject();
            if (var4 != null && !var4.isEmpty()) {
               this.putObject("Text", var4);
            }

            if (var3 != null && !var3.isEmpty()) {
               this.putObject("Locale", var3);
            }

            this.endObject();
         } else {
            this.g(var2.getText());
         }
      }
   }

   @Override
   public void putLocalizedTextArray(String var1, Collection<LocalizedText> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (LocalizedText var4 : var2) {
            this.esH();
            if (var4 == null) {
               this.putNull();
            } else {
               this.putLocalizedText(null, var4);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putLocalizedTextArray(String var1, LocalizedText[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (LocalizedText var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putLocalizedText(null, var6);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putNodeId(String var1, NodeId var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         this.s(var2.getIdType().getValue());
         this.putObject("Id", var2.getValue());
         this.a("Namespace", var2.getNamespaceIndex());
         this.endObject();
      }
   }

   @Override
   public void putNodeIdArray(String var1, Collection<NodeId> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (NodeId var4 : var2) {
            if (var4 == null) {
               this.putNull();
            } else {
               this.putNodeId(null, var4);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putNodeIdArray(String var1, NodeId[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (NodeId var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putNodeId(null, var6);
            }
         }

         this.endArray();
      }
   }

   @Deprecated
   public void putObject(String var1, Object var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.g(var2);
      }
   }

   @Override
   public void putQualifiedName(String var1, QualifiedName var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         this.putObject("Name", var2.getName());
         this.a("Uri", var2.getNamespaceIndex());
         this.endObject();
      }
   }

   @Override
   public void putQualifiedNameArray(String var1, Collection<QualifiedName> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (QualifiedName var4 : var2) {
            this.esH();
            if (var4 == null) {
               this.putNull();
            } else {
               this.putQualifiedName(null, var4);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putQualifiedNameArray(String var1, QualifiedName[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (QualifiedName var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putQualifiedName(null, var6);
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putSByte(String var1, byte var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putSByte(String var1, Byte var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putSByte(String var1, int var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putSByteArray(String var1, Byte[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putSByteArray(String var1, Collection<Byte> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putStatusCode(String var1, StatusCode var2) throws EncodingException {
      if (var2 != null) {
         if (StatusCode.GOOD.equals(var2)) {
            this.K(null);
            if (this.esD() > 0) {
               this.putNull();
            }
         } else {
            this.K(var1);
            if (!this.uc) {
               this.beginObject();
               this.putObject("Code", var2.getValue());
               this.putObject("Symbol", var2.getName());
               this.endObject();
            } else {
               this.g(var2.getValue());
            }
         }
      }
   }

   @Override
   public void putStatusCodeArray(String var1, Collection<StatusCode> var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.size());
         this.beginArray();

         for (StatusCode var4 : var2) {
            this.esH();
            if (var4 != null && var4 != StatusCode.GOOD) {
               this.putStatusCode(null, var4);
            } else {
               this.putNull();
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putStatusCodeArray(String var1, StatusCode[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (StatusCode var6 : var2) {
            this.esH();
            if (var6 != null && !var6.isGood()) {
               this.putStatusCode(null, var6);
            } else {
               this.putNull();
            }
         }

         this.endArray();
      }
   }

   @Override
   public void putString(String var1, String var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putStringArray(String var1, Collection<String> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putStringArray(String var1, String[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Deprecated
   public void putStructure(String var1, Structure var2) throws EncodingException {
      if (var2 != null) {
         this.a(var1, var2, var2.getClass());
      }
   }

   @Override
   public void putUInt16(String var1, UnsignedShort var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putUInt16Array(String var1, Collection<UnsignedShort> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putUInt16Array(String var1, UnsignedShort[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putUInt32(String var1, UnsignedInteger var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putUInt32Array(String var1, Collection<UnsignedInteger> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putUInt32Array(String var1, UnsignedInteger[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putUInt64(String var1, UnsignedLong var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putUInt64Array(String var1, Collection<UnsignedLong> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putUInt64Array(String var1, UnsignedLong[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Deprecated
   @Override
   public void putVariant(String var1, Variant var2) throws EncodingException {
      if (var2 != null && !var2.isEmpty()) {
         Integer var3 = BuiltinsMap.ID_MAP.get(var2.getCompositeClass());
         if (var3 == null && Structure.class.isAssignableFrom(var2.getCompositeClass())) {
            var3 = 22;
         }

         if (var3 == null || var3 == 0) {
            return;
         }

         this.K(var1);
         if (this.uc) {
            this.beginObject();
            this.putObject("Type", var3);
            this.a("Body", var2);
            int[] var4 = var2.getArrayDimensions();
            if (var4.length > 1) {
               this.putInt32Array("Dimensions", var4);
            }

            this.endObject();
         } else {
            this.a(null, var2);
         }
      }
   }

   @Deprecated
   public void putVariantArray(String var1, Collection<Variant> var2) throws EncodingException {
      this.p(var2.size());
      this.K(var1);
      this.beginArray();

      for (Variant var4 : var2) {
         this.esH();
         if (var4 != null && BuiltinsMap.ID_MAP.get(var4.getCompositeClass()) != 0) {
            this.putVariant(null, var4);
         } else {
            this.putNull();
         }
      }

      this.endArray();
   }

   @Override
   public void putVariantArray(String var1, Variant[] var2) throws EncodingException {
      this.p(var2.length);
      this.K(var1);
      this.beginArray();

      for (Variant var6 : var2) {
         this.esH();
         if (var6 != null && BuiltinsMap.ID_MAP.get(var6.getCompositeClass()) != 0) {
            this.putVariant(null, var6);
         } else {
            this.putNull();
         }
      }

      this.endArray();
   }

   @Override
   public void putXmlElement(String var1, XmlElement var2) throws EncodingException {
      this.putObject(var1, var2);
   }

   @Override
   public void putXmlElementArray(String var1, Collection<XmlElement> var2) throws EncodingException {
      this.a(var1, var2);
   }

   @Override
   public void putXmlElementArray(String var1, XmlElement[] var2) throws EncodingException {
      this.a(var1, var2);
   }

   public void setEncoderContext(EncoderContext var1) {
      this.ti = var1;
   }

   public void setReversibleEncoding(boolean var1) {
      this.uc = var1;
   }

   @Override
   public String toString() {
      return this.writer.toString();
   }

   private void p(int var1) throws EncodingException {
      int var2 = this.ti.getMaxArrayLength();
      if (var2 > 0 && var1 > var2) {
         EncodingException var3 = new EncodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxArrayLength " + var2 + " < " + var1);
         logger.warn("assertArrayLength: failed", var3);
         throw var3;
      }
   }

   private void l(int var1) throws EncodingException {
      int var2 = this.ti.getMaxByteStringLength();
      if (var2 > 0 && var1 > var2) {
         EncodingException var3 = new EncodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxByteStringLength " + var2 + " < " + var1);
         throw var3;
      }
   }

   private Integer esD() {
      return this.uf < 0 ? -1 : this.ud.get(this.uf);
   }

   private Integer esE() {
      return this.ub.get(this.ue);
   }

   private void a(String var1, Object var2, Class<?> var3, UaNodeId var4, int var5) throws EncodingException {
      JsonEncoder.a var6 = null;
      if (var4 == null && var3 != null) {
      }

      Class var8;
      if (var4 != null) {
         AtomicReference var9 = new AtomicReference();
         var6 = d(var4, this.getEncoderContext(), var9);
         var8 = (Class)var9.get();
      } else {
         var8 = MultiDimensionArrayUtils.getComponentType(var3);
         var6 = i(var8);
      }

      if (var5 == 0) {
         var6.put(this, var1, var2, var8);
      } else if (var5 == 1) {
         if (var2 != null) {
            Object[] var19 = (Object[])var2;
            this.p(var19.length);
            if (var1 != null) {
               this.K(var1);
               this.beginArray();

               for (int var21 = 0; var21 < var19.length; var21++) {
                  this.a(var19[var21], var6, var8);
               }

               this.endArray();
            }
         }
      } else if (var2 == null) {
         int[] var18 = new int[var5];

         for (int var20 = 0; var20 < var18.length; var20++) {
            var18[var20] = -1;
         }

         this.putInt32Array(null, var18);
      } else {
         int[] var17 = MultiDimensionArrayUtils.getArrayLengths(var2);
         Object[] var10 = (Object[])MultiDimensionArrayUtils.muxArray(var2, var17, var8);
         int var11 = var10.length;
         this.p(var11);
         this.putInt32Array(null, var17);

         for (Object var15 : var10) {
            var6.put(this, null, var15, var8);
         }
      }
   }

   private void a(String var1, Structure var2, StructureSpecification var3) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         this.b(var2, var3);
         this.endObject();
      }
   }

   private void esF() {
      this.ud.set(this.uf, this.esD() + 1);
   }

   private void esG() {
      this.ub.set(this.ue, this.esE() + 1);
   }

   private String J(String var1) {
      StringBuilder var2 = new StringBuilder(var1.length());

      for (int var3 = 0; var3 < var1.length(); var3++) {
         char var4 = var1.charAt(var3);
         switch (var4) {
            case '\b':
               var2.append("\\b");
               break;
            case '\t':
               var2.append("\\t");
               break;
            case '\n':
               var2.append("\\n");
               break;
            case '\f':
               var2.append("\\f");
               break;
            case '\r':
               var2.append("\\r");
               break;
            case '"':
               var2.append("\\\"");
               break;
            case '\\':
               var2.append("\\\\");
               break;
            default:
               var2.append(var4);
         }
      }

      return var2.toString();
   }

   private void a(String var1, Collection<?> var2) throws EncodingException {
      if (var2 != null) {
         this.p(var2.size());
         this.K(var1);
         this.beginArray();

         for (Object var4 : var2) {
            this.putArrayElement(var4);
         }

         this.endArray();
      }
   }

   private void a(String var1, Object[] var2) throws EncodingException {
      if (var2 != null) {
         this.p(var2.length);
         this.K(var1);
         this.beginArray();

         for (Object var6 : var2) {
            if (var6 != null && MultiDimensionArrayUtils.getDimension(var6) > 0) {
               this.esH();
               this.a(null, (Object[])var6);
            } else {
               this.putArrayElement(var6);
            }
         }

         this.endArray();
      }
   }

   private void esH() throws EncodingException {
      if (this.esD() > 0) {
         this.putComma();
      }

      this.esF();
   }

   private void a(Object var1, JsonEncoder.a<Object> var2, Class<?> var3) throws EncodingException {
      this.esH();
      if (var1 == null) {
         this.putNull();
      } else {
         var2.put(this, null, var1, var3);
      }
   }

   private void a(String var1, BigDecimal var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.beginObject();
         this.putObject("Scale", var2.scale());
         this.putObject("Value", var2.unscaledValue());
         this.endObject();
      }
   }

   private void K(String var1) throws EncodingException {
      if (this.ue == -1 && this.uf == -1) {
         this.beginObject();
      }

      if (var1 != null) {
         if (this.esE() > 0) {
            this.putComma();
         }

         try {
            this.writer.write(this.L(var1) + ":");
         } catch (IOException var3) {
            a(var3, "fieldName", var1);
         }

         this.esG();
      }
   }

   private void s(int var1) throws EncodingException {
      if (var1 != 0) {
         this.putObject("IdType", var1);
      }
   }

   private void a(String var1, int var2) throws EncodingException {
      if (var2 != 0) {
         if (var2 != 1 && !this.uc) {
            String var3 = this.ti.getNamespaceTable().getUri(var2);
            if (var3 == null) {
               throw new EncodingException(new String("Unable to find matching Namespace Uri for Namespace Index: " + var2));
            }

            this.putObject(var1, var3);
         } else {
            this.putObject(var1, var2);
         }
      }
   }

   private void putNull() throws EncodingException {
      try {
         this.writer.write("null");
      } catch (IOException var2) {
         a(var2, "null", null);
      }
   }

   private void a(String var1, UaOptionSet var2, Class<? extends UaOptionSet> var3) throws EncodingException {
      if (var2 != null) {
         this.put(var1, var2, var2.specification().getTypeId(), 0);
      }
   }

   private void k(UnsignedInteger var1) throws EncodingException {
      if (var1.intValue() != 0) {
         if (this.uc) {
            this.putObject("ServerUri", var1);
         } else {
            String var2 = this.ti.getServerTable().getUri(var1.intValue());
            if (var2 == null) {
               throw new EncodingException(new String("Unable to find matching Server Uri for Server Index: " + var1));
            }

            this.putObject("ServerUri", var2);
         }
      }
   }

   private void b(Structure var1, StructureSpecification var2) throws EncodingException {
      if (var1 != null) {
         Map var3 = var1.toFieldsMap(var2);
         if (StructureType.UNION != var2.getStructureType() && StructureType.UNION_SUBTYPES != var2.getStructureType()) {
            if (this.uc && StructureType.OPTIONAL == var2.getStructureType()) {
               int var10 = -1;
               long var5 = 0L;

               for (Entry var17 : var3.entrySet()) {
                  if (((FieldSpecification)var17.getKey()).isOptional()) {
                     var10++;
                     if (var17.getValue() != null) {
                        var5 |= 1 << var10;
                     }
                  }
               }

               this.putUInt64("EncodingMask", UnsignedLong.getFromBits(var5));
            }

            for (Entry var12 : var3.entrySet()) {
               FieldSpecification var13 = (FieldSpecification)var12.getKey();
               Object var15 = var12.getValue();
               if (!var13.isOptional() || var15 != null) {
                  int var18 = var13.getValueRank() < 0 ? 0 : var13.getValueRank();
                  if (var13.isAllowSubTypes()) {
                     if (ExtensionObject.class.isAssignableFrom(var13.getCompositeClass())) {
                        this.put(var13.getName(), var15, UaIds.Structure, var18);
                     } else {
                        if (!Object.class.equals(var13.getCompositeClass())) {
                           throw new EncodingException(
                              "The java class for AllowSubTypes field should either be ExtensionObject.class or Object.class, got: "
                                 + var13
                                 + "in structure type: "
                                 + var2
                           );
                        }

                        this.put(var13.getName(), var15, UaIds.BaseDataType, var18);
                     }
                  } else {
                     this.put(var13.getName(), var15, var13.getDataTypeId(), var18);
                  }
               }
            }
         } else {
            long var4 = 0L;
            FieldSpecification var6 = null;
            Object var7 = null;

            for (Entry var9 : var3.entrySet()) {
               var4++;
               if (var9.getValue() != null) {
                  var6 = (FieldSpecification)var9.getKey();
                  var7 = var9.getValue();
                  break;
               }
            }

            if (var6 != null) {
               this.putUInt32("SwitchField", UnsignedInteger.valueOf(var4));
               int var16 = var6.getValueRank() < 0 ? 0 : var6.getValueRank();
               this.put(var6.getName(), var7, var6.getDataTypeId(), var16);
            }
         }
      }
   }

   @Deprecated
   private void a(String var1, Structure[] var2) throws EncodingException {
      if (var2 != null) {
         this.K(var1);
         this.p(var2.length);
         this.beginArray();

         for (Structure var6 : var2) {
            this.esH();
            if (var6 == null) {
               this.putNull();
            } else {
               this.putStructure(null, var6);
            }
         }

         this.endArray();
      }
   }

   private void g(Object var1) throws EncodingException {
      try {
         this.writer.write(this.h(var1));
      } catch (IOException var3) {
         a(var3, "value", null);
      }
   }

   private void a(String var1, Variant var2) throws EncodingException {
      if (var2 != null) {
         if (var2 instanceof InternalHasDataTypeId) {
            UaNodeId var3 = ((InternalHasDataTypeId)var2).getDataTypeId();
            if (var3 != null) {
               EnumerationSpecification var4 = this.getEncoderContext().getEnumerationSpecification(var3);
               if (var4 != null) {
                  Object var5 = var2.asEnum(var4);
                  this.put(var1, var5, var4.getTypeId(), MultiDimensionArrayUtils.getDimension(var5));
                  return;
               }
            }
         }

         if (var2.getDimension() > 0) {
            this.b(var1, var2);
            return;
         }

         Object var6 = var2.getValue();
         Integer var7 = BuiltinsMap.ID_MAP.get(var2.getCompositeClass());
         if (var7 == null && Structure.class.isAssignableFrom(var2.getCompositeClass())) {
            var7 = 22;
         }

         switch (var7) {
            case 1:
               this.putBoolean(var1, (Boolean)var6);
               break;
            case 2:
               this.putSByte(var1, (Byte)var6);
               break;
            case 3:
               this.putByte(var1, (UnsignedByte)var6);
               break;
            case 4:
               this.putInt16(var1, (Short)var6);
               break;
            case 5:
               this.putUInt16(var1, (UnsignedShort)var6);
               break;
            case 6:
               this.putInt32(var1, (Integer)var6);
               break;
            case 7:
               this.putUInt32(var1, (UnsignedInteger)var6);
               break;
            case 8:
               this.putInt64(var1, (Long)var6);
               break;
            case 9:
               this.putUInt64(var1, (UnsignedLong)var6);
               break;
            case 10:
               this.putFloat(var1, (Float)var6);
               break;
            case 11:
               this.putDouble(var1, (Double)var6);
               break;
            case 12:
               this.putString(var1, (String)var6);
               break;
            case 13:
               this.putDateTime(var1, (DateTime)var6);
               break;
            case 14:
               this.putGuid(var1, (UUID)var6);
               break;
            case 15:
               this.putByteString(var1, (ByteString)var6);
               break;
            case 16:
               this.putXmlElement(var1, (XmlElement)var6);
               break;
            case 17:
               this.putNodeId(var1, (NodeId)var6);
               break;
            case 18:
               this.putExpandedNodeId(var1, (ExpandedNodeId)var6);
               break;
            case 19:
               this.putStatusCode(var1, (StatusCode)var6);
               break;
            case 20:
               this.putQualifiedName(var1, (QualifiedName)var6);
               break;
            case 21:
               this.putLocalizedText(var1, (LocalizedText)var6);
               break;
            case 22:
               if (var6 instanceof Structure) {
                  this.putExtensionObject(var1, new ExtensionObject((Structure)var6));
               } else {
                  this.putExtensionObject(var1, (ExtensionObject)var6);
               }
               break;
            case 23:
               this.putDataValue(var1, (DataValue)var6);
               break;
            case 24:
               this.putVariant(var1, (Variant)var6);
               break;
            case 25:
               this.putDiagnosticInfo(var1, (DiagnosticInfo)var6);
               break;
            default:
               throw new EncodingException("cannot encode builtin type " + var7);
         }
      }
   }

   private void b(String var1, Variant var2) throws EncodingException {
      int[] var3 = var2.getArrayDimensions();
      Object var4 = var2.getValue();
      if (this.uc && var3.length > 1) {
         Object[] var5 = (Object[])MultiDimensionArrayUtils.muxArray(var4, var3, var2.getCompositeClass());
         this.a(var1, var5);
      } else {
         this.a(var1, (Object[])var4);
      }
   }

   private String c(byte[] var1) throws EncodingException {
      if (var1 == null) {
         return "null";
      } else {
         this.l(var1.length);
         return "\"" + CryptoUtil.base64Encode(var1) + "\"";
      }
   }

   private String f(DateTime var1) {
      String var2;
      try {
         if (var1.compareTo(DateTime.MAX_VALUE) >= 0) {
            var2 = "9999-12-31T23:59:59Z";
         } else if (var1.compareTo(DateTime.MIN_VALUE) <= 0) {
            var2 = "0001-01-01T00:00:00Z";
         } else {
            var2 = tP.format(var1.toInstant());
         }
      } catch (Exception var4) {
         var2 = "0001-01-01T00:00:00Z";
      }

      return this.L(var2);
   }

   private String d(Double var1) {
      return !var1.equals(Double.NaN) ? var1.toString() : "\"NaN\"";
   }

   private String b(Enumeration var1) {
      if (var1 == null) {
         throw new IllegalStateException("Given Enumeration value shouldn't be null at this point");
      } else {
         String var2 = null;
         if (!this.uc) {
            if (var1.specification() != null) {
               var2 = (String)var1.specification().getIntToStringMappings().get(var1.getValue());
            } else {
               logger.warn("Encountered Enumeration for which .specification returned null, using number form for encoding as a fallback");
            }
         }

         return var2 == null ? Integer.toString(var1.getValue()) : this.L(var2 + "_" + var1.getValue());
      }
   }

   private String b(Float var1) {
      return !var1.equals(Float.NaN) ? var1.toString() : "\"NaN\"";
   }

   private String L(String var1) {
      if (var1 == null) {
         return "null";
      } else {
         return var1.startsWith("{") ? var1 : "\"" + this.J(var1) + "\"";
      }
   }

   private String b(UUID var1) {
      return this.L(var1.toString());
   }

   private String b(XmlElement var1) {
      return this.L(var1.getValue());
   }

   private String h(Object var1) throws EncodingException {
      if (var1 instanceof byte[]) {
         return this.c((byte[])var1);
      } else if (var1 instanceof ByteString) {
         return this.c(((ByteString)var1).getValue());
      } else if (var1 instanceof String || var1 == null) {
         return this.L((String)var1);
      } else if (var1 instanceof Enumeration) {
         return this.b((Enumeration)var1);
      } else if (var1 instanceof Float) {
         return this.b((Float)var1);
      } else if (var1 instanceof Double) {
         return this.d((Double)var1);
      } else if (var1 instanceof DateTime) {
         return this.f((DateTime)var1);
      } else if (var1 instanceof UUID) {
         return this.b((UUID)var1);
      } else if (var1 instanceof XmlElement) {
         return this.b((XmlElement)var1);
      } else {
         return !(var1 instanceof Long) && !(var1 instanceof UnsignedLong) ? var1.toString() : "\"" + var1 + "\"";
      }
   }

   @Deprecated
   void put(String var1, Object var2) throws EncodingException {
      this.a(var1, var2, var2.getClass());
   }

   @Deprecated
   void a(String var1, Object var2, Class<?> var3) throws EncodingException {
      int var4 = MultiDimensionArrayUtils.getClassDimensions(var3);
      this.a(var1, var2, var3, null, var4);
   }

   static {
      b(UaIds.Boolean, Boolean.class, (var0, var1, var2, var3) -> var0.putBoolean(var1, var2));
      b(UaIds.SByte, Byte.class, (var0, var1, var2, var3) -> var0.putSByte(var1, var2));
      b(UaIds.Byte, UnsignedByte.class, (var0, var1, var2, var3) -> var0.putByte(var1, var2));
      b(UaIds.Int16, Short.class, (var0, var1, var2, var3) -> var0.putInt16(var1, var2));
      b(UaIds.UInt16, UnsignedShort.class, (var0, var1, var2, var3) -> var0.putUInt16(var1, var2));
      b(UaIds.Int32, Integer.class, (var0, var1, var2, var3) -> var0.putInt32(var1, var2));
      b(UaIds.UInt32, UnsignedInteger.class, (var0, var1, var2, var3) -> var0.putUInt32(var1, var2));
      b(UaIds.Int64, Long.class, (var0, var1, var2, var3) -> var0.putInt64(var1, var2));
      b(UaIds.UInt64, UnsignedLong.class, (var0, var1, var2, var3) -> var0.putUInt64(var1, var2));
      b(UaIds.Float, Float.class, (var0, var1, var2, var3) -> var0.putFloat(var1, var2));
      b(UaIds.Double, Double.class, (var0, var1, var2, var3) -> var0.putDouble(var1, var2));
      b(UaIds.String, String.class, (var0, var1, var2, var3) -> var0.putString(var1, var2));
      b(UaIds.Guid, UUID.class, (var0, var1, var2, var3) -> var0.putGuid(var1, var2));
      b(UaIds.ByteString, ByteString.class, (var0, var1, var2, var3) -> var0.putByteString(var1, var2));
      b(UaIds.XmlElement, XmlElement.class, (var0, var1, var2, var3) -> var0.putXmlElement(var1, var2));
      b(UaIds.NodeId, NodeId.class, (var0, var1, var2, var3) -> var0.putNodeId(var1, var2));
      b(UaIds.ExpandedNodeId, ExpandedNodeId.class, (var0, var1, var2, var3) -> var0.putExpandedNodeId(var1, var2));
      b(UaIds.StatusCode, StatusCode.class, (var0, var1, var2, var3) -> var0.putStatusCode(var1, var2));
      b(UaIds.QualifiedName, QualifiedName.class, (var0, var1, var2, var3) -> var0.putQualifiedName(var1, var2));
      b(UaIds.LocalizedText, LocalizedText.class, (var0, var1, var2, var3) -> var0.putLocalizedText(var1, var2));
      a(UaIds.DateTime, DateTime.class, tS);
      a(UaIds.Structure, ExtensionObject.class, tT);
      a(UaIds.DataValue, DataValue.class, tV);
      a(UaIds.DiagnosticInfo, DiagnosticInfo.class, tX);
      a(UaIds.Decimal, BigDecimal.class, tZ);
      b(null, Object.class, (var0, var1, var2, var3) -> {
         if (var2 == null) {
            tW.put(var0, var1, null, Variant.class);
         } else if (var2 instanceof Variant) {
            tW.put(var0, var1, (Variant)var2, Variant.class);
         } else {
            tW.put(var0, var1, new Variant(var2), Variant.class);
         }
      });
   }

   private interface a<T> {
      void put(JsonEncoder var1, String var2, T var3, Class<? extends T> var4) throws EncodingException;
   }
}
