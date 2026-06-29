package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.messages.ModbusReadRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import com.tridium.modbusCore.point.BIModbusEnumBitsProxyExt;
import com.tridium.modbusCore.util.DataTypeUtil;
import javax.baja.control.BEnumPoint;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusClientEnumBitsProxyExt extends BModbusClientNumericBitsProxyExt implements BIModbusEnumBitsProxyExt {
   public static final Type TYPE = Sys.loadType(BModbusClientEnumBitsProxyExt.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void setOutValues(ModbusResponse rec) {
      if (!rec.isError()) {
         if (this.isDataTypeInteger()) {
            this.setIntegerOutValues(rec, 2);
         } else if (this.isDataTypeLong()) {
            this.setIntegerOutValues(rec, 4);
         } else if (this.isDataType64BitLong()) {
            this.set64BitLongOutValues(rec);
         } else if (this.isDataTypeDouble()) {
            this.setDoubleOutValues(rec);
         } else if (this.isDataTypeFloat()) {
            this.setFloatOutValues(rec, 4);
         }
      } else {
         this.readFail(rec.getExceptionString());
      }
   }

   private void setIntegerOutValues(ModbusResponse rec, int dataSize) {
      try {
         int regValue = this.getBitsValue(
            (int)rec.getRegister(0, dataSize, ((BModbusClientDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned())
         );
         this.readOk(new BStatusEnum(this.getEnum(regValue)));
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing integer value (" + var4 + ")");
      }
   }

   private void set64BitLongOutValues(ModbusResponse rec) {
      try {
         int regValue = this.getBitsValue(
            (int)rec.get64BitLong(0, ((BModbusClientDevice)this.getDevice()).getLong64BitDataByteOrder(), this.isDataTypeSigned())
         );
         this.readOk(new BStatusEnum(this.getEnum(regValue)));
      } catch (IllegalArgumentException var3) {
         this.readFail("error parsing 64-bit long value (" + var3 + ")");
      }
   }

   private void setFloatOutValues(ModbusResponse rec, int dataSize) {
      try {
         float regValue = this.getBitsValue(rec.getFloat(0, dataSize, ((BModbusClientDevice)this.getDevice()).getFloatDataByteOrder()));
         this.readOk(new BStatusEnum(this.getEnum((int)regValue)));
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing float value (" + var4 + ")");
      }
   }

   private void setDoubleOutValues(ModbusResponse rec) {
      try {
         double regValue = this.getBitsValue((int)rec.getDouble(0, ((BModbusClientDevice)this.getDevice()).getDouble64BitDataByteOrder()));
         this.readOk(new BStatusEnum(this.getEnum((int)regValue)));
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing double value (" + var4 + ")");
      }
   }

   @Override
   public boolean updateOutput(BStatusValue out) {
      int ordinal = ((BStatusEnum)out).getValue().getOrdinal();
      if (ordinal > this.getMask()) {
         this.writeFail("Enum ordinal " + ordinal + " is greater than maximum value of " + this.getMask() + " (" + this.getNumberOfBits() + " bits)");
         return false;
      } else {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         BModbusClientNetwork network = (BModbusClientNetwork)this.modbusNet();
         int address = device.getDeviceAddress();
         int pointAddress = this.getAbsoluteAddress().getDataAddress();
         int count = DataTypeUtil.getRegisterCount(this.getDataType());
         int code;
         if (this.isHoldingRegisterType()) {
            code = 3;
         } else {
            code = 4;
         }

         ModbusReadRequest readReq = new ModbusReadRequest(network.getModbusMode(), device, address, code, pointAddress, count);
         ModbusResponse resp = (ModbusResponse)device.sendModbusMessage(readReq);
         if (resp == null) {
            resp = new ModbusResponse(network.getModbusMode(), device);
            resp.exceptionCode = 9;
         }

         if (resp.isError() && resp.exceptionCode != 5) {
            this.writeFail(resp.getExceptionString());
            return false;
         } else {
            byte[] dataOut;
            if (this.isDataTypeInteger()) {
               dataOut = this.setIntegerByteArray(
                  this.mergeBitsValue(
                     ordinal, (float)resp.getRegister(0, 2, ((BModbusClientDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned())
                  )
               );
            } else if (this.isDataTypeLong()) {
               dataOut = this.setIntegerByteArray(
                  this.mergeBitsValue(
                     ordinal, (float)resp.getRegister(0, 4, ((BModbusClientDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned())
                  )
               );
            } else if (this.isDataType64BitLong()) {
               dataOut = this.setLong8ByteArray(
                  this.mergeBitsValue(
                     ordinal, (float)resp.get64BitLong(0, ((BModbusClientDevice)this.getDevice()).getLong64BitDataByteOrder(), this.isDataTypeSigned())
                  )
               );
            } else if (this.isDataTypeDouble()) {
               dataOut = this.setDoubleByteArray(
                  this.mergeBitsValue(ordinal, resp.getDouble(0, ((BModbusClientDevice)this.getDevice()).getDouble64BitDataByteOrder()))
               );
            } else {
               dataOut = this.setFloatByteArray(
                  this.mergeBitsValue(ordinal, resp.getFloat(0, 4, ((BModbusClientDevice)this.getDevice()).getFloatDataByteOrder()))
               );
            }

            int var15 = 6;
            if (device.isPresetMultiple()) {
               var15 = 16;
            }

            ModbusWriteRequest req = new ModbusWriteRequest(network.getModbusMode(), device, address, var15, pointAddress, count, dataOut);
            resp = (ModbusResponse)device.sendModbusMessage(req);
            if (resp == null) {
               resp = new ModbusResponse(network.getModbusMode(), device);
               resp.exceptionCode = 9;
            }

            if (resp.isError() && resp.exceptionCode != 5) {
               this.writeFail(resp.getExceptionString());
               return false;
            } else {
               if (var15 == 6 && count > 1) {
                  for (int writeRegNum = 2; writeRegNum <= count; writeRegNum++) {
                     int startOffset = (writeRegNum - 1) * 2;
                     dataOut[0] = dataOut[startOffset];
                     dataOut[1] = dataOut[startOffset + 1];
                     req = new ModbusWriteRequest(network.getModbusMode(), device, address, var15, pointAddress + writeRegNum - 1, 1, dataOut);
                     resp = (ModbusResponse)device.sendModbusMessage(req);
                     if (resp == null) {
                        resp = new ModbusResponse(network.getModbusMode(), device);
                        resp.exceptionCode = 9;
                        break;
                     }

                     if (resp.isError() && resp.exceptionCode != 5) {
                        this.writeFail(resp.getExceptionString());
                        return false;
                     }
                  }
               }

               this.writeOk(out);
               return true;
            }
         }
      }
   }

   private BEnum getEnum(int ordinal) {
      BEnumRange range = (BEnumRange)((BEnumPoint)this.getParent()).getFacets().get("range");
      return (BEnum)(range == null ? BDynamicEnum.make(ordinal) : range.get(ordinal, true));
   }

   @Override
   public float mergeBitsValue(float val, float reg) {
      return this.mergeBitsValue((int)val, reg);
   }

   @Override
   public float mergeBitsValue(int val, float reg) {
      float outVal = ((BEnumPoint)this.getParent()).getOut().getValue().getOrdinal();
      return (int)reg & ~this.getRegisterMask() | val << this.getBeginningBit();
   }
}
