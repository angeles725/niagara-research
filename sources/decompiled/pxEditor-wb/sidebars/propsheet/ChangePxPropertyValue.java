package com.tridium.px.editor.sidebars.propsheet;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.gx.BImage;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.sys.BValue;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxProperty;
import javax.baja.util.Lexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

class ChangePxPropertyValue extends Command {
   private BPxEditorPane editorPane;
   private BPxPropSheet sheet;
   private BWbCellEditor ce;
   private PxProperty group;
   private BValue oldValue;
   private BValue newValue;

   ChangePxPropertyValue(BPxEditorPane editorPane, BPxPropSheet sheet, BWbCellEditor ce, PxProperty group, BValue newValue) {
      super(sheet, Lexicon.make("pxEditor"), "commands.changeProperty");
      this.editorPane = editorPane;
      this.sheet = sheet;
      this.group = group;
      this.ce = ce;
      this.oldValue = group.getValue();
      this.newValue = newValue;
      if (newValue instanceof BImage) {
         BImage img = (BImage)newValue;
         img.setBaseOrd(editorPane.getPxEditor().getWbShell().getActiveOrd());
         img.sync();
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      ChangePxPropertyValue.Artifact artifact = new ChangePxPropertyValue.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private boolean firstTime = true;

      private Artifact() {
      }

      public void redo() throws Exception {
         if (this.firstTime) {
            this.firstTime = false;
         } else {
            ChangePxPropertyValue.this.ce.loadValue(ChangePxPropertyValue.this.newValue);
            ChangePxPropertyValue.this.ce.relayout();
         }

         this.setPropertyValue(ChangePxPropertyValue.this.newValue);
         ChangePxPropertyValue.this.sheet.updateUI(ChangePxPropertyValue.this.group, new PxPropertyEvent(2, ChangePxPropertyValue.this.group));
      }

      public void undo() throws Exception {
         ChangePxPropertyValue.this.ce.loadValue(ChangePxPropertyValue.this.oldValue);
         ChangePxPropertyValue.this.ce.relayout();
         this.setPropertyValue(ChangePxPropertyValue.this.oldValue);
         ChangePxPropertyValue.this.sheet.updateUI(ChangePxPropertyValue.this.group, new PxPropertyEvent(2, ChangePxPropertyValue.this.group));
      }

      private void setPropertyValue(BValue value) {
         ChangePxPropertyValue.this.group.setValue(value);
         ChangePxPropertyValue.this.editorPane.getPxPropertyComponents().setValue(ChangePxPropertyValue.this.group);
      }
   }
}
