package com.tridium.modbusCore.messages;

import com.tridium.basicdriver.message.Message;
import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.modbusCore.BModbusDevice;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.baja.nre.util.ByteArrayUtil;

public class ModbusReadExceptionStatusRequest extends ModbusMessage {
   int returnBytes;

   public ModbusReadExceptionStatusRequest(int comType, BModbusDevice modDevice, int addr, int returnBytes) {
      super(comType, modDevice);
      this.deviceAddress = addr;
      this.functionCode = 7;
      this.returnBytes = returnBytes;
   }

   @Override
   public final void writeRtu(OutputStream out) throws IOException {
      ModbusOutputStream modOut = new ModbusOutputStream();
      modOut.write((byte)this.deviceAddress);
      modOut.write((byte)this.functionCode);
      modOut.writeCRC();
      out.write(modOut.toByteArray());
   }

   @Override
   public final void writeAscii(OutputStream out) throws IOException {
      byte[] msgArray = new byte[]{(byte)this.deviceAddress, (byte)this.functionCode, 0};
      msgArray[2] = (byte)calcLRC(msgArray);
      ModbusOutputStream modOut = new ModbusOutputStream();
      modOut.write(msgArray);
      out.write(modOut.toAsciiHexByteArray());
   }

   @Override
   public final void writeTcp(OutputStream out) throws IOException {
      byte[] msgArray = new byte[]{0, 0, 0, 0, 0, 2, (byte)this.deviceAddress, (byte)this.functionCode};
      out.write(msgArray);
   }

   @Override
   public int getResponseMsgSize() {
      return 4 + this.returnBytes;
   }

   @Override
   public Message toResponse(ReceivedMessage response) {
      return new ModbusReadExceptionStatusResponse(this.comType, this.modbusDevice, response, this.returnBytes, this.comType == 0);
   }

   @Override
   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toDebugString());

      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         this.write(out);
         sb.append("\n  Raw Bytes = " + ByteArrayUtil.toHexString(out.toByteArray()));
      } catch (Exception var3) {
      }

      return sb.toString();
   }
}
