package com.honeywell.easybinding.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BIFile;
import javax.baja.gx.BImage;
import javax.baja.license.Feature;
import javax.baja.naming.BLocalHost;

public class KitpxUtils {
   private static Feature a;
   private static int b;
   private static boolean c;
   private static List<String> d;
   private static Logger e;
   private static final String[] z;

   private KitpxUtils() {
   }

   private static void a() {
      byte[] var0 = z[1].getBytes(StandardCharsets.UTF_8);
      b = var0[var0.length - 5] + var0.length;
   }

   public static boolean getIsEbFeature() {
      return c;
   }

   public static void setLicense(Feature var0) {
      try {
         if (a == null) {
            a = var0;
            c = true;
         }
      } catch (Exception var3) {
         Exception var1 = var3;

         try {
            if (getLogger().isLoggable(Level.FINER)) {
               getLogger().log(Level.FINER, var1.getMessage(), (Throwable)var1);
            }
         } catch (Exception var2) {
            throw var2;
         }
      }
   }

   public static boolean isInIgnoreList(String var0) {
      boolean var1 = false;

      for (String var3 : d) {
         if (var0.toLowerCase(Locale.ROOT).contains(var3) || var0.endsWith(z[4])) {
            var1 = true;
            break;
         }
      }

      return var1;
   }

   public static BImage getPlainImageOfBytes(BImage var0) {
      if (var0.getOrdList().size() > 0 && !var0.getOrdList().get(0).toString().endsWith(z[2])) {
         BIFile var1 = (BIFile)var0.getOrdList().get(0).resolve(BLocalHost.INSTANCE).get();
         return BImage.make(readFileBytes(var1, false).getFileBytes());
      } else {
         return var0;
      }
   }

   public static byte[] getPlainBytesOfImageObject(BImage var0) {
      byte[] var1 = new byte[0];

      try {
         label25: {
            try {
               if (var0.getOrdList().size() > 0 && !var0.getOrdList().get(0).toString().endsWith(z[0])) {
                  break label25;
               }
            } catch (Exception var3) {
               throw var3;
            }

            return var1;
         }

         BIFile var2 = (BIFile)var0.getOrdList().get(0).resolve(BLocalHost.INSTANCE).get();
         return readFileBytes(var2, false).getFileBytes();
      } catch (Exception var4) {
         checkAndLogError(Level.FINER, var4);
         return var1;
      }
   }

   public static FileByteData readFileBytes(BIFile param0, boolean param1) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.RuntimeException: parsing failure!
      //   at org.jetbrains.java.decompiler.modules.decompiler.decompose.DomHelper.parseGraph(DomHelper.java:211)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:166)
      //
      // Bytecode:
      // 00: bipush 0
      // 01: newarray 8
      // 03: astore 2
      // 04: bipush 0
      // 05: istore 3
      // 06: bipush 0
      // 07: istore 4
      // 09: aload 0
      // 0a: invokeinterface javax/baja/file/BIFile.getFilePath ()Ljavax/baja/file/FilePath; 1
      // 0f: invokevirtual javax/baja/file/FilePath.toString ()Ljava/lang/String;
      // 12: invokestatic com/honeywell/easybinding/util/KitpxUtils.isInIgnoreList (Ljava/lang/String;)Z
      // 15: istore 3
      // 16: aload 0
      // 17: invokeinterface javax/baja/file/BIFile.read ()[B 1
      // 1c: astore 2
      // 1d: aload 2
      // 1e: invokestatic com/honeywell/easybinding/util/KitpxUtils.a ([B)Z
      // 21: istore 4
      // 23: goto 48
      // 26: astore 5
      // 28: getstatic com/honeywell/easybinding/util/KitpxUtils.e Ljava/util/logging/Logger;
      // 2b: getstatic java/util/logging/Level.FINER Ljava/util/logging/Level;
      // 2e: invokevirtual java/util/logging/Logger.isLoggable (Ljava/util/logging/Level;)Z
      // 31: ifeq 48
      // 34: invokestatic com/honeywell/easybinding/util/KitpxUtils.getLogger ()Ljava/util/logging/Logger;
      // 37: getstatic java/util/logging/Level.FINER Ljava/util/logging/Level;
      // 3a: aload 5
      // 3c: invokevirtual java/lang/Exception.getMessage ()Ljava/lang/String;
      // 3f: aload 5
      // 41: invokevirtual java/util/logging/Logger.log (Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/Throwable;)V
      // 44: goto 48
      // 47: athrow
      // 48: iload 3
      // 49: ifne 55
      // 4c: iload 4
      // 4e: ifeq 5a
      // 51: goto 55
      // 54: athrow
      // 55: aload 2
      // 56: goto 6a
      // 59: athrow
      // 5a: iload 1
      // 5b: ifeq 66
      // 5e: aload 2
      // 5f: invokestatic com/honeywell/easybinding/util/KitpxUtils.b ([B)[B
      // 62: goto 6a
      // 65: athrow
      // 66: aload 2
      // 67: invokestatic com/honeywell/easybinding/util/KitpxUtils.decrypt ([B)[B
      // 6a: astore 5
      // 6c: new com/honeywell/easybinding/util/FileByteData
      // 6f: dup
      // 70: aload 5
      // 72: iload 4
      // 74: invokespecial com/honeywell/easybinding/util/FileByteData.<init> ([BZ)V
      // 77: areturn
   }

   private static boolean a(byte[] var0) {
      boolean var1 = false;
      if (var0.length > 1) {
         var1 = var0[var0.length - 1] == 127 && var0[var0.length - 2] == 127;
      }

      return var1;
   }

   private static byte[] b(byte[] var0) {
      try {
         EncryptDecrypt var1 = EncryptDecrypt.getInstance(a);

         try {
            if (var1 == null || a(var0)) {
               return var0;
            }
         } catch (Exception var2) {
            throw var2;
         }

         return var1.encrypt(var0);
      } catch (Exception var3) {
         checkAndLogError(Level.INFO, var3);
         return var0;
      }
   }

   public static byte[] decrypt(byte[] var0) {
      try {
         EncryptDecrypt var1 = EncryptDecrypt.getInstance(a);
         if (var1 != null) {
            return var1.decrypt(var0);
         }
      } catch (Exception var2) {
         checkAndLogError(Level.INFO, var2);
      }

      return var0;
   }

   public static void checkAndLogError(Level var0, Exception var1) {
      Logger var2 = getLogger();
      if (var2.isLoggable(var0)) {
         var2.log(var0, var1.getMessage(), (Throwable)var1);
      }
   }

   public static Logger getLogger() {
      return e;
   }

   public static boolean isSvg(BImage var0) {
      boolean var1 = false;
      if (var0.getOrdList().size() > 0) {
         var1 = var0.getOrdList().get(0).toString().toLowerCase(Locale.ROOT).endsWith(z[3]);
      }

      return var1;
   }

   public static String getFileExtension(BImage var0) {
      String var1 = "";

      try {
         try {
            if (var0.getOrdList().size() <= 0 || isInIgnoreList(var0.getOrdList().get(0).toString())) {
               return var1;
            }
         } catch (Exception var3) {
            throw var3;
         }

         BIFile var2 = (BIFile)var0.getOrdList().get(0).resolve(BLocalHost.INSTANCE).get();
         var1 = var2.getExtension().toLowerCase(Locale.ROOT);
      } catch (Exception var4) {
         checkAndLogError(Level.FINER, var4);
      }

      return var1;
   }

   public static String getFileExtension(BIFile var0) {
      String var1 = "";

      try {
         if (var0 != null) {
            var1 = var0.getExtension().toLowerCase(Locale.ROOT);
         }
      } catch (Exception var3) {
         checkAndLogError(Level.FINER, var3);
      }

      return var1;
   }

   static {
      String[] var10000 = new String[5];
      String[] var10001 = var10000;
      int var10002 = 0;
      String var10003 = "0{OR^z=";
      int var10004 = -1;

      while (true) {
         char[] var26 = var10003.toCharArray();
         int var10006 = var26.length;
         char[] var33 = var26;
         var10004 = var10006;

         for (int var0 = 0; var10004 > var0; var0++) {
            char var10008 = var33[var0];
            byte var10009;
            switch (var0 % 5) {
               case 0:
                  var10009 = 31;
                  break;
               case 1:
                  var10009 = 18;
                  break;
               case 2:
                  var10009 = 34;
                  break;
               case 3:
                  var10009 = 51;
                  break;
               default:
                  var10009 = 57;
            }

            var33[var0] = (char)(var10008 ^ var10009);
         }

         String var38 = new String(var33).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var38;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "0{OR^z=";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var38;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "1aTT";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var38;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "0{OR^z=";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var38;
               z = var10000;
               a = null;
               b = 0;
               c = false;
               d = new ArrayList<>();
               char[] var4 = "zsQJ{v|FZWx".toCharArray();
               int var18 = var4.length;
               char[] var10 = var4;
               int var5 = var18;

               for (int var1 = 0; var5 > var1; var1++) {
                  char var35 = var10[var1];
                  byte var39;
                  switch (var1 % 5) {
                     case 0:
                        var39 = 31;
                        break;
                     case 1:
                        var39 = 18;
                        break;
                     case 2:
                        var39 = 34;
                        break;
                     case 3:
                        var39 = 51;
                        break;
                     default:
                        var39 = 57;
                  }

                  var10[var1] = (char)(var35 ^ var39);
               }

               var10003 = new String(var10).intern();
               byte var7 = -1;
               e = Logger.getLogger(var10003);
               List var3 = d;
               String var8 = "r}FFUz(\r\u001cP|}L@\u0016";
               var10002 = (byte)-1;

               while (true) {
                  char[] var14 = var8.toCharArray();
                  var10004 = var14.length;
                  char[] var23 = var14;
                  var10002 = var10004;

                  for (int var2 = 0; var10002 > var2; var2++) {
                     char var40 = var23[var2];
                     byte var10007;
                     switch (var2 % 5) {
                        case 0:
                           var10007 = 31;
                           break;
                        case 1:
                           var10007 = 18;
                           break;
                        case 2:
                           var10007 = 34;
                           break;
                        case 3:
                           var10007 = 51;
                           break;
                        default:
                           var10007 = 57;
                     }

                     var23[var2] = (char)(var40 ^ var10007);
                  }

                  String var32 = new String(var23).intern();
                  switch (var10002) {
                     case 0:
                        var3.add(var32);
                        a();
                        return;
                     default:
                        var3.add(var32);
                        var3 = d;
                        var8 = "r}FFUz(\r\u001c\\~a[QPqvK]^0";
                        var10002 = (byte)0;
                  }
               }
            default:
               var10001[var10002] = var38;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "w}LvXlk`ZW{{LT";
               var10004 = 0;
         }
      }
   }
}
