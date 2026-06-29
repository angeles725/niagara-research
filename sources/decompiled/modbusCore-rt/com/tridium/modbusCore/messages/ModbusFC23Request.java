package com.tridium.modbusCore.messages;

import com.tridium.modbusCore.BModbusDevice;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.baja.nre.util.ByteArrayUtil;

public class ModbusFC23Request extends ModbusMessage {
   public int byteCount;
   public int wrAddress;
   public int wrNumberPoints;
   public byte[] wrData;

   public ModbusFC23Request(int comType, BModbusDevice modDevice) {
      super(comType, modDevice);
   }

   public ModbusFC23Request(int comType, BModbusDevice modDevice, int addr, int functionCode, int wrStart, int wrCount, int rdStart, int rdCount, byte[] wrData) {
      super(comType, modDevice);
      this.deviceAddress = addr;
      this.functionCode = functionCode;
      this.startAddress = rdStart;
      this.numberPoints = rdCount;
      this.wrAddress = wrStart;
      this.wrNumberPoints = wrCount;
      this.wrData = wrData;
   }

   @Override
   public int getResponseMsgSize() {
      return 5 + this.numberPoints * 2;
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
      out.write((byte)((this.numberPoints & 0xFF00) >> 8));
      out.write((byte)(this.numberPoints & 0xFF));
      out.write((byte)((this.wrAddress & 0xFF00) >> 8));
      out.write((byte)(this.wrAddress & 0xFF));
      out.write((byte)((this.wrNumberPoints & 0xFF00) >> 8));
      out.write((byte)(this.wrNumberPoints & 0xFF));
      out.write((byte)this.wrData.length);
      out.write(this.wrData, 0, this.wrData.length);
      return out;
   }

   @Override
   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toDebugString());
      sb.append("\n  Modbus Byte Count = " + this.byteCount);
      sb.append("\n  Modbus Data = " + ByteArrayUtil.toHexString(this.wrData));

      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         this.write(out);
         sb.append("\n  Raw Bytes = " + ByteArrayUtil.toHexString(out.toByteArray()));
      } catch (Exception var3) {
      }

      return sb.toString();
   }
}
