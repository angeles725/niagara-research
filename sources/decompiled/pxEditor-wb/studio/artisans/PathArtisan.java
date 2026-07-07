package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.awt.se.AwtUtil;
import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.artisans.path.Router;
import com.tridium.px.editor.studio.artisans.path.SegmentArtisan;
import com.tridium.px.editor.studio.trackers.HandleTracker;
import com.tridium.px.editor.studio.trackers.PathHandleTracker;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BPathGeom;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.IPathGeom;
import javax.baja.gx.PathGeom;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.gx.IPathGeom.MoveTo;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.BWidget;
import javax.baja.ui.shape.BPath;

public class PathArtisan extends Artisan {
   public SegmentArtisan artisan = new Router();

   @Override
   public void paintSelected(TrackerStudio studio, BWidget widget, Graphics g) {
      BPath path = (BPath)widget;
      BPathGeom geom = path.getGeom();
      Point pen = this.newPen(geom);

      for (int i = 0; i < geom.size(); i++) {
         pen = this.artisan.paintBars(g, studio, path, pen, geom.segment(i));
      }

      pen = this.newPen(geom);

      for (int i = 0; i < geom.size(); i++) {
         pen = this.artisan.paintHandles(g, studio, path, pen, geom.segment(i));
      }
   }

   @Override
   public RectGeom bounds(BWidget widget) {
      BPath path = (BPath)widget;
      BPathGeom geom = path.getGeom();
      Point[] pnts = AwtUtil.flatten(geom, 3.0);
      RectGeom b = new RectGeom(pnts[0].x, pnts[0].y, 0.0, 0.0);

      for (int i = 1; i < pnts.length; i++) {
         b = RectGeom.bounds(b.x, b.y, b.width, b.height, pnts[i].x, pnts[i].y, 0.0, 0.0, b);
      }

      return b;
   }

   @Override
   public String move(BWidget widget, double dx, double dy) {
      BPath path = (BPath)widget;
      BPathGeom geom = path.getGeom();
      Segment[] segs = geom.segments();
      Point pen = this.newPen(geom);

      for (int i = 0; i < segs.length; i++) {
         segs[i] = this.artisan.move(pen, segs[i], dx, dy);
         pen = this.artisan.point(pen, segs[i]);
      }

      path.setGeom(BPathGeom.make(segs));
      return "geom";
   }

   @Override
   public String zero(BWidget widget) {
      RectGeom bounds = this.bounds(widget);
      return this.move(widget, -bounds.x, -bounds.y);
   }

   @Override
   public IGeom translateGeom(BWidget widget, double dx, double dy) {
      BPath path = (BPath)widget;
      BPathGeom geom = path.getGeom();
      Segment[] segs = geom.segments();
      Point pen = this.newPen(geom);

      for (int i = 0; i < segs.length; i++) {
         segs[i] = this.artisan.move(pen, segs[i], dx, dy);
         pen = this.artisan.point(pen, segs[i]);
      }

      return new PathGeom(segs, segs.length);
   }

   @Override
   public void addHandles(TrackerStudio studio, BWidget widget, PointMap map) {
      BPath path = (BPath)widget;
      BPathGeom geom = path.getGeom();
      Segment[] segs = geom.segments();
      Point pen = this.newPen(geom);

      for (int i = 0; i < segs.length; i++) {
         pen = this.artisan.addHandles(studio, path, pen, segs[i], i, map);
      }
   }

   @Override
   public HandleTracker makeHandleTracker(BPxEditorPane editorPane, Handle handle, boolean preserveAspectRatio) {
      return new PathHandleTracker(editorPane, handle, this);
   }

   @Override
   public String setGeom(BWidget widget, IGeom geom) {
      BPath path = (BPath)widget;
      IPathGeom pg = (IPathGeom)geom;
      path.setGeom(BPathGeom.make(pg.segments()));
      return "geom";
   }

   private Point newPen(BPathGeom geom) {
      int len = geom.size();
      if (len < 2) {
         throw new IllegalStateException();
      } else {
         Segment s = geom.segment(0);
         if (!(s instanceof MoveTo)) {
            throw new IllegalStateException();
         } else if (!s.isAbsolute()) {
            throw new IllegalStateException();
         } else {
            MoveTo mv = (MoveTo)s;
            return new Point(mv.getX(), mv.getY());
         }
      }
   }
}
