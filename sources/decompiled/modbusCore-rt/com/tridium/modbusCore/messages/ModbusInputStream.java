package com.tridium.modbusCore.messages;

import java.io.ByteArrayInputStream;

public class ModbusInputStream extends ByteArrayInputStream {
   public ModbusInputStream(byte[] buf) {
      super(buf);
   }

   public ModbusInputStream(byte[] buf, int offset, int length) {
      super(buf, offset, length);
   }

   public int getCount() {
      return this.count;
   }

   public int readHexWord() {
      int rawValue = this.readDigit();

      for (int i = 1; i < 4; i++) {
         rawValue = rawValue << 4 | this.readDigit();
      }

      return rawValue;
   }

   public int readHexByte() {
      int rawValue = this.readDigit();
      return rawValue << 4 | this.readDigit();
   }

   public int readDigit() {
      return Character.digit((char)(this.read() & 0xFF), 16) & 15;
   }

   public static byte[] convertAscii2Rtu(byte[] message) {
      ModbusInputStream in = new ModbusInputStream(message);
      ModbusOutputStream out = new ModbusOutputStream();
      in.read();
      out.write(in.readHexByte());
      out.write(in.readHexByte());

      while (in.available() > 0) {
         out.write(in.readHexByte());
      }

      return out.toByteArray();
   }

   public int readWord() {
      int value = (this.read() & 0xFF) << 8;
      return value + (this.read() & 0xFF);
   }
}
