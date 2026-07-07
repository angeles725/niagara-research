package com.tridium.px.editor.studio.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.EventUtil;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.ui.BLayout;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BPane;

public class PreferredSize extends Command {
   private BPxEditorPane editorPane;
   private SelectedWidgets selected;
   private BWidget[] widgets;
   private BLayout[] oldLy;
   private BLayout[] newLy;

   public PreferredSize(BPxEditorPane editorPane, BPane freeForm) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.preferredSize");
      this.editorPane = editorPane;
      this.selected = editorPane.getSelectedWidgets();
      this.widgets = this.selected.getWidgets();
      if (freeForm == null) {
         this.setEnabled(false);
      } else {
         this.oldLy = new BLayout[this.widgets.length];
         this.newLy = new BLayout[this.widgets.length];

         for (int i = 0; i < this.widgets.length; i++) {
            BWidget w = this.widgets[i];
            BLayout ly = w.getLayout();
            this.oldLy[i] = ly;
            w.computePreferredSize();
            this.newLy[i] = BLayout.make(ly.getX(), ly.getXUnit(), ly.getY(), ly.getYUnit(), w.getPreferredWidth(), 0, w.getPreferredHeight(), 0);
         }
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      PreferredSize.Artifact artifact = new PreferredSize.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      private Artifact() {
      }

      public void redo() throws Exception {
         for (int i = 0; i < PreferredSize.this.widgets.length; i++) {
            PreferredSize.this.widgets[i].setLayout(PreferredSize.this.newLy[i]);
         }

         PreferredSize.this.selected.setWidgets(PreferredSize.this.widgets);
         PreferredSize.this.editorPane.getPxEditor().firePxEvent(EventUtil.widgetsChanged(PreferredSize.this.widgets, "layout"));
      }

      public void undo() throws Exception {
         for (int i = 0; i < PreferredSize.this.widgets.length; i++) {
            PreferredSize.this.widgets[i].setLayout(PreferredSize.this.oldLy[i]);
         }

         PreferredSize.this.selected.setWidgets(PreferredSize.this.widgets);
         PreferredSize.this.editorPane.getPxEditor().firePxEvent(EventUtil.widgetsChanged(PreferredSize.this.widgets, "layout"));
      }
   }
}
