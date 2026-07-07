package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.PolygonArtisan;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BPolygonGeom;
import javax.baja.gx.BSize;
import javax.baja.gx.Geom;
import javax.baja.gx.Point;
import javax.baja.gx.PolygonGeom;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.shape.BPolygon;

public class PolygonHandleTracker extends ShapeHandleTracker implements GeomSupplier {
   private double[] xarr;
   private double[] yarr;
   private int idx;
   private Point orig;

   public PolygonHandleTracker(BPxEditorPane editorPane, Handle handle, PolygonArtisan artisan) {
      super(editorPane, handle, artisan);
      BPolygon poly = (BPolygon)handle.widget;
      BPolygonGeom geom = poly.getGeom();
      this.xarr = geom.x();
      this.yarr = geom.y();
      this.idx = (Integer)handle.role;
      this.orig = new Point(this.xarr[this.idx], this.yarr[this.idx]);
   }

   @Override
   public Geom[] geoms() {
      double x = this.orig.x + this.delta.x;
      double y = this.orig.y + this.delta.y;
      BCanvasPane canvas = (BCanvasPane)this.handle.widget.getParent();
      BSize viewSize = canvas.getViewSize();
      if (x < 0.0) {
         x = 0.0;
      } else if (x > viewSize.width) {
         x = viewSize.width;
      }

      if (y < 0.0) {
         y = 0.0;
      } else if (y > viewSize.height) {
         y = viewSize.height;
      }

      this.xarr[this.idx] = x;
      this.yarr[this.idx] = y;
      return new Geom[]{new PolygonGeom(this.xarr, this.yarr, this.xarr.length)};
   }
}
