package com.tridium.px.editor.studio.artisans.path;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.artisans.Artisan;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BPath;

public abstract class SegmentArtisan {
   public abstract Point point(Point var1, Segment var2);

   public abstract Segment move(Point var1, Segment var2, double var3, double var5);

   public abstract Point paintBars(Graphics var1, TrackerStudio var2, BPath var3, Point var4, Segment var5);

   public abstract Point paintHandles(Graphics var1, TrackerStudio var2, BPath var3, Point var4, Segment var5);

   public abstract Point addHandles(TrackerStudio var1, BPath var2, Point var3, Segment var4, int var5, PointMap var6);

   protected static final void paintBar(Graphics g, TrackerStudio studio, BPath path, Point pnt, Point guide, BColor color) {
      Point h = studio.translateToRoot(path, new Point(pnt.x, pnt.y));
      Point a = studio.translateToRoot(path, new Point(guide.x, guide.y));
      Point b = mirror(h, a);
      g.setBrush(color);
      g.strokeLine(a.x, a.y, b.x, b.y);
      Artisan.paintHandle(g, color, a.x, a.y);
      Artisan.paintHandle(g, color, b.x, b.y);
   }

   protected static final void paintHandle(Graphics g, TrackerStudio studio, BPath path, Point pnt, BColor color) {
      Point a = studio.translateToRoot(path, new Point(pnt.x, pnt.y));
      Artisan.paintHandle(g, color, a.x, a.y);
   }

   protected static final void addHandle(TrackerStudio studio, BPath path, Point pnt, PointMap map, MouseCursor cursor, Object role) {
      Point a = studio.translateToRoot(path, new Point(pnt.x, pnt.y));
      Artisan.addHandle(a.x, a.y, map, path, cursor, role);
   }

   protected static final Point nextPoint(Point pen, boolean absolute, double x, double y) {
      return absolute ? new Point(x, y) : new Point(pen.x + x, pen.y + y);
   }

   protected static final Point mirror(Point h, Point a) {
      double dx = a.x - h.x;
      double dy = a.y - h.y;
      return new Point(h.x - dx, h.y - dy);
   }
}
