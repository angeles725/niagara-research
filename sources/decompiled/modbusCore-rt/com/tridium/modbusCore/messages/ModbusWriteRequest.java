package com.tridium.modbusCore.messages;

import com.tridium.modbusCore.BModbusDevice;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.baja.nre.util.ByteArrayUtil;

public class ModbusWriteRequest extends ModbusMessage {
   public int byteCount;
   public byte[] data;

   public ModbusWriteRequest(int comType, BModbusDevice modDevice) {
      super(comType, modDevice);
   }

   public ModbusWriteRequest(int comType, BModbusDevice modDevice, int addr, int functionCode, int start, int count, byte[] data) {
      super(comType, modDevice);
      this.deviceAddress = addr;
      this.functionCode = functionCode;
      this.startAddress = start;
      this.numberPoints = count;
      this.data = data;
   }

   @Override
   public final void writeRtu(OutputStream out) throws IOException {
      ModbusOutputStream modOut = this.formatMessage();
      modOut.writeCRC();
      out.write(modOut.toByteArray());
   }

   @Override
   public final void writeTcp(OutputStream out) throws IOException {
      byte[] msgArray = this.formatMessage().toByteArray();
      ModbusOutputStream modOut = new ModbusOutputStream();
      if (this.transactionIdentifier < 0) {
         this.transactionIdentifier = getNextTransactionId(this.getMaxTransactionId());
      }

      modOut.write((byte)((this.transactionIdentifier & 0xFF00) >> 8));
      modOut.write((byte)(this.transactionIdentifier & 0xFF));
      modOut.write((byte)0);
      modOut.write((byte)0);
      int msgLen = msgArray.length;
      modOut.write((byte)((msgLen & 0xFF00) >> 8));
      modOut.write((byte)(msgLen & 0xFF));
      modOut.write(msgArray);
      out.write(modOut.toByteArray());
   }

   @Override
   public final void writeAscii(OutputStream out) throws IOException {
      ModbusOutputStream modOut = this.formatMessage();
      modOut.writeLRC();
      out.write(modOut.toAsciiHexByteArray());
   }

   private ModbusOutputStream formatMessage() {
      ModbusOutputStream out = new ModbusOutputStream();
      out.write((byte)this.deviceAddress);
      out.write((byte)this.functionCode);
      out.write((byte)((this.startAddress & 0xFF00) >> 8));
      out.write((byte)(this.startAddress & 0xFF));
      if (this.functionCode == 5) {
         this.byteCount = 2;
         out.write(this.data, 0, this.byteCount);
      } else if (this.functionCode == 6) {
         this.byteCount = 2;
         out.write(this.data, 0, this.byteCount);
      } else if (this.functionCode == 16 || this.functionCode == 15) {
         this.byteCount = this.data.length;
         out.write((byte)((this.numberPoints & 0xFF00) >> 8));
         out.write((byte)(this.numberPoints & 0xFF));
         out.write((byte)this.byteCount);
         out.write(this.data, 0, this.byteCount);
      }

      return out;
   }

   @Override
   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toDebugString());
      sb.append("\n  Modbus Byte Count = " + this.byteCount);
      sb.append("\n  Modbus Data = " + ByteArrayUtil.toHexString(this.data));

      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         this.write(out);
         sb.append("\n  Raw Bytes = " + ByteArrayUtil.toHexString(out.toByteArray()));
      } catch (Exception var3) {
      }

      return sb.toString();
   }
}
