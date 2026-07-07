package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.LayerManager;
import javax.baja.gx.BColor;
import javax.baja.gx.BInsets;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.ui.BBorder;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.px.BLayerTag;

public class AddBorder extends Command {
   BPxEditorPane editorPane;
   BWidget parent;
   BWidget[] widgets;
   BBorderPane[] borders;
   String[] names;

   public AddBorder(BPxEditorPane editorPane) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.addBorder");
      this.editorPane = editorPane;
   }

   public CommandArtifact doInvoke() throws Exception {
      this.widgets = this.editorPane.getSelectedWidgets().getWidgets();
      if (this.widgets.length == 0) {
         throw new IllegalStateException();
      } else {
         this.parent = this.widgets[0].getParentWidget();
         this.names = new String[this.widgets.length];
         this.borders = new BBorderPane[this.widgets.length];

         for (int i = 0; i < this.widgets.length; i++) {
            this.names[i] = this.widgets[i].getPropertyInParent().getName();
            this.borders[i] = new BBorderPane();
            this.borders[i].setBorder(BBorder.make(1.0, 1, BColor.black.toBrush()));
            this.borders[i].setPadding(BInsets.make(0.0, 0.0, 0.0, 0.0));
            this.borders[i].setLayout(this.widgets[i].getLayout());
         }

         AddBorder.Artifact artifact = new AddBorder.Artifact();
         artifact.redo();
         return artifact;
      }
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         for (int i = 0; i < AddBorder.this.widgets.length; i++) {
            AddBorder.this.parent.set(AddBorder.this.names[i], AddBorder.this.borders[i]);
            AddBorder.this.borders[i].setContent(AddBorder.this.widgets[i]);
            LayerManager mgr = AddBorder.this.editorPane.getLayerManager();
            BLayerTag layerTag = mgr.getTag(AddBorder.this.widgets[i]);
            if (!layerTag.isNull()) {
               mgr.addTag(AddBorder.this.borders[i], mgr.getLayerByName(layerTag.getLayerName()));
            }
         }

         this.update(AddBorder.this.borders, new PxWidgetEvent(2, AddBorder.this.parent, AddBorder.this.names, AddBorder.this.borders));
      }

      public void undo() throws Exception {
         for (int i = 0; i < AddBorder.this.widgets.length; i++) {
            AddBorder.this.borders[i].setContent(new BNullWidget());
            AddBorder.this.parent.set(AddBorder.this.names[i], AddBorder.this.widgets[i]);
            AddBorder.this.editorPane.getLayerManager().removeTag(AddBorder.this.borders[i]);
         }

         this.update(AddBorder.this.widgets, new PxWidgetEvent(2, AddBorder.this.parent, AddBorder.this.names, AddBorder.this.widgets));
      }

      private void update(BWidget[] sel, PxEvent event) {
         AddBorder.this.editorPane.getSelectedWidgets().deselectAll();
         if (AddBorder.this.editorPane.getTool().isNormal()) {
            AddBorder.this.editorPane.getSelectedWidgets().setWidgets(sel);
         }

         AddBorder.this.editorPane.getPxEditor().firePxEvent(event);
      }
   }
}
