package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.gx.BImage;
import javax.baja.space.Mark;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.transfer.BTransferWidget;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferEnvelope;

public class NewWidget extends Command {
   BPxEditorPane editorPane;
   BTransferWidget transWidget;
   BWidget widget;

   public NewWidget(BPxEditorPane editorPane, BTransferWidget transWidget, BWidget widget, String name) {
      super(editorPane, name, BImage.make(widget.getIcon()), null, null);
      this.editorPane = editorPane;
      this.transWidget = transWidget;
      this.widget = widget;
   }

   public CommandArtifact doInvoke() throws Exception {
      return this.transWidget
         .insertTransferData(
            new TransferContext(
               null, 16, TransferEnvelope.make(new Mark(new BWidget[]{this.editorPane.getPxEditor().cloneWidget(this.widget)}, new String[]{""}))
            )
         );
   }
}
