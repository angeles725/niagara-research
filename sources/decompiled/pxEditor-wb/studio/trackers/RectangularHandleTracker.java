package com.tridium.px.editor.studio.trackers;

import com.tridium.gx.util.GeomUtil;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.ArtisanRouter;
import com.tridium.px.editor.studio.artisans.RectangularArtisan;
import com.tridium.px.editor.studio.commands.MorphWidget;
import com.tridium.px.editor.studio.painters.DefaultPainter;
import com.tridium.px.editor.studio.painters.GeomPainter;
import com.tridium.px.editor.util.Handle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.baja.gx.Geom;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.ui.Command;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.commands.CompoundCommand;
import javax.baja.ui.event.BMouseEvent;

public class RectangularHandleTracker extends HandleTracker implements GeomSupplier {
   private boolean dragged = false;
   private RectangularArtisan artisan;
   private boolean preserveAspectRatio;
   private RectangularHandleTracker[] friendly;
   private Geom[] geoms;

   public static RectangularHandleTracker make(BPxEditorPane editorPane, Handle handle, RectangularArtisan artisan, boolean preserveAspectRatio) {
      return new RectangularHandleTracker(editorPane, handle, artisan, true, preserveAspectRatio);
   }

   private RectangularHandleTracker(BPxEditorPane editorPane, Handle handle, RectangularArtisan artisan, boolean loadFriendly, boolean preserveAspectRatio) {
      super(editorPane, handle);
      this.artisan = artisan;
      this.preserveAspectRatio = preserveAspectRatio;
      if (loadFriendly) {
         Handle[] matching = editorPane.getSelectedWidgets().getHandlesHavingRole(handle.role);
         List<Handle> a = new ArrayList<>(Arrays.asList(matching));
         a.remove(handle);
         matching = a.toArray(new Handle[0]);
         this.friendly = new RectangularHandleTracker[matching.length];

         for (int i = 0; i < matching.length; i++) {
            this.friendly[i] = new RectangularHandleTracker(
               editorPane, matching[i], (RectangularArtisan)ArtisanRouter.artisan(matching[i].widget), false, preserveAspectRatio
            );
         }

         this.geoms = new Geom[this.friendly.length + 1];
      } else {
         this.geoms = new Geom[1];
         this.friendly = new RectangularHandleTracker[0];
      }
   }

   @Override
   public Geom[] geoms() {
      this.geoms[0] = this.geom();

      for (int i = 0; i < this.friendly.length; i++) {
         this.geoms[i + 1] = this.friendly[i].geom();
      }

      return this.geoms;
   }

   private Geom geom() {
      RectGeom rect = this.artisan.bounds(this.handle.widget);
      this.applyDelta(rect);
      double x0 = rect.x;
      double y0 = rect.y;
      double x1 = rect.x + rect.width;
      double y1 = rect.y + rect.height;
      if (this.editorPane.getOptions().getUseSnap()) {
         x0 = Math.round(x0);
         y0 = Math.round(y0);
         x1 = Math.round(x1);
         y1 = Math.round(y1);
      }

      return GeomUtil.makeRectangle(x0, y0, x1, y1);
   }

   private void applyDelta(RectGeom rect) {
      double rw = rect.width;
      double rh = rect.height;
      if (this.handle.cursor == MouseCursor.eResize) {
         rect.width = rect.width + this.delta.x;
         if (this.preserveAspectRatio && rw != 0.0) {
            rect.height = rect.height * (rect.width / rw);
         }
      } else if (this.handle.cursor == MouseCursor.wResize) {
         rect.width = rect.width - this.delta.x;
         rect.x = rect.x + this.delta.x;
         if (this.preserveAspectRatio && rw != 0.0) {
            rect.height = rect.height * (rect.width / rw);
         }
      } else if (this.handle.cursor == MouseCursor.sResize) {
         rect.height = rect.height + this.delta.y;
         if (this.preserveAspectRatio && rh != 0.0) {
            rect.width = rect.width * (rect.height / rh);
         }
      } else if (this.handle.cursor == MouseCursor.nResize) {
         rect.height = rect.height - this.delta.y;
         rect.y = rect.y + this.delta.y;
         if (this.preserveAspectRatio && rh != 0.0) {
            rect.width = rect.width * (rect.height / rh);
         }
      } else if (this.handle.cursor == MouseCursor.nwResize) {
         rect.height = rect.height - this.delta.y;
         rect.width = rect.width - this.delta.x;
         this.applyAspectToCorner(rect, rw, rh);
         rect.y = rect.y + (rh - rect.height);
         rect.x = rect.x + (rw - rect.width);
      } else if (this.handle.cursor == MouseCursor.neResize) {
         rect.height = rect.height - this.delta.y;
         rect.width = rect.width + this.delta.x;
         this.applyAspectToCorner(rect, rw, rh);
         rect.y = rect.y + (rh - rect.height);
      } else if (this.handle.cursor == MouseCursor.swResize) {
         rect.height = rect.height + this.delta.y;
         rect.width = rect.width - this.delta.x;
         this.applyAspectToCorner(rect, rw, rh);
         rect.x = rect.x + (rw - rect.width);
      } else if (this.handle.cursor == MouseCursor.seResize) {
         rect.height = rect.height + this.delta.y;
         rect.width = rect.width + this.delta.x;
         this.applyAspectToCorner(rect, rw, rh);
      }
   }

   private void applyAspectToCorner(RectGeom rect, double rw, double rh) {
      if (this.preserveAspectRatio && rh != 0.0 && rect.height != 0.0) {
         double oldAspect = rw / rh;
         double newAspect = rect.width / rect.height;
         if (oldAspect > newAspect) {
            if (rw != 0.0) {
               rect.height = rh * rect.width / rw;
            }
         } else if (oldAspect < newAspect) {
            rect.width = rw * rect.height / rh;
         }
      }
   }

   @Override
   public Tracker mouseDragged(BMouseEvent event) {
      this.setAllDeltas(event);
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
         this.setAllDeltas(event);
         if (this.friendly.length == 0) {
            this.morphWidget().invoke();
         } else {
            Command[] cmds = new Command[this.friendly.length + 1];
            cmds[0] = this.morphWidget();

            for (int i = 0; i < this.friendly.length; i++) {
               cmds[i + 1] = this.friendly[i].morphWidget();
            }

            new CompoundCommand(this.editorPane, "morph", cmds).invoke();
         }

         this.studio.setPainter(new DefaultPainter(this.editorPane.getPainterStudio()));
         this.editorPane.repaint();
      }

      return new UnpressedTracker(this.editorPane);
   }

   private void setAllDeltas(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      this.setDelta(x, y);

      for (int i = 0; i < this.friendly.length; i++) {
         this.friendly[i].setDelta(x + (this.friendly[i].anchor.x - this.anchor.x), y + (this.friendly[i].anchor.y - this.anchor.y));
      }
   }

   private MorphWidget morphWidget() {
      return new MorphWidget(this.editorPane, this.handle.widget, this.artisan.bounds(this.handle.widget), this.geoms()[0], this.artisan);
   }
}
