package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class SmoothCurveTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.SmoothCurveTo n = (javax.baja.gx.IPathGeom.SmoothCurveTo)seg;
      return new Point(n.getX(), n.getY());
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.SmoothCurveTo sc = (javax.baja.gx.IPathGeom.SmoothCurveTo)seg;
         return new javax.baja.gx.IPathGeom.SmoothCurveTo(sc.isAbsolute(), sc.getX2() + dx, sc.getY2() + dy, sc.getX() + dx, sc.getY() + dy);
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.SmoothCurveTo sc = (javax.baja.gx.IPathGeom.SmoothCurveTo)seg;
      Point next = nextPoint(pen, sc.isAbsolute(), sc.getX(), sc.getY());
      Point p2 = nextPoint(pen, sc.isAbsolute(), sc.getX2(), sc.getY2());
      paintBar(g, studio, path, next, p2, BColor.fuchsia);
      return next;
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.SmoothCurveTo sc = (javax.baja.gx.IPathGeom.SmoothCurveTo)seg;
      Point next = nextPoint(pen, sc.isAbsolute(), sc.getX(), sc.getY());
      Point p2 = nextPoint(pen, sc.isAbsolute(), sc.getX2(), sc.getY2());
      paintHandle(g, studio, path, next, BColor.lime);
      return next;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.SmoothCurveTo sc = (javax.baja.gx.IPathGeom.SmoothCurveTo)seg;
      Point next = nextPoint(pen, sc.isAbsolute(), sc.getX(), sc.getY());
      Point p2a = nextPoint(pen, sc.isAbsolute(), sc.getX2(), sc.getY2());
      Point p2b = mirror(next, p2a);
      addHandle(studio, path, next, map, MouseCursor.move, new SmoothCurveTo.SmoothCurveToRole(next, idx, 0, next, p2a));
      addHandle(studio, path, p2a, map, MouseCursor.move, new SmoothCurveTo.SmoothCurveToRole(p2a, idx, 1, next, p2a, false));
      addHandle(studio, path, p2b, map, MouseCursor.move, new SmoothCurveTo.SmoothCurveToRole(p2b, idx, 1, next, p2b, true));
      return next;
   }

   static class SmoothCurveToRole extends Role {
      int which;
      Point p0;
      Point p2;
      boolean mirror;

      SmoothCurveToRole(Point orig, int idx, int which, Point p0, Point p2) {
         super(orig, idx);
         this.which = which;
         this.p0 = p0;
         this.p2 = p2;
      }

      SmoothCurveToRole(Point orig, int idx, int which, Point p0, Point p2, boolean mirror) {
         super(orig, idx);
         this.which = which;
         this.p0 = p0;
         this.p2 = p2;
         this.mirror = mirror;
      }

      @Override
      public Segment apply(double dx, double dy) {
         if (this.which == 0) {
            return new javax.baja.gx.IPathGeom.SmoothCurveTo(true, this.p2.x, this.p2.y, this.orig.x + dx, this.orig.y + dy);
         } else if (this.mirror) {
            Point z = new Point(this.orig.x + dx, this.orig.y + dy);
            z = SegmentArtisan.mirror(this.p0, z);
            return new javax.baja.gx.IPathGeom.SmoothCurveTo(true, z.x, z.y, this.p0.x, this.p0.y);
         } else {
            return new javax.baja.gx.IPathGeom.SmoothCurveTo(true, this.orig.x + dx, this.orig.y + dy, this.p0.x, this.p0.y);
         }
      }
   }
}
