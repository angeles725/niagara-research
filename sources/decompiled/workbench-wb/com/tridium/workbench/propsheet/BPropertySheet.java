package com.tridium.workbench.propsheet;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.util.BTitlePane;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Component", "baja:IPropertyContainer"},
      requiredPermissions = "r"
   )}
)
public class BPropertySheet extends BWbComponentView {
   public static final Type TYPE = Sys.loadType(BPropertySheet.class);
   BFieldEditorSheet sheet;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPropertySheet() {
      this.autoRegisterForComponentEvents = false;
   }

   public void build() {
      this.sheet = new BFieldEditorSheet(this.getSubscribeDepth());
      this.sheet.unsubscribeOnCollapse = this.getUnsubscribeOnCollapse();
      this.linkTo(this.sheet, BFieldEditorSheet.pluginModified, setModified);
      this.linkTo(this.sheet, BFieldEditorSheet.actionPerformed, actionPerformed);
      BWbShell shell = this.getWbShell();
      if (shell != null) {
         BGridPane buttons = new BGridPane(2);
         buttons.setColumnAlign(BHalign.fill);
         buttons.setUniformColumnWidth(true);
         buttons.add(null, new BButton(shell.getRefreshCommand()));
         buttons.add(null, new BButton(shell.getSaveCommand()));
         BEdgePane edge = new BEdgePane();
         edge.setCenter(this.sheet);
         edge.setBottom(buttons);
         this.setContent(new BTitlePane("Property Sheet", edge));
      } else {
         this.setContent(this.sheet);
      }
   }

   protected int getSubscribeDepth() {
      return 1;
   }

   protected boolean getUnsubscribeOnCollapse() {
      return false;
   }

   @Override
   public void doLoadValue(BObject value, Context cx) {
      this.build();
      this.sheet.loadValue(value, cx);
      PropSheetState.restore(this);
   }

   @Override
   public BObject doSaveValue(BObject value, Context cx) throws Exception {
      return this.sheet.saveValue(cx);
   }

   @Override
   public void deactivated() {
      super.deactivated();
      PropSheetState.save(this);
   }

   @Override
   public void handleComponentEvent(BComponentEvent event) {
      if (this.sheet != null) {
         this.sheet.handleComponentEvent(event);
      }
   }
}
