package com.tridium.px.editor.studio.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.util.EventUtil;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class Align extends Command {
   private BPxEditorPane editorPane;
   private boolean horiz;
   private int type;
   private Artisan artisan = Artisan.instance();
   private BWidget[] widgets;
   private double[] deltaX;
   private double[] deltaY;
   public static final int MIN = 0;
   public static final int MAX = 1;
   public static final int CENTER = 2;

   public Align(BPxEditorPane editorPane, boolean horiz, int type) {
      super(
         editorPane,
         BPxEditorPane.lexicon(),
         horiz
            ? (type == 0 ? "commands.align.left" : (type == 1 ? "commands.align.right" : "commands.align.center"))
            : (type == 0 ? "commands.align.top" : (type == 1 ? "commands.align.bottom" : "commands.align.middle"))
      );
      this.editorPane = editorPane;
      this.type = type;
      this.horiz = horiz;
   }

   public CommandArtifact doInvoke() throws Exception {
      this.widgets = this.editorPane.getSelectedWidgets().getWidgets();
      this.deltaX = new double[this.widgets.length];
      this.deltaY = new double[this.widgets.length];
      if (this.horiz) {
         switch (this.type) {
            case 0:
               double val = MoveWidget.getMinX(this.widgets, this.artisan);

               for (int i = 0; i < this.widgets.length; i++) {
                  RectGeom r = this.artisan.bounds(this.widgets[i]);
                  this.deltaX[i] = val - r.x;
               }
               break;
            case 1:
               double val = MoveWidget.getMaxX(this.widgets, this.artisan);

               for (int i = 0; i < this.widgets.length; i++) {
                  RectGeom r = this.artisan.bounds(this.widgets[i]);
                  this.deltaX[i] = val - (r.x + r.width);
               }
               break;
            case 2:
               double val = MoveWidget.getCenterX(this.widgets, this.artisan);

               for (int i = 0; i < this.widgets.length; i++) {
                  RectGeom r = this.artisan.bounds(this.widgets[i]);
                  this.deltaX[i] = val - (r.x + r.width / 2.0);
               }
         }
      } else {
         switch (this.type) {
            case 0:
               double val = MoveWidget.getMinY(this.widgets, this.artisan);

               for (int i = 0; i < this.widgets.length; i++) {
                  RectGeom r = this.artisan.bounds(this.widgets[i]);
                  this.deltaY[i] = val - r.y;
               }
               break;
            case 1:
               double val = MoveWidget.getMaxY(this.widgets, this.artisan);

               for (int i = 0; i < this.widgets.length; i++) {
                  RectGeom r = this.artisan.bounds(this.widgets[i]);
                  this.deltaY[i] = val - (r.y + r.height);
               }
               break;
            case 2:
               double val = MoveWidget.getCenterY(this.widgets, this.artisan);

               for (int i = 0; i < this.widgets.length; i++) {
                  RectGeom r = this.artisan.bounds(this.widgets[i]);
                  this.deltaY[i] = val - (r.y + r.height / 2.0);
               }
         }
      }

      Align.Artifact artifact = new Align.Artifact();
      artifact.redo();
      return artifact;
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         String[] props = new String[Align.this.widgets.length];

         for (int i = 0; i < Align.this.widgets.length; i++) {
            props[i] = Align.this.artisan.move(Align.this.widgets[i], Align.this.deltaX[i], Align.this.deltaY[i]);
         }

         this.update(props);
      }

      public void undo() throws Exception {
         String[] props = new String[Align.this.widgets.length];

         for (int i = 0; i < Align.this.widgets.length; i++) {
            props[i] = Align.this.artisan.move(Align.this.widgets[i], -Align.this.deltaX[i], -Align.this.deltaY[i]);
         }

         this.update(props);
      }

      private void update(String[] props) {
         Align.this.editorPane.getSelectedWidgets().setWidgets(Align.this.widgets);
         Align.this.editorPane.getPxEditor().firePxEvent(EventUtil.widgetsChanged(Align.this.widgets, props));
      }
   }
}
