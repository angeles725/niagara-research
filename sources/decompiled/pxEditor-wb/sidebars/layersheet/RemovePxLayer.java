package com.tridium.px.editor.sidebars.layersheet;

import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxLayer;
import javax.baja.util.Lexicon;

class RemovePxLayer extends Command {
   private BPxLayerSheet sheet;

   RemovePxLayer(BPxLayerSheet sheet) {
      super(sheet, Lexicon.make("pxEditor"), "commands.remove");
      this.sheet = sheet;
      this.setEnabled(!sheet.readonly);
   }

   public CommandArtifact doInvoke() throws Exception {
      PxLayer[] layers = this.sheet.editor.getPxLayers();
      final int n = this.sheet.table.getSelection().getRow();
      final PxLayer layer = layers[n];
      CommandArtifact af = new CommandArtifact() {
         CommandArtifact removeTags;

         public void redo() throws Exception {
            this.removeTags = RemovePxLayer.this.sheet.removeLayer(n);
         }

         public void undo() throws Exception {
            RemovePxLayer.this.sheet.insertLayer(n, layer, this.removeTags);
         }
      };
      af.redo();
      return af;
   }
}
