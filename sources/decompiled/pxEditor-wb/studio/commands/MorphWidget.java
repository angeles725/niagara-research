package com.tridium.px.editor.studio.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.Artisan;
import javax.baja.gx.IGeom;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class MorphWidget extends Command {
   private BPxEditorPane editorPane;
   private BWidget widget;
   private IGeom newGeom;
   private IGeom oldGeom;
   private Artisan artisan;

   public MorphWidget(BPxEditorPane editorPane, BWidget widget, IGeom oldGeom, IGeom newGeom, Artisan artisan) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.resize");
      this.editorPane = editorPane;
      this.widget = widget;
      this.artisan = artisan;
      this.oldGeom = oldGeom;
      this.newGeom = newGeom;
   }

   public CommandArtifact doInvoke() throws Exception {
      MorphWidget.Artifact artifact = new MorphWidget.Artifact();
      artifact.redo();
      return artifact;
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         this.update(MorphWidget.this.artisan.setGeom(MorphWidget.this.widget, MorphWidget.this.newGeom));
      }

      public void undo() throws Exception {
         this.update(MorphWidget.this.artisan.setGeom(MorphWidget.this.widget, MorphWidget.this.oldGeom));
      }

      private void update(String propName) {
         MorphWidget.this.editorPane.getSelectedWidgets().select(MorphWidget.this.widget);
         MorphWidget.this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(2, MorphWidget.this.widget, propName, MorphWidget.this.widget.get(propName)));
      }
   }
}
