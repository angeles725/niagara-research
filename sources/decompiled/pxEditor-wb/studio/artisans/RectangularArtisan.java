package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.trackers.HandleTracker;
import com.tridium.px.editor.studio.trackers.RectangularHandleTracker;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BColor;
import javax.baja.gx.BPen;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.MouseCursor;

public abstract class RectangularArtisan extends Artisan {
   private static final MouseCursor DNE = MouseCursor.doNotEnter;

   protected final void paintRect(RectGeom b, Graphics g) {
      double w = b.width;
      double h = b.height;
      g.setPen(BPen.DEFAULT);
      g.setBrush(BColor.lime);
      g.strokeRect(b.x, b.y, w, h);
      paintHandle(g, BColor.lime, b.x, b.y);
      paintHandle(g, BColor.lime, b.x + w, b.y);
      paintHandle(g, BColor.lime, b.x, b.y + h);
      paintHandle(g, BColor.lime, b.x + w, b.y + h);
      paintHandle(g, BColor.lime, b.x, b.y + h / 2.0);
      paintHandle(g, BColor.lime, b.x + w, b.y + h / 2.0);
      paintHandle(g, BColor.lime, b.x + w / 2.0, b.y);
      paintHandle(g, BColor.lime, b.x + w / 2.0, b.y + h);
   }

   protected final void addRectHandles(BWidget widget, PointMap map, RectGeom b, boolean dne) {
      double w = b.width;
      double h = b.height;
      addHandle(b.x, b.y, map, widget, dne ? DNE : MouseCursor.nwResize, dne ? DNE : MouseCursor.nwResize);
      addHandle(b.x + w, b.y, map, widget, dne ? DNE : MouseCursor.neResize, dne ? DNE : MouseCursor.neResize);
      addHandle(b.x, b.y + h, map, widget, dne ? DNE : MouseCursor.swResize, dne ? DNE : MouseCursor.swResize);
      addHandle(b.x + w, b.y + h, map, widget, dne ? DNE : MouseCursor.seResize, dne ? DNE : MouseCursor.seResize);
      addHandle(b.x, b.y + h / 2.0, map, widget, dne ? DNE : MouseCursor.wResize, dne ? DNE : MouseCursor.wResize);
      addHandle(b.x + w, b.y + h / 2.0, map, widget, dne ? DNE : MouseCursor.eResize, dne ? DNE : MouseCursor.eResize);
      addHandle(b.x + w / 2.0, b.y, map, widget, dne ? DNE : MouseCursor.nResize, dne ? DNE : MouseCursor.nResize);
      addHandle(b.x + w / 2.0, b.y + h, map, widget, dne ? DNE : MouseCursor.sResize, dne ? DNE : MouseCursor.sResize);
   }

   @Override
   public IGeom translateGeom(BWidget widget, double dx, double dy) {
      RectGeom b = this.bounds(widget);
      b.x += dx;
      b.y += dy;
      return b;
   }

   @Override
   public HandleTracker makeHandleTracker(BPxEditorPane editorPane, Handle handle, boolean preserveAspectRatio) {
      return RectangularHandleTracker.make(editorPane, handle, this, preserveAspectRatio);
   }
}
