package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BConverterCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxPropertyCE;
import javax.baja.space.Mark;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.transfer.Clipboard;
import javax.baja.ui.transfer.TransferEnvelope;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

public class CopyCell extends Command {
   private BWbCellEditor ce;

   public CopyCell(BPxEditorPane editorPane, BWbCellEditor ce) {
      super(editorPane, UiLexicon.bajaui(), "commands.copy");
      this.ce = ce;
      boolean isBound = ce instanceof BConverterCE;
      boolean isGrouped = ce instanceof BPxPropertyCE;
      this.setEnabled(!isBound && !isGrouped);
   }

   public CommandArtifact doInvoke() throws Exception {
      Mark.setCurrent(null);
      Mark mark = new Mark(this.ce.saveValue());
      Clipboard.getDefault().setContents(TransferEnvelope.make(mark));
      Mark.setCurrent(mark);
      return null;
   }
}
