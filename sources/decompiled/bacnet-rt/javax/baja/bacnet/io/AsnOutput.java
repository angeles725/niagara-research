package javax.baja.bacnet.io;

import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BTime;

public interface AsnOutput {
   void writeNull();

   void writeNull(int var1);

   void writeBoolean(boolean var1);

   void writeBoolean(int var1, boolean var2);

   void writeBoolean(BBoolean var1);

   void writeBoolean(int var1, BBoolean var2);

   void writeUnsignedInteger(long var1);

   void writeUnsignedInteger(int var1, long var2);

   void writeUnsigned(BBacnetUnsigned var1);

   void writeUnsigned(int var1, BBacnetUnsigned var2);

   void writeSignedInteger(int var1);

   void writeSignedInteger(int var1, int var2);

   void writeSignedInteger(BInteger var1);

   void writeSignedInteger(int var1, BInteger var2);

   void writeReal(double var1);

   void writeReal(int var1, double var2);

   void writeReal(BNumber var1);

   void writeReal(int var1, BNumber var2);

   void writeDouble(double var1);

   void writeDouble(int var1, double var2);

   void writeDouble(BNumber var1);

   void writeDouble(int var1, BNumber var2);

   void writeOctetString(byte[] var1);

   void writeOctetString(int var1, byte[] var2);

   void writeOctetString(BBacnetOctetString var1);

   void writeOctetString(int var1, BBacnetOctetString var2);

   void writeCharacterString(String var1);

   void writeCharacterString(String var1, BCharacterSetEncoding var2);

   void writeCharacterString(int var1, String var2);

   void writeCharacterString(int var1, String var2, BCharacterSetEncoding var3);

   void writeCharacterString(BString var1);

   void writeCharacterString(BString var1, BCharacterSetEncoding var2);

   void writeCharacterString(int var1, BString var2);

   void writeCharacterString(int var1, BString var2, BCharacterSetEncoding var3);

   void writeBitString(boolean[] var1);

   void writeBitString(int var1, boolean[] var2);

   void writeBitString(BBacnetBitString var1);

   void writeBitString(int var1, BBacnetBitString var2);

   void writeEnumerated(int var1);

   void writeEnumerated(int var1, int var2);

   void writeEnumerated(BEnum var1);

   void writeEnumerated(int var1, BEnum var2);

   void writeDate(int var1, int var2, int var3, int var4);

   void writeDate(int var1, int var2, int var3, int var4, int var5);

   void writeDate(BBacnetDate var1);

   void writeDate(int var1, BBacnetDate var2);

   void writeDate(BAbsTime var1);

   void writeDate(int var1, BAbsTime var2);

   void writeTime(int var1, int var2, int var3, int var4);

   void writeTime(int var1, int var2, int var3, int var4, int var5);

   void writeTime(BBacnetTime var1);

   void writeTime(int var1, BBacnetTime var2);

   void writeTime(BTime var1);

   void writeTime(int var1, BTime var2);

   void writeTime(BAbsTime var1);

   void writeTime(int var1, BAbsTime var2);

   void writeObjectIdentifier(int var1, int var2);

   void writeObjectIdentifier(int var1, int var2, int var3);

   void writeObjectIdentifier(BBacnetObjectIdentifier var1);

   void writeObjectIdentifier(int var1, BBacnetObjectIdentifier var2);

   void writeEncodedValue(byte[] var1);

   void writeEncodedValue(int var1, byte[] var2);

   void writeOpeningTag(int var1);

   void writeClosingTag(int var1);

   void write(byte[] var1);
}
