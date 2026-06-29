package com.tridium.modbusTcp.comm;

import com.tridium.basicdriver.message.Message;
import com.tridium.modbusTcp.BModbusTcpDevice;

public class ModbusTcpSendRequest implements Runnable {
   protected Message msg;
   protected boolean responseExpected;
   protected Message response = null;
   protected boolean complete = false;
   protected BModbusTcpDevice tcpDevice;

   public ModbusTcpSendRequest() {
   }

   public ModbusTcpSendRequest(BModbusTcpDevice tcpDevice, Message msg, boolean responseExpected) {
      this.tcpDevice = tcpDevice;
      this.msg = msg;
      this.msg.setResponseExpected(responseExpected);
      this.responseExpected = responseExpected;
   }

   @Override
   public String toString() {
      return new String("ModbusTcpSendRequest: " + this.msg);
   }

   @Override
   public void run() {
      this.execute();
   }

   public synchronized void execute() {
      this.response = null;

      try {
         if (this.msg == null) {
            this.tcpDevice.modbusNet().getModbusLog().message("ModbusTcpSendRequest received null message to send.  Not sending null message");
            return;
         }

         if (!this.responseExpected) {
            this.tcpDevice.getComm().transmitNoResponse(this.msg);
         } else {
            this.response = this.tcpDevice.getComm().transmit(this.msg);
         }
      } catch (Exception var2) {
         this.tcpDevice.modbusNet().getModbusLog().error("ModbusTcpSendRequest caught exception in execute(): ", var2);
         this.complete = true;
         this.notify();
         return;
      }

      this.complete = true;
      this.notify();
   }

   public synchronized Message getResponse(int timeout) {
      if (!this.complete) {
         try {
            this.wait(timeout);
         } catch (Exception var3) {
            this.tcpDevice.modbusNet().getModbusLog().error("ModbusTcpSendRequest caught exception in getResponse(): ", var3);
         }
      }

      return this.response;
   }
}
