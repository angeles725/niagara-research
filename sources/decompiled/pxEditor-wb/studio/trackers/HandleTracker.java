package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.Point;
import javax.baja.ui.pane.BCanvasPane;

public abstract class HandleTracker extends Tracker {
   protected TrackerStudio studio;
   protected Handle handle;
   protected BCanvasPane canvas = null;
   protected Point anchor;
   protected Point delta = new Point(0.0, 0.0);

   public HandleTracker(BPxEditorPane editorPane, Handle handle) {
      super(editorPane);
      this.studio = editorPane.getTrackerStudio();
      this.handle = handle;
      this.canvas = (BCanvasPane)handle.widget.getParentWidget();
      this.anchor = this.studio.toViewbox(handle.pnt.x, handle.pnt.y, this.canvas);
   }

   protected void setDelta(double ex, double ey) {
      Point e = this.studio.toViewbox(ex, ey, this.canvas);
      Point pnt = this.studio.snap(e.x, e.y);
      this.delta = new Point(pnt.x - this.anchor.x, pnt.y - this.anchor.y);
   }
}
