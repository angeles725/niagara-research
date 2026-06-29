package com.tridium.modbusCore.messages;

import com.tridium.basicdriver.message.Message;
import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.modbusCore.BModbusDevice;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.baja.nre.util.ByteArrayUtil;

public class ModbusReadFileRequest extends ModbusMessage {
   public int fileNumber;

   public ModbusReadFileRequest(int comType, BModbusDevice modDevice, int addr, int fileNumber, int start, int count) {
      super(comType, modDevice);
      this.deviceAddress = addr;
      this.functionCode = 20;
      this.fileNumber = fileNumber;
      this.startAddress = start;
      this.numberPoints = count;
   }

   @Override
   public final void writeRtu(OutputStream out) throws IOException {
      ModbusOutputStream modOut = new ModbusOutputStream();
      modOut.write((byte)this.deviceAddress);
      modOut.write((byte)this.functionCode);
      modOut.write((byte)7);
      modOut.write((byte)6);
      modOut.write((byte)((this.fileNumber & 0xFF00) >> 8));
      modOut.write((byte)(this.fileNumber & 0xFF));
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
         7,
         6,
         (byte)((this.fileNumber & 0xFF00) >> 8),
         (byte)(this.fileNumber & 0xFF),
         (byte)((this.startAddress & 0xFF00) >> 8),
         (byte)(this.startAddress & 0xFF),
         (byte)((this.numberPoints & 0xFF00) >> 8),
         (byte)(this.numberPoints & 0xFF),
         0
      };
      msgArray[10] = (byte)calcLRC(msgArray);
      ModbusOutputStream modOut = new ModbusOutputStream();
      modOut.write(msgArray);
      out.write(modOut.toAsciiHexByteArray());
   }

   @Override
   public final void writeTcp(OutputStream out) throws IOException {
      byte[] msgArray = new byte[16];
      if (this.transactionIdentifier < 0) {
         this.transactionIdentifier = getNextTransactionId(this.getMaxTransactionId());
      }

      msgArray[0] = (byte)((this.transactionIdentifier & 0xFF00) >> 8);
      msgArray[1] = (byte)(this.transactionIdentifier & 0xFF);
      msgArray[2] = 0;
      msgArray[3] = 0;
      msgArray[4] = 0;
      msgArray[5] = 10;
      msgArray[6] = (byte)this.deviceAddress;
      msgArray[7] = (byte)this.functionCode;
      msgArray[8] = 7;
      msgArray[9] = 6;
      msgArray[10] = (byte)((this.fileNumber & 0xFF00) >> 8);
      msgArray[11] = (byte)(this.fileNumber & 0xFF);
      msgArray[12] = (byte)((this.startAddress & 0xFF00) >> 8);
      msgArray[13] = (byte)(this.startAddress & 0xFF);
      msgArray[14] = (byte)((this.numberPoints & 0xFF00) >> 8);
      msgArray[15] = (byte)(this.numberPoints & 0xFF);
      out.write(msgArray);
   }

   @Override
   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toDebugString());
      sb.append("\n  Modbus File Number = " + this.fileNumber);

      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         this.write(out);
         sb.append("\n  Raw Bytes = " + ByteArrayUtil.toHexString(out.toByteArray()));
      } catch (Exception var3) {
      }

      return sb.toString();
   }

   @Override
   public int getResponseMsgSize() {
      return 7 + this.numberPoints * 2;
   }

   @Override
   protected Message toResponseAscii(ReceivedMessage response) {
      ModbusReceivedMessage modResp = (ModbusReceivedMessage)response;
      byte[] resp = modResp.getBytes();
      ModbusInputStream in = new ModbusInputStream(resp);
      ModbusResponse respMessage = new ModbusResponse(this.comType, this.modbusDevice);
      if (in.read() != 58) {
         respMessage.exceptionCode = -4;
         return respMessage;
      } else {
         int respSize = modResp.getLength();
         if (respSize <= 4) {
            respMessage.exceptionCode = -4;
            return respMessage;
         } else {
            byte[] noLRCBits = new byte[respSize - 4];
            System.arraycopy(resp, 0, noLRCBits, 0, respSize - 4);
            int lrc = calcLRC(ModbusInputStream.convertAscii2Rtu(noLRCBits));
            byte[] lrcArray = new byte[]{resp[respSize - 4], resp[respSize - 3]};
            ModbusInputStream lrcIn = new ModbusInputStream(lrcArray);
            int readLRC = lrcIn.readHexByte();
            if (lrc != readLRC) {
               respMessage.exceptionCode = -5;
               if (this.modbusDevice != null) {
                  this.modbusDevice.incrementLrcErrors();
               }

               return respMessage;
            } else {
               respMessage.deviceAddress = in.readHexByte() & 0xFF;
               respMessage.functionCode = in.readHexByte() & 0xFF;
               respMessage.startAddress = this.startAddress;
               respMessage.numberPoints = this.numberPoints;
               if ((respMessage.functionCode & 128) != 0) {
                  respMessage.exceptionCode = in.readHexByte();
                  return respMessage;
               } else if (respMessage.functionCode == this.functionCode && respMessage.deviceAddress == this.deviceAddress) {
                  respMessage.exceptionCode = 0;
                  respMessage.byteCount = (byte)(in.readHexByte() & 0xFF);
                  respMessage.byteCount = (byte)(in.readHexByte() & 0xFF);
                  in.readHexByte();
                  respMessage.data = new byte[respMessage.byteCount - 1];

                  for (int i = 0; i < respMessage.byteCount - 1; i++) {
                     respMessage.data[i] = (byte)(in.readHexByte() & 0xFF);
                  }

                  return respMessage;
               } else {
                  respMessage.exceptionCode = -4;
                  return respMessage;
               }
            }
         }
      }
   }

   @Override
   public Message toResponse(ReceivedMessage response) {
      if (this.comType == 0) {
         return this.toResponseAscii(response);
      } else {
         ModbusReceivedMessage modResp = (ModbusReceivedMessage)response;
         byte[] resp = modResp.getBytes();
         ModbusResponse respMessage = new ModbusResponse(this.comType, this.modbusDevice);
         respMessage.deviceAddress = resp[0] & 255;
         respMessage.functionCode = resp[1] & 255;
         respMessage.startAddress = this.startAddress;
         respMessage.numberPoints = this.numberPoints;
         if (this.comType == 1) {
            int respSize = modResp.getLength();
            if (respSize <= 2) {
               respMessage.exceptionCode = -4;
               return respMessage;
            }

            byte[] noCRCBits = new byte[respSize - 2];
            System.arraycopy(resp, 0, noCRCBits, 0, respSize - 2);
            int crc = calcCRC(noCRCBits);
            int readCRC = (resp[respSize - 2] & 255) << 8;
            readCRC |= resp[respSize - 1] & 255;
            if (crc != readCRC) {
               respMessage.exceptionCode = -1;
               if (this.modbusDevice != null) {
                  this.modbusDevice.incrementCrcErrors();
               }

               return respMessage;
            }
         } else if (this.comType == 2) {
            if (this.transactionIdentifier != modResp.getTransactionId()) {
               respMessage.exceptionCode = -4;
               if (this.modbusDevice != null) {
                  this.modbusDevice.incrementTransactionIdErrors();
               }

               return respMessage;
            }

            respMessage.transactionIdentifier = this.transactionIdentifier;
         }

         try {
            if (resp[1] < 0) {
               respMessage.exceptionCode = resp[2];
               return respMessage;
            } else if (respMessage.functionCode == this.functionCode && respMessage.deviceAddress == this.deviceAddress) {
               respMessage.exceptionCode = 0;
               respMessage.byteCount = resp[2] & 255;
               respMessage.byteCount = resp[3] & 255;
               respMessage.data = new byte[respMessage.byteCount - 1];

               for (int i = 0; i < respMessage.byteCount - 1; i++) {
                  respMessage.data[i] = resp[5 + i];
               }

               return respMessage;
            } else {
               respMessage.exceptionCode = -4;
               return respMessage;
            }
         } catch (ArrayIndexOutOfBoundsException var9) {
            respMessage.exceptionCode = -4;
            return respMessage;
         }
      }
   }
}
