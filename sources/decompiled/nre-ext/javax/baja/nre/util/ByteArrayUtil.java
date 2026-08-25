package javax.baja.nre.util;

import java.io.PrintWriter;

public class ByteArrayUtil {
   public static byte[] clone(byte[] a) {
      byte[] clone = new byte[a.length];
      System.arraycopy(a, 0, clone, 0, a.length);
      return clone;
   }

   public static void copy(byte[] source, byte[] target) {
      System.arraycopy(source, 0, target, 0, source.length);
   }

   public static boolean equals(byte[] a1, byte[] a2) {
      int len = a1.length;
      if (len != a2.length) {
         return false;
      }

      for (int i = 0; i < len; i++) {
         if (a1[i] != a2[i]) {
            return false;
         }
      }

      return true;
   }

   public static void memset(byte[] buf, byte value) {
      buf[0] = value;

      for (int i = 1; i < buf.length; i += i) {
         System.arraycopy(buf, 0, buf, i, buf.length - i < i ? buf.length - i : i);
      }
   }

   public static int readUnsignedByte(byte[] buf, int offset) {
      return buf[offset] & 0xFF;
   }

   public static short readShort(byte[] buf, int offset) {
      return (short)readUnsignedShort(buf, offset);
   }

   public static int readUnsignedShort(byte[] buf, int offset) {
      int b0 = readUnsignedByte(buf, offset + 0);
      int b1 = readUnsignedByte(buf, offset + 1);
      return (b0 << 8) + (b1 << 0);
   }

   public static int readInt(byte[] buf, int offset) {
      int b0 = readUnsignedByte(buf, offset + 0);
      int b1 = readUnsignedByte(buf, offset + 1);
      int b2 = readUnsignedByte(buf, offset + 2);
      int b3 = readUnsignedByte(buf, offset + 3);
      return (b0 << 24) + (b1 << 16) + (b2 << 8) + (b3 << 0);
   }

   public static long readLong(byte[] buf, int offset) {
      return ((long)readInt(buf, offset) << 32) + (readInt(buf, offset + 4) & 4294967295L);
   }

   public static int writeByte(byte[] buf, int offset, int value) {
      buf[offset] = (byte)(value & 0xFF);
      return offset + 1;
   }

   public static int writeShort(byte[] buf, int offset, int value) {
      buf[offset + 0] = (byte)(value >>> 8 & 0xFF);
      buf[offset + 1] = (byte)(value >>> 0 & 0xFF);
      return offset + 2;
   }

   public static int writeInt(byte[] buf, int offset, int value) {
      buf[offset + 0] = (byte)(value >>> 24 & 0xFF);
      buf[offset + 1] = (byte)(value >>> 16 & 0xFF);
      buf[offset + 2] = (byte)(value >>> 8 & 0xFF);
      buf[offset + 3] = (byte)(value >>> 0 & 0xFF);
      return offset + 4;
   }

   public static int writeLong(byte[] buf, int offset, long value) {
      buf[offset + 0] = (byte)(value >>> 56 & 255L);
      buf[offset + 1] = (byte)(value >>> 48 & 255L);
      buf[offset + 2] = (byte)(value >>> 40 & 255L);
      buf[offset + 3] = (byte)(value >>> 32 & 255L);
      buf[offset + 4] = (byte)(value >>> 24 & 255L);
      buf[offset + 5] = (byte)(value >>> 16 & 255L);
      buf[offset + 6] = (byte)(value >>> 8 & 255L);
      buf[offset + 7] = (byte)(value >>> 0 & 255L);
      return offset + 8;
   }

   public static String toHexString(byte[] b) {
      return b == null ? "null" : toHexString(b, 0, b.length, "");
   }

   public static String toHexString(byte[] b, String delimiter) {
      return b == null ? "null" : toHexString(b, 0, b.length, delimiter);
   }

   public static String toHexString(byte[] b, int off, int len) {
      return b == null ? "null" : toHexString(b, off, len, "");
   }

   public static String toHexString(byte[] b, int off, int len, String delimiter) {
      boolean first = true;
      if (b == null) {
         return "null";
      }

      if (b.length == 0) {
         return "";
      }

      if (off >= 0 && len >= 0 && off < b.length && off + len <= b.length) {
         if (len == 0) {
            return "";
         }

         StringBuilder s = new StringBuilder();

         for (int i = off; i < off + len; i++) {
            if (!first) {
               s.append(delimiter);
            }

            s.append(TextUtil.byteToHexString(b[i] & 255));
            first = false;
         }

         return s.toString();
      } else {
         throw new IllegalArgumentException();
      }
   }

   public static byte[] hexStringToBytes(String hex) {
      byte[] b = new byte[hex.length() / 2];

      for (int i = 0; i < b.length; i++) {
         b[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
      }

      return b;
   }

   public static void hexDump(byte[] b) {
      PrintWriter out = new PrintWriter(System.out);
      hexDump("", out, b, 0, b.length);
      out.flush();
   }

   public static void hexDump(String linePrefix, byte[] b) {
      PrintWriter out = new PrintWriter(System.out);
      hexDump(linePrefix, out, b, 0, b.length);
      out.flush();
   }

   public static void hexDump(byte[] b, int offset, int length) {
      PrintWriter out = new PrintWriter(System.out);
      hexDump("", out, b, offset, length);
      out.flush();
   }

   public static void hexDump(String linePrefix, byte[] b, int offset, int length) {
      PrintWriter out = new PrintWriter(System.out);
      hexDump(linePrefix, out, b, offset, length);
      out.flush();
   }

   public static void hexDump(PrintWriter out, byte[] b, int offset, int length) {
      hexDump("", out, b, offset, length);
   }

   public static void hexDump(String linePrefix, PrintWriter out, byte[] b, int offset, int length) {
      int rowLen = 0;
      byte[] row = new byte[16];
      int i = 0;

      while (i < length) {
         rowLen = Math.min(16, length - i);
         System.arraycopy(b, offset + i, row, 0, rowLen);
         out.print(linePrefix);
         String off = Integer.toHexString(i + offset);
         out.print(TextUtil.padLeft(off, 3));
         out.print(':');

         for (int j = 0; j < 16; j++) {
            if (j % 4 == 0) {
               out.print(' ');
            }

            if (j >= rowLen) {
               out.print("  ");
            } else {
               out.print(TextUtil.byteToHexString(row[j] & 255));
            }
         }

         out.print("  ");

         for (int j = 0; j < rowLen; j++) {
            out.print(TextUtil.byteToChar(row[j] & 255, '.'));
         }

         out.println();
         i += rowLen;
      }
   }
}
