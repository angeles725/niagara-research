package com.tridium.px.editor.sidebars.propsheet;

import com.tridium.util.ObjectUtil;
import com.tridium.util.ObjectUtil.NameContainer;
import com.tridium.workbench.fieldeditors.BTypeSpecFE;
import javax.baja.naming.SlotPath;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.ui.BDialog;
import javax.baja.ui.BTextField;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.px.PxProperty;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

class AddPxProperty extends Command implements NameContainer {
   private static BTypeSpec lastType = BTypeSpec.make("gx", "Brush");
   private static String lastName = "newProp";
   private static final Lexicon lex = Lexicon.make("pxEditor");
   private static final String alreadyExists = lex.getText("pxPropSheet.alreadyExists");
   private BPxPropSheet sheet;

   AddPxProperty(BPxPropSheet sheet) {
      super(sheet, Lexicon.make("pxEditor"), "commands.add");
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
      String newName = ObjectUtil.generateUniqueName(lastName, this);
      BTextField nameEditor = new BTextField(newName, 25);
      BGridPane nameInner = new BGridPane();
      nameInner.setHalign(BHalign.left);
      nameInner.add(null, nameEditor);
      BBorderPane namePane = new BBorderPane(nameInner, "Name");
      BTypeSpecFE typeEditor = new BTypeSpecFE();
      BFacets facets = BFacets.make("allowNull", BBoolean.FALSE, "targetType", BString.make("baja:Simple"), "showAbstract", BBoolean.FALSE);
      typeEditor.loadValue(lastType, facets);
      BGridPane typeInner = new BGridPane();
      typeInner.setHalign(BHalign.left);
      typeInner.add(null, typeEditor);
      BBorderPane typePane = new BBorderPane(typeInner, "Type");
      BGridPane pane = new BGridPane(1);
      pane.setColumnAlign(BHalign.fill);
      pane.add(null, namePane);
      pane.add(null, typePane);
      String title = this.getLabel();
      int buttons = 3;
      if (BDialog.open(this.sheet, title, pane, buttons, null) == 2) {
         return null;
      } else {
         lastName = nameEditor.getText();
         newName = SlotPath.escape(nameEditor.getText());
         BTypeSpec typeSpec = lastType = (BTypeSpec)typeEditor.saveValue();
         BValue value = (BValue)typeSpec.getInstance();
         final PxProperty[] props = this.sheet.editor.getPxProperties();

         for (int i = 0; i < props.length; i++) {
            PxProperty p = props[i];
            if (p.getName().equals(newName)) {
               BDialog.error(this.sheet, title, alreadyExists);
               return null;
            }
         }

         final PxProperty newProp = new PxProperty(newName, typeSpec, value, new SlotPath[0]);
         CommandArtifact af = new CommandArtifact() {
            public void redo() throws Exception {
               AddPxProperty.this.sheet.insertProperty(props.length, newProp, null);
            }

            public void undo() throws Exception {
               AddPxProperty.this.sheet.removeProperty(props.length);
            }
         };
         af.redo();
         return af;
      }
   }
}
