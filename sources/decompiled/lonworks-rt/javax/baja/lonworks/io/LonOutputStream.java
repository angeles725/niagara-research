package javax.baja.lonworks.io;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import javax.baja.sys.BajaRuntimeException;

public class LonOutputStream extends ByteArrayOutputStream {
   int bitFieldMark = 0;

   public LonOutputStream() {
   }

   public LonOutputStream(int size) {
      super(size);
   }

   public void writeByteArray(byte[] byteArray) {
      this.write(byteArray, 0, byteArray.length);
   }

   public void writeByteArray(byte[] byteArray, int count) {
      int copyCnt = byteArray.length > count ? count : byteArray.length;
      this.write(byteArray, 0, copyCnt);

      while (copyCnt < count) {
         this.write(0);
         copyCnt++;
      }
   }

   public void writeCharArray(String str, int count) {
      char[] a = str.toCharArray();

      for (int i = 0; i < count; i++) {
         if (i < a.length) {
            this.write(a[i]);
         } else {
            this.write(0);
         }
      }
   }

   public void writeString(String str) {
      char[] a = str.toCharArray();

      for (int i = 0; i < a.length; i++) {
         this.write(a[i]);
      }

      this.write(0);
   }

   public void writeBooleanBit(boolean val, int byteOffset, int bitOffset, int bitCount) {
      this.writeBit(val ? 1 : 0, byteOffset, bitOffset, bitCount);
   }

   public void writeBit(int val, int byteOffset, int bitOffset, int bitCount) {
      int bytOffset = this.bitFieldMark + byteOffset;

      while (this.count <= bytOffset) {
         this.write(0);
      }

      int mask = 1;

      while (--bitCount > 0) {
         mask = mask << 1 | 1;
      }

      if (bytOffset >= 0 && bytOffset < this.buf.length) {
         this.buf[bytOffset] = (byte)(this.buf[bytOffset] | (val & mask) << bitOffset);
      } else {
         System.out.println("*********\nwriteBit byteOffset = " + bytOffset + "  len = " + this.buf.length);
         Thread.dumpStack();
      }
   }

   public void writeSignedBit(int val, int byteOffset, int bitOffset, int bitCount) {
      this.writeBit(val, byteOffset, bitOffset, bitCount);
   }

   public void writeBoolean(boolean b) {
      this.write(b ? 1 : 0);
   }

   public void writeSigned16(int l) {
      int highByte = l >> 8 & 0xFF;
      int lowByte = l & 0xFF;
      this.write(highByte);
      this.write(lowByte);
   }

   public void writeUnsigned16(int l) {
      int highByte = l >> 8 & 0xFF;
      int lowByte = l & 0xFF;
      this.write(highByte);
      this.write(lowByte);
   }

   public void writeSigned8(int i) {
      this.write(i & 0xFF);
   }

   public void writeUnsigned8(int i) {
      this.write(i & 0xFF);
   }

   public void writeSigned32(int i) {
      this.write((i & 0xFF000000) >> 24);
      this.write((i & 0xFF0000) >> 16);
      this.write((i & 0xFF00) >> 8);
      this.write(i & 0xFF);
   }

   public void writeUnsigned32(long i) {
      this.write((int)(i & -16777216L) >> 24);
      this.write((int)(i & 16711680L) >> 16);
      this.write((int)(i & 65280L) >> 8);
      this.write((int)(i & 255L));
   }

   public void writeSigned64(long i) {
      this.write((int)((i & -72057594037927936L) >> 56));
      this.write((int)((i & 71776119061217280L) >> 48));
      this.write((int)((i & 280375465082880L) >> 40));
      this.write((int)((i & 1095216660480L) >> 32));
      this.write((int)((i & 4278190080L) >> 24));
      this.write((int)((i & 16711680L) >> 16));
      this.write((int)((i & 65280L) >> 8));
      this.write((int)(i & 255L));
   }

   public void writeUnsigned64(BigInteger val) {
      byte[] a = val.toByteArray();
      int len = a.length;
      int msb = 0;

      while (a[msb] == 0 && len - msb > 8) {
         msb++;
      }

      if (len - msb > 8) {
         throw new BajaRuntimeException("LonOutputStream.writeUnsigned64() error:" + val + " greater than 64 bits.");
      } else {
         for (int i = 0; i < 8 - len + msb; i++) {
            this.write(0);
         }

         for (int i = msb; i < len; i++) {
            this.write(a[i]);
         }
      }
   }

   public void writeFloat(float value) {
      int bitView = Float.floatToIntBits(value);
      this.write((bitView & 0xFF000000) >> 24);
      this.write((bitView & 0xFF0000) >> 16);
      this.write((bitView & 0xFF00) >> 8);
      this.write(bitView & 0xFF);
   }

   public void writeDouble(double value) {
      long bitView = Double.doubleToLongBits(value);
      this.write((int)((bitView & -72057594037927936L) >> 56));
      this.write((int)((bitView & 71776119061217280L) >> 48));
      this.write((int)((bitView & 280375465082880L) >> 40));
      this.write((int)((bitView & 1095216660480L) >> 32));
      this.write((int)((bitView & 4278190080L) >> 24));
      this.write((int)((bitView & 16711680L) >> 16));
      this.write((int)((bitView & 65280L) >> 8));
      this.write((int)(bitView & 255L));
   }

   public int setBitFieldMark() {
      int orig = this.bitFieldMark;
      this.bitFieldMark = this.count;
      return orig;
   }

   public void resetBitFieldMark(int orig) {
      this.bitFieldMark = orig;
   }

   public void setPosition(int position) {
      while (this.count < position) {
         this.write(0);
      }
   }
}
