package com.tridium.px.editor.make;

import com.tridium.ui.theme.Theme;
import javax.baja.gx.BInsets;
import javax.baja.gx.Graphics;
import javax.baja.gx.IRectGeom;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BPane;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
public class BCellPane extends BPane {
   public static final Type TYPE = Sys.loadType(BCellPane.class);
   private static BInsets insets = BInsets.make(2.0);
   private BConstrainedPane cons = new BConstrainedPane();
   private BWbCellEditor ce;

   public Type getType() {
      return TYPE;
   }

   public BCellPane(BWbCellEditor ce, double width, double height) {
      this.add(null, this.cons);
      this.cons.setContent(ce);
      this.cons.setMinWidth(width - insets.left - insets.right);
      this.cons.setMaxWidth(width - insets.left - insets.right);
      this.cons.setMinHeight(height - insets.top - insets.bottom);
      this.cons.setMaxHeight(height - insets.top - insets.bottom);
   }

   public void computePreferredSize() {
      this.cons.computePreferredSize();
      this.setPreferredSize(this.cons.getPreferredWidth() + insets.left + insets.right, this.cons.getPreferredHeight() + insets.top + insets.bottom);
   }

   public void doLayout(BWidget[] children) {
      this.cons.setBounds(insets.left, insets.top, this.getWidth() - insets.left - insets.right, this.getHeight() - insets.top - insets.bottom);
   }

   public void paint(Graphics g) {
      IRectGeom clip = g.getClipBounds();
      g.setBrush(this.isEnabled() ? Theme.widget().getWindowBackground() : Theme.widget().getControlBackground());
      g.fill(clip);
      Theme.textField().paintBorder(g, this, this.getWidth(), this.getHeight());
      super.paint(g);
   }

   public BWbCellEditor getCellEditor() {
      return this.ce;
   }
}
