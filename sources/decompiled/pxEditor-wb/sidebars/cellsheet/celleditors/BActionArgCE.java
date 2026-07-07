package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import javax.baja.agent.AgentFilter;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.workbench.celleditor.BButtonCE;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BActionArgCE extends BButtonCE {
   public static final Type TYPE = Sys.loadType(BActionArgCE.class);
   private boolean readonly;
   private Action action;

   public Type getType() {
      return TYPE;
   }

   public BActionArgCE(Action action) {
      this.action = action;
   }

   protected void doSetReadonly(boolean readonly) {
      this.readonly = readonly;
   }

   protected void doLoadValue(BObject target, Context context) throws Exception {
      if (this.action != null && this.action.getParameterType() != null) {
         if (!this.readonly) {
            this.setReadonly(false);
         }
      } else {
         if (!this.readonly) {
            this.setReadonly(true);
         }

         target = BString.DEFAULT;
      }

      super.doLoadValue(target, context);
   }

   protected BObject dialog() throws Exception {
      BObject target = this.value;
      boolean doNull = false;
      if (target instanceof BString && ((BString)target).equals(BString.DEFAULT)) {
         target = this.action.getParameterDefault();
         doNull = true;
      }

      BWbFieldEditor inner = (BWbFieldEditor)target.getAgents().filter(AgentFilter.is(BWbFieldEditor.TYPE)).getDefault().getInstance();
      BActionArg arg = new BActionArg(inner, this.action, target, this.getCurrentContext(), doNull);
      int result = BDialog.open(this, this.getPropertyName(), arg, 3);
      return result == 2 ? null : arg.save();
   }
}
