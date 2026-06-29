package javax.baja.bacnet.io;

import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;

public interface AsnInput {
   int END_OF_DATA = -1;

   int available();

   int peekTag() throws AsnException;

   boolean isApplicationTag(int var1);

   boolean isContextTag(int var1);

   boolean isOpeningTag(int var1);

   void skipOpeningTag(int var1) throws AsnException;

   boolean isClosingTag(int var1);

   void skipClosingTag(int var1) throws AsnException;

   boolean isValueTag(int var1);

   int peekApplicationTag() throws AsnException;

   int getDataLength();

   int skipTag() throws AsnException;

   BBacnetNull readNull() throws AsnException;

   BBacnetNull readNull(int var1) throws AsnException;

   boolean readBoolean() throws AsnException;

   boolean readBoolean(int var1) throws AsnException;

   int readInteger() throws AsnException;

   long readUnsignedInteger() throws AsnException;

   long readUnsignedInteger(int var1) throws AsnException;

   int readUnsignedInt() throws AsnException;

   int readUnsignedInt(int var1) throws AsnException;

   BBacnetUnsigned readUnsigned() throws AsnException;

   BBacnetUnsigned readUnsigned(int var1) throws AsnException;

   int readSignedInteger() throws AsnException;

   int readSignedInteger(int var1) throws AsnException;

   BInteger readSigned() throws AsnException;

   BInteger readSigned(int var1) throws AsnException;

   float readReal() throws AsnException;

   float readReal(int var1) throws AsnException;

   BFloat readFloat() throws AsnException;

   BFloat readFloat(int var1) throws AsnException;

   double readDouble() throws AsnException;

   double readDouble(int var1) throws AsnException;

   byte[] readOctetString() throws AsnException;

   byte[] readOctetString(int var1) throws AsnException;

   BBacnetOctetString readBacnetOctetString() throws AsnException;

   BBacnetOctetString readBacnetOctetString(int var1) throws AsnException;

   String readCharacterString() throws AsnException;

   String readCharacterString(int var1) throws AsnException;

   BBacnetBitString readBitString() throws AsnException;

   BBacnetBitString readBitString(int var1) throws AsnException;

   int readEnumerated() throws AsnException;

   int readEnumerated(int var1) throws AsnException;

   BBacnetDate readDate() throws AsnException;

   BBacnetDate readDate(int var1) throws AsnException;

   BBacnetTime readTime() throws AsnException;

   BBacnetTime readTime(int var1) throws AsnException;

   BBacnetObjectIdentifier readObjectIdentifier() throws AsnException;

   BBacnetObjectIdentifier readObjectIdentifier(int var1) throws AsnException;

   byte[] readContextTaggedData() throws AsnException;

   byte[] readEncodedValue(int var1) throws AsnException;

   int read(byte[] var1);
}
