package javax.baja.lonworks.io;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;

public class LonInputStream extends ByteArrayInputStream {
   int bitFieldMark = 0;

   public LonInputStream(byte[] buf) {
      super(buf);
   }

   public boolean readBoolean() {
      return this.read() != 0;
   }

   public int readSigned32() {
      int byte3 = this.read() & 0xFF;
      int byte2 = this.read() & 0xFF;
      int byte1 = this.read() & 0xFF;
      int byte0 = this.read() & 0xFF;
      return byte3 << 24 | byte2 << 16 | byte1 << 8 | byte0;
   }

   public long readUnsigned32() {
      long byte3 = this.read() & 0xFF;
      long byte2 = this.read() & 0xFF;
      long byte1 = this.read() & 0xFF;
      long byte0 = this.read() & 0xFF;
      return (byte3 << 24 | byte2 << 16 | byte1 << 8 | byte0) & -1L;
   }

   public long readSigned64() {
      long byte7 = this.read() & 0xFF;
      long byte6 = this.read() & 0xFF;
      long byte5 = this.read() & 0xFF;
      long byte4 = this.read() & 0xFF;
      long byte3 = this.read() & 0xFF;
      long byte2 = this.read() & 0xFF;
      long byte1 = this.read() & 0xFF;
      long byte0 = this.read() & 0xFF;
      return byte7 << 56 | byte6 << 48 | byte5 << 40 | byte4 << 32 | byte3 << 24 | byte2 << 16 | byte1 << 8 | byte0;
   }

   public BigInteger readUnsigned64() {
      byte[] a = new byte[9];
      a[0] = 0;

      for (int i = 1; i < a.length; i++) {
         a[i] = (byte)this.read();
      }

      return new BigInteger(a);
   }

   public int readUnsigned16() {
      int highByte = this.read() & 0xFF;
      int lowByte = this.read() & 0xFF;
      return highByte << 8 | lowByte;
   }

   public int readSigned16() {
      int highByte = this.read();
      int lowByte = this.read() & 0xFF;
      boolean signBit = false;
      int temp = 0;
      if ((highByte & 128) != 0) {
         signBit = true;
      }

      temp = highByte << 8 | lowByte;
      return signBit ? temp | -65536 : temp;
   }

   public int readSigned8() {
      int highByte = this.read();
      return (highByte & 128) != 0 ? highByte | -256 : highByte;
   }

   public int readUnsigned8() {
      return this.read() & 0xFF;
   }

   public int readBit(int byteOffset, int bitOffset, int bitCount) {
      if (byteOffset < this.count && byteOffset >= 0) {
         this.pos = this.bitFieldMark + byteOffset;
         int b = this.read() >> bitOffset;
         int mask = 1;

         while (--bitCount > 0) {
            mask = mask << 1 | 1;
         }

         return b & mask;
      } else {
         throw new RuntimeException("OutOfRange byteOffset in readBit. " + byteOffset + ":" + this.count);
      }
   }

   public int readSignedBit(int byteOffset, int bitOffset, int bitCount) {
      int n = this.readBit(byteOffset, bitOffset, bitCount);
      int signBit = 1 << bitCount - 1;
      if ((n & signBit) == 0) {
         return n;
      } else {
         int mask = 0;

         for (int i = 1; i < bitCount; i++) {
            mask = (mask << 1) + 1;
         }

         return (n & mask) - signBit;
      }
   }

   public boolean readBooleanBit(int byteOffset, int bitOffset, int bitCount) {
      int val = this.readBit(byteOffset, bitOffset, bitCount);
      return val > 0;
   }

   public String readString() {
      StringBuilder sb = new StringBuilder();

      char c8;
      while ((c8 = (char)this.read()) != 0) {
         sb.append(c8);
      }

      return sb.toString();
   }

   public String readCharArray(int len) {
      if (len > this.available()) {
         len = this.available();
      }

      StringBuilder sb = new StringBuilder();
      boolean terminated = false;

      for (int i = 0; i < len; i++) {
         char c8 = (char)this.read();
         if (c8 == 0) {
            terminated = true;
         }

         if (!terminated) {
            sb.append(c8);
         }
      }

      return sb.toString();
   }

   public byte[] readByteArray(int len) {
      if (len > this.available()) {
         len = this.available();
      }

      byte[] a = new byte[len];

      for (int i = 0; i < len; i++) {
         a[i] = (byte)this.read();
      }

      return a;
   }

   public byte[] readByteArray() {
      return this.readByteArray(this.available());
   }

   public float readFloat() {
      return Float.intBitsToFloat(this.readSigned32());
   }

   public double readDouble() {
      long v = this.read();

      for (int i = 0; i < 7; i++) {
         v = v << 8 | this.read();
      }

      return Double.longBitsToDouble(v);
   }

   @Override
   public void reset() {
      this.pos = 0;
   }

   public void reset(int position) {
      this.pos = position;
   }

   public int setBitFieldMark() {
      int orig = this.bitFieldMark;
      this.bitFieldMark = this.pos;
      return orig;
   }

   public void resetBitFieldMark(int orig) {
      this.bitFieldMark = orig;
   }

   public int position() {
      return this.pos;
   }
}
