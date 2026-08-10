package javax.baja.ui.transfer;

import com.tridium.ui.NiagaraWbShell;
import com.tridium.ui.ShellManager;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.Mark;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.BWidgetShell;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.util.UiLexicon;

@NiagaraType
public abstract class BTransferWidget extends BWidget implements TransferConst {
   public static final Type TYPE = Sys.loadType(BTransferWidget.class);
   boolean cutEnabled = false;
   boolean copyEnabled = false;
   boolean pasteEnabled = false;
   boolean pasteSpecialEnabled = false;
   boolean duplicateEnabled = false;
   boolean deleteEnabled = false;
   boolean renameEnabled = false;

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isCutEnabled() {
      return this.cutEnabled;
   }

   public void setCutEnabled(boolean cutEnabled) {
      if (this.cutEnabled != cutEnabled) {
         this.cutEnabled = cutEnabled;
         this.updateStates();
      }
   }

   public boolean isCopyEnabled() {
      return this.copyEnabled;
   }

   public void setCopyEnabled(boolean copyEnabled) {
      if (this.copyEnabled != copyEnabled) {
         this.copyEnabled = copyEnabled;
         this.updateStates();
      }
   }

   public boolean isPasteEnabled() {
      return this.pasteEnabled;
   }

   public void setPasteEnabled(boolean pasteEnabled) {
      if (this.pasteEnabled != pasteEnabled) {
         this.pasteEnabled = pasteEnabled;
         this.updateStates();
      }
   }

   public boolean isPasteSpecialEnabled() {
      return this.pasteSpecialEnabled;
   }

   public void setPasteSpecialEnabled(boolean pasteSpecialEnabled) {
      if (this.pasteSpecialEnabled != pasteSpecialEnabled) {
         this.pasteSpecialEnabled = pasteSpecialEnabled;
         this.updateStates();
      }
   }

   public boolean isDuplicateEnabled() {
      return this.duplicateEnabled;
   }

   public void setDuplicateEnabled(boolean duplicateEnabled) {
      if (this.duplicateEnabled != duplicateEnabled) {
         this.duplicateEnabled = duplicateEnabled;
         this.updateStates();
      }
   }

   public boolean isDeleteEnabled() {
      return this.deleteEnabled;
   }

   public void setDeleteEnabled(boolean deleteEnabled) {
      if (this.deleteEnabled != deleteEnabled) {
         this.deleteEnabled = deleteEnabled;
         this.updateStates();
      }
   }

   public boolean isRenameEnabled() {
      return this.renameEnabled;
   }

   public void setRenameEnabled(boolean renameEnabled) {
      if (this.renameEnabled != renameEnabled) {
         this.renameEnabled = renameEnabled;
         this.updateStates();
      }
   }

   private void updateStates() {
      BWidgetShell shell = this.getShell();
      if (shell instanceof NiagaraWbShell) {
         ((NiagaraWbShell)shell).updateTransferWidgetStates();
      }
   }

   public void startDrag(BMouseEvent event, TransferContext context, DragRenderer dragRenderer) {
      ShellManager manager = (ShellManager)this.widgetSupport(null);
      manager.startDragOperation(this, event, context, dragRenderer);
   }

   public void startDrag(BMouseEvent event, TransferEnvelope envelope, DragRenderer dragRenderer) {
      this.startDrag(event, this.makeTransferContext(envelope), dragRenderer);
   }

   public void dragEnter(TransferContext cx) {
   }

   public int dragOver(TransferContext cx) {
      return 0;
   }

   public void dragExit(TransferContext cx) {
   }

   public CommandArtifact drop(TransferContext cx) throws Exception {
      return null;
   }

   public CommandArtifact doCopy() throws Exception {
      this.setCurrentMark(null);
      if (!this.copyEnabled) {
         return null;
      } else {
         TransferEnvelope envelope = this.getTransferData();
         if (envelope == null) {
            return null;
         } else {
            Clipboard.getDefault().setContents(envelope);
            if (envelope.supports(TransferFormat.mark)) {
               Mark mark = (Mark)envelope.getData(TransferFormat.mark);
               this.setCurrentMark(mark);
            }

            return null;
         }
      }
   }

   public CommandArtifact doCut() throws Exception {
      this.setCurrentMark(null);
      if (!this.cutEnabled) {
         return null;
      } else {
         TransferEnvelope envelope = this.getTransferData();
         if (envelope == null) {
            return null;
         } else {
            Clipboard.getDefault().setContents(envelope);
            if (envelope.supports(TransferFormat.mark)) {
               Mark mark = (Mark)envelope.getData(TransferFormat.mark);
               mark.setPendingMove(true);
               this.setCurrentMark(mark);
            }

            TransferContext cx = this.makeTransferContext(null, 32, envelope);
            return this.removeTransferData(cx);
         }
      }
   }

   public CommandArtifact doPaste() throws Exception {
      if (!this.pasteEnabled) {
         return null;
      } else {
         try {
            TransferEnvelope envelope = Clipboard.getDefault().getContents();
            if (envelope == null) {
               BDialog.error(this, UiLexicon.bajaui().getText("command.paste.emptyClipboard"));
               return null;
            } else {
               int action = 16;
               if (envelope.supports(TransferFormat.mark)) {
                  Mark mark = (Mark)envelope.getData(TransferFormat.mark);
                  if (mark.isPendingMove()) {
                     action = 32;
                     Clipboard.getDefault().setContents(null);
                     this.setCurrentMark(null);
                     mark.setPendingMove(false);
                  }
               }

               TransferContext cx = this.makeTransferContext(null, action, envelope);
               return this.insertTransferData(cx);
            }
         } catch (UnsupportedFormatException var4) {
            BDialog.error(this, UiLexicon.bajaui().getText("command.paste.unsupportedFormat"));
            return null;
         }
      }
   }

   public CommandArtifact doPasteSpecial() throws Exception {
      if (!this.pasteSpecialEnabled) {
         return null;
      } else {
         throw new UnsupportedOperationException();
      }
   }

   public CommandArtifact doDuplicate() throws Exception {
      if (!this.duplicateEnabled) {
         return null;
      } else {
         TransferEnvelope envelope = this.getTransferData();
         if (envelope == null) {
            return null;
         } else {
            TransferContext cx = this.makeTransferContext(null, 16, envelope);
            return this.insertTransferData(cx);
         }
      }
   }

   public CommandArtifact doDelete() throws Exception {
      if (!this.deleteEnabled) {
         return null;
      } else {
         throw new UnsupportedOperationException();
      }
   }

   public CommandArtifact doRename() throws Exception {
      if (!this.renameEnabled) {
         return null;
      } else {
         throw new UnsupportedOperationException();
      }
   }

   public abstract TransferEnvelope getTransferData() throws Exception;

   public abstract CommandArtifact insertTransferData(TransferContext var1) throws Exception;

   public abstract CommandArtifact removeTransferData(TransferContext var1) throws Exception;

   public final TransferContext makeTransferContext(TransferEnvelope envelope) {
      return this.makeTransferContext(null, 16, envelope);
   }

   public TransferContext makeTransferContext(Context sourceContext, int action, TransferEnvelope envelope) {
      return new TransferContext(sourceContext, action, envelope);
   }

   void setCurrentMark(Mark mark) {
      Mark.setCurrent(mark);
      this.getShell().repaint();
   }
}
