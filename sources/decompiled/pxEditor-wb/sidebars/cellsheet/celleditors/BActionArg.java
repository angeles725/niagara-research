package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
@NiagaraAction(
   name = "innerModified"
)
public class BActionArg extends BEdgePane {
   public static final Action innerModified = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BActionArg.class);
   private BWbFieldEditor inner;
   private Action action;
   private BObject target;
   private Context context;
   private boolean modified = false;
   private boolean loading = false;
   private BCheckBox isNull = new BCheckBox(new BActionArg.Null());

   public void innerModified() {
      this.invoke(innerModified, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BActionArg(BWbFieldEditor inner, Action action, BObject target, Context context, boolean doNull) {
      this.inner = inner;
      this.action = action;
      this.target = target;
      this.context = context;
      inner.computePreferredSize();
      BConstrainedPane cons = new BConstrainedPane(inner);
      cons.setMinSize(inner.getPreferredWidth(), inner.getPreferredHeight());
      this.setCenter(cons);
      this.setBottom(this.isNull);
      this.loading = true;
      this.isNull.setSelected(doNull);
      this.loading = false;
      if (doNull) {
         inner.setReadonly(true);
      }

      inner.loadValue(target, context);
      this.linkTo(inner, BWbFieldEditor.pluginModified, innerModified);
   }

   public void doInnerModified() {
      this.modified = true;
   }

   BValue save() {
      if (!this.modified) {
         return null;
      } else if (this.isNull.isSelected()) {
         return BString.DEFAULT;
      } else {
         try {
            return (BValue)this.inner.saveValue();
         } catch (Exception var2) {
            throw new BajaRuntimeException(var2.getMessage(), var2.getCause());
         }
      }
   }

   private class Null extends ToggleCommand {
      private Null() {
         super(BActionArg.this, BPxEditorPane.text("actionArg.prompUser"));
      }

      public CommandArtifact doInvoke() {
         if (BActionArg.this.loading) {
            return null;
         } else {
            boolean selected = BActionArg.this.isNull.isSelected();
            BActionArg.this.inner.setReadonly(selected);
            BActionArg.this.inner
               .loadValue((BObject)(selected ? BActionArg.this.action.getParameterDefault() : BActionArg.this.target), BActionArg.this.context);
            BActionArg.this.inner.setModified();
            BActionArg.this.modified = true;
            return null;
         }
      }
   }
}
