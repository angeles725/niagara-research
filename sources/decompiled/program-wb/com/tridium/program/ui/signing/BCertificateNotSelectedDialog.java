package com.tridium.program.ui.signing;

import com.tridium.crypto.core.cert.KeyPurpose;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.platcrypto.ui.BSelfSignedDialog;
import com.tridium.platcrypto.ui.BCertsTable.CertGenRequest;
import com.tridium.workbench.fieldeditors.BWbCertificateAliasFE;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BString;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.ui.wizard.BWizardHeader;
import javax.baja.util.Lexicon;
import javax.baja.workbench.BWbPlugin;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "okButtonPressed",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   ), @NiagaraAction(
      name = "createButtonPressed",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   ), @NiagaraAction(
      name = "cancelButtonPressed",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   ), @NiagaraAction(
      name = "setModified",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   )})
public class BCertificateNotSelectedDialog extends BDialog {
   public static final Action okButtonPressed = newAction(0, new BWidgetEvent(), null);
   public static final Action createButtonPressed = newAction(0, new BWidgetEvent(), null);
   public static final Action cancelButtonPressed = newAction(0, new BWidgetEvent(), null);
   public static final Action setModified = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BCertificateNotSelectedDialog.class);
   private String result;
   private BButton okButton;
   private BButton createButton;
   private BButton cancelButton;
   private BWbCertificateAliasFE aliasFe;
   protected static final Lexicon lex = Lexicon.make("program");

   public void okButtonPressed(BWidgetEvent parameter) {
      this.invoke(okButtonPressed, parameter, null);
   }

   public void createButtonPressed(BWidgetEvent parameter) {
      this.invoke(createButtonPressed, parameter, null);
   }

   public void cancelButtonPressed(BWidgetEvent parameter) {
      this.invoke(cancelButtonPressed, parameter, null);
   }

   public void setModified(BWidgetEvent parameter) {
      this.invoke(setModified, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BCertificateNotSelectedDialog(BWidget owner) throws Exception {
      super(owner, lex.getText("program.certNotSelected.title"), true);
      this.setContent(this.buildDialog());
      this.setDefaultButton(this.okButton);
   }

   public static String show(BWidget owner) throws Exception {
      BCertificateNotSelectedDialog dialog = new BCertificateNotSelectedDialog(owner);
      dialog.setBoundsCenteredOnOwner();
      dialog.setResizable(false);
      dialog.open();
      return dialog.getResult();
   }

   private BWidget buildDialog() throws Exception {
      BEdgePane pane = new BEdgePane();
      pane.setTop(this.buildHeader());
      pane.setCenter(this.buildContent());
      pane.setBottom(this.buildButtonPane());
      return pane;
   }

   private BWidget buildHeader() {
      return new BWizardHeader(null, lex.getText("program.certNotSelected.title"), lex.get("program.certNotSelected.description"));
   }

   private BWidget buildContent() {
      this.aliasFe = new BWbCertificateAliasFE();
      this.aliasFe.loadValue(BString.make(BCodeSigningOptions.make().getSigningCert()));
      BGridPane grid = new BGridPane(2);
      grid.setColumnAlign(BHalign.center);
      grid.setHalign(BHalign.center);
      grid.setColumnGap(5.0);
      grid.add(null, new BLabel(lex.getText("program.certNotSelected.alias")));
      grid.add(null, this.aliasFe);
      return new BBorderPane(grid, 7.0, 7.0, 7.0, 7.0);
   }

   private BWidget buildButtonPane() throws Exception {
      this.okButton = new BButton(UiLexicon.bajaui().getText("dialog.ok"));
      this.cancelButton = new BButton(UiLexicon.bajaui().getText("dialog.cancel"));
      this.createButton = new BButton(lex.getText("program.certNotSelected.create"));
      String alias = ((BString)this.aliasFe.saveValue()).getString();
      this.okButton.setEnabled(alias != null && !alias.isEmpty());
      this.linkTo("linkA", this.okButton, BButton.actionPerformed, okButtonPressed);
      this.linkTo("linkB", this.cancelButton, BButton.actionPerformed, cancelButtonPressed);
      this.linkTo("linkC", this.createButton, BButton.actionPerformed, createButtonPressed);
      this.linkTo("linkD", this.aliasFe, BWbPlugin.setModified, setModified);
      BGridPane buttonPane = new BGridPane(3);
      buttonPane.setColumnAlign(BHalign.fill);
      buttonPane.setUniformColumnWidth(true);
      buttonPane.add(null, this.okButton);
      buttonPane.add(null, this.createButton);
      buttonPane.add(null, this.cancelButton);
      return new BBorderPane(buttonPane, 7.0, 7.0, 7.0, 7.0);
   }

   public void doOkButtonPressed(BWidgetEvent event) throws Exception {
      this.result = ((BString)this.aliasFe.saveValue()).getString();
      this.close();
   }

   public void doCreateButtonPressed(BWidgetEvent event) throws Exception {
      CoreCryptoManager ccm = AccessController.doPrivileged((PrivilegedAction<CoreCryptoManager>)(() -> {
         try {
            return CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
         } catch (Exception var1x) {
            return null;
         }
      }));
      this.setContent(new BLabel(lex.getText("program.certNotSelected.generating")));
      BSelfSignedDialog dialog = new BSelfSignedDialog(this, ccm.getKeyStore());
      dialog.setKeyPurpose(KeyPurpose.CODE_SIGNING_CERT);
      dialog.setBoundsCenteredOnOwner();
      dialog.setResizable(false);
      dialog.open();
      CertGenRequest req = dialog.getResult(ccm);
      if (req == null) {
         this.result = null;
         this.close();
      } else {
         new Thread(new BCertificateNotSelectedDialog.CertGenMonitor(ccm, req)).start();
      }
   }

   public void doCancelButtonPressed(BWidgetEvent event) {
      this.close();
   }

   public void doSetModified(BWidgetEvent event) throws Exception {
      String alias = ((BString)this.aliasFe.saveValue()).getString();
      this.okButton.setEnabled(alias != null && !alias.isEmpty());
   }

   public String getResult() {
      return this.result;
   }

   private class CertGenMonitor implements Runnable {
      private final CoreCryptoManager ccm;
      private final CertGenRequest req;

      public CertGenMonitor(CoreCryptoManager ccm, CertGenRequest req) {
         this.ccm = ccm;
         this.req = req;
      }

      @Override
      public void run() {
         BCertificateNotSelectedDialog.this.enterBusy();

         while (true) {
            int status = this.ccm.getCertGenerationStatus(this.req.getRequestId());
            if (status == -1) {
               BCertificateNotSelectedDialog.this.result = null;
               break;
            }

            if (status == 2) {
               BCertificateNotSelectedDialog.this.result = this.req.getAlias();
               break;
            }

            try {
               Thread.sleep(500L);
            } catch (InterruptedException var3) {
            }
         }

         BCertificateNotSelectedDialog.this.exitBusy();
         BCertificateNotSelectedDialog.this.close();
      }
   }
}
