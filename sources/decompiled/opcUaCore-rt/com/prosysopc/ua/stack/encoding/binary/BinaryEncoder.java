package com.prosysopc.ua.stack.encoding.binary;

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
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.IdType;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.encoding.EncodeType;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.EncoderMode;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.IEncoder;
import com.prosysopc.ua.stack.utils.BijectionMap;
import com.prosysopc.ua.stack.utils.EncodingLimitsExceededIoException;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils.ArrayIterator;
import com.prosysopc.ua.stack.utils.bytebuffer.ByteBufferWriteable;
import com.prosysopc.ua.stack.utils.bytebuffer.IBinaryWriteable;
import com.prosysopc.ua.stack.utils.bytebuffer.OutputStreamWriteable;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.SimpleTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryEncoder implements IEncoder {
   private static final Logger logger = LoggerFactory.getLogger(BinaryEncoder.class);
   private static final Map<Class<?>, BinaryEncoder.a<?>> sU = new HashMap<>();
   private static final Map<UaNodeId, BinaryEncoder.a<?>> sV = new HashMap<>();
   private static final BijectionMap<UaNodeId, Class<?>> sW = new BijectionMap();
   private static final BinaryEncoder.a<DateTime> tk = (var0, var1, var2, var3) -> var0.e(var2);
   private static final BinaryEncoder.a<ExtensionObject> tl = (var0, var1, var2, var3) -> var0.b(var2);
   private static final BinaryEncoder.a<Structure> tm = (var0, var1, var2, var3) -> {
      StructureSpecification var4 = var0.getEncoderContext().getStructureSpecification(var3);
      if (var4 == null) {
         throw new EncodingException("Cannot find StructureSpecification for DataType: " + var3);
      } else {
         var0.a(var2, var4);
      }
   };
   private static final BinaryEncoder.a<DataValue> tn = (var0, var1, var2, var3) -> var0.j(var2);
   private static final BinaryEncoder.a<Variant> to = (var0, var1, var2, var3) -> var0.c(var2);
   private static final BinaryEncoder.a<DiagnosticInfo> tp = (var0, var1, var2, var3) -> var0.b(var2);
   private static final BinaryEncoder.a<Enumeration> tq = (var0, var1, var2, var3) -> var0.a(var2);
   private static final BinaryEncoder.a<BigDecimal> tr = (var0, var1, var2, var3) -> var0.b(var2);
   private static final BinaryEncoder.a<UaOptionSet> ts = (var0, var1, var2, var3) -> var0.a(var2, var3);
   private static final ExpandedNodeId tg = new ExpandedNodeId("http://opcfoundation.org/UA/", Identifiers.Decimal.getValue());
   private final List<Locale> lW = new CopyOnWriteArrayList<>();
   IBinaryWriteable tt;
   EncoderContext ti;
   EncoderMode tu = EncoderMode.NonStrict;

   private static <T> void a(UaNodeId var0, Class<T> var1, BinaryEncoder.a<T> var2) throws Error {
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

   private static <T> void b(UaNodeId var0, Class<T> var1, BinaryEncoder.a<T> var2) {
      if (!Object.class.equals(var1) && !Modifier.isFinal(var1.getModifiers())) {
         throw new Error("Class " + var1 + " is not final, and cannot be put to known final classes serialization helper");
      } else if (sU.put(var1, var2) != null) {
         throw new Error("Class " + var1 + " already has a serializer defined");
      } else {
         a(var0, var1, var2);
      }
   }

   private static <T> BinaryEncoder.a<T> g(Class<?> var0) throws EncodingException {
      if (var0 == null) {
         throw new EncodingException("Cannot encode class null");
      } else {
         BinaryEncoder.a var1 = sU.get(var0);
         if (var1 != null) {
            return var1;
         } else if (ExtensionObject.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)tl;
         } else if (Structure.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)tm;
         } else if (DataValue.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)tn;
         } else if (Variant.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)to;
         } else if (DiagnosticInfo.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)tp;
         } else if (Enumeration.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)tq;
         } else if (DateTime.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)tk;
         } else if (BigDecimal.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)tr;
         } else if (UaOptionSet.class.isAssignableFrom(var0)) {
            return (BinaryEncoder.a<T>)ts;
         } else {
            throw new EncodingException("Cannot encode class: " + var0);
         }
      }
   }

   private static BinaryEncoder.a<Object> b(UaNodeId var0, EncoderContext var1, AtomicReference<Class<?>> var2) throws EncodingException {
      if (var0 == null) {
         throw new IllegalArgumentException("The given DataTypeId cannot be null");
      } else if (UaIds.BaseDataType.equals(var0)) {
         var2.set(Object.class);
         return g(Object.class);
      } else if (UaIds.Structure.equals(var0)) {
         var2.set(ExtensionObject.class);
         return g(ExtensionObject.class);
      } else {
         BinaryEncoder.a var3 = sV.get(var0);
         if (var3 == null) {
            UaDataTypeSpecification var4 = var1.getDataTypeSpecification(var0);
            if (var4 == null) {
               logger.warn("Cannot find UaDataTypeSpecification for DataTypeId: {}", var0);
            } else if (var4 instanceof SimpleTypeSpecification) {
               var3 = sV.get(((SimpleTypeSpecification)var4).getBaseTypeId());
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof StructureSpecification) {
               var3 = tm;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof OptionSetSpecification) {
               var3 = ts;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof EnumerationSpecification) {
               var3 = tq;
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

   private static UnsignedInteger b(UnsignedInteger var0, int var1) {
      if (var0 == null) {
         throw new IllegalArgumentException("mask cannot be null");
      } else if (var1 >= 0 && var1 <= 31) {
         int var2 = 1 << var1;
         return var0.or(var2);
      } else {
         throw new IllegalArgumentException("position must be between 0-31");
      }
   }

   private static EncodingException b(IOException var0) {
      if (var0 instanceof ClosedChannelException) {
         return new EncodingException(StatusCodes.Bad_ConnectionClosed, var0);
      } else if (var0 instanceof EOFException) {
         return new EncodingException(StatusCodes.Bad_EndOfStream, var0);
      } else if (var0 instanceof ConnectException) {
         return new EncodingException(StatusCodes.Bad_ConnectionRejected, var0);
      } else if (var0 instanceof SocketException) {
         return new EncodingException(StatusCodes.Bad_CommunicationError, var0);
      } else {
         return var0 instanceof EncodingLimitsExceededIoException
            ? new EncodingException(StatusCodes.Bad_EncodingLimitsExceeded, var0, var0.getMessage())
            : new EncodingException(StatusCodes.Bad_UnexpectedError, var0);
      }
   }

   static ExtensionObject[] a(BigDecimal[] var0) throws EncodingException {
      if (var0 == null) {
         return null;
      } else {
         ExtensionObject[] var1 = new ExtensionObject[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = a(var0[var2]);
         }

         return var1;
      }
   }

   static ExtensionObject a(BigDecimal var0) throws EncodingException {
      int var1 = var0.scale();
      if (var1 > 32767) {
         throw new EncodingException("Decimal scale overflow Short max value: " + var1);
      } else if (var1 < -32768) {
         throw new EncodingException("Decimal scale underflow Short min value: " + var1);
      } else {
         short var2 = (short)var1;
         ByteBuffer var3 = ByteBuffer.allocate(2);
         var3.order(ByteOrder.LITTLE_ENDIAN);
         var3.putShort(var2);
         byte[] var4 = var3.array();
         byte[] var5 = com.prosysopc.ua.stack.encoding.binary.a.reverse(var0.unscaledValue().toByteArray());
         byte[] var6 = com.prosysopc.ua.stack.encoding.binary.a.concat(var4, var5);
         return new ExtensionObject(tg, ByteString.valueOf(var6));
      }
   }

   public BinaryEncoder(byte[] var1) {
      ByteBuffer var2 = ByteBuffer.wrap(var1);
      var2.order(ByteOrder.LITTLE_ENDIAN);
      this.setWriteable(new ByteBufferWriteable(var2));
   }

   public BinaryEncoder(byte[] var1, int var2, int var3) {
      ByteBuffer var4 = ByteBuffer.wrap(var1, var2, var3);
      var4.order(ByteOrder.LITTLE_ENDIAN);
      this.setWriteable(new ByteBufferWriteable(var4));
   }

   public BinaryEncoder(ByteBuffer var1) {
      ByteBufferWriteable var2 = new ByteBufferWriteable(var1);
      this.setWriteable(var2);
   }

   public BinaryEncoder(IBinaryWriteable var1) {
      this.setWriteable(var1);
   }

   public BinaryEncoder(OutputStream var1) {
      OutputStreamWriteable var2 = new OutputStreamWriteable(var1);
      var2.order(ByteOrder.LITTLE_ENDIAN);
      this.setWriteable(var2);
   }

   @Override
   public EncoderContext getEncoderContext() {
      return this.ti;
   }

   public EncoderMode getEncoderType() {
      return this.tu;
   }

   @Override
   public List<Locale> getLocales() {
      return this.lW;
   }

   public IBinaryWriteable getOutput() {
      return this.tt;
   }

   public IBinaryWriteable getWriteable() {
      return this.tt;
   }

   @Override
   public void put(String var1, Object var2, UaNodeId var3, int var4) throws EncodingException {
      try {
         if (var4 < 0) {
            throw new EncodingException("The given dimensions cannot be negative");
         } else {
            this.a(var2, null, var3, var4);
         }
      } catch (Exception var6) {
         throw new EncodingException("Error while trying to encode DataTypeId: " + var3, var6);
      }
   }

   public void setEncoderContext(EncoderContext var1) {
      this.ti = var1;
   }

   public void setEncoderMode(EncoderMode var1) {
      this.tu = var1;
   }

   public void setWriteable(IBinaryWriteable var1) {
      if (var1.order() != ByteOrder.LITTLE_ENDIAN) {
         throw new IllegalArgumentException("Writeable must be in Little-Ending byte order");
      } else {
         this.tt = var1;
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

   private void l(int var1) throws EncodingException {
      int var2 = this.ti.getMaxByteStringLength();
      if (var2 > 0 && var1 > var2) {
         EncodingException var3 = new EncodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxByteStringLength " + var2 + " < " + var1);
         logger.warn("assertByteStringLength: failed", var3);
         throw var3;
      }
   }

   private void f(Object var1) throws EncodingException {
      if (var1 == null) {
         if (this.tu == EncoderMode.Strict) {
            throw new EncodingException("Cannot encode null value");
         }
      }
   }

   private void m(int var1) throws EncodingException {
      int var2 = this.ti.getMaxStringLength();
      if (var2 > 0 && var1 > var2) {
         EncodingException var3 = new EncodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxStringLength " + var2 + " < " + var1);
         logger.warn("assertStringLength: failed", var3);
         throw var3;
      }
   }

   private void a(Object var1, Class<?> var2, UaNodeId var3, int var4) throws EncodingException {
      if (var4 < 0) {
         throw new EncodingException("The given dimensions cannot be negative");
      } else {
         BinaryEncoder.a var5 = null;
         UaNodeId var6 = var3;
         if (var3 == null && var2 != null) {
            var6 = (UaNodeId)sW.getLeft(MultiDimensionArrayUtils.getComponentType(var2));
         }

         if (var6 != null) {
            AtomicReference var8 = new AtomicReference();
            var5 = b(var6, this.getEncoderContext(), var8);
            Class var7 = (Class)var8.get();
         } else {
            Class var16 = MultiDimensionArrayUtils.getComponentType(var2);
            var5 = g(var16);
         }

         if (var4 == 0) {
            var5.put(this, null, var1, var6);
         } else if (var4 == 1) {
            if (var1 == null) {
               this.b(-1);
            } else {
               Object[] var19 = (Object[])var1;
               int var21 = var19.length;
               this.p(var21);
               this.b(var21);

               for (Object var25 : var19) {
                  var5.put(this, null, var25, var6);
               }
            }
         } else if (var1 == null) {
            int[] var18 = new int[var4];

            for (int var20 = 0; var20 < var18.length; var20++) {
               var18[var20] = -1;
            }

            this.a(var18);
         } else {
            int[] var17 = MultiDimensionArrayUtils.getArrayLengths(var1);
            Object[] var9 = (Object[])MultiDimensionArrayUtils.muxArray(var1, var17, Object.class);
            int var10 = var9.length;
            this.p(var10);
            this.a(var17);

            for (Object var14 : var9) {
               var5.put(this, null, var14, var6);
            }
         }
      }
   }

   private void a(Boolean var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.putSByte(null, 0);
         } else {
            this.tt.put((byte)(var1 ? 1 : 0));
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(UnsignedByte var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.q(0);
         } else {
            this.tt.put(var1.toByteBits());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void b(byte[] var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.tt.putInt(-1);
         } else {
            this.l(var1.length);
            this.tt.putInt(var1.length);
            this.tt.put(var1);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void e(ByteString var1) throws EncodingException {
      this.b(var1 == null ? null : var1.getValue());
   }

   private void j(DataValue var1) throws EncodingException {
      if (var1 == null) {
         this.q(0);
      } else {
         byte var2 = 0;
         if (var1.getValue() != null) {
            var2 |= 1;
         }

         if (var1.getStatusCode() != null && !var1.getStatusCode().equals(StatusCode.GOOD)) {
            var2 |= 2;
         }

         if (var1.getSourceTimestamp() != null && !var1.getSourceTimestamp().equals(DateTime.MIN_VALUE)) {
            var2 |= 4;
         }

         if (var1.getServerTimestamp() != null && !var1.getServerTimestamp().equals(DateTime.MIN_VALUE)) {
            var2 |= 8;
         }

         if (var1.getSourcePicoseconds() != null && !var1.getSourcePicoseconds().equals(UnsignedShort.MIN_VALUE)) {
            var2 |= 16;
         }

         if (var1.getServerPicoseconds() != null && !var1.getServerPicoseconds().equals(UnsignedShort.MIN_VALUE)) {
            var2 |= 32;
         }

         this.q(var2);
         if ((var2 & 1) == 1) {
            this.c(var1.getValue());
         }

         if ((var2 & 2) == 2) {
            this.a(var1.getStatusCode());
         }

         if ((var2 & 4) == 4) {
            this.e(var1.getSourceTimestamp());
         }

         if ((var2 & 16) == 16) {
            this.a(var1.getSourcePicoseconds());
         }

         if ((var2 & 8) == 8) {
            this.e(var1.getServerTimestamp());
         }

         if ((var2 & 32) == 32) {
            this.a(var1.getServerPicoseconds());
         }
      }
   }

   private void e(DateTime var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putLong(0L);
         } else if (var1.compareTo(DateTime.MAX_VALUE) >= 0) {
            this.tt.putLong(Long.MAX_VALUE);
         } else if (var1.compareTo(DateTime.MIN_VALUE) <= 0) {
            this.tt.putLong(0L);
         } else {
            this.tt.putLong(var1.getValue());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void b(BigDecimal var1) throws EncodingException {
      ExtensionObject var2 = a(var1);
      this.b(var2);
   }

   private void b(DiagnosticInfo var1) throws EncodingException {
      if (var1 == null) {
         this.q(0);
      } else {
         byte var2 = 0;
         if (var1.getSymbolicId() != null) {
            var2 |= 1;
         }

         if (var1.getNamespaceUriStr() != null) {
            var2 |= 2;
         }

         if (var1.getLocalizedTextStr() != null) {
            var2 |= 4;
         }

         if (var1.getLocaleStr() != null) {
            var2 |= 8;
         }

         if (var1.getAdditionalInfo() != null) {
            var2 |= 16;
         }

         if (var1.getInnerStatusCode() != null) {
            var2 |= 32;
         }

         if (var1.getInnerDiagnosticInfo() != null) {
            var2 |= 64;
         }

         try {
            this.putSByte(null, var2);
            if ((var2 & 1) == 1) {
               this.tt.putInt(var1.getSymbolicId());
            }

            if ((var2 & 2) == 2) {
               this.tt.putInt(var1.getNamespaceUri());
            }

            if ((var2 & 8) == 8) {
               this.tt.putInt(var1.getLocale());
            }

            if ((var2 & 4) == 4) {
               this.tt.putInt(var1.getLocalizedText());
            }

            if ((var2 & 16) == 16) {
               this.H(var1.getAdditionalInfo());
            }

            if ((var2 & 32) == 32) {
               this.a(var1.getInnerStatusCode());
            }

            if ((var2 & 64) == 64) {
               this.b(var1.getInnerDiagnosticInfo());
            }
         } catch (IOException var4) {
            throw b(var4);
         }
      }
   }

   private void c(Double var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putDouble(0.0);
         } else {
            this.tt.putDouble(var1);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(Enumeration var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.tt.putInt(0);
         } else {
            this.tt.putInt(var1.getValue());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void b(ExpandedNodeId var1) throws EncodingException {
      try {
         if (var1 == null) {
            var1 = ExpandedNodeId.NULL;
         }

         byte var2 = 0;
         if (var1.getNamespaceUri() != null) {
            var2 = (byte)(var2 | 128);
         }

         if (var1.getServerIndex() != null) {
            var2 = (byte)(var2 | 64);
         }

         Object var3 = var1.getValue();
         if (var1.getIdType() == IdType.Numeric) {
            UnsignedInteger var4 = (UnsignedInteger)var3;
            if (var4.compareTo((Number)UnsignedByte.MAX_VALUE) <= 0 && var1.getNamespaceIndex() == 0) {
               this.q(NodeIdEncoding.TwoByte.getBits() | var2);
               this.tt.put(var4.byteValue());
            } else if (var4.compareTo((Number)UnsignedShort.MAX_VALUE) <= 0 && var1.getNamespaceIndex() < 256) {
               this.q(NodeIdEncoding.FourByte.getBits() | var2);
               this.q(var1.getNamespaceIndex());
               this.tt.putShort(var4.shortValue());
            } else {
               this.q(NodeIdEncoding.Numeric.getBits() | var2);
               this.tt.putShort((short)var1.getNamespaceIndex());
               this.tt.putInt(var4.intValue());
            }
         }

         if (var1.getIdType() == IdType.String) {
            this.q(NodeIdEncoding.String.getBits() | var2);
            this.tt.putShort((short)var1.getNamespaceIndex());
            this.H((String)var1.getValue());
         } else if (var1.getIdType() == IdType.Opaque) {
            this.q(NodeIdEncoding.ByteString.getBits() | var2);
            this.tt.putShort((short)var1.getNamespaceIndex());
            this.e((ByteString)var1.getValue());
         } else if (var1.getIdType() == IdType.Guid) {
            this.q(NodeIdEncoding.Guid.getBits() | var2);
            this.tt.putShort((short)var1.getNamespaceIndex());
            this.a((UUID)var1.getValue());
         }

         if (var1.getNamespaceUri() != null) {
            this.H(var1.getNamespaceUri());
         }

         if (var1.getServerIndex() != null) {
            this.tt.putInt(var1.getServerIndex().intValue());
         }
      } catch (IOException var5) {
         throw b(var5);
      }
   }

   private void b(ExtensionObject var1) throws EncodingException {
      if (var1 == null) {
         this.f(var1);
         this.n(null);
         this.q(0);
      } else if (!var1.isEncoded()) {
         this.b(ExtensionObject.binaryEncode((Structure)var1.getObject(), this.ti, this.lW));
      } else {
         NodeId var2;
         try {
            var2 = this.ti.getNamespaceTable().toNodeId(var1.getTypeId());
         } catch (ServiceResultException var4) {
            throw new EncodingException("Could not get namespace index for given id");
         }

         this.n(var2);
         Object var3 = var1.getObject();
         if (var3 == null) {
            this.q(0);
         } else if (var1.getEncodeType() == EncodeType.Binary) {
            this.q(1);
            this.e((ByteString)var3);
         } else {
            if (var1.getEncodeType() != EncodeType.Xml) {
               throw new EncodingException("Unexpected object " + var1.getEncodeType());
            }

            this.q(2);
            this.a((XmlElement)var3);
         }
      }
   }

   private void a(Collection<ExtensionObject> var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.tt.putInt(-1);
         } else {
            this.p(var1.size());
            this.tt.putInt(var1.size());

            for (ExtensionObject var3 : var1) {
               this.b(var3);
            }
         }
      } catch (IOException var4) {
         throw b(var4);
      }
   }

   private void c(ExtensionObject[] var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.tt.putInt(-1);
         } else {
            this.p(var1.length);
            this.tt.putInt(var1.length);

            for (ExtensionObject var5 : var1) {
               this.b(var5);
            }
         }
      } catch (IOException var6) {
         throw b(var6);
      }
   }

   private void a(Structure[] var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.tt.putInt(-1);
         } else {
            this.p(var1.length);
            this.tt.putInt(var1.length);

            for (Structure var5 : var1) {
               this.b(new ExtensionObject(var5));
            }
         }
      } catch (IOException var6) {
         throw b(var6);
      }
   }

   private void a(Float var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putFloat(0.0F);
         } else {
            this.tt.putFloat(var1);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(UUID var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putLong(0L);
            this.tt.putLong(0L);
         } else {
            long var2 = var1.getMostSignificantBits();
            long var4 = var1.getLeastSignificantBits();
            byte[] var6 = new byte[16];

            for (int var7 = 0; var7 < 8; var7++) {
               var6[var7] = (byte)(var2 >>> 8 * (7 - var7));
            }

            for (int var9 = 8; var9 < 16; var9++) {
               var6[var9] = (byte)(var4 >>> 8 * (7 - var9));
            }

            this.tt.put(var6[3]);
            this.tt.put(var6[2]);
            this.tt.put(var6[1]);
            this.tt.put(var6[0]);
            this.tt.put(var6[5]);
            this.tt.put(var6[4]);
            this.tt.put(var6[7]);
            this.tt.put(var6[6]);

            for (int var10 = 8; var10 < 16; var10++) {
               this.tt.put(var6[var10]);
            }
         }
      } catch (IOException var8) {
         throw b(var8);
      }
   }

   private void a(Short var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putShort((short)0);
         } else {
            this.tt.putShort(var1);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void b(Integer var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putInt(0);
         } else {
            this.tt.putInt(var1);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(int[] var1) throws EncodingException {
      if (var1 == null) {
         this.a(null, Integer[].class, UaIds.Int32, 1);
      } else {
         Integer[] var2 = new Integer[var1.length];

         for (int var3 = 0; var3 < var1.length; var3++) {
            var2[var3] = var1[var3];
         }

         this.a(var2, Integer[].class, UaIds.Int32, 1);
      }
   }

   private void b(Long var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putLong(0L);
         } else {
            this.tt.putLong(var1);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(LocalizedText var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.putSByte(null, 0);
         } else {
            var1 = var1.asSingleLocale(this.getLocales());
            String var2 = var1.getLocaleId();
            String var3 = var1.getText();
            boolean var4 = var2 != null && !var2.isEmpty();
            boolean var5 = var3 != null && !var3.isEmpty();
            byte var6 = (byte)((var4 ? 1 : 0) | (var5 ? 2 : 0));
            this.tt.put(var6);
            if (var4) {
               this.H(var2);
            }

            if (var5) {
               this.H(var3);
            }
         }
      } catch (IOException var7) {
         throw b(var7);
      }
   }

   private void n(NodeId var1) throws EncodingException {
      try {
         if (var1 == null) {
            var1 = NodeId.NULL;
         }

         Object var2 = var1.getValue();
         if (var1.getIdType() == IdType.Numeric) {
            UnsignedInteger var3 = (UnsignedInteger)var2;
            if (var3.compareTo((Number)UnsignedByte.MAX_VALUE) <= 0 && var1.getNamespaceIndex() == 0) {
               this.tt.put(NodeIdEncoding.TwoByte.getBits());
               this.tt.put(var3.byteValue());
            } else if (var3.compareTo((Number)UnsignedShort.MAX_VALUE) <= 0 && var1.getNamespaceIndex() < 256) {
               this.tt.put(NodeIdEncoding.FourByte.getBits());
               this.q(var1.getNamespaceIndex());
               this.tt.putShort(var3.shortValue());
            } else {
               this.tt.put(NodeIdEncoding.Numeric.getBits());
               this.tt.putShort((short)var1.getNamespaceIndex());
               this.tt.putInt(var3.intValue());
            }
         } else if (var1.getIdType() == IdType.String) {
            this.tt.put(NodeIdEncoding.String.getBits());
            this.tt.putShort((short)var1.getNamespaceIndex());
            this.H((String)var1.getValue());
         } else if (var1.getIdType() == IdType.Opaque) {
            this.tt.put(NodeIdEncoding.ByteString.getBits());
            this.tt.putShort((short)var1.getNamespaceIndex());
            this.e((ByteString)var1.getValue());
         } else {
            if (var1.getIdType() != IdType.Guid) {
               throw new EncodingException("Unknown IdType: " + var1.getIdType());
            }

            UUID var5 = (UUID)var1.getValue();
            this.tt.put(NodeIdEncoding.Guid.getBits());
            this.tt.putShort((short)var1.getNamespaceIndex());
            this.a(var5);
         }
      } catch (IOException var4) {
         throw b(var4);
      }
   }

   private void a(UaOptionSet var1, UaNodeId var2) throws EncodingException {
      OptionSetSpecification var3 = this.getEncoderContext().getOptionSetSpecification(var2);
      if (var3 == null) {
         throw new EncodingException(
            "Cannot resolve OptionSetSpecification for: " + var2 + ", thus cannot encode the 'null' form of this type as it's length is unknown"
         );
      } else {
         if (var1 == null) {
            this.put(null, null, var3.getBaseTypeId(), 0);
         } else {
            this.put(null, var1.getValue(), var3.getBaseTypeId(), 0);
         }
      }
   }

   private void d(QualifiedName var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.tt.putShort((short)0);
            this.H(null);
         } else {
            this.tt.putShort((short)var1.getNamespaceIndex());
            this.H(var1.getName());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(Byte var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.q(0);
         } else {
            this.tt.put(var1);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void q(int var1) throws EncodingException {
      this.a((byte)var1);
   }

   private void a(StatusCode var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putInt(StatusCode.GOOD.getValueAsIntBits());
         } else {
            this.tt.putInt(var1.getValueAsIntBits());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void H(String var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putInt(-1);
         } else {
            this.m(var1.length());
            byte[] var2 = var1.getBytes(StandardCharsets.UTF_8);
            this.tt.putInt(var2.length);
            this.tt.put(var2);
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(Structure var1, StructureSpecification var2) throws EncodingException {
      try {
         if (var1 == null) {
            var1 = var2.toInstanceBuilder().build();
         }

         Map var3 = var1.toFieldsMap(var2);
         if (StructureType.UNION != var2.getStructureType() && StructureType.UNION_SUBTYPES != var2.getStructureType()) {
            if (StructureType.OPTIONAL == var2.getStructureType()) {
               int var11 = -1;
               UnsignedInteger var5 = UnsignedInteger.ZERO;

               for (Entry var16 : var3.entrySet()) {
                  if (((FieldSpecification)var16.getKey()).isOptional()) {
                     var11++;
                     if (var1 != null && var16.getValue() != null) {
                        var5 = b(var5, var11);
                     }
                  }
               }

               this.putUInt32("EncodingMask", var5);
            }

            for (Entry var13 : var3.entrySet()) {
               FieldSpecification var15 = (FieldSpecification)var13.getKey();
               Object var17 = var13.getValue();
               if (!var15.isOptional() || var17 != null) {
                  int var19 = var15.getValueRank() < 0 ? 0 : var15.getValueRank();
                  if (var15.isAllowSubTypes()) {
                     if (ExtensionObject.class.isAssignableFrom(var15.getCompositeClass())) {
                        this.put(null, var17, UaIds.Structure, var19);
                     } else {
                        if (!Object.class.equals(var15.getCompositeClass())) {
                           throw new EncodingException(
                              "The java class for AllowSubTypes field should either be ExtensionObject.class or Object.class, got: "
                                 + var15
                                 + "in structure type: "
                                 + var2
                           );
                        }

                        this.put(null, var17, UaIds.BaseDataType, var19);
                     }
                  } else {
                     this.put(null, var17, var15.getDataTypeId(), var19);
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
               int var18 = var6.getValueRank() < 0 ? 0 : var6.getValueRank();
               this.put("Value", var7, var6.getDataTypeId(), var18);
            }
         }
      } catch (Exception var10) {
         throw new EncodingException("Could not encode Structure", var10);
      }
   }

   private void a(UnsignedShort var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putShort((short)0);
         } else {
            this.tt.putShort(var1.toShortBits());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void j(UnsignedInteger var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putInt(0);
         } else {
            this.tt.putInt(var1.toIntBits());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(UnsignedLong var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.f(var1);
            this.tt.putLong(0L);
         } else {
            this.tt.putLong(var1.toLongBits());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   private void a(XmlElement var1) throws EncodingException {
      try {
         if (var1 == null) {
            this.tt.putInt(-1);
         } else {
            this.b(var1.getData());
         }
      } catch (IOException var3) {
         throw b(var3);
      }
   }

   @Deprecated
   void a(int var1, Object var2) throws EncodingException {
      switch (var1) {
         case 1:
            this.putBooleanArray(null, (Boolean[])var2);
            break;
         case 2:
            this.putSByteArray(null, (Byte[])var2);
            break;
         case 3:
            this.putByteArray(null, (UnsignedByte[])var2);
            break;
         case 4:
            this.putInt16Array(null, (Short[])var2);
            break;
         case 5:
            this.putUInt16Array(null, (UnsignedShort[])var2);
            break;
         case 6:
            this.putInt32Array(null, (Integer[])var2);
            break;
         case 7:
            this.putUInt32Array(null, (UnsignedInteger[])var2);
            break;
         case 8:
            this.putInt64Array(null, (Long[])var2);
            break;
         case 9:
            this.putUInt64Array(null, (UnsignedLong[])var2);
            break;
         case 10:
            this.putFloatArray(null, (Float[])var2);
            break;
         case 11:
            this.putDoubleArray(null, (Double[])var2);
            break;
         case 12:
            this.putStringArray(null, (String[])var2);
            break;
         case 13:
            this.putDateTimeArray(null, (DateTime[])var2);
            break;
         case 14:
            this.putGuidArray(null, (UUID[])var2);
            break;
         case 15:
            this.putByteStringArray(null, (ByteString[])var2);
            break;
         case 16:
            this.putXmlElementArray(null, (XmlElement[])var2);
            break;
         case 17:
            this.putNodeIdArray(null, (NodeId[])var2);
            break;
         case 18:
            this.putExpandedNodeIdArray(null, (ExpandedNodeId[])var2);
            break;
         case 19:
            this.putStatusCodeArray(null, (StatusCode[])var2);
            break;
         case 20:
            this.putQualifiedNameArray(null, (QualifiedName[])var2);
            break;
         case 21:
            this.putLocalizedTextArray(null, (LocalizedText[])var2);
            break;
         case 22:
            if (var2 instanceof ExtensionObject[]) {
               this.c((ExtensionObject[])var2);
            } else {
               if (!(var2 instanceof Structure[])) {
                  throw new EncodingException("cannot encode " + var2);
               }

               this.a((Structure[])var2);
            }
            break;
         case 23:
            this.putDataValueArray(null, (DataValue[])var2);
            break;
         case 24:
            this.putVariantArray(null, (Variant[])var2);
            break;
         case 25:
            this.putDiagnosticInfoArray(null, (DiagnosticInfo[])var2);
            break;
         default:
            throw new EncodingException("cannot encode builtin type " + var1);
      }
   }

   @Deprecated
   void b(int var1, Object var2) throws EncodingException {
      switch (var1) {
         case 1:
            this.a((Boolean)var2);
            break;
         case 2:
            this.a((Byte)var2);
            break;
         case 3:
            this.a((UnsignedByte)var2);
            break;
         case 4:
            this.a((Short)var2);
            break;
         case 5:
            this.a((UnsignedShort)var2);
            break;
         case 6:
            this.b((Integer)var2);
            break;
         case 7:
            this.j((UnsignedInteger)var2);
            break;
         case 8:
            this.b((Long)var2);
            break;
         case 9:
            this.a((UnsignedLong)var2);
            break;
         case 10:
            this.a((Float)var2);
            break;
         case 11:
            this.c((Double)var2);
            break;
         case 12:
            this.H((String)var2);
            break;
         case 13:
            this.e((DateTime)var2);
            break;
         case 14:
            this.a((UUID)var2);
            break;
         case 15:
            this.e((ByteString)var2);
            break;
         case 16:
            this.a((XmlElement)var2);
            break;
         case 17:
            this.n((NodeId)var2);
            break;
         case 18:
            this.b((ExpandedNodeId)var2);
            break;
         case 19:
            this.a((StatusCode)var2);
            break;
         case 20:
            this.d((QualifiedName)var2);
            break;
         case 21:
            this.a((LocalizedText)var2);
            break;
         case 22:
            if (var2 instanceof Structure) {
               this.b(new ExtensionObject((Structure)var2));
            } else {
               this.b((ExtensionObject)var2);
            }
            break;
         case 23:
            this.j((DataValue)var2);
            break;
         case 24:
            this.c((Variant)var2);
            break;
         case 25:
            this.b((DiagnosticInfo)var2);
            break;
         default:
            throw new EncodingException("cannot encode builtin type " + var1);
      }
   }

   void c(Variant var1) throws EncodingException {
      if (var1 == null) {
         this.f(var1);
         this.q(0);
      } else {
         Object var2 = var1.getValue();
         if (var2 == null) {
            this.q(0);
         } else {
            Class var3 = var1.getCompositeClass();
            int var4;
            boolean var5;
            if (BigDecimal.class.isAssignableFrom(var3)) {
               var4 = 22;
               var5 = true;
            } else {
               var5 = false;
               if (Structure.class.isAssignableFrom(var3)) {
                  var4 = 22;
               } else {
                  if (!BuiltinsMap.ID_MAP.containsKey(var3)) {
                     throw new EncodingException("Non-suitable composite class for Variant: " + var3);
                  }

                  var4 = BuiltinsMap.ID_MAP.get(var3);
               }
            }

            if (!var1.isArray()) {
               this.q(var4);
               if (var5) {
                  var2 = a((BigDecimal)var2);
               }

               this.b(var4, var2);
            } else {
               int var6 = var1.getDimension();
               if (var6 == 1) {
                  this.q(var4 | 128);
                  if (var5) {
                     var2 = a((BigDecimal[])var2);
                  }

                  this.a(var4, var2);
               } else {
                  int[] var7 = var1.getArrayDimensions();
                  int var8 = MultiDimensionArrayUtils.getLength(var7);
                  ArrayIterator var9 = MultiDimensionArrayUtils.arrayIterator(var1.getValue(), var1.getArrayDimensions());

                  try {
                     this.q(var4 | 192);
                     this.tt.putInt(var8);

                     while (var9.hasNext()) {
                        Object var10 = var9.next();
                        if (var5) {
                           var10 = a((BigDecimal)var10);
                        }

                        this.b(var4, var10);
                     }

                     this.a(var7);
                  } catch (ArrayIndexOutOfBoundsException var11) {
                     throw new EncodingException("The dimensions of inner array elements of a multi-dimension variable must be equal in length", var11);
                  } catch (IOException var12) {
                     throw b(var12);
                  }
               }
            }
         }
      }
   }

   static {
      b(UaIds.Boolean, Boolean.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.SByte, Byte.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.Byte, UnsignedByte.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.Int16, Short.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.UInt16, UnsignedShort.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.Int32, Integer.class, (var0, var1, var2, var3) -> var0.b(var2));
      b(UaIds.UInt32, UnsignedInteger.class, (var0, var1, var2, var3) -> var0.j(var2));
      b(UaIds.Int64, Long.class, (var0, var1, var2, var3) -> var0.b(var2));
      b(UaIds.UInt64, UnsignedLong.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.Float, Float.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.Double, Double.class, (var0, var1, var2, var3) -> var0.c(var2));
      b(UaIds.String, String.class, (var0, var1, var2, var3) -> var0.H(var2));
      b(UaIds.Guid, UUID.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.ByteString, ByteString.class, (var0, var1, var2, var3) -> var0.b(ByteString.asByteArray(var2)));
      b(UaIds.XmlElement, XmlElement.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.NodeId, NodeId.class, (var0, var1, var2, var3) -> var0.n(var2));
      b(UaIds.ExpandedNodeId, ExpandedNodeId.class, (var0, var1, var2, var3) -> var0.b(var2));
      b(UaIds.StatusCode, StatusCode.class, (var0, var1, var2, var3) -> var0.a(var2));
      b(UaIds.QualifiedName, QualifiedName.class, (var0, var1, var2, var3) -> var0.d(var2));
      b(UaIds.LocalizedText, LocalizedText.class, (var0, var1, var2, var3) -> var0.a(var2));
      a(UaIds.DateTime, DateTime.class, tk);
      a(UaIds.Structure, ExtensionObject.class, tl);
      a(UaIds.DataValue, DataValue.class, tn);
      a(UaIds.DiagnosticInfo, DiagnosticInfo.class, tp);
      a(UaIds.Decimal, BigDecimal.class, tr);
      b(null, Object.class, (var0, var1, var2, var3) -> {
         if (var2 == null) {
            to.put(var0, var1, null, UaIds.BaseDataType);
         } else if (var2 instanceof Variant) {
            to.put(var0, var1, (Variant)var2, UaIds.BaseDataType);
         } else {
            to.put(var0, var1, new Variant(var2), UaIds.BaseDataType);
         }
      });
   }

   private interface a<T> {
      void put(BinaryEncoder var1, String var2, T var3, UaNodeId var4) throws EncodingException;
   }
}
