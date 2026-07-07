package com.tridium.px.editor.studio;

import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BLabelPane;
import javax.baja.ui.pane.BPane;

public interface TreeStudio {
   BLabelPane[] makeTabs(BWidget[] var1);

   void dropWidgetsInCanvas(double var1, double var3, BWidget[] var5, int var6);

   BCanvasPane getCurrentCanvas();

   BPane getCurrentFreeForm();

   boolean alignable(BCanvasPane var1);

   boolean distributable(BCanvasPane var1);

   boolean reorgable(BPane var1);
}
