package com.honeywell.easybinding.util;

import com.tridium.json.JSONArray;
import java.io.IOException;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;

public class EbConfigUtil {
   private static final String[] z;

   private EbConfigUtil() {
   }

   public static BIFile getEBConfigFile() throws IOException {
      BFileSystem var0 = BFileSystem.INSTANCE;
      FilePath var1 = new FilePath(z[58]);
      BIFile var2 = var0.findFile(var1);
      if (var2 == null) {
         var2 = var0.makeFile(var1);
         XWriter var3 = new XWriter(var2.getOutputStream());
         XElem var4 = new XElem(z[59]);
         var4.addContent(a());

         try {
            var4.write(var3);
         } finally {
            var3.flush();
            var3.close();
         }
      }

      return var2;
   }

   private static XElem a() {
      XElem var0 = new XElem(z[28]);
      var0.setAttr(z[41], "");
      XElem var1 = new XElem(z[33]);
      var1.setAttr(z[32], z[39]);
      XElem var2 = new XElem(z[25]);
      var2.setAttr(z[26], z[21]);
      XElem var3 = new XElem(z[35]);
      var3.setAttr(z[37], z[45]);
      XElem var4 = new XElem(z[40]);
      var4.setAttr(z[30], "");
      XElem var5 = new XElem(z[43]);
      var5.setAttr(z[31], "");
      XElem var6 = new XElem(z[44]);
      XElem var7 = new XElem(z[36]);
      var7.setAttr(z[38], z[42]);
      XElem var8 = new XElem(z[23]);
      var8.setAttr(z[22], z[27]);
      XElem var9 = new XElem(z[34]);
      var9.setAttr(z[29], z[24]);
      var6.addContent(var0);
      var6.addContent(var1);
      var6.addContent(var2);
      var6.addContent(var3);
      var6.addContent(var4);
      var6.addContent(var5);
      var6.addContent(var7);
      var6.addContent(var8);
      var6.addContent(var9);
      return var6;
   }

   public static String getLastSearchText(XElem var0) {
      XElem var1 = var0.elem(z[16]);
      XElem var2 = var1.elem(z[15]);
      return var2.get(z[17]);
   }

   public static void setLastSearchText(XElem var0, String var1) {
      XElem var2 = var0.elem(z[46]);
      XElem var3 = var2.elem(z[48]);
      var3.setAttr(z[47], var1);
   }

   public static boolean getLastBooleanCheckboxValue(XElem var0) {
      XElem var1 = var0.elem(z[76]);
      XElem var2 = var1.elem(z[77]);
      return Boolean.valueOf(var2.get(z[75]));
   }

   public static void setLastBooleanCheckboxValue(XElem var0, boolean var1) {
      XElem var2 = var0.elem(z[2]);
      XElem var3 = var2.elem(z[0]);
      var3.setAttr(z[1], String.valueOf(var1));
   }

   public static boolean getLastNumericCheckboxValue(XElem var0) {
      XElem var1 = var0.elem(z[70]);
      XElem var2 = var1.elem(z[71]);
      return Boolean.valueOf(var2.get(z[69]));
   }

   public static void setLastNumericCheckboxValue(XElem var0, boolean var1) {
      XElem var2 = var0.elem(z[20]);
      XElem var3 = var2.elem(z[18]);
      var3.setAttr(z[19], String.valueOf(var1));
   }

   public static boolean getLastEnumCheckboxValue(XElem var0) {
      XElem var1 = var0.elem(z[14]);
      XElem var2 = var1.elem(z[12]);
      return Boolean.valueOf(var2.get(z[13]));
   }

   public static void setLastEnumCheckboxValue(XElem var0, boolean var1) {
      XElem var2 = var0.elem(z[3]);
      XElem var3 = var2.elem(z[4]);
      var3.setAttr(z[5], String.valueOf(var1));
   }

   public static String getLastSelectedFolder(XElem var0) {
      XElem var1 = var0.elem(z[52]);
      XElem var2 = var1.elem(z[54]);
      return var2.get(z[53]);
   }

   public static void setLastSelectedFolder(XElem var0, String var1) {
      XElem var2 = var0.elem(z[63]);
      XElem var3 = var2.elem(z[64]);
      var3.setAttr(z[65], var1);
   }

   public static String getLastSelectedPoint(XElem var0) {
      XElem var1 = var0.elem(z[6]);
      XElem var2 = var1.elem(z[8]);
      return var2.get(z[7]);
   }

   public static void setLastSelectedPoint(XElem var0, String var1) {
      XElem var2 = var0.elem(z[50]);
      XElem var3 = var2.elem(z[51]);
      var3.setAttr(z[49], var1);
   }

   public static boolean getLastSecureBooleanCheckboxValue(XElem var0) {
      XElem var1 = var0.elem(z[11]);
      XElem var2 = var1.elem(z[10]);
      return Boolean.valueOf(var2.get(z[9]));
   }

   public static void setLastSecureBooleanCheckboxValue(XElem var0, boolean var1) {
      XElem var2 = var0.elem(z[56]);
      XElem var3 = var2.elem(z[57]);
      var3.setAttr(z[55], String.valueOf(var1));
   }

   public static boolean getLastSecureNumericCheckboxValue(XElem var0) {
      XElem var1 = var0.elem(z[68]);
      XElem var2 = var1.elem(z[67]);
      return Boolean.valueOf(var2.get(z[66]));
   }

   public static void setLastSecureNumericCheckboxValue(XElem var0, boolean var1) {
      XElem var2 = var0.elem(z[60]);
      XElem var3 = var2.elem(z[62]);
      var3.setAttr(z[61], String.valueOf(var1));
   }

   public static boolean getLastSecureEnumCheckboxValue(XElem var0) {
      XElem var1 = var0.elem(z[73]);
      XElem var2 = var1.elem(z[72]);
      return Boolean.valueOf(var2.get(z[74]));
   }

   public static void setLastSecureEnumCheckboxValue(XElem var0, boolean var1) {
      XElem var2 = var0.elem(z[80]);
      XElem var3 = var2.elem(z[78]);
      var3.setAttr(z[79], String.valueOf(var1));
   }

   public static boolean isObjectExistsInJsonAray(JSONArray var0, String var1, String var2) {
      boolean var3 = false;

      for (int var4 = 0; var4 < var0.length(); var4++) {
         if (var0.getJSONObject(var4).get(var1) != null && var0.getJSONObject(var4).get(var1).equals(var2)) {
            var3 = true;
            break;
         }
      }

      return var3;
   }

   static {
      String[] var10000 = new String[81];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "z^\u001d\u0007DYP\u0002\u0016gX|\u0006\u0016e]]\u0001\u000bPWS\u001b\u0016";
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
                  var10009 = 54;
                  break;
               case 1:
                  var10009 = 63;
                  break;
               case 2:
                  var10009 = 110;
                  break;
               case 3:
                  var10009 = 115;
                  break;
               default:
                  var10009 = 6;
            }

            var6[var0] = (char)(var10008 ^ var10009);
         }

         String var10 = new String(var6).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 2;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 3;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 4;
               var10003 = "z^\u001d\u0007CXJ\u00030nS\\\u0005\u0011iNi\u000f\u001fsS";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "@^\u0002\u0006c";
               var10004 = 4;
               break;
            case 4:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 5;
               break;
            case 5:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "@^\u0002\u0006c";
               var10004 = 6;
               break;
            case 6:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 8;
               var10003 = "z^\u001d\u0007USS\u000b\u0010rS[>\u001coXK";
               var10004 = 7;
               break;
            case 7:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 9;
               var10003 = "@^\u0002\u0006c";
               var10004 = 8;
               break;
            case 8:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001ctP\u0001\u001fcWQ-\u001bcUT\f\u001c~`^\u0002\u0006c";
               var10004 = 9;
               break;
            case 9:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 11;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 10;
               break;
            case 10:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 12;
               var10003 = "z^\u001d\u0007CXJ\u00030nS\\\u0005\u0011iNi\u000f\u001fsS";
               var10004 = 11;
               break;
            case 11:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 13;
               var10003 = "@^\u0002\u0006c";
               var10004 = 12;
               break;
            case 12:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 14;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 13;
               break;
            case 13:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 15;
               var10003 = "z^\u001d\u0007US^\u001c\u0010nbZ\u0016\u0007";
               var10004 = 14;
               break;
            case 14:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 16;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 15;
               break;
            case 15:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 17;
               var10003 = "@^\u0002\u0006c";
               var10004 = 16;
               break;
            case 16:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 18;
               var10003 = "z^\u001d\u0007HCR\u000b\u0001oU|\u0006\u0016e]]\u0001\u000bPWS\u001b\u0016";
               var10004 = 17;
               break;
            case 17:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 19;
               var10003 = "@^\u0002\u0006c";
               var10004 = 18;
               break;
            case 18:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 20;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 19;
               break;
            case 19:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 21;
               var10003 = "BM\u001b\u0016";
               var10004 = 20;
               break;
            case 20:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 22;
               var10003 = "@^\u0002\u0006c";
               var10004 = 21;
               break;
            case 21:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 23;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001cxJ\u0003\u0016t_\\-\u001bcUT\f\u001c~`^\u0002\u0006c";
               var10004 = 22;
               break;
            case 22:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 24;
               var10003 = "BM\u001b\u0016";
               var10004 = 23;
               break;
            case 23:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 25;
               var10003 = "z^\u001d\u0007HCR\u000b\u0001oU|\u0006\u0016e]]\u0001\u000bPWS\u001b\u0016";
               var10004 = 24;
               break;
            case 24:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 26;
               var10003 = "@^\u0002\u0006c";
               var10004 = 25;
               break;
            case 25:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 27;
               var10003 = "BM\u001b\u0016";
               var10004 = 26;
               break;
            case 26:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 28;
               var10003 = "z^\u001d\u0007US^\u001c\u0010nbZ\u0016\u0007";
               var10004 = 27;
               break;
            case 27:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 29;
               var10003 = "@^\u0002\u0006c";
               var10004 = 28;
               break;
            case 28:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 30;
               var10003 = "@^\u0002\u0006c";
               var10004 = 29;
               break;
            case 29:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 31;
               var10003 = "@^\u0002\u0006c";
               var10004 = 30;
               break;
            case 30:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 32;
               var10003 = "@^\u0002\u0006c";
               var10004 = 31;
               break;
            case 31:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 33;
               var10003 = "z^\u001d\u0007DYP\u0002\u0016gX|\u0006\u0016e]]\u0001\u000bPWS\u001b\u0016";
               var10004 = 32;
               break;
            case 32:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 34;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001csQ\u001b\u001eE^Z\r\u0018dYG8\u0012jCZ";
               var10004 = 33;
               break;
            case 33:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 35;
               var10003 = "z^\u001d\u0007CXJ\u00030nS\\\u0005\u0011iNi\u000f\u001fsS";
               var10004 = 34;
               break;
            case 34:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 36;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001ctP\u0001\u001fcWQ-\u001bcUT\f\u001c~`^\u0002\u0006c";
               var10004 = 35;
               break;
            case 35:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 37;
               var10003 = "@^\u0002\u0006c";
               var10004 = 36;
               break;
            case 36:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 38;
               var10003 = "@^\u0002\u0006c";
               var10004 = 37;
               break;
            case 37:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 39;
               var10003 = "BM\u001b\u0016";
               var10004 = 38;
               break;
            case 38:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 40;
               var10003 = "z^\u001d\u0007USS\u000b\u0010rS[(\u001cjRZ\u001c";
               var10004 = 39;
               break;
            case 39:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 41;
               var10003 = "@^\u0002\u0006c";
               var10004 = 40;
               break;
            case 40:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 42;
               var10003 = "BM\u001b\u0016";
               var10004 = 41;
               break;
            case 41:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 43;
               var10003 = "z^\u001d\u0007USS\u000b\u0010rS[>\u001coXK";
               var10004 = 42;
               break;
            case 42:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 44;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 43;
               break;
            case 43:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 45;
               var10003 = "BM\u001b\u0016";
               var10004 = 44;
               break;
            case 44:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 46;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 45;
               break;
            case 45:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 47;
               var10003 = "@^\u0002\u0006c";
               var10004 = 46;
               break;
            case 46:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 48;
               var10003 = "z^\u001d\u0007US^\u001c\u0010nbZ\u0016\u0007";
               var10004 = 47;
               break;
            case 47:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 49;
               var10003 = "@^\u0002\u0006c";
               var10004 = 48;
               break;
            case 48:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 50;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 49;
               break;
            case 49:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 51;
               var10003 = "z^\u001d\u0007USS\u000b\u0010rS[>\u001coXK";
               var10004 = 50;
               break;
            case 50:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 52;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 51;
               break;
            case 51:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 53;
               var10003 = "@^\u0002\u0006c";
               var10004 = 52;
               break;
            case 52:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 54;
               var10003 = "z^\u001d\u0007USS\u000b\u0010rS[(\u001cjRZ\u001c";
               var10004 = 53;
               break;
            case 53:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 55;
               var10003 = "@^\u0002\u0006c";
               var10004 = 54;
               break;
            case 54:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 56;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 55;
               break;
            case 55:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 57;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001ctP\u0001\u001fcWQ-\u001bcUT\f\u001c~`^\u0002\u0006c";
               var10004 = 56;
               break;
            case 56:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 58;
               var10003 = "h\\\u0001\u001d`_XA\u0016di\\\u0001\u001d`_X@\u000bkZ";
               var10004 = 57;
               break;
            case 57:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 59;
               var10003 = "s^\u001d\nD_Q\n\u001ahQ|\u0001\u001d`_X\u001b\u0001gBV\u0001\u001du";
               var10004 = 58;
               break;
            case 58:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 60;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 59;
               break;
            case 59:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 61;
               var10003 = "@^\u0002\u0006c";
               var10004 = 60;
               break;
            case 60:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 62;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001cxJ\u0003\u0016t_\\-\u001bcUT\f\u001c~`^\u0002\u0006c";
               var10004 = 61;
               break;
            case 61:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 63;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 62;
               break;
            case 62:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 64;
               var10003 = "z^\u001d\u0007USS\u000b\u0010rS[(\u001cjRZ\u001c";
               var10004 = 63;
               break;
            case 63:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 65;
               var10003 = "@^\u0002\u0006c";
               var10004 = 64;
               break;
            case 64:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 66;
               var10003 = "@^\u0002\u0006c";
               var10004 = 65;
               break;
            case 65:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 67;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001cxJ\u0003\u0016t_\\-\u001bcUT\f\u001c~`^\u0002\u0006c";
               var10004 = 66;
               break;
            case 66:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 68;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 67;
               break;
            case 67:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 69;
               var10003 = "@^\u0002\u0006c";
               var10004 = 68;
               break;
            case 68:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 70;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 69;
               break;
            case 69:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 71;
               var10003 = "z^\u001d\u0007HCR\u000b\u0001oU|\u0006\u0016e]]\u0001\u000bPWS\u001b\u0016";
               var10004 = 70;
               break;
            case 70:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 72;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001csQ\u001b\u001eE^Z\r\u0018dYG8\u0012jCZ";
               var10004 = 71;
               break;
            case 71:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 73;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 72;
               break;
            case 72:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 74;
               var10003 = "@^\u0002\u0006c";
               var10004 = 73;
               break;
            case 73:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 75;
               var10003 = "@^\u0002\u0006c";
               var10004 = 74;
               break;
            case 74:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 76;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 75;
               break;
            case 75:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 77;
               var10003 = "z^\u001d\u0007DYP\u0002\u0016gX|\u0006\u0016e]]\u0001\u000bPWS\u001b\u0016";
               var10004 = 76;
               break;
            case 76:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 78;
               var10003 = "z^\u001d\u0007US\\\u001b\u0001csQ\u001b\u001eE^Z\r\u0018dYG8\u0012jCZ";
               var10004 = 77;
               break;
            case 77:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 79;
               var10003 = "@^\u0002\u0006c";
               var10004 = 78;
               break;
            case 78:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 80;
               var10003 = "fP\u0007\u001druW\u0001\u001cuSM";
               var10004 = 79;
               break;
            case 79:
               var10001[var10002] = var10;
               z = var10000;
               return;
            default:
               var10001[var10002] = var10;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "@^\u0002\u0006c";
               var10004 = 0;
         }
      }
   }
}
