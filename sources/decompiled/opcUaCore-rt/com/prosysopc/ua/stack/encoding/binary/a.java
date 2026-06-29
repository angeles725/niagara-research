package com.prosysopc.ua.stack.encoding.binary;

import java.io.ByteArrayOutputStream;

class a {
   static byte[] concat(byte[] var0, byte[] var1) {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      var2.write(var0, 0, var0.length);
      var2.write(var1, 0, var1.length);
      return var2.toByteArray();
   }

   static byte[] reverse(byte[] var0) {
      if (var0 != null && var0.length != 0) {
         byte[] var1 = new byte[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = var0[var0.length - 1 - var2];
         }

         return var1;
      } else {
         return var0;
      }
   }
}
