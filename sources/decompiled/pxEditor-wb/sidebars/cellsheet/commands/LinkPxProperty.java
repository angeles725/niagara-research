package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import javax.baja.sys.BComponent;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxProperty;

public class LinkPxProperty extends Command {
   private CellSheetContext context;
   private PxProperty group;
   private BComponent[] components;
   private String propName;

   public LinkPxProperty(BPxEditorPane editorPane, CellSheetContext context, PxProperty group, BComponent[] components, String propName, boolean enabled) {
      super(editorPane, group.getName());
      this.context = context;
      this.group = group;
      this.components = components;
      this.propName = propName;
      this.setEnabled(enabled);
   }

   public CommandArtifact doInvoke() throws Exception {
      CommandArtifact artifact = new CommandArtifact() {
         public void redo() throws Exception {
            for (int i = 0; i < LinkPxProperty.this.components.length; i++) {
               LinkPxProperty.this.context.pxPropertyLinked(LinkPxProperty.this.group, LinkPxProperty.this.components[i], LinkPxProperty.this.propName);
            }
         }

         public void undo() throws Exception {
            for (int i = 0; i < LinkPxProperty.this.components.length; i++) {
               LinkPxProperty.this.context.pxPropertyUnlinked(LinkPxProperty.this.group, LinkPxProperty.this.components[i], LinkPxProperty.this.propName);
            }
         }
      };
      artifact.redo();
      return artifact;
   }
}
