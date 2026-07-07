package com.tridium.px.editor.studio.painters;

import com.tridium.px.editor.studio.PainterStudio;
import javax.baja.gx.Graphics;

public class DefaultPainter extends Painter {
   private PainterStudio studio;

   public DefaultPainter(PainterStudio studio) {
      this.studio = studio;
      studio.unbuffer();
   }

   @Override
   public synchronized void doPaint(Graphics g) {
      this.studio.paintPage(g);
      this.studio.paintSelected(g);
      this.studio.paintDrag(g);
   }
}
