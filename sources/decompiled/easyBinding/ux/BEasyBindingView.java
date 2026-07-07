package com.honeywell.easybinding.easybinding;

import com.tridium.workbench.shell.BNiagaraWbShell;
import com.tridium.workbench.web.browser.BWebWidget;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BLayout;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.view.BWbView;

public class BEasyBindingView extends BWbView {
   private static Logger a;
   public static final Type TYPE;
   private static final String z;

   public Type getType() {
      return TYPE;
   }

   public void doLoadValue(BObject var1, Context var2) throws Exception {
      BGridPane var3 = new BGridPane(1);
      var3.setLayout(BLayout.makeAbs(0.0, 0.0, 1024.0, 768.0));
      BWebWidget var4 = new BWebWidget(BEasyBinding.TYPE.getTypeInfo().getAgentInfo());
      var4.loadValue(var1);
      var3.add(z, var4);
      this.setContent(new BBorderPane(var3, BBorder.none));
      this.a();
   }

   private void a() {
      BWbShell var1 = this.getWbShell();
      if (var1 instanceof BNiagaraWbShell) {
         try {
            ((BNiagaraWbShell)this.getWbShell()).commands.showSideBar.setSelected(false);
            ((BNiagaraWbShell)this.getWbShell()).commands.showSideBar.doInvoke();
         } catch (Exception var4) {
            Exception var2 = var4;

            try {
               if (a.isLoggable(Level.SEVERE)) {
                  a.log(Level.SEVERE, var2.getMessage(), (Throwable)var2);
               }
            } catch (Exception var3) {
               throw var3;
            }
         }
      }
   }

   static {
      String var10000 = "]2n\u007f`^";
      int var10001 = -1;

      while (true) {
         char[] var3 = var10000.toCharArray();
         int var10003 = var3.length;
         char[] var6 = var3;
         var10001 = var10003;

         for (int var0 = 0; var10001 > var0; var0++) {
            char var10005 = var6[var0];
            byte var10006;
            switch (var0 % 5) {
               case 0:
                  var10006 = 42;
                  break;
               case 1:
                  var10006 = 91;
                  break;
               case 2:
                  var10006 = 10;
                  break;
               case 3:
                  var10006 = 24;
                  break;
               default:
                  var10006 = 5;
            }

            var6[var0] = (char)(var10005 ^ var10006);
         }

         String var10 = new String(var6).intern();
         switch (var10001) {
            case 0:
               a = Logger.getLogger(var10);
               TYPE = Sys.loadType(BEasyBindingView.class);
               return;
            default:
               z = var10;
               var10000 = "o:yaGC5nqkM\u000eC";
               var10001 = 0;
         }
      }
   }
}
