package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class DeleteBinding extends Command {
   private BPxEditorPane editorPane;
   private CellSheetContext context;
   private BWidget widget;
   private BBinding binding;

   public DeleteBinding(BPxEditorPane editorPane, CellSheetContext context, BWidget widget, BBinding binding) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.apply");
      this.editorPane = editorPane;
      this.context = context;
      this.widget = widget;
      this.binding = binding;
   }

   public CommandArtifact doInvoke() throws Exception {
      DeleteBinding.Artifact artifact = new DeleteBinding.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private Artifact() {
      }

      public void redo() throws Exception {
         Property prop = DeleteBinding.this.binding.getPropertyInParent();
         DeleteBinding.this.widget.remove(DeleteBinding.this.binding);
         DeleteBinding.this.context.bindingDeleted(DeleteBinding.this.widget, prop, DeleteBinding.this.binding);
      }

      public void undo() throws Exception {
         Property prop = DeleteBinding.this.widget.add(null, DeleteBinding.this.binding);
         DeleteBinding.this.context.bindingAdded(DeleteBinding.this.widget, prop, DeleteBinding.this.binding);
      }
   }
}
