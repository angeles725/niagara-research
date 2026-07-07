package com.tridium.px.editor.sidebars.layersheet;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.px.editor.event.PxLayerEvent;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.BLayerStatus;
import javax.baja.ui.px.PxLayer;
import javax.baja.util.Lexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

class ChangePxLayerValue extends Command {
   private BPxEditorPane editorPane;
   private BPxLayerSheet sheet;
   private BWbCellEditor ce;
   private PxLayer layer;
   private BLayerStatus oldStatus;
   private BLayerStatus newStatus;

   ChangePxLayerValue(BPxEditorPane editorPane, BPxLayerSheet sheet, BWbCellEditor ce, PxLayer layer, BLayerStatus newStatus) {
      super(sheet, Lexicon.make("pxEditor"), "commands.changeLayer");
      this.editorPane = editorPane;
      this.sheet = sheet;
      this.layer = layer;
      this.ce = ce;
      this.oldStatus = layer.getStatus();
      this.newStatus = newStatus;
   }

   public CommandArtifact doInvoke() throws Exception {
      ChangePxLayerValue.Artifact artifact = new ChangePxLayerValue.Artifact();
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
            ChangePxLayerValue.this.ce.loadValue(ChangePxLayerValue.this.newStatus);
            ChangePxLayerValue.this.ce.relayout();
         }

         ChangePxLayerValue.this.sheet.layerMgr.setLayerStatus(ChangePxLayerValue.this.layer, ChangePxLayerValue.this.newStatus);
         ChangePxLayerValue.this.sheet.updateUI(ChangePxLayerValue.this.layer, new PxLayerEvent(2, ChangePxLayerValue.this.layer));
      }

      public void undo() throws Exception {
         ChangePxLayerValue.this.ce.loadValue(ChangePxLayerValue.this.oldStatus);
         ChangePxLayerValue.this.ce.relayout();
         ChangePxLayerValue.this.sheet.layerMgr.setLayerStatus(ChangePxLayerValue.this.layer, ChangePxLayerValue.this.oldStatus);
         ChangePxLayerValue.this.sheet.updateUI(ChangePxLayerValue.this.layer, new PxLayerEvent(2, ChangePxLayerValue.this.layer));
      }
   }
}
