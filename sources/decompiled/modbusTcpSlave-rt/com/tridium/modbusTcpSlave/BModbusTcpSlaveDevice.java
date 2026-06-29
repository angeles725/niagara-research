package com.tridium.modbusTcpSlave;

import com.tridium.modbusCore.server.BModbusServerDevice;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "deviceAddress",
   type = "int",
   defaultValue = "1",
   facets = {@Facet("BFacets.makeInt(null, 0, 255)")},
   override = true
)
public class BModbusTcpSlaveDevice extends BModbusServerDevice {
   public static final Property deviceAddress = newProperty(0, 1, BFacets.makeInt(null, 0, 255));
   public static final Type TYPE = Sys.loadType(BModbusTcpSlaveDevice.class);

   public Type getType() {
      return TYPE;
   }

   public int getModbusMode() {
      return 2;
   }

   public Type getNetworkType() {
      return BModbusTcpSlaveNetwork.TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusTcpSlaveNetwork || parent instanceof BModbusTcpSlaveDeviceFolder;
   }
}
