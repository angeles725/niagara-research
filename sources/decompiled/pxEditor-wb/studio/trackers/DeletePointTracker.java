package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.artisans.PathArtisan;
import com.tridium.px.editor.studio.artisans.PolygonArtisan;
import com.tridium.px.editor.studio.artisans.path.Role;
import com.tridium.px.editor.studio.commands.MorphWidget;
import com.tridium.px.editor.studio.painters.PointPainter;
import com.tridium.px.editor.util.Handle;
import com.tridium.px.editor.util.SelectedWidgets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.baja.gx.BPathGeom;
import javax.baja.gx.BPolygonGeom;
import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.MoveTo;
import javax.baja.gx.IPathGeom.Segment;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.shape.BPath;
import javax.baja.ui.shape.BPolygon;

public class DeletePointTracker extends GeometryTracker {
   private SelectedWidgets selected;
   private TrackerStudio studio;

   public DeletePointTracker(BPxEditorPane editorPane, TrackerStudio studio) {
      super(editorPane);
      this.studio = studio;
      this.selected = editorPane.getSelectedWidgets();
      this.selected.deselectAll();
      studio.setPainter(new PointPainter(editorPane.getPainterStudio(), this.selected));
      this.selectShapes(this.selected, editorPane.getRootContainer());
      editorPane.repaint();
   }

   @Override
   public Tracker mousePressed(BMouseEvent event) {
      if (event.getClickCount() == 1 && event.isButton1Down()) {
         double x = event.getX();
         double y = event.getY();
         Handle handle = this.selected.getHandle(x, y);
         if (handle != null) {
            if (!(handle.widget instanceof BPath) && !(handle.widget instanceof BPolygon)) {
               throw new IllegalStateException();
            }

            this.deletePoint(handle);
         }

         this.editorPane.repaint();
         return this;
      } else {
         return this;
      }
   }

   private void deletePoint(Handle handle) {
      if (handle.widget instanceof BPath) {
         PathArtisan pa = new PathArtisan();
         BPath path = (BPath)handle.widget;
         BPathGeom geom = path.getGeom();
         if (geom.size() <= 2) {
            return;
         }

         List<Segment> arr = new ArrayList<>(Arrays.asList(geom.segments()));
         Role role = (Role)handle.role;
         arr.remove(role.idx);
         Segment[] segs = arr.toArray(new Segment[0]);
         if (role.idx == 0) {
            Point p = pa.artisan.point(null, segs[0]);
            segs[0] = new MoveTo(true, p.x, p.y);
         }

         geom = BPathGeom.make(segs);
         new MorphWidget(this.editorPane, path, path.getGeom(), geom, pa).invoke();
      } else {
         if (!(handle.widget instanceof BPolygon)) {
            throw new IllegalStateException();
         }

         BPolygon poly = (BPolygon)handle.widget;
         BPolygonGeom geomx = poly.getGeom();
         if (geomx.size() <= 3) {
            return;
         }

         double[] x = geomx.x();
         double[] y = geomx.y();
         int idx = (Integer)handle.role;

         for (int i = idx; i < geomx.size() - 1; i++) {
            x[i] = x[i + 1];
            y[i] = y[i + 1];
         }

         geomx = BPolygonGeom.make(x, y, geomx.size() - 1);
         new MorphWidget(this.editorPane, poly, poly.getGeom(), geomx, new PolygonArtisan()).invoke();
      }
   }
}
