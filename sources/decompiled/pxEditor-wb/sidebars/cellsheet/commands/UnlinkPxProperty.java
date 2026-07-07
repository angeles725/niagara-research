package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import javax.baja.sys.BComponent;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxProperty;

public class UnlinkPxProperty extends Command {
   private CellSheetContext context;
   private BComponent[] components;
   private String propName;

   public UnlinkPxProperty(BPxEditorPane editorPane, CellSheetContext context, BComponent[] components, String propName, boolean enabled) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.unlink");
      this.context = context;
      this.components = components;
      this.propName = propName;
      this.setEnabled(enabled);
   }

   public CommandArtifact doInvoke() throws Exception {
      CommandArtifact artifact = new CommandArtifact() {
         PxProperty[] groups;

         public void redo() throws Exception {
            this.groups = new PxProperty[UnlinkPxProperty.this.components.length];

            for (int i = 0; i < UnlinkPxProperty.this.components.length; i++) {
               BComponent comp = UnlinkPxProperty.this.components[i];
               this.groups[i] = UnlinkPxProperty.this.context.getPxPropertyComponents().getPxProperty(comp, UnlinkPxProperty.this.propName);
               UnlinkPxProperty.this.context.pxPropertyUnlinked(this.groups[i], comp, UnlinkPxProperty.this.propName);
            }
         }

         public void undo() throws Exception {
            for (int i = 0; i < UnlinkPxProperty.this.components.length; i++) {
               BComponent comp = UnlinkPxProperty.this.components[i];
               UnlinkPxProperty.this.context.pxPropertyLinked(this.groups[i], comp, UnlinkPxProperty.this.propName);
            }
         }
      };
      artifact.redo();
      return artifact;
   }
}
