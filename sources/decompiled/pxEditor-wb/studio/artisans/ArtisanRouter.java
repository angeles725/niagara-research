package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.trackers.HandleTracker;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.shape.BEllipse;
import javax.baja.ui.shape.BLine;
import javax.baja.ui.shape.BPath;
import javax.baja.ui.shape.BPolygon;
import javax.baja.ui.shape.BRect;

public class ArtisanRouter extends Artisan {
   static Artisan wid = new WidgetArtisan();
   static Artisan ellipse = new EllipseArtisan();
   static Artisan rect = new RectArtisan();
   static Artisan poly = new PolygonArtisan();
   static Artisan line = new LineArtisan();
   static Artisan path = new PathArtisan();

   @Override
   public void paintSelected(TrackerStudio studio, BWidget widget, Graphics g) {
      artisan(widget).paintSelected(studio, widget, g);
   }

   @Override
   public RectGeom bounds(BWidget widget) {
      return artisan(widget).bounds(widget);
   }

   @Override
   public RectGeom rootBounds(TrackerStudio studio, BWidget widget) {
      return artisan(widget).rootBounds(studio, widget);
   }

   @Override
   public String move(BWidget widget, double dx, double dy) {
      return artisan(widget).move(widget, dx, dy);
   }

   @Override
   public String zero(BWidget widget) {
      return artisan(widget).zero(widget);
   }

   @Override
   public IGeom translateGeom(BWidget widget, double dx, double dy) {
      return artisan(widget).translateGeom(widget, dx, dy);
   }

   @Override
   public void addHandles(TrackerStudio studio, BWidget widget, PointMap map) {
      artisan(widget).addHandles(studio, widget, map);
   }

   @Override
   public HandleTracker makeHandleTracker(BPxEditorPane editorPane, Handle handle, boolean preserveAspectRatio) {
      return artisan(handle.widget).makeHandleTracker(editorPane, handle, preserveAspectRatio);
   }

   @Override
   public String setGeom(BWidget widget, IGeom geom) {
      return artisan(widget).setGeom(widget, geom);
   }

   public static Artisan artisan(BWidget widget) {
      if (widget.getType().is(BEllipse.TYPE)) {
         return ellipse;
      } else if (widget.getType().is(BRect.TYPE)) {
         return rect;
      } else if (widget.getType().is(BPolygon.TYPE)) {
         return poly;
      } else if (widget.getType().is(BLine.TYPE)) {
         return line;
      } else {
         return widget.getType().is(BPath.TYPE) ? path : wid;
      }
   }
}
