package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;

public class InsertDynamic extends Insert {
   private BWidget[] newWidgets;
   private String[] names;

   public InsertDynamic(BPxEditor editor, BPxEditorPane editorPane, BWidget parentWidget, BWidget[] newWidgets) {
      super(editor, parentWidget, newWidgets);
      if (newWidgets.length == 0) {
         throw new IllegalStateException();
      } else {
         this.newWidgets = newWidgets;
         this.names = new String[newWidgets.length];

         for (int i = 0; i < newWidgets.length; i++) {
            editorPane.getLayerManager().stripMissingLayer(newWidgets[i]);
         }
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      CommandArtifact artifact = new InsertDynamic.Artifact();
      artifact.redo();
      return artifact;
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         InsertDynamic.this.selected.deselectAll();

         for (int i = 0; i < InsertDynamic.this.newWidgets.length; i++) {
            InsertDynamic.this.names[i] = InsertDynamic.this.parentWidget.add(null, InsertDynamic.this.newWidgets[i]).getName();
            if (InsertDynamic.this.editorPane.getTool().isNormal()) {
               InsertDynamic.this.selected.select(InsertDynamic.this.newWidgets[i]);
            }
         }

         InsertDynamic.this.editor.firePxEvent(new PxWidgetEvent(0, InsertDynamic.this.parentWidget, InsertDynamic.this.names, InsertDynamic.this.newWidgets));
      }

      public void undo() throws Exception {
         InsertDynamic.this.selected.deselectAll();

         for (int i = 0; i < InsertDynamic.this.newWidgets.length; i++) {
            InsertDynamic.this.parentWidget.remove(InsertDynamic.this.newWidgets[i].getPropertyInParent());
         }

         InsertDynamic.this.selected.deselectAll();
         InsertDynamic.this.editor.firePxEvent(new PxWidgetEvent(1, InsertDynamic.this.parentWidget, InsertDynamic.this.names, InsertDynamic.this.newWidgets));
      }
   }
}
