package com.tridium.workbench.transfer;

import com.tridium.sys.transfer.DeployToComp;
import com.tridium.sys.transfer.TransferListener;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import javax.baja.gx.BImage;
import javax.baja.space.Mark;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableException;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.util.UiLexicon;

public class TransferArtifact implements CommandArtifact {
   static final BImage clockIcon = BImage.make("module://icons/x32/clock.png");
   final UiLexicon lex = UiLexicon.bajaui();
   final String lexPleaseWait = this.lex.getText("pleaseWait");
   BWidget owner;
   int action;
   Mark mark;
   BObject target;
   BComponent params;
   Context context;
   TransferResult result;

   public TransferArtifact(BWidget owner, int action, Mark mark, BObject target, BComponent params, Context cx) {
      this.owner = owner;
      this.action = action;
      this.mark = mark;
      this.target = target;
      this.params = params;
      this.context = cx;
   }

   public TransferResult getResult() {
      return this.result;
   }

   public String[] getInsertNames() {
      return this.getResult().getInsertNames();
   }

   public void redo() throws Exception {
      TransferStrategy strategy = TransferStrategy.make(this.action, this.mark, this.target, this.params, this.context);
      if (strategy == null) {
         throw new LocalizableException("bajaui", "transfer.noStrategy");
      } else {
         new TransferArtifact.Worker().transfer(strategy);
      }
   }

   public void undo() throws Exception {
      this.result.undo();
   }

   class Worker extends Thread implements TransferListener {
      TransferStrategy strategy;
      Exception exception;
      BDialog dialog;
      BLabel status;
      Object lock = new Object();
      boolean done;
      boolean isDeploy = false;
      boolean isPostDeploy = false;
      boolean isDialogOpen = false;

      public void transfer(TransferStrategy strategy) throws Exception {
         this.strategy = strategy;
         strategy.setListener(this);
         this.status = new BLabel(TransferArtifact.clockIcon, TransferArtifact.this.lexPleaseWait);
         this.status.setHalign(BHalign.left);
         BConstrainedPane pane = new BConstrainedPane(this.status);
         pane.setMinHeight(20.0);
         pane.setMinWidth(450.0);
         this.dialog = new BDialog(TransferArtifact.this.owner, TransferArtifact.this.lexPleaseWait, true, new BBorderPane(pane));
         if (strategy instanceof DeployToComp) {
            this.deploy((DeployToComp)strategy);
         } else {
            this.start();
            synchronized (this.lock) {
               this.lock.wait(2000L);
            }

            if (!this.done) {
               this.dialog.setBoundsCenteredOnOwner();
               this.isDialogOpen = true;
               this.dialog.open();
            }

            if (this.exception != null) {
               throw this.exception;
            }
         }
      }

      public void deploy(DeployToComp strategy) throws Exception {
         if (strategy.getSteps(TransferArtifact.this.owner)) {
            strategy.setContext(TransferArtifact.this.context);
            this.isDeploy = true;
            this.start();
            if (!this.done && !this.isPostDeploy) {
               this.dialog.setBoundsCenteredOnOwner();
               this.isDialogOpen = true;
               this.dialog.open();
            }

            if (this.exception != null) {
               throw this.exception;
            }
         }
      }

      public void updateStatus(String msg) {
         if (this.status != null) {
            this.status.setText(msg);
            this.status.relayout();
            if (this.isDeploy) {
               if (msg.equals("postDeploy")) {
                  this.isPostDeploy = true;
               }
            }
         }
      }

      @Override
      public void run() {
         try {
            TransferArtifact.this.result = this.strategy.transfer();
         } catch (Exception var4) {
            this.exception = var4;
         }

         synchronized (this.lock) {
            this.done = true;
            this.lock.notifyAll();
         }

         this.dialog.close();
      }
   }
}
