package com.tridium.modbusTcp.comm;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.BModbusNetwork;

public class ModbusTcpComm extends Comm {
   protected Thread tcpRxThread;
   private static int nextSoManSerialNo = 0;
   private BModbusDevice device = null;

   public ModbusTcpComm(BModbusNetwork serialNetwork) {
      super(serialNetwork, new ModbusTcpRxDriver(serialNetwork), new ModbusTcpTxDriver());
   }

   protected boolean started() throws Exception {
      this.getCommReceiver().setAlive(true);
      this.tcpRxThread = new Thread(this.getCommReceiver(), "ModTcp:" + this.devName() + nextSoManSerialNo++);
      this.tcpRxThread.start();
      this.tcpRxThread.setPriority(5);
      ((ModbusTcpRxDriver)this.getCommReceiver()).startSocketManager();
      return true;
   }

   protected void stopped() throws Exception {
      this.getCommReceiver().setAlive(false);
      ((ModbusTcpRxDriver)this.getCommReceiver()).stopSocketManager();
      if (this.getCommReceiver() != null && this.tcpRxThread != null) {
         this.tcpRxThread.interrupt();
         ((ModbusTcpRxDriver)this.getCommReceiver()).closeSocket();
         ((ModbusTcpRxDriver)this.getCommReceiver()).nullStreams();
      }
   }

   public void setDevice(BModbusDevice device) {
      this.device = device;
   }

   public BModbusDevice getDevice() {
      return this.device;
   }

   public String devName() {
      if (this.device != null) {
         return this.device.getName();
      } else {
         return this.getNetwork() != null ? this.getNetwork().getName() : "UnknownDevice";
      }
   }
}
