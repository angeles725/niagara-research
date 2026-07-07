package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class MoveTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.MoveTo n = (javax.baja.gx.IPathGeom.MoveTo)seg;
      return new Point(n.getX(), n.getY());
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.MoveTo mv = (javax.baja.gx.IPathGeom.MoveTo)seg;
         return new javax.baja.gx.IPathGeom.MoveTo(mv.isAbsolute(), mv.getX() + dx, mv.getY() + dy);
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.MoveTo mv = (javax.baja.gx.IPathGeom.MoveTo)seg;
      if (!mv.isAbsolute()) {
         throw new IllegalStateException();
      } else {
         return new Point(mv.getX(), mv.getY());
      }
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.MoveTo mv = (javax.baja.gx.IPathGeom.MoveTo)seg;
      if (!mv.isAbsolute()) {
         throw new IllegalStateException();
      } else {
         Point next = new Point(mv.getX(), mv.getY());
         paintHandle(g, studio, path, next, BColor.lime);
         return next;
      }
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.MoveTo mv = (javax.baja.gx.IPathGeom.MoveTo)seg;
      if (!mv.isAbsolute()) {
         throw new IllegalStateException();
      } else {
         Point next = new Point(mv.getX(), mv.getY());
         addHandle(studio, path, next, map, MouseCursor.move, new MoveTo.MoveToRole(next, idx));
         return next;
      }
   }

   static class MoveToRole extends Role {
      MoveToRole(Point orig, int idx) {
         super(orig, idx);
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.MoveTo(true, this.orig.x + dx, this.orig.y + dy);
      }
   }
}
