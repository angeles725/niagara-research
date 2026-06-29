package com.tridium.modbusCore.server.messages;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.messages.ModbusInputStream;
import com.tridium.modbusCore.messages.ModbusMessage;

public class ModbusServerReadRequest extends ModbusMessage {
   public int transactionIdentifier = 0;

   public ModbusServerReadRequest(int comType, BModbusDevice modDevice, byte[] data) {
      super(comType, modDevice);
      ModbusInputStream in = new ModbusInputStream(data);
      this.deviceAddress = in.read() & 0xFF;
      this.functionCode = in.read() & 0xFF;
      this.startAddress = in.readWord();
      this.numberPoints = in.readWord();
   }

   public void setTransactionIdentifier(int ti) {
      this.transactionIdentifier = ti;
   }
}
