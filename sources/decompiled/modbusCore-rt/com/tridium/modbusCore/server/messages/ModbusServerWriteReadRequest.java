package com.tridium.modbusCore.server.messages;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.messages.ModbusInputStream;
import com.tridium.modbusCore.messages.ModbusWriteRequest;

public class ModbusServerWriteReadRequest extends ModbusWriteRequest {
   public int transactionIdentifier = 0;
   public int wrAddress;
   public int wrNumberPoints;

   public ModbusServerWriteReadRequest(int comType, BModbusDevice modDevice, byte[] data) {
      super(comType, modDevice);
      ModbusInputStream in = new ModbusInputStream(data);
      this.deviceAddress = in.read() & 0xFF;
      this.functionCode = in.read() & 0xFF;
      this.startAddress = in.readWord();
      this.numberPoints = in.readWord();
      this.wrAddress = in.readWord();
      this.wrNumberPoints = in.readWord();
      this.byteCount = in.read() & 0xFF;
      this.data = new byte[this.byteCount];

      for (int i = 0; i < this.byteCount; i++) {
         this.data[i] = (byte)(in.read() & 0xFF);
      }
   }

   public void setTransactionIdentifier(int ti) {
      this.transactionIdentifier = ti;
   }
}
