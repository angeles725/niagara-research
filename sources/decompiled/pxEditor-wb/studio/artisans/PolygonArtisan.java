package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.trackers.HandleTracker;
import com.tridium.px.editor.studio.trackers.PolygonHandleTracker;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BColor;
import javax.baja.gx.BPen;
import javax.baja.gx.BPolygonGeom;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.IPolygonGeom;
import javax.baja.gx.Point;
import javax.baja.gx.PolygonGeom;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPolygon;

public class PolygonArtisan extends Artisan {
   @Override
   public void paintSelected(TrackerStudio studio, BWidget widget, Graphics g) {
      Point[] pnts = this.rootPoints(studio, (BPolygon)widget);
      int len = pnts.length;
      g.setPen(BPen.DEFAULT);
      g.setBrush(BColor.lime);

      for (int i = 0; i < len; i++) {
         paintHandle(g, BColor.lime, pnts[i].x, pnts[i].y);
      }
   }

   @Override
   public RectGeom bounds(BWidget widget) {
      BPolygon poly = (BPolygon)widget;
      BPolygonGeom geom = poly.getGeom();
      double[] x = geom.x();
      double[] y = geom.y();
      RectGeom r = new RectGeom(x[0], y[0], 0.0, 0.0);

      for (int i = 1; i < x.length; i++) {
         r = RectGeom.bounds(r.x, r.y, r.width, r.height, x[i], y[i], 0.0, 0.0, r);
      }

      return r;
   }

   @Override
   public String move(BWidget widget, double dx, double dy) {
      BPolygon poly = (BPolygon)widget;
      BPolygonGeom geom = poly.getGeom();
      double[] x = geom.x();
      double[] y = geom.y();

      for (int i = 0; i < x.length; i++) {
         x[i] += dx;
         y[i] += dy;
      }

      poly.setGeom(BPolygonGeom.make(x, y));
      return "geom";
   }

   @Override
   public String zero(BWidget widget) {
      RectGeom bounds = this.bounds(widget);
      return this.move(widget, -bounds.x, -bounds.y);
   }

   @Override
   public IGeom translateGeom(BWidget widget, double dx, double dy) {
      BPolygon poly = (BPolygon)widget;
      BPolygonGeom geom = poly.getGeom();
      double[] x = geom.x();
      double[] y = geom.y();

      for (int i = 0; i < x.length; i++) {
         x[i] += dx;
         y[i] += dy;
      }

      return new PolygonGeom(x, y, x.length);
   }

   @Override
   public void addHandles(TrackerStudio studio, BWidget widget, PointMap map) {
      Point[] pnts = this.rootPoints(studio, (BPolygon)widget);

      for (int i = 0; i < pnts.length; i++) {
         addHandle(pnts[i].x, pnts[i].y, map, widget, MouseCursor.move, i);
      }
   }

   @Override
   public HandleTracker makeHandleTracker(BPxEditorPane editorPane, Handle handle, boolean preserveAspectRatio) {
      return new PolygonHandleTracker(editorPane, handle, this);
   }

   @Override
   public String setGeom(BWidget widget, IGeom geom) {
      BPolygon poly = (BPolygon)widget;
      IPolygonGeom pg = (IPolygonGeom)geom;
      poly.setGeom(BPolygonGeom.make(pg.x(), pg.y(), pg.size()));
      return "geom";
   }

   private Point[] rootPoints(TrackerStudio studio, BPolygon poly) {
      BPolygonGeom geom = poly.getGeom();
      double[] x = geom.x();
      double[] y = geom.y();
      Point[] pnts = new Point[x.length];

      for (int i = 0; i < x.length; i++) {
         pnts[i] = studio.translateToRoot(poly, new Point(x[i], y[i]));
      }

      return pnts;
   }
}
