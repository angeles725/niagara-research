package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.sys.IllegalParentException;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;

public class InsertFrozen extends Insert {
   private BWidget oldWidget;
   private BWidget newWidget;
   private String slotName;

   public InsertFrozen(BPxEditor editor, BPxEditorPane editorPane, BWidget parentWidget, BWidget newWidget, String slotName) {
      super(editor, parentWidget, new BWidget[]{newWidget});
      this.oldWidget = (BWidget)parentWidget.get(slotName);
      this.newWidget = newWidget;
      this.slotName = slotName;
      editorPane.getLayerManager().stripMissingLayer(newWidget);
   }

   public CommandArtifact doInvoke() throws Exception {
      CommandArtifact artifact = new InsertFrozen.Artifact();
      artifact.redo();
      return artifact;
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         InsertFrozen.this.selected.deselectAll();
         if (!InsertFrozen.this.newWidget.isParentLegal(InsertFrozen.this.parentWidget)) {
            throw new IllegalParentException(
               "baja", "IllegalParentException.parentAndChild", new Object[]{InsertFrozen.this.parentWidget.getType(), InsertFrozen.this.newWidget.getType()}
            );
         } else {
            InsertFrozen.this.parentWidget.set(InsertFrozen.this.slotName, InsertFrozen.this.newWidget);
            InsertFrozen.this.selected.select(InsertFrozen.this.newWidget);
            InsertFrozen.this.editor.firePxEvent(new PxWidgetEvent(0, InsertFrozen.this.parentWidget, InsertFrozen.this.slotName, InsertFrozen.this.newWidget));
         }
      }

      public void undo() throws Exception {
         InsertFrozen.this.selected.deselectAll();
         InsertFrozen.this.parentWidget.set(InsertFrozen.this.slotName, InsertFrozen.this.oldWidget);
         InsertFrozen.this.selected.deselectAll();
         InsertFrozen.this.editor.firePxEvent(new PxWidgetEvent(1, InsertFrozen.this.parentWidget, InsertFrozen.this.slotName, InsertFrozen.this.newWidget));
      }
   }
}
