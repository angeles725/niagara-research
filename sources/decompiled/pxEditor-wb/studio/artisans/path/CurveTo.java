package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class CurveTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.CurveTo n = (javax.baja.gx.IPathGeom.CurveTo)seg;
      return new Point(n.getX(), n.getY());
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.CurveTo cv = (javax.baja.gx.IPathGeom.CurveTo)seg;
         return new javax.baja.gx.IPathGeom.CurveTo(
            cv.isAbsolute(), cv.getX1() + dx, cv.getY1() + dy, cv.getX2() + dx, cv.getY2() + dy, cv.getX() + dx, cv.getY() + dy
         );
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.CurveTo cv = (javax.baja.gx.IPathGeom.CurveTo)seg;
      Point next = nextPoint(pen, cv.isAbsolute(), cv.getX(), cv.getY());
      Point p1 = nextPoint(pen, cv.isAbsolute(), cv.getX1(), cv.getY1());
      Point p2 = nextPoint(pen, cv.isAbsolute(), cv.getX2(), cv.getY2());
      paintBar(g, studio, path, pen, p1, BColor.fuchsia);
      paintBar(g, studio, path, next, p2, BColor.fuchsia);
      return next;
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.CurveTo cv = (javax.baja.gx.IPathGeom.CurveTo)seg;
      Point next = nextPoint(pen, cv.isAbsolute(), cv.getX(), cv.getY());
      Point p1 = nextPoint(pen, cv.isAbsolute(), cv.getX1(), cv.getY1());
      Point p2 = nextPoint(pen, cv.isAbsolute(), cv.getX2(), cv.getY2());
      paintHandle(g, studio, path, next, BColor.lime);
      return next;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.CurveTo cv = (javax.baja.gx.IPathGeom.CurveTo)seg;
      Point p1a = nextPoint(pen, cv.isAbsolute(), cv.getX1(), cv.getY1());
      Point p1b = mirror(pen, p1a);
      Point next = nextPoint(pen, cv.isAbsolute(), cv.getX(), cv.getY());
      Point p2a = nextPoint(pen, cv.isAbsolute(), cv.getX2(), cv.getY2());
      Point p2b = mirror(next, p2a);
      addHandle(studio, path, next, map, MouseCursor.move, new CurveTo.Next(idx, next, p1a, p2a));
      addHandle(studio, path, p1a, map, MouseCursor.move, new CurveTo.P1a(idx, next, p1a, p2a));
      addHandle(studio, path, p1b, map, MouseCursor.move, new CurveTo.P1b(idx, next, p1b, p2a, pen));
      addHandle(studio, path, p2a, map, MouseCursor.move, new CurveTo.P2a(idx, next, p1a, p2a));
      addHandle(studio, path, p2b, map, MouseCursor.move, new CurveTo.P2b(idx, next, p1a, p2b));
      return next;
   }

   static class Next extends Role {
      Point next;
      Point p1a;
      Point p2a;

      Next(int idx, Point next, Point p1a, Point p2a) {
         super(next, idx);
         this.next = next;
         this.p1a = p1a;
         this.p2a = p2a;
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.CurveTo(true, this.p1a.x, this.p1a.y, this.p2a.x, this.p2a.y, this.next.x + dx, this.next.y + dy);
      }
   }

   static class P1a extends Role {
      Point next;
      Point p1a;
      Point p2a;

      P1a(int idx, Point next, Point p1a, Point p2a) {
         super(p1a, idx);
         this.next = next;
         this.p1a = p1a;
         this.p2a = p2a;
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.CurveTo(true, this.p1a.x + dx, this.p1a.y + dy, this.p2a.x, this.p2a.y, this.next.x, this.next.y);
      }
   }

   static class P1b extends Role {
      Point next;
      Point p1b;
      Point p2a;
      Point pen;

      P1b(int idx, Point next, Point p1b, Point p2a, Point pen) {
         super(p1b, idx);
         this.next = next;
         this.p1b = p1b;
         this.p2a = p2a;
         this.pen = pen;
      }

      @Override
      public Segment apply(double dx, double dy) {
         Point p1a = SegmentArtisan.mirror(this.pen, new Point(this.p1b.x + dx, this.p1b.y + dy));
         return new javax.baja.gx.IPathGeom.CurveTo(true, p1a.x, p1a.y, this.p2a.x, this.p2a.y, this.next.x, this.next.y);
      }
   }

   static class P2a extends Role {
      Point next;
      Point p1a;
      Point p2a;

      P2a(int idx, Point next, Point p1a, Point p2a) {
         super(p2a, idx);
         this.next = next;
         this.p1a = p1a;
         this.p2a = p2a;
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.CurveTo(true, this.p1a.x, this.p1a.y, this.p2a.x + dx, this.p2a.y + dy, this.next.x, this.next.y);
      }
   }

   static class P2b extends Role {
      Point next;
      Point p1a;
      Point p2b;

      P2b(int idx, Point next, Point p1a, Point p2b) {
         super(p2b, idx);
         this.next = next;
         this.p1a = p1a;
         this.p2b = p2b;
      }

      @Override
      public Segment apply(double dx, double dy) {
         Point p2a = SegmentArtisan.mirror(this.next, new Point(this.p2b.x + dx, this.p2b.y + dy));
         return new javax.baja.gx.IPathGeom.CurveTo(true, this.p1a.x, this.p1a.y, p2a.x, p2a.y, this.next.x, this.next.y);
      }
   }
}
