package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import com.tridium.px.editor.util.LayerManager;
import javax.baja.px.editor.event.PxLayerEvent;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.BLayerTag;
import javax.baja.ui.px.PxLayer;

public class ChangeLayer extends Command {
   private BPxEditorPane editorPane;
   private LayerManager layerMgr;
   private BPxCellSheet cellSheet;
   private BWidget[] widgets;
   private PxLayer[] oldLayers;
   private PxLayer[] newLayers;

   public ChangeLayer(BPxEditorPane editorPane, BPxCellSheet cellSheet, PxLayer changeTo) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.changeLayer");
      this.editorPane = editorPane;
      this.layerMgr = editorPane.getLayerManager();
      this.widgets = editorPane.getSelectedWidgets().getWidgets();
      this.cellSheet = cellSheet;
      this.oldLayers = new PxLayer[this.widgets.length];
      this.newLayers = new PxLayer[this.widgets.length];

      for (int i = 0; i < this.widgets.length; i++) {
         BLayerTag tag = this.layerMgr.getTag(this.widgets[i]);
         this.oldLayers[i] = tag == null ? null : this.layerMgr.getLayerByName(tag.getLayerName());
         this.newLayers[i] = changeTo;
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      ChangeLayer.Artifact artifact = new ChangeLayer.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private Artifact() {
      }

      public void redo() throws Exception {
         this.perform(ChangeLayer.this.newLayers);
      }

      public void undo() throws Exception {
         this.perform(ChangeLayer.this.oldLayers);
      }

      private void perform(PxLayer[] layers) {
         for (int i = 0; i < ChangeLayer.this.widgets.length; i++) {
            ChangeLayer.this.layerMgr.removeTag(ChangeLayer.this.widgets[i]);
            if (layers[i] != null) {
               ChangeLayer.this.layerMgr.addTag(ChangeLayer.this.widgets[i], layers[i]);
            }
         }

         ChangeLayer.this.editorPane.getSelectedWidgets().setWidgets(ChangeLayer.this.widgets);
         ChangeLayer.this.editorPane.getPxEditor().firePxEvent(new PxLayerEvent(2, layers[0]));
      }
   }
}
