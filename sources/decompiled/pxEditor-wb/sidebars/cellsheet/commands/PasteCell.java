package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.CellTableContext;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BConverterCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxPropertyCE;
import javax.baja.space.Mark;
import javax.baja.sys.BObject;
import javax.baja.ui.BDialog;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.transfer.Clipboard;
import javax.baja.ui.transfer.TransferConst;
import javax.baja.ui.transfer.TransferEnvelope;
import javax.baja.ui.transfer.TransferFormat;
import javax.baja.ui.transfer.UnsupportedFormatException;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

public class PasteCell extends Command implements TransferConst {
   private BPxEditorPane editorPane;
   private BWbCellEditor ce;
   private CellTableContext tableContext;

   public PasteCell(BPxEditorPane editorPane, BWbCellEditor ce, CellTableContext tableContext) {
      super(editorPane, UiLexicon.bajaui(), "commands.paste");
      this.editorPane = editorPane;
      this.ce = ce;
      this.tableContext = tableContext;
      boolean isBound = ce instanceof BConverterCE;
      boolean isGrouped = ce instanceof BPxPropertyCE;
      if (!isBound && !isGrouped) {
         try {
            TransferEnvelope envelope = Clipboard.getDefault().getContents();
            this.setEnabled(envelope != null);
         } catch (Exception var7) {
            var7.printStackTrace();
            this.setEnabled(false);
         }
      } else {
         this.setEnabled(false);
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      TransferEnvelope envelope = null;

      try {
         envelope = Clipboard.getDefault().getContents();
      } catch (Exception var3) {
         var3.printStackTrace();
         return null;
      }

      if (envelope == null) {
         BDialog.error(this.editorPane, UiLexicon.bajaui().getText("command.paste.emptyClipboard"));
         return null;
      } else {
         if (envelope.supports(TransferFormat.mark)) {
            Mark mark = (Mark)envelope.getData(TransferFormat.mark);
            if (mark.isPendingMove()) {
               throw new IllegalStateException();
            }
         }

         BObject[] objects = ((Mark)envelope.getData(TransferFormat.mark)).getValues();
         if (objects.length == 1 && this.ce.getCurrentValue().getClass().isInstance(objects[0])) {
            this.ce.loadValue(objects[0], this.ce.getCurrentContext());
            this.ce.relayout();
            this.tableContext.doPaste();
            return null;
         } else {
            throw new UnsupportedFormatException(UiLexicon.bajaui().getText("command.paste.unsupportedFormat"));
         }
      }
   }
}
