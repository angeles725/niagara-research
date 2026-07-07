package com.honeywell.easybinding.service;

import com.honeywell.easybinding.util.KitpxUtils;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BDirectory;
import javax.baja.file.BIFile;
import javax.baja.job.BSimpleJob;
import javax.baja.job.JobLogItem;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BUpgradeToEncryptedEasyWidgets extends BSimpleJob {
   public static final Type TYPE;
   private static final Logger a;
   private boolean b;
   private int c;
   private static final String[] z;

   public Type getType() {
      return TYPE;
   }

   public void run(Context var1) throws Exception {
      this.b = true;
      this.c = 0;
      BDirectory var2 = (BDirectory)BOrd.make(z[3]).resolve().get();

      try {
         this.a(z[4]);
         this.processAllPxFiles(var2);
         if (!this.b) {
            this.failed(new Exception(z[5]));
         }
      } catch (Exception var4) {
         throw var4;
      }

      String var3 = MessageFormat.format(z[2], this.c);
      a.info(var3);
      a.info(z[6]);
      this.a(var3);
   }

   public void processAllPxFiles(BDirectory param1) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.RuntimeException: parsing failure!
      //   at org.jetbrains.java.decompiler.modules.decompiler.decompose.DomHelper.parseGraph(DomHelper.java:211)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:166)
      //
      // Bytecode:
      // 00: aload 1
      // 01: invokevirtual javax/baja/file/BDirectory.listFiles ()[Ljavax/baja/file/BIFile;
      // 04: astore 2
      // 05: aload 2
      // 06: astore 3
      // 07: aload 3
      // 08: arraylength
      // 09: istore 4
      // 0b: bipush 0
      // 0c: istore 5
      // 0e: iload 5
      // 10: iload 4
      // 12: if_icmpge 95
      // 15: aload 3
      // 16: iload 5
      // 18: aaload
      // 19: astore 6
      // 1b: aload 6
      // 1d: invokeinterface javax/baja/file/BIFile.isDirectory ()Z 1
      // 22: ifne 3c
      // 25: aload 6
      // 27: instanceof javax/baja/file/types/text/BPxFile
      // 2a: ifeq 45
      // 2d: goto 31
      // 30: athrow
      // 31: aload 0
      // 32: aload 6
      // 34: invokespecial com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.a (Ljavax/baja/file/BIFile;)Z
      // 37: pop
      // 38: goto 45
      // 3b: athrow
      // 3c: aload 0
      // 3d: aload 6
      // 3f: checkcast javax/baja/file/BDirectory
      // 42: invokevirtual com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.processAllPxFiles (Ljavax/baja/file/BDirectory;)V
      // 45: goto 8f
      // 48: astore 7
      // 4a: aload 0
      // 4b: bipush 0
      // 4c: putfield com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.b Z
      // 4f: getstatic com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.a Ljava/util/logging/Logger;
      // 52: getstatic java/util/logging/Level.FINER Ljava/util/logging/Level;
      // 55: aload 6
      // 57: invokedynamic get (Ljavax/baja/file/BIFile;)Ljava/util/function/Supplier; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Ljava/lang/Object;, com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.b (Ljavax/baja/file/BIFile;)Ljava/lang/String;, ()Ljava/lang/String; ]
      // 5c: invokevirtual java/util/logging/Logger.log (Ljava/util/logging/Level;Ljava/util/function/Supplier;)V
      // 5f: getstatic com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.a Ljava/util/logging/Logger;
      // 62: getstatic java/util/logging/Level.FINER Ljava/util/logging/Level;
      // 65: aload 7
      // 67: invokevirtual java/lang/Exception.getMessage ()Ljava/lang/String;
      // 6a: aload 7
      // 6c: invokevirtual java/util/logging/Logger.log (Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/Throwable;)V
      // 6f: aload 0
      // 70: new java/lang/StringBuilder
      // 73: dup
      // 74: invokespecial java/lang/StringBuilder.<init> ()V
      // 77: getstatic com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.z [Ljava/lang/String;
      // 7a: bipush 0
      // 7b: aaload
      // 7c: invokevirtual java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
      // 7f: aload 6
      // 81: invokeinterface javax/baja/file/BIFile.getFileName ()Ljava/lang/String; 1
      // 86: invokevirtual java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
      // 89: invokevirtual java/lang/StringBuilder.toString ()Ljava/lang/String;
      // 8c: invokespecial com/honeywell/easybinding/service/BUpgradeToEncryptedEasyWidgets.a (Ljava/lang/String;)V
      // 8f: iinc 5 1
      // 92: goto 0e
      // 95: return
   }

   private boolean a(BIFile var1) {
      boolean var2 = false;
      String var3 = z[11];

      try {
         this.a(z[14] + var1.getFileName());
         byte[] var4 = new byte[var1.getInputStream().available()];
         var1.getInputStream().read(var4);
         var1.getInputStream().close();
         String var5 = new String(var4);
         var2 = var5.contains(var3);
         if (var2) {
            var5 = var5.replace(z[13], z[16]);
            var5 = var5.replace(z[17], z[9]);
            var5 = var5.replace(z[18], z[10]);
            var5 = var5.replace(z[8], z[7]);
            byte[] var6 = var5.getBytes(StandardCharsets.UTF_8);
            var1.getOutputStream().write(var6, 0, var6.length);
            this.c++;
            this.a(z[15] + var1.getFileName());
         }
      } catch (Exception var7) {
         KitpxUtils.checkAndLogError(Level.WARNING, var7);
         this.a(z[12] + var1.getFileName());
         this.b = false;
      }

      return var2;
   }

   private void a(String var1) {
      this.log().add(new JobLogItem(0, var1));
   }

   static {
      String[] var10000 = new String[19];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "\u000bl\u001f+\u0017)-\u0002(R8}\u0012&\u0006(-\u0006?R+d\u001a\"H";
      int var10004 = -1;

      while (true) {
         char[] var14 = var10003.toCharArray();
         int var10006 = var14.length;
         char[] var17 = var14;
         var10004 = var10006;

         for (int var0 = 0; var10004 > var0; var0++) {
            char var10008 = var17[var0];
            byte var10009;
            switch (var0 % 5) {
               case 0:
                  var10009 = 77;
                  break;
               case 1:
                  var10009 = 13;
                  break;
               case 2:
                  var10009 = 118;
                  break;
               case 3:
                  var10009 = 71;
                  break;
               default:
                  var10009 = 114;
            }

            var17[var0] = (char)(var10008 ^ var10009);
         }

         String var22 = new String(var17).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "6=\u000bg\u0017#n\u0004>\u00029h\u0012g\u00025-\u0006&\u0015(~V2\u0002*\u007f\u0017#\u0017)";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "+d\u001a\"H\u0013";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "\u001ey\u00175\u0006(iV\u0012\u0002*\u007f\u0017#\u001b#jV7\nm}\u0017 \u0017>-\u0010(\u0000mh\u0018$\u00004}\u0002\"\u0016mh\u00174\u000bm}\u0017+\u001e(y\u0005";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "\u000bl\u001f+\u0017)-\u0002(R8}\u00115\u0013)hV&\u001e!-\u0006?R\u001dl\u0011\"\u0001a-\u0015/\u0017.fV+\u001d*~V!\u001d?-\u001b(\u0000(-\u0012\"\u0006,d\u001a4";
               var10004 = 4;
               break;
            case 4:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "\u0007b\u0014g75h\u00152\u0006(i";
               var10004 = 5;
               break;
            case 5:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "qH\u00174\u000b\u0001l\u0014\"\u001emd\u001b&\u0015(0T*\u001d)x\u001a\"Hb\"";
               var10004 = 6;
               break;
            case 6:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 8;
               var10003 = "qA\u0017%\u0017!-\u001f*\u0013*hKe\u001f\"i\u0003+\u0017w\"Y";
               var10004 = 7;
               break;
            case 7:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 9;
               var10003 = "qH\u00174\u000b\u001dd\u00153\u0007?hV)\u0013 hKe\u0004,a\u0003\"; l\u0011\">,o\u0013+P";
               var10004 = 8;
               break;
            case 8:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "qH\u00174\u000b\u001dd\u00153\u0007?hV)\u0013 hKe\u0004,a\u0003\"; l\u0011\">,o\u0013+P";
               var10004 = 9;
               break;
            case 9:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 11;
               var10003 = "q]\u001f$\u00068\u007f\u0013g\u001c,`\u0013zP;l\u001a2\u0017\u0004`\u0017 \u0017\u0001l\u0014\"\u001eo";
               var10004 = 10;
               break;
            case 10:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 12;
               var10003 = "\u000bl\u001f+\u0017)-\u0002(R8}\u0012&\u0006(-\u0006?R+d\u001a\"H";
               var10004 = 11;
               break;
            case 11:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 13;
               var10003 = "q]\u001f$\u00068\u007f\u0013g\u001c,`\u0013zP;l\u001a2\u0017\u0004`\u0017 \u0017\u0001l\u0014\"\u001eo";
               var10004 = 12;
               break;
            case 12:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 14;
               var10003 = "8}\u00115\u0013)d\u0018 R=uL";
               var10004 = 13;
               break;
            case 13:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 15;
               var10003 = "+d\u0018.\u0001%h\u0012g\u0007=j\u0004&\u0016$c\u0011g\u000257";
               var10004 = 14;
               break;
            case 14:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 16;
               var10003 = "qH\u00174\u000b\u001dd\u00153\u0007?hV)\u0013 hKe\u0004,a\u0003\"; l\u0011\">,o\u0013+P";
               var10004 = 15;
               break;
            case 15:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 17;
               var10003 = "q]\u001f$\u00068\u007f\u0013g\u001c,`\u0013zP,a\u00175\u001f\u0004`\u0017 \u0017\u0001l\u0014\"\u001eo";
               var10004 = 16;
               break;
            case 16:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 18;
               var10003 = "q]\u001f$\u00068\u007f\u0013g\u001c,`\u0013zP\"{\u00135\u0000$i\u0013\u000e\u001f,j\u0013\u000b\u0013/h\u001ae";
               var10004 = 17;
               break;
            case 17:
               var10001[var10002] = var22;
               z = var10000;
               TYPE = Sys.loadType(BUpgradeToEncryptedEasyWidgets.class);
               char[] var2 = "\bl\u0005>0$c\u0012.\u001c*".toCharArray();
               int var9 = var2.length;
               char[] var6 = var2;
               int var3 = var9;

               for (int var1 = 0; var3 > var1; var1++) {
                  char var19 = var6[var1];
                  byte var23;
                  switch (var1 % 5) {
                     case 0:
                        var23 = 77;
                        break;
                     case 1:
                        var23 = 13;
                        break;
                     case 2:
                        var23 = 118;
                        break;
                     case 3:
                        var23 = 71;
                        break;
                     default:
                        var23 = 114;
                  }

                  var6[var1] = (char)(var19 ^ var23);
               }

               var10003 = new String(var6).intern();
               byte var5 = -1;
               a = Logger.getLogger(var10003);
               return;
            default:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "\u000bl\u001f+\u0017)-\u0002(R8}\u0012&\u0006(-\u0006?R+d\u001a\"H";
               var10004 = 0;
         }
      }
   }
}
