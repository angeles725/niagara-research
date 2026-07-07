package com.honeywell.easybinding.service;

import com.honeywell.easybinding.util.EbConfigUtil;
import com.honeywell.easybinding.util.EbLicenseUtil;
import com.honeywell.easybinding.util.KitpxUtils;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.control.BControlPoint;
import javax.baja.file.BDataFile;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.license.Feature;
import javax.baja.license.FeatureNotLicensedException;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XWriter;

@NiagaraType
public class BEasyBindingUtil extends BComponent {
   public static final Type TYPE;
   private static Logger a;
   private static final String[] b;
   private static final Set<String> c;
   private static final String[] z;

   public Type getType() {
      return TYPE;
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static void save(String var0, String var1, Context var2) {
      try {
         BStation var3 = Sys.getStation();
         BOrd var4 = BOrd.make(var1);
         BDataFile var5 = (BDataFile)var4.get(var3);
         byte[] var6 = var0.getBytes();
         OutputStream var7 = var5.getOutputStream();
         var7.write(var6);
         var7.close();
      } catch (Exception var8) {
         a(Level.WARNING, var8.getMessage(), var8);
      }
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static void saveTempFile(String var0, String var1, Context var2) {
      try {
         BStation var3 = Sys.getStation();
         BOrd var4 = BOrd.make(var1);
         OrdQuery[] var5 = var4.parse();
         FilePath var6 = (FilePath)var5[var5.length - 1];
         BFileSystem.INSTANCE.makeFile(var6);
         BDataFile var7 = (BDataFile)var4.get(var3);
         byte[] var8 = var0.getBytes();
         OutputStream var9 = var7.getOutputStream();
         var9.write(var8);
         var9.close();
      } catch (Exception var10) {
         a(Level.INFO, var10.getMessage(), var10);
      }
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static void removeTempFile(String var0, Context var1) {
      try {
         BStation var2 = Sys.getStation();
         BOrd var3 = BOrd.make(var0);
         OrdQuery[] var4 = var3.parse();
         FilePath var5 = (FilePath)var4[var4.length - 1];
         BFileSystem.INSTANCE.makeFile(var5);
         BDataFile var6 = (BDataFile)var3.get(var2);
         var6.delete();
      } catch (Exception var7) {
         a(Level.WARNING, var7.getMessage(), var7);
      }
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static JSONObject getNavTree(Context var0) {
      JSONObject var1 = new JSONObject();

      try {
         var1.put(z[16], a());
      } catch (Exception var3) {
         a(Level.WARNING, var3.getMessage(), var3);
      }

      return var1;
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static boolean checkFeatureLicense(Context var0) {
      return AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> {
         boolean var0x = false;

         try {
            String var1 = EbLicenseUtil.getBrandFromLicenseFile();
            Feature var2 = EbLicenseUtil.obtainEBFeatureFromLicense(var1);
            if (var2 != null) {
               var0x = true;
            }
         } catch (FeatureNotLicensedException var3) {
            var0x = false;
            a(Level.FINER, z[34], var3);
         }

         return var0x;
      }));
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static void logToStation(String var0, Context var1) {
      try {
         a.log(Level.INFO, var0);
      } catch (Exception var3) {
         a(Level.INFO, var3.getMessage(), var3);
      }
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static void savePointChooserConfig(Map var0, Context var1) {
      try {
         BIFile var2 = EbConfigUtil.getEBConfigFile();
         XElem var3 = XParser.make(var2.getInputStream()).parse(true);
         EbConfigUtil.setLastSearchText(var3, var0.get(z[25]).toString());
         EbConfigUtil.setLastBooleanCheckboxValue(var3, Boolean.valueOf(var0.get(z[31]).toString()));
         EbConfigUtil.setLastNumericCheckboxValue(var3, Boolean.valueOf(var0.get(z[28]).toString()));
         EbConfigUtil.setLastEnumCheckboxValue(var3, Boolean.valueOf(var0.get(z[33]).toString()));
         EbConfigUtil.setLastSecureBooleanCheckboxValue(var3, Boolean.valueOf(var0.get(z[32]).toString()));
         EbConfigUtil.setLastSecureNumericCheckboxValue(var3, Boolean.valueOf(var0.get(z[27]).toString()));
         EbConfigUtil.setLastSecureEnumCheckboxValue(var3, Boolean.valueOf(var0.get(z[30]).toString()));
         EbConfigUtil.setLastSelectedFolder(var3, var0.get(z[26]).toString());
         EbConfigUtil.setLastSelectedPoint(var3, var0.get(z[29]).toString());
         XWriter var4 = new XWriter(var2.getOutputStream());

         try {
            var3.write(var4);
         } finally {
            var4.flush();
            var4.close();
         }
      } catch (Exception var9) {
         a(Level.INFO, var9.getMessage(), var9);
      }
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static JSONObject getPointChooserConfig(Context var0) {
      JSONObject var1 = new JSONObject();

      try {
         BIFile var2 = EbConfigUtil.getEBConfigFile();
         XElem var3 = XParser.make(var2.getInputStream()).parse(true);
         var1.put(z[5], EbConfigUtil.getLastSearchText(var3));
         var1.put(z[2], EbConfigUtil.getLastBooleanCheckboxValue(var3));
         var1.put(z[8], EbConfigUtil.getLastNumericCheckboxValue(var3));
         var1.put(z[3], EbConfigUtil.getLastEnumCheckboxValue(var3));
         var1.put(z[7], EbConfigUtil.getLastSecureBooleanCheckboxValue(var3));
         var1.put(z[1], EbConfigUtil.getLastSecureNumericCheckboxValue(var3));
         var1.put(z[6], EbConfigUtil.getLastSecureEnumCheckboxValue(var3));
         var1.put(z[4], EbConfigUtil.getLastSelectedFolder(var3));
         var1.put(z[9], EbConfigUtil.getLastSelectedPoint(var3));
      } catch (Exception var4) {
         a(Level.WARNING, var4.getMessage(), var4);
      }

      return var1;
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static String getSupervisorIcon(Context var0) {
      String var1 = z[0];

      try {
         String var2 = EbLicenseUtil.getBrandFromLicenseFile();
         var1 = EbLicenseUtil.obtainBrandBasedIcon(var2);
      } catch (Exception var3) {
         a(Level.INFO, var3.getMessage(), var3);
      }

      return var1;
   }

   private static JSONArray a() {
      JSONArray var0 = new JSONArray();
      BStation var1 = Sys.getStation();
      BComponent[] var2 = var1.getChildComponents();

      for (BComponent var6 : var2) {
         if (b(var6)) {
            JSONObject var7 = d(var6);
            var0.put(var7);
         }
      }

      return var0;
   }

   private static boolean a(BComponent param0) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.RuntimeException: parsing failure!
      //   at org.jetbrains.java.decompiler.modules.decompiler.decompose.DomHelper.parseGraph(DomHelper.java:211)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:166)
      //
      // Bytecode:
      // 00: bipush 0
      // 01: istore 1
      // 02: aload 0
      // 03: instanceof javax/baja/control/BControlPoint
      // 06: ifeq 24
      // 09: aload 0
      // 0a: instanceof javax/baja/control/BStringWritable
      // 0d: ifne 24
      // 10: goto 14
      // 13: athrow
      // 14: aload 0
      // 15: instanceof javax/baja/control/BStringPoint
      // 18: ifne 24
      // 1b: goto 1f
      // 1e: athrow
      // 1f: bipush 1
      // 20: istore 1
      // 21: goto 53
      // 24: aload 0
      // 25: invokevirtual javax/baja/sys/BComponent.getChildComponents ()[Ljavax/baja/sys/BComponent;
      // 28: astore 2
      // 29: aload 2
      // 2a: astore 3
      // 2b: aload 3
      // 2c: arraylength
      // 2d: istore 4
      // 2f: bipush 0
      // 30: istore 5
      // 32: iload 5
      // 34: iload 4
      // 36: if_icmpge 53
      // 39: aload 3
      // 3a: iload 5
      // 3c: aaload
      // 3d: astore 6
      // 3f: aload 6
      // 41: invokestatic com/honeywell/easybinding/service/BEasyBindingUtil.a (Ljavax/baja/sys/BComponent;)Z
      // 44: istore 1
      // 45: iload 1
      // 46: ifeq 4d
      // 49: goto 53
      // 4c: athrow
      // 4d: iinc 5 1
      // 50: goto 32
      // 53: iload 1
      // 54: ireturn
   }

   private static boolean b(BComponent param0) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.RuntimeException: parsing failure!
      //   at org.jetbrains.java.decompiler.modules.decompiler.decompose.DomHelper.parseGraph(DomHelper.java:211)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:166)
      //
      // Bytecode:
      // 00: bipush 1
      // 01: istore 1
      // 02: aload 0
      // 03: instanceof javax/baja/driver/BDeviceNetwork
      // 06: ifeq 1b
      // 09: getstatic com/honeywell/easybinding/service/BEasyBindingUtil.c Ljava/util/Set;
      // 0c: aload 0
      // 0d: invokevirtual javax/baja/sys/BComponent.getType ()Ljavax/baja/sys/Type;
      // 10: invokeinterface javax/baja/sys/Type.toString ()Ljava/lang/String; 1
      // 15: invokeinterface java/util/Set.contains (Ljava/lang/Object;)Z 2
      // 1a: istore 1
      // 1b: iload 1
      // 1c: ifeq 2f
      // 1f: aload 0
      // 20: invokestatic com/honeywell/easybinding/service/BEasyBindingUtil.a (Ljavax/baja/sys/BComponent;)Z
      // 23: ifeq 2f
      // 26: goto 2a
      // 29: athrow
      // 2a: bipush 1
      // 2b: goto 30
      // 2e: athrow
      // 2f: bipush 0
      // 30: istore 1
      // 31: iload 1
      // 32: ireturn
   }

   private static JSONArray c(BComponent var0) {
      JSONArray var1 = new JSONArray();
      BComponent[] var2 = var0.getChildComponents();

      for (BComponent var6 : var2) {
         try {
            if (b(var6)) {
               var1.put(d(var6));
            }
         } catch (FeatureNotLicensedException var7) {
            throw var7;
         }
      }

      return var1;
   }

   private static JSONObject d(BComponent var0) {
      JSONObject var1 = new JSONObject();
      var1.put(z[23], var0.getName());
      var1.put(z[17], var0.getSlotPath().toString().replace(z[20], ""));
      String var2 = var0.getDisplayName(null);
      var1.put(z[22], var2);
      var1.put(z[18], var0.getType().toString());
      var1.put(z[24], var0 instanceof BControlPoint);
      var1.put(z[19], var0.getNavIcon().toString());
      var1.put(z[21], c(var0));
      return var1;
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static boolean checkEdgeController(Context var0) {
      return AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> {
         boolean var0x = true;

         try {
            var0x = EbLicenseUtil.isConnectedToEdgeController();
         } catch (Exception var2) {
            a(Level.FINER, z[15], var2);
         }

         return var0x;
      }));
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "unrestricted"
   )
   public static JSONObject getBase64Image(Map var0, Context var1) {
      return AccessController.doPrivileged((PrivilegedAction<JSONObject>)(() -> {
         JSONObject var1x = new JSONObject();

         try {
            var0.forEach((var1xx, var2) -> {
               BIFile var3x = (BIFile)BOrd.make(var2.toString()).resolve(BLocalHost.INSTANCE).get();
               byte[] var4 = new byte[0];

               try {
                  var4 = var3x.read();
               } catch (IOException var9) {
                  a(Level.FINER, z[13], var9);
               }

               var4 = KitpxUtils.decrypt(var4);
               String var5 = KitpxUtils.getFileExtension(var3x);
               boolean var6 = var5.toLowerCase(Locale.ROOT).equals(z[11]);
               String var7 = Base64.getEncoder().encodeToString(var4);

               String var10000;
               label25: {
                  try {
                     if (var6) {
                        var10000 = z[12];
                        break label25;
                     }
                  } catch (IOException var10) {
                     throw var10;
                  }

                  var10000 = String.format(z[14], var5);
               }

               String var8 = var10000;
               var1x.put(var1xx.toString(), var8 + var7);
            });
         } catch (Exception var3) {
            a(Level.FINER, z[10], var3);
         }

         return var1x;
      }));
   }

   private static void a(Level var0, String var1, Exception var2) {
      try {
         if (a.isLoggable(var0)) {
            a.log(var0, var1, (Throwable)var2);
         }
      } catch (FeatureNotLicensedException var3) {
         throw var3;
      }
   }

   static {
      String[] var10000 = new String[35];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "EXn\u000eM\u0006P.\u0003[\u0005[rE@Y\u0007.\u001dW\u0018^c\u000fV\t]/\u001aV\r";
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
                  var10009 = 106;
                  break;
               case 1:
                  var10009 = 53;
                  break;
               case 2:
                  var10009 = 1;
                  break;
               case 3:
                  var10009 = 106;
                  break;
               default:
                  var10009 = 56;
            }

            var17[var0] = (char)(var10008 ^ var10009);
         }

         String var22 = new String(var17).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "\u0006Tr\u001ez\u0005Zm\u000fY\u0004vi\u000f[\u0001Wn\u0012n\u000bYt\u000f";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "\u0006Tr\u001e}\u0004@l)P\u000fVj\bW\u0012c`\u0006M\u000f";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "\u0006Tr\u001ek\u000fYd\tL\u000fQG\u0005T\u000ePs";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "\u0006Tr\u001ek\u000fTs\tP>Py\u001e";
               var10004 = 4;
               break;
            case 4:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "\u0006Tr\u001ek\u000fVt\u0018]/[t\u0007{\u0002Pb\u0001Z\u0005MW\u000bT\u001fP";
               var10004 = 5;
               break;
            case 5:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "\u0006Tr\u001ek\u000fVt\u0018](Zn\u0006]\u000b[B\u0002]\t^c\u0005@<Tm\u001f]";
               var10004 = 6;
               break;
            case 6:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 8;
               var10003 = "\u0006Tr\u001ev\u001fXd\u0018Q\tvi\u000f[\u0001Wn\u0012n\u000bYt\u000f";
               var10004 = 7;
               break;
            case 7:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 9;
               var10003 = "\u0006Tr\u001ek\u000fYd\tL\u000fQQ\u0005Q\u0004A";
               var10004 = 8;
               break;
            case 8:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "/Gs\u0005JJ\\oJ_\u000fAu\u0003V\r\u0015c\u000bK\u000f\u00157^\u0018\u0003X`\r]\u0019";
               var10004 = 9;
               break;
            case 9:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 11;
               var10003 = "\u0019Cf";
               var10004 = 10;
               break;
            case 10:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 12;
               var10003 = "\u000eTu\u000b\u0002\u0003X`\r]EFw\r\u0013\u0012XmQZ\u000bFd\\\fF";
               var10004 = 11;
               break;
            case 11:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 13;
               var10003 = "/Gs\u0005JJ\\oJ_\u000fAu\u0003V\r\u0015c\u000bK\u000f\u00157^\u0018\u0003X`\r]\u0019";
               var10004 = 12;
               break;
            case 12:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 14;
               var10003 = "\u000eTu\u000b\u0002\u0003X`\r]E\u0010rQZ\u000bFd\\\fF";
               var10004 = 13;
               break;
            case 13:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 15;
               var10003 = "/Gs\u0005JJ\\oJ{\u0002Pb\u0001Q\u0004R!9M\u001aPs\u001cQ\u0019Zs";
               var10004 = 14;
               break;
            case 14:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 16;
               var10003 = "\u0018Pr\u001aW\u0004Fd";
               var10004 = 15;
               break;
            case 15:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 17;
               var10003 = "\u0019Yn\u001eh\u000bAi%J\u000e";
               var10004 = 16;
               break;
            case 16:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 18;
               var10003 = "\u0004Ze\u000fl\u0013Ed";
               var10004 = 17;
               break;
            case 17:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 19;
               var10003 = "\u0003Vn\u0004";
               var10004 = 18;
               break;
            case 18:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 20;
               var10003 = "\u0019Yn\u001e\u0002E";
               var10004 = 19;
               break;
            case 19:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 21;
               var10003 = "\u0004Ze\u000fK";
               var10004 = 20;
               break;
            case 20:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 22;
               var10003 = "\u000e\\r\u001aT\u000bLO\u000bU\u000f";
               var10004 = 21;
               break;
            case 21:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 23;
               var10003 = "\u001aZh\u0004L$Tl\u000f";
               var10004 = 22;
               break;
            case 22:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 24;
               var10003 = "\u0003FQ\u0005Q\u0004A";
               var10004 = 23;
               break;
            case 23:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 25;
               var10003 = "\u0006Tr\u001ek\u000fTs\tP>Py\u001e";
               var10004 = 24;
               break;
            case 24:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 26;
               var10003 = "\u0006Tr\u001ek\u000fYd\tL\u000fQG\u0005T\u000ePs";
               var10004 = 25;
               break;
            case 25:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 27;
               var10003 = "\u0006Tr\u001ek\u000fVt\u0018]$@l\u000fJ\u0003VB\u0002]\t^c\u0005@<Tm\u001f]";
               var10004 = 26;
               break;
            case 26:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 28;
               var10003 = "\u0006Tr\u001ev\u001fXd\u0018Q\tvi\u000f[\u0001Wn\u0012n\u000bYt\u000f";
               var10004 = 27;
               break;
            case 27:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 29;
               var10003 = "\u0006Tr\u001ek\u000fYd\tL\u000fQQ\u0005Q\u0004A";
               var10004 = 28;
               break;
            case 28:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 30;
               var10003 = "\u0006Tr\u001ek\u000fVt\u0018]/[t\u0007{\u0002Pb\u0001Z\u0005MW\u000bT\u001fP";
               var10004 = 29;
               break;
            case 29:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 31;
               var10003 = "\u0006Tr\u001ez\u0005Zm\u000fY\u0004vi\u000f[\u0001Wn\u0012n\u000bYt\u000f";
               var10004 = 30;
               break;
            case 30:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 32;
               var10003 = "\u0006Tr\u001ek\u000fVt\u0018](Zn\u0006]\u000b[B\u0002]\t^c\u0005@<Tm\u001f]";
               var10004 = 31;
               break;
            case 31:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 33;
               var10003 = "\u0006Tr\u001e}\u0004@l)P\u000fVj\bW\u0012c`\u0006M\u000f";
               var10004 = 32;
               break;
            case 32:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 34;
               var10003 = "#[w\u000bT\u0003Q!&Q\tPo\u0019]";
               var10004 = 33;
               break;
            case 33:
               var10001[var10002] = var22;
               z = var10000;
               TYPE = Sys.loadType(BEasyBindingUtil.class);
               char[] var2 = "/Tr\u0013z\u0003[e\u0003V\r".toCharArray();
               int var9 = var2.length;
               char[] var6 = var2;
               int var3 = var9;

               for (int var1 = 0; var3 > var1; var1++) {
                  char var19 = var6[var1];
                  byte var23;
                  switch (var1 % 5) {
                     case 0:
                        var23 = 106;
                        break;
                     case 1:
                        var23 = 53;
                        break;
                     case 2:
                        var23 = 1;
                        break;
                     case 3:
                        var23 = 106;
                        break;
                     default:
                        var23 = 56;
                  }

                  var6[var1] = (char)(var19 ^ var23);
               }

               var10003 = new String(var6).intern();
               byte var5 = -1;
               a = Logger.getLogger(var10003);
               var10000 = new String[11];
               var10001 = var10000;
               var10002 = 0;
               var10003 = ">Gd\u0004\\$\u0001;>J\u000f[e$]\u001eBn\u0018S";
               var10004 = 34;
               break;
            case 34:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "\bTb\u0004]\u001e\u000fC\u000b[\u0004Pu$]\u001eBn\u0018S";
               var10004 = 35;
               break;
            case 35:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "\bTb\u0004]\u001etv\u0019\u0002(Tb\u0004]\u001etv\u0019v\u000fAv\u0005J\u0001";
               var10004 = 36;
               break;
            case 36:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "\bTb\u0004]\u001ezv\u0019\u0002(Tb\u0004]\u001ezv\u0019v\u000fAv\u0005J\u0001";
               var10004 = 37;
               break;
            case 37:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "\u0019WbPk\bVO\u000fL\u001dZs\u0001";
               var10004 = 38;
               break;
            case 38:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "\tYB(M\u0019\u000fB(M\u0019{d\u001eO\u0005Gj";
               var10004 = 39;
               break;
            case 39:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "\u0003Eb)W\u0007XC\u001fKP|Q)v\u000fAv\u0005J\u0001";
               var10004 = 40;
               break;
            case 40:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "\u0007Ze\bM\u0019ab\u001a\u0002'Ze\bM\u0019ab\u001av\u000fAv\u0005J\u0001";
               var10004 = 41;
               break;
            case 41:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 8;
               var10003 = "\u0007Wt\u0019\u0002'Wt\u0019l\tEH\u001av\u000fAv\u0005J\u0001";
               var10004 = 42;
               break;
            case 42:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 9;
               var10003 = "\u0007Wt\u0019\u0002'Wt\u0019v\u000fAv\u0005J\u0001";
               var10004 = 43;
               break;
            case 43:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "\u0004\\`\rY\u0018TE\u0018Q\u001cPsPv\u0003Tf\u000bJ\u000b{d\u001eO\u0005Gj";
               var10004 = 44;
               break;
            case 44:
               var10001[var10002] = var22;
               b = var10000;
               c = new HashSet<>(Arrays.asList(b));
               return;
            default:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "\u0006Tr\u001ek\u000fVt\u0018]$@l\u000fJ\u0003VB\u0002]\t^c\u0005@<Tm\u001f]";
               var10004 = 0;
         }
      }
   }
}
