package com.tridium.px.editor.studio.trackers;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TrackerStudio;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BSplitPane;

public class PassThroughTracker extends Tracker {
   private TrackerStudio studio;
   private BWidget widget;
   private double beforeDivWidth;

   public PassThroughTracker(BPxEditorPane editorPane, BWidget widget) {
      super(editorPane);
      this.widget = widget;
      this.studio = editorPane.getTrackerStudio();
      if (widget instanceof BSplitPane) {
         this.beforeDivWidth = ((BSplitPane)widget).getDividerPosition();
      }
   }

   @Override
   public Tracker mouseDragged(BMouseEvent event) {
      this.widget.mouseDragged(this.makeMouseEvent(this.studio, this.widget, event));
      this.editorPane.forceRootLayout();
      this.editorPane.repaint();
      return this;
   }

   @Override
   public Tracker mouseReleased(BMouseEvent event) {
      this.widget.mouseReleased(this.makeMouseEvent(this.studio, this.widget, event));
      this.studio.setMouseCursor(MouseCursor.normal);
      if (this.widget instanceof BSplitPane) {
         double afterDivWidth = ((BSplitPane)this.widget).getDividerPosition();
         new PassThroughTracker.ChangeDividerPosition((BSplitPane)this.widget, this.beforeDivWidth, afterDivWidth).invoke();
      } else {
         this.editor.firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      }

      return new UnpressedTracker(this.editorPane);
   }

   class ChangeDividerPosition extends Command {
      BSplitPane pane;
      double before;
      double after;

      ChangeDividerPosition(BSplitPane pane, double before, double after) {
         super(PassThroughTracker.this.editorPane, "changeDividerPosition");
         this.pane = pane;
         this.before = before;
         this.after = after;
      }

      public CommandArtifact doInvoke() throws Exception {
         PassThroughTracker.ChangeDividerPosition.Artifact artifact = new PassThroughTracker.ChangeDividerPosition.Artifact();
         artifact.redo();
         return artifact;
      }

      class Artifact implements CommandArtifact {
         public void redo() throws Exception {
            ChangeDividerPosition.this.pane.setDividerPosition(ChangeDividerPosition.this.after);
            this.update();
         }

         public void undo() throws Exception {
            ChangeDividerPosition.this.pane.setDividerPosition(ChangeDividerPosition.this.before);
            this.update();
         }

         void update() {
            PassThroughTracker.this.editorPane.getSelectedWidgets().deselectAll();
            PassThroughTracker.this.editorPane.getSelectedWidgets().select(ChangeDividerPosition.this.pane);
            PassThroughTracker.this.editor
               .firePxEvent(new PxWidgetEvent(2, ChangeDividerPosition.this.pane, "dividerPosition", ChangeDividerPosition.this.pane.get("dividerPosition")));
         }
      }
   }
}
