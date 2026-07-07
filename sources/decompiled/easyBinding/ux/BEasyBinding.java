package com.honeywell.easybinding.easybinding;

import com.honeywell.easybinding.BBaseWidget;
import javax.baja.naming.BOrd;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.BIFormFactorMax;
import javax.baja.web.js.BIJavaScript;
import javax.baja.web.js.JsInfo;

public class BEasyBinding extends BBaseWidget implements BIJavaScript, BIFormFactorMax {
   public static final BEasyBinding INSTANCE = new BEasyBinding();
   public static final Type TYPE = Sys.loadType(BEasyBinding.class);
   private static final JsInfo a;

   private BEasyBinding() {
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public JsInfo getGuiJsInfo(Context var1) {
      return a;
   }

   static {
      char[] var10001 = "\u0015+\nHr\u001d~A\u0012{\u00197\u0017\u007fw\u0016 \u0007SyW6\r\u0012}\u0017)\u001eRp\u001d*\u001aN1\u00127Atp\u001c!\u0016\u0013t\u000b"
         .toCharArray();
      int var10003 = var10001.length;
      char[] var10002 = var10001;
      int var1 = var10003;

      for (int var0 = 0; var1 > var0; var0++) {
         char var10005 = var10002[var0];
         byte var10006;
         switch (var0 % 5) {
            case 0:
               var10006 = 120;
               break;
            case 1:
               var10006 = 68;
               break;
            case 2:
               var10006 = 110;
               break;
            case 3:
               var10006 = 61;
               break;
            default:
               var10006 = 30;
         }

         var10002[var0] = (char)(var10005 ^ var10006);
      }

      String var7 = new String(var10002).intern();
      byte var3 = -1;
      a = JsInfo.make(BOrd.make(var7));
   }
}
