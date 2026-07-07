package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.LayerManager;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BResponsivePane;
import javax.baja.ui.px.BLayerTag;

public class RemoveResponsive extends Command {
   private BPxEditorPane editorPane;
   private BWidget parent;
   private BResponsivePane[] panes;
   private BWidget[] kids;
   private String[] names;

   public RemoveResponsive(BPxEditorPane editorPane) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.removeResponsive");
      this.editorPane = editorPane;
   }

   public CommandArtifact doInvoke() throws Exception {
      BWidget[] arr = this.editorPane.getSelectedWidgets().getWidgets();
      this.panes = new BResponsivePane[arr.length];

      for (int i = 0; i < arr.length; i++) {
         this.panes[i] = (BResponsivePane)arr[i];
      }

      this.parent = this.panes[0].getParentWidget();
      this.names = new String[this.panes.length];
      this.kids = new BWidget[this.panes.length];

      for (int i = 0; i < this.panes.length; i++) {
         this.names[i] = this.panes[i].getPropertyInParent().getName();
         this.kids[i] = this.panes[i].getContent();
      }

      RemoveResponsive.Artifact artifact = new RemoveResponsive.Artifact(this);
      artifact.redo();
      return artifact;
   }

   static class Artifact implements CommandArtifact {
      private BPxEditorPane editorPane;
      private BWidget parent;
      private BResponsivePane[] panes;
      private BWidget[] kids;
      private String[] names;

      private Artifact(RemoveResponsive addResponsive) {
         this.editorPane = addResponsive.editorPane;
         this.parent = addResponsive.parent;
         this.kids = addResponsive.kids;
         this.panes = addResponsive.panes;
         this.names = addResponsive.names;
      }

      public void redo() throws Exception {
         for (int i = 0; i < this.panes.length; i++) {
            this.panes[i].setContent(new BNullWidget());
            this.parent.set(this.names[i], this.kids[i]);
            if (this.parent instanceof BCanvasPane) {
               this.kids[i].setLayout(this.panes[i].getLayout());
            }

            this.editorPane.getLayerManager().removeTag(this.panes[i]);
         }

         this.update(this.kids, new PxWidgetEvent(2, this.parent, this.names, this.kids));
      }

      public void undo() throws Exception {
         for (int i = 0; i < this.panes.length; i++) {
            this.parent.set(this.names[i], this.panes[i]);
            this.panes[i].setContent(this.kids[i]);
            LayerManager mgr = this.editorPane.getLayerManager();
            BLayerTag layerTag = mgr.getTag(this.kids[i]);
            if (layerTag != null) {
               mgr.addTag(this.panes[i], mgr.getLayerByName(layerTag.getLayerName()));
            }
         }

         this.update(this.panes, new PxWidgetEvent(2, this.parent, this.names, this.panes));
      }

      private void update(BWidget[] sel, PxEvent event) {
         this.editorPane.getSelectedWidgets().deselectAll();
         if (this.editorPane.getTool().isNormal()) {
            this.editorPane.getSelectedWidgets().setWidgets(sel);
         }

         this.editorPane.getPxEditor().firePxEvent(event);
      }
   }
}
