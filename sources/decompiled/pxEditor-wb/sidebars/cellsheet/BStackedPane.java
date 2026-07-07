package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.ui.theme.Theme;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BPane;

@NiagaraType
public class BStackedPane extends BPane {
   public static final Type TYPE = Sys.loadType(BStackedPane.class);
   private static final int SEP_HEIGHT = 6;

   public Type getType() {
      return TYPE;
   }

   public void computePreferredSize() {
      BWidget[] children = this.getChildWidgets();
      double pw = 0.0;
      double ph = 0.0;

      for (int i = 0; i < children.length; i++) {
         if (children[i] instanceof BSeparator) {
            ph += 6.0;
         } else {
            children[i].computePreferredSize();
            pw = Math.max(pw, children[i].getPreferredWidth());
            ph += children[i].getPreferredHeight();
         }
      }

      this.setPreferredSize(pw, ph + 1.0);
   }

   public void doLayout(BWidget[] children) {
      double w = this.getWidth();
      double h = 1.0;

      for (int i = 0; i < children.length; i++) {
         if (children[i] instanceof BSeparator) {
            children[i].setBounds(0.0, h, w, 6.0);
            h += 6.0;
         } else {
            children[i].computePreferredSize();
            double ph = children[i].getPreferredHeight();
            children[i].setBounds(0.0, h, w, ph);
            h += ph;
         }
      }
   }

   public void paint(Graphics g) {
      BWidget[] children = this.getChildWidgets();

      for (int i = 0; i < children.length; i++) {
         if (children[i] instanceof BSeparator) {
            Point pnt = this.translateFromChild(children[i], new Point(0.0, 0.0));
            double x0 = pnt.x;
            double y0 = pnt.y;
            double x1 = pnt.x + this.getWidth();
            double y1 = pnt.y + 6.0 - 1.0;
            g.setBrush(Theme.widget().getControlBackground());
            g.fillRect(x0, y0, x1, 6.0);
            g.setBrush(Theme.widget().getControlHighlight());
            g.strokeLine(x0, y0, x1, y0);
            g.setBrush(BColor.black.toBrush());
            g.strokeLine(x0, y1, x1, y1);
         } else {
            this.paintChild(g, children[i]);
            BCellSheetExpandablePane exp = (BCellSheetExpandablePane)children[i];
            if (!exp.isExpanded()) {
               Point pnt = this.translateFromChild(exp, new Point(0.0, exp.getHeight()));
               g.setBrush(BColor.black.toBrush());
               g.strokeLine(pnt.x, pnt.y, pnt.x + this.getWidth(), pnt.y);
            }
         }
      }
   }
}
