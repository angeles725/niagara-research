package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import com.tridium.modbusCore.enums.BDataTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import com.tridium.modbusCore.messages.ModbusReadRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import com.tridium.modbusCore.point.BIModbusNumericBitsProxyExt;
import com.tridium.modbusCore.util.DataTypeUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "beginningBit",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, 15)")}
   ), @NiagaraProperty(
      name = "numberOfBits",
      type = "int",
      defaultValue = "1",
      facets = {@Facet("BFacets.makeInt(null, 1, 16)")}
   )})
public class BModbusClientNumericBitsProxyExt extends BModbusClientNumericProxyExt implements BIModbusNumericBitsProxyExt {
   public static final Property beginningBit = newProperty(0, 0, BFacets.makeInt(null, 0, 15));
   public static final Property numberOfBits = newProperty(0, 1, BFacets.makeInt(null, 1, 16));
   public static final Type TYPE = Sys.loadType(BModbusClientNumericBitsProxyExt.class);

   @Override
   public int getBeginningBit() {
      return this.getInt(beginningBit);
   }

   @Override
   public void setBeginningBit(int v) {
      this.setInt(beginningBit, v, null);
   }

   @Override
   public int getNumberOfBits() {
      return this.getInt(numberOfBits);
   }

   @Override
   public void setNumberOfBits(int v) {
      this.setInt(numberOfBits, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void changed(Property prop, Context context) {
      if (!this.isRunning()) {
         super.changed(prop, context);
      } else {
         if (!prop.equals(beginningBit) && !prop.equals(numberOfBits)) {
            if (prop.equals(dataType)) {
               if (context != noAddressCheck && !this.getDataType().equals(BDataTypeEnum.integerType)) {
                  this.set(dataType, BDataTypeEnum.integerType, noAddressCheck);
               }

               this.setStale(true, null);
               if (this.getDevice() != null) {
                  this.adjustPollSubscription();
               }

               if (this.getParentPoint().isWritablePoint()) {
                  if (((BModbusClientDevice)this.getDevice()).writablePointAlreadyExists(this)) {
                     this.modbusNet()
                        .getModbusLog()
                        .error(this.getParent().getName() + " Duplicate Writable point for register address: " + this.getDataAddress());
                     this.set(dataAddress, new BFlexAddress(BAddressFormatEnum.hex, "-1"), null);
                  } else {
                     this.getTuning().writeDesired();
                  }
               }
            } else {
               super.changed(prop, context);
            }
         } else {
            if (context != noAddressCheck) {
               this.checkBitFieldSize();
            }

            if (((BModbusClientDevice)this.getDevice()).writablePointAlreadyExists(this)) {
               this.modbusNet().getModbusLog().error(this.getParent().getName() + " Duplicate Writable point for register address: " + this.getDataAddress());
               this.setDataAddress(new BFlexAddress(BAddressFormatEnum.hex, "-1"));
            }
         }
      }
   }

   @Override
   public void started() throws Exception {
      Property prop = this.getProperty("conversion");
      this.setFlags(prop, this.getFlags(prop) | 4);
      prop = this.getProperty("dataType");
      this.setFlags(prop, this.getFlags(prop) | 4);
      if (!this.getDataType().equals(BDataTypeEnum.integerType)) {
         this.setDataType(BDataTypeEnum.integerType);
      }

      this.checkBitFieldSize();
      super.started();
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
      int regValue;
      try {
         regValue = this.getBitsValue(
            (int)rec.getRegister(0, dataSize, ((BModbusClientDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned())
         );
      } catch (IllegalArgumentException var5) {
         this.readFail("error parsing integer value (" + var5 + ")");
         return;
      }

      this.readOk(new BStatusNumeric(regValue));
   }

   private void set64BitLongOutValues(ModbusResponse rec) {
      try {
         int regValue = this.getBitsValue(
            (int)rec.get64BitLong(0, ((BModbusClientDevice)this.getDevice()).getLong64BitDataByteOrder(), this.isDataTypeSigned())
         );
         this.readOk(new BStatusNumeric(regValue));
      } catch (IllegalArgumentException var3) {
         this.readFail("error parsing 64-bit long value (" + var3 + ")");
      }
   }

   private void setFloatOutValues(ModbusResponse rec, int dataSize) {
      float regValue;
      try {
         regValue = this.getBitsValue(rec.getFloat(0, dataSize, ((BModbusClientDevice)this.getDevice()).getFloatDataByteOrder()));
      } catch (IllegalArgumentException var5) {
         this.readFail("error parsing float value (" + var5 + ")");
         return;
      }

      this.readOk(new BStatusNumeric(regValue));
   }

   private void setDoubleOutValues(ModbusResponse rec) {
      double regValue;
      try {
         regValue = this.getBitsValue(rec.getDouble(0, ((BModbusClientDevice)this.getDevice()).getDouble64BitDataByteOrder()));
      } catch (IllegalArgumentException var5) {
         this.readFail("error parsing double value (" + var5 + ")");
         return;
      }

      this.readOk(new BStatusNumeric(regValue));
   }

   @Override
   public boolean updateOutput(BStatusValue out) {
      float fValue = (float)((BStatusNumeric)out).getValue();
      if (fValue > this.getMask()) {
         this.writeFail(fValue + " is greater than maximum value of " + this.getMask() + " (" + this.getNumberOfBits() + " bits)");
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
                     fValue, (float)resp.getRegister(0, 2, ((BModbusClientDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned())
                  )
               );
            } else if (this.isDataTypeLong()) {
               dataOut = this.setLongByteArray(
                  this.mergeBitsValue(
                     fValue, (float)resp.getRegister(0, 4, ((BModbusClientDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned())
                  )
               );
            } else if (this.isDataType64BitLong()) {
               dataOut = this.setLong8ByteArray(
                  this.mergeBitsValue(
                     fValue, (float)resp.get64BitLong(0, ((BModbusClientDevice)this.getDevice()).getLong64BitDataByteOrder(), this.isDataTypeSigned())
                  )
               );
            } else if (this.isDataTypeDouble()) {
               dataOut = this.setDoubleByteArray(
                  this.mergeBitsValue((int)fValue, resp.getDouble(0, ((BModbusClientDevice)this.getDevice()).getDouble64BitDataByteOrder()))
               );
            } else {
               dataOut = this.setFloatByteArray(
                  this.mergeBitsValue(fValue, resp.getFloat(0, 4, ((BModbusClientDevice)this.getDevice()).getFloatDataByteOrder()))
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

   @Override
   public boolean isValidAddress(BFlexAddress addr) {
      if (!addr.isModbusFormat()) {
         return addr.isValid();
      } else if (addr.isModbusAnalogAddress()) {
         if (this.getParentPoint().isWritablePoint()) {
            return addr.isModbusHoldingAddress();
         } else {
            if (addr.isModbusHoldingAddress()) {
               this.set(regType, BRegisterTypeEnum.holding, noAddressCheck);
            } else {
               this.set(regType, BRegisterTypeEnum.input, noAddressCheck);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public void checkBitFieldSize() {
      int beginBit = this.getBeginningBit();
      int numBits = this.getNumberOfBits();
      if (beginBit + numBits > 16) {
         this.modbusNet().getModbusLog().error(this.getParent().getName() + " bits " + beginBit + "-" + (beginBit + numBits - 1) + " exceed register bounds.");
         this.setInt(this.getProperty("numberOfBits"), 16 - beginBit, noAddressCheck);
      }
   }

   public float getBitsValue(float val) {
      return Integer.valueOf(this.getBitsValue((int)val)).floatValue();
   }

   public double getBitsValue(double val) {
      return Integer.valueOf(this.getBitsValue((int)val)).doubleValue();
   }

   public int getBitsValue(int val) {
      return (val & this.getRegisterMask()) >> this.getBeginningBit();
   }

   public float mergeBitsValue(float val, float reg) {
      return this.mergeBitsValue((int)val, reg);
   }

   public float mergeBitsValue(int val, float reg) {
      return (int)reg & ~this.getRegisterMask() | val << this.getBeginningBit();
   }

   public double mergeBitsValue(int val, double reg) {
      return (long)reg & ~this.getRegisterMask() | (long)val << this.getBeginningBit();
   }

   public int getMask() {
      int mask = 0;
      int bits = this.getNumberOfBits();

      for (int i = 0; i < bits; i++) {
         mask <<= 1;
         mask |= 1;
      }

      return mask;
   }

   public int getRegisterMask() {
      return this.getMask() << this.getBeginningBit();
   }
}
