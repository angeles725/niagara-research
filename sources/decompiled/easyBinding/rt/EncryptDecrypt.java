package com.honeywell.easybinding.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.logging.Level;
import javax.baja.license.Feature;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptDecrypt {
   private String a;
   private static EncryptDecrypt b;
   private static Key c;
   private static final String[] z;

   private EncryptDecrypt(String var1) {
      StringBuilder var2 = new StringBuilder(var1);
      if (var1.length() < 16) {
         for (int var3 = var1.length(); var3 < 16; var3++) {
            String var4 = String.valueOf(var3);
            var2.append(var4.substring(var4.length() - 1));
         }
      }

      this.a = var2.toString();
   }

   public static EncryptDecrypt getInstance(Feature var0) {
      if (b == null && var0 != null) {
         b = new EncryptDecrypt(var0.getFeatureName());
         b.a();
      }

      return b;
   }

   public static EncryptDecrypt getInstance() {
      return b;
   }

   private Key a() {
      if (c == null) {
         c = new SecretKeySpec(this.a.getBytes(StandardCharsets.UTF_8), z[1]);
      }

      return c;
   }

   public byte[] encrypt(byte[] var1) throws Exception {
      Cipher var2 = Cipher.getInstance(z[0]);
      var2.init(1, c);
      byte[] var3 = var2.doFinal(var1);
      return this.a(var3);
   }

   private byte[] a(byte[] var1) {
      byte[] var2 = new byte[]{127, 127};
      return addAll(var1, var2);
   }

   public static byte[] addAll(byte[] var0, byte[] var1) {
      byte[] var2 = Arrays.copyOf(var0, var0.length + var1.length);
      System.arraycopy(var1, 0, var2, var0.length, var1.length);
      return var2;
   }

   public static byte[] getSubArray(byte[] var0) {
      byte[] var1 = new byte[var0.length - 2];

      for (int var2 = 0; var2 < var0.length - 2; var2++) {
         var1[var2] = var0[var2];
      }

      return var1;
   }

   public byte[] decrypt(byte[] var1) {
      byte[] var2 = new byte[0];

      try {
         var1 = getSubArray(var1);
         Cipher var3 = Cipher.getInstance(z[2]);
         var3.init(2, c);
         var2 = var3.doFinal(var1);
      } catch (Exception var4) {
         KitpxUtils.checkAndLogError(Level.FINER, var4);
      }

      return var2;
   }

   static {
      String[] var10000 = new String[3];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "jX\u000f";
      int var10004 = -1;

      while (true) {
         char[] var3 = var10003.toCharArray();
         int var10006 = var3.length;
         char[] var6 = var3;
         var10004 = var10006;

         for (int var0 = 0; var10004 > var0; var0++) {
            char var10008 = var6[var0];
            byte var10009;
            switch (var0 % 5) {
               case 0:
                  var10009 = 43;
                  break;
               case 1:
                  var10009 = 29;
                  break;
               case 2:
                  var10009 = 92;
                  break;
               case 3:
                  var10009 = 109;
                  break;
               default:
                  var10009 = 122;
            }

            var6[var0] = (char)(var10008 ^ var10009);
         }

         String var10 = new String(var6).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "jX\u000f";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var10;
               z = var10000;
               b = null;
               c = null;
               return;
            default:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "jX\u000f";
               var10004 = 0;
         }
      }
   }
}
