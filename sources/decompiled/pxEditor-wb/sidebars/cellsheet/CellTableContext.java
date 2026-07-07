package com.tridium.px.editor.sidebars.cellsheet;

import javax.baja.sys.BComponent;

public interface CellTableContext {
   boolean allowAnimate();

   BComponent[] getGroupableComponents();

   void doPaste();
}
