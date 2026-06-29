package com.tridium.fox.sys;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BServerConnections extends BComponent {
   public static final Type TYPE = Sys.loadType(BServerConnections.class);
   private static final BIcon icon = BIcon.std("connections.png");

   public Type getType() {
      return TYPE;
   }

   public BIcon getIcon() {
      return icon;
   }
}
