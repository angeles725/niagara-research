package com.tridium.modbusCore.server.messages;

import com.tridium.modbusCore.messages.ModbusInputStream;

public class ModbusServerReadFileRequest {
   public int deviceAddress;
   public int functionCode;
   private int subRequestCount;
   private int[] fileNumber;
   private int[] startingRecNumber;
   private int[] recLength;

   public ModbusServerReadFileRequest(byte[] data) {
      ModbusInputStream in = new ModbusInputStream(data);
      this.deviceAddress = in.read() & 0xFF;
      this.functionCode = in.read() & 0xFF;
      int byteCount = in.read() & 0xFF;
      this.subRequestCount = byteCount / 7;
      this.fileNumber = new int[this.subRequestCount];
      this.startingRecNumber = new int[this.subRequestCount];
      this.recLength = new int[this.subRequestCount];

      for (int i = 0; i < this.subRequestCount; i++) {
         int refType = in.read();
         this.fileNumber[i] = in.readWord();
         this.startingRecNumber[i] = in.readWord();
         this.recLength[i] = in.readWord();
      }
   }

   public int getNumSubRequests() {
      return this.subRequestCount;
   }

   public int getFileNumber(int subRequestIndex) {
      return this.fileNumber[subRequestIndex];
   }

   public int getStartingRecordNumber(int subRequestIndex) {
      return this.startingRecNumber[subRequestIndex];
   }

   public int getRecordLength(int subRequestIndex) {
      return this.recLength[subRequestIndex];
   }
}
