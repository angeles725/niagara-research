package javax.baja.nre.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UTFDataFormatException;

public class ByteBuffer implements DataOutput, DataInput {
   protected int length;
   protected byte[] buffer;
   protected int pos;
   protected boolean bigEndian = true;

   public ByteBuffer(byte[] buf, int len) {
      this.length = len;
      this.buffer = buf;
   }

   public ByteBuffer(byte[] buf) {
      this.length = buf.length;
      this.buffer = buf;
   }

   public ByteBuffer(int initialCapacity) {
      this.buffer = new byte[initialCapacity];
   }

   public ByteBuffer() {
      this(64);
   }

   public int getLength() {
      return this.length;
   }

   public void reset() {
      this.pos = 0;
      this.length = 0;
   }

   public void seek(int pos) {
      this.pos = pos;
   }

   public int getPosition() {
      return this.pos;
   }

   public byte[] getBytes() {
      return this.buffer;
   }

   public byte[] toByteArray() {
      byte[] copy = new byte[this.length];
      System.arraycopy(this.buffer, 0, copy, 0, this.length);
      return copy;
   }

   public boolean isBigEndian() {
      return this.bigEndian;
   }

   public void setBigEndian(boolean bigEndian) {
      this.bigEndian = bigEndian;
   }

   public InputStream getInputStream() {
      return new InputStream() {
         @Override
         public final int available() throws IOException {
            return ByteBuffer.this.available();
         }

         @Override
         public final int read() throws IOException {
            try {
               return ByteBuffer.this.read();
            } catch (EOFException eof) {
               return -1;
            }
         }

         @Override
         public final int read(byte[] buf, int off, int len) throws IOException {
            return ByteBuffer.this.read(buf, off, len);
         }
      };
   }

   public OutputStream getOutputStream() {
      return new OutputStream() {
         @Override
         public final void write(int v) {
            ByteBuffer.this.write(v);
         }

         @Override
         public final void write(byte[] buf, int off, int len) {
            ByteBuffer.this.write(buf, off, len);
         }
      };
   }

   public int readFrom(InputStream in, int len) throws IOException {
      this.insureCapacityToAdd(len);
      int n = 0;

      while (n < len) {
         int count = in.read(this.buffer, this.pos + n, len - n);
         if (count < 0) {
            break;
         }

         n += count;
      }

      this.length += len;
      return n;
   }

   public void readFullyFrom(InputStream in, int len) throws IOException {
      if (len < 0) {
         this.readToEnd(in);
      } else {
         this.insureCapacityToAdd(len);
         int n = 0;

         while (n < len) {
            int count = in.read(this.buffer, this.pos + n, len - n);
            if (count < 0) {
               throw new EOFException();
            }

            n += count;
         }

         this.length += len;
      }
   }

   public void readToEnd(InputStream in) throws IOException {
      byte[] buf = new byte[1024];

      int n;
      while ((n = in.read(buf, 0, 1024)) >= 0) {
         this.write(buf, 0, n);
      }
   }

   public void writeTo(OutputStream out) throws IOException {
      out.write(this.buffer, 0, this.length);
   }

   public void writeTo(OutputStream out, int offset, int len) throws IOException {
      if (len > this.length) {
         throw new IOException("len > internal buffer");
      }

      out.write(this.buffer, offset, len);
   }

   public void writeTo(DataOutput out) throws IOException {
      out.write(this.buffer, 0, this.length);
   }

   public void writeTo(DataOutput out, int offset, int len) throws IOException {
      if (len > this.length) {
         throw new IOException("len > internal buffer");
      }

      out.write(this.buffer, offset, len);
   }

   @Override
   public void write(int b) {
      if (this.length + 1 > this.buffer.length) {
         byte[] temp = new byte[Math.max(this.length << 2, 256)];
         System.arraycopy(this.buffer, 0, temp, 0, this.length);
         this.buffer = temp;
      }

      this.buffer[this.length++] = (byte)b;
   }

   @Override
   public void write(byte[] buf) {
      this.write(buf, 0, buf.length);
   }

   @Override
   public void write(byte[] buf, int offset, int len) {
      this.insureCapacityToAdd(len);
      System.arraycopy(buf, offset, this.buffer, this.length, len);
      this.length += len;
   }

   private final void insureCapacityToAdd(int toAddLength) {
      int needed = this.length + toAddLength;
      if (needed > this.buffer.length) {
         byte[] temp = new byte[Math.max(needed, 256)];
         System.arraycopy(this.buffer, 0, temp, 0, this.length);
         this.buffer = temp;
      }
   }

   @Override
   public void writeBoolean(boolean v) {
      this.write(v ? 1 : 0);
   }

   @Override
   public void writeByte(int v) {
      this.write(v);
   }

   @Override
   public void writeShort(int v) {
      if (this.bigEndian) {
         this.write(v >>> 8 & 0xFF);
         this.write(v >>> 0 & 0xFF);
      } else {
         this.write(v >>> 0 & 0xFF);
         this.write(v >>> 8 & 0xFF);
      }
   }

   @Override
   public void writeChar(int v) {
      if (this.bigEndian) {
         this.write(v >>> 8 & 0xFF);
         this.write(v >>> 0 & 0xFF);
      } else {
         this.write(v >>> 0 & 0xFF);
         this.write(v >>> 8 & 0xFF);
      }
   }

   @Override
   public void writeInt(int v) {
      if (this.bigEndian) {
         this.write(v >>> 24 & 0xFF);
         this.write(v >>> 16 & 0xFF);
         this.write(v >>> 8 & 0xFF);
         this.write(v >>> 0 & 0xFF);
      } else {
         this.write(v >>> 0 & 0xFF);
         this.write(v >>> 8 & 0xFF);
         this.write(v >>> 16 & 0xFF);
         this.write(v >>> 24 & 0xFF);
      }
   }

   @Override
   public void writeLong(long v) {
      if (this.bigEndian) {
         this.write((int)(v >>> 56) & 0xFF);
         this.write((int)(v >>> 48) & 0xFF);
         this.write((int)(v >>> 40) & 0xFF);
         this.write((int)(v >>> 32) & 0xFF);
         this.write((int)(v >>> 24) & 0xFF);
         this.write((int)(v >>> 16) & 0xFF);
         this.write((int)(v >>> 8) & 0xFF);
         this.write((int)(v >>> 0) & 0xFF);
      } else {
         this.write((int)(v >>> 0) & 0xFF);
         this.write((int)(v >>> 8) & 0xFF);
         this.write((int)(v >>> 16) & 0xFF);
         this.write((int)(v >>> 24) & 0xFF);
         this.write((int)(v >>> 32) & 0xFF);
         this.write((int)(v >>> 40) & 0xFF);
         this.write((int)(v >>> 48) & 0xFF);
         this.write((int)(v >>> 56) & 0xFF);
      }
   }

   @Override
   public void writeFloat(float v) {
      this.writeInt(Float.floatToIntBits(v));
   }

   @Override
   public void writeDouble(double v) {
      this.writeLong(Double.doubleToLongBits(v));
   }

   @Override
   public void writeBytes(String s) {
      int strlen = s.length();

      for (int i = 0; i < strlen; i++) {
         this.write((byte)s.charAt(i));
      }
   }

   @Override
   public void writeChars(String s) {
      int strlen = s.length();

      for (int i = 0; i < strlen; i++) {
         int v = s.charAt(i);
         this.write(v >>> 8 & 0xFF);
         this.write(v >>> 0 & 0xFF);
      }
   }

   @Override
   public void writeUTF(String s) throws UTFDataFormatException {
      int strlen = s.length();
      int utflen = 0;
      char[] chars = new char[strlen];
      s.getChars(0, strlen, chars, 0);

      for (int i = 0; i < strlen; i++) {
         int c = chars[i];
         if (c >= 1 && c <= 127) {
            utflen++;
         } else if (c > 2047) {
            utflen += 3;
         } else {
            utflen += 2;
         }
      }

      if (utflen > 65535) {
         throw new UTFDataFormatException();
      }

      this.insureCapacityToAdd(utflen + 2);
      byte[] buf = this.buffer;
      int count = this.length;
      this.length += utflen + 2;
      buf[count++] = (byte)(utflen >>> 8 & 0xFF);
      buf[count++] = (byte)(utflen >>> 0 & 0xFF);

      for (int i = 0; i < strlen; i++) {
         int c = chars[i];
         if (c >= 1 && c <= 127) {
            buf[count++] = (byte)c;
         } else if (c > 2047) {
            buf[count++] = (byte)(224 | c >> 12 & 15);
            buf[count++] = (byte)(128 | c >> 6 & 63);
            buf[count++] = (byte)(128 | c >> 0 & 63);
         } else {
            buf[count++] = (byte)(192 | c >> 6 & 31);
            buf[count++] = (byte)(128 | c >> 0 & 63);
         }
      }
   }

   public static int utfEncodedSize(String string) {
      int result = 2;
      int len = string.length();

      for (int i = 0; i < len; i++) {
         result += utfEncodedSize(string.charAt(i));
      }

      return result;
   }

   public static int utfEncodedSize(char c) {
      if (c >= 1 && c <= 127) {
         return 1;
      } else {
         return c > 2047 ? 3 : 2;
      }
   }

   public int available() {
      return this.length - this.pos;
   }

   public int peek() throws IOException {
      if (this.pos >= this.length) {
         throw new EOFException();
      } else {
         return this.buffer[this.pos] & 0xFF;
      }
   }

   public int read() throws IOException {
      if (this.pos >= this.length) {
         throw new EOFException();
      } else {
         return this.buffer[this.pos++] & 0xFF;
      }
   }

   public int read(byte[] buf) throws IOException {
      return this.read(buf, 0, buf.length);
   }

   public int read(byte[] buf, int offset, int len) throws IOException {
      if (buf == null) {
         throw new NullPointerException();
      }

      if (offset < 0 || len < 0 || len > buf.length - offset) {
         throw new IndexOutOfBoundsException();
      }

      if (len == 0) {
         return 0;
      }

      if (this.pos >= this.length) {
         return -1;
      }

      int actual = Math.min(len, this.length - this.pos);
      System.arraycopy(this.buffer, this.pos, buf, offset, actual);
      this.pos += actual;
      return actual;
   }

   @Override
   public void readFully(byte[] buf) throws IOException {
      this.readFully(buf, 0, buf.length);
   }

   @Override
   public void readFully(byte[] buf, int offset, int len) throws IOException {
      if (this.read(buf, offset, len) != len) {
         throw new EOFException();
      }
   }

   @Override
   public int skipBytes(int n) throws IOException {
      this.pos += n;
      return n;
   }

   @Override
   public boolean readBoolean() throws IOException {
      return this.read() != 0;
   }

   @Override
   public byte readByte() throws IOException {
      return (byte)this.read();
   }

   @Override
   public int readUnsignedByte() throws IOException {
      return this.read();
   }

   @Override
   public short readShort() throws IOException {
      return this.bigEndian ? (short)((this.read() << 8) + this.read()) : (short)(this.read() + (this.read() << 8));
   }

   @Override
   public int readUnsignedShort() throws IOException {
      return this.bigEndian ? (this.read() << 8) + this.read() : this.read() + (this.read() << 8);
   }

   @Override
   public char readChar() throws IOException {
      return this.bigEndian ? (char)((this.read() << 8) + this.read()) : (char)(this.read() + (this.read() << 8));
   }

   @Override
   public int readInt() throws IOException {
      return this.bigEndian
         ? (this.read() << 24) + (this.read() << 16) + (this.read() << 8) + (this.read() << 0)
         : (this.read() << 0) + (this.read() << 8) + (this.read() << 16) + (this.read() << 24);
   }

   @Override
   public long readLong() throws IOException {
      return this.bigEndian ? ((long)this.readInt() << 32) + (this.readInt() & 4294967295L) : this.readInt() & 4294967295L + this.readInt() << 32;
   }

   @Override
   public float readFloat() throws IOException {
      return Float.intBitsToFloat(this.readInt());
   }

   @Override
   public double readDouble() throws IOException {
      return Double.longBitsToDouble(this.readLong());
   }

   @Override
   public String readLine() throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public String readUTF() throws IOException {
      int utflen = this.readUnsignedShort();
      StringBuilder str = new StringBuilder(utflen);
      int count = 0;

      while (count < utflen) {
         int c = this.read();
         switch (c >> 4) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
               count++;
               str.append((char)c);
               break;
            case 8:
            case 9:
            case 10:
            case 11:
            default:
               throw new UTFDataFormatException();
            case 12:
            case 13:
               count += 2;
               if (count > utflen) {
                  throw new UTFDataFormatException();
               }

               int c2 = this.read();
               if ((c2 & 192) != 128) {
                  throw new UTFDataFormatException();
               }

               str.append((char)((c & 31) << 6 | c2 & 63));
               break;
            case 14:
               count += 3;
               if (count > utflen) {
                  throw new UTFDataFormatException();
               }

               int c2 = this.read();
               int c3 = this.read();
               if ((c2 & 192) != 128 || (c3 & 192) != 128) {
                  throw new UTFDataFormatException();
               }

               str.append((char)((c & 15) << 12 | (c2 & 63) << 6 | (c3 & 63) << 0));
         }
      }

      return str.toString();
   }

   public boolean startsWith(int b) {
      return this.length < 1 ? false : this.buffer[0] == (byte)(b & 0xFF);
   }

   public boolean startsWith(byte[] b) {
      if (this.length < b.length) {
         return false;
      }

      byte[] buf = this.buffer;

      for (int i = 0; i < b.length; i++) {
         if (buf[i] != b[i]) {
            return false;
         }
      }

      return true;
   }

   public boolean endsWith(int b) {
      return this.length < 1 ? false : this.buffer[this.buffer.length - 1] == (byte)(b & 0xFF);
   }

   public boolean endsWith(byte[] b) {
      if (this.length < b.length) {
         return false;
      }

      byte[] buf = this.buffer;
      int index = this.length - b.length;

      for (int i = 0; i < b.length; i++) {
         if (buf[index++] != b[i]) {
            return false;
         }
      }

      return true;
   }

   public int indexOf(int b) {
      return this.indexOf(b, 0);
   }

   public int indexOf(int b, int fromIndex) {
      if (this.length < 0) {
         return -1;
      }

      if (fromIndex < 0) {
         fromIndex = 0;
      }

      for (int i = fromIndex; i < this.length; i++) {
         if (this.buffer[i] == b) {
            return i;
         }
      }

      return -1;
   }

   public int indexOf(byte[] b) {
      return this.indexOf(b, 0);
   }

   public int indexOf(byte[] b, int fromIndex) {
      byte b0 = b[0];
      if (this.length < 0) {
         return -1;
      }

      if (fromIndex < 0) {
         fromIndex = 0;
      }

      int len = this.length - b.length + 1;

      for (int i = fromIndex; i < len; i++) {
         if (this.buffer[i] == b0) {
            boolean match = true;

            for (int j = 1; j < b.length; j++) {
               if (b[j] != this.buffer[i + j]) {
                  match = false;
                  break;
               }
            }

            if (match) {
               return i;
            }
         }
      }

      return -1;
   }

   public int lastIndexOf(int b) {
      return this.lastIndexOf(b, this.length - 1);
   }

   public int lastIndexOf(int b, int fromIndex) {
      if (this.length < 0) {
         return -1;
      }

      if (fromIndex < 0) {
         return -1;
      }

      if (fromIndex >= this.length) {
         fromIndex = this.length - 1;
      }

      for (int i = fromIndex; i >= 0; i--) {
         if (this.buffer[i] == b) {
            return i;
         }
      }

      return -1;
   }

   public int lastIndexOf(byte[] b) {
      return this.lastIndexOf(b, this.length - 1);
   }

   public int lastIndexOf(byte[] b, int fromIndex) {
      byte b0 = b[0];
      if (this.length < 0) {
         return -1;
      }

      if (fromIndex < 0) {
         return -1;
      }

      if (fromIndex >= this.length) {
         fromIndex = this.length - 1;
      }

      int start = Math.min(fromIndex, this.length - b.length + 1);

      for (int i = start; i >= 0; i--) {
         if (this.buffer[i] == b0) {
            boolean match = true;

            for (int j = 1; j < b.length; j++) {
               if (b[j] != this.buffer[i + j]) {
                  match = false;
                  break;
               }
            }

            if (match) {
               return i;
            }
         }
      }

      return -1;
   }

   public void setLength(int newLen) {
      this.length = newLen;
   }

   public void setBuffer(byte[] newBuf) {
      this.buffer = newBuf;
   }

   public void dump() {
      ByteArrayUtil.hexDump(this.buffer, 0, this.length);
   }

   public String dumpToString() {
      StringWriter sout = new StringWriter();
      PrintWriter out = new PrintWriter(sout);
      ByteArrayUtil.hexDump(out, this.buffer, 0, this.length);
      out.flush();
      return sout.toString();
   }
}
