package com.tridium.modbusTcpSlave;

import javax.baja.driver.BDeviceFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusTcpSlaveDeviceFolder extends BDeviceFolder {
   public static final Type TYPE = Sys.loadType(BModbusTcpSlaveDeviceFolder.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusTcpSlaveNetwork || parent instanceof BModbusTcpSlaveDeviceFolder;
   }
}
