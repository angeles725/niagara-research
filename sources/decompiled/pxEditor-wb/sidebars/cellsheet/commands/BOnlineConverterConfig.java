package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.fieldeditors.BIEnumToSimpleFE;
import javax.baja.agent.AgentFilter;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.util.BConverter;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "bindingComboChanged"
   ), @NiagaraAction(
      name = "converterComboChanged"
   )})
public class BOnlineConverterConfig extends BConverterConfig {
   public static final Action bindingComboChanged = newAction(0, null);
   public static final Action converterComboChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BOnlineConverterConfig.class);
   private BConverter[] conv;
   private BBinding[] bindings;
   private BObject[] from;
   private BObject to;
   private TypeInfo toType;
   private BWbFieldEditor fe;
   private BListDropDown bindCombo = new BListDropDown();
   private BListDropDown convCombo = new BListDropDown();

   public void bindingComboChanged() {
      this.invoke(bindingComboChanged, null, null);
   }

   public void converterComboChanged() {
      this.invoke(converterComboChanged, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BOnlineConverterConfig(BBinding[] bindings, BObject[] from, BObject to) {
      this.bindings = bindings;
      this.from = from;
      this.to = to;
      this.toType = to.getType().getTypeInfo();

      for (int i = 0; i < bindings.length; i++) {
         TypeInfo t = bindings[i].getType().getTypeInfo();
         this.bindCombo.getList().addItem(t.getModuleName() + ":" + t.getDisplayName(null) + " [" + bindings[i].getOrd() + "]");
         TypeInfo fromType = from[i].getType().getTypeInfo();
         TypeInfo[] adapters = Sys.getRegistry().getAdapters(fromType, this.toType);
         BConverter[] conv = new BConverter[adapters.length];

         for (int j = 0; j < adapters.length; j++) {
            conv[j] = (BConverter)adapters[j].getInstance();
            conv[j].init(from[i], to);
         }
      }

      BGridPane grid = new BGridPane(1);
      grid.setHalign(BHalign.left);
      grid.setUniformColumnWidth(true);
      grid.add(null, new BLabel(BPxEditorPane.text("converter.binding"), BHalign.left));
      grid.add(null, this.bindCombo);
      grid.add(null, new BLabel(BPxEditorPane.text("converter.type"), BHalign.left));
      grid.add(null, this.convCombo);
      this.setTop(grid);
      this.bindCombo.getList().setSelectedIndex(0);
      this.loadBinding(bindings[0], from[0]);
      this.linkTo(null, this.bindCombo, BListDropDown.listActionPerformed, bindingComboChanged);
      this.linkTo(null, this.convCombo, BListDropDown.listActionPerformed, converterComboChanged);
   }

   public void doBindingComboChanged() {
      int a = this.bindCombo.getSelectedIndex();
      if (a != -1) {
         this.loadBinding(this.bindings[a], this.from[a]);
      }
   }

   public void doConverterComboChanged() {
      int a = this.bindCombo.getSelectedIndex();
      if (a != -1) {
         int b = this.convCombo.getSelectedIndex();
         if (b != -1) {
            this.loadFe(this.conv[b], this.from[a]);
            this.relayout();
         }
      }
   }

   private void loadBinding(BBinding binding, BObject from) {
      TypeInfo fromType = from.getType().getTypeInfo();
      TypeInfo[] adapters = Sys.getRegistry().getAdapters(fromType, this.toType);
      this.conv = new BConverter[adapters.length];
      this.convCombo.getList().removeAllItems();

      for (int j = 0; j < adapters.length; j++) {
         this.conv[j] = (BConverter)adapters[j].getInstance();
         this.conv[j].init(from, this.to);
         this.convCombo.getList().addItem(this.name(this.conv[j]));
      }

      this.convCombo.getList().setSelectedIndex(0);
      this.loadFe(this.conv[0], from);
   }

   private void loadFe(BConverter c, BObject from) {
      this.fe = this.makeFe(c, from);
      BScrollPane scrollPane = new BScrollPane();
      scrollPane.setContent(this.fe);
      BConstrainedPane cons = new BConstrainedPane(scrollPane);
      cons.setMinWidth(150.0);
      cons.setMinHeight(250.0);
      this.setCenter(new BBorderPane(cons, 10.0, 0.0, 0.0, 0.0));
   }

   private BWbFieldEditor makeFe(BConverter c, BObject from) {
      BWbFieldEditor fe = (BWbFieldEditor)c.getAgents().filter(AgentFilter.is(BWbFieldEditor.TYPE)).getDefault().getInstance();
      BFacets facets = null;
      if (fe instanceof BIEnumToSimpleFE) {
         BIEnum en = (BIEnum)from;
         if (en instanceof BComponent) {
            ((BComponent)en).loadSlots();
         }

         facets = en.getEnumFacets();
         BObject range = facets.get("range");
         if (!(range instanceof BEnumRange) || range.isNull()) {
            facets = en.getEnum().getEnumFacets();
         }
      }

      fe.loadValue(c, facets);
      fe.computePreferredSize();
      return fe;
   }

   private String name(BConverter c) {
      String name = TextUtil.getClassName(c.getClass());
      return TextUtil.toFriendly(name.substring(1));
   }

   @Override
   BBinding binding() {
      int n = this.bindCombo.getSelectedIndex();
      return this.bindings[n];
   }

   @Override
   BConverter converter() {
      try {
         return (BConverter)this.fe.saveValue();
      } catch (Exception var2) {
         throw new BajaRuntimeException(var2);
      }
   }
}
