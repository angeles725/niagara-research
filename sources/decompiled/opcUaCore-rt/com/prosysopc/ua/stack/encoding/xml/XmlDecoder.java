package com.prosysopc.ua.stack.encoding.xml;

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
import com.prosysopc.ua.stack.common.NamespaceTable;
import com.prosysopc.ua.stack.common.ServerTable;
import com.prosysopc.ua.stack.common.UriTable;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.encoding.DecodingException;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.IDecoder;
import com.prosysopc.ua.stack.utils.BijectionMap;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.stack.utils.XMLFactoryCache;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.SimpleTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlDecoder implements IDecoder {
   private static final String XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
   private static final String EMPTY_STRING = "";
   private static final String ug = "http://opcfoundation.org/UA/2008/02/Types.xsd";
   private static Logger logger = LoggerFactory.getLogger(XmlDecoder.class);
   private static final Map<Class<?>, XmlDecoder.a<?>> sU = new HashMap<>();
   private static final Map<UaNodeId, XmlDecoder.a<?>> sV = new HashMap<>();
   private static final BijectionMap<UaNodeId, Class<?>> sW = new BijectionMap();
   private static final XmlDecoder.a<DateTime> uh = (var0, var1, var2, var3) -> var0.T(var1);
   private static final XmlDecoder.a<ExtensionObject> ui = (var0, var1, var2, var3) -> var0.X(var1);
   private static final XmlDecoder.a<Structure> uj = (var0, var1, var2, var3) -> {
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
   private static final XmlDecoder.a<DataValue> uk = (var0, var1, var2, var3) -> var0.S(var1);
   private static final XmlDecoder.a<Variant> ul = (var0, var1, var2, var3) -> var0.am(var1);
   private static final XmlDecoder.a<DiagnosticInfo> um = (var0, var1, var2, var3) -> var0.U(var1);
   private static final XmlDecoder.a<Enumeration> un = (var0, var1, var2, var3) -> var0.a(var1, var2, var3);
   private static final XmlDecoder.a<BigDecimal> uo = (var0, var1, var2, var3) -> var0.G(var1);
   private static final XmlDecoder.a<UaOptionSet> up = (var0, var1, var2, var3) -> var0.a(var1, var3);
   private static final ExpandedNodeId tg = new ExpandedNodeId("http://opcfoundation.org/UA/", Identifiers.Decimal.getValue());
   private XMLStreamReader reader;
   private NamespaceTable iz;
   private ServerTable sN;
   private EncoderContext kg;
   private UnsignedShort[] uq;
   private UnsignedShort[] ur;

   private static <T> void a(UaNodeId var0, Class<T> var1, XmlDecoder.a<T> var2) throws Error {
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

   private static <T> void b(UaNodeId var0, Class<T> var1, XmlDecoder.a<T> var2) {
      if (!Object.class.equals(var1) && !Modifier.isFinal(var1.getModifiers())) {
         throw new Error("Class " + var1 + " is not final, and cannot be put to known final classes serialization helper");
      } else if (sU.put(var1, var2) != null) {
         throw new Error("Class " + var1 + " already has a serializer defined");
      } else {
         a(var0, var1, var2);
      }
   }

   private static <T> XmlDecoder.a<T> j(Class<?> var0) throws DecodingException {
      if (var0 == null) {
         throw new DecodingException("Cannot decode class null");
      } else {
         XmlDecoder.a var1 = sU.get(var0);
         if (var1 != null) {
            return var1;
         } else if (ExtensionObject.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)ui;
         } else if (Structure.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)uj;
         } else if (DataValue.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)uk;
         } else if (Variant.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)ul;
         } else if (DiagnosticInfo.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)um;
         } else if (Enumeration.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)un;
         } else if (DateTime.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)uh;
         } else if (BigDecimal.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)uo;
         } else if (UaOptionSet.class.isAssignableFrom(var0)) {
            return (XmlDecoder.a<T>)up;
         } else {
            throw new DecodingException("Cannot decode class: " + var0);
         }
      }
   }

   private static XmlDecoder.a<Object> e(UaNodeId var0, EncoderContext var1, AtomicReference<Class<?>> var2) throws DecodingException {
      if (var0 == null) {
         throw new IllegalArgumentException("The given DataTypeId cannot be null");
      } else if (UaIds.BaseDataType.equals(var0)) {
         var2.set(Object.class);
         return j(Object.class);
      } else if (UaIds.Structure.equals(var0)) {
         var2.set(ExtensionObject.class);
         return j(ExtensionObject.class);
      } else {
         XmlDecoder.a var3 = sV.get(var0);
         if (var3 == null) {
            UaDataTypeSpecification var4 = var1.getDataTypeSpecification(var0);
            if (var4 == null) {
               logger.warn("Cannot find UaDataTypeSpecification for DataTypeId: {}", var0);
            } else if (var4 instanceof SimpleTypeSpecification) {
               var3 = sV.get(((SimpleTypeSpecification)var4).getBaseTypeId());
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof StructureSpecification) {
               var3 = uj;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof OptionSetSpecification) {
               var3 = up;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof EnumerationSpecification) {
               var3 = un;
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

   public XmlDecoder(XmlElement var1, EncoderContext var2) throws DecodingException {
      if (var2 == null) {
         throw new NullPointerException("context");
      } else {
         this.initialize();

         try {
            this.reader = XMLFactoryCache.getXMLInputFactory().createXMLStreamReader(new StringReader(var1.toString()));
         } catch (XMLStreamException var4) {
            throw new DecodingException((Exception)var4);
         }

         this.kg = var2;
      }
   }

   public XmlDecoder(XMLStreamReader var1, EncoderContext var2) throws DecodingException {
      if (var2 == null) {
         throw new NullPointerException("context");
      } else {
         this.initialize();
         this.reader = var1;
         this.kg = var2;
      }
   }

   public void close() throws DecodingException {
      try {
         this.reader.close();
      } catch (XMLStreamException var2) {
         throw new DecodingException((Exception)var2);
      }
   }

   public void close(boolean var1) throws DecodingException {
      if (var1 && this.reader.getEventType() != 8) {
         this.getEndElement();
      }

      try {
         this.reader.close();
      } catch (XMLStreamException var3) {
         throw new DecodingException((Exception)var3);
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

         try {
            if (var3 < 0) {
               throw new DecodingException("The given dimensions cannot be negative");
            } else {
               return this.a(var1, (Class<T>)var4, var2, var3);
            }
         } catch (Exception var6) {
            throw new DecodingException("Error while trying to decode, DataTypeId: " + var2, var6);
         }
      }
   }

   @Override
   public Boolean[] getBooleanArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Boolean")) {
            var2.add(this.getBoolean("Boolean"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Boolean[0]);
   }

   @Override
   public UnsignedByte[] getByteArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Byte")) {
            var2.add(this.getByte("Byte"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new UnsignedByte[0]);
   }

   @Override
   public ByteString[] getByteStringArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("ByteString")) {
            var2.add(this.getByteString("ByteString"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new ByteString[0]);
   }

   @Override
   public DataValue[] getDataValueArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("DataValue")) {
            var2.add(this.getDataValue("DataValue"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new DataValue[0]);
   }

   @Override
   public DateTime[] getDateTimeArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("DateTime")) {
            var2.add(this.getDateTime("DateTime"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new DateTime[0]);
   }

   public DiagnosticInfo getDiagnosticInfo() throws DecodingException {
      DiagnosticInfo var1 = new DiagnosticInfo();
      if (this.c("SymbolicId", true)) {
         var1.setSymbolicId(this.getInt32(null));
         this.ap("SymbolicId");
      }

      if (this.c("NamespaceUri", true)) {
         var1.setNamespaceUri(this.getInt32(null));
         this.ap("NamespaceUri");
      }

      if (this.c("Locale", true)) {
         var1.setLocale(this.getInt32(null));
         this.ap("Locale");
      }

      if (this.c("LocalizedText", true)) {
         var1.setLocalizedText(this.getInt32(null));
         this.ap("LocalizedText");
      }

      var1.setAdditionalInfo(this.getString("AdditionalInfo"));
      var1.setInnerStatusCode(this.getStatusCode("InnerStatusCode"));
      if (this.c("InnerDiagnosticInfo", true)) {
         var1.setInnerDiagnosticInfo(this.getDiagnosticInfo());
         this.ap("InnerDiagnosticInfo");
      }

      return var1;
   }

   @Override
   public DiagnosticInfo[] getDiagnosticInfoArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("DiagnosticInfo")) {
            var2.add(this.getDiagnosticInfo("DiagnosticInfo"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new DiagnosticInfo[0]);
   }

   @Override
   public Double[] getDoubleArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Double")) {
            var2.add(this.getDouble("Double"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Double[0]);
   }

   @Override
   public EncoderContext getEncoderContext() {
      return this.kg;
   }

   public void getEndElement() throws DecodingException {
      this.esL();
      if (this.reader.isEndElement()) {
         try {
            this.reader.next();
         } catch (XMLStreamException var2) {
            throw new DecodingException((Exception)var2);
         }
      } else {
         throw new DecodingException("Not an end element");
      }
   }

   @Override
   public ExpandedNodeId[] getExpandedNodeIdArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("ExpandedNodeId")) {
            var2.add(this.getExpandedNodeId("ExpandedNodeId"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new ExpandedNodeId[0]);
   }

   @Override
   public ExtensionObject[] getExtensionObjectArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("ExtensionObject")) {
            var2.add(this.getExtensionObject("ExtensionObject"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new ExtensionObject[0]);
   }

   public Object getExtensionObjectBody(ExpandedNodeId var1) throws DecodingException {
      this.esL();
      return this.reader.getLocalName() == "ByteString" && this.reader.getNamespaceURI() == "http://opcfoundation.org/UA/2008/02/Types.xsd"
         ? this.getByteString("ByteString")
         : this.getXmlElement("");
   }

   @Override
   public Float[] getFloatArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Float")) {
            var2.add(this.getFloat("Float"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Float[0]);
   }

   @Override
   public UUID[] getGuidArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Guid")) {
            var2.add(this.getGuid("Guid"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new UUID[0]);
   }

   @Override
   public Short[] getInt16Array(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Int16")) {
            var2.add(this.getInt16("Int16"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Short[0]);
   }

   @Override
   public Integer[] getInt32Array(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Int32")) {
            var2.add(this.getInt32("Int32"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Integer[0]);
   }

   @Override
   public int[] getInt32Array_(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (!this.c(var1, true)) {
         return new int[0];
      } else {
         while (this.aq("Int32")) {
            var2.add(this.getInt32("Int32"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         } else {
            this.ap(var1);
            int[] var3 = new int[var2.size()];

            for (int var4 = 0; var4 < var2.size(); var4++) {
               var3[var4] = (Integer)var2.get(var4);
            }

            return var3;
         }
      }
   }

   @Override
   public Long[] getInt64Array(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Int64")) {
            var2.add(this.getInt64("Int64"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Long[0]);
   }

   @Override
   public LocalizedText[] getLocalizedTextArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("LocalizedText")) {
            var2.add(this.getLocalizedText("LocalizedText"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new LocalizedText[0]);
   }

   public Object getMatrix(String var1) throws DecodingException {
      if (!this.c(var1, true)) {
         return null;
      } else {
         ArrayList var2 = new ArrayList();
         int[] var3 = this.getInt32Array_("Dimensions");
         if (this.c("Elements", true)) {
            this.esM();

            while (this.reader.getEventType() != 2) {
               Object var4 = null;
               String var5 = this.reader.getLocalName();
               if (var5.equals("Boolean")) {
                  var4 = this.getBoolean(var5);
               } else if (var5.equals("SByte")) {
                  var4 = this.getSByte(var5);
               } else if (var5.equals("Byte")) {
                  var4 = this.getByte(var5);
               } else if (var5.equals("Int16")) {
                  var4 = this.getInt16(var5);
               } else if (var5.equals("UInt16")) {
                  var4 = this.getUInt16(var5);
               } else if (var5.equals("Int32")) {
                  var4 = this.getInt32(var5);
               } else if (var5.equals("UInt32")) {
                  var4 = this.getUInt32(var5);
               } else if (var5.equals("Int64")) {
                  var4 = this.getInt64(var5);
               } else if (var5.equals("UInt64")) {
                  var4 = this.getUInt64(var5);
               } else if (var5.equals("Float")) {
                  var4 = this.getFloat(var5);
               } else if (var5.equals("Double")) {
                  var4 = this.getDouble(var5);
               } else if (var5.equals("String")) {
                  var4 = this.getString(var5);
               } else if (var5.equals("DateTime")) {
                  var4 = this.getDateTime(var5);
               } else if (var5.equals("Guid")) {
                  var4 = this.getGuid(var5);
               } else if (var5.equals("ByteString")) {
                  var4 = this.getByteString(var5);
               } else if (var5.equals("XmlElement")) {
                  var4 = this.getXmlElement(var5);
               } else if (var5.equals("NodeId")) {
                  var4 = this.getNodeId(var5);
               } else if (var5.equals("ExpandedNodeId")) {
                  var4 = this.getExpandedNodeId(var5);
               } else if (var5.equals("StatusCode")) {
                  var4 = this.getStatusCode(var5);
               } else if (var5.equals("DiagnosticInfo")) {
                  var4 = this.getDiagnosticInfo(var5);
               } else if (var5.equals("QualifiedName")) {
                  var4 = this.getQualifiedName(var5);
               } else if (var5.equals("LocalizedText")) {
                  var4 = this.getLocalizedText(var5);
               } else if (var5.equals("ExtensionObject")) {
                  ExtensionObject var6 = this.getExtensionObject(var5);

                  try {
                     var4 = this.c(var6);
                  } catch (DecodingException var8) {
                     var4 = var6;
                  }
               } else if (var5.equals("DataValue")) {
                  var4 = this.getDataValue(var5);
               } else if (var5.equals("Variant")) {
                  var4 = this.getVariant(var5);
               }

               var2.add(var4);
               this.esM();
            }

            this.ap("Elements");
         }

         this.ap(var1);
         if (MultiDimensionArrayUtils.getLength(var3) != var2.size()) {
            throw new DecodingException(StatusCodes.Bad_DecodingError);
         } else {
            return MultiDimensionArrayUtils.demuxArray(var2.toArray(), var3, var2.get(0).getClass());
         }
      }
   }

   public NamespaceTable getNamespaceTable() {
      return this.iz;
   }

   @Override
   public NodeId[] getNodeIdArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("NodeId")) {
            var2.add(this.getNodeId("NodeId"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new NodeId[0]);
   }

   @Override
   public QualifiedName[] getQualifiedNameArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("QualifiedName")) {
            var2.add(this.getQualifiedName("QualifiedName"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new QualifiedName[0]);
   }

   @Override
   public Byte[] getSByteArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("SByte")) {
            var2.add(this.getSByte("SByte"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Byte[0]);
   }

   public ServerTable getServerTable() {
      return this.sN;
   }

   public void getStartElement() throws DecodingException {
      if (this.reader.isStartElement()) {
         try {
            this.reader.next();
         } catch (XMLStreamException var2) {
            throw new DecodingException((Exception)var2);
         }
      }
   }

   @Override
   public StatusCode[] getStatusCodeArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("StatusCode")) {
            var2.add(this.getStatusCode("StatusCode"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new StatusCode[0]);
   }

   @Override
   public String[] getStringArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("String")) {
            var2.add(this.getString("String"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new String[0]);
   }

   @Override
   public UnsignedShort[] getUInt16Array(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("UInt16")) {
            var2.add(this.getUInt16("UInt16"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new UnsignedShort[0]);
   }

   @Override
   public UnsignedInteger[] getUInt32Array(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("UInt32")) {
            var2.add(this.getUInt32("UInt32"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new UnsignedInteger[0]);
   }

   @Override
   public UnsignedLong[] getUInt64Array(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("UInt64")) {
            var2.add(this.getUInt64("UInt64"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new UnsignedLong[0]);
   }

   @Override
   public Variant[] getVariantArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("Variant")) {
            var2.add(this.getVariant("Variant"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new Variant[0]);
   }

   public Object getVariantContents() throws DecodingException {
      while (this.reader.getEventType() != 1) {
         try {
            this.reader.next();
         } catch (XMLStreamException var5) {
            throw new DecodingException((Exception)var5);
         }
      }

      String var1 = this.reader.getLocalName();
      if (var1.startsWith("ListOf")) {
         String var2 = var1.substring("ListOf".length());
         if (var2.equals("Boolean")) {
            return this.getBooleanArray(var1);
         }

         if (var2.equals("SByte")) {
            return this.getSByteArray(var1);
         }

         if (var2.equals("Byte")) {
            return this.getByteArray(var1);
         }

         if (var2.equals("Int16")) {
            return this.getInt16Array(var1);
         }

         if (var2.equals("UInt16")) {
            return this.getUInt16Array(var1);
         }

         if (var2.equals("Int32")) {
            return this.getInt32Array(var1);
         }

         if (var2.equals("UInt32")) {
            return this.getUInt32Array(var1);
         }

         if (var2.equals("Int64")) {
            return this.getInt64Array(var1);
         }

         if (var2.equals("UInt64")) {
            return this.getUInt64Array(var1);
         }

         if (var2.equals("Float")) {
            return this.getFloatArray(var1);
         }

         if (var2.equals("Double")) {
            return this.getDoubleArray(var1);
         }

         if (var2.equals("String")) {
            return this.getStringArray(var1);
         }

         if (var2.equals("DateTime")) {
            return this.getDateTimeArray(var1);
         }

         if (var2.equals("Guid")) {
            return this.getGuidArray(var1);
         }

         if (var2.equals("ByteString")) {
            return this.getByteStringArray(var1);
         }

         if (var2.equals("XmlElement")) {
            return this.getXmlElementArray(var1);
         }

         if (var2.equals("NodeId")) {
            return this.getNodeIdArray(var1);
         }

         if (var2.equals("ExpandedNodeId")) {
            return this.getExpandedNodeIdArray(var1);
         }

         if (var2.equals("StatusCode")) {
            return this.getStatusCodeArray(var1);
         }

         if (var2.equals("DiagnosticInfo")) {
            return this.getDiagnosticInfoArray(var1);
         }

         if (var2.equals("QualifiedName")) {
            return this.getQualifiedNameArray(var1);
         }

         if (var2.equals("LocalizedText")) {
            return this.getLocalizedTextArray(var1);
         }

         if (var2.equals("ExtensionObject")) {
            ExtensionObject[] var3 = this.getExtensionObjectArray(var1);

            try {
               return this.decode(var3);
            } catch (DecodingException var6) {
               return var3;
            }
         }

         if (var2.equals("DataValue")) {
            return this.getDataValueArray(var1);
         }

         if (var2.equals("Variant")) {
            return this.getVariantArray(var1);
         }
      } else {
         if (var1.equals("Null")) {
            if (this.c(var1, true)) {
               this.ap(var1);
            }

            return null;
         }

         if (var1.equals("Boolean")) {
            return this.getBoolean(var1);
         }

         if (var1.equals("SByte")) {
            return this.getSByte(var1);
         }

         if (var1.equals("Byte")) {
            return this.getByte(var1);
         }

         if (var1.equals("Int16")) {
            return this.getInt16(var1);
         }

         if (var1.equals("UInt16")) {
            return this.getUInt16(var1);
         }

         if (var1.equals("Int32")) {
            return this.getInt32(var1);
         }

         if (var1.equals("UInt32")) {
            return this.getUInt32(var1);
         }

         if (var1.equals("Int64")) {
            return this.getInt64(var1);
         }

         if (var1.equals("UInt64")) {
            return this.getUInt64(var1);
         }

         if (var1.equals("Float")) {
            return this.getFloat(var1);
         }

         if (var1.equals("Double")) {
            return this.getDouble(var1);
         }

         if (var1.equals("String")) {
            return this.getString(var1);
         }

         if (var1.equals("DateTime")) {
            return this.getDateTime(var1);
         }

         if (var1.equals("Guid")) {
            return this.getGuid(var1);
         }

         if (var1.equals("ByteString")) {
            return this.getByteString(var1);
         }

         if (var1.equals("XmlElement")) {
            return this.getXmlElement(var1);
         }

         if (var1.equals("NodeId")) {
            return this.getNodeId(var1);
         }

         if (var1.equals("ExpandedNodeId")) {
            return this.getExpandedNodeId(var1);
         }

         if (var1.equals("StatusCode")) {
            return this.getStatusCode(var1);
         }

         if (var1.equals("DiagnosticInfo")) {
            return this.getDiagnosticInfo(var1);
         }

         if (var1.equals("QualifiedName")) {
            return this.getQualifiedName(var1);
         }

         if (var1.equals("LocalizedText")) {
            return this.getLocalizedText(var1);
         }

         if (var1.equals("ExtensionObject")) {
            ExtensionObject var8 = this.getExtensionObject(var1);

            try {
               return this.c(var8);
            } catch (DecodingException var7) {
               return var8;
            }
         }

         if (var1.equals("DataValue")) {
            return this.getDataValue(var1);
         }

         if (var1.equals("Matrix")) {
            return this.getMatrix(var1);
         }
      }

      throw new DecodingException(
         StatusCodes.Bad_DecodingError, "Element '" + this.reader.getNamespaceURI() + ":" + this.reader.getLocalName() + "' is not allowed in an Variant."
      );
   }

   @Override
   public XmlElement[] getXmlElementArray(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("XmlElement")) {
            var2.add(this.getXmlElement("XmlElement"));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new XmlElement[0]);
   }

   public boolean loadStringTable(String var1, String var2, List<String> var3) throws DecodingException {
      if (!this.peek(var1)) {
         return false;
      } else {
         this.getStartElement();

         while (this.peek(var2)) {
            String var4 = this.getString(var2);
            var3.add(var4);
         }

         this.a(new QName(var1, "http://opcfoundation.org/UA/2008/02/Types.xsd"));
         return true;
      }
   }

   public QName peek(int var1) {
      this.esJ();
      return var1 != this.reader.getEventType() ? null : new QName(this.reader.getLocalName(), this.reader.getNamespaceURI());
   }

   public boolean peek(String var1) {
      this.esJ();
      if (1 != this.reader.getEventType()) {
         return false;
      } else {
         String var2 = this.reader.getLocalName();
         return var1.equals(var2);
      }
   }

   public void setEncoderContext(EncoderContext var1) {
      this.kg = var1;
   }

   public void setNamespaceTable(NamespaceTable var1) {
      this.iz = var1;
      this.uq = null;
      if (var1 != null && this.kg.getNamespaceTable() != null) {
         this.uq = this.a(var1, this.kg.getNamespaceTable(), false);
      }
   }

   public void setServerTable(ServerTable var1) {
      this.sN = var1;
      this.ur = null;
      if (var1 != null && this.kg.getServerTable() != null) {
         this.ur = this.a(var1, this.kg.getServerTable(), false);
      }
   }

   private boolean b(String var1, boolean var2) throws DecodingException {
      if (this.isNullOrEmpty(var1)) {
         return true;
      } else {
         this.esL();
         if (!this.ao(var1)) {
            if (!var2) {
               throw new DecodingException(
                  StatusCodes.Bad_DecodingError,
                  String.format(
                     Locale.ROOT,
                     "Encountered element: '{1}:{0}' when expecting element: '{2}'.",
                     this.reader.getLocalName(),
                     this.reader.getNamespaceURI(),
                     var1
                  )
               );
            } else {
               return false;
            }
         } else {
            if (this.reader.getAttributeCount() != 0) {
               String var3 = this.reader.getAttributeValue("nil", "http://www.w3.org/2001/XMLSchema-instance");
               if (!this.isNullOrEmpty(var3) && Boolean.parseBoolean(var3)) {
                  return false;
               }
            }

            this.getStartElement();
            this.esJ();
            if (this.reader.getEventType() == 2 && this.reader.getLocalName() == var1) {
               this.getEndElement();
               return false;
            } else {
               return true;
            }
         }
      }
   }

   private UnsignedShort[] a(UriTable var1, UriTable var2, boolean var3) {
      if (var1 == null) {
         return null;
      } else {
         UnsignedShort[] var4 = new UnsignedShort[var1.size()];

         for (int var5 = 0; var5 < var1.size(); var5++) {
            String var6 = var1.getUri(var5);
            int var7 = var2.getIndex(var6);
            if (var7 < 0) {
               if (!var3) {
                  var4[var5] = UnsignedShort.MAX_VALUE;
                  continue;
               }

               var7 = var2.add(-1, var6);
            }

            var4[var5] = UnsignedShort.valueOf(var7);
         }

         return var4;
      }
   }

   private Object c(ExtensionObject var1) throws DecodingException {
      return this.a(var1) ? this.c((XmlElement)var1.getObject()) : var1.decode(this.getEncoderContext(), this.iz);
   }

   private Object decode(ExtensionObject[] var1) throws DecodingException {
      Object var2 = this.b(var1);
      return var2 instanceof ExtensionObject[] ? this.getEncoderContext().decode(var1, this.iz) : var2;
   }

   private String esI() throws DecodingException {
      if (4 != this.reader.getEventType()) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder();

         while (true) {
            String var2 = this.reader.getText();
            if (var2 != null) {
               var1.append(var2);
            }

            try {
               if (4 != this.reader.next()) {
                  break;
               }
            } catch (XMLStreamException var4) {
               break;
            }
         }

         return var1.toString();
      }
   }

   private BigDecimal G(String var1) throws DecodingException {
      ExtensionObject var2 = this.getExtensionObject(var1);
      XmlElement var3 = (XmlElement)var2.getObject();
      return this.c(var3);
   }

   private BigDecimal[] N(String var1) throws DecodingException {
      ArrayList var2 = new ArrayList();
      if (this.c(var1, true)) {
         while (this.aq("ExtensionObject")) {
            ExtensionObject var3 = this.getExtensionObject("ExtensionObject");
            var2.add(this.c((XmlElement)var3.getObject()));
         }

         if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var2.size()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         this.ap(var1);
      }

      return var2.toArray(new BigDecimal[0]);
   }

   private String O(String var1) throws DecodingException {
      String var2 = "";
      boolean var3 = true;
      int var4 = 0;

      try {
         do {
            int var5 = this.reader.getEventType();
            switch (var5) {
               case 1:
                  if (this.reader.getLocalName().equals(var1)) {
                     var3 = true;
                  }

                  if (var3) {
                     var2 = var2 + "<" + this.reader.getLocalName() + ">";
                  }

                  var4++;
                  break;
               case 2:
                  if (--var4 < 0) {
                     return var2;
                  }

                  if (this.reader.getLocalName().equals(var1)) {
                     var2 = var2 + "</" + this.reader.getLocalName() + ">";
                     var3 = false;
                  }

                  if (var3 && !this.reader.getLocalName().equals(var1)) {
                     var2 = var2 + "</" + this.reader.getLocalName() + ">";
                  }
               case 3:
               default:
                  break;
               case 4:
                  if (var3) {
                     var2 = var2 + this.reader.getText();
                  }
            }

            this.reader.next();
         } while (this.reader.hasNext());

         return var2;
      } catch (XMLStreamException var7) {
         throw new DecodingException((Exception)var7);
      }
   }

   private UaOptionSet a(String var1, UaNodeId var2) throws DecodingException {
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
   }

   private String getString() throws DecodingException {
      String var1 = this.esI();
      if (var1 != null && this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var1.length()) {
         throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
      } else {
         return var1;
      }
   }

   private <T> T a(String var1, Class<T> var2, UaNodeId var3, int var4) throws DecodingException {
      if (var4 < 0) {
         throw new DecodingException("The given dimensions cannot be negative");
      } else {
         Object var5 = null;
         UaNodeId var6 = var3;
         if (var3 == null && var2 != null) {
            var6 = (UaNodeId)sW.getLeft(MultiDimensionArrayUtils.getComponentType(var2));
         }

         Class var7;
         if (var6 != null) {
            AtomicReference var8 = new AtomicReference();
            var5 = e(var6, this.getEncoderContext(), var8);
            var7 = (Class)var8.get();
         } else {
            var7 = MultiDimensionArrayUtils.getComponentType(var2);
            var5 = j(var7);
         }

         if (var4 == 0) {
            return (T)((XmlDecoder.a)var5).get(this, var1, var2, var3);
         } else if (var4 == 1) {
            ArrayList var16 = new ArrayList();
            XmlDecoder.a var17 = (XmlDecoder.a)var5;
            String var19;
            if (var6 != null) {
               var19 = this.kg.getDataTypeSpecification(var6).getName();
            } else {
               var19 = var7.getSimpleName();
            }

            if (BigDecimal.class.equals(var7) || UaIds.Decimal.equals(var6)) {
               var19 = "ExtensionObject";
            }

            this.esL();
            if (!this.peek(var1)) {
               return null;
            } else {
               if (this.c(var1, true)) {
                  while (this.aq(var19)) {
                     var16.add(var17.get(this, var19, var7, var3));
                  }

                  if (this.kg.getMaxArrayLength() > 0 && this.kg.getMaxArrayLength() < var16.size()) {
                     throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
                  }

                  this.ap(var1);
               }

               int var20 = var16.size();
               Object[] var21 = (Object[])Array.newInstance(var7, var20);

               for (int var22 = 0; var22 < var20; var22++) {
                  var21[var22] = var16.get(var22);
               }

               return (T)var21;
            }
         } else if (this.c(var1, true)) {
            int[] var15 = this.getInt32Array_("Dimensions");
            if (var15 == null) {
               return null;
            } else {
               int var9 = 1;

               for (int var13 : var15) {
                  if (var13 < 0) {
                     return null;
                  }

                  var9 *= var13;
               }

               Object[] var18 = this.a("Elements", MultiDimensionArrayUtils.arrayClassOf(var7, 1), var3, 1);
               if (var18.length != var9) {
                  throw new DecodingException("The number of elements in array does not match dimensions");
               } else {
                  this.ap(var1);
                  return (T)MultiDimensionArrayUtils.demuxArray(var18, var15, var7);
               }
            }
         } else {
            return null;
         }
      }
   }

   private Boolean P(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            boolean var3 = Boolean.parseBoolean(var2.toLowerCase(Locale.ROOT));
            this.ap(var1);
            return var3;
         }
      }

      return false;
   }

   private UnsignedByte Q(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            UnsignedByte var3 = UnsignedByte.parseUnsignedByte(var2);
            this.ap(var1);
            return var3;
         }
      }

      return UnsignedByte.ZERO;
   }

   private ByteString R(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         Object var2 = null;
         String var3 = this.esI();
         if (!this.isNullOrEmpty(var3)) {
            var2 = CryptoUtil.base64Decode(var3);
         } else {
            var2 = new byte[0];
         }

         if (this.kg.getMaxByteStringLength() > 0 && this.kg.getMaxByteStringLength() < ((Object[])var2).length) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         } else {
            this.ap(var1);
            return ByteString.valueOf((byte[])var2);
         }
      } else {
         return null;
      }
   }

   private DataValue S(String var1) throws DecodingException {
      DataValue var2 = new DataValue();
      if (this.c(var1, true)) {
         var2.setValue(this.getVariant("Value"));
         var2.setStatusCode(this.getStatusCode("StatusCode"));
         var2.setSourceTimestamp(this.getDateTime("SourceTimestamp"));
         var2.setSourcePicoseconds(this.getUInt16("SourcePicoseconds"));
         var2.setServerTimestamp(this.getDateTime("ServerTimestamp"));
         var2.setServerPicoseconds(this.getUInt16("ServerPicoseconds"));
         this.ap(var1);
      }

      return var2;
   }

   private DateTime T(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (this.kg.getMaxStringLength() > 0 && this.kg.getMaxStringLength() < var2.length()) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded);
         }

         if (!this.isNullOrEmpty(var2)) {
            DateTime var3;
            try {
               var3 = DateTime.parseDateTime(var2);
            } catch (ParseException var5) {
               throw new DecodingException((Exception)var5);
            }

            this.ap(var1);
            return var3;
         }
      }

      return DateTime.MIN_VALUE;
   }

   private DiagnosticInfo U(String var1) throws DecodingException {
      DiagnosticInfo var2 = null;
      if (this.c(var1, true)) {
         var2 = this.getDiagnosticInfo();
         this.ap(var1);
         return var2;
      } else {
         return var2;
      }
   }

   private Double V(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            double var3 = 0.0;
            if (var2.length() == 3) {
               if (var2 == "NaN") {
                  var3 = Double.NaN;
               }

               if (var2 == "INF") {
                  var3 = Double.POSITIVE_INFINITY;
               }
            }

            if (var2.length() == 4 && var2 == "-INF") {
               var3 = Double.NEGATIVE_INFINITY;
            }

            if (var3 == 0.0) {
               var3 = Double.parseDouble(var2);
            }

            this.ap(var1);
            return var3;
         }
      }

      return 0.0;
   }

   private <T extends Enumeration> T a(String var1, Class<T> var2, UaNodeId var3) throws DecodingException {
      Enumeration var4 = null;
      if (var2.getEnumConstants() != null && ((Enumeration[])var2.getEnumConstants()).length > 0) {
         var4 = ((Enumeration[])var2.getEnumConstants())[0];
      }

      if (this.c(var1, true)) {
         String var5 = this.getString();
         if (!this.isNullOrEmpty(var5)) {
            int var6 = var5.lastIndexOf(95);
            if (var6 != -1) {
               int var7 = Integer.parseInt(var5.substring(var6 + 1));

               try {
                  EnumerationSpecification var9 = this.getEncoderContext().getEnumerationSpecification(var3);
                  if (var9 != null) {
                     var4 = var9.getByValue(var7);
                  } else {
                     Method var8 = var2.getMethod("valueOf", int.class);
                     var4 = (Enumeration)var8.invoke(null, var7);
                  }
               } catch (SecurityException var15) {
                  throw new DecodingException((Exception)var15);
               } catch (NoSuchMethodException var16) {
                  throw new DecodingException((Exception)var16);
               } catch (IllegalArgumentException var17) {
                  throw new DecodingException((Exception)var17);
               } catch (IllegalAccessException var18) {
                  throw new DecodingException((Exception)var18);
               } catch (InvocationTargetException var19) {
                  throw new DecodingException((Exception)var19);
               }
            } else {
               int var20 = Integer.parseInt(var5);

               try {
                  EnumerationSpecification var22 = this.getEncoderContext().getEnumerationSpecification(var3);
                  if (var22 != null) {
                     var4 = var22.getByValue(var20);
                  } else {
                     Method var21 = var2.getMethod("valueOf", int.class);
                     var4 = (Enumeration)var21.invoke(null, var20);
                  }
               } catch (SecurityException var10) {
                  throw new DecodingException((Exception)var10);
               } catch (NoSuchMethodException var11) {
                  throw new DecodingException((Exception)var11);
               } catch (IllegalArgumentException var12) {
                  throw new DecodingException((Exception)var12);
               } catch (IllegalAccessException var13) {
                  throw new DecodingException((Exception)var13);
               } catch (InvocationTargetException var14) {
                  throw new DecodingException((Exception)var14);
               }
            }
         }

         this.ap(var1);
      }

      return (T)var4;
   }

   private ExpandedNodeId W(String var1) throws DecodingException {
      ExpandedNodeId var2 = ExpandedNodeId.NULL;
      if (this.c(var1, true)) {
         var2 = ExpandedNodeId.parseExpandedNodeId(this.getString("Identifier"));
         this.ap(var1);
      }

      int var3 = var2.getNamespaceIndex();
      int var4 = var2.getServerIndex().intValue();
      boolean var5 = false;
      if (this.uq != null && this.uq.length > var2.getNamespaceIndex()) {
         var3 = this.uq[var2.getNamespaceIndex()].intValue();
         var5 = true;
      }

      if (this.ur != null && this.ur.length > var2.getServerIndex().intValue()) {
         var4 = this.ur[var2.getServerIndex().intValue()].intValue();
         var5 = true;
      }

      if (var5) {
         var2 = new ExpandedNodeId(UnsignedInteger.valueOf(var4), var3, var2.getValue());
      }

      return var2;
   }

   private ExtensionObject X(String var1) throws IllegalArgumentException, DecodingException {
      if (!this.c(var1, true)) {
         return null;
      } else {
         NodeId var2 = this.getNodeId("TypeId");
         ExpandedNodeId var3 = this.kg.getNamespaceTable().toExpandedNodeId(var2);
         if (!NodeId.isNull(var2) && ExpandedNodeId.isNull(var3)) {
            logger.error("Cannot de-serialized extension objects if the NamespaceUri is not in the NamespaceTable: Type = {}", var2);
         }

         if (!this.c("Body", true)) {
            this.ap(var1);
            return new ExtensionObject(var3, new XmlElement(""));
         } else {
            Object var4 = this.getExtensionObjectBody(var3);
            this.ap("Body");
            this.ap(var1);
            ExtensionObject var5;
            if (var4 instanceof XmlElement) {
               var5 = new ExtensionObject(var3, (XmlElement)var4);
            } else {
               var5 = new ExtensionObject(var3, (ByteString)var4);
            }

            try {
               if (this.iz == null) {
                  Structure var8 = var5.decode(this.getEncoderContext());
                  return new ExtensionObject(var8);
               } else {
                  Structure var6 = var5.decode(this.getEncoderContext(), this.iz);
                  return new ExtensionObject(var6);
               }
            } catch (DecodingException var7) {
               return var5;
            }
         }
      }
   }

   private Float Y(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            float var3 = 0.0F;
            if (var2.length() == 3) {
               if (var2 == "NaN") {
                  var3 = Float.NaN;
               }

               if (var2 == "INF") {
                  var3 = Float.POSITIVE_INFINITY;
               }
            }

            if (var2.length() == 4 && var2 == "-INF") {
               var3 = Float.NEGATIVE_INFINITY;
            }

            if (var3 == 0.0F) {
               var3 = Float.parseFloat(var2);
            }

            this.ap(var1);
            return var3;
         }
      }

      return 0.0F;
   }

   private UUID Z(String var1) throws DecodingException {
      String var2 = null;
      if (this.c(var1, true)) {
         var2 = this.getString("String");
         this.ap(var1);
      }

      return var2 == null ? null : UUID.fromString(var2);
   }

   private Short aa(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            short var3 = Short.parseShort(var2);
            this.ap(var1);
            return var3;
         }
      }

      return (short)0;
   }

   private Integer ab(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            int var3 = Integer.parseInt(var2);
            this.ap(var1);
            return var3;
         }
      }

      return 0;
   }

   private Long ac(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            long var3 = Long.parseLong(var2);
            this.ap(var1);
            return var3;
         }
      }

      return 0L;
   }

   private LocalizedText ad(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         boolean var2 = false;
         String var3 = null;
         String var4 = null;
         if (this.c("Locale", true)) {
            var4 = this.getString(null);
            this.ap("Locale");
         } else if (!var2) {
            var4 = "";
         }

         if (this.c("Text", true)) {
            var3 = this.getString(null);
            this.ap("Text");
         } else if (!var2) {
            var3 = "";
         }

         LocalizedText var5 = LocalizedText.builder().setText(var3, var4).build();
         this.ap(var1);
         return var5;
      } else {
         return LocalizedText.EMPTY;
      }
   }

   private NodeId ae(String var1) throws IllegalArgumentException, DecodingException {
      NodeId var2 = null;
      if (this.c(var1, true)) {
         var2 = NodeId.parseNodeId(this.getString("Identifier"));
         this.ap(var1);
      }

      if (this.uq != null && this.uq.length > var2.getNamespaceIndex()) {
         var2 = NodeId.get(var2.getIdType(), this.uq[var2.getNamespaceIndex()].intValue(), var2.getValue());
      }

      return var2;
   }

   private QualifiedName af(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         UnsignedShort var2 = UnsignedShort.ZERO;
         if (this.c("NamespaceIndex", true)) {
            var2 = this.getUInt16(null);
            this.ap("NamespaceIndex");
         }

         String var3 = null;
         if (this.c("Name", true)) {
            var3 = this.getString(null);
            this.ap("Name");
         }

         this.ap(var1);
         if (this.uq != null && this.uq.length > var2.getValue()) {
            var2 = this.uq[var2.getValue()];
         }

         return new QualifiedName(var2, var3);
      } else {
         return QualifiedName.NULL;
      }
   }

   private Byte ag(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            byte var3 = Byte.parseByte(var2);
            this.ap(var1);
            return var3;
         }
      }

      return (byte)0;
   }

   private StatusCode ah(String var1) throws DecodingException {
      StatusCode var2 = StatusCode.getFromBits(0);
      if (this.c(var1, true)) {
         var2 = new StatusCode(this.getUInt32("Code"));
         this.ap(var1);
      }

      return var2;
   }

   private String ai(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (var2 != null) {
            var2 = var2.trim();
         }

         this.ap(var1);
         return var2;
      } else {
         return null;
      }
   }

   private Structure a(String var1, StructureSpecification var2) throws DecodingException {
      UnsignedLong var3 = null;
      Structure.Builder var4 = var2.toInstanceBuilder();
      if (!this.c(var1, true)) {
         return var4.build();
      } else {
         if (StructureType.OPTIONAL == var2.getStructureType()) {
            var3 = this.getUInt64("EncodingMask");
            logger.trace("EncodingMask: {}", var3);
         }

         if (StructureType.UNION != var2.getStructureType() && StructureType.UNION_SUBTYPES != var2.getStructureType()) {
            int var11 = -1;

            for (FieldSpecification var13 : var2.getFields()) {
               logger.trace("Field: {}", var13);
               if (var13.isOptional()) {
                  if ((var3.longValue() & 1 << ++var11) != 0L) {
                     var4.set(var13, this.a(var13));
                  }
               } else {
                  var4.set(var13, this.a(var13));
               }
            }

            this.ap(var1);
            return var4.build();
         } else {
            long var5 = this.getUInt32("SwitchField").longValue();
            logger.trace("SwitchField: {}", var5);
            if (var5 < 0L) {
               throw new DecodingException("Union SwitchField must be >= 0");
            } else if (var5 == 0L) {
               return var4.build();
            } else {
               long var7 = 0L;

               for (FieldSpecification var10 : var2.getFields()) {
                  if (var5 == ++var7) {
                     logger.trace("Decoded Union Field: {}, SwitchValue: {}", var10, var7);
                     var4.set(var10, this.a(var10));
                     return var4.build();
                  }
               }

               throw new DecodingException("Union SwitchField overflow: " + ++var7);
            }
         }
      }
   }

   private Object a(FieldSpecification var1) throws DecodingException {
      int var2 = var1.getValueRank() < 0 ? 0 : var1.getValueRank();
      Class var3 = var1.getJavaClass();
      if (var1.isAllowSubTypes()) {
         Class var4 = MultiDimensionArrayUtils.getComponentType(var3);
         return ExtensionObject.class.isAssignableFrom(var4)
            ? this.a(var1.getName(), MultiDimensionArrayUtils.arrayClassOf(ExtensionObject.class, var2), UaIds.Structure, var2)
            : this.a(var1.getName(), MultiDimensionArrayUtils.arrayClassOf(Variant.class, var2), UaIds.BaseDataType, var2);
      } else {
         return this.a(var1.getName(), var3, var1.getDataTypeId(), var2);
      }
   }

   private UnsignedShort aj(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            UnsignedShort var3 = UnsignedShort.parseUnsignedShort(var2);
            this.ap(var1);
            return var3;
         }
      }

      return UnsignedShort.ZERO;
   }

   private UnsignedInteger ak(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            UnsignedInteger var3 = UnsignedInteger.parseUnsignedInteger(var2);
            this.ap(var1);
            return var3;
         }
      }

      return UnsignedInteger.ZERO;
   }

   private UnsignedLong al(String var1) throws DecodingException {
      if (this.c(var1, true)) {
         String var2 = this.getString();
         if (!this.isNullOrEmpty(var2)) {
            UnsignedLong var3 = UnsignedLong.parseUnsignedLong(var2);
            this.ap(var1);
            return var3;
         }
      }

      return UnsignedLong.valueOf(0L);
   }

   private Variant am(String var1) throws DecodingException {
      Variant var2 = new Variant(null);
      if (this.c(var1, true)) {
         if (this.c("Value", true)) {
            Object var3 = this.getVariantContents();
            var2 = new Variant(var3);
            this.ap("Value");
         }

         if (!this.isNullOrEmpty(var1)) {
            this.ap(var1);
         }
      }

      return var2;
   }

   private XmlElement an(String var1) throws DecodingException {
      return this.c(var1, true) ? new XmlElement(this.O("")) : null;
   }

   private void initialize() {
      this.reader = null;
   }

   private boolean a(ExtensionObject var1) {
      return this.getEncoderContext().getNamespaceTable().nodeIdEquals(Identifiers.Decimal, var1.getTypeId());
   }

   private boolean isNullOrEmpty(String var1) {
      return var1 == null ? true : var1.trim().length() == 0;
   }

   private boolean ao(String var1) {
      this.esJ();
      return this.peek(var1);
   }

   private void esJ() {
      while (
         this.reader.getEventType() != 12
            && this.reader.getEventType() != 1
            && this.reader.getEventType() != 2
            && this.reader.getEventType() != 9
            && this.reader.getEventType() != 4
            && this.reader.getEventType() != 8
      ) {
         try {
            this.reader.next();
         } catch (XMLStreamException var2) {
            return;
         }
      }
   }

   private void esK() {
      while (this.reader.getEventType() != 2 && this.reader.getEventType() != 8) {
         try {
            this.reader.next();
         } catch (XMLStreamException var2) {
            return;
         }
      }
   }

   private void esL() throws DecodingException {
      while (this.reader.getEventType() != 1 && this.reader.getEventType() != 2 && this.reader.getEventType() != 9 && this.reader.getEventType() != 8) {
         try {
            this.reader.nextTag();
         } catch (XMLStreamException var2) {
            return;
         }
      }
   }

   private BigDecimal c(XmlElement var1) throws DecodingException {
      XmlDecoder var2 = new XmlDecoder(var1, this.getEncoderContext());
      var2.c("Decimal", false);
      Short var3 = var2.getInt16("Scale");
      String var4 = var2.getString("Value").trim();
      var2.close();
      return new BigDecimal(new BigInteger(var4), var3);
   }

   private void skip() throws XMLStreamException {
      int var1 = 0;
      this.reader.next();
      if (this.reader.getEventType() != 2) {
         var1++;

         while (var1 != 0) {
            this.reader.next();
            if (this.reader.getEventType() != 1) {
               var1++;
            } else if (this.reader.getEventType() != 1) {
               var1--;
            }
         }
      }

      this.reader.next();
   }

   private void a(QName var1) throws DecodingException {
      this.esJ();

      for (int var2 = 1; var2 > 0; this.esJ()) {
         if (this.reader.getEventType() == 2) {
            if (this.reader.getLocalName().equals(var1.getLocalPart()) && this.reader.getNamespaceURI().equals(var1.getNamespaceURI())) {
               var2--;
            }
         } else if (this.reader.getEventType() == 1
            && this.reader.getLocalName().equals(var1.getLocalPart())
            && this.reader.getNamespaceURI().equals(var1.getNamespaceURI())) {
            var2++;
         }

         try {
            this.skip();
         } catch (XMLStreamException var4) {
            throw new DecodingException((Exception)var4);
         }
      }
   }

   private void esM() {
      while (this.reader.getEventType() == 4) {
         try {
            logger.trace("Skipping: <{}>", this.reader.getText());
            if (!this.reader.hasNext()) {
               logger.warn("Reached the END_DOCUMENT while skipping CHARACTERS events.");
               break;
            }

            this.reader.next();
         } catch (XMLStreamException var2) {
            logger.error("Could not skip CHARACTERS events from the stream", var2);
         }
      }
   }

   private Object b(ExtensionObject[] var1) throws DecodingException {
      BigDecimal[] var2 = new BigDecimal[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         ExtensionObject var4 = var1[var3];
         if (!this.a(var4)) {
            return var1;
         }

         var2[var3] = this.c((XmlElement)var4.getObject());
      }

      return var2;
   }

   boolean c(String var1, boolean var2) throws DecodingException {
      return this.b(var1, var2);
   }

   void ap(String var1) throws DecodingException {
      if (!this.isNullOrEmpty(var1)) {
         this.esK();
         int var2 = this.reader.getEventType();
         String var3 = this.reader.getLocalName();
         if (var2 != 2) {
            throw new DecodingException(
               StatusCodes.Bad_DecodingError, "No end element found: '" + var3 + ":" + this.reader.getNamespaceURI() + "' eventType=" + var2
            );
         }

         if (!var3.equals(var1)) {
            throw new DecodingException(
               StatusCodes.Bad_DecodingError,
               "Encountered end element: '" + var3 + ":" + this.reader.getNamespaceURI() + "' when expecting element: '" + var1 + "'."
            );
         }

         this.getEndElement();
      }
   }

   @Deprecated
   <T> T a(String var1, Class<T> var2) throws DecodingException {
      int var3 = MultiDimensionArrayUtils.getClassDimensions(var2);
      return this.a(var1, var2, null, var3);
   }

   boolean aq(String var1) throws DecodingException {
      while (!this.reader.isStartElement()) {
         if (this.reader.getEventType() == 2) {
            return false;
         }

         try {
            this.reader.next();
         } catch (XMLStreamException var3) {
            throw new DecodingException((Exception)var3);
         }
      }

      return this.isNullOrEmpty(var1) ? true : this.reader.getLocalName().equals(var1);
   }

   static {
      b(UaIds.Boolean, Boolean.class, (var0, var1, var2, var3) -> var0.P(var1));
      b(UaIds.SByte, Byte.class, (var0, var1, var2, var3) -> var0.ag(var1));
      b(UaIds.Byte, UnsignedByte.class, (var0, var1, var2, var3) -> var0.Q(var1));
      b(UaIds.Int16, Short.class, (var0, var1, var2, var3) -> var0.aa(var1));
      b(UaIds.UInt16, UnsignedShort.class, (var0, var1, var2, var3) -> var0.aj(var1));
      b(UaIds.Int32, Integer.class, (var0, var1, var2, var3) -> var0.ab(var1));
      b(UaIds.UInt32, UnsignedInteger.class, (var0, var1, var2, var3) -> var0.ak(var1));
      b(UaIds.Int64, Long.class, (var0, var1, var2, var3) -> var0.ac(var1));
      b(UaIds.UInt64, UnsignedLong.class, (var0, var1, var2, var3) -> var0.al(var1));
      b(UaIds.Float, Float.class, (var0, var1, var2, var3) -> var0.Y(var1));
      b(UaIds.Double, Double.class, (var0, var1, var2, var3) -> var0.V(var1));
      b(UaIds.String, String.class, (var0, var1, var2, var3) -> var0.ai(var1));
      b(UaIds.Guid, UUID.class, (var0, var1, var2, var3) -> var0.Z(var1));
      b(UaIds.ByteString, ByteString.class, (var0, var1, var2, var3) -> var0.R(var1));
      b(UaIds.XmlElement, XmlElement.class, (var0, var1, var2, var3) -> var0.an(var1));
      b(UaIds.NodeId, NodeId.class, (var0, var1, var2, var3) -> var0.ae(var1));
      b(UaIds.ExpandedNodeId, ExpandedNodeId.class, (var0, var1, var2, var3) -> var0.W(var1));
      b(UaIds.StatusCode, StatusCode.class, (var0, var1, var2, var3) -> var0.ah(var1));
      b(UaIds.QualifiedName, QualifiedName.class, (var0, var1, var2, var3) -> var0.af(var1));
      b(UaIds.LocalizedText, LocalizedText.class, (var0, var1, var2, var3) -> var0.ad(var1));
      a(UaIds.DateTime, DateTime.class, uh);
      a(UaIds.Structure, ExtensionObject.class, ui);
      a(UaIds.DataValue, DataValue.class, uk);
      a(UaIds.DiagnosticInfo, DiagnosticInfo.class, um);
      a(UaIds.Decimal, BigDecimal.class, uo);
      b(null, Object.class, (var0, var1, var2, var3) -> ul.get(var0, var1, Variant.class, var3).getValue());
   }

   private interface a<T> {
      T get(XmlDecoder var1, String var2, Class<? extends T> var3, UaNodeId var4) throws DecodingException;
   }
}
