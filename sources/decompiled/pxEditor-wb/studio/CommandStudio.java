package com.tridium.px.editor.studio;

import com.tridium.px.editor.studio.painters.Painter;
import javax.baja.ui.pane.BPane;

public interface CommandStudio {
   BPane getCurrentFreeForm();

   Painter getPainter();
}
