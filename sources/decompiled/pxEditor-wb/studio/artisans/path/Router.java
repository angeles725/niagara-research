package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.shape.BPath;

public class Router extends SegmentArtisan {
   static SegmentArtisan closePath = new ClosePath();
   static SegmentArtisan moveTo = new MoveTo();
   static SegmentArtisan lineTo = new LineTo();
   static SegmentArtisan hLineTo = new HLineTo();
   static SegmentArtisan vLineTo = new VLineTo();
   static SegmentArtisan curveTo = new CurveTo();
   static SegmentArtisan smoothCurveTo = new SmoothCurveTo();
   static SegmentArtisan quadTo = new QuadTo();
   static SegmentArtisan smoothQuadTo = new SmoothQuadTo();
   static SegmentArtisan arcTo = new ArcTo();

   @Override
   public Point point(Point pen, Segment seg) {
      return artisan(seg.getClass()).point(pen, seg);
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      return artisan(seg.getClass()).move(pen, seg, dx, dy);
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      return artisan(seg.getClass()).paintBars(g, studio, path, pen, seg);
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      return artisan(seg.getClass()).paintHandles(g, studio, path, pen, seg);
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      return artisan(seg.getClass()).addHandles(studio, path, pen, seg, idx, map);
   }

   private static SegmentArtisan artisan(Class<?> cls) {
      if (cls.equals(javax.baja.gx.IPathGeom.ClosePath.class)) {
         return closePath;
      } else if (cls.equals(javax.baja.gx.IPathGeom.MoveTo.class)) {
         return moveTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.LineTo.class)) {
         return lineTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.HLineTo.class)) {
         return hLineTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.VLineTo.class)) {
         return vLineTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.CurveTo.class)) {
         return curveTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.SmoothCurveTo.class)) {
         return smoothCurveTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.QuadTo.class)) {
         return quadTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.SmoothQuadTo.class)) {
         return smoothQuadTo;
      } else if (cls.equals(javax.baja.gx.IPathGeom.ArcTo.class)) {
         return arcTo;
      } else {
         throw new IllegalStateException();
      }
   }
}
