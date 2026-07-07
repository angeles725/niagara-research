package com.tridium.px.editor.studio;

import com.tridium.px.editor.studio.painters.Painter;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.ui.BWidget;
import javax.baja.ui.MouseCursor;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BCanvasPane;

public interface TrackerStudio {
   BWidget rootDescendant(Point var1);

   BWidget[] rootDescendants(Point var1);

   Point translateFromRoot(BWidget var1, Point var2);

   Point translateToRoot(BWidget var1, Point var2);

   Point fromViewbox(double var1, double var3, BCanvasPane var5);

   Point toViewbox(double var1, double var3, BCanvasPane var5);

   MouseCursor getMouseCursor();

   MouseCursor setMouseCursor(MouseCursor var1);

   BCanvasPane getCurrentCanvas();

   Point snap(double var1, double var3);

   void selectWidgets(RectGeom var1, BCanvasPane var2);

   void setPainter(Painter var1);

   void setShiftDown(boolean var1);

   void showPopupMenu(BMouseEvent var1);
}
