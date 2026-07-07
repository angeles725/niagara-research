package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.commands.InsertDynamic;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.painters.AddGeometryPainter;
import com.tridium.px.editor.util.Reflector;
import java.util.ArrayList;
import java.util.List;
import javax.baja.gx.BTransform;
import javax.baja.gx.Point;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BCanvasPane;

public abstract class AddGeometryTracker extends GeometryTracker implements GeomSupplier {
   protected TrackerStudio studio;
   private AddGeometryPainter painter;
   protected BCanvasPane canvas;
   protected List<Point> handles = new ArrayList<>();
   protected List<Point> bars = new ArrayList<>();
   protected Point mouse;
   public BTransform scaleTransform;
   public Point pageOffset;

   public AddGeometryTracker(BPxEditorPane editorPane, TrackerStudio studio) {
      super(editorPane);
      this.studio = studio;
      editorPane.getSelectedWidgets().deselectAll();
      this.painter = new AddGeometryPainter(editorPane.getPainterStudio(), this);
      studio.setPainter(this.painter);
      this.editor.firePxEvent(new PxSelectionEvent(editorPane.getSelectedWidgets().getWidgets()));
   }

   protected abstract boolean widgetMakeable();

   protected abstract BWidget makeWidget();

   @Override
   public Tracker mouseMoved(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      if (this.canvas != null) {
         this.mouse = this.studio.toViewbox(x, y, this.canvas);
         this.mouse = this.studio.snap(this.mouse.x, this.mouse.y);
         this.editorPane.repaint();
      }

      return this;
   }

   @Override
   public Tracker mousePressed(BMouseEvent event) {
      if (event.getClickCount() == 1 && event.isButton1Down()) {
         double x = event.getX();
         double y = event.getY();
         BWidget widget = this.studio.rootDescendant(new Point(x, y));
         BCanvasPane c = Reflector.canvas(widget);
         if (c != null) {
            if (c != this.canvas) {
               this.handles.clear();
               this.bars.clear();
               this.canvas = c;
            }

            this.mouse = this.studio.toViewbox(x, y, c);
            this.mouse = this.studio.snap(this.mouse.x, this.mouse.y);
            this.handles.add(this.mouse);
            this.bars.add(null);
            this.scaleTransform = c.getScaleTransform();
            this.pageOffset = this.studio.translateToRoot(c, new Point(0.0, 0.0));
            this.editorPane.repaint();
         }

         return this;
      } else {
         return this;
      }
   }

   @Override
   public Tracker keyTyped(BKeyEvent event) {
      if (event.getKeyChar() == 27) {
         if (this.widgetMakeable()) {
            BWidget[] widgets = new BWidget[]{this.makeWidget()};

            try {
               InsertDynamic ins = new InsertDynamic(this.editor, this.editorPane, this.canvas, widgets);
               if (ins.checkMedia()) {
                  ins.doInvoke();
               }
            } catch (Exception var4) {
               throw new BajaRuntimeException(var4);
            }

            this.editorPane.getSelectedWidgets().deselectAll();
            this.painter.reset();
         }

         this.handles.clear();
         this.bars.clear();
         this.editorPane.repaint();
      }

      return this;
   }

   public Point[] handles() {
      return this.convert(this.handles.toArray(new Point[0]));
   }

   public Point[] bars() {
      return this.convert(this.bars.toArray(new Point[0]));
   }

   private Point[] convert(Point[] p) {
      Point[] q = new Point[p.length];

      for (int i = 0; i < p.length; i++) {
         if (p[i] != null) {
            q[i] = this.studio.fromViewbox(p[i].x, p[i].y, this.canvas);
         }
      }

      return q;
   }
}
