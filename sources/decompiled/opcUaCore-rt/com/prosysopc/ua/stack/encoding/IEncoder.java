package com.prosysopc.ua.stack.encoding;

import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.ExtensionObject;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedByte;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.UnsignedLong;
import com.prosysopc.ua.stack.builtintypes.UnsignedShort;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.builtintypes.XmlElement;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public interface IEncoder {
   EncoderContext getEncoderContext();

   List<Locale> getLocales();

   void put(String var1, Object var2, UaNodeId var3, int var4) throws EncodingException;

   default void putBoolean(String var1, Boolean var2) throws EncodingException {
      this.put(var1, var2, UaIds.Boolean, 0);
   }

   default void putBooleanArray(String var1, Boolean[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Boolean, 1);
   }

   default void putBooleanArray(String var1, Collection<Boolean> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Boolean, 1);
      } else {
         this.put(var1, var2.toArray(new Boolean[0]), UaIds.Boolean, 1);
      }
   }

   default void putByte(String var1, UnsignedByte var2) throws EncodingException {
      this.put(var1, var2, UaIds.Byte, 0);
   }

   default void putByteArray(String var1, Collection<UnsignedByte> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Byte, 1);
      } else {
         this.putByteArray(var1, var2.toArray(UnsignedByte.EMPTY_ARRAY));
      }
   }

   default void putByteArray(String var1, UnsignedByte[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Byte, 1);
   }

   default void putByteString(String var1, ByteString var2) throws EncodingException {
      this.put(var1, var2, UaIds.ByteString, 0);
   }

   default void putByteStringArray(String var1, ByteString[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.ByteString, 1);
   }

   default void putByteStringArray(String var1, Collection<ByteString> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.ByteString, 1);
      } else {
         this.put(var1, var2.toArray(ByteString.EMPTY_ARRAY), UaIds.ByteString, 1);
      }
   }

   default void putDataValue(String var1, DataValue var2) throws EncodingException {
      this.put(var1, var2, UaIds.DataValue, 0);
   }

   default void putDataValueArray(String var1, Collection<DataValue> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.DataValue, 1);
      } else {
         this.put(var1, var2.toArray(DataValue.EMPTY_ARRAY), UaIds.DataValue, 1);
      }
   }

   default void putDataValueArray(String var1, DataValue[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.DataValue, 1);
   }

   default void putDateTime(String var1, DateTime var2) throws EncodingException {
      this.put(var1, var2, UaIds.DateTime, 0);
   }

   default void putDateTimeArray(String var1, Collection<DateTime> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.DateTime, 1);
      } else {
         this.put(var1, var2.toArray(DateTime.EMPTY_ARRAY), UaIds.DateTime, 1);
      }
   }

   default void putDateTimeArray(String var1, DateTime[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.DateTime, 1);
   }

   default void putDiagnosticInfo(String var1, DiagnosticInfo var2) throws EncodingException {
      this.put(var1, var2, UaIds.DiagnosticInfo, 0);
   }

   default void putDiagnosticInfoArray(String var1, Collection<DiagnosticInfo> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.DiagnosticInfo, 1);
      } else {
         this.put(var1, var2.toArray(DiagnosticInfo.EMPTY_ARRAY), UaIds.DiagnosticInfo, 1);
      }
   }

   default void putDiagnosticInfoArray(String var1, DiagnosticInfo[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.DiagnosticInfo, 1);
   }

   default void putDouble(String var1, double var2) throws EncodingException {
      this.put(var1, var2, UaIds.Double, 0);
   }

   default void putDouble(String var1, Double var2) throws EncodingException {
      this.put(var1, var2, UaIds.Double, 0);
   }

   default void putDoubleArray(String var1, Collection<Double> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Double, 1);
      } else {
         this.put(var1, var2.toArray(new Double[0]), UaIds.Double, 1);
      }
   }

   default void putDoubleArray(String var1, Double[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Double, 1);
   }

   default void putExpandedNodeId(String var1, ExpandedNodeId var2) throws EncodingException {
      this.put(var1, var2, UaIds.ExpandedNodeId, 0);
   }

   default void putExpandedNodeIdArray(String var1, Collection<ExpandedNodeId> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.ExpandedNodeId, 1);
      } else {
         this.put(var1, var2.toArray(ExpandedNodeId.EMPTY_ARRAY), UaIds.ExpandedNodeId, 1);
      }
   }

   default void putExpandedNodeIdArray(String var1, ExpandedNodeId[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.ExpandedNodeId, 1);
   }

   default void putExtensionObject(String var1, ExtensionObject var2) throws EncodingException {
      this.put(var1, var2, UaIds.Structure, 0);
   }

   default void putExtensionObjectArray(String var1, Collection<ExtensionObject> var2) throws EncodingException {
      this.put(var1, var2 == null ? null : var2.toArray(ExtensionObject.EMPTY_ARRAY), UaIds.Structure, 1);
   }

   default void putExtensionObjectArray(String var1, ExtensionObject[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Structure, 1);
   }

   default void putFloat(String var1, float var2) throws EncodingException {
      this.put(var1, var2, UaIds.Float, 0);
   }

   default void putFloat(String var1, Float var2) throws EncodingException {
      this.put(var1, var2, UaIds.Float, 0);
   }

   default void putFloatArray(String var1, Collection<Float> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Float, 1);
      } else {
         this.put(var1, var2.toArray(new Float[0]), UaIds.Float, 1);
      }
   }

   default void putFloatArray(String var1, Float[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Float, 1);
   }

   default void putGuid(String var1, UUID var2) throws EncodingException {
      this.put(var1, var2, UaIds.Guid, 0);
   }

   default void putGuidArray(String var1, Collection<UUID> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Guid, 1);
      } else {
         this.put(var1, var2.toArray(new UUID[0]), UaIds.Guid, 1);
      }
   }

   default void putGuidArray(String var1, UUID[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Guid, 1);
   }

   default void putInt16(String var1, short var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int16, 0);
   }

   default void putInt16(String var1, Short var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int16, 0);
   }

   default void putInt16Array(String var1, Collection<Short> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Int16, 1);
      } else {
         this.put(var1, var2.toArray(new Short[0]), UaIds.Int16, 1);
      }
   }

   default void putInt16Array(String var1, Short[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int16, 1);
   }

   default void putInt32(String var1, int var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int32, 0);
   }

   default void putInt32(String var1, Integer var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int32, 0);
   }

   default void putInt32Array(String var1, Collection<Integer> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Int32, 1);
      } else {
         this.put(var1, var2.toArray(new Integer[0]), UaIds.Int32, 1);
      }
   }

   default void putInt32Array(String var1, int[] var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Int32, 1);
      } else {
         Integer[] var3 = new Integer[var2.length];

         for (int var4 = 0; var4 < var2.length; var4++) {
            var3[var4] = var2[var4];
         }

         this.put(var1, var3, UaIds.Int32, 1);
      }
   }

   default void putInt32Array(String var1, Integer[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int32, 1);
   }

   default void putInt64(String var1, long var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int64, 0);
   }

   default void putInt64(String var1, Long var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int64, 0);
   }

   default void putInt64Array(String var1, Collection<Long> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.Int64, 1);
      } else {
         this.put(var1, var2.toArray(new Long[0]), UaIds.Int64, 1);
      }
   }

   default void putInt64Array(String var1, Long[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.Int64, 1);
   }

   default void putLocalizedText(String var1, LocalizedText var2) throws EncodingException {
      this.put(var1, var2, UaIds.LocalizedText, 0);
   }

   default void putLocalizedTextArray(String var1, Collection<LocalizedText> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.LocalizedText, 1);
      } else {
         this.put(var1, var2.toArray(LocalizedText.EMPTY_ARRAY), UaIds.LocalizedText, 1);
      }
   }

   default void putLocalizedTextArray(String var1, LocalizedText[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.LocalizedText, 1);
   }

   default void putNodeId(String var1, NodeId var2) throws EncodingException {
      this.put(var1, var2, UaIds.NodeId, 0);
   }

   default void putNodeIdArray(String var1, Collection<NodeId> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.NodeId, 1);
      } else {
         this.put(var1, var2.toArray(NodeId.EMPTY_ARRAY), UaIds.NodeId, 1);
      }
   }

   default void putNodeIdArray(String var1, NodeId[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.NodeId, 1);
   }

   default void putQualifiedName(String var1, QualifiedName var2) throws EncodingException {
      this.put(var1, var2, UaIds.QualifiedName, 0);
   }

   default void putQualifiedNameArray(String var1, Collection<QualifiedName> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.QualifiedName, 1);
      } else {
         this.put(var1, var2.toArray(QualifiedName.EMPTY_ARRAY), UaIds.QualifiedName, 1);
      }
   }

   default void putQualifiedNameArray(String var1, QualifiedName[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.QualifiedName, 1);
   }

   default void putSByte(String var1, byte var2) throws EncodingException {
      this.put(var1, var2, UaIds.SByte, 0);
   }

   default void putSByte(String var1, Byte var2) throws EncodingException {
      this.put(var1, var2, UaIds.SByte, 0);
   }

   default void putSByte(String var1, int var2) throws EncodingException {
      this.put(var1, (byte)var2, UaIds.SByte, 0);
   }

   default void putSByteArray(String var1, Byte[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.SByte, 1);
   }

   default void putSByteArray(String var1, Collection<Byte> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.SByte, 1);
      } else {
         this.put(var1, var2.toArray(new Byte[0]), UaIds.SByte, 1);
      }
   }

   default void putStatusCode(String var1, StatusCode var2) throws EncodingException {
      this.put(var1, var2, UaIds.StatusCode, 0);
   }

   default void putStatusCodeArray(String var1, Collection<StatusCode> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.StatusCode, 1);
      } else {
         this.put(var1, var2.toArray(StatusCode.EMPTY_ARRAY), UaIds.StatusCode, 1);
      }
   }

   default void putStatusCodeArray(String var1, StatusCode[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.StatusCode, 1);
   }

   default void putString(String var1, String var2) throws EncodingException {
      this.put(var1, var2, UaIds.String, 0);
   }

   default void putStringArray(String var1, Collection<String> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.String, 1);
      } else {
         this.put(var1, var2.toArray(new String[0]), UaIds.String, 1);
      }
   }

   default void putStringArray(String var1, String[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.String, 1);
   }

   default void putUInt16(String var1, UnsignedShort var2) throws EncodingException {
      this.put(var1, var2, UaIds.UInt16, 0);
   }

   default void putUInt16Array(String var1, Collection<UnsignedShort> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.UInt16, 1);
      } else {
         this.put(var1, var2.toArray(UnsignedShort.EMPTY_ARRAY), UaIds.UInt16, 1);
      }
   }

   default void putUInt16Array(String var1, UnsignedShort[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.UInt16, 1);
   }

   default void putUInt32(String var1, UnsignedInteger var2) throws EncodingException {
      this.put(var1, var2, UaIds.UInt32, 0);
   }

   default void putUInt32Array(String var1, Collection<UnsignedInteger> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.UInt32, 1);
      } else {
         this.putUInt32Array(var1, var2.toArray(UnsignedInteger.EMPTY_ARRAY));
      }
   }

   default void putUInt32Array(String var1, UnsignedInteger[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.UInt32, 1);
   }

   default void putUInt64(String var1, UnsignedLong var2) throws EncodingException {
      this.put(var1, var2, UaIds.UInt64, 0);
   }

   default void putUInt64Array(String var1, Collection<UnsignedLong> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.UInt64, 1);
      } else {
         this.putUInt64Array(var1, var2.toArray(UnsignedLong.EMPTY_ARRAY));
      }
   }

   default void putUInt64Array(String var1, UnsignedLong[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.UInt64, 1);
   }

   default void putVariant(String var1, Variant var2) throws EncodingException {
      this.put(var1, var2, UaIds.BaseDataType, 0);
   }

   default void putVariantArray(String var1, Variant[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.BaseDataType, 1);
   }

   default void putXmlElement(String var1, XmlElement var2) throws EncodingException {
      this.put(var1, var2, UaIds.XmlElement, 0);
   }

   default void putXmlElementArray(String var1, Collection<XmlElement> var2) throws EncodingException {
      if (var2 == null) {
         this.put(var1, null, UaIds.XmlElement, 1);
      } else {
         this.put(var1, var2.toArray(XmlElement.EMPTY_ARRAY), UaIds.XmlElement, 1);
      }
   }

   default void putXmlElementArray(String var1, XmlElement[] var2) throws EncodingException {
      this.put(var1, var2, UaIds.XmlElement, 1);
   }
}
