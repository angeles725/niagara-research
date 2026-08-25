package com.tridium.crypto.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class FriendlyPemReader extends Reader {
   private BufferedReader buffer;
   private StringBuffer outBuffer = new StringBuffer();

   public FriendlyPemReader(Reader reader) {
      if (reader instanceof BufferedReader) {
         this.buffer = (BufferedReader)reader;
      } else {
         this.buffer = new BufferedReader(reader);
      }
   }

   @Override
   public int read(char[] cbuf, int off, int len) throws IOException {
      String line = null;

      while (this.outBuffer.length() < len) {
         line = this.buffer.readLine();
         if (line == null) {
            break;
         }

         if (line.trim().length() > 0) {
            this.outBuffer.append(line).append("\n");
         }
      }

      int min = Math.min(this.outBuffer.length(), len);
      if (min <= 0) {
         return -1;
      }

      this.outBuffer.getChars(0, min, cbuf, off);
      this.outBuffer.delete(0, min);
      return min;
   }

   @Override
   public void close() throws IOException {
      this.buffer.close();
   }
}
