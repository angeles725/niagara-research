package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class QuadTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.QuadTo n = (javax.baja.gx.IPathGeom.QuadTo)seg;
      return new Point(n.getX(), n.getY());
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.QuadTo qd = (javax.baja.gx.IPathGeom.QuadTo)seg;
         return new javax.baja.gx.IPathGeom.QuadTo(qd.isAbsolute(), qd.getX1() + dx, qd.getY1() + dy, qd.getX() + dx, qd.getY() + dy);
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.QuadTo qd = (javax.baja.gx.IPathGeom.QuadTo)seg;
      Point next = nextPoint(pen, qd.isAbsolute(), qd.getX(), qd.getY());
      Point p1 = nextPoint(pen, qd.isAbsolute(), qd.getX1(), qd.getY1());
      paintBar(g, studio, path, next, p1, BColor.fuchsia);
      return next;
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.QuadTo qd = (javax.baja.gx.IPathGeom.QuadTo)seg;
      Point next = nextPoint(pen, qd.isAbsolute(), qd.getX(), qd.getY());
      Point p1 = nextPoint(pen, qd.isAbsolute(), qd.getX1(), qd.getY1());
      paintHandle(g, studio, path, next, BColor.lime);
      return next;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.QuadTo qd = (javax.baja.gx.IPathGeom.QuadTo)seg;
      Point next = nextPoint(pen, qd.isAbsolute(), qd.getX(), qd.getY());
      Point p1a = nextPoint(pen, qd.isAbsolute(), qd.getX1(), qd.getY1());
      Point p1b = mirror(next, p1a);
      addHandle(studio, path, next, map, MouseCursor.move, new QuadTo.QuadToRole(next, idx, 0, next, p1a));
      addHandle(studio, path, p1a, map, MouseCursor.move, new QuadTo.QuadToRole(p1a, idx, 1, next, p1a, false));
      addHandle(studio, path, p1b, map, MouseCursor.move, new QuadTo.QuadToRole(p1b, idx, 1, next, p1b, true));
      return next;
   }

   static class QuadToRole extends Role {
      int which;
      Point p0;
      Point p1;
      boolean mirror;

      QuadToRole(Point orig, int idx, int which, Point p0, Point p1) {
         super(orig, idx);
         this.which = which;
         this.p0 = p0;
         this.p1 = p1;
      }

      QuadToRole(Point orig, int idx, int which, Point p0, Point p1, boolean mirror) {
         super(orig, idx);
         this.which = which;
         this.p0 = p0;
         this.p1 = p1;
         this.mirror = mirror;
      }

      @Override
      public Segment apply(double dx, double dy) {
         if (this.which == 0) {
            return new javax.baja.gx.IPathGeom.QuadTo(true, this.p1.x, this.p1.y, this.orig.x + dx, this.orig.y + dy);
         } else if (this.mirror) {
            Point z = new Point(this.orig.x + dx, this.orig.y + dy);
            z = SegmentArtisan.mirror(this.p0, z);
            return new javax.baja.gx.IPathGeom.QuadTo(true, z.x, z.y, this.p0.x, this.p0.y);
         } else {
            return new javax.baja.gx.IPathGeom.QuadTo(true, this.orig.x + dx, this.orig.y + dy, this.p0.x, this.p0.y);
         }
      }
   }
}
