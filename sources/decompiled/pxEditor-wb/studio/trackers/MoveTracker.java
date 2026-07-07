package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.studio.commands.MoveWidget;
import com.tridium.px.editor.studio.painters.DefaultPainter;
import com.tridium.px.editor.studio.painters.MovePainter;
import javax.baja.gx.BSize;
import javax.baja.gx.IGeom;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BCanvasPane;

public class MoveTracker extends Tracker {
   private TrackerStudio studio;
   private boolean dragged = false;
   private BCanvasPane canvas;
   private BWidget[] widgets;
   private BSize viewBox;
   private Point anchor;
   private RectGeom origBounds;
   private RectGeom moveRect;

   public MoveTracker(BPxEditorPane editorPane, BCanvasPane canvas, BWidget[] widgets) {
      super(editorPane);
      this.studio = editorPane.getTrackerStudio();
      this.studio.setMouseCursor(MouseCursor.move);
      this.canvas = canvas;
      this.viewBox = canvas.getViewSize();
      this.widgets = widgets;
      Artisan a = Artisan.instance();
      this.origBounds = Artisan.boundsUnion(a, widgets);
      this.moveRect = new RectGeom(this.origBounds);
      this.anchor = this.studio.toViewbox(getAnchorX(), getAnchorY(), canvas);
   }

   public IGeom[] geoms() {
      double dx = this.moveRect.x - this.origBounds.x;
      double dy = this.moveRect.y - this.origBounds.y;
      Artisan a = Artisan.instance();
      IGeom[] geoms = new IGeom[this.widgets.length];

      for (int i = 0; i < this.widgets.length; i++) {
         geoms[i] = a.translateGeom(this.widgets[i], dx, dy);
      }

      return geoms;
   }

   void setDelta(BMouseEvent event) {
      Point e = this.studio.toViewbox(event.getX(), event.getY(), this.canvas);
      double dX = e.x - this.anchor.x;
      double dY = e.y - this.anchor.y;
      this.moveRect = new RectGeom(this.origBounds);
      this.moveRect.x += dX;
      this.moveRect.y += dY;
      if (this.moveRect.x < 0.0) {
         this.moveRect.x = 0.0;
      } else if (this.moveRect.x + this.moveRect.width > this.viewBox.width) {
         this.moveRect.x = this.viewBox.width - this.moveRect.width;
      }

      if (this.moveRect.y < 0.0) {
         this.moveRect.y = 0.0;
      } else if (this.moveRect.y + this.moveRect.height > this.viewBox.height) {
         this.moveRect.y = this.viewBox.height - this.moveRect.height;
      }

      Point pnt = this.studio.snap(this.moveRect.x, this.moveRect.y);
      this.moveRect.x = pnt.x;
      this.moveRect.y = pnt.y;
   }

   @Override
   public Tracker mouseDragged(BMouseEvent event) {
      if (!this.dragged && this.moved(event)) {
         this.dragged = true;
         this.studio
            .setPainter(
               new MovePainter(
                  this.editorPane.getPainterStudio(), this, this.canvas.getScaleTransform(), this.studio.translateToRoot(this.canvas, new Point(0.0, 0.0))
               )
            );
      }

      this.setDelta(event);
      this.editorPane.repaint();
      return this;
   }

   @Override
   public Tracker mouseReleased(BMouseEvent event) {
      if (this.dragged) {
         this.dragged = false;
         this.setDelta(event);
         new MoveWidget(this.editorPane, this.widgets, this.moveRect.x - this.origBounds.x, this.moveRect.y - this.origBounds.y).invoke();
         this.studio.setPainter(new DefaultPainter(this.editorPane.getPainterStudio()));
         this.editorPane.repaint();
      }

      return new UnpressedTracker(this.editorPane);
   }
}
