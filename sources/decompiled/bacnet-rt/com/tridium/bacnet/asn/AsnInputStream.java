package com.tridium.bacnet.asn;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.status.BStatus;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;

public class AsnInputStream extends ByteArrayInputStream implements AsnInput, AsnConst {
   private static final int BACNET_STATUS_FLAGS_ALARM_BIT = 128;
   private static final int BACNET_STATUS_FLAGS_FAULT_BIT = 64;
   private static final int BACNET_STATUS_FLAGS_OVERRIDDEN_BIT = 32;
   private static final int BACNET_STATUS_FLAGS_OUT_OF_SERVICE_BIT = 16;
   private static final int STREAM_POOL_SIZE = 20;
   private static final byte[] NULL_BUFFER = new byte[0];
   private int currentTag;
   private int currentTagNumber;
   private int dataLength;
   private int currentTagIndex;
   private int nextTag;
   private boolean freeBuf = false;
   private static int cnt = 0;
   private static AsnInputStream[] pool = new AsnInputStream[20];
   private static Object poolLock = new Object();

   public AsnInputStream() {
      super(new byte[0]);
   }

   public AsnInputStream(byte[] encodedData) {
      super(encodedData);
      this.nextTag = 0;
   }

   public AsnInputStream(byte[] encodedData, int offset, int length) {
      super(encodedData, offset, length);
      this.nextTag = 0;
   }

   public void setBuffer(byte[] buffer) {
      this.setBuffer(buffer, 0);
   }

   public void setBuffer(byte[] buffer, int newPos) {
      if (buffer == null) {
         this.buf = NULL_BUFFER;
      } else {
         this.buf = buffer;
      }

      this.pos = this.mark = newPos;
      this.count = this.buf.length;
      this.nextTag = 0;
   }

   public int getPos() {
      return this.pos;
   }

   @Override
   public int peekTag() throws AsnException {
      if (this.available() == 0) {
         this.currentTag = -1;
         this.currentTagNumber = -1;
         this.dataLength = -1;
         this.currentTagIndex = -1;
         this.nextTag = -1;
         return -1;
      } else {
         int savedPos = this.pos;
         int peekTag = this.readTag();
         this.pos = savedPos;
         return peekTag;
      }
   }

   @Override
   public boolean isApplicationTag(int tagNumber) {
      return this.currentTagNumber == tagNumber && (this.currentTag & 8) == 0;
   }

   @Override
   public boolean isContextTag(int tagNumber) {
      return this.currentTagNumber == tagNumber && (this.currentTag & 8) == 8;
   }

   @Override
   public boolean isOpeningTag(int tagNumber) {
      return this.currentTagNumber == tagNumber && (this.currentTag & 8) == 8 && (this.currentTag & 7) == 6;
   }

   @Override
   public void skipOpeningTag(int tagNumber) throws AsnException {
      int tag = this.peekTag();
      if (this.isOpeningTag(tagNumber)) {
         this.skipTag();
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   @Override
   public boolean isClosingTag(int tagNumber) {
      return this.currentTagNumber == tagNumber && (this.currentTag & 8) == 8 && (this.currentTag & 7) == 7;
   }

   @Override
   public void skipClosingTag(int tagNumber) throws AsnException {
      int tag = this.peekTag();
      if (this.isClosingTag(tagNumber)) {
         this.skipTag();
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   @Override
   public boolean isValueTag(int tagNumber) {
      return this.currentTagNumber == tagNumber && (this.currentTag & 7) != 6 && (this.currentTag & 7) != 7;
   }

   @Override
   public int peekApplicationTag() throws AsnException {
      int tag = this.peekTag();
      return this.isApplicationTag(tag) ? tag : -1;
   }

   @Override
   public int getDataLength() {
      return this.dataLength;
   }

   @Override
   public int skipTag() throws AsnException {
      if (this.available() == 0) {
         this.currentTag = -1;
         this.currentTagNumber = -1;
         this.dataLength = -1;
         this.currentTagIndex = -1;
         this.nextTag = -1;
         return -1;
      } else {
         int tagNumber = this.readTag();
         if (this.pos != this.nextTag) {
            this.pos = this.nextTag;
         }

         return tagNumber;
      }
   }

   @Override
   public BBacnetNull readNull() throws AsnException {
      this.readTag();
      this.validateApplicationTag(0);
      return BBacnetNull.DEFAULT;
   }

   @Override
   public BBacnetNull readNull(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return BBacnetNull.DEFAULT;
   }

   @Override
   public boolean readBoolean() throws AsnException {
      this.readTag();
      this.validateApplicationTag(1);
      return this.dataLength != 0;
   }

   @Override
   public boolean readBoolean(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      int value = this.read();
      if (value == 0) {
         return false;
      } else if (value == 1) {
         return true;
      } else {
         throw new AsnException("Invalid boolean value");
      }
   }

   @Override
   public long readUnsignedInteger() throws AsnException {
      this.readTag();
      this.validateApplicationTag(2);
      return this.decodeUnsignedInteger();
   }

   @Override
   public long readUnsignedInteger(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeUnsignedInteger();
   }

   private long decodeUnsignedInteger() throws AsnException {
      long value = 0L;
      if (this.dataLength > 4) {
         throw new AsnException("Integer overflow");
      } else {
         for (int i = 0; i < this.dataLength; i++) {
            value <<= 8;
            value |= this.read();
         }

         if (value < 0L) {
            throw new AsnException("Integer overflow");
         } else {
            return value;
         }
      }
   }

   @Override
   public int readUnsignedInt() throws AsnException {
      this.readTag();
      this.validateApplicationTag(2);
      return (int)this.decodeUnsignedInteger();
   }

   @Override
   public int readUnsignedInt(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return (int)this.decodeUnsignedInteger();
   }

   @Override
   public BBacnetUnsigned readUnsigned() throws AsnException {
      this.readTag();
      this.validateApplicationTag(2);
      return BBacnetUnsigned.make(this.decodeUnsignedInteger());
   }

   @Override
   public BBacnetUnsigned readUnsigned(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return BBacnetUnsigned.make(this.decodeUnsignedInteger());
   }

   @Override
   public int readSignedInteger() throws AsnException {
      this.readTag();
      this.validateApplicationTag(3);
      return this.decodeSignedInteger();
   }

   @Override
   public int readSignedInteger(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeSignedInteger();
   }

   private int decodeSignedInteger() throws AsnException {
      if (this.dataLength > 4) {
         throw new AsnException("Integer overflow");
      } else {
         int val = this.read();
         if (this.dataLength < 4 && (val & 128) != 0) {
            val |= -256;
         }

         for (int len = this.dataLength - 1; len > 0; len--) {
            val <<= 8;
            val |= this.read();
         }

         return val;
      }
   }

   @Override
   public BInteger readSigned() throws AsnException {
      this.readTag();
      this.validateApplicationTag(3);
      return BInteger.make(this.decodeSignedInteger());
   }

   @Override
   public BInteger readSigned(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return BInteger.make(this.decodeSignedInteger());
   }

   @Override
   public int readInteger() throws AsnException {
      this.readTag();
      boolean unsignedInt = true;

      try {
         this.validateApplicationTag(2);
      } catch (AsnException var3) {
         this.validateApplicationTag(3);
         unsignedInt = false;
      }

      return unsignedInt ? (int)this.decodeUnsignedInteger() : this.decodeSignedInteger();
   }

   @Override
   public float readReal() throws AsnException {
      this.readTag();
      this.validateApplicationTag(4);
      return this.decodeReal();
   }

   @Override
   public float readReal(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeReal();
   }

   private float decodeReal() throws AsnException {
      if (this.dataLength != 4) {
         throw new AsnException("Invalid real value");
      } else {
         int floatBits = 0;

         for (int i = 0; i < this.dataLength; i++) {
            floatBits <<= 8;
            floatBits |= this.read();
         }

         return Float.intBitsToFloat(floatBits);
      }
   }

   @Override
   public BFloat readFloat() throws AsnException {
      this.readTag();
      this.validateApplicationTag(4);
      return BFloat.make(this.decodeReal());
   }

   @Override
   public BFloat readFloat(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return BFloat.make(this.decodeReal());
   }

   @Override
   public double readDouble() throws AsnException {
      this.readTag();
      this.validateApplicationTag(5);
      return this.decodeDouble();
   }

   @Override
   public double readDouble(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeDouble();
   }

   private double decodeDouble() throws AsnException {
      long doubleBits = 0L;
      if (this.dataLength != 8) {
         throw new AsnException("Invalid double value");
      } else {
         for (int i = 0; i < this.dataLength; i++) {
            doubleBits <<= 8;
            doubleBits |= this.read();
         }

         return Double.longBitsToDouble(doubleBits);
      }
   }

   @Override
   public byte[] readOctetString() throws AsnException {
      this.readTag();
      this.validateApplicationTag(6);
      return this.decodeOctetString();
   }

   @Override
   public byte[] readOctetString(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeOctetString();
   }

   @Override
   public BBacnetOctetString readBacnetOctetString() throws AsnException {
      this.readTag();
      this.validateApplicationTag(6);
      return BBacnetOctetString.make(this.decodeOctetString());
   }

   @Override
   public BBacnetOctetString readBacnetOctetString(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return BBacnetOctetString.make(this.decodeOctetString());
   }

   private byte[] decodeOctetString() {
      byte[] octetString = new byte[this.dataLength];

      for (int i = 0; i < octetString.length; i++) {
         octetString[i] = (byte)this.read();
      }

      return octetString;
   }

   public BCharacterSetEncoding peekEncoding() throws AsnException {
      int savedPos = this.pos;
      this.readTag();
      this.validateApplicationTag(7);
      int peekEnc = this.read();
      this.pos = savedPos;
      return BCharacterSetEncoding.make(peekEnc);
   }

   public BCharacterSetEncoding peekEncoding(int contextTag) throws AsnException {
      int savedPos = this.pos;
      this.readTag();
      this.validateContextTag(contextTag);
      int peekEnc = this.read();
      this.pos = savedPos;
      return BCharacterSetEncoding.make(peekEnc);
   }

   @Override
   public String readCharacterString() throws AsnException {
      this.readTag();
      this.validateApplicationTag(7);
      return this.decodeCharacterString();
   }

   @Override
   public String readCharacterString(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeCharacterString();
   }

   private String decodeCharacterString() throws AsnException {
      BCharacterSetEncoding encoding = BCharacterSetEncoding.make(this.read());
      String enc = null;
      int strlen = this.dataLength - 1;
      if (encoding == BCharacterSetEncoding.ibmMicrosoftDBCS) {
         int codePage = this.read() << 8;
         codePage |= this.read();
         enc = "cp" + codePage;
         strlen -= 2;
      } else {
         enc = encoding.getEncodingName();
      }

      if (enc == null) {
         throw new AsnException("Unsupported char set " + encoding.getTag());
      } else if (strlen == 0) {
         return "";
      } else {
         byte[] charArray = new byte[strlen];
         this.read(charArray);

         try {
            return new String(charArray, enc);
         } catch (UnsupportedEncodingException var6) {
            throw new AsnException("Unsupported char set " + encoding.getTag());
         }
      }
   }

   public void skipCharacterString(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      BCharacterSetEncoding encoding = BCharacterSetEncoding.make(this.read());
      int strlen = this.dataLength - 1;
      if (encoding == BCharacterSetEncoding.ibmMicrosoftDBCS) {
         this.read();
         this.read();
         strlen -= 2;
      }

      this.skip(strlen);
   }

   @Override
   public BBacnetBitString readBitString() throws AsnException {
      this.readTag();
      this.validateApplicationTag(8);
      return this.decodeBitString();
   }

   @Override
   public BBacnetBitString readBitString(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeBitString();
   }

   private BBacnetBitString decodeBitString() throws AsnException {
      int unusedBits = this.read();
      int totalBits = (this.dataLength - 1) * 8 - unusedBits;
      if (totalBits < 0) {
         throw new AsnException("BitString totalBits cannot be negative; unusedBits: " + unusedBits + ", dataLength: " + this.dataLength);
      } else {
         boolean[] bitString = new boolean[totalBits];
         int bit = 0;

         for (int i = 0; i < this.dataLength - 1; i++) {
            int byteValue = this.read();
            int bitmask = 128;

            for (int j = 0; j < 8; j++) {
               if (bit < bitString.length) {
                  bitString[bit] = (byteValue & bitmask) != 0;
               }

               bitmask >>= 1;
               bit++;
            }
         }

         return BBacnetBitString.make(bitString);
      }
   }

   public BStatus readStatusFlags() throws AsnException {
      this.readTag();
      this.validateApplicationTag(8);
      return this.decodeStatusFlags();
   }

   public BStatus readStatusFlags(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeStatusFlags();
   }

   private BStatus decodeStatusFlags() throws AsnException {
      if (this.read() != 4) {
         throw new AsnException("Invalid bit string length");
      } else {
         int byteValue = this.read();
         int status = 0;
         if ((byteValue & 128) != 0) {
            status |= 8;
         }

         if ((byteValue & 64) != 0) {
            status |= 2;
         }

         if ((byteValue & 32) != 0) {
            status |= 32;
         }

         boolean outOfService = (byteValue & 16) != 0;
         return !outOfService ? BStatus.make(status) : BStatus.make(status, BFacets.make("outOfService", true));
      }
   }

   @Override
   public int readEnumerated() throws AsnException {
      this.readTag();
      this.validateApplicationTag(9);
      return (int)this.decodeUnsignedInteger();
   }

   @Override
   public int readEnumerated(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return (int)this.decodeUnsignedInteger();
   }

   @Override
   public BBacnetDate readDate() throws AsnException {
      this.readTag();
      this.validateApplicationTag(10);
      return this.decodeDate();
   }

   @Override
   public BBacnetDate readDate(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeDate();
   }

   private BBacnetDate decodeDate() {
      return BBacnetDate.make(this.read(), this.read(), this.read(), this.read());
   }

   @Override
   public BBacnetTime readTime() throws AsnException {
      this.readTag();
      this.validateApplicationTag(11);
      return this.decodeTime();
   }

   @Override
   public BBacnetTime readTime(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeTime();
   }

   public BBacnetTime decodeTime() {
      return BBacnetTime.make(this.read(), this.read(), this.read(), this.read());
   }

   @Override
   public BBacnetObjectIdentifier readObjectIdentifier() throws AsnException {
      this.readTag();
      this.validateApplicationTag(12);
      return this.decodeObjectIdentifier();
   }

   @Override
   public BBacnetObjectIdentifier readObjectIdentifier(int contextTag) throws AsnException {
      this.readTag();
      this.validateContextTag(contextTag);
      return this.decodeObjectIdentifier();
   }

   private BBacnetObjectIdentifier decodeObjectIdentifier() throws AsnException {
      int objectId = (int)this.decodeUnsignedInteger();
      return BBacnetObjectIdentifier.makeId(objectId);
   }

   public BSimple readAsn() throws AsnException {
      this.readTag();
      this.validateApplicationTag(this.currentTagNumber);
      switch (this.currentTagNumber) {
         case 0:
            return this.readNull();
         case 1:
            return BBoolean.make(this.readBoolean());
         case 2:
            return this.readUnsigned();
         case 3:
            return this.readSigned();
         case 4:
            return this.readFloat();
         case 5:
            return BDouble.make(this.readDouble());
         case 6:
            return this.readBacnetOctetString();
         case 7:
            return BString.make(this.readCharacterString());
         case 8:
            return this.readBitString();
         case 9:
            return BInteger.make(this.readEnumerated());
         case 10:
            return this.readDate();
         case 11:
            return this.readTime();
         case 12:
            return this.readObjectIdentifier();
         case 13:
         case 14:
         case 15:
         default:
            throw new AsnException("Invalid tag: " + this.currentTagNumber);
      }
   }

   @Override
   public byte[] readContextTaggedData() throws AsnException {
      byte[] b = new byte[this.dataLength];
      this.readTag();
      this.read(b);
      return b;
   }

   @Override
   public byte[] readEncodedValue(int tagNumber) throws AsnException {
      this.readTag();
      if (this.currentTagNumber != tagNumber) {
         throw new AsnException("Invalid tag: " + this.currentTagNumber);
      } else if ((this.currentTag & 7) != 6) {
         throw new AsnException("Expected opening tag");
      } else {
         byte[] encodedValue = null;
         int valueLength = 0;
         boolean closingTagFound = false;
         int valueBeginIndex = this.pos;
         int tagCount = 1;

         while (this.skipTag() != -1) {
            if (this.isOpeningTag(tagNumber)) {
               tagCount++;
            }

            if (this.isClosingTag(tagNumber)) {
               if (--tagCount == 0) {
                  closingTagFound = true;
                  valueLength = this.currentTagIndex - valueBeginIndex;
                  break;
               }
            }
         }

         if (!closingTagFound) {
            throw new AsnException("No closing tag");
         } else {
            if (valueLength != 0) {
               int oldPos = this.pos;
               this.pos = valueBeginIndex;
               encodedValue = new byte[valueLength];
               this.read(encodedValue);
               this.pos = oldPos;
            } else {
               encodedValue = new byte[0];
            }

            return encodedValue;
         }
      }
   }

   private void validateContextTag(int tagNumber) throws AsnException {
      if ((this.currentTag & 8) != 8) {
         throw new AsnException("Invalid tag class actual=" + this.currentTag + " expected=" + tagNumber);
      } else if (!this.isValueTag(tagNumber)) {
         throw new AsnException("Invalid tag: " + tagNumber);
      }
   }

   private void validateApplicationTag(int tagNumber) throws AsnException {
      if ((this.currentTag & 8) != 0) {
         throw new AsnException("Invalid tag class actual=" + this.currentTag + " expected=" + tagNumber);
      } else if (!this.isValueTag(tagNumber)) {
         throw new AsnException("Invalid tag: " + tagNumber);
      }
   }

   private int readTag() throws AsnException {
      this.currentTagIndex = this.pos;
      this.currentTag = this.read();
      this.currentTagNumber = this.readTagNumber(this.currentTag);
      if ((this.currentTag & 15) != 14 && (this.currentTag & 15) != 15) {
         this.parsePrimitiveData(this.currentTag);
      } else {
         this.parseConstructedData(this.currentTag);
      }

      return this.currentTagNumber;
   }

   private int readTagNumber(int tag) throws AsnException {
      int tagNumber = 0;
      if ((tag & 240) == 240) {
         tagNumber = this.read();
         if (tagNumber == 255) {
            throw new AsnException("Invalid tag number");
         }
      } else {
         tagNumber = (tag & 240) >> 4;
      }

      return tagNumber;
   }

   private void parsePrimitiveData(int tag) throws AsnException {
      if ((tag & 8) == 0 && this.currentTagNumber == 1) {
         this.dataLength = tag & 7;
         this.nextTag = this.pos;
      } else {
         if ((tag & 7) != 5) {
            this.dataLength = tag & 7;
         } else {
            int extendedLengthTag = this.read();
            if (extendedLengthTag <= 253) {
               this.dataLength = extendedLengthTag;
            } else if (extendedLengthTag == 254) {
               int temp = this.read();
               this.dataLength = temp << 8;
               temp = this.read();
               this.dataLength |= temp;
            } else {
               if (extendedLengthTag != 255) {
                  throw new AsnException("Invalid length tag");
               }

               this.dataLength = 0;

               for (int i = 0; i < 4; i++) {
                  this.dataLength <<= 8;
                  this.dataLength = this.dataLength | this.read();
               }
            }
         }

         if (this.available() < this.dataLength) {
            throw new AsnException("Invalid length tag");
         } else {
            this.nextTag = this.pos + this.dataLength;
         }
      }
   }

   private void parseConstructedData(int tag) {
      this.nextTag = this.pos;
      this.dataLength = 0;
   }

   @Override
   public int read(byte[] array) {
      return this.read(array, 0, array.length);
   }

   public static void setPoolSize(int poolSize) {
      synchronized (poolLock) {
         AsnInputStream[] tmp = new AsnInputStream[poolSize];
         System.arraycopy(pool, 0, tmp, 0, Math.min(pool.length, tmp.length));
         pool = tmp;
      }
   }

   public static AsnInputStream make(byte[] buf) {
      return buf == null ? make(NULL_BUFFER, 0, 0) : make(buf, 0, buf.length);
   }

   public static AsnInputStream make(byte[] buf, int offset, int length) {
      if (buf == null) {
         buf = NULL_BUFFER;
      }

      synchronized (poolLock) {
         AsnInputStream strm;
         if (cnt > 0 && cnt <= pool.length) {
            strm = pool[--cnt];
            strm.pos = offset;
            strm.mark = offset;
            strm.buf = buf;
            strm.count = length;
         } else {
            strm = new AsnInputStream(buf, offset, length);
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
