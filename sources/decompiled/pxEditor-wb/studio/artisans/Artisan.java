package com.tridium.px.editor.studio.artisans;

import com.tridium.gx.util.PointMap;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.trackers.HandleTracker;
import com.tridium.px.editor.util.Handle;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.MouseCursor;

public abstract class Artisan {
   private static Artisan router = new ArtisanRouter();
   private static final int LARGE = 3;
   private static final int SMALL = 2;
   private static int handleSize = 3;

   public abstract RectGeom bounds(BWidget var1);

   public abstract String setGeom(BWidget var1, IGeom var2);

   public abstract void paintSelected(TrackerStudio var1, BWidget var2, Graphics var3);

   public abstract String move(BWidget var1, double var2, double var4);

   public abstract String zero(BWidget var1);

   public abstract IGeom translateGeom(BWidget var1, double var2, double var4);

   public abstract void addHandles(TrackerStudio var1, BWidget var2, PointMap var3);

   public abstract HandleTracker makeHandleTracker(BPxEditorPane var1, Handle var2, boolean var3);

   public RectGeom rootBounds(TrackerStudio studio, BWidget widget) {
      RectGeom geom = this.bounds(widget);
      double x = geom.x;
      double y = geom.y;
      double w = geom.width;
      double h = geom.height;
      Point a = studio.translateToRoot(widget, new Point(x, y));
      Point b = studio.translateToRoot(widget, new Point(x + w, y + h));
      return new RectGeom(a.x, a.y, b.x - a.x, b.y - a.y);
   }

   public static void paintHandle(Graphics g, BColor color, double x, double y) {
      int n = handleSize * 2 + 1;
      g.setBrush(color);
      g.fillRect(x - handleSize, y - handleSize, n, n);
      if (handleSize == 3) {
         g.setBrush(BColor.black);
         g.strokeRect(x - handleSize, y - handleSize, n, n);
      }
   }

   public static void addHandle(double x, double y, PointMap map, BWidget widget, MouseCursor cursor, Object context) {
      map.put(x, y, new Handle(new Point(x, y), widget, cursor, context));
   }

   public static RectGeom boundsUnion(Artisan a, BWidget[] w) {
      if (w.length == 0) {
         return null;
      } else {
         RectGeom r = new RectGeom(a.bounds(w[0]));

         for (int i = 1; i < w.length; i++) {
            r = RectGeom.bounds(r, a.bounds(w[i]), r);
         }

         return r;
      }
   }

   public static Artisan instance() {
      return router;
   }

   public static void largeHandles() {
      handleSize = 3;
   }

   public static void smallHandles() {
      handleSize = 2;
   }
}
