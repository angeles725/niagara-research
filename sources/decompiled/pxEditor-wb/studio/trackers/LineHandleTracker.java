package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.LineArtisan;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BLineGeom;
import javax.baja.gx.BSize;
import javax.baja.gx.Geom;
import javax.baja.gx.LineGeom;
import javax.baja.gx.Point;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.shape.BLine;

public class LineHandleTracker extends ShapeHandleTracker implements GeomSupplier {
   private Point here;
   private Point there;

   public LineHandleTracker(BPxEditorPane editorPane, Handle handle, LineArtisan artisan) {
      super(editorPane, handle, artisan);
      BLine line = (BLine)handle.widget;
      BLineGeom geom = line.getGeom();
      switch ((Integer)handle.role) {
         case 1:
            this.here = new Point(geom.x1, geom.y1);
            this.there = new Point(geom.x2, geom.y2);
            break;
         case 2:
            this.there = new Point(geom.x1, geom.y1);
            this.here = new Point(geom.x2, geom.y2);
            break;
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public Geom[] geoms() {
      double x = this.here.x + this.delta.x;
      double y = this.here.y + this.delta.y;
      BCanvasPane canvas = (BCanvasPane)this.handle.widget.getParent();
      BSize viewSize = canvas.getViewSize();
      if (x < 0.0) {
         x = 0.0;
      } else if (x > viewSize.width) {
         x = viewSize.width;
      }

      if (y < 0.0) {
         y = 0.0;
      } else if (y > viewSize.height) {
         y = viewSize.height;
      }

      return new Geom[]{new LineGeom(x, y, this.there.x, this.there.y)};
   }
}
