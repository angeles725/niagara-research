package com.tridium.modbusCore.server.messages;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.messages.ModbusOutputStream;
import com.tridium.modbusCore.messages.ModbusResponse;
import java.io.IOException;

public class ModbusServerReadFileResponse extends ModbusResponse {
   public ModbusServerReadFileResponse(int comType, BModbusDevice modDevice, ModbusServerReadFileRequest readRequest) {
      super(comType, modDevice);
      this.setResponseExpected(false);
      this.deviceAddress = readRequest.deviceAddress;
      this.functionCode = readRequest.functionCode;
      this.data = null;
   }

   @Override
   protected ModbusOutputStream formatBaseMessage() throws IOException {
      ModbusOutputStream out = new ModbusOutputStream();
      out.write((byte)this.deviceAddress);
      out.write((byte)this.functionCode);
      out.write((byte)this.byteCount);
      out.write(this.data);
      return out;
   }

   public void addSubRequestData(int fileNum, int startRecNum, int recLength, byte[] recData) throws IOException {
      ModbusOutputStream out = new ModbusOutputStream();
      if (this.data != null) {
         out.write(this.data);
      }

      out.write((byte)(recData.length + 1));
      out.write((byte)6);
      out.write(recData);
      this.data = out.toByteArray();
      this.byteCount = this.data.length;
   }
}
