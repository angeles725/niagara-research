package com.tridium.template.ui;

import com.tridium.template.BConfigBinding;
import com.tridium.template.BTemplateConfig;
import java.util.ArrayList;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.query.BNull;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.TypeException;
import javax.baja.ui.BLabel;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;
import javax.baja.workbench.CannotSaveException;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BTemplateConfigFE extends BWbFieldEditor {
   public static final Type TYPE = Sys.loadType(BTemplateConfigFE.class);
   private static Lexicon lex = Lexicon.make("template");
   private BTemplateConfig deployConfig = null;
   private BGridPane gridPane = new BGridPane();

   public Type getType() {
      return TYPE;
   }

   public BTemplateConfigFE() {
      this.deployConfig = null;
      this.gridPane.setColumnCount(3);
      this.gridPane.setColumnAlign(BHalign.left);
      this.gridPane.setRowAlign(BValign.top);
      this.gridPane.setColumnGap(10.0);
      this.gridPane.setRowGap(6.0);
      this.setContent(this.gridPane);
   }

   protected void doLoadValue(BObject val, Context cx) throws Exception {
      this.gridPane.removeAll();
      BTemplateConfigFE.RowInfo[] values = this.makeRowValues(val, cx);
      if (values.length == 0) {
         this.addRowWidget(null, 0);
      } else {
         for (int i = 0; i < values.length; i++) {
            this.addRowWidget(values[i], i);
         }
      }

      this.relayout();
   }

   protected BObject doSaveValue(BObject val, Context cx) throws CannotSaveException, Exception {
      if (this.deployConfig == null) {
         throw new CannotSaveException(lex.getText("templateConfig.cannotSaveError"));
      } else {
         Property[] props = this.gridPane.getDynamicPropertiesArray();

         for (int i = 0; i < props.length; i += 3) {
            BLabel sourceSlotFe = (BLabel)this.gridPane.get(props[i]);
            BWbFieldEditor configFe = (BWbFieldEditor)this.gridPane.get(props[i + 1]);
            BValue configValue = (BValue)configFe.saveValue(cx);
            String sourceSlot = SlotPath.escape(sourceSlotFe.getText());
            Property configProperty = this.deployConfig.getProperty(sourceSlot);
            this.deployConfig.set(configProperty, configValue);
         }

         return this.deployConfig;
      }
   }

   protected BTemplateConfigFE.RowInfo[] makeRowValues(BObject val, Context cx) throws Exception {
      if (!val.getType().is(BTemplateConfig.TYPE)) {
         throw new TypeException(lex.getText("templateConfig.configTypeError"));
      } else {
         ArrayList<BTemplateConfigFE.RowInfo> configList = new ArrayList<>();
         BTemplateConfig tc = (BTemplateConfig)val;
         SlotCursor<Property> c = tc.getProperties();

         while (c.next()) {
            Property p = c.property();
            if (!p.isFrozen() && p.getType().is(BConfigBinding.TYPE)) {
               String userTip = ((BConfigBinding)tc.get(p)).getUserTip();
               String sourceSlot = ((BConfigBinding)tc.get(p)).getSourceSlot();
               BValue configValue = tc.get(sourceSlot);
               BTemplateConfigFE.RowInfo rowInfo = new BTemplateConfigFE.RowInfo();
               rowInfo.sourceSlot = sourceSlot;
               rowInfo.config = configValue.newCopy(true);
               rowInfo.userTip = userTip;
               configList.add(rowInfo);
            }
         }

         this.deployConfig = tc;
         return configList.toArray(new BTemplateConfigFE.RowInfo[0]);
      }
   }

   private void addRowWidget(BTemplateConfigFE.RowInfo rowVal, int rowNumber) {
      String nameSuffix = Integer.toString(rowNumber);
      String sourceSlot = "";
      BValue config = BNull.DEFAULT;
      String userTip = "";
      if (rowVal != null) {
         sourceSlot = SlotPath.unescape(rowVal.sourceSlot);
         config = rowVal.config;
         userTip = rowVal.userTip;
      }

      BWbFieldEditor configFe = BWbFieldEditor.makeFor(config);
      configFe.loadValue(config);
      this.gridPane.add("sourceSlot" + nameSuffix, new BLabel(sourceSlot));
      this.gridPane.add("config" + nameSuffix, configFe);
      this.gridPane.add("userTip" + nameSuffix, new BLabel(userTip));
      this.linkTo(configFe, BWbFieldEditor.setModified, setModified);
   }

   private static class RowInfo {
      String sourceSlot;
      BValue config;
      String userTip;

      private RowInfo() {
      }
   }
}
