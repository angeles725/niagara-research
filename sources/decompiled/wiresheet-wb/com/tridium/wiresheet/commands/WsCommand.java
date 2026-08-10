package com.tridium.wiresheet.commands;

import com.tridium.wiresheet.BWireSheetPane;
import com.tridium.wiresheet.WsConst;
import javax.baja.ui.Command;

public class WsCommand extends Command implements WsConst {
   protected final BWireSheetPane ws;

   public WsCommand(BWireSheetPane ws, String name) {
      super(ws.getShell(), name);
      this.ws = ws;
   }
}
