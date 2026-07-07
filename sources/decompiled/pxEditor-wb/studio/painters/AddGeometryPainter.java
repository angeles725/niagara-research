package com.tridium.px.editor.studio.painters;

import com.tridium.px.editor.studio.PainterStudio;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.studio.trackers.AddGeometryTracker;
import javax.baja.gx.BColor;
import javax.baja.gx.BPen;
import javax.baja.gx.Geom;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;

public class AddGeometryPainter extends Painter {
   private PainterStudio studio;
   private AddGeometryTracker tracker;

   public AddGeometryPainter(PainterStudio studio, AddGeometryTracker tracker) {
      this.studio = studio;
      this.tracker = tracker;
      studio.buffer();
   }

   @Override
   public synchronized void doPaint(Graphics g) {
      this.studio.paintBuffer(g);
      if (this.tracker.pageOffset != null && this.tracker.scaleTransform != null) {
         g.setBrush(BColor.black);
         g.setPen(BPen.DEFAULT);
         g.translate(this.tracker.pageOffset.x, this.tracker.pageOffset.y);
         g.transform(this.tracker.scaleTransform);
         Geom[] geoms = this.tracker.geoms();

         for (int i = 0; i < geoms.length; i++) {
            if (geoms[i] != null) {
               g.stroke(geoms[i]);
            }
         }

         g.transform(this.tracker.scaleTransform.getInverse());
         g.translate(-this.tracker.pageOffset.x, -this.tracker.pageOffset.y);
         Point[] handles = this.tracker.handles();
         Point[] bars = this.tracker.bars();
         Artisan.smallHandles();

         for (int ix = 0; ix < bars.length; ix++) {
            if (bars[ix] != null) {
               Point h = handles[ix];
               Point a = bars[ix];
               double dx = a.x - h.x;
               double dy = a.y - h.y;
               Point b = new Point(h.x - dx, h.y - dy);
               g.setBrush(BColor.fuchsia);
               g.strokeLine(h.x, h.y, a.x, a.y);
               g.strokeLine(h.x, h.y, b.x, b.y);
               Artisan.paintHandle(g, BColor.fuchsia, a.x, a.y);
               Artisan.paintHandle(g, BColor.fuchsia, b.x, b.y);
            }
         }

         for (int ixx = 0; ixx < handles.length; ixx++) {
            Artisan.paintHandle(g, BColor.lime, handles[ixx].x, handles[ixx].y);
         }

         Artisan.largeHandles();
      }
   }

   @Override
   public void reset() {
      this.studio.unbuffer();
      this.studio.buffer();
   }
}
