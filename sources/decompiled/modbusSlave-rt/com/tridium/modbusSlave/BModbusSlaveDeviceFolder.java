package com.tridium.modbusSlave;

import javax.baja.driver.BDeviceFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusSlaveDeviceFolder extends BDeviceFolder {
   public static final Type TYPE = Sys.loadType(BModbusSlaveDeviceFolder.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusSlaveNetwork || parent instanceof BModbusSlaveDeviceFolder;
   }
}
