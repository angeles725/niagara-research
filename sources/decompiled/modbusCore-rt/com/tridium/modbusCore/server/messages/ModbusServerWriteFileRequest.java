package com.tridium.modbusCore.server.messages;

import com.tridium.modbusCore.messages.ModbusInputStream;
import java.util.Vector;

public class ModbusServerWriteFileRequest {
   public int deviceAddress;
   public int functionCode;
   private int subRequestCount;
   private int[] fileNumber;
   private int[] startingRecNumber;
   private int[] recLength;
   private Vector<byte[]> recData;

   public ModbusServerWriteFileRequest(byte[] data) {
      ModbusInputStream in = new ModbusInputStream(data);
      this.deviceAddress = in.read() & 0xFF;
      this.functionCode = in.read() & 0xFF;
      int byteCount = in.read() & 0xFF;
      int estimateSubRequestCount = byteCount / 7;
      this.fileNumber = new int[estimateSubRequestCount];
      this.startingRecNumber = new int[estimateSubRequestCount];
      this.recLength = new int[estimateSubRequestCount];
      this.recData = new Vector<>(estimateSubRequestCount);

      for (this.subRequestCount = 0; byteCount > 6; this.subRequestCount++) {
         int refType = in.read();
         this.fileNumber[this.subRequestCount] = in.readWord();
         this.startingRecNumber[this.subRequestCount] = in.readWord();
         this.recLength[this.subRequestCount] = in.readWord();
         byteCount -= 7;
         byte[] record = new byte[this.recLength[this.subRequestCount] * 2];

         for (int j = 0; j < record.length; j++) {
            record[j] = (byte)(in.read() & 0xFF);
            byteCount--;
         }

         this.recData.addElement(record);
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

   public byte[] getRecordData(int subRequestIndex) {
      return this.recData.elementAt(subRequestIndex);
   }
}
