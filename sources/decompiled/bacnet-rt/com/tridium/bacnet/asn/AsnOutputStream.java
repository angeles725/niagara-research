package com.tridium.bacnet.asn;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BTime;

public class AsnOutputStream extends ByteArrayOutputStream implements AsnOutput, AsnConst {
   private static final int DEFAULT_BUFFER_CAPACITY = 16;
   private static final int STREAM_POOL_SIZE = 20;
   private boolean freeBuf = false;
   private static int cnt = 0;
   private static AsnOutputStream[] pool = new AsnOutputStream[20];
   private static Object poolLock = new Object();

   public AsnOutputStream() {
      super(16);
   }

   public AsnOutputStream(int size) {
      super(size);
   }

   @Override
   public void writeNull() {
      this.writePrimitiveApplicationTag(0, 0);
   }

   @Override
   public void writeNull(int contextTag) {
      this.writePrimitiveContextTag(contextTag, 0);
   }

   @Override
   public void writeBoolean(boolean value) {
      if (value) {
         this.writePrimitiveApplicationTag(1, 1);
      } else {
         this.writePrimitiveApplicationTag(1, 0);
      }
   }

   @Override
   public void writeBoolean(int contextTag, boolean value) {
      this.writePrimitiveContextTag(contextTag, 1);
      if (value) {
         this.write(1);
      } else {
         this.write(0);
      }
   }

   @Override
   public void writeBoolean(BBoolean value) {
      this.writeBoolean(value.getBoolean());
   }

   @Override
   public void writeBoolean(int contextTag, BBoolean value) {
      this.writeBoolean(contextTag, value.getBoolean());
   }

   @Override
   public void writeUnsignedInteger(long value) {
      if (value < 0L) {
         throw new IllegalArgumentException("Unsigned integer values must be >= 0.");
      } else {
         int dataLength = this.findIntegerLength(value);
         this.writePrimitiveApplicationTag(2, dataLength);
         this.writeIntegerData(value, dataLength);
      }
   }

   @Override
   public void writeUnsignedInteger(int contextTag, long value) {
      if (value < 0L) {
         throw new IllegalArgumentException("Unsigned integer values must be >= 0.");
      } else {
         int dataLength = this.findIntegerLength(value);
         this.writePrimitiveContextTag(contextTag, dataLength);
         this.writeIntegerData(value, dataLength);
      }
   }

   @Override
   public void writeUnsigned(BBacnetUnsigned value) {
      this.writeUnsignedInteger(value.getUnsigned());
   }

   @Override
   public void writeUnsigned(int contextTag, BBacnetUnsigned value) {
      this.writeUnsignedInteger(contextTag, value.getUnsigned());
   }

   @Override
   public void writeSignedInteger(int value) {
      int dataLength = this.findIntegerLength(value);
      if (value > 0) {
         int mask = 1 << 8 * dataLength - 1;
         if ((value & mask) != 0) {
            dataLength++;
         }
      }

      this.writePrimitiveApplicationTag(3, dataLength);
      this.writeIntegerData(value, dataLength);
   }

   @Override
   public void writeSignedInteger(int contextTag, int value) {
      int dataLength = this.findIntegerLength(value);
      if (value > 0) {
         int mask = 1 << 8 * dataLength - 1;
         if ((value & mask) != 0) {
            dataLength++;
         }
      }

      this.writePrimitiveContextTag(contextTag, dataLength);
      this.writeIntegerData(value, dataLength);
   }

   private int findIntegerLength(long value) {
      if (value == 0L) {
         return 1;
      } else if (value > 0L) {
         if (value > 4294967295L) {
            throw new IllegalArgumentException("Number too big for Asn conversion: " + Long.toHexString(value));
         } else if ((value & -16777216L) != 0L) {
            return 4;
         } else if ((value & 16711680L) != 0L) {
            return 3;
         } else {
            return (value & 65280L) != 0L ? 2 : 1;
         }
      } else if (value < -2147483648L) {
         throw new IllegalArgumentException("Number too big for Asn conversion: " + Long.toHexString(value));
      } else if ((value & -16777216L) != -16777216L) {
         return 4;
      } else if ((value & 8388608L) == 0L) {
         return 4;
      } else if ((value & 16711680L) != 16711680L) {
         return 3;
      } else if ((value & 32768L) == 0L) {
         return 3;
      } else if ((value & 65280L) != 65280L) {
         return 2;
      } else {
         return (value & 128L) == 0L ? 2 : 1;
      }
   }

   private void writeIntegerData(long value, int dataLength) {
      for (int i = 1; i <= dataLength; i++) {
         int bitShift = (dataLength - i) * 8;
         this.write((int)(value >> bitShift));
      }
   }

   @Override
   public void writeSignedInteger(BInteger value) {
      this.writeSignedInteger(value.getInt());
   }

   @Override
   public void writeSignedInteger(int contextTag, BInteger value) {
      this.writeSignedInteger(contextTag, value.getInt());
   }

   @Override
   public void writeReal(double value) {
      this.writePrimitiveApplicationTag(4, 4);
      this.encodeReal((float)value);
   }

   @Override
   public void writeReal(int contextTag, double value) {
      this.writePrimitiveContextTag(contextTag, 4);
      this.encodeReal((float)value);
   }

   private void encodeReal(float value) {
      int bitValue = Float.floatToIntBits(value);
      int bitShift = 24;

      for (int i = 0; i < 4; i++) {
         this.write(bitValue >> bitShift);
         bitShift -= 8;
      }
   }

   @Override
   public void writeReal(BNumber value) {
      this.writeReal(value.getFloat());
   }

   @Override
   public void writeReal(int contextTag, BNumber value) {
      this.writeReal(contextTag, value.getFloat());
   }

   @Override
   public void writeDouble(double value) {
      this.writePrimitiveApplicationTag(5, 8);
      this.encodeDouble(value);
   }

   @Override
   public void writeDouble(int contextTag, double value) {
      this.writePrimitiveContextTag(contextTag, 8);
      this.encodeDouble(value);
   }

   private void encodeDouble(double value) {
      long bitValue = Double.doubleToLongBits(value);
      long bitShift = 64L;

      for (int i = 0; i < 8; i++) {
         bitShift -= 8L;
         this.write((int)(bitValue >> (int)bitShift));
      }
   }

   @Override
   public void writeDouble(BNumber value) {
      this.writeDouble(value.getDouble());
   }

   @Override
   public void writeDouble(int contextTag, BNumber value) {
      this.writeDouble(contextTag, value.getDouble());
   }

   @Override
   public void writeOctetString(byte[] octetString) {
      this.writePrimitiveApplicationTag(6, octetString.length);
      this.writeOctetStringData(octetString);
   }

   @Override
   public void writeOctetString(int contextTag, byte[] octetString) {
      this.writePrimitiveContextTag(contextTag, octetString.length);
      this.writeOctetStringData(octetString);
   }

   @Override
   public void writeOctetString(BBacnetOctetString octetString) {
      this.writePrimitiveApplicationTag(6, octetString.length());
      this.writeOctetStringData(octetString.getBytes());
   }

   @Override
   public void writeOctetString(int contextTag, BBacnetOctetString octetString) {
      this.writePrimitiveContextTag(contextTag, octetString.length());
      this.writeOctetStringData(octetString.getBytes());
   }

   private void writeOctetStringData(byte[] octetString) {
      for (int i = 0; i < octetString.length; i++) {
         this.write(octetString[i]);
      }
   }

   @Override
   public void writeCharacterString(String value) {
      this.writeCharacterString(value, BBacnetNetwork.localDevice().getCharacterSet());
   }

   @Override
   public void writeCharacterString(String value, BCharacterSetEncoding encoding) {
      if (encoding == null) {
         encoding = BBacnetNetwork.localDevice().getCharacterSet();
      }

      try {
         byte[] charArray = value.getBytes(encoding.getEncodingName());
         this.writePrimitiveApplicationTag(7, charArray.length + 1);
         this.write(encoding.getOrdinal());
         this.write(charArray);
      } catch (UnsupportedEncodingException var4) {
         throw new IllegalArgumentException("Unsupported Encoding:" + encoding.getTag());
      }
   }

   @Override
   public void writeCharacterString(int contextTag, String value) {
      this.writeCharacterString(contextTag, value, BBacnetNetwork.localDevice().getCharacterSet());
   }

   @Override
   public void writeCharacterString(int contextTag, String value, BCharacterSetEncoding encoding) {
      if (encoding == null) {
         encoding = BBacnetNetwork.localDevice().getCharacterSet();
      }

      try {
         byte[] charArray = value.getBytes(encoding.getEncodingName());
         this.writePrimitiveContextTag(contextTag, charArray.length + 1);
         this.write(encoding.getOrdinal());
         this.write(charArray);
      } catch (UnsupportedEncodingException var5) {
         throw new IllegalArgumentException("Unsupported Encoding:" + encoding.getTag());
      }
   }

   @Override
   public void writeCharacterString(BString value) {
      this.writeCharacterString(value.getString());
   }

   @Override
   public void writeCharacterString(BString value, BCharacterSetEncoding encoding) {
      this.writeCharacterString(value.getString(), encoding);
   }

   @Override
   public void writeCharacterString(int contextTag, BString value) {
      this.writeCharacterString(contextTag, value.getString());
   }

   @Override
   public void writeCharacterString(int contextTag, BString value, BCharacterSetEncoding encoding) {
      this.writeCharacterString(contextTag, value.getString(), encoding);
   }

   @Override
   public void writeBitString(boolean[] bitString) {
      this.writePrimitiveApplicationTag(8, this.getBitStringDataLength(bitString.length));
      this.writeBitStringData(bitString);
   }

   @Override
   public void writeBitString(int contextTag, boolean[] bitString) {
      this.writePrimitiveContextTag(contextTag, this.getBitStringDataLength(bitString.length));
      this.writeBitStringData(bitString);
   }

   @Override
   public void writeBitString(BBacnetBitString bitString) {
      this.writeBitString(bitString.getBits());
   }

   @Override
   public void writeBitString(int contextTag, BBacnetBitString bitString) {
      this.writeBitString(contextTag, bitString.getBits());
   }

   private int getBitStringDataLength(int numBits) {
      int dataLength = numBits / 8 + 1;
      if (numBits % 8 != 0) {
         dataLength++;
      }

      return dataLength;
   }

   private void writeBitStringData(boolean[] bitString) {
      int len = bitString.length;
      int unusedBits = 8 - len % 8;
      int totalBytes = len / 8;
      if (unusedBits == 8) {
         unusedBits = 0;
      } else {
         totalBytes++;
      }

      this.write(unusedBits);
      int bit = 0;

      for (int i = 0; i < totalBytes; i++) {
         int byteValue = 0;
         int bitmask = 128;

         for (int j = 0; j < 8; j++) {
            if (bit < len && bitString[bit]) {
               byteValue |= bitmask;
            }

            bitmask >>= 1;
            bit++;
         }

         this.write(byteValue);
      }
   }

   public void writeStatusFlags(BStatus s) {
      this.writePrimitiveApplicationTag(8, this.getBitStringDataLength(4));
      this.writeBitStringData(new boolean[]{s.isAlarm(), s.isFault(), s.isOverridden(), s.isDisabled()});
   }

   public void writeStatusFlags(int contextTag, BStatus s) {
      this.writePrimitiveContextTag(contextTag, this.getBitStringDataLength(4));
      this.writeBitStringData(new boolean[]{s.isAlarm(), s.isFault(), s.isOverridden(), s.isDisabled()});
   }

   @Override
   public void writeEnumerated(int enumValue) {
      if (enumValue < 0) {
         throw new IllegalArgumentException("Enumerated values must be greater than zero.");
      } else {
         int dataLength = this.findIntegerLength(enumValue);
         this.writePrimitiveApplicationTag(9, dataLength);
         this.writeIntegerData(enumValue, dataLength);
      }
   }

   @Override
   public void writeEnumerated(int contextTag, int enumValue) {
      if (enumValue < 0) {
         throw new IllegalArgumentException("Enumerated values must be nonnegative.");
      } else {
         int dataLength = this.findIntegerLength(enumValue);
         this.writePrimitiveContextTag(contextTag, dataLength);
         this.writeIntegerData(enumValue, dataLength);
      }
   }

   @Override
   public void writeEnumerated(BEnum e) {
      this.writeEnumerated(e.getOrdinal());
   }

   @Override
   public void writeEnumerated(int contextTag, BEnum e) {
      this.writeEnumerated(contextTag, e.getOrdinal());
   }

   @Override
   public void writeDate(int year, int month, int day, int weekday) {
      this.writePrimitiveApplicationTag(10, 4);
      this.writeDateData(year, month, day, weekday);
   }

   @Override
   public void writeDate(int contextTag, int year, int month, int day, int weekday) {
      this.writePrimitiveContextTag(contextTag, 4);
      this.writeDateData(year, month, day, weekday);
   }

   @Override
   public void writeDate(BBacnetDate date) {
      this.writePrimitiveApplicationTag(10, 4);
      this.writeDateData(date.getRawYear(), date.getMonth(), date.getDayOfMonth(), date.getDayOfWeek());
   }

   @Override
   public void writeDate(int contextTag, BBacnetDate date) {
      this.writePrimitiveContextTag(contextTag, 4);
      this.writeDateData(date.getRawYear(), date.getMonth(), date.getDayOfMonth(), date.getDayOfWeek());
   }

   @Override
   public void writeDate(BAbsTime t) {
      this.writePrimitiveApplicationTag(10, 4);
      if (t.isNull()) {
         this.writeDateData(-1, -1, -1, -1);
      } else {
         this.writeDateData(t.getYear() - 1900, t.getMonth().getOrdinal() + 1, t.getDay(), t.getWeekday().getOrdinal() == 0 ? 7 : t.getWeekday().getOrdinal());
      }
   }

   @Override
   public void writeDate(int contextTag, BAbsTime t) {
      this.writePrimitiveContextTag(contextTag, 4);
      if (t.isNull()) {
         this.writeDateData(-1, -1, -1, -1);
      } else {
         this.writeDateData(t.getYear() - 1900, t.getMonth().getOrdinal() + 1, t.getDay(), t.getWeekday().getOrdinal() == 0 ? 7 : t.getWeekday().getOrdinal());
      }
   }

   private void writeDateData(int y, int m, int d, int w) {
      this.write(y);
      this.write(m);
      this.write(d);
      this.write(w);
   }

   @Override
   public void writeTime(int hour, int minute, int second, int hundredth) {
      this.writePrimitiveApplicationTag(11, 4);
      this.writeTimeData(hour, minute, second, hundredth);
   }

   @Override
   public void writeTime(int contextTag, int hour, int minute, int second, int hundredth) {
      this.writePrimitiveContextTag(contextTag, 4);
      this.writeTimeData(hour, minute, second, hundredth);
   }

   @Override
   public void writeTime(BBacnetTime time) {
      this.writePrimitiveApplicationTag(11, 4);
      this.writeTimeData(time.getHour(), time.getMinute(), time.getSecond(), time.getHundredth());
   }

   @Override
   public void writeTime(int contextTag, BBacnetTime time) {
      this.writePrimitiveContextTag(contextTag, 4);
      this.writeTimeData(time.getHour(), time.getMinute(), time.getSecond(), time.getHundredth());
   }

   @Override
   public void writeTime(BTime time) {
      this.writePrimitiveApplicationTag(11, 4);
      this.writeTimeData(time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond() / 10);
   }

   @Override
   public void writeTime(int contextTag, BTime time) {
      this.writePrimitiveContextTag(contextTag, 4);
      this.writeTimeData(time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond() / 10);
   }

   @Override
   public void writeTime(BAbsTime time) {
      this.writePrimitiveApplicationTag(11, 4);
      if (time.isNull()) {
         this.writeTimeData(-1, -1, -1, -1);
      } else {
         this.writeTimeData(time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond() / 10);
      }
   }

   @Override
   public void writeTime(int contextTag, BAbsTime time) {
      this.writePrimitiveContextTag(contextTag, 4);
      if (time.isNull()) {
         this.writeTimeData(-1, -1, -1, -1);
      } else {
         this.writeTimeData(time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond() / 10);
      }
   }

   private void writeTimeData(int h, int m, int s, int u) {
      this.write(h);
      this.write(m);
      this.write(s);
      this.write(u);
   }

   @Override
   public void writeObjectIdentifier(int objectType, int instanceNumber) {
      this.writePrimitiveApplicationTag(12, 4);
      int objectId = objectType << 22 & -4194304 | instanceNumber & 4194303;
      this.writeIntegerData(objectId, 4);
   }

   @Override
   public void writeObjectIdentifier(int contextTag, int objectType, int instanceNumber) {
      this.writePrimitiveContextTag(contextTag, 4);
      int objectId = objectType << 22 & -4194304 | instanceNumber & 4194303;
      this.writeIntegerData(objectId, 4);
   }

   @Override
   public void writeObjectIdentifier(BBacnetObjectIdentifier objectId) {
      this.writeObjectIdentifier(objectId.getObjectType(), objectId.getInstanceNumber());
   }

   @Override
   public void writeObjectIdentifier(int contextTag, BBacnetObjectIdentifier objectId) {
      this.writeObjectIdentifier(contextTag, objectId.getObjectType(), objectId.getInstanceNumber());
   }

   @Override
   public void writeEncodedValue(byte[] encodedValue) {
      if (encodedValue != null) {
         this.write(encodedValue, 0, encodedValue.length);
      }
   }

   @Override
   public void writeEncodedValue(int tagNumber, byte[] encodedValue) {
      this.writeOpeningTag(tagNumber);
      if (encodedValue != null) {
         this.write(encodedValue);
      }

      this.writeClosingTag(tagNumber);
   }

   @Override
   public void writeOpeningTag(int tagNumber) {
      this.writeTag(tagNumber, 8, 6);
   }

   @Override
   public void writeClosingTag(int tagNumber) {
      this.writeTag(tagNumber, 8, 7);
   }

   private void writePrimitiveApplicationTag(int tagNumber, int length) {
      this.writePrimitiveTag(tagNumber, 0, length);
   }

   private void writePrimitiveContextTag(int tagNumber, int length) {
      this.writePrimitiveTag(tagNumber, 8, length);
   }

   private void writePrimitiveTag(int tagNumber, int tagClass, int length) {
      int lvt;
      if (length <= 4) {
         lvt = length;
      } else {
         lvt = 5;
      }

      this.writeTag(tagNumber, tagClass, lvt);
      if (lvt == 5) {
         this.writeExtendedLength(length);
      }
   }

   private void writeTag(int tagNumber, int tagClass, int lengthValueType) {
      if (tagNumber <= 14) {
         tagNumber <<= 4;
         this.write(tagNumber | tagClass | lengthValueType);
      } else {
         if (tagNumber > 254) {
            throw new IllegalArgumentException("Tag number > 254 not permitted !");
         }

         this.write(240 | tagClass | lengthValueType);
         this.write(tagNumber);
      }
   }

   void writeExtendedLength(int length) {
      if (length <= 4) {
         throw new IllegalArgumentException("Length too small to be extended");
      } else {
         if (length <= 253) {
            this.write(length);
         } else if (length <= 65535) {
            int msb = (length & 0xFF00) >> 8;
            int lsb = length & 0xFF;
            this.write(254);
            this.write(msb);
            this.write(lsb);
         } else {
            int msb = (length & 0xFF000000) >> 24;
            int msb2 = (length & 0xFF0000) >> 16;
            int msb3 = (length & 0xFF00) >> 8;
            int lsb = length & 0xFF;
            this.write(255);
            this.write(msb);
            this.write(msb2);
            this.write(msb3);
            this.write(lsb);
         }
      }
   }

   @Override
   public void write(byte[] array) {
      this.write(array, 0, array.length);
   }

   public static void setPoolSize(int poolSize) {
      synchronized (poolLock) {
         AsnOutputStream[] tmp = new AsnOutputStream[poolSize];
         System.arraycopy(pool, 0, tmp, 0, Math.min(pool.length, tmp.length));
         pool = tmp;
      }
   }

   public static AsnOutputStream make() {
      synchronized (poolLock) {
         AsnOutputStream strm;
         if (cnt > 0 && cnt <= pool.length) {
            strm = pool[--cnt];
            strm.reset();
         } else {
            strm = new AsnOutputStream();
         }

         strm.freeBuf = false;
         return strm;
      }
   }

   public void release() {
      synchronized (poolLock) {
         if (!this.freeBuf) {
            if (cnt < pool.length) {
               pool[cnt++] = this;
            }

            this.freeBuf = true;
         }
      }
   }
}
