package com.tridium.modbusTcp;

import javax.baja.driver.BDeviceFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusTcpDeviceFolder extends BDeviceFolder {
   public static final Type TYPE = Sys.loadType(BModbusTcpDeviceFolder.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusTcpNetwork && !(parent instanceof BModbusTcpGateway) || parent instanceof BModbusTcpDeviceFolder;
   }
}
