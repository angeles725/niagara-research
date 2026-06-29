package com.tridium.modbusTcp;

import javax.baja.driver.BDeviceFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusTcpGatewayDeviceFolder extends BDeviceFolder {
   public static final Type TYPE = Sys.loadType(BModbusTcpGatewayDeviceFolder.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusTcpGateway || parent instanceof BModbusTcpGatewayDeviceFolder;
   }
}
