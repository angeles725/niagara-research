package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.common.NamespaceTable;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.encoding.DecodingException;
import com.prosysopc.ua.stack.encoding.EncodeType;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.IEncodeable;
import com.prosysopc.ua.stack.encoding.binary.BinaryDecoder;
import com.prosysopc.ua.stack.encoding.binary.BinaryEncoder;
import com.prosysopc.ua.stack.encoding.json.JsonDecoder;
import com.prosysopc.ua.stack.encoding.json.JsonEncoder;
import com.prosysopc.ua.stack.encoding.xml.XmlDecoder;
import com.prosysopc.ua.stack.encoding.xml.XmlEncoder;
import com.prosysopc.ua.stack.utils.LimitedByteArrayOutputStream;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionObject implements Cloneable {
   public static final ExtensionObject[] EMPTY_ARRAY = new ExtensionObject[0];
   private static Logger logger = LoggerFactory.getLogger(ExtensionObject.class);
   private Object object;
   private final ExpandedNodeId typeId;
   private final EncodeType encodeType;

   public static ExtensionObject binaryEncode(Structure var0, EncoderContext var1) throws EncodingException {
      return binaryEncodeImpl(var0, var1, null);
   }

   public static ExtensionObject binaryEncode(Structure var0, EncoderContext var1, List<Locale> var2) throws EncodingException {
      return binaryEncodeImpl(var0, var1, var2);
   }

   public static ExtensionObject encode(Structure var0, QualifiedName var1, EncoderContext var2) throws EncodingException {
      if (var0 == null) {
         return null;
      } else if (var1.equals(QualifiedName.DEFAULT_BINARY_ENCODING)) {
         return binaryEncode(var0, var2);
      } else if (var1.equals(QualifiedName.DEFAULT_XML_ENCODING)) {
         return xmlEncode(var0, var2);
      } else {
         throw new EncodingException(StatusCodes.Bad_DataEncodingUnsupported);
      }
   }

   public static ExtensionObject jsonEncode(Structure var0, EncoderContext var1) throws EncodingException {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      OutputStreamWriter var3 = new OutputStreamWriter(var2, StandardCharsets.UTF_8);
      JsonEncoder var4 = new JsonEncoder(var3);
      var4.setEncoderContext(var1);
      var4.put(null, var0, var0.specification().getTypeId(), 0);
      var4.close();
      String var5 = new String(var2.toByteArray(), StandardCharsets.UTF_8);
      return new ExtensionObject(var0.specification().getJsonEncodeId().asExpandedNodeId(), var5);
   }

   public static ExtensionObject xmlEncode(Structure var0, EncoderContext var1) throws EncodingException {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      OutputStreamWriter var3 = new OutputStreamWriter(var2, StandardCharsets.UTF_8);
      XmlEncoder var4 = new XmlEncoder();
      var4.setEncoderContext(var1);
      var4.setOmitXmlDeclaration(true);
      var4.put(var0.specification().getName(), var0, var0.specification().getTypeId(), 0);
      var4.write(var3);
      String var5 = new String(var2.toByteArray(), StandardCharsets.UTF_8);
      return new ExtensionObject(var0.specification().getXmlEncodeId().asExpandedNodeId(), new XmlElement(var5));
   }

   private static ExtensionObject binaryEncodeImpl(Structure var0, EncoderContext var1, List<Locale> var2) throws EncodingException {
      if (var2 == null) {
         var2 = Collections.emptyList();
      }

      int var3 = var1.getMaxByteStringLength();
      if (var3 == 0) {
         var3 = var1.getMaxMessageSize();
      }

      if (var3 == 0) {
         var3 = Integer.MAX_VALUE;
      }

      LimitedByteArrayOutputStream var4 = LimitedByteArrayOutputStream.withSizeLimit(var3);
      BinaryEncoder var5 = new BinaryEncoder(var4);
      var5.getLocales().addAll(var2);
      var5.setEncoderContext(var1);
      var5.put(null, var0, var0.specification().getTypeId(), 0);
      return new ExtensionObject(var0.specification().getBinaryEncodeId().asExpandedNodeId(), ByteString.valueOf(var4.toByteArray()));
   }

   public ExtensionObject(ExpandedNodeId var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("typeId argument must not be null");
      } else {
         this.typeId = var1;
         this.object = null;
         this.encodeType = null;
      }
   }

   @Deprecated
   public ExtensionObject(ExpandedNodeId var1, byte[] var2) {
      this(var1, ByteString.valueOf(var2));
   }

   public ExtensionObject(ExpandedNodeId var1, ByteString var2) {
      if (var1 == null) {
         throw new IllegalArgumentException("typeId argument must not be null");
      } else {
         this.typeId = var1;
         if (var2 != null) {
            this.object = var2;
            this.encodeType = EncodeType.Binary;
         } else {
            this.object = null;
            this.encodeType = null;
         }
      }
   }

   public ExtensionObject(ExpandedNodeId var1, String var2) {
      if (var1 == null) {
         throw new IllegalArgumentException("typeId argument must not be null");
      } else {
         this.typeId = var1;
         if (var2 != null) {
            this.object = var2;
            this.encodeType = EncodeType.Json;
         } else {
            this.object = null;
            this.encodeType = null;
         }
      }
   }

   public ExtensionObject(ExpandedNodeId var1, XmlElement var2) {
      if (var1 == null) {
         throw new IllegalArgumentException("typeId argument must not be null");
      } else {
         if (var2 == null) {
            this.object = new XmlElement("");
         } else {
            this.object = var2;
         }

         this.typeId = var1;
         this.encodeType = EncodeType.Xml;
      }
   }

   public ExtensionObject(Structure var1) {
      this.encodeType = null;
      this.typeId = null;
      this.object = var1;
   }

   public ExtensionObject clone() {
      try {
         ExtensionObject var1 = (ExtensionObject)super.clone();
         var1.object = StructureUtils.clone(var1.object);
         return var1;
      } catch (CloneNotSupportedException var2) {
         logger.error("Got a CloneNotSupportedException, should be impossible", var2);
         throw new Error("Every ExtensionObject implementation shall be Cloneable", var2);
      }
   }

   public <T extends IEncodeable> T decode(EncoderContext var1) throws DecodingException {
      return this.decode(var1, var1.getNamespaceTable());
   }

   public <T extends IEncodeable> T decode(EncoderContext var1, Class<T> var2, UnsignedInteger var3) throws StatusException {
      try {
         return (T)var2.cast(this.decode(var1));
      } catch (Exception var5) {
         throw new StatusException(var3, var5);
      }
   }

   public <T extends IEncodeable> T decode(EncoderContext var1, NamespaceTable var2) throws DecodingException {
      if (this.object instanceof Structure) {
         return (T)this.object;
      } else {
         UaNodeId var3 = UaNodeId.fromLocal(this.typeId);
         StructureSpecification var4 = var1.getStructureSpecification(var3);
         if (var4 == null) {
            throw new DecodingException("Cannot decode, could not find StructureSpecification for " + var3);
         } else if (this.object == null) {
            return (T)var4.toInstanceBuilder().build();
         } else if (this.object instanceof Structure) {
            return (T)this.object;
         } else if (this.object instanceof XmlElement) {
            XmlDecoder var12 = new XmlDecoder((XmlElement)this.object, var1);
            var12.setNamespaceTable(var2);

            IEncodeable var6;
            try {
               boolean var7 = var12.peek(var4.getName());
               if (var7) {
                  var12.getStartElement();
               }

               var6 = var12.get(null, var4.getTypeId(), 0);
               if (var7) {
                  var12.getEndElement();
               }
            } finally {
               var12.close();
            }

            return (T)var6;
         } else if (this.object instanceof ByteString) {
            BinaryDecoder var11 = new BinaryDecoder(((ByteString)this.object).getValue());
            var11.setEncoderContext(var1);
            return var11.get(null, var4.getTypeId(), 0);
         } else if (this.object instanceof String) {
            JsonDecoder var5 = new JsonDecoder();
            var5.setReadable(new ByteArrayInputStream(((String)this.object).getBytes(StandardCharsets.UTF_8)));
            var5.setEncoderContext(var1);
            return var5.get(null, var4.getTypeId(), 0);
         } else {
            throw new DecodingException("unexpected data, not null, Structure, ByteString or XmlElement, was: " + this.object.getClass());
         }
      }
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return false;
      } else if (!(var1 instanceof ExtensionObject)) {
         return false;
      } else {
         ExtensionObject var2 = (ExtensionObject)var1;
         return !Objects.equals(this.typeId, var2.typeId) ? false : Objects.equals(this.object, var2.object);
      }
   }

   public EncodeType getEncodeType() {
      return this.encodeType;
   }

   public Object getObject() {
      return this.object;
   }

   public ExpandedNodeId getTypeId() {
      return this.typeId;
   }

   @Override
   public int hashCode() {
      return this.object == null ? 0 : this.object.hashCode();
   }

   public boolean isEncoded() {
      return this.object == null ? true : !(this.object instanceof Structure);
   }

   @Override
   public String toString() {
      return "ExtensionObject [typeId=" + this.typeId + ", encodeType=" + this.encodeType + ", object=" + this.object + "]";
   }
}
