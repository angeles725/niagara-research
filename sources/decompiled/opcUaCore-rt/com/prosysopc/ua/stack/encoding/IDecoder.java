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
import java.util.UUID;

public interface IDecoder {
   <T> T get(String var1, UaNodeId var2, int var3) throws DecodingException;

   default Boolean getBoolean(String var1) throws DecodingException {
      return this.get(var1, UaIds.Boolean, 0);
   }

   default Boolean[] getBooleanArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.Boolean, 1);
   }

   default UnsignedByte getByte(String var1) throws DecodingException {
      return this.get(var1, UaIds.Byte, 0);
   }

   default UnsignedByte[] getByteArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.Byte, 1);
   }

   default ByteString getByteString(String var1) throws DecodingException {
      return this.get(var1, UaIds.ByteString, 0);
   }

   default ByteString[] getByteStringArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.ByteString, 1);
   }

   default DataValue getDataValue(String var1) throws DecodingException {
      return this.get(var1, UaIds.DataValue, 0);
   }

   default DataValue[] getDataValueArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.DataValue, 1);
   }

   default DateTime getDateTime(String var1) throws DecodingException {
      return this.get(var1, UaIds.DateTime, 0);
   }

   default DateTime[] getDateTimeArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.DateTime, 1);
   }

   default DiagnosticInfo getDiagnosticInfo(String var1) throws DecodingException {
      return this.get(var1, UaIds.DiagnosticInfo, 0);
   }

   default DiagnosticInfo[] getDiagnosticInfoArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.DiagnosticInfo, 1);
   }

   default Double getDouble(String var1) throws DecodingException {
      return this.get(var1, UaIds.Double, 0);
   }

   default Double[] getDoubleArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.Double, 1);
   }

   EncoderContext getEncoderContext();

   default ExpandedNodeId getExpandedNodeId(String var1) throws DecodingException {
      return this.get(var1, UaIds.ExpandedNodeId, 0);
   }

   default ExpandedNodeId[] getExpandedNodeIdArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.ExpandedNodeId, 1);
   }

   default ExtensionObject getExtensionObject(String var1) throws DecodingException {
      return this.get(var1, UaIds.Structure, 0);
   }

   default ExtensionObject[] getExtensionObjectArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.Structure, 1);
   }

   default Float getFloat(String var1) throws DecodingException {
      return this.get(var1, UaIds.Float, 0);
   }

   default Float[] getFloatArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.Float, 1);
   }

   default UUID getGuid(String var1) throws DecodingException {
      return this.get(var1, UaIds.Guid, 0);
   }

   default UUID[] getGuidArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.Guid, 1);
   }

   default Short getInt16(String var1) throws DecodingException {
      return this.get(var1, UaIds.Int16, 0);
   }

   default Short[] getInt16Array(String var1) throws DecodingException {
      return this.get(var1, UaIds.Int16, 1);
   }

   default Integer getInt32(String var1) throws DecodingException {
      return this.get(var1, UaIds.Int32, 0);
   }

   default Integer[] getInt32Array(String var1) throws DecodingException {
      return this.get(var1, UaIds.Int32, 1);
   }

   int[] getInt32Array_(String var1) throws DecodingException;

   default Long getInt64(String var1) throws DecodingException {
      return this.get(var1, UaIds.Int64, 0);
   }

   default Long[] getInt64Array(String var1) throws DecodingException {
      return this.get(var1, UaIds.Int64, 1);
   }

   default LocalizedText getLocalizedText(String var1) throws DecodingException {
      return this.get(var1, UaIds.LocalizedText, 0);
   }

   default LocalizedText[] getLocalizedTextArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.LocalizedText, 1);
   }

   default NodeId getNodeId(String var1) throws DecodingException {
      return this.get(var1, UaIds.NodeId, 0);
   }

   default NodeId[] getNodeIdArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.NodeId, 1);
   }

   default QualifiedName getQualifiedName(String var1) throws DecodingException {
      return this.get(var1, UaIds.QualifiedName, 0);
   }

   default QualifiedName[] getQualifiedNameArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.QualifiedName, 1);
   }

   default Byte getSByte(String var1) throws DecodingException {
      return this.get(var1, UaIds.SByte, 0);
   }

   default Byte[] getSByteArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.SByte, 1);
   }

   default StatusCode getStatusCode(String var1) throws DecodingException {
      return this.get(var1, UaIds.StatusCode, 0);
   }

   default StatusCode[] getStatusCodeArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.StatusCode, 1);
   }

   default String getString(String var1) throws DecodingException {
      return this.get(var1, UaIds.String, 0);
   }

   default String[] getStringArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.String, 1);
   }

   default UnsignedShort getUInt16(String var1) throws DecodingException {
      return this.get(var1, UaIds.UInt16, 0);
   }

   default UnsignedShort[] getUInt16Array(String var1) throws DecodingException {
      return this.get(var1, UaIds.UInt16, 1);
   }

   default UnsignedInteger getUInt32(String var1) throws DecodingException {
      return this.get(var1, UaIds.UInt32, 0);
   }

   default UnsignedInteger[] getUInt32Array(String var1) throws DecodingException {
      return this.get(var1, UaIds.UInt32, 1);
   }

   default UnsignedLong getUInt64(String var1) throws DecodingException {
      return this.get(var1, UaIds.UInt64, 0);
   }

   default UnsignedLong[] getUInt64Array(String var1) throws DecodingException {
      return this.get(var1, UaIds.UInt64, 1);
   }

   default Variant getVariant(String var1) throws DecodingException {
      return new Variant(this.get(var1, UaIds.BaseDataType, 0));
   }

   default Variant[] getVariantArray(String var1) throws DecodingException {
      return Variant.asVariantArray(this.get(var1, UaIds.BaseDataType, 1));
   }

   default XmlElement getXmlElement(String var1) throws DecodingException {
      return this.get(var1, UaIds.XmlElement, 0);
   }

   default XmlElement[] getXmlElementArray(String var1) throws DecodingException {
      return this.get(var1, UaIds.XmlElement, 1);
   }
}
