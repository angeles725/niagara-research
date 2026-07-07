package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class HLineTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.HLineTo hl = (javax.baja.gx.IPathGeom.HLineTo)seg;
      return new Point(hl.getX(), pen.y);
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.HLineTo hl = (javax.baja.gx.IPathGeom.HLineTo)seg;
         return new javax.baja.gx.IPathGeom.HLineTo(hl.isAbsolute(), hl.getX() + dx);
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.HLineTo hl = (javax.baja.gx.IPathGeom.HLineTo)seg;
      return nextPoint(pen, hl.isAbsolute(), hl.getX(), pen.y);
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.HLineTo hl = (javax.baja.gx.IPathGeom.HLineTo)seg;
      Point next = nextPoint(pen, hl.isAbsolute(), hl.getX(), pen.y);
      paintHandle(g, studio, path, next, BColor.lime);
      return next;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.HLineTo hl = (javax.baja.gx.IPathGeom.HLineTo)seg;
      Point next = nextPoint(pen, hl.isAbsolute(), hl.getX(), pen.y);
      addHandle(studio, path, next, map, MouseCursor.move, new HLineTo.Next(idx, next, hl));
      return next;
   }

   static class Next extends Role {
      Point next;
      javax.baja.gx.IPathGeom.HLineTo hl;

      Next(int idx, Point next, javax.baja.gx.IPathGeom.HLineTo hl) {
         super(next, idx);
         this.next = next;
         this.hl = hl;
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.HLineTo(this.hl.isAbsolute(), this.hl.getX() + dx);
      }
   }
}
