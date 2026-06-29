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
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.encoding.EncodeType;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.EncoderMode;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.IEncoder;
import com.prosysopc.ua.stack.utils.BijectionMap;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.stack.utils.XMLFactoryCache;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils.ArrayIterator;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.SimpleTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import java.io.EOFException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlEncoder implements IEncoder {
   public static final String OPC_UA_TYPES_NAMESPACE = "http://opcfoundation.org/UA/2008/02/Types.xsd";
   private static final Logger logger = LoggerFactory.getLogger(XmlEncoder.class);
   private static final String tO = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
   private static final String tQ = "0001-01-01T00:00:00Z";
   private static final String tR = "9999-12-31T23:59:59Z";
   private static final Map<Class<?>, XmlEncoder.a<?>> sU = new HashMap<>();
   private static final Map<UaNodeId, XmlEncoder.a<?>> sV = new HashMap<>();
   private static final BijectionMap<UaNodeId, Class<?>> sW = new BijectionMap();
   private static final XmlEncoder.a<DateTime> us = (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.e(var2)));
   private static final XmlEncoder.a<ExtensionObject> ut = (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.b(var2)));
   private static final XmlEncoder.a<Structure> uu = (var0, var1, var2, var3) -> {
      StructureSpecification var4 = var0.getEncoderContext().getStructureSpecification(var3);
      if (var4 == null) {
         throw new EncodingException("Cannot find StructureSpecification for DataType: " + var3);
      } else {
         var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2, var4)));
      }
   };
   private static final XmlEncoder.a<DataValue> uv = (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.j(var2)));
   private static final XmlEncoder.a<Variant> uw = (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.c(var2)));
   private static final XmlEncoder.a<DiagnosticInfo> ux = (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.b(var2)));
   private static final XmlEncoder.a<Enumeration> uy = (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2)));
   private static final XmlEncoder.a<BigDecimal> uz = (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.b(var2)));
   private static final XmlEncoder.a<UaOptionSet> uA = (var0, var1, var2, var3) -> var0.a(var1, var2);
   private static final ExpandedNodeId tg = new ExpandedNodeId("http://opcfoundation.org/UA/", Identifiers.Decimal.getValue());
   private final SimpleDateFormat uB = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
   private EncoderContext ti;
   EncoderMode tu = EncoderMode.NonStrict;
   private boolean uC = true;
   private int uD = 4;
   private String encoding = "UTF-8";
   private boolean uE = true;
   private final Document uF;
   private Element uG = null;
   private Element uH = null;
   private int uI = 0;
   private int uJ = 100;
   private String uK = null;

   private static <T> void a(UaNodeId var0, Class<T> var1, XmlEncoder.a<T> var2) throws Error {
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

   private static <T> void b(UaNodeId var0, Class<T> var1, XmlEncoder.a<T> var2) {
      if (!Object.class.equals(var1) && !Modifier.isFinal(var1.getModifiers())) {
         throw new Error("Class " + var1 + " is not final, and cannot be put to known final classes serialization helper");
      } else if (sU.put(var1, var2) != null) {
         throw new Error("Class " + var1 + " already has a serializer defined");
      } else {
         a(var0, var1, var2);
      }
   }

   private static <T> XmlEncoder.a<T> k(Class<?> var0) throws EncodingException {
      if (var0 == null) {
         throw new EncodingException("Cannot encode class null");
      } else {
         XmlEncoder.a var1 = sU.get(var0);
         if (var1 != null) {
            return var1;
         } else if (ExtensionObject.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)ut;
         } else if (Structure.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)uu;
         } else if (DataValue.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)uv;
         } else if (Variant.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)uw;
         } else if (DiagnosticInfo.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)ux;
         } else if (Enumeration.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)uy;
         } else if (DateTime.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)us;
         } else if (BigDecimal.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)uz;
         } else if (UaOptionSet.class.isAssignableFrom(var0)) {
            return (XmlEncoder.a<T>)uA;
         } else {
            throw new EncodingException("Cannot encode class: " + var0);
         }
      }
   }

   private static XmlEncoder.a<Object> f(UaNodeId var0, EncoderContext var1, AtomicReference<Class<?>> var2) throws EncodingException {
      if (var0 == null) {
         throw new IllegalArgumentException("The given DataTypeId cannot be null");
      } else if (UaIds.BaseDataType.equals(var0)) {
         var2.set(Object.class);
         return k(Object.class);
      } else if (UaIds.Structure.equals(var0)) {
         var2.set(ExtensionObject.class);
         return k(ExtensionObject.class);
      } else {
         XmlEncoder.a var3 = sV.get(var0);
         if (var3 == null) {
            UaDataTypeSpecification var4 = var1.getDataTypeSpecification(var0);
            if (var4 == null) {
               logger.warn("Cannot find UaDataTypeSpecification for DataTypeId: {}", var0);
            } else if (var4 instanceof SimpleTypeSpecification) {
               var3 = sV.get(((SimpleTypeSpecification)var4).getBaseTypeId());
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof StructureSpecification) {
               var3 = uu;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof OptionSetSpecification) {
               var3 = uA;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof EnumerationSpecification) {
               var3 = uy;
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

   public XmlEncoder() throws EncodingException {
      this.uB.setTimeZone(TimeZone.getTimeZone("UTC"));

      try {
         this.uF = XMLFactoryCache.getDocumentBuilderFactory().newDocumentBuilder().newDocument();
      } catch (ParserConfigurationException var2) {
         throw new EncodingException("Unable to create DocumentBuilder", var2);
      }
   }

   public String getDefaultNamespace() {
      return this.uK;
   }

   @Override
   public EncoderContext getEncoderContext() {
      return this.ti;
   }

   public EncoderMode getEncoderType() {
      return this.tu;
   }

   public String getEncoding() {
      return this.encoding;
   }

   public int getIndentAmount() {
      return this.uD;
   }

   @Override
   public List<Locale> getLocales() {
      return new ArrayList<>();
   }

   public int getMaxDiagnosticInfoNestingLevel() {
      return this.uJ;
   }

   public boolean isIndent() {
      return this.uC;
   }

   public boolean isOmitXmlDeclaration() {
      return this.uE;
   }

   @Override
   public void put(String var1, Object var2, UaNodeId var3, int var4) throws EncodingException {
      try {
         if (var4 < 0) {
            throw new EncodingException("The given dimensions cannot be negative");
         } else {
            this.a(var1, var2, null, var3, var4);
         }
      } catch (Exception var6) {
         throw new EncodingException("Error while trying to encode, DataTypeId: " + var3, var6);
      }
   }

   public void setDefaultNamespace(String var1) {
      this.uK = var1;
   }

   public void setEncoderContext(EncoderContext var1) {
      this.ti = var1;
   }

   public void setEncoderMode(EncoderMode var1) {
      this.tu = var1;
   }

   public void setEncoding(String var1) {
      this.encoding = var1;
   }

   public void setIndent(boolean var1) {
      this.uC = var1;
   }

   public void setIndentAmount(int var1) {
      this.uD = var1;
   }

   public void setMaxDiagnosticInfoNestingLevel(int var1) {
      this.uJ = var1;
   }

   public void setOmitXmlDeclaration(boolean var1) {
      this.uE = var1;
   }

   public void write(Writer var1) throws EncodingException {
      if (this.uG != null) {
         if (this.uK != null) {
            this.uG.setAttribute("xmlns", this.uK);
         }

         this.uF.appendChild(this.uG);
      }

      try {
         Transformer var2 = XMLFactoryCache.getTransformerFactory().newTransformer();
         var2.setOutputProperty("indent", this.isIndent() ? "yes" : "no");
         var2.setOutputProperty("method", "xml");
         var2.setOutputProperty("encoding", this.getEncoding());
         var2.setOutputProperty("omit-xml-declaration", this.isOmitXmlDeclaration() ? "yes" : "no");
         int var3 = this.getIndentAmount() > 0 ? this.getIndentAmount() : 0;
         var2.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(var3));
         var2.transform(new DOMSource(this.uF), new StreamResult(var1));
      } catch (TransformerException var4) {
         a(var4, "write", null);
      }
   }

   private void p(int var1) throws EncodingException {
      int var2 = this.ti.getMaxArrayLength();
      if (var2 > 0 && var1 > var2) {
         EncodingException var3 = new EncodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxArrayLength " + var2 + " < " + var1);
         logger.warn("assertArrayLength: failed", var3);
         throw var3;
      }
   }

   private void endElement() {
      if (this.uH != this.uG) {
         this.uH = (Element)this.uH.getParentNode();
      }
   }

   private boolean i(Object var1) {
      if (var1 != null) {
         return false;
      } else {
         this.putNull();
         return true;
      }
   }

   private boolean a(Object var1, XmlEncoder.b var2) throws EncodingException {
      if (var1 != null) {
         return false;
      } else {
         if (this.tu == EncoderMode.Strict) {
            var2.serialize();
         } else {
            this.putNull();
         }

         return true;
      }
   }

   private void a(String var1, Object var2, Class<?> var3, UaNodeId var4, int var5) throws EncodingException {
      if (var5 < 0) {
         throw new EncodingException("The given dimensions cannot be negative");
      } else {
         XmlEncoder.a var6 = null;
         UaNodeId var7 = var4;
         if (var4 == null && var3 != null) {
            var7 = (UaNodeId)sW.getLeft(MultiDimensionArrayUtils.getComponentType(var3));
         }

         Class var8;
         if (var7 != null) {
            AtomicReference var9 = new AtomicReference();
            var6 = f(var7, this.getEncoderContext(), var9);
            var8 = (Class)var9.get();
         } else {
            var8 = MultiDimensionArrayUtils.getComponentType(var3);
            var6 = k(var8);
         }

         if (var5 == 0) {
            var6.put(this, var1, var2, var7);
         } else if (var5 == 1) {
            this.a(var1, var2, var8, var7, var6);
         } else {
            this.as(var1);
            Object[] var13 = null;
            int[] var10;
            if (var2 == null) {
               var10 = new int[var5];

               for (int var11 = 0; var11 < var10.length; var11++) {
                  var10[var11] = -1;
               }
            } else {
               var10 = MultiDimensionArrayUtils.getArrayLengths(var2);
               var13 = (Object[])MultiDimensionArrayUtils.muxArray(var2, var10, var8);
               int var14 = var13.length;
               this.p(var14);
            }

            this.a("Dimensions", var10);
            this.a("Elements", var13, var8, var7, 1);
            this.endElement();
         }
      }
   }

   private void a(Boolean var1) throws EncodingException {
      if (!this.a(var1, () -> this.a(false))) {
         this.ar(var1.toString());
      }
   }

   private void a(UnsignedByte var1) throws EncodingException {
      if (!this.a(var1, () -> this.a(UnsignedByte.ZERO))) {
         this.ar(var1.toString());
      }
   }

   private void b(byte[] var1) {
      if (!this.i(var1)) {
         String var2 = CryptoUtil.base64Encode(var1);
         this.ar(var2);
      }
   }

   private void j(DataValue var1) throws EncodingException {
      if (!this.a(
         var1, () -> this.j(new DataValue(Variant.NULL, StatusCode.GOOD, DateTime.MIN_VALUE, UnsignedShort.ZERO, DateTime.MIN_VALUE, UnsignedShort.ZERO))
      )) {
         Variant var2 = var1.getValue();
         if (var2 != null) {
            this.putVariant("Value", var2);
         }

         StatusCode var3 = var1.getStatusCode();
         if (var3 != null) {
            this.putStatusCode("StatusCode", var3);
         }

         DateTime var4 = var1.getSourceTimestamp();
         if (var4 != null) {
            this.putDateTime("SourceTimestamp", var4);
         }

         UnsignedShort var5 = var1.getSourcePicoseconds();
         if (var5 != null) {
            this.putUInt16("SourcePicoseconds", var5);
         }

         DateTime var6 = var1.getServerTimestamp();
         if (var6 != null) {
            this.putDateTime("ServerTimestamp", var6);
         }

         UnsignedShort var7 = var1.getServerPicoseconds();
         if (var7 != null) {
            this.putUInt16("ServerPicoseconds", var7);
         }
      }
   }

   private void e(DateTime var1) throws EncodingException {
      if (!this.a(var1, () -> this.e(DateTime.MIN_VALUE))) {
         String var2;
         if (var1.compareTo(DateTime.MIN_VALUE) <= 0) {
            var2 = "0001-01-01T00:00:00Z";
         } else if (var1.compareTo(DateTime.MAX_VALUE) >= 0) {
            var2 = "9999-12-31T23:59:59Z";
         } else {
            try {
               var2 = this.uB.format(var1.getTimeInMillis());
            } catch (Exception var4) {
               var2 = "0001-01-01T00:00:00Z";
            }
         }

         this.ar(var2);
      }
   }

   private void b(BigDecimal var1) throws EncodingException {
      if (!this.i(var1)) {
         this.putNodeId("TypeId", Identifiers.Decimal);
         this.as("Body");
         UnsignedShort var2 = UnsignedShort.valueOf(var1.scale());
         this.putUInt16("Scale", var2);
         this.putString("Value", var1.unscaledValue().toString());
         this.endElement();
      }
   }

   private void b(DiagnosticInfo var1) throws EncodingException {
      if (!this.a(var1, () -> this.b(new DiagnosticInfo()))) {
         Integer var2 = var1.getSymbolicId();
         if (var2 != null) {
            this.putInt32("SymbolicId", var2);
         }

         Integer var3 = var1.getNamespaceUri();
         if (var3 != null) {
            this.putInt32("NamespaceUri", var3);
         }

         Integer var4 = var1.getLocale();
         if (var4 != null) {
            this.putInt32("Locale", var4);
         }

         Integer var5 = var1.getLocalizedText();
         if (var5 != null) {
            this.putInt32("LocalizedText", var5);
         }

         String var6 = var1.getAdditionalInfo();
         if (var6 != null) {
            this.putString("AdditionalInfo", var6);
         }

         StatusCode var7 = var1.getInnerStatusCode();
         if (var7 != null) {
            this.putStatusCode("InnerStatusCode", var7);
         }

         DiagnosticInfo var8 = var1.getInnerDiagnosticInfo();
         if (var8 != null) {
            if (this.uI >= this.uJ) {
               throw new EncodingException("Max DiagnosticInfo nesting level exceeded");
            }

            this.uI++;
            this.putDiagnosticInfo("InnerDiagnosticInfo", var8);
            this.uI--;
         }
      }
   }

   private void c(Double var1) throws EncodingException {
      if (!this.a(var1, () -> this.b(0))) {
         String var2;
         if (var1.isInfinite()) {
            var2 = var1 > 0.0 ? "INF" : "-INF";
         } else if (var1.isNaN()) {
            var2 = "NaN";
         } else {
            var2 = var1.toString();
         }

         this.ar(var2);
      }
   }

   private void a(Enumeration var1) throws EncodingException {
      if (!this.i(var1)) {
         String var2 = null;
         if (var1.specification() != null) {
            var2 = (String)var1.specification().getIntToStringMappings().get(var1.getValue());
         } else {
            logger.warn("Encountered Enumeration for which .specification returned null, using number form for encoding as a fallback");
         }

         if (var2 == null) {
            var2 = Integer.toString(var1.getValue());
         } else {
            var2 = var2 + "_" + var1.getValue();
         }

         this.ar(var2);
      }
   }

   private void b(ExpandedNodeId var1) throws EncodingException {
      if (!this.a(var1, () -> this.putString("Identifier", null))) {
         this.putString("Identifier", var1.toString());
      }
   }

   private void b(ExtensionObject var1) throws EncodingException {
      if (!this.a(var1, () -> {})) {
         if (!var1.isEncoded()) {
            this.b(ExtensionObject.binaryEncode((Structure)var1.getObject(), this.ti));
         } else {
            NodeId var2;
            try {
               var2 = this.ti.getNamespaceTable().toNodeId(var1.getTypeId());
            } catch (ServiceResultException var6) {
               throw new EncodingException("Could not get namespace index for given id");
            }

            this.putNodeId("TypeId", var2);
            this.as("Body");
            Object var3 = var1.getObject();
            if (var3 == null) {
               this.i(null);
            } else if (var1.getEncodeType() == EncodeType.Binary) {
               this.putByteString("ByteString", (ByteString)var3);
            } else if (var1.getEncodeType() == EncodeType.Json) {
               String var4 = (String)var3;
               ByteString var5 = ByteString.valueOf(var4.getBytes(StandardCharsets.UTF_8));
               this.putByteString("ByteString", var5);
            } else {
               if (var1.getEncodeType() != EncodeType.Xml) {
                  throw new EncodingException("Unexpected object " + var1.getEncodeType());
               }

               XmlElement var7 = (XmlElement)var3;
               this.a(var7);
            }

            this.endElement();
         }
      }
   }

   private void a(Float var1) throws EncodingException {
      if (!this.a(var1, () -> this.b(0))) {
         String var2;
         if (var1.isInfinite()) {
            var2 = var1 > 0.0F ? "INF" : "-INF";
         } else if (var1.isNaN()) {
            var2 = "NaN";
         } else {
            var2 = var1.toString();
         }

         this.ar(var2);
      }
   }

   private void a(UUID var1) throws EncodingException {
      if (!this.a(var1, () -> this.a(UUID.fromString("00000000-0000-0000-0000-000000000000")))) {
         this.putString("String", var1.toString());
      }
   }

   private void a(Short var1) throws EncodingException {
      if (!this.a(var1, () -> this.a((short)0))) {
         this.ar(var1.toString());
      }
   }

   private void b(Integer var1) throws EncodingException {
      if (!this.a(var1, () -> this.b(0))) {
         this.ar(var1.toString());
      }
   }

   private void a(String var1, int[] var2) throws EncodingException {
      if (var2 == null) {
         this.a(var1, null, Integer[].class, UaIds.Int32, 1);
      } else {
         Integer[] var3 = new Integer[var2.length];

         for (int var4 = 0; var4 < var2.length; var4++) {
            var3[var4] = var2[var4];
         }

         this.a(var1, var3, Integer[].class, UaIds.Int32, 1);
      }
   }

   private void b(Long var1) throws EncodingException {
      if (!this.a(var1, () -> this.b(0L))) {
         this.ar(var1.toString());
      }
   }

   private void a(LocalizedText var1) throws EncodingException {
      if (!this.a(var1, () -> this.a(new LocalizedText(null, (String)null)))) {
         var1 = var1.asSingleLocale(this.getLocales());
         String var2 = var1.getLocaleId();
         String var3 = var1.getText();
         if (var2 != null && !var2.isEmpty()) {
            this.putString("Locale", var2);
         }

         if (var3 != null && !var3.isEmpty()) {
            this.putString("Text", var3);
         }
      }
   }

   private void n(NodeId var1) throws EncodingException {
      if (!this.a(var1, () -> this.putString("Identifier", null))) {
         this.putString("Identifier", var1.toString());
      }
   }

   private void a(String var1, UaOptionSet var2) throws EncodingException {
      if (var2 == null) {
         this.a(var1, (XmlEncoder.b)(() -> this.b(0)));
      } else {
         this.put(var1, var2.getValue(), var2.specification().getBaseTypeId(), 0);
      }
   }

   private void d(QualifiedName var1) throws EncodingException {
      if (!this.a(var1, () -> this.d(new QualifiedName(0, null)))) {
         Integer var2 = var1.getNamespaceIndex();
         this.putInt32("NamespaceIndex", var2);
         this.putString("Name", var1.getName());
      }
   }

   private void a(Byte var1) throws EncodingException {
      if (!this.a(var1, () -> this.a((byte)0))) {
         this.ar(var1.toString());
      }
   }

   private void a(StatusCode var1) throws EncodingException {
      if (!this.a(var1, () -> this.a(StatusCode.GOOD))) {
         this.putUInt32("Code", var1.getValue());
      }
   }

   private void H(String var1) throws EncodingException {
      if (!this.a((Object)var1, (XmlEncoder.b)(() -> {}))) {
         this.ar(var1);
      }
   }

   private void a(Structure var1, StructureSpecification var2) throws EncodingException {
      EncoderMode var3 = this.tu;
      this.tu = EncoderMode.Strict;

      try {
         this.b(var1, var2);
      } catch (Exception var8) {
         throw new EncodingException("Could not encode Structure", var8);
      } finally {
         this.tu = var3;
      }
   }

   private void a(UnsignedShort var1) throws EncodingException {
      if (!this.a(var1, () -> this.a(UnsignedShort.ZERO))) {
         this.ar(var1.toString());
      }
   }

   private void j(UnsignedInteger var1) throws EncodingException {
      if (!this.a(var1, () -> this.j(UnsignedInteger.ZERO))) {
         this.ar(var1.toString());
      }
   }

   private void a(UnsignedLong var1) throws EncodingException {
      if (!this.a(var1, () -> this.a(UnsignedLong.ZERO))) {
         this.ar(var1.toString());
      }
   }

   private void c(Variant var1) throws EncodingException {
      if (!this.a(var1, () -> this.c(Variant.NULL))) {
         this.as("Value");
         this.d(var1);
         this.endElement();
      }
   }

   private void a(XmlElement var1) {
      if (!this.i(var1)) {
         Node var2 = var1.getNode();
         if (var2 instanceof Document) {
            var2 = var2.getFirstChild();
         }

         Node var3 = this.uF.importNode(var2, true);
         this.uH.appendChild(var3);
      }
   }

   private void a(Object var1, Class<?> var2, UaNodeId var3, XmlEncoder.a<Object> var4) throws EncodingException {
      Object[] var5 = (Object[])var1;
      int var6 = var5.length;
      this.p(var6);
      if (var6 != 0) {
         String var7;
         if (var3 == null || UaIds.Structure.equals(var3)) {
            var7 = var2.getSimpleName();
         } else if (UaIds.BaseDataType.equals(var3)) {
            var7 = "Variant";
         } else {
            var7 = this.ti.getDataTypeSpecification(var3).getName();
         }

         for (Object var11 : var5) {
            UaNodeId var12 = var3;
            if (var11 instanceof Structure) {
               var12 = ((Structure)var11).specification().getTypeId();
            }

            var4.put(this, var7, var11, var12);
         }
      }
   }

   private void a(String var1, Object var2, Class<?> var3, UaNodeId var4, XmlEncoder.a<Object> var5) throws EncodingException {
      if (var2 != null) {
         this.as(var1);
         this.a(var2, var3, var4, var5);
         this.endElement();
      }
   }

   private void a(String var1, XmlEncoder.b var2) throws EncodingException {
      if (var1 == null) {
         var2.serialize();
      } else {
         this.as(var1);
         var2.serialize();
         this.endElement();
      }
   }

   private void putNull() {
      this.uH.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      this.uH.setAttribute("xsi:nil", "true");
   }

   private void b(Structure var1, StructureSpecification var2) throws Exception {
      if (var1 == null) {
         var1 = var2.toInstanceBuilder().build();
      }

      Map var3 = var1.toFieldsMap(var2);
      if (StructureType.UNION != var2.getStructureType() && StructureType.UNION_SUBTYPES != var2.getStructureType()) {
         if (StructureType.OPTIONAL == var2.getStructureType()) {
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
            if ((!var13.isOptional() || var15 != null) && (var15 != null || !var13.getDataTypeId().equals(UaIds.BaseDataType))) {
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

         if (var6 == null) {
            this.putUInt32("SwitchField", UnsignedInteger.ZERO);
         } else {
            this.putUInt32("SwitchField", UnsignedInteger.valueOf(var4));
            int var16 = var6.getValueRank() < 0 ? 0 : var6.getValueRank();
            this.put(var6.getName(), var7, var6.getDataTypeId(), var16);
         }
      }
   }

   private void ar(String var1) {
      this.uH.appendChild(this.uF.createTextNode(var1));
   }

   private void d(Variant var1) throws EncodingException {
      Object var2 = var1.getValue();
      if (!this.i(var2)) {
         Class var3 = var1.getCompositeClass();
         boolean var4;
         if (BigDecimal.class.isAssignableFrom(var3)) {
            var4 = true;
            var3 = ExtensionObject.class;
         } else {
            var4 = false;
         }

         UaNodeId var5 = (UaNodeId)sW.getLeft(var3);
         String var6;
         if (var5 == null || UaIds.Structure.equals(var5)) {
            var6 = var3.getSimpleName();
         } else if (UaIds.BaseDataType.equals(var5)) {
            var6 = "Variant";
         } else {
            var6 = this.ti.getDataTypeSpecification(var5).getName();
         }

         if (!var1.isArray()) {
            if (var4) {
               var2 = this.a((BigDecimal)var2);
            }

            this.a(var6, var2, var3, var5, 0);
         } else {
            int var7 = var1.getDimension();
            if (var7 == 1) {
               if (var4) {
                  var2 = this.a((BigDecimal[])var2);
               }

               this.a("ListOf" + var6, var2, var3, var5, 1);
            } else {
               if (var4) {
                  int[] var8 = var1.getArrayDimensions();
                  int var9 = MultiDimensionArrayUtils.getLength(var8);
                  ArrayIterator var10 = MultiDimensionArrayUtils.arrayIterator(var1.getValue(), var8);
                  ExtensionObject[] var11 = new ExtensionObject[var9];

                  for (int var12 = 0; var12 < var9; var12++) {
                     var11[var12] = this.a((BigDecimal)var10.next());
                  }

                  var2 = var11;
               }

               this.a("Matrix", var2, var3, var5, var7);
            }
         }
      }
   }

   private Element as(String var1) {
      Element var2 = this.uF.createElement(var1);
      if (this.uG == null) {
         this.uG = var2;
      } else if (this.uH != null) {
         this.uH.appendChild(var2);
      }

      this.uH = var2;
      return var2;
   }

   ExtensionObject[] a(BigDecimal[] var1) throws EncodingException {
      if (var1 == null) {
         return new ExtensionObject[0];
      } else {
         ExtensionObject[] var2 = new ExtensionObject[var1.length];

         for (int var3 = 0; var3 < var1.length; var3++) {
            var2[var3] = this.a(var1[var3]);
         }

         return var2;
      }
   }

   ExtensionObject a(BigDecimal var1) throws EncodingException {
      XmlEncoder var2 = new XmlEncoder();
      var2.setEncoderContext(this.ti);
      var2.b(var1);
      Element var3 = this.uF.createElement("Decimal");
      NodeList var4 = var2.uG.getLastChild().getChildNodes();
      var3.appendChild(this.uF.importNode(var4.item(0), true));
      var3.appendChild(this.uF.importNode(var4.item(1), true));
      return new ExtensionObject(tg, new XmlElement(var3));
   }

   @Deprecated
   void put(String var1, Object var2) throws EncodingException {
      if (var2 == null) {
         throw new EncodingException("the put(fieldname, obj) cannot be used for null values");
      } else {
         Class var3 = MultiDimensionArrayUtils.getComponentType(var2.getClass());
         int var4 = MultiDimensionArrayUtils.getDimension(var2);
         UaNodeId var5;
         if (Enumeration.class.isAssignableFrom(var3)) {
            if (!(var2 instanceof Enumeration)) {
               throw new EncodingException("the put(fieldname, obj) cannot be used for arrays of Enumerations");
            }

            var5 = ((Enumeration)var2).specification().getTypeId();
         } else if (Structure.class.isAssignableFrom(var3)) {
            if (!(var2 instanceof Structure)) {
               throw new EncodingException("the put(fieldname, obj) cannot be used for arrays of Structures");
            }

            var5 = ((Structure)var2).specification().getTypeId();
         } else if (UaOptionSet.class.isAssignableFrom(var3)) {
            if (!(var2 instanceof UaOptionSet)) {
               throw new EncodingException("the put(fieldname, obj) cannot be used for arrays of UaOptionSet");
            }

            var5 = ((UaOptionSet)var2).specification().getTypeId();
         } else if (Variant.class.isAssignableFrom(var3)) {
            var5 = UaIds.BaseDataType;
         } else {
            var5 = (UaNodeId)sW.getLeft(var3);
         }

         if (var5 == null) {
            throw new EncodingException("Unsupported class: " + var2.getClass());
         } else {
            this.a(var1, var2, var2.getClass(), var5, var4);
         }
      }
   }

   static {
      b(UaIds.Boolean, Boolean.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.SByte, Byte.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.Byte, UnsignedByte.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.Int16, Short.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.UInt16, UnsignedShort.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.Int32, Integer.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.b(var2))));
      b(UaIds.UInt32, UnsignedInteger.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.j(var2))));
      b(UaIds.Int64, Long.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.b(var2))));
      b(UaIds.UInt64, UnsignedLong.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.Float, Float.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.Double, Double.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.c(var2))));
      b(UaIds.String, String.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.H(var2))));
      b(UaIds.Guid, UUID.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.ByteString, ByteString.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.b(ByteString.asByteArray(var2)))));
      b(UaIds.XmlElement, XmlElement.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.NodeId, NodeId.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.n(var2))));
      b(UaIds.ExpandedNodeId, ExpandedNodeId.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.b(var2))));
      b(UaIds.StatusCode, StatusCode.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      b(UaIds.QualifiedName, QualifiedName.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.d(var2))));
      b(UaIds.LocalizedText, LocalizedText.class, (var0, var1, var2, var3) -> var0.a(var1, (XmlEncoder.b)(() -> var0.a(var2))));
      a(UaIds.DateTime, DateTime.class, us);
      a(UaIds.Structure, ExtensionObject.class, ut);
      a(UaIds.DataValue, DataValue.class, uv);
      a(UaIds.DiagnosticInfo, DiagnosticInfo.class, ux);
      a(UaIds.Decimal, BigDecimal.class, uz);
      b(null, Object.class, (var0, var1, var2, var3) -> {
         if (var2 == null) {
            uw.put(var0, var1, null, UaIds.BaseDataType);
         } else if (var2 instanceof Variant) {
            uw.put(var0, var1, (Variant)var2, UaIds.BaseDataType);
         } else {
            uw.put(var0, var1, new Variant(var2), UaIds.BaseDataType);
         }
      });
   }

   private interface a<T> {
      void put(XmlEncoder var1, String var2, T var3, UaNodeId var4) throws EncodingException;
   }

   private interface b {
      void serialize() throws EncodingException;
   }
}
