package com.tridium.modbusCore.server.messages;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.messages.ModbusInputStream;
import com.tridium.modbusCore.messages.ModbusWriteRequest;

public class ModbusServerWriteRequest extends ModbusWriteRequest {
   public int transactionIdentifier = 0;

   public ModbusServerWriteRequest(int comType, BModbusDevice modDevice, byte[] data) {
      super(comType, modDevice);
      ModbusInputStream in = new ModbusInputStream(data);
      this.deviceAddress = in.read() & 0xFF;
      this.functionCode = in.read() & 0xFF;
      this.startAddress = in.readWord();
      this.data = new byte[data.length - 4];
      switch (this.functionCode) {
         case 5:
         case 6:
            this.data = new byte[2];
            this.data[0] = (byte)(in.read() & 0xFF);
            this.data[1] = (byte)(in.read() & 0xFF);
            this.numberPoints = 1;
            break;
         case 15:
         case 16:
            this.numberPoints = in.readWord();
            this.byteCount = in.read() & 0xFF;
            this.data = new byte[this.byteCount];

            for (int i = 0; i < this.byteCount; i++) {
               this.data[i] = (byte)(in.read() & 0xFF);
            }
      }
   }

   public void setTransactionIdentifier(int ti) {
      this.transactionIdentifier = ti;
   }
}
