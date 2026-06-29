package com.tridium.modbusTcp;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import javax.baja.driver.BDevice;
import javax.baja.license.Feature;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "socketOptionTimeout",
   type = "BRelTime",
   defaultValue = "BRelTime.makeMinutes(1)",
   facets = {@Facet("BFacets.make(BFacets.MIN, BRelTime.make(1))")}
)
public class BModbusTcpNetwork extends BModbusClientNetwork {
   public static final Property socketOptionTimeout = newProperty(0, BRelTime.makeMinutes(1), BFacets.make("min", BRelTime.make(1L)));
   public static final Type TYPE = Sys.loadType(BModbusTcpNetwork.class);
   protected boolean commActive = false;
   protected boolean networkInitialized = false;

   public BRelTime getSocketOptionTimeout() {
      return (BRelTime)this.get(socketOptionTimeout);
   }

   public void setSocketOptionTimeout(BRelTime v) {
      this.set(socketOptionTimeout, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getDeviceType() {
      return BModbusTcpDevice.TYPE;
   }

   public Type getDeviceFolderType() {
      return BModbusTcpDeviceFolder.TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "modbusTcp");
   }

   public BModbusTcpNetwork() {
      this.setResponseTimeout(BRelTime.make(2000L));
   }

   public int getModbusMode() {
      return 2;
   }

   protected Comm makeComm() {
      return null;
   }

   public void startComm() throws Exception {
      super.startComm();
      String modbusLogName = this.getName();
      if (!SlotPath.isValidName(modbusLogName)) {
         modbusLogName = SlotPath.escape(modbusLogName);
      }

      if (this.modbusLog == null) {
         this.modbusLog = this.getModbusLog();
      }

      synchronized (this.modbusLog) {
         this.modbusLog = Log.getLog(modbusLogName);
      }

      if (!this.isDisabled() && !this.isFatalFault() && !this.isDown() && !this.initializeNetwork()) {
         this.getModbusLog().warning("Unable to initialize network " + this.getName() + "!!");
      }
   }

   public void stopComm() throws Exception {
      super.stopComm();
      BDevice[] devices = this.getDevices();

      for (int i = 0; i < devices.length; i++) {
         if (devices[i] instanceof BModbusTcpDevice) {
            ((BModbusTcpDevice)devices[i]).stopComm();
         }
      }

      this.commActive = false;
      this.networkInitialized = false;
   }

   protected boolean initializeNetwork() {
      if (this.networkInitialized) {
         return true;
      } else {
         this.commActive = true;
         this.networkInitialized = true;
         return true;
      }
   }

   public boolean isCommActive() {
      return this.commActive && !this.isDisabled() && !this.isDown() && !this.isFatalFault();
   }

   public Log getLog() {
      return this.getModbusLog();
   }
}
