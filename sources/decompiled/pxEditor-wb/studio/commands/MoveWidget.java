package com.tridium.px.editor.studio.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.util.EventUtil;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class MoveWidget extends Command {
   private BPxEditorPane editorPane;
   private BWidget[] widgets;
   private double deltaX;
   private double deltaY;
   private MoveWidget.Artifact artifact = new MoveWidget.Artifact();

   public MoveWidget(BPxEditorPane editorPane, BWidget[] widgets, double deltaX, double deltaY) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.move");
      this.editorPane = editorPane;
      this.widgets = widgets;
      this.deltaX = deltaX;
      this.deltaY = deltaY;
   }

   public CommandArtifact doInvoke() throws Exception {
      this.artifact.redo();
      return this.artifact;
   }

   public static double getMinX(BWidget[] widgets, Artisan a) {
      RectGeom r = a.bounds(widgets[0]);
      double val = r.x;

      for (int i = 1; i < widgets.length; i++) {
         r = a.bounds(widgets[i]);
         val = Math.min(val, r.x);
      }

      return val;
   }

   public static double getMaxX(BWidget[] widgets, Artisan a) {
      RectGeom r = a.bounds(widgets[0]);
      double val = r.x + r.width;

      for (int i = 1; i < widgets.length; i++) {
         r = a.bounds(widgets[i]);
         val = Math.max(val, r.x + r.width);
      }

      return val;
   }

   public static double getCenterX(BWidget[] widgets, Artisan a) {
      RectGeom r = a.bounds(widgets[0]);
      double min = r.x;
      double max = r.x + r.width;

      for (int i = 1; i < widgets.length; i++) {
         r = a.bounds(widgets[i]);
         min = Math.min(r.x, min);
         max = Math.max(r.x + r.width, max);
      }

      return (max + min) / 2.0;
   }

   public static double getMinY(BWidget[] widgets, Artisan a) {
      RectGeom r = a.bounds(widgets[0]);
      double val = r.y;

      for (int i = 1; i < widgets.length; i++) {
         r = a.bounds(widgets[i]);
         val = Math.min(val, r.y);
      }

      return val;
   }

   public static double getMaxY(BWidget[] widgets, Artisan a) {
      RectGeom r = a.bounds(widgets[0]);
      double val = r.y + r.height;

      for (int i = 1; i < widgets.length; i++) {
         r = a.bounds(widgets[i]);
         val = Math.max(val, r.y + r.height);
      }

      return val;
   }

   public static double getCenterY(BWidget[] widgets, Artisan a) {
      RectGeom r = a.bounds(widgets[0]);
      double min = r.y;
      double max = r.y + r.height;

      for (int i = 1; i < widgets.length; i++) {
         r = a.bounds(widgets[i]);
         min = Math.min(r.y, min);
         max = Math.max(r.y + r.height, max);
      }

      return (max + min) / 2.0;
   }

   public static String[] shiftWidgets(BWidget[] widgets, Artisan a, double dx, double dy) {
      String[] props = new String[widgets.length];

      for (int i = 0; i < widgets.length; i++) {
         props[i] = a.move(widgets[i], dx, dy);
      }

      return props;
   }

   public static void toZero(BWidget[] widgets, Artisan a) {
      for (int i = 0; i < widgets.length; i++) {
         a.zero(widgets[i]);
      }
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         this.doCmd(MoveWidget.this.deltaX, MoveWidget.this.deltaY);
      }

      public void undo() throws Exception {
         this.doCmd(-MoveWidget.this.deltaX, -MoveWidget.this.deltaY);
      }

      private void doCmd(double dx, double dy) throws Exception {
         Artisan a = Artisan.instance();
         MoveWidget.this.editorPane.getSelectedWidgets().setWidgets(MoveWidget.this.widgets);
         String[] props = MoveWidget.shiftWidgets(MoveWidget.this.widgets, a, dx, dy);

         for (int i = 0; i < MoveWidget.this.widgets.length; i++) {
            MoveWidget.this.widgets[i].getParentWidget().relayout();
         }

         MoveWidget.this.editorPane.getPxEditor().firePxEvent(EventUtil.widgetsChanged(MoveWidget.this.widgets, props));
      }
   }
}
