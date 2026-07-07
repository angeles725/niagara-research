package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.LayerManager;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.px.BLayerTag;

public class RemoveBorder extends Command {
   BPxEditorPane editorPane;
   BWidget parent;
   BBorderPane[] borders;
   BWidget[] kids;
   String[] names;

   public RemoveBorder(BPxEditorPane editorPane) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.removeBorder");
      this.editorPane = editorPane;
   }

   public CommandArtifact doInvoke() throws Exception {
      BWidget[] arr = this.editorPane.getSelectedWidgets().getWidgets();
      this.borders = new BBorderPane[arr.length];

      for (int i = 0; i < arr.length; i++) {
         this.borders[i] = (BBorderPane)arr[i];
      }

      this.parent = this.borders[0].getParentWidget();
      this.names = new String[this.borders.length];
      this.kids = new BWidget[this.borders.length];

      for (int i = 0; i < this.borders.length; i++) {
         this.names[i] = this.borders[i].getPropertyInParent().getName();
         this.kids[i] = this.borders[i].getContent();
      }

      RemoveBorder.Artifact artifact = new RemoveBorder.Artifact();
      artifact.redo();
      return artifact;
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         for (int i = 0; i < RemoveBorder.this.borders.length; i++) {
            RemoveBorder.this.borders[i].setContent(new BNullWidget());
            RemoveBorder.this.parent.set(RemoveBorder.this.names[i], RemoveBorder.this.kids[i]);
            if (RemoveBorder.this.parent instanceof BCanvasPane) {
               RemoveBorder.this.kids[i].setLayout(RemoveBorder.this.borders[i].getLayout());
            }

            RemoveBorder.this.editorPane.getLayerManager().removeTag(RemoveBorder.this.borders[i]);
         }

         this.update(RemoveBorder.this.kids, new PxWidgetEvent(2, RemoveBorder.this.parent, RemoveBorder.this.names, RemoveBorder.this.kids));
      }

      public void undo() throws Exception {
         for (int i = 0; i < RemoveBorder.this.borders.length; i++) {
            RemoveBorder.this.parent.set(RemoveBorder.this.names[i], RemoveBorder.this.borders[i]);
            RemoveBorder.this.borders[i].setContent(RemoveBorder.this.kids[i]);
            LayerManager mgr = RemoveBorder.this.editorPane.getLayerManager();
            BLayerTag layerTag = mgr.getTag(RemoveBorder.this.kids[i]);
            if (layerTag != null) {
               mgr.addTag(RemoveBorder.this.borders[i], mgr.getLayerByName(layerTag.getLayerName()));
            }
         }

         this.update(RemoveBorder.this.borders, new PxWidgetEvent(2, RemoveBorder.this.parent, RemoveBorder.this.names, RemoveBorder.this.borders));
      }

      private void update(BWidget[] sel, PxEvent event) {
         RemoveBorder.this.editorPane.getSelectedWidgets().deselectAll();
         if (RemoveBorder.this.editorPane.getTool().isNormal()) {
            RemoveBorder.this.editorPane.getSelectedWidgets().setWidgets(sel);
         }

         RemoveBorder.this.editorPane.getPxEditor().firePxEvent(event);
      }
   }
}
