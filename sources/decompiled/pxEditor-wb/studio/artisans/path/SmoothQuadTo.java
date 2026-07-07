package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class SmoothQuadTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.SmoothQuadTo n = (javax.baja.gx.IPathGeom.SmoothQuadTo)seg;
      return new Point(n.getX(), n.getY());
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.SmoothQuadTo sq = (javax.baja.gx.IPathGeom.SmoothQuadTo)seg;
         return new javax.baja.gx.IPathGeom.SmoothQuadTo(sq.isAbsolute(), sq.getX() + dx, sq.getY() + dy);
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.SmoothQuadTo sq = (javax.baja.gx.IPathGeom.SmoothQuadTo)seg;
      return nextPoint(pen, sq.isAbsolute(), sq.getX(), sq.getY());
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.SmoothQuadTo sq = (javax.baja.gx.IPathGeom.SmoothQuadTo)seg;
      Point next = nextPoint(pen, sq.isAbsolute(), sq.getX(), sq.getY());
      paintHandle(g, studio, path, next, BColor.lime);
      return next;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.SmoothQuadTo sq = (javax.baja.gx.IPathGeom.SmoothQuadTo)seg;
      Point next = nextPoint(pen, sq.isAbsolute(), sq.getX(), sq.getY());
      addHandle(studio, path, next, map, MouseCursor.move, new SmoothQuadTo.SmoothQuadToRole(next, idx));
      return next;
   }

   static class SmoothQuadToRole extends Role {
      SmoothQuadToRole(Point orig, int idx) {
         super(orig, idx);
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.SmoothQuadTo(true, this.orig.x + dx, this.orig.y + dy);
      }
   }
}
