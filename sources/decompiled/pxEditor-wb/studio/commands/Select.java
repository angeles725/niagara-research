package com.tridium.px.editor.studio.commands;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class Select extends Command {
   private BPxEditorPane editorPane;
   private BWidget[] stack;
   private int idx;
   private boolean above;
   private boolean byOne;

   public Select(BPxEditorPane editorPane, BWidget[] stack, int idx, boolean above, boolean byOne) {
      super(
         editorPane,
         BPxEditorPane.lexicon(),
         byOne ? (above ? "commands.nextAbove" : "commands.nextBelow") : (above ? "commands.firstAbove" : "commands.lastBelow")
      );
      this.editorPane = editorPane;
      this.stack = stack;
      this.idx = idx;
      this.above = above;
      this.byOne = byOne;
      if (idx == -1 || above && idx == 0 || !above && idx == stack.length - 1) {
         this.setEnabled(false);
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      this.editorPane.getSelectedWidgets().deselect(this.stack[this.idx]);
      this.editorPane
         .getSelectedWidgets()
         .select(this.stack[this.above ? (this.byOne ? this.idx - 1 : 0) : (this.byOne ? this.idx + 1 : this.stack.length - 1)]);
      this.editorPane.getPxEditor().firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      return null;
   }
}
