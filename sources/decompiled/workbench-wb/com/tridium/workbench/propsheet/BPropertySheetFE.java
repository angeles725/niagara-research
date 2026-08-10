package com.tridium.workbench.propsheet;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Complex", "baja:IPropertyContainer"}
   )}
)
public class BPropertySheetFE extends BWbFieldEditor implements BWbFieldEditor.IDialogContentProvider {
   public static final Type TYPE = Sys.loadType(BPropertySheetFE.class);
   private final BFieldEditorSheet sheet = new BFieldEditorSheet();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPropertySheetFE() {
      this.linkTo(this.sheet, BPropertySheet.pluginModified, setModified);
      this.linkTo(this.sheet, BPropertySheet.actionPerformed, actionPerformed);
      this.setContent(this.sheet);
   }

   @Override
   public void doSetReadonly(boolean readonly) {
      this.sheet.setReadonly(readonly);
   }

   @Override
   public void doLoadValue(BObject value, Context cx) {
      this.sheet.loadValue(value, cx);
   }

   @Override
   public BObject doSaveValue(BObject value, Context cx) throws Exception {
      return this.sheet.saveValue(cx);
   }

   @Override
   public BWidget getDialogContent() {
      BConstrainedPane constrainedPane = new BConstrainedPane(this);
      constrainedPane.setMinWidth(400.0);
      constrainedPane.setMinHeight(300.0);
      return constrainedPane;
   }
}
