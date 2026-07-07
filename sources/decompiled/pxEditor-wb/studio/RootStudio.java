package com.tridium.px.editor.studio;

import javax.baja.gx.Point;
import javax.baja.ui.BWidget;

public interface RootStudio {
   void repaint();

   void repaint(double var1, double var3, double var5, double var7);

   Point translateToRoot(BWidget var1, Point var2);
}
