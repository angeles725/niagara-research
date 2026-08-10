package com.tridium.program.ui.signing;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.crypto.core.cert.NX509Certificate;
import com.tridium.crypto.core.io.ICoreTrustStore;
import com.tridium.platcrypto.fox.ChannelCryptoManager;
import com.tridium.platcrypto.ui.BCertViewDialog;
import com.tridium.ui.theme.custom.nss.StyleUtils;
import javax.baja.gx.BImage;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BButton;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.wizard.BWizardHeader;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraAction(
   name = "cancelButtonPressed",
   parameterType = "BWidgetEvent",
   defaultValue = "new BWidgetEvent()"
)
public class BCertificateNotTrustedDialog extends BCertViewDialog {
   public static final Action cancelButtonPressed = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BCertificateNotTrustedDialog.class);
   protected static Lexicon lex = Lexicon.make("program");
   protected static BImage logo = BImage.make("module://icons/x32/warning.png");
   protected BButton cancelButton;
   protected boolean closeResult = false;

   public void cancelButtonPressed(BWidgetEvent parameter) {
      this.invoke(cancelButtonPressed, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   protected BCertificateNotTrustedDialog(BWidget parent, NX509Certificate cert) throws Exception {
      super(parent, lex.getText("program.certNotTrusted"));
      this.cert = cert;
      this.setContent(this.buildDialog());
      this.setDefaultButton(this.okButton);
   }

   public static boolean showWithResult(BWidget owner, NX509Certificate cert) throws Exception {
      BCertificateNotTrustedDialog dialog = new BCertificateNotTrustedDialog(owner, cert);
      dialog.setBoundsCenteredOnOwner();
      dialog.setResizable(false);
      dialog.open();
      return dialog.getResult();
   }

   protected BWidget buildHeader() {
      return new BWizardHeader(logo, this.cert.getSubject(), lex.getText("program.certNotTrusted", new Object[]{this.cert.getSubject()}));
   }

   protected BWidget buildContent() throws Exception {
      BEdgePane edgePane = new BEdgePane();
      edgePane.setTop(buildInfo());
      edgePane.setCenter(super.buildContent());
      return edgePane;
   }

   private static BWidget buildInfo() {
      BLabel label = new BLabel(lex.getText("cert.prompt.notTrusted") + '\n', BHalign.left);
      StyleUtils.addStyleClass(label, "strong");
      return new BBorderPane(label, 10.0, 10.0, 5.0, 10.0);
   }

   protected BWidget buildButtons() {
      BGridPane buttonPane = new BGridPane(2);
      buttonPane.setColumnAlign(BHalign.fill);
      buttonPane.setUniformColumnWidth(true);
      buttonPane.add(null, this.okButton = new BButton(lex.getText("cert.prompt.dialog.accept")));
      this.linkTo("linkA", this.okButton, BAbstractButton.actionPerformed, okButtonPressed);
      this.okButton.setEnabled(true);
      buttonPane.add(null, this.cancelButton = new BButton(lex.getText("cert.prompt.dialog.reject")));
      this.linkTo("linkB", this.cancelButton, BAbstractButton.actionPerformed, cancelButtonPressed);
      this.cancelButton.setEnabled(true);
      return new BBorderPane(buttonPane, 7.0, 0.0, 0.0, 0.0);
   }

   public void doOkButtonPressed(BWidgetEvent event) {
      this.closeResult = true;
      this.close();
   }

   public void doCancelButtonPressed(BWidgetEvent event) {
      this.closeResult = false;
      this.close();
   }

   public boolean getResult() {
      return this.closeResult;
   }

   public static boolean installUntrustedCertificate(NX509Certificate cert, BWidget parent, BComponent componentInSession) throws Exception {
      if (showWithResult(parent, cert)) {
         ChannelCryptoManager ccm = new ChannelCryptoManager(componentInSession);
         ICoreTrustStore trustStore = ccm.getUserTrustStore();
         CertUtils.addUniqueCertificate(cert.getCertificate(), trustStore);
         trustStore.save();
         return true;
      } else {
         return false;
      }
   }
}
