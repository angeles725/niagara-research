package com.tridium.modbusTcp;

import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusTcp.comm.ModbusTcpComm;
import com.tridium.modbusTcp.comm.ModbusTcpRxDriver;
import com.tridium.modbusTcp.comm.ModbusTcpSendRequest;
import java.net.InetAddress;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceAddress",
      type = "int",
      defaultValue = "1",
      facets = {@Facet("BFacets.makeInt(null, 0, 255)")},
      override = true
   ), @NiagaraProperty(
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
      flags = 67
   ), @NiagaraProperty(
      name = "disableTransactionIdCheck",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "maxTransactionId",
      type = "int",
      defaultValue = "65535",
      facets = {@Facet("BFacets.makeInt(null, 0, 65535)")}
   ), @NiagaraProperty(
      name = "rxProcessMode",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.makeBoolean(\"packet\", \"byte\")")}
   )})
public class BModbusTcpDevice extends BModbusTcpGatewayDevice {
   public static final Property deviceAddress = newProperty(0, 1, BFacets.makeInt(null, 0, 255));
   public static final Property ipAddress = newProperty(0, "###.###.###.###", null);
   public static final Property port = newProperty(0, 502, BFacets.makeInt(null, 0, 65535));
   public static final Property socketStatus = newProperty(67, BSocketStatusEnum.closed, null);
   public static final Property disableTransactionIdCheck = newProperty(0, false, null);
   public static final Property maxTransactionId = newProperty(0, 65535, BFacets.makeInt(null, 0, 65535));
   public static final Property rxProcessMode = newProperty(0, false, BFacets.makeBoolean("packet", "byte"));
   public static final Type TYPE = Sys.loadType(BModbusTcpDevice.class);
   protected ModbusTcpComm tcpComm = null;

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

   public boolean getDisableTransactionIdCheck() {
      return this.getBoolean(disableTransactionIdCheck);
   }

   public void setDisableTransactionIdCheck(boolean v) {
      this.setBoolean(disableTransactionIdCheck, v, null);
   }

   public int getMaxTransactionId() {
      return this.getInt(maxTransactionId);
   }

   public void setMaxTransactionId(int v) {
      this.setInt(maxTransactionId, v, null);
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
   public Type getNetworkType() {
      return BModbusTcpNetwork.TYPE;
   }

   @Override
   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusTcpNetwork && !(parent instanceof BModbusTcpGateway) || parent instanceof BModbusTcpDeviceFolder;
   }

   public void started() throws Exception {
      super.started();
      if (Sys.isStationStarted()) {
         this.setSocketStatus(BSocketStatusEnum.closed);
         this.startComm();
      }
   }

   public void stationStarted() {
      this.startComm();
   }

   public void startComm() {
      try {
         this.tcpComm = new ModbusTcpComm(this.modbusNet());
         this.tcpComm.setDevice(this);
         if (!this.isDisabled() && !this.isFatalFault()) {
            this.tcpComm.start();
         }
      } catch (Exception var2) {
         this.modbusNet().getModbusLog().error("Exception generated in BModbusTcpDevice.deviceStarted(): ", var2);
      }
   }

   public void stopped() throws Exception {
      this.stopComm();
      super.stopped();
   }

   public void stopComm() {
      if (this.tcpComm != null && this.tcpComm.isCommStarted()) {
         try {
            this.tcpComm.stop();
         } catch (Exception var2) {
         }
      }
   }

   @Override
   public int getModbusMode() {
      return 2;
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
      if (this.isRunning()) {
         try {
            if (!prop.equals(ipAddress) && !prop.equals(port)) {
               if (prop.equals(status)) {
                  if (!this.isDisabled() && !this.isFatalFault()) {
                     if (this.tcpComm != null && !this.tcpComm.isCommStarted()) {
                        this.tcpComm.start();
                     }
                  } else if (this.tcpComm != null && this.tcpComm.isCommStarted()) {
                     this.tcpComm.stop();
                  }
               }
            } else if (this.tcpComm != null && this.tcpComm.isCommStarted()) {
               long ticks = Clock.ticks();

               while (this.getSocketStatus().equals(BSocketStatusEnum.openPending)) {
                  try {
                     Thread.sleep(250L);
                  } catch (Exception var8) {
                  }

                  if (Clock.ticks() - ticks > 30000L) {
                     break;
                  }
               }

               this.tcpComm.stop();
               InetAddress ipaddr = this.getInetAddr();
               if (ipaddr == null && this.modbusNet() != null && this.modbusNet().getModbusLog() != null) {
                  this.modbusNet().getModbusLog().error(this.getName() + ": Could not resolve IP address [" + this.getIpAddress() + "].");
               }

               try {
                  Thread.sleep(100L);
               } catch (Exception var7) {
               }

               this.tcpComm.start();
               this.ping();
            }
         } catch (Exception var9) {
            if (this.modbusNet() != null && this.modbusNet().getModbusLog() != null) {
               this.modbusNet().getModbusLog().error(this.getName() + ": Caught exception in BModbusTcpDevice.changed()", var9);
            }
         }
      }
   }

   @Override
   public Message sendModbusMessage(Message msg) {
      if (this.modbusNet().isCommActive() && this.tcpComm != null && this.tcpComm.isCommStarted()) {
         ModbusTcpSendRequest req = new ModbusTcpSendRequest(this, msg, true);
         Message response = null;
         int socketStatus = this.getSocketStatus().getOrdinal();
         if (socketStatus == 0 || socketStatus == 2) {
            ((ModbusTcpRxDriver)this.tcpComm.getCommReceiver()).initSocketConnection();
            socketStatus = this.getSocketStatus().getOrdinal();
         }

         if (socketStatus == 3) {
            this.incrementRequest();
            this.modbusNet().dispatch(req);
            response = req.getResponse(0);
         }

         if (response != null && ((ModbusResponse)response).exceptionCode != 10 && ((ModbusResponse)response).exceptionCode != 11) {
            this.pingOk();
            this.resetPingsFailed();
         } else if (this.incrementPingsFailed() > ((BModbusTcpNetwork)this.modbusNet()).getMaxFailsUntilDeviceDown()) {
            String cause = this.getLexicon().getText("pingFail");
            if (response != null) {
               cause = cause + ": " + ((ModbusResponse)response).getExceptionString();
            } else {
               this.incrementTimeouts();
            }

            this.pingFail(cause);
         }

         return response;
      } else {
         return null;
      }
   }

   public ModbusTcpComm getComm() {
      return this.tcpComm;
   }
}
