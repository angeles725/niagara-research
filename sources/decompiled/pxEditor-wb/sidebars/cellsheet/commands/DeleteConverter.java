package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.util.BConverter;

public class DeleteConverter extends Command {
   private BPxEditorPane editorPane;
   private CellSheetContext context;
   private String propName;
   private BBinding binding;
   private BConverter converter;

   public DeleteConverter(BPxEditorPane editorPane, CellSheetContext context, boolean enabled, String propName, BConverter converter) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.unanimate");
      this.editorPane = editorPane;
      this.context = context;
      this.setEnabled(enabled);
      if (enabled) {
         this.propName = propName;
         this.converter = converter;
         this.binding = (BBinding)converter.getParent();
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      DeleteConverter.Artifact artifact = new DeleteConverter.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private Artifact() {
      }

      public void redo() throws Exception {
         Property prop = DeleteConverter.this.binding.getProperty(DeleteConverter.this.propName);
         DeleteConverter.this.binding.remove(DeleteConverter.this.propName);
         DeleteConverter.this.context.converterDeleted(DeleteConverter.this.binding, prop, DeleteConverter.this.converter);
      }

      public void undo() throws Exception {
         Property prop = DeleteConverter.this.binding.add(DeleteConverter.this.propName, DeleteConverter.this.converter);
         DeleteConverter.this.context.converterAdded(DeleteConverter.this.binding, prop, DeleteConverter.this.converter);
      }
   }
}
