package com.tridium.modbusTcp.comm;

import com.tridium.basicdriver.comm.CommTransmitter;
import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.BModbusNetwork;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import com.tridium.modbusTcp.BModbusTcpDevice;
import com.tridium.modbusTcp.BModbusTcpGateway;

public class ModbusTcpTxDriver extends CommTransmitter implements ModbusMessageConst {
   public void writeMessage(Message message) {
      ModbusTcpRxDriver rxDriver = (ModbusTcpRxDriver)this.getComm().getCommReceiver();
      BModbusTcpDevice device = null;

      try {
         ModbusTcpComm comm = (ModbusTcpComm)this.getComm();
         var network = (BModbusNetwork & BModbusNetwork)comm.getNetwork();
         device = (BModbusTcpDevice)comm.getDevice();
         if (device != null) {
            if (network.getLog().isTraceOn()) {
               network.getLog()
                  .trace("**** ModbusTcp Send Bytes [destination " + device.getIpAddress() + ":" + device.getPort() + "]: " + message.toDebugString());
            }
         } else if (network.getLog().isTraceOn()) {
            network.getLog()
               .trace(
                  "**** ModbusTcp Send Bytes [destination "
                     + ((BModbusTcpGateway)network).getIpAddress()
                     + ":"
                     + ((BModbusTcpGateway)network).getPort()
                     + "]: "
                     + message.toDebugString()
               );
         }

         rxDriver.writeOutputStream(message);
      } catch (Exception var6) {
         if (device != null) {
            this.getComm()
               .getNetwork()
               .getLog()
               .error(
                  "["
                     + device.getIpAddress()
                     + ":"
                     + device.getPort()
                     + "]: Problem sending ModbusTcp message to device address "
                     + device.getDeviceAddress()
                     + ": "
                     + var6.getMessage()
               );
         } else {
            this.getComm().getNetwork().getLog().error("Problem sending ModbusTcp message: " + var6.getMessage());
         }
      }
   }
}
