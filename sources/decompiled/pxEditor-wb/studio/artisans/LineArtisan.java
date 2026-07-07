package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.util.GeomUtil;
import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.trackers.HandleTracker;
import com.tridium.px.editor.studio.trackers.LineHandleTracker;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BColor;
import javax.baja.gx.BLineGeom;
import javax.baja.gx.BPen;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.ILineGeom;
import javax.baja.gx.LineGeom;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.shape.BLine;

public class LineArtisan extends Artisan {
   @Override
   public void paintSelected(TrackerStudio studio, BWidget widget, Graphics g) {
      BLine line = (BLine)widget;
      BLineGeom geom = line.getGeom();
      Point a = studio.translateToRoot(line, new Point(geom.x1, geom.y1));
      Point b = studio.translateToRoot(line, new Point(geom.x2, geom.y2));
      g.setPen(BPen.DEFAULT);
      g.setBrush(BColor.lime);
      paintHandle(g, BColor.lime, a.x, a.y);
      paintHandle(g, BColor.lime, b.x, b.y);
   }

   @Override
   public RectGeom bounds(BWidget widget) {
      BLine line = (BLine)widget;
      BLineGeom geom = line.getGeom();
      return GeomUtil.makeRectangle(geom.x1, geom.y1, geom.x2, geom.y2);
   }

   @Override
   public String move(BWidget widget, double dx, double dy) {
      BLine line = (BLine)widget;
      BLineGeom geom = line.getGeom();
      line.setGeom(BLineGeom.make(geom.x1 + dx, geom.y1 + dy, geom.x2 + dx, geom.y2 + dy));
      return "geom";
   }

   @Override
   public String zero(BWidget widget) {
      BLine line = (BLine)widget;
      BLineGeom geom = line.getGeom();
      double x = Math.min(geom.x1, geom.x2);
      double y = Math.min(geom.y1, geom.y2);
      line.setGeom(BLineGeom.make(geom.x1 - x, geom.y1 - y, geom.x2 - x, geom.y2 - y));
      return "geom";
   }

   @Override
   public IGeom translateGeom(BWidget widget, double dx, double dy) {
      BLine line = (BLine)widget;
      BLineGeom geom = line.getGeom();
      return new LineGeom(geom.x1() + dx, geom.y1() + dy, geom.x2() + dx, geom.y2() + dy);
   }

   @Override
   public void addHandles(TrackerStudio studio, BWidget widget, PointMap map) {
      BLine line = (BLine)widget;
      BLineGeom geom = line.getGeom();
      Point a = studio.translateToRoot(line, new Point(geom.x1, geom.y1));
      Point b = studio.translateToRoot(line, new Point(geom.x2, geom.y2));
      addHandle(a.x, a.y, map, widget, MouseCursor.move, 1);
      addHandle(b.x, b.y, map, widget, MouseCursor.move, 2);
   }

   @Override
   public HandleTracker makeHandleTracker(BPxEditorPane editorPane, Handle handle, boolean preserveAspectRatio) {
      return new LineHandleTracker(editorPane, handle, this);
   }

   @Override
   public String setGeom(BWidget widget, IGeom geom) {
      BLine line = (BLine)widget;
      ILineGeom ln = (ILineGeom)geom;
      line.setGeom(BLineGeom.make(ln.x1(), ln.y1(), ln.x2(), ln.y2()));
      return "geom";
   }
}
