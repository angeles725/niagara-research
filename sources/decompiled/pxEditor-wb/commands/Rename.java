package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.Reflector;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.ui.BDialog;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class Rename extends Command {
   BPxEditorPane editorPane;
   BWidget parent;
   BWidget widget;
   String newName;
   String oldName;

   public Rename(BPxEditorPane editorPane) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.rename");
      this.editorPane = editorPane;
   }

   public CommandArtifact doInvoke() throws Exception {
      BWidget[] widgets = this.editorPane.getSelectedWidgets().getWidgets();
      if (widgets.length != 1) {
         throw new IllegalStateException();
      } else {
         this.widget = widgets[0];
         this.parent = this.widget.getParentWidget();
         if (!Reflector.isFreeFormPane(this.parent)) {
            throw new IllegalStateException();
         } else {
            String title = BPxEditorPane.text("commands.rename");
            BTextField text = new BTextField(this.widget.getName(), 20);
            int r = BDialog.open(this.editorPane, title, text, 3);
            if (r != 1) {
               return null;
            } else {
               this.oldName = this.widget.getName();
               this.newName = text.getText();
               Rename.Artifact artifact = new Rename.Artifact();
               artifact.redo();
               return artifact;
            }
         }
      }
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         Property prop = Rename.this.widget.getPropertyInParent();
         String propName = prop.getName();
         Rename.this.parent.rename(prop, Rename.this.newName);
         this.update(propName, Rename.this.oldName);
      }

      public void undo() throws Exception {
         Property prop = Rename.this.widget.getPropertyInParent();
         String propName = prop.getName();
         Rename.this.parent.rename(prop, Rename.this.oldName);
         this.update(propName, Rename.this.newName);
      }

      private void update(String propName, String previousWidgetName) {
         Rename.this.editorPane.getCommandStudio().getPainter().reset();
         Rename.this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(3, Rename.this.parent, propName, BString.make(previousWidgetName)));
      }
   }
}
