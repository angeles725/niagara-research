package com.tridium.webChart.ux;

import com.tridium.web.BICollectionSupport;
import javax.baja.history.BIChartableHistoryAgent;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BSingleton;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.BIFormFactorMax;
import javax.baja.web.BIJavaScriptToPdf;
import javax.baja.web.js.JsInfo;
import javax.baja.webchart.ux.BWebChartJsBuild;

@NiagaraType(
   agent = {@AgentOn(
      types = {"history:HistorySpace", "history:HistoryFolder", "history:HistoryDevice", "history:IHistory", "history:HistoryFolder", "history:HistoryTimeQuery", "history:HistoryDeltaQuery", "control:NumericPoint", "control:EnumPoint", "control:BooleanPoint", "webChart:ChartFile", "schedule:NumericSchedule", "schedule:BooleanSchedule", "schedule:EnumSchedule", "niagaraVirtual:NiagaraVirtualBooleanPoint", "niagaraVirtual:NiagaraVirtualEnumPoint", "niagaraVirtual:NiagaraVirtualNumericPoint"},
      requiredPermissions = "r"
   )}
)
@NiagaraSingleton
public final class BChartWidget extends BSingleton implements BIJavaScriptToPdf, BIFormFactorMax, BIChartableHistoryAgent, BICollectionSupport {
   public static final BChartWidget INSTANCE = new BChartWidget();
   public static final Type TYPE = Sys.loadType(BChartWidget.class);
   private static final JsInfo jsInfo = JsInfo.make(BOrd.make("module://webChart/rc/ChartWidget.js"), BWebChartJsBuild.TYPE);

   public Type getType() {
      return TYPE;
   }

   private BChartWidget() {
   }

   public JsInfo getJsInfo(Context cx) {
      return jsInfo;
   }
}
