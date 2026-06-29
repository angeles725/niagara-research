package com.tridium.modbusCore.messages;

import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.modbusCore.BModbusDevice;

public class ModbusReadExceptionStatusResponse extends ModbusResponse {
   int returnBytes;
   int statusData;

   public ModbusReadExceptionStatusResponse(int comType, BModbusDevice modDevice, ReceivedMessage response, int returnBytes, boolean isAscii) {
      super(comType, modDevice);
      this.returnBytes = returnBytes;
      ModbusReceivedMessage modResp = (ModbusReceivedMessage)response;
      ModbusInputStream in = new ModbusInputStream(modResp.getBytes(), 0, modResp.getLength());
      if (!isAscii) {
         this.deviceAddress = in.read() & 0xFF;
         this.functionCode = in.read() & 0xFF;
         if ((this.functionCode & 128) != 0) {
            this.exceptionCode = in.read() & 0xFF;
         } else {
            this.exceptionCode = 0;
            this.statusData = 0;
            if (returnBytes == 2) {
               this.statusData = (in.read() & 0xFF) << 8;
            }

            this.statusData = this.statusData & 0xFF00 | in.read() & 0xFF;
            System.out.println("    StatusData = 0x" + Integer.toHexString(this.statusData));
         }
      } else if (in.read() != 58) {
         this.exceptionCode = -4;
      } else {
         this.deviceAddress = in.readHexByte() & 0xFF;
         this.functionCode = in.readHexByte() & 0xFF;
         if ((this.functionCode & 128) != 0) {
            this.exceptionCode = in.readHexByte();
         } else {
            this.exceptionCode = 0;
            this.statusData = 0;
            if (returnBytes == 2) {
               this.statusData = (in.readHexByte() & 0xFF) << 8;
            }

            this.statusData = this.statusData & 0xFF00 | in.readHexByte() & 0xFF;
         }
      }
   }

   public double getDouble() {
      return this.statusData;
   }

   public boolean getStatusBit(int bit) {
      int mask = 1 << bit;
      return (this.statusData & mask) != 0;
   }
}
