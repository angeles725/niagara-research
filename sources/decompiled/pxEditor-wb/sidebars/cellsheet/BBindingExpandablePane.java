package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.commands.DeleteBinding;
import com.tridium.ui.theme.Theme;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BMouseEvent;

@NiagaraType
public class BBindingExpandablePane extends BCellSheetExpandablePane {
   public static final Type TYPE = Sys.loadType(BBindingExpandablePane.class);
   private BPxEditorPane editorPane;
   private CellSheetContext context;
   private BWidget widget;
   private BBinding binding;
   private static final double GAP = 2.0;
   private static final double SIZE = 17.0;
   private RectGeom closeButton = new RectGeom();
   private boolean overCloseButton;
   private boolean deletable = true;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBindingExpandablePane(BPxEditorPane editorPane, CellSheetContext context, BWidget widget, BBinding binding) {
      this.editorPane = editorPane;
      this.context = context;
      this.widget = widget;
      this.binding = binding;
   }

   @Override
   public void doLayout(BWidget[] kids) {
      double w = this.getWidth();
      double h = this.getHeight();
      BWidget summary = this.getSummary();
      summary.computePreferredSize();
      double sw = summary.getPreferredWidth();
      double sh = summary.getPreferredHeight();
      double seh = Math.max(sh, 17.0);
      double gap = summary.isNull() ? 0.0 : 2.0;
      if (this.expanderVisible) {
         if (this.getExpanderHalign() == BHalign.right) {
            summary.setBounds(0.0, (seh - sh) / 2.0, w - gap - 17.0 - 17.0, sh);
            this.button.set(sw + gap, (seh - 17.0) / 2.0, 17.0, Math.max(sh, 19.0));
         } else {
            this.button.set(0.0, (seh - 17.0) / 2.0, 17.0, Math.max(sh, 19.0));
            summary.setBounds(17.0 + gap, (seh - sh) / 2.0, w - gap - 17.0 - 17.0, sh);
         }
      } else {
         summary.setBounds(0.0, (seh - sh) / 2.0, sw, sh);
         this.button.set(0.0, 0.0, 0.0, 0.0);
      }

      BWidget expansion = this.getExpansion();
      if (this.isExpanded) {
         expansion.setBounds(0.0, seh, w, h - seh);
      } else {
         expansion.setBounds(0.0, 0.0, 0.0, 0.0);
      }

      this.closeButton.set(w - 2.0 - 17.0, (seh - 17.0) / 2.0, 17.0, Math.max(sh, 19.0));
   }

   @Override
   public void paint(Graphics g) {
      super.paint(g);
      g.setBrush(this.deletable ? BColor.black.toBrush() : Theme.widget().getControlShadow());
      int x = (int)this.closeButton.x;
      int y = (int)this.closeButton.y;
      int w = (int)this.closeButton.width;
      int h = (int)this.closeButton.height - 2;
      int ax = x + 5;
      int ay = y + 5;
      int bx = ax + 6;
      int by = ay + 6;
      g.strokeLine(ax, ay, bx, by);
      g.strokeLine(ax + 1, ay, bx + 1, by);
      g.strokeLine(ax, by, bx, ay);
      g.strokeLine(ax + 1, by, bx + 1, ay);
      if (this.deletable && this.overCloseButton) {
         g.setBrush(Theme.widget().getControlHighlight());
         g.strokeLine(x + 1, y + 1, x + w - 2, y + 1);
         g.strokeLine(x + 1, y + 1, x + 1, y + h - 2);
         g.setBrush(Theme.widget().getControlShadow());
         g.strokeLine(x + w - 1, y + 1, x + w - 1, y + h - 2);
         g.strokeLine(x + 1, y + h - 2, x + w - 1, y + h - 2);
      }
   }

   @Override
   public void mouseReleased(BMouseEvent event) {
      super.mouseReleased(event);
      if (this.deletable && this.closeButton.contains(event.getX(), event.getY())) {
         new DeleteBinding(this.editorPane, this.context, this.widget, this.binding).invoke();
      }
   }

   @Override
   public void mouseEntered(BMouseEvent event) {
      super.mouseEntered(event);
      this.checkOverCloseButton(event);
   }

   @Override
   public void mouseExited(BMouseEvent event) {
      super.mouseExited(event);
      this.overCloseButton = false;
      this.repaint();
   }

   @Override
   public void mouseMoved(BMouseEvent event) {
      super.mouseMoved(event);
      this.checkOverCloseButton(event);
   }

   private void checkOverCloseButton(BMouseEvent event) {
      double mx = event.getX();
      double my = event.getY();
      boolean nowOverButton = this.closeButton.contains(mx, my);
      if (nowOverButton != this.overCloseButton) {
         this.overCloseButton = nowOverButton;
         this.repaint();
      }
   }

   void setDeletable(boolean flag) {
      this.deletable = flag;
   }
}
