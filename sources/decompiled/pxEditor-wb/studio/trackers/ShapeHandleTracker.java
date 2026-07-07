package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.studio.commands.MorphWidget;
import com.tridium.px.editor.studio.painters.DefaultPainter;
import com.tridium.px.editor.studio.painters.GeomPainter;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.Point;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.shape.BShape;

public abstract class ShapeHandleTracker extends HandleTracker implements GeomSupplier {
   boolean dragged = false;
   private Artisan artisan;

   public ShapeHandleTracker(BPxEditorPane editorPane, Handle handle, Artisan artisan) {
      super(editorPane, handle);
      this.artisan = artisan;
   }

   @Override
   public Tracker mouseDragged(BMouseEvent event) {
      this.setDelta(event.getX(), event.getY());
      if (!this.dragged && this.moved(event)) {
         this.dragged = true;
         this.studio
            .setPainter(
               new GeomPainter(
                  this.editorPane.getPainterStudio(), this, this.canvas.getScaleTransform(), this.studio.translateToRoot(this.canvas, new Point(0.0, 0.0))
               )
            );
      }

      this.editorPane.repaint();
      return this;
   }

   @Override
   public Tracker mouseReleased(BMouseEvent event) {
      if (this.dragged) {
         this.dragged = false;
         this.setDelta(event.getX(), event.getY());
         new MorphWidget(this.editorPane, this.handle.widget, ((BShape)this.handle.widget).getShapeGeom(), this.geoms()[0], this.artisan).invoke();
         this.studio.setPainter(new DefaultPainter(this.editorPane.getPainterStudio()));
         this.editorPane.repaint();
      }

      return new UnpressedTracker(this.editorPane);
   }
}
