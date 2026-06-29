package com.tridium.modbusCore.client;

import com.tridium.modbusCore.BModbusNetwork;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import javax.baja.driver.BDevice;
import javax.baja.driver.ping.BPingMonitor;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "usePresetMultipleRegister",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "useForceMultipleCoil",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "maxFailsUntilDeviceDown",
      type = "int",
      defaultValue = "2",
      facets = {@Facet("BFacets.makeInt(null, 0, Integer.MAX_VALUE)")}
   )})
public abstract class BModbusClientNetwork extends BModbusNetwork implements ModbusMessageConst {
   public static final Property usePresetMultipleRegister = newProperty(0, false, null);
   public static final Property useForceMultipleCoil = newProperty(0, false, null);
   public static final Property maxFailsUntilDeviceDown = newProperty(0, 2, BFacets.makeInt(null, 0, Integer.MAX_VALUE));
   public static final Type TYPE = Sys.loadType(BModbusClientNetwork.class);

   public boolean getUsePresetMultipleRegister() {
      return this.getBoolean(usePresetMultipleRegister);
   }

   public void setUsePresetMultipleRegister(boolean v) {
      this.setBoolean(usePresetMultipleRegister, v, null);
   }

   public boolean getUseForceMultipleCoil() {
      return this.getBoolean(useForceMultipleCoil);
   }

   public void setUseForceMultipleCoil(boolean v) {
      this.setBoolean(useForceMultipleCoil, v, null);
   }

   public int getMaxFailsUntilDeviceDown() {
      return this.getInt(maxFailsUntilDeviceDown);
   }

   public void setMaxFailsUntilDeviceDown(int v) {
      this.setInt(maxFailsUntilDeviceDown, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.getMonitor().setNumRetriesUntilPingFail(0);
      this.getMonitor().setFlags(BPingMonitor.numRetriesUntilPingFail, this.getFlags(BPingMonitor.numRetriesUntilPingFail) | 4 | 1);
   }

   protected BModbusClientDevice findDeviceInNetwork(int address) {
      BDevice[] devices = this.getDevices();

      for (int i = 0; i < devices.length; i++) {
         if (devices[i] instanceof BModbusClientDevice && ((BModbusClientDevice)devices[i]).getDeviceAddress() == address) {
            return (BModbusClientDevice)devices[i];
         }
      }

      return null;
   }
}
