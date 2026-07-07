package com.tridium.template.ui;

import javax.baja.gx.BImage;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLabel;
import javax.baja.ui.BProgressDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.BProgressDialog.Worker;

@NiagaraType
public class BTemplateDeployProgressDialog extends BProgressDialog {
   public static final Type TYPE = Sys.loadType(BTemplateDeployProgressDialog.class);
   private static double maxMessageWidth = 0.0;
   private static final BImage NTPL_IMAGE = BImage.make("module://icons/x16/files/ntpl.png");
   private static double MAX_PROGRESS_WIDTH = 600.0;

   public Type getType() {
      return TYPE;
   }

   private BTemplateDeployProgressDialog() {
   }

   private BTemplateDeployProgressDialog(BWidget owner, String title, Worker worker) {
      super(owner, title, worker, NTPL_IMAGE, 0.0, 100.0);
   }

   public static void open(BWidget owner, String title, TemplateDeployWorker worker) {
      String maxMessage = worker.getMaxMessage();
      BLabel deployMessageLabel = new BLabel(maxMessage);
      deployMessageLabel.computePreferredSize();
      maxMessageWidth = deployMessageLabel.getPreferredWidth() * 1.5;
      if (maxMessageWidth > MAX_PROGRESS_WIDTH) {
         maxMessageWidth = MAX_PROGRESS_WIDTH;
      }

      BTemplateDeployProgressDialog dialog = new BTemplateDeployProgressDialog(owner, title, worker);
      dialog.setBoundsCenteredOnOwner();
      worker.setDialog(dialog);
      worker.start();
      dialog.open();
   }

   public void computePreferredSize() {
      super.computePreferredSize();
      this.getMessageLabel().setPreferredSize(maxMessageWidth, this.getMessageLabel().getPreferredHeight());
      double pw = this.getPreferredWidth();
      if (maxMessageWidth > pw) {
         this.setPreferredSize(maxMessageWidth, this.getPreferredHeight());
      }
   }
}
