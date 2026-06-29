package com.tridium.modbusCore.messages;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.enums.BDataByteOrder64BitEnum;
import com.tridium.modbusCore.enums.BDataByteOrderEnum;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;

public class ModbusResponse extends ModbusMessage {
   public int exceptionCode;
   public int startAddress;
   public int numberPoints;
   public int byteCount;
   public byte[] data = new byte[255];

   public ModbusResponse(int comType, BModbusDevice modDevice) {
      super(comType, modDevice);
   }

   public ModbusResponse(int comType, BModbusDevice modDevice, ModbusWriteRequest writeRequest) {
      super(comType, modDevice);
      this.deviceAddress = writeRequest.deviceAddress;
      this.functionCode = writeRequest.functionCode;
      this.startAddress = writeRequest.startAddress;
      this.numberPoints = writeRequest.numberPoints;
      this.byteCount = writeRequest.byteCount;
      this.data = new byte[writeRequest.data.length];

      for (int i = 0; i < writeRequest.data.length; i++) {
         this.data[i] = writeRequest.data[i];
      }
   }

   @Override
   public final void writeRtu(OutputStream out) throws IOException {
      ModbusOutputStream modOut = this.formatBaseMessage();
      modOut.writeCRC();
      out.write(modOut.toByteArray());
   }

   @Override
   public void writeAscii(OutputStream out) throws IOException {
      ModbusOutputStream modOut = this.formatBaseMessage();
      modOut.writeLRC();
      out.write(modOut.toAsciiHexByteArray());
   }

   @Override
   public void writeTcp(OutputStream out) throws IOException {
      ModbusOutputStream modOut = this.formatBaseMessage();
      byte[] ba = modOut.toByteArray();
      modOut = new ModbusOutputStream();
      modOut.write((byte)((this.transactionIdentifier & 0xFF00) >> 8));
      modOut.write((byte)(this.transactionIdentifier & 0xFF));
      modOut.write((byte)0);
      modOut.write((byte)0);
      int msgLen = ba.length;
      modOut.write((byte)((msgLen & 0xFF00) >> 8));
      modOut.write((byte)(msgLen & 0xFF));
      modOut.write(ba);
      out.write(modOut.toByteArray());
   }

   protected ModbusOutputStream formatBaseMessage() throws IOException {
      ModbusOutputStream out = new ModbusOutputStream();
      out.write((byte)this.deviceAddress);
      out.write((byte)this.functionCode);
      switch (this.functionCode) {
         case 5:
         case 6:
            out.writeWord(this.startAddress);
            out.write(this.data);
            break;
         case 15:
         case 16:
            out.writeWord(this.startAddress);
            out.writeWord(this.numberPoints);
            break;
         default:
            out.write((byte)this.byteCount);
            if ((this.functionCode & 128) == 0) {
               out.write(this.data);
            }
      }

      return out;
   }

   public boolean isError() {
      return this.exceptionCode != 0;
   }

   public String getString(int length) {
      if (length > this.byteCount) {
         throw new IllegalArgumentException("Incomplete string returned: ");
      } else {
         byte[] byteArray = new byte[this.byteCount];

         for (int i = 0; i < this.byteCount; i++) {
            byteArray[i] = this.data[i];
         }

         return new String(byteArray, StandardCharsets.UTF_8);
      }
   }

   public boolean getBinary(int index) {
      if (index >= this.numberPoints) {
         throw new IllegalArgumentException("Point not returned: " + index);
      } else {
         int byteOffset = index / 8;
         int bitOffset = index % 8;
         if (byteOffset >= this.byteCount) {
            throw new IllegalArgumentException("Point not returned: " + index);
         } else {
            return (this.data[byteOffset] & 1 << bitOffset) != 0;
         }
      }
   }

   public long getRegister(int index, int dataSize) {
      return this.getRegister(index, dataSize, BDataByteOrderEnum.order3210);
   }

   public long getRegister(int index, int dataSize, BDataByteOrderEnum byteOrder) {
      return this.getRegister(index, dataSize, byteOrder, false);
   }

   public long getRegister(int index, int dataSize, BDataByteOrderEnum byteOrder, boolean signed) {
      long value = 0L;
      if (index >= this.numberPoints) {
         throw new IllegalArgumentException("Point not returned: " + index);
      } else {
         int byteOffset = index * dataSize;
         if (byteOffset >= this.byteCount) {
            throw new IllegalArgumentException("Point not returned: " + index);
         } else {
            switch (dataSize) {
               case 2:
                  int iValue = this.data[byteOffset + 1] & 255 | (this.data[byteOffset] & 255) << 8;
                  if (signed && iValue >= 32768) {
                     iValue |= -65536;
                  }

                  return iValue;
               case 4:
                  if (byteOrder == BDataByteOrderEnum.order3210) {
                     value = this.data[byteOffset + 3] & 255
                        | (this.data[byteOffset + 2] & 255) << 8
                        | (this.data[byteOffset + 1] & 255) << 16
                        | (this.data[byteOffset] & 255) << 24;
                  } else if (byteOrder == BDataByteOrderEnum.order0123) {
                     value = this.data[byteOffset] & 255
                        | (this.data[byteOffset + 1] & 255) << 8
                        | (this.data[byteOffset + 2] & 255) << 16
                        | (this.data[byteOffset + 3] & 255) << 24;
                  } else {
                     value = this.data[byteOffset + 1] & 255
                        | (this.data[byteOffset] & 255) << 8
                        | (this.data[byteOffset + 3] & 255) << 16
                        | (this.data[byteOffset + 2] & 255) << 24;
                  }

                  if (signed) {
                     return value;
                  }

                  return value & 4294967295L;
               default:
                  throw new IllegalArgumentException("Unsupported data size: " + dataSize);
            }
         }
      }
   }

   public long get64BitLong(int index, BDataByteOrder64BitEnum byteOrder, boolean isSigned) {
      if (index >= this.numberPoints) {
         throw new IllegalArgumentException("Point not returned: " + index);
      } else {
         int byteOffset = index * 8;
         if (byteOffset >= this.byteCount) {
            throw new IllegalArgumentException("Point not returned: " + index);
         } else {
            long value = this.get64BitArrangedBits(byteOffset, byteOrder);
            return isSigned ? value : value & 1152921504606846975L;
         }
      }
   }

   public double getDouble(int index, BDataByteOrder64BitEnum byteOrder) {
      if (index >= this.numberPoints) {
         throw new IllegalArgumentException("Point not returned: " + index);
      } else {
         int byteOffset = index * 8;
         if (byteOffset >= this.byteCount) {
            throw new IllegalArgumentException("Point not returned: " + index);
         } else {
            long bits = this.get64BitArrangedBits(byteOffset, byteOrder);
            return Double.longBitsToDouble(bits);
         }
      }
   }

   public float getFloat(int index, int dataSize, BDataByteOrderEnum byteOrder) {
      if (index >= this.numberPoints) {
         throw new IllegalArgumentException("Point not returned: " + index);
      } else {
         int byteOffset = index * dataSize;
         if (byteOffset >= this.byteCount) {
            throw new IllegalArgumentException("Point not returned: " + index);
         } else {
            long bits;
            if (byteOrder == BDataByteOrderEnum.order3210) {
               bits = this.data[byteOffset + 3] & 255
                  | (this.data[byteOffset + 2] & 255) << 8
                  | (this.data[byteOffset + 1] & 255) << 16
                  | (this.data[byteOffset] & 255) << 24;
            } else if (byteOrder == BDataByteOrderEnum.order0123) {
               bits = this.data[byteOffset] & 255
                  | (this.data[byteOffset + 1] & 255) << 8
                  | (this.data[byteOffset + 2] & 255) << 16
                  | (this.data[byteOffset + 3] & 255) << 24;
            } else {
               bits = this.data[byteOffset + 1] & 255
                  | (this.data[byteOffset] & 255) << 8
                  | (this.data[byteOffset + 3] & 255) << 16
                  | (this.data[byteOffset + 2] & 255) << 24;
            }

            return Float.intBitsToFloat((int)bits);
         }
      }
   }

   @Override
   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      String comTypeString = "RTU";
      if (this.comType == 0) {
         comTypeString = "ASCII";
      } else if (this.comType == 2) {
         comTypeString = "TCP";
      }

      sb.append("Modbus " + comTypeString + " Response Message = " + TextUtil.getClassName(this.getClass()));
      sb.append("\n  Tag = " + this.getTag());
      sb.append("\n  Modbus Device Address = " + (this.deviceAddress & 0xFF));
      sb.append("\n  Modbus Function Code = " + this.functionCode);
      sb.append("\n  Modbus Exception Code = " + this.exceptionCode);
      sb.append("\n  Modbus Data Starting Address = " + this.startAddress);
      sb.append("\n  Modbus Number of Data Points = " + this.numberPoints);
      sb.append("\n  Modbus Byte Count = " + this.byteCount);
      sb.append("\n  Modbus Transaction ID = " + this.transactionIdentifier);
      sb.append("\n  Modbus Data = " + ByteArrayUtil.toHexString(this.data));

      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         this.write(out);
         sb.append("\n  Raw Bytes = " + ByteArrayUtil.toHexString(out.toByteArray()));
      } catch (Exception var4) {
      }

      return sb.toString();
   }

   public String getExceptionString() {
      switch (this.exceptionCode) {
         case -8:
            return "disabled";
         case -7:
            return "fault";
         case -6:
            return "down";
         case -5:
            return "lrc error";
         case -4:
         default:
            return "other error";
         case -3:
            return "unknown";
         case -2:
            return "ok not active";
         case -1:
            return "crc error";
         case 0:
            return "ok";
         case 1:
            return "illegal function";
         case 2:
            return "illegal data address";
         case 3:
            return "illegal data value";
         case 4:
            return "slave device failure";
         case 5:
            return "acknowledge";
         case 6:
            return "slave device busy";
         case 7:
            return "negative acknowledge";
         case 8:
            return "memory parity error";
         case 9:
            return "device timeout";
         case 10:
            return "gateway path unavailable";
         case 11:
            return "gateway target device failed to respond";
      }
   }

   private long get64BitArrangedBits(int byteOffset, BDataByteOrder64BitEnum byteOrder) {
      long bits = 0L;
      if (byteOrder == BDataByteOrder64BitEnum.order76543210) {
         bits = this.data[byteOffset + 7] & 255 | (this.data[byteOffset + 6] & 255) << 8 | (this.data[byteOffset + 5] & 255) << 16
            | (long)(this.data[byteOffset + 4] & 255) << 24
            | (long)(this.data[byteOffset + 3] & 255) << 32
            | (long)(this.data[byteOffset + 2] & 255) << 40
            | (long)(this.data[byteOffset + 1] & 255) << 48
            | (long)(this.data[byteOffset] & 255) << 56;
      } else if (byteOrder == BDataByteOrder64BitEnum.order67452301) {
         bits = this.data[byteOffset + 6] & 255 | (this.data[byteOffset + 7] & 255) << 8 | (this.data[byteOffset + 4] & 255) << 16
            | (long)(this.data[byteOffset + 5] & 255) << 24
            | (long)(this.data[byteOffset + 2] & 255) << 32
            | (long)(this.data[byteOffset + 3] & 255) << 40
            | (long)(this.data[byteOffset] & 255) << 48
            | (long)(this.data[byteOffset + 1] & 255) << 56;
      } else if (byteOrder == BDataByteOrder64BitEnum.order54761032) {
         bits = this.data[byteOffset + 5] & 255 | (this.data[byteOffset + 4] & 255) << 8 | (this.data[byteOffset + 7] & 255) << 16
            | (long)(this.data[byteOffset + 6] & 255) << 24
            | (long)(this.data[byteOffset + 1] & 255) << 32
            | (long)(this.data[byteOffset] & 255) << 40
            | (long)(this.data[byteOffset + 3] & 255) << 48
            | (long)(this.data[byteOffset + 2] & 255) << 56;
      } else if (byteOrder == BDataByteOrder64BitEnum.order45670123) {
         bits = this.data[byteOffset + 4] & 255 | (this.data[byteOffset + 5] & 255) << 8 | (this.data[byteOffset + 6] & 255) << 16
            | (long)(this.data[byteOffset + 7] & 255) << 24
            | (long)(this.data[byteOffset] & 255) << 32
            | (long)(this.data[byteOffset + 1] & 255) << 40
            | (long)(this.data[byteOffset + 2] & 255) << 48
            | (long)(this.data[byteOffset + 3] & 255) << 56;
      } else if (byteOrder == BDataByteOrder64BitEnum.order01234567) {
         bits = this.data[byteOffset] & 255 | (this.data[byteOffset + 1] & 255) << 8 | (this.data[byteOffset + 2] & 255) << 16
            | (long)(this.data[byteOffset + 3] & 255) << 24
            | (long)(this.data[byteOffset + 4] & 255) << 32
            | (long)(this.data[byteOffset + 5] & 255) << 40
            | (long)(this.data[byteOffset + 6] & 255) << 48
            | (long)(this.data[byteOffset + 7] & 255) << 56;
      } else if (byteOrder == BDataByteOrder64BitEnum.order10325476) {
         bits = this.data[byteOffset + 1] & 255 | (this.data[byteOffset] & 255) << 8 | (this.data[byteOffset + 3] & 255) << 16
            | (long)(this.data[byteOffset + 2] & 255) << 24
            | (long)(this.data[byteOffset + 5] & 255) << 32
            | (long)(this.data[byteOffset + 4] & 255) << 40
            | (long)(this.data[byteOffset + 7] & 255) << 48
            | (long)(this.data[byteOffset + 6] & 255) << 56;
      } else if (byteOrder == BDataByteOrder64BitEnum.order23016745) {
         bits = this.data[byteOffset + 2] & 255 | (this.data[byteOffset + 3] & 255) << 8 | (this.data[byteOffset] & 255) << 16
            | (long)(this.data[byteOffset + 1] & 255) << 24
            | (long)(this.data[byteOffset + 6] & 255) << 32
            | (long)(this.data[byteOffset + 7] & 255) << 40
            | (long)(this.data[byteOffset + 4] & 255) << 48
            | (long)(this.data[byteOffset + 5] & 255) << 56;
      } else if (byteOrder == BDataByteOrder64BitEnum.order32107654) {
         bits = this.data[byteOffset + 3] & 255 | (this.data[byteOffset + 2] & 255) << 8 | (this.data[byteOffset + 1] & 255) << 16
            | (long)(this.data[byteOffset] & 255) << 24
            | (long)(this.data[byteOffset + 7] & 255) << 32
            | (long)(this.data[byteOffset + 6] & 255) << 40
            | (long)(this.data[byteOffset + 5] & 255) << 48
            | (long)(this.data[byteOffset + 4] & 255) << 56;
      }

      return bits;
   }
}
