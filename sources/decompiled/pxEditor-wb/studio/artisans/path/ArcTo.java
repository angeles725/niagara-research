package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class ArcTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.ArcTo n = (javax.baja.gx.IPathGeom.ArcTo)seg;
      return new Point(n.getX(), n.getY());
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.ArcTo arc = (javax.baja.gx.IPathGeom.ArcTo)seg;
         return new javax.baja.gx.IPathGeom.ArcTo(
            arc.isAbsolute(),
            arc.getRadiusX() + dx,
            arc.getRadiusY() + dy,
            arc.getXAxisRotation(),
            arc.getLargeArcFlag(),
            arc.getSweepFlag(),
            arc.getX() + dx,
            arc.getY() + dy
         );
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.ArcTo arc = (javax.baja.gx.IPathGeom.ArcTo)seg;
      Point next = nextPoint(pen, arc.isAbsolute(), arc.getX(), arc.getY());
      Point rad = nextPoint(pen, arc.isAbsolute(), arc.getRadiusX(), arc.getRadiusY());
      Point h = studio.translateToRoot(path, new Point(rad.x, rad.y));
      Point a = studio.translateToRoot(path, new Point(pen.x, pen.y));
      Point b = studio.translateToRoot(path, new Point(next.x, next.y));
      g.setBrush(BColor.fuchsia);
      g.strokeLine(a.x, a.y, h.x, h.y);
      g.strokeLine(b.x, b.y, h.x, h.y);
      return next;
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.ArcTo arc = (javax.baja.gx.IPathGeom.ArcTo)seg;
      Point next = nextPoint(pen, arc.isAbsolute(), arc.getX(), arc.getY());
      Point rad = nextPoint(pen, arc.isAbsolute(), arc.getRadiusX(), arc.getRadiusY());
      paintHandle(g, studio, path, next, BColor.lime);
      paintHandle(g, studio, path, rad, BColor.fuchsia);
      return next;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.ArcTo arc = (javax.baja.gx.IPathGeom.ArcTo)seg;
      Point next = nextPoint(pen, arc.isAbsolute(), arc.getX(), arc.getY());
      Point rad = nextPoint(pen, arc.isAbsolute(), arc.getRadiusX(), arc.getRadiusY());
      addHandle(studio, path, next, map, MouseCursor.move, new ArcTo.Next(idx, next, arc));
      addHandle(studio, path, rad, map, MouseCursor.move, new ArcTo.Radius(idx, rad, arc));
      return next;
   }

   static class Next extends Role {
      Point next;
      javax.baja.gx.IPathGeom.ArcTo arc;

      Next(int idx, Point next, javax.baja.gx.IPathGeom.ArcTo arc) {
         super(next, idx);
         this.next = next;
         this.arc = arc;
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.ArcTo(
            this.arc.isAbsolute(),
            this.arc.getRadiusX() + dx,
            this.arc.getRadiusY() + dy,
            this.arc.getXAxisRotation(),
            this.arc.getLargeArcFlag(),
            this.arc.getSweepFlag(),
            this.next.x + dx,
            this.next.y + dy
         );
      }
   }

   static class Radius extends Role {
      Point rad;
      javax.baja.gx.IPathGeom.ArcTo arc;

      Radius(int idx, Point rad, javax.baja.gx.IPathGeom.ArcTo arc) {
         super(rad, idx);
         this.rad = rad;
         this.arc = arc;
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.ArcTo(
            this.arc.isAbsolute(),
            this.rad.x + dx,
            this.rad.y + dy,
            this.arc.getXAxisRotation(),
            this.arc.getLargeArcFlag(),
            this.arc.getSweepFlag(),
            this.arc.getX(),
            this.arc.getY()
         );
      }
   }
}
