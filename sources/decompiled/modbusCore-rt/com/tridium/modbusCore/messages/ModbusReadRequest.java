package com.tridium.modbusCore.messages;

import com.tridium.modbusCore.BModbusDevice;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.baja.nre.util.ByteArrayUtil;

public class ModbusReadRequest extends ModbusMessage {
   public ModbusReadRequest(int comType, BModbusDevice modDevice, int addr, int functionCode, int start, int count) {
      super(comType, modDevice);
      this.deviceAddress = addr;
      this.functionCode = functionCode;
      this.startAddress = start;
      this.numberPoints = count;
   }

   @Override
   public final void writeRtu(OutputStream out) throws IOException {
      ModbusOutputStream modOut = new ModbusOutputStream();
      modOut.write((byte)this.deviceAddress);
      modOut.write((byte)this.functionCode);
      modOut.write((byte)((this.startAddress & 0xFF00) >> 8));
      modOut.write((byte)(this.startAddress & 0xFF));
      modOut.write((byte)((this.numberPoints & 0xFF00) >> 8));
      modOut.write((byte)(this.numberPoints & 0xFF));
      modOut.writeCRC();
      out.write(modOut.toByteArray());
   }

   @Override
   public final void writeAscii(OutputStream out) throws IOException {
      byte[] msgArray = new byte[]{
         (byte)this.deviceAddress,
         (byte)this.functionCode,
         (byte)((this.startAddress & 0xFF00) >> 8),
         (byte)(this.startAddress & 0xFF),
         (byte)((this.numberPoints & 0xFF00) >> 8),
         (byte)(this.numberPoints & 0xFF),
         0
      };
      msgArray[6] = (byte)calcLRC(msgArray);
      ModbusOutputStream modOut = new ModbusOutputStream();
      modOut.write(msgArray);
      out.write(modOut.toAsciiHexByteArray());
   }

   @Override
   public final void writeTcp(OutputStream out) throws IOException {
      byte[] msgArray = new byte[12];
      if (this.transactionIdentifier < 0) {
         this.transactionIdentifier = getNextTransactionId(this.getMaxTransactionId());
      }

      msgArray[0] = (byte)((this.transactionIdentifier & 0xFF00) >> 8);
      msgArray[1] = (byte)(this.transactionIdentifier & 0xFF);
      msgArray[2] = 0;
      msgArray[3] = 0;
      msgArray[4] = 0;
      msgArray[5] = 6;
      msgArray[6] = (byte)this.deviceAddress;
      msgArray[7] = (byte)this.functionCode;
      msgArray[8] = (byte)((this.startAddress & 0xFF00) >> 8);
      msgArray[9] = (byte)(this.startAddress & 0xFF);
      msgArray[10] = (byte)((this.numberPoints & 0xFF00) >> 8);
      msgArray[11] = (byte)(this.numberPoints & 0xFF);
      out.write(msgArray);
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
