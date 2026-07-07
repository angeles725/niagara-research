package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import javax.baja.px.editor.event.PxBindingEvent;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class ChangeBinding extends Command {
   private BPxEditorPane editorPane;
   private BPxCellSheet cellSheet;
   private BBinding binding;
   private Property prop;
   private BValue oldValue;
   private BValue newValue;

   public ChangeBinding(BPxEditorPane editorPane, BPxCellSheet cellSheet, BBinding binding, Property prop, BValue newValue) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.changeProperty");
      this.editorPane = editorPane;
      this.cellSheet = cellSheet;
      this.binding = binding;
      this.prop = prop;
      this.oldValue = binding.get(prop);
      this.newValue = newValue.newCopy();
   }

   public CommandArtifact doInvoke() throws Exception {
      ChangeBinding.Artifact artifact = new ChangeBinding.Artifact();
      if (this.prop.getName().equals("ord")) {
         artifact.initialInvocation = false;
      }

      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private boolean initialInvocation = true;

      private Artifact() {
      }

      public void redo() throws Exception {
         this.perform(ChangeBinding.this.newValue);
      }

      public void undo() throws Exception {
         this.perform(ChangeBinding.this.oldValue);
      }

      private void perform(BValue value) {
         ChangeBinding.this.binding.set(ChangeBinding.this.prop, value);
         PxEvent event = new PxBindingEvent(2, ChangeBinding.this.binding, ChangeBinding.this.prop.getName(), value);
         ChangeBinding.this.editorPane.getSelectedWidgets().setWidgets(new BWidget[]{(BWidget)ChangeBinding.this.binding.getParent()});
         if (this.initialInvocation) {
            ChangeBinding.this.editorPane.getPxEditor().removePxListener(ChangeBinding.this.cellSheet);
            ChangeBinding.this.editorPane.getPxEditor().firePxEvent(event);
            ChangeBinding.this.editorPane.getPxEditor().addPxListener(ChangeBinding.this.cellSheet);
            this.initialInvocation = false;
         } else {
            ChangeBinding.this.editorPane.getPxEditor().firePxEvent(event);
         }
      }
   }
}
