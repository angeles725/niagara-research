package com.tridium.webChart;

import javax.baja.agent.AgentList;
import javax.baja.file.BDataFile;
import javax.baja.file.BIFileStore;
import javax.baja.file.types.text.BITextFile;
import javax.baja.nre.annotations.FileExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   ext = {@FileExt(
      name = "chart"
   )}
)
public final class BChartFile extends BDataFile implements BITextFile {
   public static final Type TYPE = Sys.loadType(BChartFile.class);
   private static final BIcon icon = BIcon.std("charts/line.png");

   public Type getType() {
      return TYPE;
   }

   public BChartFile(BIFileStore store) {
      super(store);
   }

   public BChartFile() {
   }

   public String getMimeType() {
      return "application/json";
   }

   public BIcon getIcon() {
      return icon;
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.toTop("mobile:MobileHistoryAppChartView");
      agents.toTop("webChart:ChartWidget");
      return agents;
   }
}
