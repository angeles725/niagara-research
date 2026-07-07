package com.honeywell.easybinding.service;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbstractService;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "updateVirtualEasyBindings",
      returnType = "BOrd",
      flags = 8
   ), @NiagaraAction(
      name = "updatePxPagesWithEncryptedEasyPallet",
      returnType = "BOrd",
      flags = 8
   )})
public class BEasyBindingSupportService extends BAbstractService {
   public static final Action updateVirtualEasyBindings;
   public static final Action updatePxPagesWithEncryptedEasyPallet;
   public static final Type TYPE;
   private static Type[] a;
   private static final Logger b;
   private static final String[] z;

   public BOrd updateVirtualEasyBindings() {
      return (BOrd)this.invoke(updateVirtualEasyBindings, null, null);
   }

   public BOrd updatePxPagesWithEncryptedEasyPallet() {
      return (BOrd)this.invoke(updatePxPagesWithEncryptedEasyPallet, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOrd doUpdateVirtualEasyBindings(Context var1) {
      if (this.getEnabled()) {
         BEasyBindingNiagaraVirtualSupportJob var2 = new BEasyBindingNiagaraVirtualSupportJob();
         return var2.submit(var1);
      } else {
         b.log(Level.INFO, z[1]);
         return BOrd.DEFAULT;
      }
   }

   public BOrd doUpdatePxPagesWithEncryptedEasyPallet(Context var1) {
      BOrd var2 = BOrd.DEFAULT;

      try {
         if (this.getEnabled()) {
            BUpgradeToEncryptedEasyWidgets var6 = new BUpgradeToEncryptedEasyWidgets();
            return var6.submit(var1);
         }

         b.log(Level.INFO, z[0]);
      } catch (Exception var5) {
         Exception var3 = var5;

         try {
            if (b.isLoggable(Level.FINER)) {
               b.log(Level.FINER, var3.getMessage(), (Throwable)var3);
            }
         } catch (Exception var4) {
            throw var4;
         }
      }

      return var2;
   }

   public Type[] getServiceTypes() {
      return a;
   }

   static {
      String[] var10000 = new String[2];
      String[] var10001 = var10000;
      byte var10002 = 0;
      String var10003 = "\u001d)?ot:!\"r=6/la=</)bt\u000e!>b!9$lE!(8#d x\u001b)d\"1+)6:7<ls:9* s0";
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
                  var10009 = 88;
                  break;
               case 1:
                  var10009 = 72;
                  break;
               case 2:
                  var10009 = 76;
                  break;
               case 3:
                  var10009 = 22;
                  break;
               default:
                  var10009 = 84;
            }

            var17[var0] = (char)(var10008 ^ var10009);
         }

         String var22 = new String(var17).intern();
         switch (var10004) {
            case 0:
               var10001[var10002] = var22;
               z = var10000;
               updateVirtualEasyBindings = newAction(8, null);
               updatePxPagesWithEncryptedEasyPallet = newAction(8, null);
               TYPE = Sys.loadType(BEasyBindingSupportService.class);
               a = new Type[]{TYPE};
               char[] var2 = "\u001d)?o\u00161&(\u007f:?".toCharArray();
               int var9 = var2.length;
               char[] var6 = var2;
               int var3 = var9;

               for (int var1 = 0; var3 > var1; var1++) {
                  char var19 = var6[var1];
                  byte var23;
                  switch (var1 % 5) {
                     case 0:
                        var23 = 88;
                        break;
                     case 1:
                        var23 = 72;
                        break;
                     case 2:
                        var23 = 76;
                        break;
                     case 3:
                        var23 = 22;
                        break;
                     default:
                        var23 = 84;
                  }

                  var6[var1] = (char)(var19 ^ var23);
               }

               var10003 = new String(var6).intern();
               byte var5 = -1;
               b = Logger.getLogger(var10003);
               return;
            default:
               var10001[var10002] = var22;
               var10001 = var10000;
               var10002 = 1;
               var10003 = "\u001d)?ot:!\"r=6/la=</)bt\u000e!>b!9$lE!(8#d x\u001b)d\"1+)6:7<ls:9* s0";
               var10004 = 0;
         }
      }
   }
}
