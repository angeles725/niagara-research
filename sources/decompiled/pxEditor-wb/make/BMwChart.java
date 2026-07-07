package com.tridium.px.editor.make;

import com.tridium.bql.util.BDynamicTimeRange;
import com.tridium.bql.util.BDynamicTimeRangeType;
import com.tridium.util.ClassUtil;
import com.tridium.workbench.fieldeditors.BDynamicTimeRangeFE;
import com.tridium.workbench.fieldeditors.BTimeEditors;
import com.tridium.workbench.util.PropertyManager;
import javax.baja.chart.BChartPane;
import javax.baja.chart.BLineChart;
import javax.baja.chart.binding.BColumnIdentifier;
import javax.baja.chart.binding.BTableChartBinding;
import javax.baja.collection.BITable;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BLabel;
import javax.baja.ui.BLayout;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
@NiagaraAction(
   name = "typeChoiceChanged"
)
public class BMwChart extends BMwConfig {
   public static final Action typeChoiceChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BMwChart.class);
   private static Lexicon historyLex = Lexicon.make("history");
   private static final BBrush[] BRUSHES = new BBrush[]{BColor.blue.toBrush(), BColor.red.toBrush(), BColor.green.toBrush(), BColor.purple.toBrush()};
   private BListDropDown typeChoice;
   private BTimeEditors eds;
   private BTextField column0 = new BTextField();
   private BTextField column1 = new BTextField();

   public void typeChoiceChanged() {
      this.invoke(typeChoiceChanged, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwChart() {
      this.command = new BMwChart.Cmd();
      this.typeChoice = BDynamicTimeRangeFE.makeTypeChoice();
      this.eds = new BTimeEditors(BAbsTime.NULL, BAbsTime.NULL, null);
      this.eds.setVisible(false);
      BGridPane g2 = new BGridPane(2);
      g2.setValign(BValign.top);
      g2.setHalign(BHalign.left);
      g2.add(null, new BLabel(BMakeWidget.text("chartColumn0")));
      g2.add(null, this.column0);
      g2.add(null, new BLabel(BMakeWidget.text("chartColumn1")));
      g2.add(null, this.column1);
      g2.add(null, new BLabel(historyLex.getText("chartBuilder.timeRange")));
      g2.add(null, this.typeChoice);
      BGridPane g1 = new BGridPane(1);
      g1.setValign(BValign.top);
      g1.setHalign(BHalign.left);
      g1.add(null, g2);
      g1.add(null, this.eds);
      BConstrainedPane cons = new BConstrainedPane(g1);
      this.eds.computePreferredSize();
      cons.setMinWidth(this.eds.getPreferredWidth());
      this.setContent(new BBorderPane(cons, BBorder.groove));
      this.linkTo(null, this.typeChoice, BListDropDown.listActionPerformed, typeChoiceChanged);
   }

   public void doTypeChoiceChanged() {
      BDynamicTimeRangeType type = (BDynamicTimeRangeType)this.typeChoice.getSelectedItem();
      this.eds.setVisible(type == BDynamicTimeRangeType.timeRange);
   }

   @Override
   public void load() {
      BObject[] objects = this.mw.getDrawingObjects();
      this.command.setEnabled(ClassUtil.all(objects, BITable.class));
   }

   @Override
   public void setWorkingWidget() {
      throw new IllegalStateException();
   }

   @Override
   public BWidget[] makePxWidgets(WidgetCopier wc) {
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
         pane.getHeader().setTitle(BMakeWidget.text("chart"));
      }

      BLineChart chart = new BLineChart();
      pane.add(null, chart);
      String timeQuery = this.timeQuery();

      for (int i = 0; i < objects.length; i++) {
         BTableChartBinding binding = new BTableChartBinding();
         chart.add(null, binding);
         binding.setOrd(BOrd.make(ords[i].toString() + timeQuery));
         binding.setBrush(BRUSHES[i % BRUSHES.length]);
         binding.setXColumn(BColumnIdentifier.makeTableColumn(this.column0.getText()));
         binding.setYColumn(BColumnIdentifier.makeTableColumn(this.column1.getText()));
      }

      return new BWidget[]{pane};
   }

   protected String timeQuery() {
      BDynamicTimeRangeType type = (BDynamicTimeRangeType)this.typeChoice.getSelectedItem();
      if (type == BDynamicTimeRangeType.timeRange) {
         BAbsTime start = this.eds.getStartTime();
         BAbsTime end = this.eds.getEndTime();
         if (start.isNull()) {
            return end.isNull() ? "" : "?end=" + end.encodeToString();
         } else {
            return "?start=" + start.encodeToString() + (end.isNull() ? "" : ";end=" + end.encodeToString());
         }
      } else {
         return "?period=" + BDynamicTimeRange.make(type).toString();
      }
   }

   @Override
   public void pickle(PropertyManager propMgr) {
      propMgr.set("mwChartColumn0", this.column0.getText());
      propMgr.set("mwChartColumn1", this.column1.getText());
      BDynamicTimeRangeType type = (BDynamicTimeRangeType)this.typeChoice.getSelectedItem();
      propMgr.seti("mwChartRangeType", type.getOrdinal());
      if (type == BDynamicTimeRangeType.timeRange) {
         propMgr.set("mwChartStartTime", this.eds.getStartTime().encodeToString());
         propMgr.set("mwChartEndTime", this.eds.getEndTime().encodeToString());
      }
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
      this.column0.setText(propMgr.get("mwChartColumn0", "timestamp"));
      this.column1.setText(propMgr.get("mwChartColumn1", "value"));
      BDynamicTimeRangeType type = BDynamicTimeRangeType.make(propMgr.geti("mwChartRangeType", BDynamicTimeRangeType.today.getOrdinal()));
      this.typeChoice.setSelectedItem(type);
      this.eds.setVisible(type == BDynamicTimeRangeType.timeRange);
      if (type == BDynamicTimeRangeType.timeRange) {
         try {
            this.eds.setStartTime((BAbsTime)BAbsTime.NULL.decodeFromString(propMgr.get("mwChartStartTime", BAbsTime.NULL.encodeToString())));
            this.eds.setEndTime((BAbsTime)BAbsTime.NULL.decodeFromString(propMgr.get("mwChartEndTime", BAbsTime.NULL.encodeToString())));
         } catch (Exception var4) {
            throw new BajaRuntimeException(var4);
         }
      }
   }

   protected class Cmd extends ToggleCommand {
      public Cmd() {
         super(BMwChart.this, BMakeWidget.text("chart"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwChart.this.mw.getRightPane().setContent(new BNullWidget());
            BMwChart.this.mw.getLeftPane().setContent(BMwChart.this);
         }

         return null;
      }
   }
}
