package com.tridium.px.editor.studio;

import javax.baja.gx.Graphics;

public interface PainterStudio {
   void buffer();

   void unbuffer();

   void paintBuffer(Graphics var1);

   void paintPage(Graphics var1);

   void paintSelected(Graphics var1);

   void paintDrag(Graphics var1);
}
