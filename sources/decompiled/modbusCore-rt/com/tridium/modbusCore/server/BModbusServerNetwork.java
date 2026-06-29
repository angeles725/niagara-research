package com.tridium.modbusCore.server;

import com.tridium.modbusCore.BModbusNetwork;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import java.util.HashSet;
import java.util.Set;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;

@NiagaraType
public abstract class BModbusServerNetwork extends BModbusNetwork implements ModbusMessageConst {
   public static final Type TYPE = Sys.loadType(BModbusServerNetwork.class);
   private Set<Integer> deviceAddressSet = new HashSet<>();
   protected boolean commActive = false;
   protected boolean networkInitialized = false;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BModbusServerNetwork() {
      this.setFlags(retryCount, 4);
      this.setFlags(responseTimeout, 4);
   }

   public void startComm() throws Exception {
      super.startComm();
      if (!this.isDisabled() && !this.isDown() && !this.isFault()) {
         if (this.getModbusLog().isTraceOn()) {
            this.getModbusLog().trace("*** Starting comm for " + this.getName() + " ***");
         }

         if (!this.initializeNetwork()) {
            this.getModbusLog().warning("Unable to initialize network " + this.getName() + "!!");
         }
      }
   }

   public void stopComm() throws Exception {
      super.stopComm();
      if (this.getModbusLog().isTraceOn()) {
         this.getModbusLog().trace("*** Stopping comm for " + this.getName() + " ***");
      }

      this.cleanupNetwork();
   }

   protected boolean initializeNetwork() {
      if (this.networkInitialized) {
         return true;
      } else {
         this.startHelpers();
         this.networkInitialized = true;
         return true;
      }
   }

   protected void cleanupNetwork() {
      if (this.networkInitialized) {
         this.stopHelpers();
         this.networkInitialized = false;
      }
   }

   protected void startHelpers() {
      if (this.getModbusLog().isTraceOn()) {
         this.getModbusLog().trace(this.getName() + ".startHelpers()");
      }

      this.commActive = true;
   }

   protected void stopHelpers() {
      if (this.getModbusLog().isTraceOn()) {
         this.getModbusLog().trace(this.getName() + ".stopHelpers()");
      }

      this.commActive = false;
   }

   public boolean isCommActive() {
      return this.commActive && super.isCommActive();
   }

   public void removed(Property property, BValue oldValue, Context context) {
      super.removed(property, oldValue, context);
      if (oldValue instanceof BModbusServerDevice) {
         this.deleteSlaveDevice((BComponent)oldValue);
      }

      if (oldValue instanceof BFolder) {
         this.searchAndDeleteSlaveDevice((BComponent)oldValue);
      }
   }

   private void searchAndDeleteSlaveDevice(BComponent bComponent) {
      BComponent[] device = ((BFolder)bComponent).getChildComponents();

      for (int i = 0; i < device.length; i++) {
         if (device[i] instanceof BModbusServerDevice) {
            this.deleteSlaveDevice(device[i]);
         }

         if (device[i] instanceof BFolder) {
            this.searchAndDeleteSlaveDevice(device[i]);
         }
      }
   }

   private void deleteSlaveDevice(BComponent device) {
      if (((BModbusServerDevice)device).getStatus() != BStatus.fault || !((BModbusServerDevice)device).getFaultCause().contains("Duplicate Device Address")) {
         this.removeDeviceAddress(((BModbusServerDevice)device).getDeviceAddress());
      }
   }

   public void removeDeviceAddress(int deviceAdd) {
      this.deviceAddressSet.remove(deviceAdd);
   }

   public void addDeviceAddress(int deviceAdd) {
      this.deviceAddressSet.add(deviceAdd);
   }

   public boolean isUniqueDeviceAddress(int deviceAdd) {
      return !this.deviceAddressSet.contains(deviceAdd);
   }
}
