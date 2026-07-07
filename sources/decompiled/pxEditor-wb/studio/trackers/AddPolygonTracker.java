package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.Geom;
import javax.baja.gx.LineGeom;
import javax.baja.gx.Point;
import javax.baja.gx.PolygonGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.shape.BPolygon;

public class AddPolygonTracker extends AddGeometryTracker {
   public AddPolygonTracker(BPxEditorPane editorPane, TrackerStudio studio) {
      super(editorPane, studio);
   }

   @Override
   protected boolean widgetMakeable() {
      return this.handles.size() >= 3;
   }

   @Override
   protected BWidget makeWidget() {
      Point[] pnts = this.handles.toArray(new Point[0]);
      int len = pnts.length;
      double[] x = new double[len];
      double[] y = new double[len];

      for (int i = 0; i < len; i++) {
         x[i] = pnts[i].x;
         y[i] = pnts[i].y;
      }

      BPolygon poly = new BPolygon(new PolygonGeom(x, y, len));
      poly.setStroke(BBrush.makeSolid(BColor.black));
      return poly;
   }

   @Override
   public Geom[] geoms() {
      if (this.handles.size() == 0) {
         return new Geom[0];
      } else {
         Point[] pnts = this.handles.toArray(new Point[0]);
         int len = pnts.length;
         if (len == 1) {
            return new Geom[]{new LineGeom(pnts[0].x, pnts[0].y, this.mouse.x, this.mouse.y)};
         } else {
            double[] x = new double[len + 1];
            double[] y = new double[len + 1];

            for (int i = 0; i < len; i++) {
               x[i] = pnts[i].x;
               y[i] = pnts[i].y;
            }

            x[len] = this.mouse.x;
            y[len] = this.mouse.y;
            return new Geom[]{new PolygonGeom(x, y, len + 1)};
         }
      }
   }
}
