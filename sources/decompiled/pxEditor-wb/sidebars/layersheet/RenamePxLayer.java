package com.tridium.px.editor.sidebars.layersheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.util.ObjectUtil;
import com.tridium.util.ObjectUtil.NameContainer;
import javax.baja.px.editor.event.PxLayerEvent;
import javax.baja.ui.BDialog;
import javax.baja.ui.BTextField;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxLayer;
import javax.baja.util.Lexicon;

class RenamePxLayer extends Command implements NameContainer {
   private static final Lexicon lex = Lexicon.make("pxEditor");
   private static final String title = lex.getText("commands.rename.label");
   private static final String defaultPxLayerName = lex.getText("pxLayerSheet.defaultPxLayerName");
   private static final String alreadyExists = lex.getText("pxLayerSheet.alreadyExists");
   private BPxEditorPane editorPane;
   private BPxLayerSheet sheet;

   RenamePxLayer(BPxEditorPane editorPane, BPxLayerSheet sheet) {
      super(sheet, Lexicon.make("pxEditor"), "commands.rename");
      this.editorPane = editorPane;
      this.sheet = sheet;
      this.setEnabled(!sheet.readonly);
   }

   public boolean contains(String name) {
      PxLayer[] layers = this.sheet.editor.getPxLayers();

      for (int i = 0; i < layers.length; i++) {
         if (layers[i].getName().equals(name)) {
            return true;
         }
      }

      return false;
   }

   public CommandArtifact doInvoke() throws Exception {
      PxLayer[] layers = this.sheet.editor.getPxLayers();
      final int row = this.sheet.table.getSelection().getRow();
      final PxLayer layer = layers[row];
      final String oldName = layer.getName();
      BTextField text = new BTextField(ObjectUtil.generateUniqueName(defaultPxLayerName, this), 20);
      int r = BDialog.open(this.sheet, title, text, 3);
      if (r != 1) {
         return null;
      } else {
         final String newName = text.getText();
         if (oldName.equals(newName)) {
            return null;
         } else if (newName.length() == 0) {
            throw new IllegalArgumentException("Layer name must be defined.");
         } else {
            for (int i = 0; i < layers.length; i++) {
               PxLayer p = layers[i];
               if (p != layer && p.getName().equals(newName)) {
                  BDialog.error(this.sheet, title, alreadyExists);
                  return null;
               }
            }

            CommandArtifact af = new CommandArtifact() {
               public void redo() throws Exception {
                  layer.setName(newName);
                  RenamePxLayer.this.sheet.table.setRowName(row, newName);
                  RenamePxLayer.this.sheet.layerMgr.renameLayerTag(oldName, newName);
                  RenamePxLayer.this.sheet.updateUI(layer, new PxLayerEvent(3, layer));
               }

               public void undo() throws Exception {
                  layer.setName(oldName);
                  RenamePxLayer.this.sheet.table.setRowName(row, oldName);
                  RenamePxLayer.this.sheet.layerMgr.renameLayerTag(newName, oldName);
                  RenamePxLayer.this.sheet.updateUI(layer, new PxLayerEvent(3, layer));
               }
            };
            af.redo();
            return af;
         }
      }
   }
}
