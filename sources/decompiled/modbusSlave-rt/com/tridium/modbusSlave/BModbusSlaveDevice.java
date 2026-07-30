package com.tridium.modbusSlave;

import com.tridium.modbusCore.server.BModbusServerDevice;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusSlaveDevice extends BModbusServerDevice {
   public static final Type TYPE = Sys.loadType(BModbusSlaveDevice.class);

   public Type getType() {
      return TYPE;
   }

   public Type getNetworkType() {
      return BModbusSlaveNetwork.TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusSlaveNetwork || parent instanceof BModbusSlaveDeviceFolder;
   }

   public int getModbusMode() {
      return ((BModbusSlaveNetwork)this.getNetwork()).getModbusMode();
   }
}
