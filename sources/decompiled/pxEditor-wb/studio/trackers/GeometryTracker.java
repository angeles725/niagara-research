package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.ui.BWidget;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.shape.BPath;
import javax.baja.ui.shape.BPolygon;

public abstract class GeometryTracker extends Tracker {
   private TrackerStudio studio;
   private MouseCursor oldCursor;

   public GeometryTracker(BPxEditorPane editorPane) {
      super(editorPane);
      this.studio = editorPane.getTrackerStudio();
      this.oldCursor = this.studio.getMouseCursor();
      this.studio.setMouseCursor(MouseCursor.crosshair);
   }

   @Override
   public Tracker mouseEntered(BMouseEvent event) {
      this.studio.setMouseCursor(MouseCursor.crosshair);
      return this;
   }

   @Override
   public Tracker mouseExited(BMouseEvent event) {
      this.studio.setMouseCursor(this.oldCursor);
      return this;
   }

   protected void selectShapes(SelectedWidgets selected, BWidget widget) {
      if (!(widget instanceof BPolygon) && !(widget instanceof BPath)) {
         BWidget[] kids = widget.getChildWidgets();

         for (int i = 0; i < kids.length; i++) {
            this.selectShapes(selected, kids[i]);
         }
      } else {
         selected.select(widget);
      }
   }
}
