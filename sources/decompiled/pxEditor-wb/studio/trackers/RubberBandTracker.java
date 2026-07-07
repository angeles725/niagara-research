package com.tridium.px.editor.studio.trackers;

import com.tridium.gx.util.GeomUtil;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.painters.DefaultPainter;
import com.tridium.px.editor.studio.painters.GeomPainter;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.gx.Geom;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BCanvasPane;

public class RubberBandTracker extends Tracker implements GeomSupplier {
   private SelectedWidgets selected;
   private TrackerStudio studio;
   private BCanvasPane canvas;
   private boolean wasShiftDown;
   private RectGeom[] rubberBand = new RectGeom[1];
   private boolean dragged = false;
   private Point anchor;

   public RubberBandTracker(BPxEditorPane editorPane, BCanvasPane canvas, boolean wasShiftDown) {
      super(editorPane);
      this.studio = editorPane.getTrackerStudio();
      this.canvas = canvas;
      this.wasShiftDown = wasShiftDown;
      this.selected = editorPane.getSelectedWidgets();
      this.anchor = this.studio.toViewbox(getAnchorX(), getAnchorY(), canvas);
   }

   @Override
   public Tracker mouseDragged(BMouseEvent event) {
      if (!this.dragged && this.moved(event)) {
         this.dragged = true;
         if (!this.wasShiftDown) {
            this.selected.deselectAll();
         }

         this.studio
            .setPainter(
               new GeomPainter(
                  this.editorPane.getPainterStudio(), this, this.canvas.getScaleTransform(), this.studio.translateToRoot(this.canvas, new Point(0.0, 0.0))
               )
            );
      }

      this.makeRubberBand(event);
      this.editorPane.repaint();
      return this;
   }

   @Override
   public Tracker mouseReleased(BMouseEvent event) {
      if (this.dragged) {
         this.dragged = false;
         this.makeRubberBand(event);
         this.studio.selectWidgets(this.rubberBand[0], this.canvas);
         this.studio.setPainter(new DefaultPainter(this.editorPane.getPainterStudio()));
      } else {
         boolean paneSel = this.selected.isSelected(this.canvas);
         this.selected.deselectAll();
         if (!paneSel) {
            this.selected.select(this.canvas);
         }
      }

      this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      return new UnpressedTracker(this.editorPane);
   }

   @Override
   public Tracker keyPressed(BKeyEvent event) {
      this.studio.setShiftDown(event.isShiftDown());
      if (event.getKeyCode() == 27) {
         this.studio.setPainter(new DefaultPainter(this.editorPane.getPainterStudio()));
         return new UnpressedTracker(this.editorPane);
      } else {
         return this;
      }
   }

   private void makeRubberBand(BMouseEvent event) {
      Point e = this.studio.toViewbox(event.getX(), event.getY(), this.canvas);
      this.rubberBand[0] = GeomUtil.makeRectangle(this.anchor.x, this.anchor.y, e.x, e.y);
   }

   @Override
   public Geom[] geoms() {
      return this.rubberBand;
   }
}
