package com.tridium.px.editor.make;

import com.tridium.util.ClassUtil;
import com.tridium.workbench.fieldeditors.BIntegerFE;
import com.tridium.workbench.util.PropertyManager;
import javax.baja.chart.BChartPane;
import javax.baja.chart.BLineChart;
import javax.baja.chart.binding.BAxisSpec;
import javax.baja.chart.binding.BValueChartBinding;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDouble;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BLabel;
import javax.baja.ui.BLayout;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.BTypeSpec;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
public class BMwTimePlot extends BMwConfig {
   public static final Type TYPE = Sys.loadType(BMwTimePlot.class);
   private static final BBrush[] BRUSHES = new BBrush[]{BColor.blue.toBrush(), BColor.red.toBrush(), BColor.green.toBrush(), BColor.purple.toBrush()};
   private BIntegerFE min = new BIntegerFE();
   private BIntegerFE max = new BIntegerFE();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwTimePlot() {
      this.command = new BMwTimePlot.Cmd();
      this.min.loadValue(BInteger.make(0));
      this.max.loadValue(BInteger.make(100));
      BGridPane g2 = new BGridPane(2);
      g2.setValign(BValign.top);
      g2.setHalign(BHalign.left);
      g2.add(null, new BLabel(BMakeWidget.text("timePlotMin")));
      g2.add(null, this.min);
      g2.add(null, new BLabel(BMakeWidget.text("timePlotMax")));
      g2.add(null, this.max);
      this.setContent(new BBorderPane(g2, BBorder.groove));
   }

   @Override
   public void load() {
      BObject[] objects = this.mw.getDrawingObjects();
      this.command.setEnabled(ClassUtil.all(objects, BINumeric.class));
   }

   @Override
   public void setWorkingWidget() {
      throw new IllegalStateException();
   }

   @Override
   public BWidget[] makePxWidgets(WidgetCopier wc) {
      BDouble minVal = saveFE(this.min, BDouble.make(0.0));
      BDouble maxVal = saveFE(this.max, BDouble.make(100.0));
      BChartPane pane = new BChartPane();
      pane.setLayout(BLayout.make("0,0,200,150"));
      BObject[] objects = this.mw.getDrawingObjects();
      BOrd[] ords = this.mw.getOrds();
      if (objects.length == 1) {
         String str = ords[0].toString();
         int n = str.lastIndexOf(124);
         if (n != -1) {
            str = str.substring(n + 1);
         }

         n = str.lastIndexOf(58);
         if (n != -1) {
            str = str.substring(n + 1);
         }

         pane.getHeader().setTitle(str);
      } else {
         pane.getHeader().setTitle(BMakeWidget.text("timePlot"));
      }

      BLineChart chart = new BLineChart();
      pane.add(null, chart);

      for (int i = 0; i < objects.length; i++) {
         BValueChartBinding binding = new BValueChartBinding();
         chart.add(null, binding);
         binding.setOrd(ords[i]);
         binding.setBrush(BRUSHES[i % BRUSHES.length]);
         binding.setXAxis(new BAxisSpec(BTypeSpec.make("baja", "AbsTime")));
         binding.setYAxis(new BAxisSpec(BTypeSpec.make("baja", "INumeric"), minVal, maxVal));
      }

      return new BWidget[]{pane};
   }

   private static BDouble saveFE(BIntegerFE fe, BDouble defVal) {
      try {
         return BDouble.make(((BInteger)fe.saveValue()).getDouble());
      } catch (Exception var3) {
         var3.printStackTrace();
         return defVal;
      }
   }

   @Override
   public void pickle(PropertyManager propMgr) {
      try {
         propMgr.seti("mwTimePlotMin", ((BInteger)this.min.saveValue()).getInt());
         propMgr.seti("mwTimePlotMax", ((BInteger)this.max.saveValue()).getInt());
      } catch (Exception var3) {
         throw new BajaRuntimeException(var3);
      }
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
      this.min.loadValue(BInteger.make(propMgr.geti("mwTimePlotMin", 0)));
      this.max.loadValue(BInteger.make(propMgr.geti("mwTimePlotMax", 100)));
   }

   private class Cmd extends ToggleCommand {
      Cmd() {
         super(BMwTimePlot.this, BMakeWidget.text("timePlot"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwTimePlot.this.mw.getRightPane().setContent(new BNullWidget());
            BMwTimePlot.this.mw.getLeftPane().setContent(BMwTimePlot.this);
         }

         return null;
      }
   }
}
