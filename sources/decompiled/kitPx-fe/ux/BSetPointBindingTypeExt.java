package com.tridium.kitPx.ux;

import com.tridium.kitPx.ux.fe.BKitPxJsBuild;
import javax.baja.bajascript.BBajaScriptTypeExt;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.JsInfo;

@NiagaraType(
   agent = {@AgentOn(
      types = {"kitPx:SetPointBinding"}
   )}
)
@NiagaraSingleton
public class BSetPointBindingTypeExt extends BBajaScriptTypeExt {
   public static final BSetPointBindingTypeExt INSTANCE = new BSetPointBindingTypeExt();
   public static final Type TYPE = Sys.loadType(BSetPointBindingTypeExt.class);
   private static final JsInfo jsInfo = JsInfo.make(BOrd.make("module://kitPx/rc/binding/SetPointBinding.js"), BKitPxJsBuild.TYPE);

   public Type getType() {
      return TYPE;
   }

   protected BSetPointBindingTypeExt() {
   }

   public JsInfo getTypeExtJs(Context cx) {
      return jsInfo;
   }
}
