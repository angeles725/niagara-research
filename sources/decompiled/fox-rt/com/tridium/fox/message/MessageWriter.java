package com.tridium.fox.message;

import java.io.IOException;
import java.io.OutputStream;

public class MessageWriter {
   public int indent = 0;
   public boolean isDebug;
   private OutputStream out;

   public MessageWriter(OutputStream out, boolean isDebug) {
      this.out = out;
      this.isDebug = isDebug;
   }

   public MessageWriter(OutputStream out) {
      this.out = out;
   }

   public final MessageWriter write(int c) throws IOException {
      this.out.write(c);
      return this;
   }

   public final MessageWriter write(byte[] buf, int off, int len) throws IOException {
      if (this.isDebug) {
         this.writeDebug(buf, off, len);
      } else {
         this.out.write(buf, off, len);
      }

      return this;
   }

   public final MessageWriter write(byte[] buf) throws IOException {
      if (this.isDebug) {
         this.writeDebug(buf, 0, buf.length);
      } else {
         this.out.write(buf, 0, buf.length);
      }

      return this;
   }

   private void writeDebug(byte[] buf, int off, int len) throws IOException {
      int dumpLen = Math.min(len, 30);

      for (int i = 0; i < dumpLen; i++) {
         int c = buf[i];
         if (c == 10) {
            this.write(92).write(110);
         } else {
            if (c < 32 || c > 127) {
               c = 63;
            }

            this.write(c);
         }
      }

      if (dumpLen < len) {
         this.write(46).write(46).write(46);
      }
   }

   public final void writeIndent() throws IOException {
      for (int i = 0; i < this.indent; i++) {
         this.out.write(32);
      }
   }

   public final MessageWriter writeName(String name) throws IOException {
      try {
         int len = name.length();

         for (int i = 0; i < len; i++) {
            this.out.write(name.charAt(i));
         }
      } catch (NullPointerException var4) {
         this.write(110).write(117).write(108).write(108);
      }

      return this;
   }

   public final MessageWriter writeInt(int value) throws IOException {
      return this.writeName(Integer.toString(value));
   }

   public final MessageWriter writeLong(long value) throws IOException {
      return this.writeName(Long.toString(value));
   }

   public final MessageWriter writeHex(int value) throws IOException {
      return this.writeName(Integer.toHexString(value));
   }

   public final MessageWriter writeHexLong(long value) throws IOException {
      return this.writeName(Long.toHexString(value));
   }

   public final MessageWriter writeSafe(String text) throws IOException {
      if (text == null) {
         this.writeName("#null;");
         return this;
      } else {
         int len = text.length();

         for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c >= ' ' && c <= 127 && c != '#') {
               this.write(c);
            } else {
               this.write(35).writeHex(c).write(59);
            }
         }

         return this;
      }
   }

   public final void flush() throws IOException {
      this.out.flush();
   }

   public final void close() throws IOException {
      this.out.close();
   }
}
