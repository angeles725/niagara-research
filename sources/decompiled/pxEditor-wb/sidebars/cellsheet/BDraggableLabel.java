package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.ui.theme.Theme;
import javax.baja.gx.BFont;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.UndoManager;
import javax.baja.ui.transfer.BTransferWidget;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferEnvelope;

@NiagaraType
public abstract class BDraggableLabel extends BTransferWidget {
   public static final Type TYPE = Sys.loadType(BDraggableLabel.class);
   protected static final BFont FONT = Theme.widget().getTextFont();
   protected BPxCellSheet sheet;
   protected boolean dragging = false;
   private BPxEditorPane editorPane;
   private String text = "";

   public Type getType() {
      return TYPE;
   }

   public BDraggableLabel(BPxEditorPane editorPane, BPxCellSheet sheet) {
      this.editorPane = editorPane;
      this.sheet = sheet;
      this.setCutEnabled(false);
      this.setCopyEnabled(false);
      this.setPasteEnabled(false);
      this.setDuplicateEnabled(false);
      this.setDeleteEnabled(false);
      this.setRenameEnabled(false);
   }

   public TransferEnvelope getTransferData() throws Exception {
      throw new IllegalStateException();
   }

   public CommandArtifact insertTransferData(TransferContext cx) throws Exception {
      throw new IllegalStateException();
   }

   public CommandArtifact removeTransferData(TransferContext cx) throws Exception {
      throw new IllegalStateException();
   }

   public void dragEnter(TransferContext cx) {
   }

   public void dragExit(TransferContext cx) {
      this.dragging = false;
      this.repaint();
   }

   public UndoManager getUndoManager() {
      return this.editorPane.getUndoManager();
   }

   void setText(String s) {
      this.text = s;
   }

   String getText() {
      return this.text;
   }

   void setDragging(boolean b) {
      this.dragging = b;
   }
}
