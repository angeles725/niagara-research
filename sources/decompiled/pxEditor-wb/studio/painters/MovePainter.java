package com.tridium.px.editor.studio.painters;

import com.tridium.px.editor.studio.PainterStudio;
import com.tridium.px.editor.studio.trackers.MoveTracker;
import javax.baja.gx.BColor;
import javax.baja.gx.BPen;
import javax.baja.gx.BTransform;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.Point;

public class MovePainter extends Painter {
   private PainterStudio studio;
   private MoveTracker tracker;
   private BTransform scaleTransform;
   private Point pageOffset;

   public MovePainter(PainterStudio studio, MoveTracker tracker, BTransform scaleTransform, Point pageOffset) {
      this.studio = studio;
      this.tracker = tracker;
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
      IGeom[] geoms = this.tracker.geoms();

      for (int i = 0; i < geoms.length; i++) {
         g.stroke(geoms[i]);
      }

      g.transform(this.scaleTransform.getInverse());
      g.translate(-this.pageOffset.x, -this.pageOffset.y);
   }
}
