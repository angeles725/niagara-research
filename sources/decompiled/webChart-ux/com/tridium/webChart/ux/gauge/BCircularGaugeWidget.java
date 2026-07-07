package com.tridium.webChart.ux.gauge;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BSingleton;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.BIFormFactorCompact;
import javax.baja.web.BIOffline;
import javax.baja.web.js.BIJavaScript;
import javax.baja.web.js.JsInfo;
import javax.baja.webchart.ux.BWebChartJsBuild;

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:NumericPoint", "control:EnumPoint", "control:BooleanPoint", "schedule:NumericSchedule", "schedule:NumericScheduleSelector", "kitControl:KitNumeric", "kitControl:NumericDelay", "kitControl:NumericConst", "niagaraVirtual:NiagaraVirtualBooleanPoint", "niagaraVirtual:NiagaraVirtualEnumPoint", "niagaraVirtual:NiagaraVirtualNumericPoint"},
      requiredPermissions = "r"
   )}
)
@NiagaraSingleton
public final class BCircularGaugeWidget extends BSingleton implements BIJavaScript, BIFormFactorCompact, BIOffline {
   public static final BCircularGaugeWidget INSTANCE = new BCircularGaugeWidget();
   public static final Type TYPE = Sys.loadType(BCircularGaugeWidget.class);
   private static final JsInfo jsInfo = JsInfo.make(BOrd.make("module://webChart/rc/gauge/CircularGaugeWidget.js"), BWebChartJsBuild.TYPE);

   public Type getType() {
      return TYPE;
   }

   private BCircularGaugeWidget() {
   }

   public JsInfo getJsInfo(Context cx) {
      return jsInfo;
   }
}
