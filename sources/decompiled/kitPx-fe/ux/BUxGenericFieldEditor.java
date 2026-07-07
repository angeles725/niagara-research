package com.tridium.kitPx.ux.fe;

import javax.baja.bajaux.BIJavaScriptWidget;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BSingleton;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.JsInfo;

@NiagaraType(
   agent = {@AgentOn(
      types = {"kitPx:GenericFieldEditor"}
   )}
)
@NiagaraSingleton
public class BUxGenericFieldEditor extends BSingleton implements BIJavaScriptWidget {
   public static final BUxGenericFieldEditor INSTANCE = new BUxGenericFieldEditor();
   public static final Type TYPE = Sys.loadType(BUxGenericFieldEditor.class);
   private static final JsInfo JS_INFO = JsInfo.make(BOrd.make("module://kitPx/rc/fe/GenericFieldEditor.js"), BKitPxJsBuild.TYPE);

   public Type getType() {
      return TYPE;
   }

   protected BUxGenericFieldEditor() {
   }

   public JsInfo getJsInfo(Context cx) {
      return JS_INFO;
   }
}
