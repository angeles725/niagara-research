package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.shape.BPath;

public class ClosePath extends SegmentArtisan {
   @Override
   public Point point(Point pen, Segment seg) {
      return pen;
   }

   @Override
   public Segment move(Point pen, Segment seg, double dx, double dy) {
      return new javax.baja.gx.IPathGeom.ClosePath();
   }

   @Override
   public Point paintBars(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      return pen;
   }

   @Override
   public Point paintHandles(Graphics g, TrackerStudio studio, BPath path, Point pen, Segment seg) {
      return pen;
   }

   @Override
   public Point addHandles(TrackerStudio studio, BPath path, Point pen, Segment seg, int idx, PointMap map) {
      return pen;
   }
}
