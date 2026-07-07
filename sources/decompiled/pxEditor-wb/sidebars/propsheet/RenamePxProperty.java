package com.tridium.px.editor.sidebars.propsheet;

import com.tridium.util.ObjectUtil;
import com.tridium.util.ObjectUtil.NameContainer;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.ui.BDialog;
import javax.baja.ui.BTextField;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxProperty;
import javax.baja.util.Lexicon;

class RenamePxProperty extends Command implements NameContainer {
   private static final Lexicon lex = Lexicon.make("pxEditor");
   private static final String title = lex.getText("commands.rename.label");
   private static final String defaultPxPropertyName = lex.getText("pxPropSheet.defaultPxPropertyName");
   private static final String alreadyExists = lex.getText("pxPropSheet.alreadyExists");
   private BPxPropSheet sheet;

   RenamePxProperty(BPxPropSheet sheet) {
      super(sheet, Lexicon.make("pxEditor"), "commands.rename");
      this.sheet = sheet;
      this.setEnabled(!sheet.readonly);
   }

   public boolean contains(String name) {
      PxProperty[] props = this.sheet.editor.getPxProperties();

      for (int i = 0; i < props.length; i++) {
         if (props[i].getName().equals(name)) {
            return true;
         }
      }

      return false;
   }

   public CommandArtifact doInvoke() throws Exception {
      PxProperty[] props = this.sheet.editor.getPxProperties();
      final int row = this.sheet.table.getSelection().getRow();
      final PxProperty prop = props[row];
      final String oldName = prop.getName();
      BTextField text = new BTextField(ObjectUtil.generateUniqueName(defaultPxPropertyName, this), 20);
      int r = BDialog.open(this.sheet, title, text, 3);
      if (r != 1) {
         return null;
      } else {
         final String newName = text.getText();
         if (oldName.equals(newName)) {
            return null;
         } else {
            for (int i = 0; i < props.length; i++) {
               PxProperty p = props[i];
               if (p != prop && p.getName().equals(newName)) {
                  BDialog.error(this.sheet, title, alreadyExists);
                  return null;
               }
            }

            CommandArtifact af = new CommandArtifact() {
               public void redo() throws Exception {
                  prop.setName(newName);
                  RenamePxProperty.this.sheet.table.setRowName(row, newName);
                  RenamePxProperty.this.sheet.updateUI(prop, new PxPropertyEvent(3, prop));
               }

               public void undo() throws Exception {
                  prop.setName(oldName);
                  RenamePxProperty.this.sheet.table.setRowName(row, oldName);
                  RenamePxProperty.this.sheet.updateUI(prop, new PxPropertyEvent(3, prop));
               }
            };
            af.redo();
            return af;
         }
      }
   }
}
