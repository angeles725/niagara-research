package com.tridium.px.editor.studio.trackers;

import com.tridium.gx.util.GeomUtil;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.artisans.PathArtisan;
import com.tridium.px.editor.studio.artisans.PolygonArtisan;
import com.tridium.px.editor.studio.commands.MorphWidget;
import com.tridium.px.editor.studio.painters.PointPainter;
import com.tridium.px.editor.util.Handle;
import com.tridium.px.editor.util.Reflector;
import com.tridium.px.editor.util.SelectedWidgets;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.baja.gx.BPathGeom;
import javax.baja.gx.BPolygonGeom;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.LineTo;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.shape.BPath;
import javax.baja.ui.shape.BPolygon;

public class AddPointTracker extends GeometryTracker {
   private SelectedWidgets selected;
   private TrackerStudio studio;

   public AddPointTracker(BPxEditorPane editorPane, TrackerStudio studio) {
      super(editorPane);
      this.studio = studio;
      this.selected = editorPane.getSelectedWidgets();
      this.selected.deselectAll();
      studio.setPainter(new PointPainter(editorPane.getPainterStudio(), this.selected));
      this.selectShapes(this.selected, editorPane.getRootContainer());
      editorPane.repaint();
   }

   @Override
   public Tracker mousePressed(BMouseEvent event) {
      if (event.getClickCount() == 1 && event.isButton1Down()) {
         double x = event.getX();
         double y = event.getY();
         Handle handle = this.selected.getHandle(x, y);
         if (handle == null) {
            BWidget widget = this.studio.rootDescendant(new Point(x, y));
            if (widget instanceof BPath || widget instanceof BPolygon) {
               this.addPoint(x, y, widget);
            }
         } else {
            if (!(handle.widget instanceof BPath) && !(handle.widget instanceof BPolygon)) {
               throw new IllegalStateException();
            }

            this.addPoint(x, y, handle.widget);
         }

         this.editorPane.repaint();
         return this;
      } else {
         return this;
      }
   }

   private void addPoint(double x, double y, BWidget widget) {
      Point pnt = this.studio.toViewbox(x, y, Reflector.canvas(widget));
      if (widget instanceof BPath) {
         BPath path = (BPath)widget;
         BPathGeom geom = path.getGeom();
         Segment[] segs = geom.segments();
         Point[] points = this.toPoints(segs);
         int idx = GeomUtil.nearest(pnt, points, false);
         List<Segment> a = new ArrayList<>(Arrays.asList(segs));
         a.add(idx + 1, new LineTo(true, pnt.x, pnt.y));
         segs = a.toArray(new Segment[0]);
         geom = BPathGeom.make(segs);
         new MorphWidget(this.editorPane, path, path.getGeom(), geom, new PathArtisan()).invoke();
      } else {
         if (!(widget instanceof BPolygon)) {
            throw new IllegalStateException();
         }

         BPolygon poly = (BPolygon)widget;
         BPolygonGeom geom = poly.getGeom();
         Point[] points = this.toPoints(geom);
         int idx = GeomUtil.nearest(pnt, points, true);
         List<Point> a = new ArrayList<>(Arrays.asList(points));
         if (idx == points.length - 1) {
            a.add(pnt);
         } else {
            a.add(idx + 1, pnt);
         }

         points = a.toArray(new Point[0]);
         geom = this.fromPoints(points);
         new MorphWidget(this.editorPane, poly, poly.getGeom(), geom, new PolygonArtisan()).invoke();
      }
   }

   private BPolygonGeom fromPoints(Point[] points) {
      double[] x = new double[points.length];
      double[] y = new double[points.length];

      for (int i = 0; i < points.length; i++) {
         x[i] = points[i].x;
         y[i] = points[i].y;
      }

      return BPolygonGeom.make(x, y);
   }

   private Point[] toPoints(BPolygonGeom geom) {
      Point[] points = new Point[geom.size()];

      for (int i = 0; i < geom.size(); i++) {
         points[i] = new Point(geom.x(i), geom.y(i));
      }

      return points;
   }

   private Point[] toPoints(Segment[] segs) {
      Point[] points = new Point[segs.length];

      for (int i = 0; i < segs.length; i++) {
         try {
            Class<?> cls = segs[i].getClass();
            Method mx = cls.getMethod("getX");
            Method my = cls.getMethod("getY");
            double x = (Double)mx.invoke(segs[i]);
            double y = (Double)my.invoke(segs[i]);
            points[i] = new Point(x, y);
         } catch (Exception var11) {
            throw new BajaRuntimeException(var11);
         }
      }

      return points;
   }
}
