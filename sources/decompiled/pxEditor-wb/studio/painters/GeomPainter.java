package com.tridium.px.editor.studio.painters;

import com.tridium.px.editor.studio.PainterStudio;
import com.tridium.px.editor.studio.trackers.GeomSupplier;
import javax.baja.gx.BColor;
import javax.baja.gx.BPen;
import javax.baja.gx.BTransform;
import javax.baja.gx.Geom;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;

public class GeomPainter extends Painter {
   private PainterStudio studio;
   private GeomSupplier supplier;
   private BTransform scaleTransform;
   private Point pageOffset;

   public GeomPainter(PainterStudio studio, GeomSupplier supplier, BTransform scaleTransform, Point pageOffset) {
      this.studio = studio;
      this.supplier = supplier;
      this.scaleTransform = scaleTransform;
      this.pageOffset = pageOffset;
      studio.buffer();
   }

   @Override
   public synchronized void doPaint(Graphics g) {
      this.studio.paintBuffer(g);
      g.setBrush(BColor.black);
      g.setPen(BPen.DEFAULT);
      g.translate(this.pageOffset.x, this.pageOffset.y);
      g.transform(this.scaleTransform);
      Geom[] geoms = this.supplier.geoms();

      for (int i = 0; i < geoms.length; i++) {
         if (geoms[i] != null) {
            g.stroke(geoms[i]);
         }
      }

      g.transform(this.scaleTransform.getInverse());
      g.translate(-this.pageOffset.x, -this.pageOffset.y);
   }
}
