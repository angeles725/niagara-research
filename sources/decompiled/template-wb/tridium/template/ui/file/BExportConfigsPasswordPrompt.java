package com.tridium.template.ui.file;

import com.tridium.ui.BOptionDialog;
import com.tridium.workbench.user.BConfirmPasswordFE;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPassword;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BGridPane;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "applyEncryption",
   type = "boolean",
   defaultValue = "false"
)
public class BExportConfigsPasswordPrompt extends BGridPane {
   public static final Property applyEncryption = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BExportConfigsPasswordPrompt.class);
   static final BPassword DLG_CANCELLED = BPassword.make("");
   private static final Lexicon lex = Lexicon.make("template");
   private BOptionDialog dialog;
   private final BCheckBox encryptBox = new BCheckBox(lex.getText("bulkDeploy.excelExport.encryptDialog.encryptCheckbox"), false);
   private final BConfirmPasswordFE passwordFE = new BConfirmPasswordFE();
   private final BLabel noteLabel = new BLabel(lex.getText("bulkDeploy.excelExport.encryptDialog.noteLabel"));
   private final BLabel cautionLabel = new BLabel(lex.getText("bulkDeploy.excelExport.encryptDialog.cautionLabel"));
   private final BLabel reminderLabel = new BLabel(lex.getText("bulkDeploy.excelExport.encryptDialog.reminderLabel"));

   public boolean getApplyEncryption() {
      return this.getBoolean(applyEncryption);
   }

   public void setApplyEncryption(boolean v) {
      this.setBoolean(applyEncryption, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   private BExportConfigsPasswordPrompt(boolean forceEncryption, boolean defaultToEncrypt) {
      super(1);
      this.setHalign(BHalign.left);
      this.setValign(BValign.top);
      this.encryptBox.setSelected(forceEncryption || defaultToEncrypt);
      this.encryptBox.setEnabled(!forceEncryption);
      this.add(null, this.encryptBox);
      this.noteLabel.setHalign(BHalign.left);
      this.noteLabel.setWordWrapEnabled(true, 300.0);
      this.add(null, this.noteLabel);
      this.add(null, this.passwordFE);
      this.cautionLabel.setHalign(BHalign.left);
      this.cautionLabel.setWordWrapEnabled(true, 300.0);
      this.add(null, this.cautionLabel);
      this.reminderLabel.setHalign(BHalign.left);
      this.reminderLabel.setWordWrapEnabled(true, 300.0);
      this.add(null, this.reminderLabel);
      this.linkTo(null, this.encryptBox, BCheckBox.selected, applyEncryption);
   }

   public static BPassword getPassword(BWidget owner, boolean forceEncryption, boolean defaultToEncrypt) {
      BExportConfigsPasswordPrompt prompt = new BExportConfigsPasswordPrompt(forceEncryption, defaultToEncrypt);
      BOptionDialog dialog = new BOptionDialog(owner, lex.getText("bulkDeploy.excelExport.encryptDialog.title"), prompt, 3, null, null);
      prompt.dialog = dialog;
      dialog.setBoundsCenteredOnOwner();
      prompt.changed(applyEncryption, null);
      dialog.open();
      return dialog.getResult() == 1 ? prompt.getPassword() : DLG_CANCELLED;
   }

   public void changed(Property property, Context context) {
      if (property == applyEncryption) {
         boolean encrypt = this.getApplyEncryption();
         this.passwordFE.setVisible(encrypt);
         this.cautionLabel.setVisible(encrypt);
         this.reminderLabel.setVisible(encrypt);
         if (this.dialog != null) {
            this.dialog.setSize(this.dialog.getWidth(), this.dialog.getHeight() + (encrypt ? 1.0 : -1.0));
         }
      }
   }

   private BPassword getPassword() {
      BPassword password = null;
      if (this.encryptBox.getSelected()) {
         try {
            password = (BPassword)this.passwordFE.saveValue();
            if (password.getValue().isEmpty()) {
               password = BPassword.DEFAULT;
            }
         } catch (Exception var3) {
            password = BPassword.DEFAULT;
         }
      }

      return password;
   }
}
