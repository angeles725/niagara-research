package com.tridium.template.ui.file;

import com.tridium.ui.BOptionDialog;
import com.tridium.workbench.fieldeditors.BPasswordFE;
import java.io.File;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPassword;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;

@NiagaraType
public class BBulkDeployPasswordPrompt extends BGridPane {
   public static final Type TYPE = Sys.loadType(BBulkDeployPasswordPrompt.class);
   public static final BPassword DLG_CANCELLED = BPassword.make("");
   private static final Lexicon lex = Lexicon.make("template");
   private final BPasswordFE passwordFE = new BPasswordFE();

   public Type getType() {
      return TYPE;
   }

   private BBulkDeployPasswordPrompt(File importFile) {
      super(1);
      this.setHalign(BHalign.left);
      this.setValign(BValign.top);
      this.add(null, new BLabel(lex.getText("bulkDeploy.excelImport.decryptDialog.message", new Object[]{importFile.getName()})));
      BGridPane passwordGrid = new BGridPane(2);
      passwordGrid.add(null, new BLabel(lex.getText("bulkDeploy.excelImport.decryptDialog.password.title")));
      passwordGrid.add(null, this.passwordFE);
      this.add(null, passwordGrid);
   }

   public static BPassword getPassword(BWidget owner, File importFile) {
      BBulkDeployPasswordPrompt prompt = new BBulkDeployPasswordPrompt(importFile);
      BOptionDialog dialog = new BOptionDialog(owner, lex.getText("bulkDeploy.excelImport.decryptDialog.title"), prompt, 3, null, null);
      dialog.setBoundsCenteredOnOwner();
      dialog.open();
      return dialog.getResult() == 1 ? prompt.getPassword() : DLG_CANCELLED;
   }

   private BPassword getPassword() {
      BPassword password = null;

      try {
         password = (BPassword)this.passwordFE.saveValue();
         if (password.getValue().isEmpty()) {
            password = BPassword.DEFAULT;
         }
      } catch (Exception var3) {
         password = BPassword.DEFAULT;
      }

      return password;
   }
}
