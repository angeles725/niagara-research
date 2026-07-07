package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BLayout;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BCanvasPane;

public class WidgetArtisan extends RectangularArtisan {
   @Override
   public void paintSelected(TrackerStudio studio, BWidget widget, Graphics g) {
      if (!(widget instanceof BNullWidget)) {
         this.paintRect(this.rootBounds(studio, widget), g);
      }
   }

   @Override
   public RectGeom bounds(BWidget widget) {
      return new RectGeom(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
   }

   @Override
   public String move(BWidget widget, double dx, double dy) {
      BLayout ly = widget.getLayout();
      widget.setLayout(BLayout.make(widget.getX() + dx, 0, widget.getY() + dy, 0, ly.getWidth(), ly.getWidthUnit(), ly.getHeight(), ly.getHeightUnit()));
      return "layout";
   }

   @Override
   public String zero(BWidget widget) {
      BLayout ly = widget.getLayout();
      widget.setLayout(BLayout.make(0.0, 0, 0.0, 0, ly.getWidth(), ly.getWidthUnit(), ly.getHeight(), ly.getHeightUnit()));
      widget.setBounds(0.0, 0.0, 0.0, 0.0);
      return "layout";
   }

   @Override
   public void addHandles(TrackerStudio studio, BWidget widget, PointMap map) {
      this.addRectHandles(widget, map, this.rootBounds(studio, widget), !(widget.getParentWidget() instanceof BCanvasPane));
   }

   @Override
   public String setGeom(BWidget widget, IGeom geom) {
      RectGeom rect = (RectGeom)geom;
      widget.setLayout(BLayout.make(rect.x, 0, rect.y, 0, rect.width, 0, rect.height, 0));
      return "layout";
   }

   @Override
   public RectGeom rootBounds(TrackerStudio studio, BWidget widget) {
      BWidget parent = widget.getParentWidget();
      if (parent instanceof BCanvasPane) {
         BCanvasPane canvas = (BCanvasPane)parent;
         RectGeom bnd = getBounds(widget);
         Point a = studio.fromViewbox(bnd.x, bnd.y, canvas);
         Point b = studio.fromViewbox(bnd.x + bnd.width, bnd.y + bnd.height, canvas);
         return new RectGeom(a.x, a.y, b.x - a.x, b.y - a.y);
      } else {
         Point a = studio.translateToRoot(widget, new Point(0.0, 0.0));
         return new RectGeom(a.x, a.y, widget.getWidth(), widget.getHeight());
      }
   }

   private static RectGeom getBounds(BWidget widget) {
      double x = widget.getX();
      double y = widget.getY();

      for (BWidget parent = widget.getParentWidget(); parent instanceof BBorderPane; parent = parent.getParentWidget()) {
         x += parent.getX();
         y += parent.getY();
      }

      return new RectGeom(x, y, widget.getWidth(), widget.getHeight());
   }
}
