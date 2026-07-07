package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.painters.MovePainter;
import com.tridium.px.editor.util.Reflector;
import javax.baja.gx.Point;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BCanvasPane;

public class HitSelectedTracker extends Tracker {
   private TrackerStudio studio;
   private BWidget widget;

   public HitSelectedTracker(BPxEditorPane editorPane, BWidget widget) {
      super(editorPane);
      this.studio = editorPane.getTrackerStudio();
      this.widget = widget;
   }

   @Override
   public Tracker mouseDragged(BMouseEvent event) {
      if (this.editor.isReadonly()) {
         return this;
      } else if (!this.editorPane.getLayerManager().isNormal(this.widget)) {
         return this;
      } else if (!this.moved(event)) {
         return this;
      } else {
         BWidget parent = this.widget.getParentWidget();
         BCanvasPane canvas = Reflector.canvas(parent);
         if (canvas == null) {
            return this;
         } else {
            MoveTracker mover = UnpressedTracker.makeMoveTracker(this.editorPane, canvas, parent);
            this.studio
               .setPainter(
                  new MovePainter(
                     this.editorPane.getPainterStudio(), mover, canvas.getScaleTransform(), this.studio.translateToRoot(canvas, new Point(0.0, 0.0))
                  )
               );
            this.editorPane.repaint();
            return mover;
         }
      }
   }

   @Override
   public Tracker mouseReleased(BMouseEvent event) {
      BWidget[] stack = this.studio.rootDescendants(new Point(event.getX(), event.getY()));
      if (stack == null) {
         throw new IllegalStateException();
      } else if (stack.length == 0) {
         throw new IllegalStateException();
      } else {
         if (event.isControlDown()) {
            this.editorPane.getSelectedWidgets().deselect(this.widget);
         } else {
            this.editorPane.getSelectedWidgets().deselectAll();
            this.editorPane.getSelectedWidgets().select(this.widget);
         }

         this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
         return new UnpressedTracker(this.editorPane);
      }
   }
}
