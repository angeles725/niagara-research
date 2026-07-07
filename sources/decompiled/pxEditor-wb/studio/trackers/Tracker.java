package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.gx.Point;
import javax.baja.px.editor.BPxEditor;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;

public abstract class Tracker {
   static final int NEAR = 3;
   protected BPxEditorPane editorPane;
   protected BPxEditor editor;
   static double anchorX;
   static double anchorY;

   protected Tracker(BPxEditorPane editorPane) {
      this.editorPane = editorPane;
      this.editor = editorPane.getPxEditor();
   }

   public Tracker mouseMoved(BMouseEvent event) {
      return this;
   }

   public Tracker mousePressed(BMouseEvent event) {
      return this;
   }

   public Tracker mouseDragged(BMouseEvent event) {
      return this;
   }

   public Tracker mouseReleased(BMouseEvent event) {
      return this;
   }

   public Tracker mouseEntered(BMouseEvent event) {
      return this;
   }

   public Tracker mouseExited(BMouseEvent event) {
      return this;
   }

   public Tracker keyPressed(BKeyEvent event) {
      this.editorPane.getTrackerStudio().setShiftDown(event.isShiftDown());
      return this;
   }

   public Tracker keyReleased(BKeyEvent event) {
      this.editorPane.getTrackerStudio().setShiftDown(event.isShiftDown());
      return this;
   }

   public Tracker keyTyped(BKeyEvent event) {
      this.editorPane.getTrackerStudio().setShiftDown(event.isShiftDown());
      return this;
   }

   protected static double getAnchorX() {
      return anchorX;
   }

   protected static double getAnchorY() {
      return anchorY;
   }

   protected static void setAnchorX(double val) {
      anchorX = val;
   }

   protected static void setAnchorY(double val) {
      anchorY = val;
   }

   protected boolean moved(BMouseEvent event) {
      return Math.abs(event.getX() - getAnchorX()) > 3.0 || Math.abs(event.getY() - getAnchorY()) > 3.0;
   }

   protected final BMouseEvent makeMouseEvent(TrackerStudio studio, BWidget widget, BMouseEvent event) {
      Point p = studio.translateFromRoot(widget, new Point(event.getX(), event.getY()));
      return new BMouseEvent(event.getId(), widget, event.getModifiersEx(), p.x, p.y, event.getClickCount(), event.isPopupTrigger());
   }
}
