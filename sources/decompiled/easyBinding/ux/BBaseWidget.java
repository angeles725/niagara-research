package com.honeywell.easybinding;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BSingleton;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.BIFormFactorMax;
import javax.baja.web.js.BIJavaScript;
import javax.baja.web.js.JsInfo;

@NiagaraType
public abstract class BBaseWidget extends BSingleton implements BIJavaScript, BIFormFactorMax {
   public static final Type TYPE = Sys.loadType(BBaseWidget.class);

   public Type getType() {
      return TYPE;
   }

   protected BBaseWidget() {
   }

   public final JsInfo getJsInfo(Context var1) {
      return this.getGuiJsInfo(var1);
   }

   public abstract JsInfo getGuiJsInfo(Context var1);
}
