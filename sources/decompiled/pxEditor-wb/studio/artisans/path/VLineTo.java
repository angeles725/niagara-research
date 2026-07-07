package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public class VLineTo extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.VLineTo vl = (javax.baja.gx.IPathGeom.VLineTo)seg;
      return new Point(pen.x, vl.getY());
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      if (seg.isRelative()) {
         return seg;
      } else {
         javax.baja.gx.IPathGeom.VLineTo vl = (javax.baja.gx.IPathGeom.VLineTo)seg;
         return new javax.baja.gx.IPathGeom.VLineTo(vl.isAbsolute(), pen.x + dx);
      }
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.VLineTo vl = (javax.baja.gx.IPathGeom.VLineTo)seg;
      return nextPoint(pen, vl.isAbsolute(), pen.x, vl.getY());
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      javax.baja.gx.IPathGeom.VLineTo vl = (javax.baja.gx.IPathGeom.VLineTo)seg;
      Point next = nextPoint(pen, vl.isAbsolute(), pen.x, vl.getY());
      paintHandle(g, studio, path, next, BColor.lime);
      return next;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      javax.baja.gx.IPathGeom.VLineTo vl = (javax.baja.gx.IPathGeom.VLineTo)seg;
      Point next = nextPoint(pen, vl.isAbsolute(), pen.x, vl.getY());
      addHandle(studio, path, next, map, MouseCursor.move, new VLineTo.Next(idx, next, vl));
      return next;
   }

   static class Next extends Role {
      Point next;
      javax.baja.gx.IPathGeom.VLineTo vl;

      Next(int idx, Point next, javax.baja.gx.IPathGeom.VLineTo vl) {
         super(next, idx);
         this.next = next;
         this.vl = vl;
      }

      @Override
      public Segment apply(double dx, double dy) {
         return new javax.baja.gx.IPathGeom.VLineTo(this.vl.isAbsolute(), this.vl.getY() + dy);
      }
   }
}
