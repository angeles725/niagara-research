package com.tridium.modbusCore.messages;

import com.tridium.basicdriver.message.Message;
import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.modbusCore.BModbusDevice;
import java.io.IOException;
import java.io.OutputStream;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BBoolean;
import javax.baja.sys.Property;

public class ModbusMessage extends Message implements ModbusMessageConst {
   static final char[] constCRCHi = new char[]{
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0001',
      'À',
      '\u0080',
      'A',
      '\u0000',
      'Á',
      '\u0081',
      '@'
   };
   static final char[] constCRCLo = new char[]{
      '\u0000',
      'À',
      'Á',
      '\u0001',
      'Ã',
      '\u0003',
      '\u0002',
      'Â',
      'Æ',
      '\u0006',
      '\u0007',
      'Ç',
      '\u0005',
      'Å',
      'Ä',
      '\u0004',
      'Ì',
      '\f',
      '\r',
      'Í',
      '\u000f',
      'Ï',
      'Î',
      '\u000e',
      '\n',
      'Ê',
      'Ë',
      '\u000b',
      'É',
      '\t',
      '\b',
      'È',
      'Ø',
      '\u0018',
      '\u0019',
      'Ù',
      '\u001b',
      'Û',
      'Ú',
      '\u001a',
      '\u001e',
      'Þ',
      'ß',
      '\u001f',
      'Ý',
      '\u001d',
      '\u001c',
      'Ü',
      '\u0014',
      'Ô',
      'Õ',
      '\u0015',
      '×',
      '\u0017',
      '\u0016',
      'Ö',
      'Ò',
      '\u0012',
      '\u0013',
      'Ó',
      '\u0011',
      'Ñ',
      'Ð',
      '\u0010',
      'ð',
      '0',
      '1',
      'ñ',
      '3',
      'ó',
      'ò',
      '2',
      '6',
      'ö',
      '÷',
      '7',
      'õ',
      '5',
      '4',
      'ô',
      '<',
      'ü',
      'ý',
      '=',
      'ÿ',
      '?',
      '>',
      'þ',
      'ú',
      ':',
      ';',
      'û',
      '9',
      'ù',
      'ø',
      '8',
      '(',
      'è',
      'é',
      ')',
      'ë',
      '+',
      '*',
      'ê',
      'î',
      '.',
      '/',
      'ï',
      '-',
      'í',
      'ì',
      ',',
      'ä',
      '$',
      '%',
      'å',
      '\'',
      'ç',
      'æ',
      '&',
      '"',
      'â',
      'ã',
      '#',
      'á',
      '!',
      ' ',
      'à',
      ' ',
      '`',
      'a',
      '¡',
      'c',
      '£',
      '¢',
      'b',
      'f',
      '¦',
      '§',
      'g',
      '¥',
      'e',
      'd',
      '¤',
      'l',
      '¬',
      '\u00ad',
      'm',
      '¯',
      'o',
      'n',
      '®',
      'ª',
      'j',
      'k',
      '«',
      'i',
      '©',
      '¨',
      'h',
      'x',
      '¸',
      '¹',
      'y',
      '»',
      '{',
      'z',
      'º',
      '¾',
      '~',
      '\u007f',
      '¿',
      '}',
      '½',
      '¼',
      '|',
      '´',
      't',
      'u',
      'µ',
      'w',
      '·',
      '¶',
      'v',
      'r',
      '²',
      '³',
      's',
      '±',
      'q',
      'p',
      '°',
      'P',
      '\u0090',
      '\u0091',
      'Q',
      '\u0093',
      'S',
      'R',
      '\u0092',
      '\u0096',
      'V',
      'W',
      '\u0097',
      'U',
      '\u0095',
      '\u0094',
      'T',
      '\u009c',
      '\\',
      ']',
      '\u009d',
      '_',
      '\u009f',
      '\u009e',
      '^',
      'Z',
      '\u009a',
      '\u009b',
      '[',
      '\u0099',
      'Y',
      'X',
      '\u0098',
      '\u0088',
      'H',
      'I',
      '\u0089',
      'K',
      '\u008b',
      '\u008a',
      'J',
      'N',
      '\u008e',
      '\u008f',
      'O',
      '\u008d',
      'M',
      'L',
      '\u008c',
      'D',
      '\u0084',
      '\u0085',
      'E',
      '\u0087',
      'G',
      'F',
      '\u0086',
      '\u0082',
      'B',
      'C',
      '\u0083',
      'A',
      '\u0081',
      '\u0080',
      '@'
   };
   private static Object idLock = new Object();
   private static int nextId = 0;
   public int deviceAddress = -1;
   public int functionCode;
   public int startAddress;
   public int numberPoints;
   public int comType;
   public BModbusDevice modbusDevice;
   public int transactionIdentifier = -1;

   public ModbusMessage(int comType, BModbusDevice modbusDevice) {
      this.comType = comType;
      this.modbusDevice = modbusDevice;
      int deviceComType = modbusDevice.getModbusMode();
      this.comType = deviceComType;
   }

   public void writeRtu(OutputStream out) throws IOException {
   }

   public void writeAscii(OutputStream out) throws IOException {
   }

   public void writeTcp(OutputStream out) throws IOException {
   }

   public void write(OutputStream out) {
      try {
         switch (this.comType) {
            case 0:
               this.writeAscii(out);
               return;
            case 1:
               this.writeRtu(out);
               return;
            case 2:
               this.writeTcp(out);
               return;
         }
      } catch (Exception var3) {
         System.out.println("Exception writing Modbus Message: ");
         var3.printStackTrace();
      }
   }

   public int getResponseMsgSize() {
      switch (this.functionCode) {
         case 1:
         case 2:
            if (this.numberPoints % 8 == 0) {
               return 5 + this.numberPoints / 8;
            }

            return 6 + this.numberPoints / 8;
         case 3:
         case 4:
            return 5 + this.numberPoints * 2;
         case 5:
            return 8;
         case 6:
            return 8;
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         default:
            return 5;
         case 15:
            return 8;
         case 16:
            return 8;
      }
   }

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
               System.out.println("*** LRC ERROR: " + ByteArrayUtil.toHexString(resp, 0, respSize));
               System.out.println("readLRC = 0x" + Integer.toHexString(readLRC));
               System.out.println("lrc     = 0x" + Integer.toHexString(lrc));
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
                  switch (respMessage.functionCode) {
                     case 1:
                     case 2:
                     case 3:
                     case 4:
                     case 23:
                        respMessage.byteCount = in.readHexByte() & 0xFF;

                        for (int i = 0; i < respMessage.byteCount; i++) {
                           respMessage.data[i] = (byte)(in.readHexByte() & 0xFF);
                        }
                     default:
                        return respMessage;
                  }
               } else {
                  respMessage.exceptionCode = -4;
                  return respMessage;
               }
            }
         }
      }
   }

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
            boolean disableTransactionIdCheck = false;

            try {
               if (this.modbusDevice != null) {
                  disableTransactionIdCheck = ((BBoolean)this.modbusDevice.get("disableTransactionIdCheck")).getBoolean();
               }
            } catch (Exception var9) {
            }

            if (!disableTransactionIdCheck && this.transactionIdentifier != modResp.getTransactionId()) {
               respMessage.exceptionCode = -4;
               if (this.modbusDevice != null) {
                  this.modbusDevice.incrementTransactionIdErrors();
                  if (this.modbusDevice.modbusNet() != null && this.modbusDevice.modbusNet().getModbusLog().isTraceOn()) {
                     StringBuilder sb = new StringBuilder();
                     sb.append("Received invalid transaction ID in response (expected ");
                     sb.append(this.transactionIdentifier).append(", received ");
                     sb.append(modResp.getTransactionId()).append(")");
                     this.modbusDevice.modbusNet().getModbusLog().trace(sb.toString());
                  }
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
               switch (resp[1]) {
                  case 1:
                  case 2:
                  case 3:
                  case 4:
                  case 23:
                     respMessage.byteCount = resp[2] & 255;

                     for (int i = 0; i < respMessage.byteCount; i++) {
                        respMessage.data[i] = resp[3 + i];
                     }
                  case 5:
                  case 6:
                  case 7:
                  case 8:
                  case 9:
                  case 10:
                  case 11:
                  case 12:
                  case 13:
                  case 14:
                  case 15:
                  case 16:
                  case 17:
                  case 18:
                  case 19:
                  case 20:
                  case 21:
                  case 22:
                  default:
                     return respMessage;
               }
            } else {
               respMessage.exceptionCode = -4;
               return respMessage;
            }
         } catch (ArrayIndexOutOfBoundsException var10) {
            respMessage.exceptionCode = -4;
            return respMessage;
         }
      }
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      String comTypeString = "RTU";
      if (this.comType == 0) {
         comTypeString = "ASCII";
      } else if (this.comType == 2) {
         comTypeString = "TCP";
      }

      sb.append("Modbus " + comTypeString + " Message = " + TextUtil.getClassName(this.getClass()));
      sb.append("\n  Tag = " + this.getTag());
      sb.append("\n  Modbus Device Address = " + (this.deviceAddress & 0xFF));
      sb.append("\n  Modbus Function Code = " + this.functionCode);
      sb.append("\n  Modbus Data Starting Address = " + this.startAddress);
      sb.append("\n  Modbus Number of Data Points = " + this.numberPoints);
      return sb.toString();
   }

   public static int calcLRC(byte[] msg) {
      int lrc = 0;

      for (int i = 0; i < msg.length; i++) {
         lrc += msg[i] & 255;
      }

      lrc &= 255;
      lrc = 255 - lrc & 0xFF;
      return ++lrc & 0xFF;
   }

   public static boolean verifyLRC(byte[] msg) {
      int lrc = 0;

      for (int i = 0; i < msg.length - 2; i++) {
         lrc += msg[i] & 255;
      }

      lrc &= 255;
      lrc = 255 - lrc & 0xFF;
      lrc++;
      return (lrc & 0xFF) == (msg[msg.length - 2] & 255);
   }

   public static int calcCRC(byte[] msg) {
      int hiCRC = 255;
      int loCRC = 255;

      for (int i = 0; i < msg.length; i++) {
         int byteValue = msg[i] & 255;
         int index = hiCRC ^ byteValue;
         hiCRC = loCRC ^ constCRCHi[index];
         loCRC = constCRCLo[index];
      }

      return hiCRC * 256 + loCRC;
   }

   public static boolean verifyCRC(byte[] msg) {
      int hiCRC = 255;
      int loCRC = 255;

      for (int i = 0; i < msg.length - 2; i++) {
         int byteValue = msg[i] & 255;
         int index = hiCRC ^ byteValue;
         hiCRC = loCRC ^ constCRCHi[index];
         loCRC = constCRCLo[index];
      }

      return (msg[msg.length - 2] & 255) == hiCRC && (msg[msg.length - 1] & 255) == loCRC;
   }

   protected int getMaxTransactionId() {
      int maxId = 65535;

      try {
         Property p = this.modbusDevice.getProperty("maxTransactionId");
         if (p != null) {
            maxId = this.modbusDevice.getInt(p);
         }
      } catch (Exception var3) {
      }

      return maxId;
   }

   protected static int getNextTransactionId(int maxId) {
      synchronized (idLock) {
         if (nextId >= maxId) {
            nextId = 0;
         }

         return ++nextId;
      }
   }
}
