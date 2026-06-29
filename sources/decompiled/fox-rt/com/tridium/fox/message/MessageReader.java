package com.tridium.fox.message;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;

public class MessageReader {
   private static final int CM_NAME = 1;
   private static final int CM_DIGIT = 2;
   private static final int CM_HEX = 4;
   private static final byte[] charMap = new byte[256];
   private static final int HISTORY_SIZE = 160;
   private static final Logger LOGGER;
   private final InputStream in;
   private final String clientHint;
   private int pushBack = -1;
   private final byte[] history = new byte[160];
   private int historyIndex = -1;
   private long bytesRead;
   private long readLimit = Long.MAX_VALUE;

   public MessageReader(InputStream in) {
      this(in, null);
   }

   public MessageReader(InputStream in, String clientHint) {
      this.in = in;
      this.clientHint = clientHint;
   }

   public void setReadLimit(long readLimit) {
      this.resetBytesRead();
      this.readLimit = readLimit;
   }

   public long getBytesRead() {
      return this.bytesRead;
   }

   public void resetBytesRead() {
      this.bytesRead = 0L;
   }

   public final void assertReadLimit(int numBytes) throws ReadLimitExceededException {
      if (this.bytesRead + numBytes > this.readLimit) {
         throw new ReadLimitExceededException(this.readLimit, numBytes, this.bytesRead);
      }
   }

   public final int read() throws IOException {
      if (this.pushBack != -1) {
         int v = this.pushBack;
         this.pushBack = -1;
         return v;
      } else {
         this.assertReadLimit(1);
         int v = this.in.read();
         if (v < 0) {
            throw new EOFException("EOF");
         } else {
            this.bytesRead++;
            this.history[this.historyIndex = (this.historyIndex + 1) % 160] = (byte)v;
            return v;
         }
      }
   }

   public final void readFully(byte[] buf, int off, int len) throws IOException {
      this.assertReadLimit(len);
      this.read(buf, off, len);
   }

   public final byte[] readFully(int len) throws IOException {
      this.assertReadLimit(len);
      byte[] buf = new byte[len];
      this.read(buf, 0, len);
      return buf;
   }

   private final void read(byte[] buf, int off, int len) throws IOException {
      int n = 0;

      while (n < len) {
         int count = this.in.read(buf, off + n, len - n);
         if (count < 0) {
            throw this.error("EOF");
         }

         n += count;
      }

      this.bytesRead += n;
      this.history[this.historyIndex = (this.historyIndex + 1) % 160] = 98;
      this.history[this.historyIndex = (this.historyIndex + 1) % 160] = 108;
      this.history[this.historyIndex = (this.historyIndex + 1) % 160] = 111;
      this.history[this.historyIndex = (this.historyIndex + 1) % 160] = 98;
   }

   public final void pushBack(int pushBack) {
      if (this.pushBack != -1) {
         throw new IllegalStateException("Double push back");
      } else {
         this.pushBack = pushBack;
      }
   }

   public final String readName() throws IOException {
      StringBuilder s = new StringBuilder();

      while (true) {
         int c = this.read();
         if ((charMap[c] & 1) == 0) {
            if (s.length() == 0) {
               throw this.error("Expected name");
            } else {
               this.pushBack(c);
               return s.toString();
            }
         }

         s.append((char)c);
      }
   }

   public final String readTo(char stopChar) throws IOException {
      StringBuilder s = new StringBuilder();

      while (true) {
         int c = this.read();
         if (c == stopChar) {
            this.pushBack(c);
            return s.toString();
         }

         s.append((char)c);
      }
   }

   public final int readInt() throws IOException {
      int intValue = 0;
      boolean negative = false;
      int c = this.read();
      if ((charMap[c] & 2) != 0) {
         intValue = c - 48;
      } else {
         if (c != 45) {
            throw this.error("Expecting int");
         }

         negative = true;
      }

      while (true) {
         c = this.read();
         if ((charMap[c] & 2) == 0) {
            this.pushBack(c);
            if (negative) {
               intValue = -intValue;
            }

            return intValue;
         }

         intValue = intValue * 10 + (c - 48);
      }
   }

   public final long readLong() throws IOException {
      long longValue = 0L;
      boolean negative = false;
      int c = this.read();
      if ((charMap[c] & 2) != 0) {
         longValue = c - 48;
      } else {
         if (c != 45) {
            throw this.error("Expecting int");
         }

         negative = true;
      }

      while (true) {
         c = this.read();
         if ((charMap[c] & 2) == 0) {
            this.pushBack(c);
            if (negative) {
               longValue = -longValue;
            }

            return longValue;
         }

         longValue = longValue * 10L + (c - 48);
      }
   }

   public final int readHex() throws IOException {
      int intValue = 0;

      while (true) {
         int c = this.read();
         if ((charMap[c] & 4) == 0) {
            this.pushBack(c);
            return intValue;
         }

         if ((charMap[c] & 2) != 0) {
            intValue = (intValue << 4) + (c - 48);
         } else {
            intValue = (intValue << 4) + 10 + (c - 97);
         }
      }
   }

   public final long readHexLong() throws IOException {
      long longValue = 0L;

      while (true) {
         int c = this.read();
         if ((charMap[c] & 4) == 0) {
            this.pushBack(c);
            return longValue;
         }

         if ((charMap[c] & 2) != 0) {
            longValue = (longValue << 4) + (c - 48);
         } else {
            longValue = (longValue << 4) + 10L + (c - 97);
         }
      }
   }

   public final String readSafe() throws IOException {
      StringBuilder s = new StringBuilder();

      while (true) {
         int c = this.read();
         if (c < 32) {
            this.pushBack(c);
            return s.toString();
         }

         if (c == 35) {
            if (s.length() == 0) {
               int n = this.read();
               if (n == 110) {
                  this.consume(117);
                  this.consume(108);
                  this.consume(108);
                  this.consume(59);
                  return null;
               }

               this.pushBack(n);
            }

            int hex = this.readHex();
            this.consume(59);
            s.append((char)hex);
         } else {
            s.append((char)c);
         }
      }
   }

   public final void consume(String expected) throws IOException {
      int len = expected.length();

      for (int i = 0; i < len; i++) {
         this.consume(expected.charAt(i));
      }
   }

   public final void consume(int expected) throws IOException {
      int actual = this.read();
      if (actual == -1) {
         throw new EOFException();
      } else if (actual != expected) {
         throw this.error("invalid message, expected '" + this.toString(expected) + "', got '" + this.toString(actual) + "'");
      }
   }

   public final void close() throws IOException {
      this.in.close();
   }

   public IOException error(String msg) {
      if (LOGGER.isLoggable(Level.INFO)) {
         try (
            StringWriter writer = new StringWriter();
            PrintWriter pwriter = new PrintWriter(writer, true);
         ) {
            if (this.clientHint != null) {
               pwriter.println(msg + " (client hint: " + this.clientHint + ")");
            } else {
               pwriter.println(msg);
            }

            if (LOGGER.isLoggable(Level.FINEST)) {
               Exception stack = new Exception("Stack trace");
               stack.printStackTrace(pwriter);
            }

            if (LOGGER.isLoggable(Level.FINE)) {
               byte[] his = this.getHistory();
               pwriter.println("--- History ---");
               ByteArrayUtil.hexDump(pwriter, his, 0, his.length);
               byte[] fut = this.getFuture();
               if (fut.length > 0) {
                  pwriter.println("--- Future ---");
                  ByteArrayUtil.hexDump(pwriter, fut, 0, fut.length);
               }
            }

            pwriter.flush();
            LOGGER.info(writer.toString().trim());
         } catch (Exception var35) {
         }
      }

      return new IOException(msg);
   }

   public byte[] getHistory() {
      byte[] ordered = new byte[160];
      int end = this.historyIndex + 1;
      int left = 160 - end;

      for (int i = 0; i < left; i++) {
         ordered[i] = this.history[i + end];
      }

      for (int i = 0; i < end; i++) {
         ordered[i + left] = this.history[i];
      }

      return ordered;
   }

   public byte[] getFuture() {
      try {
         int available = this.in.available();
         int toRead = Math.min(available, 160);
         if (toRead > 0) {
            byte[] buf = new byte[toRead];

            for (int i = 0; i < buf.length; i++) {
               buf[i] = (byte)this.read();
            }

            return buf;
         }
      } catch (IOException var5) {
      }

      return new byte[0];
   }

   public String toString(int c) {
      if (c == 10) {
         return "\\n";
      } else {
         return c >= 32 && c <= 126 ? String.valueOf((char)c) : "0x" + Integer.toHexString(c);
      }
   }

   static {
      for (int i = 97; i <= 122; i++) {
         charMap[i] = 1;
      }

      for (int i = 65; i <= 90; i++) {
         charMap[i] = 1;
      }

      for (int i = 48; i <= 57; i++) {
         charMap[i] = 7;
      }

      for (int i = 97; i <= 102; i++) {
         charMap[i] = (byte)(charMap[i] | 4);
      }

      charMap[46] = 1;
      charMap[45] = 1;
      charMap[95] = 1;
      charMap[36] = 1;
      LOGGER = Logger.getLogger("fox.message");
   }
}
