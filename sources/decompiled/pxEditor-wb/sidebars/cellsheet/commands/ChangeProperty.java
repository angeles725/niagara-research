package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import com.tridium.px.editor.util.EventUtil;
import javax.baja.gx.BImage;
import javax.baja.sys.BValue;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class ChangeProperty extends Command {
   private BPxEditorPane editorPane;
   private BPxCellSheet cellSheet;
   private BWidget[] widgets;
   private String propertyName;
   private BValue[] oldValues;
   private BValue[] newValues;

   public ChangeProperty(BPxEditorPane editorPane, BPxCellSheet cellSheet, String propertyName, BValue newValue) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.changeProperty");
      this.editorPane = editorPane;
      this.cellSheet = cellSheet;
      this.widgets = editorPane.getSelectedWidgets().getWidgets();
      this.propertyName = propertyName;
      this.oldValues = new BValue[this.widgets.length];
      this.newValues = new BValue[this.widgets.length];

      for (int i = 0; i < this.widgets.length; i++) {
         this.oldValues[i] = this.widgets[i].get(propertyName);
         this.newValues[i] = newValue.newCopy();
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      ChangeProperty.Artifact artifact = new ChangeProperty.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private boolean initialInvocation = true;

      private Artifact() {
      }

      public void redo() throws Exception {
         this.perform(ChangeProperty.this.newValues);
      }

      public void undo() throws Exception {
         this.perform(ChangeProperty.this.oldValues);
      }

      private void perform(BValue[] values) {
         for (int i = 0; i < ChangeProperty.this.widgets.length; i++) {
            if (values[i] instanceof BImage) {
               BImage img = (BImage)values[i];
               img.setBaseOrd(ChangeProperty.this.editorPane.getPxEditor().getWbShell().getActiveOrd());
               img.sync();
            }

            ChangeProperty.this.widgets[i].set(ChangeProperty.this.propertyName, values[i]);
            ChangeProperty.this.widgets[i].bindingsChanged();
         }

         ChangeProperty.this.editorPane.getSelectedWidgets().setWidgets(ChangeProperty.this.widgets);
         if (this.initialInvocation) {
            ChangeProperty.this.editorPane.getPxEditor().removePxListener(ChangeProperty.this.cellSheet);
            ChangeProperty.this.editorPane.getPxEditor().firePxEvent(EventUtil.widgetsChanged(ChangeProperty.this.widgets, ChangeProperty.this.propertyName));
            ChangeProperty.this.editorPane.getPxEditor().addPxListener(ChangeProperty.this.cellSheet);
            this.initialInvocation = false;
         } else {
            ChangeProperty.this.editorPane.getPxEditor().firePxEvent(EventUtil.widgetsChanged(ChangeProperty.this.widgets, ChangeProperty.this.propertyName));
         }
      }
   }
}
