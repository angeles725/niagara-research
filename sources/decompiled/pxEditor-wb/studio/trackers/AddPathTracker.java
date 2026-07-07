package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.Geom;
import javax.baja.gx.PathGeom;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.CurveTo;
import javax.baja.gx.IPathGeom.LineTo;
import javax.baja.gx.IPathGeom.MoveTo;
import javax.baja.gx.IPathGeom.QuadTo;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.gx.IPathGeom.SmoothCurveTo;
import javax.baja.gx.IPathGeom.SmoothQuadTo;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.shape.BPath;

public class AddPathTracker extends AddGeometryTracker {
   private boolean dragged = false;

   public AddPathTracker(BPxEditorPane editorPane, TrackerStudio studio) {
      super(editorPane, studio);
   }

   @Override
   protected boolean widgetMakeable() {
      return this.handles.size() >= 2;
   }

   @Override
   protected BWidget makeWidget() {
      BPath path = new BPath(this.pathGeom(false));
      path.setStroke(BBrush.makeSolid(BColor.black));
      return path;
   }

   @Override
   public Tracker mouseDragged(BMouseEvent event) {
      if (!this.dragged && this.moved(event)) {
         this.dragged = true;
      }

      if (this.dragged) {
         Point pnt = this.studio.toViewbox(event.getX(), event.getY(), this.canvas);
         pnt = this.studio.snap(pnt.x, pnt.y);
         this.bars.set(this.bars.size() - 1, pnt);
      }

      this.editorPane.repaint();
      return this;
   }

   @Override
   public Tracker mouseReleased(BMouseEvent event) {
      if (this.dragged) {
         Point pnt = this.studio.toViewbox(event.getX(), event.getY(), this.canvas);
         pnt = this.studio.snap(pnt.x, pnt.y);
         this.bars.set(this.bars.size() - 1, pnt);
         this.dragged = false;
      }

      this.editorPane.repaint();
      return this;
   }

   @Override
   public Geom[] geoms() {
      return this.handles.size() == 0 ? new Geom[0] : new Geom[]{this.pathGeom(true)};
   }

   private PathGeom pathGeom(boolean useMouse) {
      int len = this.handles.size();
      if (len == 0) {
         return null;
      } else {
         int len2 = useMouse ? len + 1 : len;
         Point[] h = new Point[len2];
         Point[] a = new Point[len2];
         Point[] b = new Point[len2];

         for (int i = 0; i < len; i++) {
            Point p = this.handles.get(i);
            h[i] = new Point(p.x, p.y);
            p = this.bars.get(i);
            if (p != null) {
               a[i] = new Point(p.x, p.y);
               double dx = a[i].x - h[i].x;
               double dy = a[i].y - h[i].y;
               b[i] = new Point(h[i].x - dx, h[i].y - dy);
            }
         }

         if (useMouse) {
            h[len] = new Point(this.mouse.x, this.mouse.y);
         }

         Segment[] segs = new Segment[len2];
         segs[0] = new MoveTo(true, h[0].x, h[0].y);
         if (a[0] == null) {
            if (a[1] == null) {
               segs[1] = new LineTo(true, h[1].x, h[1].y);
            } else {
               segs[1] = new QuadTo(true, b[1].x, b[1].y, h[1].x, h[1].y);
            }
         } else if (a[1] == null) {
            segs[1] = new QuadTo(true, a[0].x, a[0].y, h[1].x, h[1].y);
         } else {
            segs[1] = new CurveTo(true, a[0].x, a[0].y, b[1].x, b[1].y, h[1].x, h[1].y);
         }

         for (int ix = 2; ix < len2; ix++) {
            if (a[ix - 1] == null) {
               if (a[ix] == null) {
                  segs[ix] = new LineTo(true, h[ix].x, h[ix].y);
               } else {
                  segs[ix] = new QuadTo(true, b[ix].x, b[ix].y, h[ix].x, h[ix].y);
               }
            } else if (a[ix] == null) {
               segs[ix] = new SmoothQuadTo(true, h[ix].x, h[ix].y);
            } else {
               segs[ix] = new SmoothCurveTo(true, b[ix].x, b[ix].y, h[ix].x, h[ix].y);
            }
         }

         return new PathGeom(segs, len2);
      }
   }
}
