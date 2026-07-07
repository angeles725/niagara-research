package com.tridium.px.editor.sidebars.layersheet;

import com.tridium.util.ObjectUtil;
import com.tridium.util.ObjectUtil.NameContainer;
import javax.baja.naming.SlotPath;
import javax.baja.ui.BDialog;
import javax.baja.ui.BTextField;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.px.PxLayer;
import javax.baja.util.Lexicon;

class AddPxLayer extends Command implements NameContainer {
   private static final Lexicon lex = Lexicon.make("pxEditor");
   private static String lastName = "newLayer";
   private static final String alreadyExists = lex.getText("pxLayerSheet.alreadyExists");
   private BPxLayerSheet sheet;

   AddPxLayer(BPxLayerSheet sheet) {
      super(sheet, Lexicon.make("pxEditor"), "commands.add");
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
      String newName = ObjectUtil.generateUniqueName(lastName, this);
      BTextField nameEditor = new BTextField(newName, 25);
      BGridPane nameInner = new BGridPane();
      nameInner.setHalign(BHalign.left);
      nameInner.add(null, nameEditor);
      BBorderPane namePane = new BBorderPane(nameInner, "Name");
      BGridPane pane = new BGridPane(1);
      pane.setColumnAlign(BHalign.fill);
      pane.add(null, namePane);
      String title = this.getLabel();
      int buttons = 3;
      if (BDialog.open(this.sheet, title, pane, buttons, null) == 2) {
         return null;
      } else {
         lastName = nameEditor.getText();
         newName = SlotPath.escape(nameEditor.getText());
         final PxLayer[] layers = this.sheet.editor.getPxLayers();

         for (int i = 0; i < layers.length; i++) {
            PxLayer p = layers[i];
            if (p.getName().equals(newName)) {
               BDialog.error(this.sheet, title, alreadyExists);
               return null;
            }
         }

         final PxLayer newLayer = new PxLayer(newName);
         CommandArtifact af = new CommandArtifact() {
            public void redo() throws Exception {
               AddPxLayer.this.sheet.insertLayer(layers.length, newLayer, null);
            }

            public void undo() throws Exception {
               AddPxLayer.this.sheet.removeLayer(layers.length);
            }
         };
         af.redo();
         return af;
      }
   }
}
