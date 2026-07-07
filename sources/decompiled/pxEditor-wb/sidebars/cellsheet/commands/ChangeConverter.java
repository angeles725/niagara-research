package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import javax.baja.px.editor.event.PxBindingEvent;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.util.BConverter;

public class ChangeConverter extends Command {
   private BPxEditorPane editorPane;
   private CellSheetContext context;
   private BWidget widget;
   private BBinding binding;
   private String propName;
   private BConverter oldConv;
   private BConverter newConv;

   public ChangeConverter(
      BPxEditorPane editorPane, CellSheetContext context, BWidget widget, BBinding binding, String propName, BConverter oldConv, BConverter newConv
   ) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.changeProperty");
      if (editorPane.getSelectedWidgets().getWidgets().length != 1) {
         throw new IllegalStateException();
      } else {
         this.editorPane = editorPane;
         this.context = context;
         this.widget = widget;
         this.binding = binding;
         this.propName = propName;
         this.oldConv = oldConv;
         this.newConv = newConv;
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      ChangeConverter.Artifact artifact = new ChangeConverter.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private Artifact() {
      }

      public void redo() throws Exception {
         ChangeConverter.this.binding.set(ChangeConverter.this.propName, ChangeConverter.this.newConv);
         ChangeConverter.this.editorPane
            .getPxEditor()
            .firePxEvent(new PxBindingEvent(2, ChangeConverter.this.binding, ChangeConverter.this.propName, ChangeConverter.this.newConv));
      }

      public void undo() throws Exception {
         ChangeConverter.this.binding.set(ChangeConverter.this.propName, ChangeConverter.this.oldConv);
         ChangeConverter.this.editorPane
            .getPxEditor()
            .firePxEvent(new PxBindingEvent(2, ChangeConverter.this.binding, ChangeConverter.this.propName, ChangeConverter.this.oldConv));
      }
   }
}
