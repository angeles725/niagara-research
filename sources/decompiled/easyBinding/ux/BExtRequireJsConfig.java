package com.honeywell.easybinding;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.sys.BSingleton;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.BIRequireJsConfig;
import javax.baja.web.js.JsInfo;

@NiagaraSingleton
public final class BExtRequireJsConfig extends BSingleton implements BIRequireJsConfig {
   public static final BExtRequireJsConfig INSTANCE = new BExtRequireJsConfig();
   public static final Type TYPE = Sys.loadType(BExtRequireJsConfig.class);
   private static final JsInfo a;

   public Type getType() {
      return TYPE;
   }

   private BExtRequireJsConfig() {
   }

   public JsInfo getJsInfo(Context var1) {
      return a;
   }

   static {
      char[] var10001 = "\u001a\fK\u0019x\u0012Y\u0000Cq\u0016\u0010V.}\u0019\u0007F\u0002sX\u0011LCw\u0018\u000e_\u0003z\u0012\r[\u001f;%\u0006^\u0019}\u0005\u0006l\u0003z\u0011\nH)l\u0003ME\u001f"
         .toCharArray();
      int var10003 = var10001.length;
      char[] var10002 = var10001;
      int var1 = var10003;

      for (int var0 = 0; var1 > var0; var0++) {
         char var10005 = var10002[var0];
         byte var10006;
         switch (var0 % 5) {
            case 0:
               var10006 = 119;
               break;
            case 1:
               var10006 = 99;
               break;
            case 2:
               var10006 = 47;
               break;
            case 3:
               var10006 = 108;
               break;
            default:
               var10006 = 20;
         }

         var10002[var0] = (char)(var10005 ^ var10006);
      }

      String var7 = new String(var10002).intern();
      byte var3 = -1;
      a = JsInfo.make(BOrd.make(var7));
   }
}
