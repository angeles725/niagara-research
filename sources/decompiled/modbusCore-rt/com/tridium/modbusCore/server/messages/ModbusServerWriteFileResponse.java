package com.tridium.modbusCore.server.messages;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.messages.ModbusOutputStream;
import com.tridium.modbusCore.messages.ModbusResponse;
import java.io.IOException;

public class ModbusServerWriteFileResponse extends ModbusResponse {
   public ModbusServerWriteFileResponse(int comType, BModbusDevice modDevice, ModbusServerWriteFileRequest writeRequest) {
      super(comType, modDevice);
      this.setResponseExpected(false);
      this.deviceAddress = writeRequest.deviceAddress;
      this.functionCode = writeRequest.functionCode;
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

      out.write((byte)6);
      out.writeWord(fileNum);
      out.writeWord(startRecNum);
      out.writeWord(recLength);
      out.write(recData);
      this.data = out.toByteArray();
      this.byteCount = this.data.length;
   }
}
