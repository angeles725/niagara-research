package com.prosysopc.ua.stack.encoding.binary;

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
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.encoding.DecodingException;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.IDecoder;
import com.prosysopc.ua.stack.utils.BijectionMap;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.stack.utils.bytebuffer.ByteBufferReadable;
import com.prosysopc.ua.stack.utils.bytebuffer.IBinaryReadable;
import com.prosysopc.ua.stack.utils.bytebuffer.InputStreamReadable;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.SimpleTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification.StructureType;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryDecoder implements IDecoder {
   private static final Logger logger = LoggerFactory.getLogger(BinaryDecoder.class);
   private static final Map<Class<?>, BinaryDecoder.a<?>> sU = new HashMap<>();
   private static final Map<UaNodeId, BinaryDecoder.a<?>> sV = new HashMap<>();
   private static final BijectionMap<UaNodeId, Class<?>> sW = new BijectionMap();
   private static final BinaryDecoder.a<DateTime> sX = (var0, var1, var2, var3) -> var0.erS();
   private static final BinaryDecoder.a<ExtensionObject> sY = (var0, var1, var2, var3) -> var0.erW();
   private static final BinaryDecoder.a<Structure> sZ = (var0, var1, var2, var3) -> {
      if (var3 == null) {
         throw new DecodingException("DataTypeId is null, class: " + var2);
      } else {
         StructureSpecification var4 = var0.getEncoderContext().getStructureSpecification(var3);
         if (var4 == null) {
            throw new DecodingException("Cannot find StructureSpecification for DataType: " + var3);
         } else {
            return var0.b(var4);
         }
      }
   };
   private static final BinaryDecoder.a<DataValue> ta = (var0, var1, var2, var3) -> var0.erR();
   private static final BinaryDecoder.a<Variant> tb = (var0, var1, var2, var3) -> var0.esm();
   private static final BinaryDecoder.a<DiagnosticInfo> tc = (var0, var1, var2, var3) -> var0.erT();
   private static final BinaryDecoder.a<Enumeration> td = (var0, var1, var2, var3) -> var0.a(var2, var3);
   private static final BinaryDecoder.a<BigDecimal> te = (var0, var1, var2, var3) -> var0.G(var1);
   private static final BinaryDecoder.a<UaOptionSet> tf = (var0, var1, var2, var3) -> var0.a(var1, var3);
   private static final ExpandedNodeId tg = new ExpandedNodeId("http://opcfoundation.org/UA/", Identifiers.Decimal.getValue());
   IBinaryReadable th;
   EncoderContext ti;
   private BiConsumer<FieldSpecification, Object> tj;

   private static <T> void a(UaNodeId var0, Class<T> var1, BinaryDecoder.a<T> var2) throws Error {
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

   private static <T> void b(UaNodeId var0, Class<T> var1, BinaryDecoder.a<T> var2) {
      if (!Object.class.equals(var1) && !Modifier.isFinal(var1.getModifiers())) {
         throw new Error("Class " + var1 + " is not final, and cannot be put to known final classes serialization helper");
      } else if (sU.put(var1, var2) != null) {
         throw new Error("Class " + var1 + " already has a serializer defined");
      } else {
         a(var0, var1, var2);
      }
   }

   private static <T> BinaryDecoder.a<T> f(Class<?> var0) throws DecodingException {
      if (var0 == null) {
         throw new DecodingException("Cannot decode class null");
      } else {
         BinaryDecoder.a var1 = sU.get(var0);
         if (var1 != null) {
            return var1;
         } else if (ExtensionObject.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)sY;
         } else if (Structure.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)sZ;
         } else if (DataValue.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)ta;
         } else if (Variant.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)tb;
         } else if (DiagnosticInfo.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)tc;
         } else if (Enumeration.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)td;
         } else if (DateTime.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)sX;
         } else if (BigDecimal.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)te;
         } else if (UaOptionSet.class.isAssignableFrom(var0)) {
            return (BinaryDecoder.a<T>)tf;
         } else {
            throw new DecodingException("Cannot decode class: " + var0);
         }
      }
   }

   private static BinaryDecoder.a<Object> a(UaNodeId var0, EncoderContext var1, AtomicReference<Class<?>> var2) throws DecodingException {
      if (var0 == null) {
         throw new IllegalArgumentException("The given DataTypeId cannot be null");
      } else if (UaIds.BaseDataType.equals(var0)) {
         var2.set(Object.class);
         return f(Object.class);
      } else if (UaIds.Structure.equals(var0)) {
         var2.set(ExtensionObject.class);
         return f(ExtensionObject.class);
      } else {
         BinaryDecoder.a var3 = sV.get(var0);
         if (var3 == null) {
            UaDataTypeSpecification var4 = var1.getDataTypeSpecification(var0);
            if (var4 == null) {
               logger.warn("Cannot find UaDataTypeSpecification for DataTypeId: {}", var0);
            } else if (var4 instanceof SimpleTypeSpecification) {
               var3 = sV.get(((SimpleTypeSpecification)var4).getBaseTypeId());
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof StructureSpecification) {
               var3 = sZ;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof OptionSetSpecification) {
               var3 = tf;
               var2.set(var4.getJavaClass());
            } else if (var4 instanceof EnumerationSpecification) {
               var3 = td;
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

   private static boolean a(UnsignedInteger var0, int var1) {
      if (var0 == null) {
         throw new IllegalArgumentException("mask cannot be null");
      } else if (var1 >= 0 && var1 <= 31) {
         int var2 = 1 << var1;
         return UnsignedInteger.ZERO.equals(var0) ? false : var0.and(var2).equals(UnsignedInteger.valueOf(var2));
      } else {
         throw new IllegalArgumentException("position must be between 0-31");
      }
   }

   private static DecodingException a(IOException var0) {
      if (var0 instanceof ClosedChannelException) {
         return new DecodingException(StatusCodes.Bad_ConnectionClosed, var0);
      } else if (var0 instanceof EOFException) {
         return new DecodingException(StatusCodes.Bad_EndOfStream, var0);
      } else if (var0 instanceof ConnectException) {
         return new DecodingException(StatusCodes.Bad_ConnectionRejected, var0);
      } else {
         return var0 instanceof SocketException
            ? new DecodingException(StatusCodes.Bad_UnexpectedError, var0)
            : new DecodingException(StatusCodes.Bad_UnexpectedError, var0);
      }
   }

   public BinaryDecoder(byte[] var1) {
      ByteBuffer var2 = ByteBuffer.wrap(var1);
      var2.order(ByteOrder.LITTLE_ENDIAN);
      this.setReadable(new ByteBufferReadable(var2));
   }

   public BinaryDecoder(byte[] var1, int var2, int var3) {
      ByteBuffer var4 = ByteBuffer.wrap(var1, var2, var3);
      var4.order(ByteOrder.LITTLE_ENDIAN);
      this.setReadable(new ByteBufferReadable(var4));
   }

   public BinaryDecoder(ByteBuffer var1) {
      this.setReadable(new ByteBufferReadable(var1));
   }

   public BinaryDecoder(IBinaryReadable var1) {
      this.setReadable(var1);
   }

   public BinaryDecoder(InputStream var1, int var2) {
      InputStreamReadable var3 = new InputStreamReadable(var1, var2);
      var3.order(ByteOrder.LITTLE_ENDIAN);
      this.setReadable(var3);
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
               return this.a((Class<T>)var4, var2, var3);
            }
         } catch (Exception var6) {
            throw new DecodingException("Error while trying to decode, DataTypeId: " + var2, var6);
         }
      }
   }

   @Override
   public EncoderContext getEncoderContext() {
      return this.ti;
   }

   @Override
   public int[] getInt32Array_(String var1) throws DecodingException {
      return this.esb();
   }

   public IBinaryReadable getReadable() {
      return this.th;
   }

   public void setEncoderContext(EncoderContext var1) {
      this.ti = var1;
   }

   public void setReadable(IBinaryReadable var1) {
      if (var1.order() != ByteOrder.LITTLE_ENDIAN) {
         throw new IllegalArgumentException("Readable must be in Little-Ending byte order");
      } else {
         this.th = var1;
      }
   }

   public void setStructureFieldDecodeListener(BiConsumer<FieldSpecification, Object> var1) {
      this.tj = var1;
   }

   private void a(int var1, int var2) throws DecodingException {
      if (var1 < -1) {
         throw new DecodingException(StatusCodes.Bad_DecodingError, "Illegal array length " + var1);
      } else {
         int var3 = this.ti.getMaxArrayLength();
         if (var3 > 0 && var1 > var3) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxArrayLength=" + var3 + " < " + var1);
         } else {
            long var4 = var1;
            if (var4 * var2 > this.remaining()) {
               throw new DecodingException(StatusCodes.Bad_EndOfStream, "Buffer underflow");
            }
         }
      }
   }

   private void l(int var1) throws DecodingException {
      if (var1 < -1) {
         throw new DecodingException(StatusCodes.Bad_DecodingError, "Unexpected byte string length " + var1);
      } else {
         int var2 = this.ti.getMaxByteStringLength();
         if (var2 > 0 && var1 > var2) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxByteStringLength " + var2 + " < " + var1);
         } else if (var1 > this.remaining()) {
            throw new DecodingException(StatusCodes.Bad_EndOfStream, "Buffer underflow");
         }
      }
   }

   private void m(int var1) throws DecodingException {
      if (var1 < -1) {
         throw new DecodingException(StatusCodes.Bad_DecodingError, "Unexpected string length " + var1);
      } else {
         int var2 = this.ti.getMaxStringLength();
         if (var2 > 0 && var1 > var2) {
            throw new DecodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxStringLength " + var2 + " < " + var1);
         } else if (var1 > this.remaining()) {
            throw new DecodingException(StatusCodes.Bad_EndOfStream, "Buffer underflow");
         }
      }
   }

   private BigDecimal d(ByteString var1) {
      byte[] var2 = var1.getValue();
      ByteBuffer var3 = ByteBuffer.allocate(2);
      var3.order(ByteOrder.LITTLE_ENDIAN);
      var3.put(var2[0]);
      var3.put(var2[1]);
      ((Buffer)var3).rewind();
      short var4 = var3.getShort();
      byte[] var5 = Arrays.copyOfRange(var2, 2, var2.length);
      var5 = com.prosysopc.ua.stack.encoding.binary.a.reverse(var5);
      BigInteger var6 = new BigInteger(var5);
      return new BigDecimal(var6, var4);
   }

   private BigDecimal G(String var1) throws DecodingException {
      ExtensionObject var2 = this.getExtensionObject(var1);
      if (!this.getEncoderContext().getNamespaceTable().nodeIdEquals(var2.getTypeId(), tg)) {
         logger.error("Encountered a Decimal that does not define correct id, is {}", var2.getTypeId());
      }

      try {
         return this.d((ByteString)var2.getObject());
      } catch (ClassCastException var4) {
         throw new DecodingException("Did not get an ExtensionObject with ByteString data for Decimal type", var4);
      }
   }

   private UaOptionSet a(String var1, UaNodeId var2) throws DecodingException {
      OptionSetSpecification var3 = this.getEncoderContext().getOptionSetSpecification(var2);
      if (var3 == null) {
         throw new DecodingException("Cannot resolve OptionSetSpecification for: " + var2);
      } else {
         Object var4 = this.a(var3.getBaseTypeJavaClass(), var3.getBaseTypeId(), 0);

         try {
            return (UaOptionSet)new Variant(var4).asOptionSet(var3);
         } catch (Exception var6) {
            throw new DecodingException(var6, "Could not resolve the value as UaOptionSet");
         }
      }
   }

   private <T> T a(Class<T> var1, UaNodeId var2, int var3) throws DecodingException {
      if (var3 < 0) {
         throw new DecodingException("The given dimensions cannot be negative");
      } else {
         Object var4 = null;
         UaNodeId var5 = var2;
         if (var2 == null && var1 != null) {
            var5 = (UaNodeId)sW.getLeft(MultiDimensionArrayUtils.getComponentType(var1));
         }

         Class var6;
         if (var5 != null) {
            AtomicReference var7 = new AtomicReference();
            var4 = a(var5, this.getEncoderContext(), var7);
            var6 = (Class)var7.get();
         } else {
            var6 = MultiDimensionArrayUtils.getComponentType(var1);
            var4 = f(var6);
         }

         if (var3 == 0) {
            return (T)((BinaryDecoder.a)var4).get(this, null, var1, var2);
         } else if (var3 == 1) {
            int var15 = this.getInt32(null);
            if (var15 == -1) {
               return null;
            } else {
               Object[] var16 = (Object[])Array.newInstance(var6, var15);
               BinaryDecoder.a var18 = (BinaryDecoder.a)var4;

               for (int var20 = 0; var20 < var15; var20++) {
                  var16[var20] = var18.get(this, null, var6, var2);
               }

               return (T)var16;
            }
         } else {
            int[] var14 = this.getInt32Array_(null);
            if (var14 == null) {
               return null;
            } else {
               int var8 = 1;

               for (int var12 : var14) {
                  if (var12 < 0) {
                     return null;
                  }

                  var8 *= var12;
               }

               Object[] var17 = (Object[])Array.newInstance(var6, var8);
               BinaryDecoder.a var19 = (BinaryDecoder.a)var4;

               for (int var21 = 0; var21 < var8; var21++) {
                  var17[var21] = var19.get(this, null, var6, var2);
               }

               return (T)MultiDimensionArrayUtils.demuxArray(var17, var14, var6);
            }
         }
      }
   }

   private Object n(int var1) throws DecodingException {
      switch (var1) {
         case 1:
            return this.getBooleanArray(null);
         case 2:
            return this.getSByteArray(null);
         case 3:
            return this.getByteArray(null);
         case 4:
            return this.getInt16Array(null);
         case 5:
            return this.getUInt16Array(null);
         case 6:
            return this.getInt32Array(null);
         case 7:
            return this.getUInt32Array(null);
         case 8:
            return this.getInt64Array(null);
         case 9:
            return this.getUInt64Array(null);
         case 10:
            return this.getFloatArray(null);
         case 11:
            return this.getDoubleArray(null);
         case 12:
            return this.getStringArray(null);
         case 13:
            return this.getDateTimeArray(null);
         case 14:
            return this.getGuidArray(null);
         case 15:
            return this.getByteStringArray(null);
         case 16:
            return this.getXmlElementArray(null);
         case 17:
            return this.getNodeIdArray(null);
         case 18:
            return this.getExpandedNodeIdArray(null);
         case 19:
            return this.getStatusCodeArray(null);
         case 20:
            return this.getQualifiedNameArray(null);
         case 21:
            return this.getLocalizedTextArray(null);
         case 22:
            return this.getExtensionObjectArray(null);
         case 23:
            return this.getDataValueArray(null);
         case 24:
            return this.getVariantArray(null);
         case 25:
            return this.getDiagnosticInfoArray(null);
         default:
            throw new DecodingException("Cannot decode builtin type id " + var1);
      }
   }

   private Boolean erO() throws DecodingException {
      try {
         return this.th.get() == 0 ? Boolean.FALSE : Boolean.TRUE;
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private UnsignedByte erP() throws DecodingException {
      try {
         return UnsignedByte.getFromBits(this.th.get());
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private ByteString erQ() throws DecodingException {
      try {
         int var1 = this.th.getInt();
         if (var1 == -1) {
            return null;
         } else {
            this.l(var1);
            byte[] var2 = new byte[var1];
            this.th.get(var2);
            return ByteString.valueOf(var2);
         }
      } catch (IOException var3) {
         throw a(var3);
      }
   }

   private DataValue erR() throws DecodingException {
      try {
         byte var1 = this.th.get();
         Variant var2 = (var1 & 1) != 0 ? this.getVariant(null) : null;
         StatusCode var3 = (var1 & 2) != 0 ? this.getStatusCode(null) : StatusCode.GOOD;
         DateTime var4 = (var1 & 4) != 0 ? this.getDateTime(null) : null;
         UnsignedShort var5 = (var1 & 16) != 0 ? this.getUInt16(null) : UnsignedShort.MIN_VALUE;
         DateTime var6 = (var1 & 8) != 0 ? this.getDateTime(null) : null;
         UnsignedShort var7 = (var1 & 32) != 0 ? this.getUInt16(null) : UnsignedShort.MIN_VALUE;
         return new DataValue(var2, var3, var4, var5, var6, var7);
      } catch (IOException var8) {
         throw a(var8);
      }
   }

   private DateTime erS() throws DecodingException {
      try {
         long var1 = this.th.getLong();
         return DateTime.valueOf(var1);
      } catch (IOException var3) {
         throw a(var3);
      }
   }

   private DiagnosticInfo erT() throws DecodingException {
      try {
         byte var1 = this.th.get();
         Integer var2 = (var1 & 1) != 0 ? this.getInt32(null) : null;
         Integer var3 = (var1 & 2) != 0 ? this.getInt32(null) : null;
         Integer var4 = (var1 & 8) != 0 ? this.getInt32(null) : null;
         Integer var5 = (var1 & 4) != 0 ? this.getInt32(null) : null;
         String var6 = (var1 & 16) != 0 ? this.getString(null) : null;
         StatusCode var7 = (var1 & 32) != 0 ? this.getStatusCode(null) : null;
         DiagnosticInfo var8 = (var1 & 64) != 0 ? this.getDiagnosticInfo(null) : null;
         return new DiagnosticInfo(var6, var8, var7, var4, var5, var3, var2);
      } catch (IOException var9) {
         throw a(var9);
      }
   }

   private Double erU() throws DecodingException {
      try {
         return this.th.getDouble();
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private <T extends Enumeration> T a(Class<T> var1, UaNodeId var2) throws DecodingException {
      try {
         int var3 = this.th.getInt();
         EnumerationSpecification var4 = this.getEncoderContext().getEnumerationSpecification(var2);
         if (var4 != null) {
            return (T)var4.getByValue(var3);
         } else {
            Method var5 = var1.getMethod("valueOf", int.class);
            return (T)var5.invoke(null, var3);
         }
      } catch (SecurityException var6) {
         throw new DecodingException(var6, "cannot decode " + var1);
      } catch (NoSuchMethodException var7) {
         throw new DecodingException(var7, "cannot decode " + var1);
      } catch (IllegalArgumentException var8) {
         throw new DecodingException(var8, "cannot decode " + var1);
      } catch (IllegalAccessException var9) {
         throw new DecodingException(var9, "cannot decode " + var1);
      } catch (InvocationTargetException var10) {
         throw new DecodingException(var10, "cannot decode " + var1);
      } catch (IOException var11) {
         throw a(var11);
      }
   }

   private ExpandedNodeId erV() throws DecodingException {
      try {
         byte var1 = this.th.get();
         boolean var2 = (var1 & 64) == 64;
         boolean var3 = (var1 & 128) == 128;
         NodeIdEncoding var4 = NodeIdEncoding.getNodeIdEncoding(var1 & 63);
         if (var4 == null) {
            throw new DecodingException("Unexpected NodeId Encoding Byte " + var1);
         } else {
            Object var5 = null;
            int var6 = 0;
            String var7 = null;
            UnsignedInteger var8 = null;
            if (var4 == NodeIdEncoding.TwoByte) {
               var6 = 0;
               var5 = UnsignedInteger.getFromBits(this.th.get() & 255);
            }

            if (var4 == NodeIdEncoding.FourByte) {
               var6 = this.th.get() & 255;
               var5 = UnsignedInteger.getFromBits(this.th.getShort() & '\uffff');
            }

            if (var4 == NodeIdEncoding.Numeric) {
               var6 = this.th.getShort() & '\uffff';
               var5 = UnsignedInteger.getFromBits(this.th.getInt());
            }

            if (var4 == NodeIdEncoding.String) {
               var6 = this.th.getShort() & '\uffff';
               var5 = this.getString(null);
            }

            if (var4 == NodeIdEncoding.ByteString) {
               var6 = this.th.getShort() & '\uffff';
               var5 = this.getByteString(null);
               if (var5 != null) {
                  var5 = ((ByteString)var5).getValue();
               }
            }

            if (var4 == NodeIdEncoding.Guid) {
               var6 = this.th.getShort() & '\uffff';
               var5 = this.getGuid(null);
            }

            if (var3) {
               var7 = this.getString(null);
            }

            if (var2) {
               var8 = this.getUInt32(null);
            }

            return var7 != null ? new ExpandedNodeId(var8, var7, var5) : new ExpandedNodeId(var8, var6, var5);
         }
      } catch (IOException var9) {
         throw a(var9);
      }
   }

   private ExtensionObject erW() throws DecodingException {
      try {
         NodeId var1 = this.getNodeId(null);
         ExpandedNodeId var2 = this.ti.getNamespaceTable().toExpandedNodeId(var1);
         byte var3 = this.th.get();
         if (var3 == 0) {
            return var1 != null && !var1.equals(NodeId.NULL) ? new ExtensionObject(var2) : null;
         } else {
            ExtensionObject var4;
            if (var3 == 1) {
               var4 = new ExtensionObject(var2, this.getByteString(null));
            } else {
               if (var3 != 2) {
                  throw new DecodingException("Unexpected encoding byte (" + var3 + ") in ExtensionObject");
               }

               var4 = new ExtensionObject(var2, this.getXmlElement(null));
            }

            if (this.a(var4)) {
               return var4;
            } else {
               try {
                  Structure var5 = var4.decode(this.getEncoderContext());
                  return new ExtensionObject(var5);
               } catch (DecodingException var6) {
                  return var4;
               }
            }
         }
      } catch (IOException var7) {
         throw a(var7);
      }
   }

   private Float erX() throws DecodingException {
      try {
         return this.th.getFloat();
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private UUID erY() throws DecodingException {
      try {
         byte[] var1 = new byte[16];
         var1[3] = this.th.get();
         var1[2] = this.th.get();
         var1[1] = this.th.get();
         var1[0] = this.th.get();
         var1[5] = this.th.get();
         var1[4] = this.th.get();
         var1[7] = this.th.get();
         var1[6] = this.th.get();

         for (int var2 = 8; var2 < 16; var2++) {
            var1[var2] = this.th.get();
         }

         long var8 = 0L;
         long var4 = 0L;

         for (int var6 = 0; var6 < 8; var6++) {
            var8 = var8 << 8 | var1[var6] & 255;
         }

         for (int var9 = 8; var9 < 16; var9++) {
            var4 = var4 << 8 | var1[var9] & 255;
         }

         return new UUID(var8, var4);
      } catch (IOException var7) {
         throw a(var7);
      }
   }

   private Short erZ() throws DecodingException {
      try {
         return this.th.getShort();
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private Integer esa() throws DecodingException {
      try {
         return this.th.getInt();
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private int[] esb() throws DecodingException {
      try {
         int var1 = this.th.getInt();
         if (var1 == -1) {
            return null;
         } else {
            this.a(var1, 4);
            int[] var2 = new int[var1];

            for (int var3 = 0; var3 < var1; var3++) {
               var2[var3] = this.getInt32(null);
            }

            return var2;
         }
      } catch (IOException var4) {
         throw a(var4);
      }
   }

   private Long esc() throws DecodingException {
      try {
         return this.th.getLong();
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private LocalizedText esd() throws DecodingException {
      try {
         byte var1 = this.th.get();
         String var2 = null;
         String var3 = null;
         if ((var1 & 1) == 1) {
            var2 = this.getString(null);
         }

         if ((var1 & 2) == 2) {
            var3 = this.getString(null);
         }

         return LocalizedText.builder().setText(var3, var2).build();
      } catch (IOException var4) {
         throw a(var4);
      }
   }

   private NodeId ese() throws DecodingException {
      try {
         byte var1 = this.th.get();
         NodeIdEncoding var2 = NodeIdEncoding.getNodeIdEncoding(var1);
         if (var2 == null) {
            throw new DecodingException("Unexpected NodeId Encoding Byte " + var1);
         } else {
            int var4 = 0;
            NodeId var3;
            if (var2 == NodeIdEncoding.TwoByte) {
               var4 = (byte)0;
               UnsignedInteger var5 = UnsignedInteger.getFromBits(this.th.get() & 255);
               var3 = new NodeId(var4, var5);
            } else if (var2 == NodeIdEncoding.FourByte) {
               var4 = this.th.get() & 255;
               UnsignedInteger var13 = UnsignedInteger.getFromBits(this.th.getShort() & '\uffff');
               var3 = new NodeId(var4, var13);
            } else if (var2 == NodeIdEncoding.Numeric) {
               var4 = this.th.getShort() & '\uffff';
               UnsignedInteger var14 = this.getUInt32(null);
               var3 = new NodeId(var4, var14);
            } else if (var2 == NodeIdEncoding.String) {
               var4 = this.th.getShort() & '\uffff';
               String var15 = this.getString(null);
               var3 = new NodeId(var4, var15);
            } else if (var2 == NodeIdEncoding.ByteString) {
               var4 = this.th.getShort() & '\uffff';
               ByteString var16 = this.getByteString(null);
               var3 = new NodeId(var4, ByteString.asByteArray(var16));
            } else {
               if (var2 != NodeIdEncoding.Guid) {
                  throw new DecodingException("Unsupported NodeId Encoding byte " + var2);
               }

               var4 = this.th.getShort() & '\uffff';
               UUID var17 = this.getGuid(null);
               var3 = new NodeId(var4, var17);
            }

            return var3;
         }
      } catch (IOException var6) {
         throw a(var6);
      }
   }

   private QualifiedName esf() throws DecodingException {
      UnsignedShort var1 = this.getUInt16(null);
      String var2 = this.getString(null);
      return var2 == null ? new QualifiedName(var1, null) : new QualifiedName(var1, var2);
   }

   private Byte esg() throws DecodingException {
      try {
         return this.th.get();
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private Object o(int var1) throws DecodingException {
      switch (var1) {
         case 1:
            return this.erO();
         case 2:
            return this.esg();
         case 3:
            return this.erP();
         case 4:
            return this.erZ();
         case 5:
            return this.esj();
         case 6:
            return this.esa();
         case 7:
            return this.esk();
         case 8:
            return this.esc();
         case 9:
            return this.esl();
         case 10:
            return this.erX();
         case 11:
            return this.erU();
         case 12:
            return this.esi();
         case 13:
            return this.erS();
         case 14:
            return this.erY();
         case 15:
            return this.erQ();
         case 16:
            return this.esn();
         case 17:
            return this.ese();
         case 18:
            return this.erV();
         case 19:
            return this.esh();
         case 20:
            return this.esf();
         case 21:
            return this.esd();
         case 22:
            return this.erW();
         case 23:
            return this.erR();
         case 24:
            return this.esm();
         case 25:
            return this.erT();
         default:
            throw new DecodingException("Cannot decode builtin type id " + var1);
      }
   }

   private StatusCode esh() throws DecodingException {
      return StatusCode.valueOf(this.esk());
   }

   private String esi() throws DecodingException {
      try {
         int var1 = this.th.getInt();
         if (var1 == -1) {
            return null;
         } else {
            this.m(var1);
            byte[] var2 = new byte[var1];
            this.th.get(var2);
            return new String(var2, StandardCharsets.UTF_8);
         }
      } catch (IOException var3) {
         throw a(var3);
      }
   }

   private Structure b(StructureSpecification var1) throws DecodingException {
      UnsignedInteger var2 = null;
      Structure.Builder var3 = var1.toInstanceBuilder();
      if (StructureType.OPTIONAL == var1.getStructureType()) {
         var2 = this.esk();
         logger.trace("EncodingMask: {}", var2);
      }

      if (StructureType.UNION != var1.getStructureType() && StructureType.UNION_SUBTYPES != var1.getStructureType()) {
         int var11 = -1;

         for (FieldSpecification var13 : var1.getFields()) {
            logger.trace("Field: {}", var13);
            if (var13.isOptional()) {
               if (a(var2, ++var11)) {
                  var3.set(var13, this.a(var13));
               }
            } else {
               var3.set(var13, this.a(var13));
            }
         }

         return var3.build();
      } else {
         var2 = this.esk();
         long var4 = var2.longValue();
         logger.trace("SwitchField: {}", var4);
         if (var4 < 0L) {
            throw new DecodingException("Union SwitchField must be >= 0");
         } else if (var4 == 0L) {
            return var3.build();
         } else {
            long var6 = 0L;

            for (FieldSpecification var9 : var1.getFields()) {
               if (var4 == ++var6) {
                  logger.trace("Decoded Union Field: {}, SwitchValue: {}", var9, var6);
                  var3.set(var9, this.a(var9));
                  return var3.build();
               }
            }

            throw new DecodingException("Union SwitchField overflow: " + ++var6);
         }
      }
   }

   private Object a(FieldSpecification var1) throws DecodingException {
      Class var2 = var1.getJavaClass();
      int var3 = MultiDimensionArrayUtils.getClassDimensions(var2);
      Object var4;
      if (var1.isAllowSubTypes()) {
         Class var5 = MultiDimensionArrayUtils.getComponentType(var2);
         if (ExtensionObject.class.isAssignableFrom(var5)) {
            var4 = this.a(MultiDimensionArrayUtils.arrayClassOf(ExtensionObject.class, var3), UaIds.Structure, var3);
         } else {
            var4 = this.a(MultiDimensionArrayUtils.arrayClassOf(Variant.class, var3), UaIds.BaseDataType, var3);
         }
      } else {
         var4 = this.a(var2, var1.getDataTypeId(), var3);
      }

      Optional.ofNullable(this.tj).ifPresent(var2x -> var2x.accept(var1, var4));
      return var4;
   }

   private UnsignedShort esj() throws DecodingException {
      try {
         return UnsignedShort.getFromBits(this.th.getShort());
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private UnsignedInteger esk() throws DecodingException {
      try {
         return UnsignedInteger.getFromBits(this.th.getInt());
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private UnsignedLong esl() throws DecodingException {
      try {
         return UnsignedLong.getFromBits(this.th.getLong());
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   private Variant esm() throws DecodingException {
      try {
         byte var1 = this.th.get();
         int var2 = var1 & 63;
         boolean var3 = (var1 & 128) == 128;
         boolean var4 = (var1 & 64) == 64;
         boolean var5 = var2 == 0;
         if (var2 > 25) {
            var2 = 15;
         }

         Object var6 = var5 ? null : (var3 ? this.n(var2) : this.o(var2));
         int[] var7 = var4 ? this.getInt32Array_(null) : null;
         boolean var8 = var3 && var7 != null && var7.length > 1;
         if (var4) {
            long var9;
            if (var7 != null && var7.length != 0) {
               var9 = 1L;

               for (int var14 : var7) {
                  var9 *= var14;
               }
            } else if (var7 == null) {
               var9 = -1L;
            } else {
               var9 = 0L;
            }

            Object[] var22 = (Object[])var6;
            long var23 = var22 == null ? -1L : var22.length;
            if (var23 != var9) {
               throw new DecodingException(
                  "Variant's ArrayDimensions "
                     + Arrays.toString(var7)
                     + " total size of "
                     + var9
                     + " does not match defined ArrayLength of "
                     + var23
                     + ". Variant value was: "
                     + MultiDimensionArrayUtils.toString(var6, this.ti)
               );
            }
         }

         if (var6 instanceof ExtensionObject && this.a((ExtensionObject)var6)) {
            try {
               var6 = this.d((ByteString)((ExtensionObject)var6).getObject());
            } catch (ClassCastException var18) {
               throw new DecodingException("Did not get an ExtensionObject with ByteString data for Decimal type", var18);
            }
         } else if (var6 instanceof ExtensionObject) {
            ExtensionObject var20 = (ExtensionObject)var6;

            try {
               var6 = var20.decode(this.ti);
            } catch (DecodingException var17) {
               var6 = var20;
            }
         }

         if (var3) {
            if (var6 instanceof ExtensionObject[]) {
               var6 = this.b((ExtensionObject[])var6);
            }

            if (var6 instanceof ExtensionObject[]) {
               ExtensionObject[] var21 = (ExtensionObject[])var6;

               try {
                  var6 = this.ti.decode(var21);
               } catch (Exception var16) {
                  var6 = var21;
               }
            }

            if (var8) {
               try {
                  var6 = MultiDimensionArrayUtils.demuxArray(var6, var7);
               } catch (IllegalArgumentException var15) {
                  throw new DecodingException("The length of ArrayDimensions-field does not match Value-field");
               }
            }
         }

         return new Variant(var6);
      } catch (IOException var19) {
         throw a(var19);
      }
   }

   private XmlElement esn() throws DecodingException {
      ByteString var1 = this.erQ();
      return var1 == null ? null : new XmlElement(ByteString.asByteArray(var1));
   }

   private boolean a(ExtensionObject var1) {
      return var1 == null ? false : this.ti.getNamespaceTable().nodeIdEquals(Identifiers.Decimal, var1.getTypeId());
   }

   private Object b(ExtensionObject[] var1) {
      BigDecimal[] var2 = new BigDecimal[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         ExtensionObject var4 = var1[var3];
         if (!this.a(var4)) {
            return var1;
         }

         var2[var3] = this.d((ByteString)var4.getObject());
      }

      return var2;
   }

   protected long remaining() throws DecodingException {
      try {
         return this.th.limit() - this.th.position();
      } catch (IOException var2) {
         throw a(var2);
      }
   }

   @Deprecated
   <T> T a(String var1, Class<T> var2) throws DecodingException {
      int var3 = MultiDimensionArrayUtils.getClassDimensions(var2);
      return this.a(var2, null, var3);
   }

   static {
      b(UaIds.Boolean, Boolean.class, (var0, var1, var2, var3) -> var0.erO());
      b(UaIds.SByte, Byte.class, (var0, var1, var2, var3) -> var0.esg());
      b(UaIds.Byte, UnsignedByte.class, (var0, var1, var2, var3) -> var0.erP());
      b(UaIds.Int16, Short.class, (var0, var1, var2, var3) -> var0.erZ());
      b(UaIds.UInt16, UnsignedShort.class, (var0, var1, var2, var3) -> var0.esj());
      b(UaIds.Int32, Integer.class, (var0, var1, var2, var3) -> var0.esa());
      b(UaIds.UInt32, UnsignedInteger.class, (var0, var1, var2, var3) -> var0.esk());
      b(UaIds.Int64, Long.class, (var0, var1, var2, var3) -> var0.esc());
      b(UaIds.UInt64, UnsignedLong.class, (var0, var1, var2, var3) -> var0.esl());
      b(UaIds.Float, Float.class, (var0, var1, var2, var3) -> var0.erX());
      b(UaIds.Double, Double.class, (var0, var1, var2, var3) -> var0.erU());
      b(UaIds.String, String.class, (var0, var1, var2, var3) -> var0.esi());
      b(UaIds.Guid, UUID.class, (var0, var1, var2, var3) -> var0.erY());
      b(UaIds.ByteString, ByteString.class, (var0, var1, var2, var3) -> var0.erQ());
      b(UaIds.XmlElement, XmlElement.class, (var0, var1, var2, var3) -> var0.esn());
      b(UaIds.NodeId, NodeId.class, (var0, var1, var2, var3) -> var0.ese());
      b(UaIds.ExpandedNodeId, ExpandedNodeId.class, (var0, var1, var2, var3) -> var0.erV());
      b(UaIds.StatusCode, StatusCode.class, (var0, var1, var2, var3) -> var0.esh());
      b(UaIds.QualifiedName, QualifiedName.class, (var0, var1, var2, var3) -> var0.esf());
      b(UaIds.LocalizedText, LocalizedText.class, (var0, var1, var2, var3) -> var0.esd());
      a(UaIds.DateTime, DateTime.class, sX);
      a(UaIds.Structure, ExtensionObject.class, sY);
      a(UaIds.DataValue, DataValue.class, ta);
      a(UaIds.DiagnosticInfo, DiagnosticInfo.class, tc);
      a(UaIds.Decimal, BigDecimal.class, te);
      b(null, Object.class, (var0, var1, var2, var3) -> tb.get(var0, var1, Variant.class, var3).getValue());
   }

   private interface a<T> {
      T get(BinaryDecoder var1, String var2, Class<? extends T> var3, UaNodeId var4) throws DecodingException;
   }
}
