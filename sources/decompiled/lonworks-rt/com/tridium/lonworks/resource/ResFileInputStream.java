package com.tridium.lonworks.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ResFileInputStream extends ByteArrayInputStream {
   int majorFmtVer;
   int minorFmtVer;
   int fileType;

   public ResFileInputStream(byte[] buf) {
      super(buf);
   }

   @Override
   public synchronized int read() {
      return super.read();
   }

   public boolean readBool() throws IOException {
      return this.read() != 0;
   }

   public int readSigned32() throws IOException {
      int byte3 = this.read() & 0xFF;
      int byte2 = this.read() & 0xFF;
      int byte1 = this.read() & 0xFF;
      int byte0 = this.read() & 0xFF;
      return byte3 << 24 | byte2 << 16 | byte1 << 8 | byte0;
   }

   public long readUnsigned32() throws IOException {
      long n = 0L;

      for (int i = 0; i < 4; i++) {
         n <<= 8;
         n |= this.read() & 0xFF;
      }

      return n;
   }

   public long readSigned64() throws IOException {
      long n = 0L;

      for (int i = 0; i < 8; i++) {
         n <<= 8;
         n |= this.read() & 0xFF;
      }

      return n;
   }

   public int readDirectory(ResourceFile rf) throws IOException {
      int byte3 = this.read() & 0xFF;
      int byte2 = this.read() & 0xFF;
      int byte1 = this.read() & 0xFF;
      int byte0 = this.read() & 0xFF;
      return (this.fileType != 3 || this.majorFmtVer < 3) && (this.fileType == 3 || this.majorFmtVer < 2)
         ? byte0 << 24 | byte1 << 16 | byte2 << 8 | byte3
         : byte3 << 24 | byte2 << 16 | byte1 << 8 | byte0;
   }

   public int readUnsigned24() throws IOException {
      int byte2 = this.read() & 0xFF;
      int byte1 = this.read() & 0xFF;
      int byte0 = this.read() & 0xFF;
      return byte2 << 16 | byte1 << 8 | byte0;
   }

   public int readUnsigned16() throws IOException {
      int highByte = this.read() & 0xFF;
      int lowByte = this.read() & 0xFF;
      return highByte << 8 | lowByte;
   }

   public int readSigned16() throws IOException {
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

   public int readSigned8() throws IOException {
      int highByte = this.read();
      return (highByte & 128) != 0 ? highByte | -256 : highByte;
   }

   public int readUnsigned8() throws IOException {
      return this.read() & 0xFF;
   }

   public char readCharacter() throws IOException {
      return (char)this.read();
   }

   public String readString(int len) throws IOException {
      StringBuilder sb = new StringBuilder();
      boolean eos = false;

      for (int i = 0; i < len; i++) {
         char c8 = (char)this.read();
         if (!eos) {
            if (c8 == 0) {
               eos = true;
            } else {
               sb.append(c8);
            }
         }
      }

      return sb.toString();
   }

   public String readLString() throws IOException {
      return this.readString(this.readUnsigned8());
   }

   public byte[] readByteArray(int len) throws IOException {
      byte[] a = new byte[len];

      for (int i = 0; i < len; i++) {
         a[i] = (byte)this.read();
      }

      return a;
   }

   public float readFloat() throws IOException {
      return Float.intBitsToFloat(this.readSigned32());
   }

   public double readDouble() throws IOException {
      return Double.longBitsToDouble(this.readSigned64());
   }

   public int getCurrentPosition() {
      return this.pos;
   }

   public int swap16(int val) {
      int byte1 = val & 0xFF;
      int byte0 = val >> 8 & 0xFF;
      return byte1 << 8 | byte0;
   }

   public void seek(long p) throws IOException {
      this.pos = (int)p;
   }
}
