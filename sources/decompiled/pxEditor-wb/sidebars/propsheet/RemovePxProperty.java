package com.tridium.px.editor.sidebars.propsheet;

import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxProperty;
import javax.baja.util.Lexicon;

class RemovePxProperty extends Command {
   private BPxPropSheet sheet;

   RemovePxProperty(BPxPropSheet sheet) {
      super(sheet, Lexicon.make("pxEditor"), "commands.remove");
      this.sheet = sheet;
      this.setEnabled(!sheet.readonly);
   }

   public CommandArtifact doInvoke() throws Exception {
      PxProperty[] props = this.sheet.editor.getPxProperties();
      final int n = this.sheet.table.getSelection().getRow();
      final PxProperty group = props[n];
      CommandArtifact af = new CommandArtifact() {
         CommandArtifact removal;

         public void redo() throws Exception {
            this.removal = RemovePxProperty.this.sheet.removeProperty(n);
         }

         public void undo() throws Exception {
            RemovePxProperty.this.sheet.insertProperty(n, group, this.removal);
         }
      };
      af.redo();
      return af;
   }
}
