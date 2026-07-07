package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.BStudio;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.studio.commands.MoveWidget;
import com.tridium.px.editor.util.Handle;
import com.tridium.px.editor.util.Reflector;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.gx.Point;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.ui.BScrollBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.pane.BTabbedPane;

public class UnpressedTracker extends Tracker {
   private SelectedWidgets selected;
   private TrackerStudio studio;
   private static final int MIN_SNAP_SIZE_TO_ALLOW_MIDPOINT_SNAP = 8;

   public UnpressedTracker(BPxEditorPane editorPane) {
      this(editorPane, editorPane.getTrackerStudio());
   }

   public UnpressedTracker(BPxEditorPane editorPane, TrackerStudio studio) {
      super(editorPane);
      this.studio = studio;
      this.selected = editorPane.getSelectedWidgets();
      studio.setMouseCursor(MouseCursor.normal);
   }

   @Override
   public Tracker mouseMoved(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      Handle handle = this.editorPane.getSelectedWidgets().getHandle(x, y);
      if (handle == null) {
         BWidget widget = this.studio.rootDescendant(new Point(x, y));
         if (widget != null && widget instanceof BSplitPane) {
            Point p = this.studio.translateFromRoot(widget, new Point(x, y));
            MouseCursor c = (MouseCursor)widget.fw(304, p, null, null, null);
            this.studio.setMouseCursor(c == null ? MouseCursor.normal : c);
         } else {
            this.studio.setMouseCursor(MouseCursor.normal);
         }
      } else if (this.editorPane.getLayerManager().isNormal(handle.widget)) {
         this.studio.setMouseCursor(handle.cursor);
      }

      return this;
   }

   @Override
   public Tracker mousePressed(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      setAnchorX(x);
      setAnchorY(y);
      switch (event.getClickCount()) {
         case 1:
            if (event.isButton1Down()) {
               return this.handleLeftClick(event);
            }

            if (event.isButton3Down()) {
               return this.handleRightClick(event);
            }
            break;
         case 2:
            if (event.isButton1Down()) {
               return this.handleDoubleClick(event);
            }
      }

      return this;
   }

   @Override
   public Tracker keyPressed(BKeyEvent event) {
      BWidget[] widgets = this.selected.getWidgets();
      if (!this.editorPane.getLayerManager().allNormal(widgets)) {
         return this;
      } else {
         this.studio.setShiftDown(event.isShiftDown());
         if (this.studio.getCurrentCanvas() == null) {
            return this;
         } else {
            if (isArrowKey(event)) {
               this.moveWidgetsByArrowKey(event, widgets);
               event.consume();
            }

            return this;
         }
      }
   }

   private Tracker handleLeftClick(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      Handle handle = this.selected.getHandle(x, y);
      if (handle == null) {
         BWidget[] stack = this.studio.rootDescendants(new Point(x, y));
         if (stack == null) {
            this.selected.deselectAll();
            this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
            return this;
         } else {
            return this.hitLeftSingleWidget(event, stack[0]);
         }
      } else if (handle.cursor == MouseCursor.doNotEnter) {
         return this;
      } else if (!this.editorPane.getLayerManager().isNormal(handle.widget)) {
         return this;
      } else {
         boolean preserveAspectRatio = event.isControlDown();
         return Artisan.instance().makeHandleTracker(this.editorPane, handle, preserveAspectRatio);
      }
   }

   private Tracker hitLeftSingleWidget(BMouseEvent event, BWidget widget) {
      if (widget instanceof BCanvasPane) {
         return new RubberBandTracker(this.editorPane, (BCanvasPane)widget, event.isControlDown());
      } else {
         if (widget instanceof BTabbedPane) {
            widget.mousePressed(this.makeMouseEvent(this.studio, widget, event));
            this.editorPane.forceRootLayout();
         } else if (widget instanceof BScrollBar || widget instanceof BSplitPane) {
            if (this.editorPane.getLayerManager().isNormal(widget)) {
               return this.passThrough(event, widget);
            }

            return this;
         }

         return (Tracker)(this.selected.isSelected(widget) ? new HitSelectedTracker(this.editorPane, widget) : this.hitLeftUnselected(event, widget));
      }
   }

   private Tracker passThrough(BMouseEvent event, BWidget widget) {
      widget.mousePressed(this.makeMouseEvent(this.studio, widget, event));
      if (widget instanceof BSplitPane) {
         double x = event.getX();
         double y = event.getY();
         Point p = this.studio.translateFromRoot(widget, new Point(x, y));
         MouseCursor c = (MouseCursor)widget.fw(304, p, null, null, null);
         this.studio.setMouseCursor(c == null ? MouseCursor.normal : c);
      } else {
         this.studio.setMouseCursor(MouseCursor.normal);
      }

      this.selected.deselectAll();
      this.selected.select(widget instanceof BSplitPane ? widget : widget.getParentWidget());
      this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      return new PassThroughTracker(this.editorPane, widget);
   }

   private Tracker hitLeftUnselected(BMouseEvent event, BWidget widget) {
      if (!event.isControlDown() || !this.selected.canSelect(widget)) {
         this.selected.deselectAll();
      }

      this.selected.select(widget);
      this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      if (this.editor.isReadonly()) {
         return this;
      } else if (!this.editorPane.getLayerManager().isNormal(widget)) {
         return this;
      } else {
         BWidget parent = widget.getParentWidget();
         BCanvasPane canvas = Reflector.canvas(parent);
         return (Tracker)(canvas == null ? this : makeMoveTracker(this.editorPane, canvas, parent));
      }
   }

   static MoveTracker makeMoveTracker(BPxEditorPane editorPane, BCanvasPane canvas, BWidget parent) {
      if (canvas == parent) {
         return new MoveTracker(editorPane, canvas, editorPane.getSelectedWidgets().getWidgets());
      } else {
         for (BWidget gp = parent.getParentWidget(); gp != canvas; gp = gp.getParentWidget()) {
            parent = gp;
         }

         return new MoveTracker(editorPane, canvas, new BWidget[]{parent});
      }
   }

   private Tracker handleRightClick(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      Handle handle = this.selected.getHandle(x, y);
      if (handle == null) {
         BWidget[] stack = this.studio.rootDescendants(new Point(x, y));
         if (stack == null) {
            return this;
         } else {
            return stack.length == 1 ? this.hitRightSingleWidget(event, stack[0]) : this.hitRightWidgetStack(event, stack);
         }
      } else {
         this.studio.showPopupMenu(event);
         return this;
      }
   }

   private Tracker hitRightSingleWidget(BMouseEvent event, BWidget widget) {
      if (widget == null) {
         return this;
      } else {
         if (widget instanceof BScrollBar) {
            widget = widget.getParentWidget();
         }

         if (this.selected.isSelected(widget)) {
            this.studio.showPopupMenu(event);
            return this;
         } else {
            return this.hitRightUnselected(event, widget);
         }
      }
   }

   private Tracker hitRightUnselected(BMouseEvent event, BWidget widget) {
      if (!event.isControlDown() || !this.selected.canSelect(widget)) {
         this.selected.deselectAll();
      }

      this.selected.select(widget);
      this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      this.studio.showPopupMenu(event);
      return this;
   }

   private Tracker hitRightWidgetStack(BMouseEvent event, BWidget[] stack) {
      for (int i = 0; i < stack.length; i++) {
         if (this.selected.isSelected(stack[i])) {
            this.studio.showPopupMenu(event);
            return this;
         }
      }

      return this.hitRightUnselected(event, stack[0]);
   }

   private Tracker handleDoubleClick(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      Handle handle = this.selected.getHandle(x, y);
      if (handle == null) {
         BWidget[] stack = this.studio.rootDescendants(new Point(x, y));
         return (Tracker)(stack == null ? this : this.editSingleWidget(stack[0], event));
      } else {
         return this.editSingleWidget(handle.widget, event);
      }
   }

   private Tracker editSingleWidget(BWidget widget, BMouseEvent event) {
      if (widget == null) {
         return this;
      } else {
         if (widget instanceof BScrollBar) {
            widget = widget.getParentWidget();
         }

         this.selected.deselectAll();
         this.selected.select(widget);
         this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
         Command cmd = this.editor.getController().getDoubleClickCommand((BStudio)this.studio, event);
         if (cmd != null) {
            cmd.invoke();
         }

         return this;
      }
   }

   private void moveWidgetsByArrowKey(BKeyEvent event, BWidget[] widgets) {
      BCanvasPane pane = this.studio.getCurrentCanvas();
      Artisan artisan = Artisan.instance();
      switch (arrowDirection(event)) {
         case LEFT:
            double minxx = MoveWidget.getMinX(widgets, artisan);
            double movexxx = this.getArrowKeyMoveDistance(event, minxx);
            if (minxx + movexxx >= 0.0) {
               this.moveWidgets(widgets, movexxx, 0.0);
            }
            break;
         case RIGHT:
            double minx = MoveWidget.getMinX(widgets, artisan);
            double maxx = MoveWidget.getMaxX(widgets, artisan);
            double movexx = this.getArrowKeyMoveDistance(event, minx);
            if (maxx + movexx <= pane.getViewSize().width()) {
               this.moveWidgets(widgets, movexx, 0.0);
            }
            break;
         case UP:
            double minyx = MoveWidget.getMinY(widgets, artisan);
            double movex = this.getArrowKeyMoveDistance(event, minyx);
            if (minyx + movex >= 0.0) {
               this.moveWidgets(widgets, 0.0, movex);
            }
            break;
         case DOWN:
            double miny = MoveWidget.getMinY(widgets, artisan);
            double maxy = MoveWidget.getMaxY(widgets, artisan);
            double move = this.getArrowKeyMoveDistance(event, miny);
            if (maxy + move <= pane.getViewSize().height()) {
               this.moveWidgets(widgets, 0.0, move);
            }
      }
   }

   private int getArrowKeyMoveDistance(BKeyEvent event, double startPosition) {
      int startingPoint = (int)startPosition;
      int snapSize = this.getSnapSize();
      int move = snapSize;
      boolean allowSnappingToMidpoint = snapSize >= 8;
      switch (arrowDirection(event)) {
         case LEFT:
         case UP:
            move = -snapSize;
         default:
            if (event.isControlDown()) {
               return move > 0 ? 1 : -1;
            } else {
               if (allowSnappingToMidpoint) {
                  move /= 2;
               }

               int moveTarget = startingPoint + move;
               int lowerSnap = this.getLowerSnap(moveTarget);
               int upperSnap = lowerSnap + snapSize;
               if (startingPoint % snapSize != 0 && lowerSnap != this.getLowerSnap(startingPoint)) {
                  moveTarget = move < 0 ? upperSnap : lowerSnap;
                  lowerSnap = this.getLowerSnap(moveTarget);
                  upperSnap = lowerSnap + snapSize;
               }

               int actualTarget;
               if (allowSnappingToMidpoint) {
                  int midPoint = (lowerSnap + upperSnap) / 2;
                  actualTarget = this.closest(moveTarget, lowerSnap, midPoint, upperSnap);
               } else {
                  actualTarget = this.closest(moveTarget, lowerSnap, upperSnap);
               }

               return actualTarget - startingPoint;
            }
      }
   }

   private static boolean isArrowKey(BKeyEvent event) {
      return arrowDirection(event) != UnpressedTracker.Dir.NONE;
   }

   private static UnpressedTracker.Dir arrowDirection(BKeyEvent event) {
      switch (event.getKeyCode()) {
         case 37:
         case 226:
            return UnpressedTracker.Dir.LEFT;
         case 38:
         case 224:
            return UnpressedTracker.Dir.UP;
         case 39:
         case 227:
            return UnpressedTracker.Dir.RIGHT;
         case 40:
         case 225:
            return UnpressedTracker.Dir.DOWN;
         default:
            return UnpressedTracker.Dir.NONE;
      }
   }

   private int closest(int number, int... possibilities) {
      int closest = number;
      int minDelta = Integer.MAX_VALUE;

      for (int n : possibilities) {
         int delta = Math.abs(number - n);
         if (delta < minDelta) {
            minDelta = delta;
            closest = n;
         }
      }

      return closest;
   }

   private int getSnapSize() {
      return this.editorPane.getOptions().getSnapSize();
   }

   private int getLowerSnap(int n) {
      return n - n % this.getSnapSize();
   }

   private void moveWidgets(BWidget[] widgets, double x, double y) {
      new MoveWidget(this.editorPane, widgets, x, y).invoke();
   }

   private static enum Dir {
      UP,
      DOWN,
      LEFT,
      RIGHT,
      NONE;
   }
}
