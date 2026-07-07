package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.ui.theme.Theme;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.transfer.TransferContext;

@NiagaraType
public class BPxCellSheetLabel extends BDraggableLabel {
   public static final Type TYPE = Sys.loadType(BPxCellSheetLabel.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPxCellSheetLabel(BPxEditorPane editorPane, BPxCellSheet sheet) {
      super(editorPane, sheet);
   }

   public void computePreferredSize() {
      this.setPreferredSize(FONT.width(this.getText()), FONT.getHeight());
   }

   public void paint(Graphics g) {
      if (this.dragging) {
         double w = this.getWidth() - 1.0;
         double h = this.getHeight() - 1.0;
         g.setBrush(Theme.widget().getControlHighlight());
         g.strokeLine(0.0, 0.0, 0.0, h);
         g.strokeLine(0.0, 0.0, w, 0.0);
         g.setBrush(Theme.widget().getControlShadow());
         g.strokeLine(0.0, h, w, h);
         g.strokeLine(w, 0.0, w, h);
      }

      g.setBrush(BColor.black);
      g.setFont(FONT);
      g.drawString(this.getText(), 4.0, FONT.getHeight() + 1.0);
   }

   public int dragOver(TransferContext cx) {
      return this.sheet.getCellContext().newBindingDragOver(this, cx);
   }

   public CommandArtifact drop(TransferContext cx) throws Exception {
      this.dragging = false;
      this.repaint();
      return this.sheet.getCellContext().newBindingDrop(cx);
   }
}
