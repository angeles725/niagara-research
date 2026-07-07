package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.PxMediaValidationUtil;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.px.editor.BPxEditor;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;

public abstract class Insert extends Command {
   protected BPxEditor editor;
   protected BPxEditorPane editorPane;
   protected SelectedWidgets selected;
   protected BWidget parentWidget;
   private BWidget[] newWidgets;

   public Insert(BPxEditor editor, BWidget parentWidget, BWidget[] newWidgets) {
      super(editor, BPxEditorPane.lexicon(), "commands.insert");
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
      this.selected = this.editorPane.getSelectedWidgets();
      this.parentWidget = parentWidget;
      this.newWidgets = newWidgets;
   }

   public boolean checkMedia() {
      int result = PxMediaValidationUtil.confirmInsertDialog(this.editor.getMedia(), this.newWidgets, this.editor, this.editor.getCurrentContext());
      return result == 4;
   }
}
