package com.tridium.px.editor.studio.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.util.EventUtil;
import javax.baja.nre.util.SortUtil;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class Distribute extends Command {
   private BPxEditorPane editorPane;
   private int type;
   private Artisan artisan = Artisan.instance();
   private BWidget[] widgets;
   private double[] deltaX;
   private double[] deltaY;
   public static final int HORIZ = 0;
   public static final int VERT = 1;

   public Distribute(BPxEditorPane editorPane, int type) {
      super(editorPane, BPxEditorPane.lexicon(), type == 0 ? "commands.distribute.horizontal" : "commands.distribute.vertical");
      this.editorPane = editorPane;
      this.type = type;
   }

   public CommandArtifact doInvoke() throws Exception {
      this.widgets = this.editorPane.getSelectedWidgets().getWidgets();
      this.calculateDeltas();
      Distribute.Artifact artifact = new Distribute.Artifact();
      artifact.redo();
      return artifact;
   }

   private void calculateDeltas() {
      int len = this.widgets.length;
      this.deltaX = new double[len];
      this.deltaY = new double[len];
      if (this.type == 0) {
         double sumWidth = 0.0;
         BWidget highWidget = null;
         BWidget lowWidget = null;

         for (int i = 0; i < len; i++) {
            if (lowWidget == null || lowWidget.getX() > this.widgets[i].getX()) {
               lowWidget = this.widgets[i];
            }

            if (highWidget == null || highWidget.getX() + highWidget.getWidth() < this.widgets[i].getX() + this.widgets[i].getWidth()) {
               highWidget = this.widgets[i];
            }

            sumWidth += this.widgets[i].getWidth();
         }

         double sumSpace = highWidget.getX() + highWidget.getWidth() - lowWidget.getX();
         double freespace = (sumSpace - sumWidth) / (len - 1);
         SortUtil.sort(this.widgets, this.widgets, (arg0, arg1) -> Double.compare(arg0.getX(), arg1.getX()));
         if (this.widgets[0].getX() > this.widgets[len - 1].getX()) {
            double x = highWidget.getX() + highWidget.getWidth();

            for (int i = 0; i < len; i++) {
               this.deltaX[i] = Math.round((float)(x - (this.widgets[i].getX() + this.widgets[i].getWidth())));
               x -= this.widgets[i].getWidth() + freespace;
            }
         } else {
            double x = lowWidget.getX();

            for (int i = 0; i < len; i++) {
               this.deltaX[i] = Math.round((float)(x - this.widgets[i].getX()));
               x += this.widgets[i].getWidth() + freespace;
            }
         }
      } else {
         double sumHeight = 0.0;
         BWidget highWidget = null;
         BWidget lowWidget = null;

         for (int i = 0; i < len; i++) {
            if (lowWidget == null || lowWidget.getY() > this.widgets[i].getY()) {
               lowWidget = this.widgets[i];
            }

            if (highWidget == null || highWidget.getY() + highWidget.getHeight() < this.widgets[i].getY() + this.widgets[i].getHeight()) {
               highWidget = this.widgets[i];
            }

            sumHeight += this.widgets[i].getHeight();
         }

         double sumSpace = highWidget.getY() + highWidget.getHeight() - lowWidget.getY();
         double freespace = (sumSpace - sumHeight) / (len - 1);
         SortUtil.sort(this.widgets, this.widgets, (arg0, arg1) -> Double.compare(arg0.getY(), arg1.getY()));
         if (this.widgets[0].getY() > this.widgets[len - 1].getY()) {
            double y = highWidget.getY() + highWidget.getHeight();

            for (int i = 0; i < len; i++) {
               this.deltaY[i] = Math.round((float)(y - (this.widgets[i].getY() + this.widgets[i].getHeight())));
               y -= this.widgets[i].getHeight() + freespace;
            }
         } else {
            double y = lowWidget.getY();

            for (int i = 0; i < len; i++) {
               this.deltaY[i] = Math.round((float)(y - this.widgets[i].getY()));
               y += this.widgets[i].getHeight() + freespace;
            }
         }
      }
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         String[] props = new String[Distribute.this.widgets.length];

         for (int i = 0; i < Distribute.this.widgets.length; i++) {
            props[i] = Distribute.this.artisan.move(Distribute.this.widgets[i], Distribute.this.deltaX[i], Distribute.this.deltaY[i]);
         }

         this.update(props);
      }

      public void undo() throws Exception {
         String[] props = new String[Distribute.this.widgets.length];

         for (int i = 0; i < Distribute.this.widgets.length; i++) {
            props[i] = Distribute.this.artisan.move(Distribute.this.widgets[i], -Distribute.this.deltaX[i], -Distribute.this.deltaY[i]);
         }

         this.update(props);
      }

      private void update(String[] props) {
         Distribute.this.editorPane.getSelectedWidgets().setWidgets(Distribute.this.widgets);
         Distribute.this.editorPane.getPxEditor().firePxEvent(EventUtil.widgetsChanged(Distribute.this.widgets, props));
      }
   }
}
