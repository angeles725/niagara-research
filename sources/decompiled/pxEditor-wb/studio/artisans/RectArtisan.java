package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BRectGeom;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.shape.BRect;

public class RectArtisan extends RectangularArtisan {
   @Override
   public void paintSelected(TrackerStudio studio, BWidget widget, Graphics g) {
      this.paintRect(this.rootBounds(studio, widget), g);
   }

   @Override
   public RectGeom bounds(BWidget widget) {
      BRect rect = (BRect)widget;
      BRectGeom geom = rect.getGeom();
      return new RectGeom(geom.x, geom.y, geom.width, geom.height);
   }

   @Override
   public String move(BWidget widget, double dx, double dy) {
      BRect rect = (BRect)widget;
      BRectGeom geom = rect.getGeom();
      rect.setGeom(BRectGeom.make(geom.x + dx, geom.y + dy, geom.width, geom.height));
      return "geom";
   }

   @Override
   public String zero(BWidget widget) {
      BRect rect = (BRect)widget;
      BRectGeom geom = rect.getGeom();
      rect.setGeom(BRectGeom.make(0.0, 0.0, geom.width, geom.height));
      return "geom";
   }

   @Override
   public void addHandles(TrackerStudio studio, BWidget widget, PointMap map) {
      this.addRectHandles(widget, map, this.rootBounds(studio, widget), false);
   }

   @Override
   public String setGeom(BWidget widget, IGeom geom) {
      RectGeom rect = (RectGeom)geom;
      BRect r = (BRect)widget;
      r.setGeom(BRectGeom.make(rect.x, rect.y, rect.width, rect.height));
      return "geom";
   }
}
