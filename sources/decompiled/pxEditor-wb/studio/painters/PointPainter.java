package com.tridium.px.editor.studio.painters;

import com.tridium.px.editor.studio.PainterStudio;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.gx.Graphics;
import javax.baja.ui.BWidget;

public class PointPainter extends Painter {
   private PainterStudio studio;
   private SelectedWidgets selected;

   public PointPainter(PainterStudio studio, SelectedWidgets selected) {
      this.studio = studio;
      this.selected = selected;
      studio.buffer();
   }

   @Override
   public synchronized void doPaint(Graphics g) {
      this.studio.paintBuffer(g);
      Artisan.smallHandles();
      this.studio.paintSelected(g);
      Artisan.largeHandles();
   }

   @Override
   public void reset() {
      this.studio.unbuffer();
      BWidget[] sel = this.selected.getWidgets();
      this.selected.deselectAll();
      this.studio.buffer();
      this.selected.setWidgets(sel);
   }
}
