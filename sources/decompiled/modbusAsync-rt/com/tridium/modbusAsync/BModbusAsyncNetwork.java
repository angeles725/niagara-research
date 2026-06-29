package com.tridium.modbusAsync;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.basicdriver.message.Message;
import com.tridium.modbusAsync.comm.ModbusAsyncSerialComm;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.enums.BModbusDataModeEnum;
import com.tridium.modbusCore.messages.ModbusMessage;
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
import javax.baja.sys.Context;
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
      name = "maxRxInterCharacterDelay",
      type = "BRelTime",
      defaultValue = "BRelTime.make(50)",
      flags = 4,
      facets = {@Facet("BFacets.make(BFacets.SHOW_MILLISECONDS, BBoolean.TRUE, BFacets.MIN, BRelTime.make(0), BFacets.MAX, BRelTime.SECOND)")}
   ), @NiagaraProperty(
      name = "minRxFrameEnd",
      type = "BRelTime",
      defaultValue = "BRelTime.make(20)",
      flags = 4,
      facets = {@Facet("BFacets.make(BFacets.SHOW_MILLISECONDS, BBoolean.TRUE, BFacets.MIN, BRelTime.make(0), BFacets.MAX, BRelTime.SECOND)")}
   ), @NiagaraProperty(
      name = "rxPriority",
      type = "boolean",
      defaultValue = "false",
      flags = 4
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
   ), @NiagaraProperty(
      name = "rtuSnifferModeBufferSize",
      type = "int",
      defaultValue = "8",
      facets = {@Facet("BFacets.makeInt(null, 1, Integer.MAX_VALUE)")}
   )})
public class BModbusAsyncNetwork extends BModbusClientNetwork implements BISerialHelperParent {
   public static final Property interMessageDelay = newProperty(
      0, BRelTime.make(0L), BFacets.make("showMilliseconds", BBoolean.TRUE, "min", BRelTime.make(0L), "max", BRelTime.SECOND)
   );
   public static final Property maxRxInterCharacterDelay = newProperty(
      4, BRelTime.make(50L), BFacets.make("showMilliseconds", BBoolean.TRUE, "min", BRelTime.make(0L), "max", BRelTime.SECOND)
   );
   public static final Property minRxFrameEnd = newProperty(
      4, BRelTime.make(20L), BFacets.make("showMilliseconds", BBoolean.TRUE, "min", BRelTime.make(0L), "max", BRelTime.SECOND)
   );
   public static final Property rxPriority = newProperty(4, false, null);
   public static final Property serialPortConfig = newProperty(0, new BSerialHelper(), null);
   public static final Property modbusDataMode = newProperty(0, BModbusDataModeEnum.rtu, null);
   public static final Property snifferMode = newProperty(0, false, null);
   public static final Property rtuSnifferModeBufferSize = newProperty(0, 8, BFacets.makeInt(null, 1, Integer.MAX_VALUE));
   public static final Type TYPE = Sys.loadType(BModbusAsyncNetwork.class);
   private Log log = null;
   private boolean commActive = false;
   private boolean networkInitialized = false;

   public BRelTime getInterMessageDelay() {
      return (BRelTime)this.get(interMessageDelay);
   }

   public void setInterMessageDelay(BRelTime v) {
      this.set(interMessageDelay, v, null);
   }

   public BRelTime getMaxRxInterCharacterDelay() {
      return (BRelTime)this.get(maxRxInterCharacterDelay);
   }

   public void setMaxRxInterCharacterDelay(BRelTime v) {
      this.set(maxRxInterCharacterDelay, v, null);
   }

   public BRelTime getMinRxFrameEnd() {
      return (BRelTime)this.get(minRxFrameEnd);
   }

   public void setMinRxFrameEnd(BRelTime v) {
      this.set(minRxFrameEnd, v, null);
   }

   public boolean getRxPriority() {
      return this.getBoolean(rxPriority);
   }

   public void setRxPriority(boolean v) {
      this.setBoolean(rxPriority, v, null);
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

   public int getRtuSnifferModeBufferSize() {
      return this.getInt(rtuSnifferModeBufferSize);
   }

   public void setRtuSnifferModeBufferSize(int v) {
      this.setInt(rtuSnifferModeBufferSize, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning() && p.equals(rxPriority)) {
         Comm comm = this.getComm();
         if (comm instanceof ModbusAsyncSerialComm) {
            Thread rxThread = ((ModbusAsyncSerialComm)comm).getRxThread();
            if (rxThread != null) {
               if (this.getRxPriority()) {
                  rxThread.setPriority(7);
               } else {
                  rxThread.setPriority(5);
               }
            }
         }
      }
   }

   public Type getDeviceType() {
      return BModbusAsyncDevice.TYPE;
   }

   public Type getDeviceFolderType() {
      return BModbusAsyncDeviceFolder.TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "modbusAsync");
   }

   public BModbusAsyncNetwork() {
      this.setResponseTimeout(BRelTime.make(1000L));
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

      if (!this.isDisabled() && !this.isDown() && !this.isFault()) {
         if (this.getModbusLog().isTraceOn()) {
            this.getModbusLog().trace("*** Starting serial comm for " + this.getName() + " ***");
         }

         if (!this.initializeNetwork()) {
            this.getModbusLog().warning("Unable to initialize network " + this.getName() + "!!");
         }
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
      return new ModbusAsyncSerialComm(this);
   }

   public void stopComm() throws Exception {
      super.stopComm();
      this.commActive = false;
      this.networkInitialized = false;
   }

   protected void updateLog() {
      String serialLogName = this.getName() + "_" + this.getSerialPortConfig().getPortName();
      if (!SlotPath.isValidName(serialLogName)) {
         serialLogName = SlotPath.escape(serialLogName);
      }

      Log newLog = Log.getLog(serialLogName);
      if (this.log == null) {
         this.log = this.getLog();
      }

      synchronized (this.log) {
         if (this.log != null) {
            newLog.setSeverity(this.log.getSeverity());
            if (!newLog.getLogName().equals(this.log.getLogName())) {
               Log.deleteLog(this.log.getLogName());
            }
         }

         this.log = newLog;
      }

      super.updateLog();
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
         this.getLog().error("BModbusAsyncNetwork caught exception in reopenPort(): ", var2);
      }
   }

   private void restartSerialNetwork() throws Exception {
      if (!this.isDisabled() && !this.isDown() && !this.isFatalFault()) {
         if (this.log.isTraceOn()) {
            this.log.trace(this.getName() + " *** Restarting serial comm ***");
         }

         this.stopComm();
         this.startComm();
      }
   }

   private boolean initializeNetwork() {
      if (this.networkInitialized) {
         return true;
      } else {
         this.commActive = true;
         if (!this.getComm().isCommStarted()) {
            this.getModbusLog().warning("Unable to start Serial Comm for " + this.getName());
            this.commActive = false;
            return false;
         } else {
            try {
               ((ModbusAsyncSerialComm)this.getComm()).getSerialPort().enableReceiveThreshold(1);
            } catch (UnsupportedOperationException var2) {
               this.getModbusLog().error(this.getName() + ", ModbusAsyncSerialComm: Unable to perform enableReceiveThreshold = 1.  Exception- ", var2);
            }

            this.networkInitialized = true;
            return true;
         }
      }
   }

   public boolean isCommActive() {
      return this.commActive && super.isCommActive();
   }

   public Message sendSync(Message msg, BRelTime responseTimeout, int retryCount) {
      if (!this.getSnifferMode() && this.isCommActive()) {
         Message response = super.sendSync(msg, responseTimeout, retryCount);

         try {
            BModbusClientDevice destDevice = this.findDeviceInNetwork(((ModbusMessage)msg).deviceAddress);
            if (destDevice != null) {
               if (response != null) {
                  destDevice.pingOk();
                  destDevice.resetPingsFailed();
               } else if (destDevice.incrementPingsFailed() > this.getMaxFailsUntilDeviceDown()) {
                  destDevice.pingFail(this.getLexicon().getText("pingFail"));
               }
            }
         } catch (Exception var6) {
         }

         return response;
      } else {
         return null;
      }
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
}
