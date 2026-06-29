package com.tridium.modbusTcpSlave;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.modbusCore.server.BModbusServerNetwork;
import com.tridium.modbusTcpSlave.comm.ModbusTcpServer;
import com.tridium.modbusTcpSlave.comm.ModbusUnsolicitedReceive;
import com.tridium.nre.firewall.IpProtocol;
import javax.baja.firewall.BServerPort;
import javax.baja.license.Feature;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "port",
      type = "BServerPort",
      defaultValue = "new BServerPort(502, IpProtocol.TCP)"
   ), @NiagaraProperty(
      name = "socketTimeoutInMillis",
      type = "int",
      defaultValue = "30000",
      facets = {@Facet("BFacets.makeInt(null, 30000, Integer.MAX_VALUE)")}
   ), @NiagaraProperty(
      name = "maximumConnections",
      type = "int",
      defaultValue = "5",
      facets = {@Facet("BFacets.makeInt(null, 1, 100)")}
   ), @NiagaraProperty(
      name = "currentConnections",
      type = "int",
      defaultValue = "0",
      flags = 3
   )})
public class BModbusTcpSlaveNetwork extends BModbusServerNetwork {
   public static final Property port = newProperty(0, new BServerPort(502, IpProtocol.TCP), null);
   public static final Property socketTimeoutInMillis = newProperty(0, 30000, BFacets.makeInt(null, 30000, Integer.MAX_VALUE));
   public static final Property maximumConnections = newProperty(0, 5, BFacets.makeInt(null, 1, 100));
   public static final Property currentConnections = newProperty(3, 0, null);
   public static final Type TYPE = Sys.loadType(BModbusTcpSlaveNetwork.class);
   private ModbusUnsolicitedReceive unsolicitedReceive = null;
   private ModbusTcpServer tcpServer = null;

   public BServerPort getPort() {
      return (BServerPort)this.get(port);
   }

   public void setPort(BServerPort v) {
      this.set(port, v, null);
   }

   public int getSocketTimeoutInMillis() {
      return this.getInt(socketTimeoutInMillis);
   }

   public void setSocketTimeoutInMillis(int v) {
      this.setInt(socketTimeoutInMillis, v, null);
   }

   public int getMaximumConnections() {
      return this.getInt(maximumConnections);
   }

   public void setMaximumConnections(int v) {
      this.setInt(maximumConnections, v, null);
   }

   public int getCurrentConnections() {
      return this.getInt(currentConnections);
   }

   public void setCurrentConnections(int v) {
      this.setInt(currentConnections, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getDeviceType() {
      return BModbusTcpSlaveDevice.TYPE;
   }

   public Type getDeviceFolderType() {
      return BModbusTcpSlaveDeviceFolder.TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "modbusTcpSlave");
   }

   public int getModbusMode() {
      return 2;
   }

   public void serviceStarted() throws Exception {
      super.serviceStarted();
      this.buildComm();
   }

   private void buildComm() {
      try {
         this.tcpServer = new ModbusTcpServer(this);
         this.unsolicitedReceive = new ModbusUnsolicitedReceive(this);
         this.unsolicitedReceive.init();
      } catch (Exception var2) {
         this.getModbusLog().error("Exception generated in BModbusTcpSlaveNetwork.initSerialComm(): ", var2);
      }
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
   }

   public void stopComm() throws Exception {
      super.stopComm();
      this.tcpServer.stop();
   }

   protected Comm makeComm() {
      return null;
   }

   public void atSteadyState() {
      this.unsolicitedReceive.atSteadyState();
   }

   protected boolean initializeNetwork() {
      if (this.networkInitialized) {
         return true;
      } else {
         this.tcpServer.start();
         this.startHelpers();
         if (!this.tcpServer.isStarted()) {
            this.getModbusLog().warning("Unable to start TCP/IP Comm for " + this.getName());
            this.stopHelpers();
            return false;
         } else {
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

   public boolean isCommActive() {
      return this.commActive && !this.isDisabled() && !this.isDown() && !this.isFault() && this.tcpServer.isStarted();
   }

   public Log getLog() {
      return this.getModbusLog();
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (prop.equals(maximumConnections)) {
         if (!this.isRunning()) {
            return;
         }

         if (this.tcpServer != null && this.tcpServer.isStarted()) {
            this.tcpServer.removeOldSessions(this.getMaximumConnections());
         }
      } else if (prop.equals(port) || prop.equals(socketTimeoutInMillis)) {
         if (!this.isRunning()) {
            return;
         }

         if (this.tcpServer != null && this.tcpServer.isStarted()) {
            this.tcpServer.stop();
            this.tcpServer.start();
         }
      }
   }

   public ModbusUnsolicitedReceive unsolicitedReceive() {
      return this.unsolicitedReceive;
   }

   public ModbusTcpServer tcpServer() {
      return this.tcpServer;
   }
}
