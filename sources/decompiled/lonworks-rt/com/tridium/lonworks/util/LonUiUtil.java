package com.tridium.lonworks.util;

import javax.baja.sys.BComponent;

public final class LonUiUtil {
   public static void loadComponentSlots(BComponent c, int maxDepth) {
      c.getComponentSpace().update(c, 1);
   }
}
