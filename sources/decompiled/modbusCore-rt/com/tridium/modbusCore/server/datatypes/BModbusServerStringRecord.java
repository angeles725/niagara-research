package com.tridium.modbusCore.server.datatypes;

import com.tridium.modbusCore.ModbusErrorCodes;
import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.datatypes.BModbusStringRecord;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusServerStringRecord extends BModbusStringRecord implements ModbusErrorCodes {
   public static final Type TYPE = Sys.loadType(BModbusServerStringRecord.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BModbusServerStringRecord() {
      this.setFlags(data, 257);
      this.setFlags(output, this.getFlags(output) & -3);
   }

   @Override
   public void doWrite() {
      byte[] inputBytes = this.getInputBytes(this.getRecordLength(), this.getPadding() ? null : " ");
      this.setOutputBytes(inputBytes);
      this.fireWriteSuccessful(null);
   }

   public byte[] setBytes(byte[] bytes, int startingRec, int recLength) throws ModbusException {
      int rangeStart = this.getStartingRecordNumber();
      int rangeEnd = rangeStart + this.getRecordLength();
      int bIdx = 0;
      if (startingRec < rangeStart) {
         int difference = rangeStart - startingRec;
         startingRec = rangeStart;
         recLength -= difference;
         bIdx = difference * 2;
      }

      if (recLength <= 0) {
         throw new ModbusException(103);
      } else if (startingRec > rangeEnd) {
         throw new ModbusException(103);
      } else {
         if (startingRec + recLength > rangeEnd) {
            recLength = rangeEnd - startingRec;
         }

         if (recLength <= 0) {
            throw new ModbusException(103);
         } else {
            int startIndex = (startingRec - this.getStartingRecordNumber()) * 2;
            int endIndex = startIndex + recLength * 2;
            byte[] result = this.getData().copyBytes();
            result = padBytes(result, this.getRecordLength(), this.getPadding() ? null : " ");

            for (int i = startIndex; i < endIndex; i++) {
               result[i] = bytes[bIdx];
               bIdx++;
            }

            this.setOutputBytes(result);
            return this.getBytes(startingRec, recLength);
         }
      }
   }

   public boolean containsRecords(int startingRec, int recLength) {
      int rangeStart = this.getStartingRecordNumber();
      int rangeEnd = rangeStart + this.getRecordLength();
      return startingRec >= rangeStart && recLength > 0 && startingRec + recLength <= rangeEnd;
   }

   public byte[] getBytes(int startingRec, int recLength) throws ModbusException {
      try {
         byte[] result = new byte[recLength * 2];
         byte[] currentData = this.getData().copyBytes();
         currentData = padBytes(currentData, this.getRecordLength(), this.getPadding() ? null : " ");
         int startIdx = (startingRec - this.getStartingRecordNumber()) * 2;
         int endIdx = startIdx + recLength * 2;
         int count = 0;

         for (int i = startIdx; i < endIdx; i++) {
            result[count] = currentData[i];
            count++;
         }

         return result;
      } catch (Exception var9) {
         throw new ModbusException(103);
      }
   }
}
