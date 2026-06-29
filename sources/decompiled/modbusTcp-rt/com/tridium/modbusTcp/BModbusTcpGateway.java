package com.tridium.modbusTcp;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.messages.ModbusMessage;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusTcp.comm.ModbusTcpComm;
import com.tridium.modbusTcp.comm.ModbusTcpRxDriver;
import java.net.InetAddress;
import javax.baja.agent.AgentList;
import javax.baja.driver.BDevice;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "ipAddress",
      type = "String",
      defaultValue = "ModbusMessageConst.DEFAULT_IP"
   ), @NiagaraProperty(
      name = "port",
      type = "int",
      defaultValue = "502",
      facets = {@Facet("BFacets.makeInt(null, 0, 65535)")}
   ), @NiagaraProperty(
      name = "socketStatus",
      type = "BSocketStatusEnum",
      defaultValue = "BSocketStatusEnum.closed",
      flags = 3
   ), @NiagaraProperty(
      name = "rxProcessMode",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.makeBoolean(\"packet\", \"byte\")")}
   )})
public class BModbusTcpGateway extends BModbusTcpNetwork {
   public static final Property ipAddress = newProperty(0, "###.###.###.###", null);
   public static final Property port = newProperty(0, 502, BFacets.makeInt(null, 0, 65535));
   public static final Property socketStatus = newProperty(3, BSocketStatusEnum.closed, null);
   public static final Property rxProcessMode = newProperty(0, false, BFacets.makeBoolean("packet", "byte"));
   public static final Type TYPE = Sys.loadType(BModbusTcpGateway.class);

   public String getIpAddress() {
      return this.getString(ipAddress);
   }

   public void setIpAddress(String v) {
      this.setString(ipAddress, v, null);
   }

   public int getPort() {
      return this.getInt(port);
   }

   public void setPort(int v) {
      this.setInt(port, v, null);
   }

   public BSocketStatusEnum getSocketStatus() {
      return (BSocketStatusEnum)this.get(socketStatus);
   }

   public void setSocketStatus(BSocketStatusEnum v) {
      this.set(socketStatus, v, null);
   }

   public boolean getRxProcessMode() {
      return this.getBoolean(rxProcessMode);
   }

   public void setRxProcessMode(boolean v) {
      this.setBoolean(rxProcessMode, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public Type getDeviceType() {
      return BModbusTcpGatewayDevice.TYPE;
   }

   @Override
   public Type getDeviceFolderType() {
      return BModbusTcpGatewayDeviceFolder.TYPE;
   }

   @Override
   protected Comm makeComm() {
      return new ModbusTcpComm(this);
   }

   @Override
   protected boolean initializeNetwork() {
      if (this.networkInitialized) {
         return true;
      } else {
         this.commActive = true;
         if (!this.getComm().isCommStarted()) {
            if (this.getModbusLog().isTraceOn()) {
               this.getModbusLog().warning("Unable to start TCP/IP Comm for " + this);
            }

            this.commActive = false;
            return false;
         } else {
            this.networkInitialized = true;
            return true;
         }
      }
   }

   @Override
   public boolean isCommActive() {
      return this.getComm().isCommStarted() && this.commActive && !this.isDisabled() && !this.isDown() && !this.isFatalFault();
   }

   public InetAddress getInetAddr() {
      InetAddress newINet;
      try {
         String url = this.getIpAddress().trim();
         if (!url.equalsIgnoreCase("###.###.###.###") && url.length() != 0) {
            newINet = InetAddress.getByName(url);
         } else {
            newINet = null;
         }
      } catch (Exception var3) {
         newINet = null;
      }

      return newINet;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);

      try {
         if ((prop.equals(ipAddress) || prop.equals(port)) && this.getComm() != null && this.getComm().isCommStarted()) {
            long ticks = Clock.ticks();

            while (this.getSocketStatus().equals(BSocketStatusEnum.openPending)) {
               try {
                  Thread.sleep(250L);
               } catch (Exception var9) {
               }

               if (Clock.ticks() - ticks > 30000L) {
                  break;
               }
            }

            this.getComm().stop();
            InetAddress ipaddr = this.getInetAddr();
            if (ipaddr == null && this.getModbusLog() != null) {
               this.getModbusLog().error("\nCould not resolve IP address [" + this.getIpAddress() + "].");
            }

            try {
               Thread.sleep(100L);
            } catch (Exception var8) {
               var8.printStackTrace();
            }

            this.getComm().start();
            BDevice[] devices = this.getDevices();

            for (int i = 0; i < devices.length; i++) {
               devices[i].ping();
            }
         }
      } catch (Exception var10) {
         if (this.getModbusLog() != null) {
            this.getModbusLog().error("Caught exception in BModbusTcpGateway.changed()", var10);
         }
      }
   }

   public Message sendSync(Message msg, BRelTime responseTimeout, int retryCount) {
      if (!this.isCommActive()) {
         return null;
      } else {
         int socketStatus = this.getSocketStatus().getOrdinal();
         if (socketStatus == 0 || socketStatus == 2) {
            ((ModbusTcpRxDriver)this.getComm().getCommReceiver()).initSocketConnection();
            socketStatus = this.getSocketStatus().getOrdinal();
         }

         Message response = null;
         if (socketStatus == 3) {
            response = super.sendSync(msg, responseTimeout, retryCount);
         }

         try {
            BModbusClientDevice destDevice = this.findDeviceInNetwork(((ModbusMessage)msg).deviceAddress);
            if (destDevice != null) {
               if (response != null && ((ModbusResponse)response).exceptionCode != 10 && ((ModbusResponse)response).exceptionCode != 11) {
                  destDevice.pingOk();
                  destDevice.resetPingsFailed();
               } else if (destDevice.incrementPingsFailed() > this.getMaxFailsUntilDeviceDown()) {
                  String cause = this.getLexicon().getText("pingFail");
                  if (response != null) {
                     cause = cause + ": " + ((ModbusResponse)response).getExceptionString();
                  }

                  destDevice.pingFail(cause);
               }
            }
         } catch (Exception var8) {
         }

         return response;
      }
   }

   public AgentList getAgents(Context cx) {
      TypeInfo deviceManager = Sys.getRegistry().getType("modbusTcp:ModbusTcpDeviceManager");
      AgentList agents = super.getAgents(cx);
      agents.remove(deviceManager.getAgentInfo());
      return agents;
   }
}
