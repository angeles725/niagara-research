package com.tridium.modbusSlave;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.modbusCore.enums.BModbusDataModeEnum;
import com.tridium.modbusCore.server.BModbusServerNetwork;
import com.tridium.modbusSlave.comm.ModbusSlaveSerialComm;
import com.tridium.modbusSlave.comm.ModbusUnsolicitedReceive;
import javax.baja.driver.BDevice;
import javax.baja.license.Feature;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.serial.BISerialHelperParent;
import javax.baja.serial.BSerialHelper;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "interMessageDelay",
      type = "BRelTime",
      defaultValue = "BRelTime.make(0)",
      facets = {@Facet("BFacets.make(BFacets.SHOW_MILLISECONDS, BBoolean.TRUE, BFacets.MIN, BRelTime.make(0), BFacets.MAX, BRelTime.SECOND)")}
   ), @NiagaraProperty(
      name = "serialPortConfig",
      type = "BSerialHelper",
      defaultValue = "new BSerialHelper()"
   ), @NiagaraProperty(
      name = "modbusDataMode",
      type = "BModbusDataModeEnum",
      defaultValue = "BModbusDataModeEnum.rtu"
   ), @NiagaraProperty(
      name = "snifferMode",
      type = "boolean",
      defaultValue = "false"
   )})
public class BModbusSlaveNetwork extends BModbusServerNetwork implements BISerialHelperParent {
   public static final Property interMessageDelay = newProperty(
      0, BRelTime.make(0L), BFacets.make("showMilliseconds", BBoolean.TRUE, "min", BRelTime.make(0L), "max", BRelTime.SECOND)
   );
   public static final Property serialPortConfig = newProperty(0, new BSerialHelper(), null);
   public static final Property modbusDataMode = newProperty(0, BModbusDataModeEnum.rtu, null);
   public static final Property snifferMode = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BModbusSlaveNetwork.class);
   private Log log = null;
   private ModbusUnsolicitedReceive unsolicitedReceive = null;

   public BRelTime getInterMessageDelay() {
      return (BRelTime)this.get(interMessageDelay);
   }

   public void setInterMessageDelay(BRelTime v) {
      this.set(interMessageDelay, v, null);
   }

   public BSerialHelper getSerialPortConfig() {
      return (BSerialHelper)this.get(serialPortConfig);
   }

   public void setSerialPortConfig(BSerialHelper v) {
      this.set(serialPortConfig, v, null);
   }

   public BModbusDataModeEnum getModbusDataMode() {
      return (BModbusDataModeEnum)this.get(modbusDataMode);
   }

   public void setModbusDataMode(BModbusDataModeEnum v) {
      this.set(modbusDataMode, v, null);
   }

   public boolean getSnifferMode() {
      return this.getBoolean(snifferMode);
   }

   public void setSnifferMode(boolean v) {
      this.setBoolean(snifferMode, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getDeviceType() {
      return BModbusSlaveDevice.TYPE;
   }

   public Type getDeviceFolderType() {
      return BModbusSlaveDeviceFolder.TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "modbusSlave");
   }

   public int getModbusMode() {
      return this.getModbusDataMode().getOrdinal();
   }

   public void serviceStarted() throws Exception {
      super.serviceStarted();
      this.getNameSubscriber().subscribe(this.getSerialPortConfig());
      this.getSerialPortConfig().setSerialHelperParent(this);
      if (this.log == null) {
         this.log = this.getLog();
      }

      synchronized (this.log) {
         this.log = this.getLog();
      }
   }

   public void startComm() throws Exception {
      if (this.getSerialPortConfig().getPortName().equals("none")) {
         this.configFail("No port selected for Modbus communication.");
      } else {
         try {
            super.startComm();
         } catch (Exception var2) {
            this.configFail("Could not enable Modbus serial communication (" + var2 + ")");
            throw var2;
         }

         this.configOk();
      }
   }

   public Log getLog() {
      String serialLogName = this.getName() + "_" + this.getSerialPortConfig().getPortName();
      if (!SlotPath.isValidName(serialLogName)) {
         serialLogName = SlotPath.escape(serialLogName);
      }

      return Log.getLog(serialLogName);
   }

   protected Comm makeComm() {
      return new ModbusSlaveSerialComm(this);
   }

   protected void initComm(Comm comm) {
      this.unsolicitedReceive = new ModbusUnsolicitedReceive(this);
      this.unsolicitedReceive.init();
   }

   public void reopenPort() {
      try {
         String newPort = this.getSerialPortConfig().getPortName();
         if (newPort.equals("none")) {
            this.configFail("No port selected for Modbus communication.");
            this.stopComm();
            return;
         }

         this.restartSerialNetwork();
      } catch (Exception var2) {
         this.getLog().warning("BModbusSlaveNetwork caught exception in reopenPort(): " + var2.getLocalizedMessage());
      }
   }

   private void restartSerialNetwork() throws Exception {
      if (!this.isDisabled() && !this.isDown() && !this.isFatalFault()) {
         this.getLog().message(" *** Restarting serial comm ***");
         this.stopComm();
         this.startComm();
      }
   }

   public void atSteadyState() {
      this.unsolicitedReceive.atSteadyState();
   }

   protected boolean initializeNetwork() {
      if (this.networkInitialized) {
         return true;
      } else {
         this.startHelpers();
         if (!this.getComm().isCommStarted()) {
            this.getModbusLog().warning("Unable to start Serial Comm for " + this.getName());
            this.stopHelpers();
            return false;
         } else {
            try {
               ((ModbusSlaveSerialComm)this.getComm()).getSerialPort().enableReceiveThreshold(1);
            } catch (UnsupportedOperationException var2) {
               this.getModbusLog().error(this.getName() + ", ModbusSlaveSerialComm: Unable to perform enableReceiveThreshold = 1.  Exception- ", var2);
            }

            this.networkInitialized = true;
            return true;
         }
      }
   }

   protected void startHelpers() {
      super.startHelpers();
      this.unsolicitedReceive.start();
   }

   protected void stopHelpers() {
      super.stopHelpers();
      this.unsolicitedReceive.stop();
   }

   public ModbusUnsolicitedReceive unsolicitedReceive() {
      return this.unsolicitedReceive;
   }

   protected void processNameSubscriberEvent(BComponentEvent event) {
      super.processNameSubscriberEvent(event);

      try {
         if (event.getId() == 0 && event.getSlot().equals(BSerialHelper.portName)) {
            this.updateLog();
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   public BModbusSlaveDevice findModbusDevice(int address) {
      BDevice[] devices = this.getDevices();

      for (int i = 0; i < devices.length; i++) {
         if (devices[i] != null && devices[i] instanceof BModbusSlaveDevice && ((BModbusSlaveDevice)devices[i]).getDeviceAddress() == address) {
            return (BModbusSlaveDevice)devices[i];
         }
      }

      return null;
   }
}
