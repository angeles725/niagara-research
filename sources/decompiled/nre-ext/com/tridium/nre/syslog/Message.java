package com.tridium.nre.syslog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Message {
   private static final byte NON_ASCII_SYMBOL = 46;
   private static final byte LF_SYMBOL = 92;
   private final ByteArrayOutputStream value = new ByteArrayOutputStream();

   public int getLength() {
      return this.value.size();
   }

   public byte[] getBytes() {
      return this.value.toByteArray();
   }

   public void print(String s) throws IOException {
      byte[] b = s.getBytes(StandardCharsets.UTF_8);

      for (int i = 0; i < b.length; i++) {
         byte c = b[i];
         if (c < 32 || c > 126) {
            if (c == 10) {
               b[i] = 92;
            } else {
               b[i] = 46;
            }
         }
      }

      this.value.write(b);
   }

   @Override
   public String toString() {
      return new String(this.value.toByteArray(), StandardCharsets.UTF_8);
   }
}
