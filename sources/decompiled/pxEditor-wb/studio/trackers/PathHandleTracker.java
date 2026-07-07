package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.PathArtisan;
import com.tridium.px.editor.studio.artisans.path.Role;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BPathGeom;
import javax.baja.gx.BSize;
import javax.baja.gx.Geom;
import javax.baja.gx.PathGeom;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.shape.BPath;

public class PathHandleTracker extends ShapeHandleTracker implements GeomSupplier {
   private Segment[] segments;
   private Role role;

   public PathHandleTracker(BPxEditorPane editorPane, Handle handle, PathArtisan artisan) {
      super(editorPane, handle, artisan);
      BPath path = (BPath)handle.widget;
      BPathGeom geom = path.getGeom();
      this.segments = geom.segments();
      this.role = (Role)handle.role;
   }

   @Override
   public Geom[] geoms() {
      double x = this.role.orig.x + this.delta.x;
      double y = this.role.orig.y + this.delta.y;
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

      this.segments[this.role.idx] = this.role.apply(this.delta.x, this.delta.y);
      return new Geom[]{new PathGeom(this.segments, this.segments.length)};
   }
}
