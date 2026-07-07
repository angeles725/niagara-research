package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.LayerManager;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BResponsivePane;
import javax.baja.ui.px.BLayerTag;

public class AddResponsive extends Command {
   private BPxEditorPane editorPane;
   private BWidget parent;
   private BWidget[] widgets;
   private BResponsivePane[] panes;
   private String[] names;

   public AddResponsive(BPxEditorPane editorPane) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.addResponsive");
      this.editorPane = editorPane;
   }

   public CommandArtifact doInvoke() throws Exception {
      this.widgets = this.editorPane.getSelectedWidgets().getWidgets();
      if (this.widgets.length == 0) {
         throw new IllegalStateException();
      } else {
         this.parent = this.widgets[0].getParentWidget();
         this.names = new String[this.widgets.length];
         this.panes = new BResponsivePane[this.widgets.length];

         for (int i = 0; i < this.widgets.length; i++) {
            this.names[i] = this.widgets[i].getPropertyInParent().getName();
            this.panes[i] = new BResponsivePane();
            this.panes[i].setLayout(this.widgets[i].getLayout());
         }

         AddResponsive.Artifact artifact = new AddResponsive.Artifact(this);
         artifact.redo();
         return artifact;
      }
   }

   static class Artifact implements CommandArtifact {
      private BPxEditorPane editorPane;
      private BWidget parent;
      private BWidget[] widgets;
      private BResponsivePane[] panes;
      private String[] names;

      private Artifact(AddResponsive addResponsive) {
         this.editorPane = addResponsive.editorPane;
         this.parent = addResponsive.parent;
         this.widgets = addResponsive.widgets;
         this.panes = addResponsive.panes;
         this.names = addResponsive.names;
      }

      public void redo() throws Exception {
         for (int i = 0; i < this.widgets.length; i++) {
            this.parent.set(this.names[i], this.panes[i]);
            this.panes[i].setContent(this.widgets[i]);
            LayerManager mgr = this.editorPane.getLayerManager();
            BLayerTag layerTag = mgr.getTag(this.widgets[i]);
            if (!layerTag.isNull()) {
               mgr.addTag(this.panes[i], mgr.getLayerByName(layerTag.getLayerName()));
            }
         }

         this.update(this.panes, new PxWidgetEvent(2, this.parent, this.names, this.panes));
      }

      public void undo() throws Exception {
         for (int i = 0; i < this.widgets.length; i++) {
            this.panes[i].setContent(new BNullWidget());
            this.parent.set(this.names[i], this.widgets[i]);
            this.editorPane.getLayerManager().removeTag(this.panes[i]);
         }

         this.update(this.widgets, new PxWidgetEvent(2, this.parent, this.names, this.widgets));
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
