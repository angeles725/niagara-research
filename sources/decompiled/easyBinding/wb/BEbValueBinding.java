package com.honeywell.easybinding.bindings;

import com.honeywell.easybinding.logger.EasyBindingException;
import com.tridium.json.JSONObject;
import com.tridium.workbench.web.browser.BWebWidget;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.converters.BIBooleanToSimple;
import javax.baja.converters.BIEnumToSimple;
import javax.baja.converters.BINumericToSimple;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.AgentOn.Preference;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BValueBinding;
import javax.baja.util.BConverter;

@NiagaraType(
   agent = {@AgentOn(
      types = {"workbench:WebWidget"},
      defaultAgent = Preference.PREFERRED,
      requiredPermissions = "rw"
   )}
)
public class BEbValueBinding extends BValueBinding {
   public static final Type TYPE;
   private static Logger a;
   public static final String EB_BINDING_TYPE;
   public static final String EB_IMAGES;
   public static final String EB_TRUE_IMAGE;
   public static final String EB_FALSE_IMAGE;
   public static final String EB_TRUE_VALUE;
   public static final String EB_FALSE_VALUE;
   public static final String EB_MAP;
   public static final String EB_DATA;
   public static final String EB_VALUEORD;
   public static final String EB_ENUM;
   public static final String EB_BOOLEAN;
   public static final String EB_NUMERIC;
   public static final String EB_GENERIC;
   public static final String ALARM_IMAGE;
   public static final String OVERRIDE_IMAGE;
   private static final String[] z;

   public Type getType() {
      return TYPE;
   }

   public void started() {
      this.a();
      super.started();
   }

   public void changed(Property var1, Context var2) {
      this.a();
      super.changed(var1, var2);
   }

   public String generateData(boolean var1, String var2, String var3, String var4) throws EasyBindingException, IOException {
      JSONObject var5 = this.a(var1);
      var5 = this.a(var5, var2, var3);
      if (var4 != null) {
         var5 = this.a(var5, var4);
      }

      return var5.toString();
   }

   private JSONObject a(JSONObject var1, String var2) {
      return var1.put(z[0], var2);
   }

   private JSONObject a(JSONObject var1, String var2, String var3) {
      if (var2 != "") {
         var1.put(z[3], var2);
      } else {
         var1.put(z[1], "");
      }

      if (var3 != "") {
         var1.put(z[2], var3);
      } else {
         var1.put(z[4], "");
      }

      return var1;
   }

   public String generateData() throws EasyBindingException, IOException {
      return this.generateData(false, "", "", "");
   }

   private JSONObject a(boolean var1) throws EasyBindingException, IOException {
      JSONObject var2 = new JSONObject();
      BConverter var3 = (BConverter)this.get(z[10]);
      Type var4 = var3.getType();
      if (var1) {
         BIBooleanToSimple var5 = (BIBooleanToSimple)var3;
         var2.put(z[25], z[11]);
         var2.put(z[23], var5.get(z[26]).toString());
         var2.put(z[13], var5.get(z[21]).toString());
      } else if (var4.equals(BIBooleanToSimple.TYPE)) {
         BIBooleanToSimple var6 = (BIBooleanToSimple)var3;
         var2.put(z[20], z[22]);
         var2.put(z[17], var6.get(z[24]).toString());
         var2.put(z[14], var6.get(z[28]).toString());
      } else if (var4.equals(BIEnumToSimple.TYPE)) {
         BIEnumToSimple var7 = (BIEnumToSimple)var3;
         var2.put(z[16], z[12]);
         var2.put(z[27], var7.getMap().encodeToString());
      } else {
         if (!var4.equals(BINumericToSimple.TYPE)) {
            return var2;
         }

         BINumericToSimple var8 = (BINumericToSimple)var3;
         var2.put(z[19], z[15]);
         var2.put(z[18], var8.getMap().encodeToString());
      }

      return var2;
   }

   private void a() {
      try {
         if (Sys.getStation() == null) {
            return;
         }
      } catch (Exception var9) {
         throw var9;
      }

      try {
         if (this.getParent() == null || !this.getParent().getType().equals(BWebWidget.TYPE)) {
            return;
         }
      } catch (Exception var10) {
         throw var10;
      }

      BWebWidget var1 = (BWebWidget)this.getParent();
      String var2 = z[5];
      if (var1.getProperty(var2) != null) {
         try {
            JSONObject var3 = new JSONObject(var1.get(var2).toString());
            String var4 = var3.getString(z[9]);
            String var5 = var3.getString(z[8]);
            String var6 = var3.getString(z[6]);
            var1.set(var2, BString.make(this.generateData(true, var4, var5, var6)));
         } catch (Exception var8) {
            a.log(Level.INFO, var8.getMessage(), (Throwable)var8);
         }
      }

      var2 = z[7];

      try {
         if (var1.getProperty(var2) != null) {
            var1.set(var2, BString.make(this.getOrd().toString()));
         }
      } catch (Exception var7) {
         throw var7;
      }
   }

   static {
      String[] var10000 = new String[29];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "\rH\u0006y\u000f\u000eI";
      int var10004 = 28;

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
                  var10009 = 111;
                  break;
               case 1:
                  var10009 = 39;
                  break;
               case 2:
                  var10009 = 105;
                  break;
               case 3:
                  var10009 = 21;
                  break;
               default:
                  var10009 = 106;
            }

            var17[var0] = (char)(var10008 ^ var10009);
         }

         String var22 = new String(var17).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 2;
               var10003 = " Q\fg\u0018\u0006C\f\\\u0007\u000e@\f";
               var10004 = 1;
               break;
            case 1:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 3;
               var10003 = ".K\bg\u0007&J\br\u000f";
               var10004 = 2;
               break;
            case 2:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 4;
               var10003 = " Q\fg\u0018\u0006C\f\\\u0007\u000e@\f";
               var10004 = 3;
               break;
            case 3:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 5;
               var10003 = "\u000bF\u001dt";
               var10004 = 4;
               break;
            case 4:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 6;
               var10003 = "\u001bB\u0004e\u0006\u000eS\fQ\u000b\u001bF";
               var10004 = 5;
               break;
            case 5:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 7;
               var10003 = "\u0019F\u0005`\u000f U\r";
               var10004 = 6;
               break;
            case 6:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 8;
               var10003 = " Q\fg\u0018\u0006C\f\\\u0007\u000e@\f";
               var10004 = 7;
               break;
            case 7:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 9;
               var10003 = ".K\bg\u0007&J\br\u000f";
               var10004 = 8;
               break;
            case 8:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 10;
               var10003 = "\u0006J\br\u000f\u001c";
               var10004 = 9;
               break;
            case 9:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 11;
               var10003 = "\bB\u0007p\u0018\u0006D";
               var10004 = 10;
               break;
            case 10:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 12;
               var10003 = "\nI\u001cx";
               var10004 = 11;
               break;
            case 11:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 13;
               var10003 = "\tF\u0005f\u000f&J\br\u000f";
               var10004 = 12;
               break;
            case 12:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 14;
               var10003 = "\tF\u0005f\u000f&J\br\u000f";
               var10004 = 13;
               break;
            case 13:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 15;
               var10003 = "\u0001R\u0004p\u0018\u0006D";
               var10004 = 14;
               break;
            case 14:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 16;
               var10003 = "\rN\u0007q\u0003\u0001@=l\u001a\n";
               var10004 = 15;
               break;
            case 15:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 17;
               var10003 = "\u001bU\u001cp#\u0002F\u000ep";
               var10004 = 16;
               break;
            case 16:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 18;
               var10003 = "\u0002F\u0019";
               var10004 = 17;
               break;
            case 17:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 19;
               var10003 = "\rN\u0007q\u0003\u0001@=l\u001a\n";
               var10004 = 18;
               break;
            case 18:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 20;
               var10003 = "\rN\u0007q\u0003\u0001@=l\u001a\n";
               var10004 = 19;
               break;
            case 19:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 21;
               var10003 = "\tF\u0005f\u000f9F\u0005`\u000f";
               var10004 = 20;
               break;
            case 20:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 22;
               var10003 = "\rH\u0006y\u000f\u000eI";
               var10004 = 21;
               break;
            case 21:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 23;
               var10003 = "\u001bU\u001cp#\u0002F\u000ep";
               var10004 = 22;
               break;
            case 22:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 24;
               var10003 = "\u001bU\u001cp<\u000eK\u001cp";
               var10004 = 23;
               break;
            case 23:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 25;
               var10003 = "\rN\u0007q\u0003\u0001@=l\u001a\n";
               var10004 = 24;
               break;
            case 24:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 26;
               var10003 = "\u001bU\u001cp<\u000eK\u001cp";
               var10004 = 25;
               break;
            case 25:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 27;
               var10003 = "\u0002F\u0019";
               var10004 = 26;
               break;
            case 26:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 28;
               var10003 = "\tF\u0005f\u000f9F\u0005`\u000f";
               var10004 = 27;
               break;
            case 27:
               var10001[var10002] = var22;
               z = var10000;
               TYPE = Sys.loadType(BEbValueBinding.class);
               char[] var2 = "&I\u000fz&\u0000@\u000e|\u0004\b".toCharArray();
               int var9 = var2.length;
               char[] var6 = var2;
               int var3 = var9;

               for (int var1 = 0; var3 > var1; var1++) {
                  char var19 = var6[var1];
                  byte var23;
                  switch (var1 % 5) {
                     case 0:
                        var23 = 111;
                        break;
                     case 1:
                        var23 = 39;
                        break;
                     case 2:
                        var23 = 105;
                        break;
                     case 3:
                        var23 = 21;
                        break;
                     default:
                        var23 = 106;
                  }

                  var6[var1] = (char)(var19 ^ var23);
               }

               var10003 = new String(var6).intern();
               byte var5 = -1;
               a = Logger.getLogger(var10003);
               return;
            case 28:
               EB_BOOLEAN = var22;
               var10003 = "\u0006J\br\u000f\u001c";
               var10004 = 29;
               break;
            case 29:
               EB_IMAGES = var22;
               var10003 = "\u0019F\u0005`\u000f U\r";
               var10004 = 30;
               break;
            case 30:
               EB_VALUEORD = var22;
               var10003 = "\u000bF\u001dt";
               var10004 = 31;
               break;
            case 31:
               EB_DATA = var22;
               var10003 = "\bB\u0007p\u0018\u0006D";
               var10004 = 32;
               break;
            case 32:
               EB_GENERIC = var22;
               var10003 = "\rN\u0007q\u0003\u0001@=l\u001a\n";
               var10004 = 33;
               break;
            case 33:
               EB_BINDING_TYPE = var22;
               var10003 = "\nI\u001cx";
               var10004 = 34;
               break;
            case 34:
               EB_ENUM = var22;
               var10003 = "\u0001R\u0004p\u0018\u0006D";
               var10004 = 35;
               break;
            case 35:
               EB_NUMERIC = var22;
               var10003 = "\u0002F\u0019";
               var10004 = 36;
               break;
            case 36:
               EB_MAP = var22;
               var10003 = "\tF\u0005f\u000f9F\u0005`\u000f";
               var10004 = 37;
               break;
            case 37:
               EB_FALSE_VALUE = var22;
               var10003 = "\tF\u0005f\u000f&J\br\u000f";
               var10004 = 38;
               break;
            case 38:
               EB_FALSE_IMAGE = var22;
               var10003 = "\u001bU\u001cp<\u000eK\u001cp";
               var10004 = 39;
               break;
            case 39:
               EB_TRUE_VALUE = var22;
               var10003 = " Q\fg\u0018\u0006C\f\\\u0007\u000e@\f";
               var10004 = 40;
               break;
            case 40:
               OVERRIDE_IMAGE = var22;
               var10003 = "\u001bU\u001cp#\u0002F\u000ep";
               var10004 = 41;
               break;
            case 41:
               EB_TRUE_IMAGE = var22;
               var10003 = ".K\bg\u0007&J\br\u000f";
               var10004 = 42;
               break;
            case 42:
               ALARM_IMAGE = var22;
               var10003 = "\u001bB\u0004e\u0006\u000eS\fQ\u000b\u001bF";
               var10004 = -1;
               break;
            default:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 1;
               var10003 = ".K\bg\u0007&J\br\u000f";
               var10004 = 0;
         }
      }
   }
}
