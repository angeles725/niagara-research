package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigEntry;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import com.tridium.modbusCore.enums.BDataTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.messages.ModbusReadRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import com.tridium.modbusCore.point.BIModbusNumericProxyExt;
import com.tridium.modbusCore.util.ByteConverterUtil;
import com.tridium.modbusCore.util.DataTypeUtil;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "regType",
      type = "BRegisterTypeEnum",
      defaultValue = "BRegisterTypeEnum.holding"
   ), @NiagaraProperty(
      name = "dataType",
      type = "BDataTypeEnum",
      defaultValue = "BDataTypeEnum.integerType"
   )})
public class BModbusClientNumericProxyExt extends BModbusClientProxyExt implements BIModbusNumericProxyExt {
   public static final Property regType = newProperty(0, BRegisterTypeEnum.holding, null);
   public static final Property dataType = newProperty(0, BDataTypeEnum.integerType, null);
   public static final Type TYPE = Sys.loadType(BModbusClientNumericProxyExt.class);

   @Override
   public BRegisterTypeEnum getRegType() {
      return (BRegisterTypeEnum)this.get(regType);
   }

   @Override
   public void setRegType(BRegisterTypeEnum v) {
      this.set(regType, v, null);
   }

   @Override
   public BDataTypeEnum getDataType() {
      return (BDataTypeEnum)this.get(dataType);
   }

   @Override
   public void setDataType(BDataTypeEnum v) {
      this.set(dataType, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      if (this.getParentPoint().isWritablePoint()) {
         this.setFlags(regType, 1);
      }
   }

   public BReadWriteMode getMode() {
      return this.getParentPoint().isWritablePoint() ? BReadWriteMode.readWrite : BReadWriteMode.readonly;
   }

   @Override
   public void read() {
      if (!this.configFault) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         int address = device.getDeviceAddress();
         int pointAddress = this.getAbsoluteAddress().getDataAddress();
         int count = DataTypeUtil.getRegisterCount(this.getDataType());
         int code;
         if (this.isHoldingRegisterType()) {
            code = 3;
         } else {
            code = 4;
         }

         ModbusReadRequest req = new ModbusReadRequest(device.modbusNet().getModbusMode(), device, address, code, pointAddress, count);
         ModbusResponse rsp = (ModbusResponse)device.sendModbusMessage(req);
         if (rsp == null) {
            rsp = new ModbusResponse(device.modbusNet().getModbusMode(), device);
            rsp.exceptionCode = 9;
         }

         this.setOutValues(rsp);
      }
   }

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
         long regValue = rec.getRegister(0, dataSize, ((BModbusClientDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned());
         this.readOk(new BStatusNumeric(regValue));
      } catch (IllegalArgumentException var5) {
         this.readFail("error parsing integer value (" + var5 + ")");
      }
   }

   private void set64BitLongOutValues(ModbusResponse rec) {
      try {
         long regValue = rec.get64BitLong(0, ((BModbusClientDevice)this.getDevice()).getLong64BitDataByteOrder(), this.isDataTypeSigned());
         this.readOk(new BStatusNumeric(regValue));
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing 64-bit long value (" + var4 + ")");
      }
   }

   private void setFloatOutValues(ModbusResponse rec, int dataSize) {
      try {
         float regValue = rec.getFloat(0, dataSize, ((BModbusClientDevice)this.getDevice()).getFloatDataByteOrder());
         this.readOk(new BStatusNumeric(regValue));
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing float value (" + var4 + ")");
      }
   }

   private void setDoubleOutValues(ModbusResponse rec) {
      try {
         double regValue = rec.getDouble(0, ((BModbusClientDevice)this.getDevice()).getDouble64BitDataByteOrder());
         this.readOk(new BStatusNumeric(regValue));
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing double value (" + var4 + ")");
      }
   }

   @Override
   public BRegisterTypesEnum determineRegisterType() {
      return this.getRegType().equals(BRegisterTypeEnum.holding) ? BRegisterTypesEnum.holdingRegister : BRegisterTypesEnum.inputRegister;
   }

   @Override
   public BEnum getRegisterType() {
      return this.getRegType();
   }

   @Override
   public int determineNumRegisters() {
      return DataTypeUtil.getRegisterCount(this.getDataType());
   }

   @Override
   public void changed(Property prop, Context context) {
      if (!this.isRunning()) {
         super.changed(prop, context);
      } else {
         if (prop.equals(dataType)) {
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
         } else if (prop.equals(regType)) {
            if (context != noAddressCheck) {
               if (this.getParentPoint().isWritablePoint() && !this.isHoldingRegisterType()) {
                  this.set(regType, BRegisterTypeEnum.holding, noAddressCheck);
               } else if (this.getDataAddress().isModbusFormat()) {
                  this.setStale(true, null);
                  if (this.getDataAddress().isModbusHoldingAddress()) {
                     if (!this.isHoldingRegisterType()) {
                        this.set(regType, BRegisterTypeEnum.holding, noAddressCheck);
                     }
                  } else if (this.isHoldingRegisterType()) {
                     this.set(regType, BRegisterTypeEnum.input, noAddressCheck);
                  }
               }
            }

            if (this.getDevice() != null) {
               if (this.getParentPoint().isWritablePoint() && ((BModbusClientDevice)this.getDevice()).writablePointAlreadyExists(this)) {
                  this.modbusNet()
                     .getModbusLog()
                     .error(this.getParent().getName() + " Duplicate Writable point for register address: " + this.getDataAddress());
                  this.setDataAddress(new BFlexAddress(BAddressFormatEnum.hex, "-1"));
               }

               this.adjustPollSubscription();
            }
         } else {
            super.changed(prop, context);
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

   public boolean isHoldingRegisterType() {
      return this.getRegType().equals(BRegisterTypeEnum.holding);
   }

   public boolean isDataTypeInteger() {
      return DataTypeUtil.is16BitInteger(this.getDataType());
   }

   public boolean isDataTypeLong() {
      return DataTypeUtil.is32BitLong(this.getDataType());
   }

   public boolean isDataType64BitLong() {
      return DataTypeUtil.is64BitLong(this.getDataType());
   }

   public boolean isDataTypeDouble() {
      return DataTypeUtil.isDouble(this.getDataType());
   }

   public boolean isDataTypeFloat() {
      return DataTypeUtil.isFloat(this.getDataType());
   }

   public boolean isDataTypeSigned() {
      return DataTypeUtil.isSigned(this.getDataType());
   }

   @Override
   public void devicePoll(BDevicePollConfigEntry entry) {
      if (!this.configFault && !this.isUnoperational()) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         int numRegisters = DataTypeUtil.getRegisterCount(this.getDataType());
         ModbusResponse rsp = new ModbusResponse(device.modbusNet().getModbusMode(), device);
         int pointAddress = this.getAbsoluteAddress().getDataAddress();

         try {
            if (this.isHoldingRegisterType()) {
               rsp.data = device.getHoldingRegisterValues(pointAddress, numRegisters, entry);
               rsp.exceptionCode = device.getHoldingRegistersReadStatus(pointAddress, numRegisters, entry).getErrorCode();
            } else {
               rsp.data = device.getInputRegisterValues(pointAddress, numRegisters, entry);
               rsp.exceptionCode = device.getInputRegistersReadStatus(pointAddress, numRegisters, entry).getErrorCode();
            }

            rsp.byteCount = (byte)rsp.data.length;
            rsp.numberPoints = numRegisters;
            this.setOutValues(rsp);
         } catch (ModbusException var7) {
            if (this.modbusNet().getModbusLog().isTraceOn()) {
               this.modbusNet().getModbusLog().trace(this.getParent().getName() + ">>> devicePoll error", var7);
            }
         }
      }
   }

   public void doWrite(BStatusValue out) {
      if (this.getParentPoint().isWritablePoint() && !this.configFault) {
         if (this.modbusNet() != null) {
            if (out == null) {
               this.updateOutput(this.getWriteValue());
               if (this.getParentPoint().isSubscribed()) {
                  this.read();
               }
            } else {
               this.updateOutput(out);
               if (this.getParentPoint().isSubscribed()) {
                  this.read();
               }
            }
         }
      } else if (!this.getParentPoint().isWritablePoint() && this.modbusNet().getModbusLog().isTraceOn()) {
         this.modbusNet().getModbusLog().trace(this.getParent().getName() + "- This ModbusClient float point is not writable, disregarding write request.");
      }

      super.doWrite(out);
   }

   public boolean updateOutput(BStatusValue out) {
      double fValue = ((BStatusNumeric)out).getValue();
      BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
      BModbusClientNetwork network = (BModbusClientNetwork)this.modbusNet();
      int address = device.getDeviceAddress();
      int pointAddress = this.getAbsoluteAddress().getDataAddress();
      byte[] dataOut;
      if (this.isDataTypeInteger()) {
         dataOut = this.setIntegerByteArray(fValue);
      } else if (this.isDataTypeLong()) {
         dataOut = this.setLongByteArray(fValue);
      } else if (this.isDataType64BitLong()) {
         dataOut = ByteConverterUtil.to8ByteLongArray(fValue, device.getLong64BitDataByteOrder(), this.isDataTypeSigned());
      } else if (this.isDataTypeDouble()) {
         dataOut = ByteConverterUtil.to8ByteDoubleArray(fValue, device.getDouble64BitDataByteOrder());
      } else {
         dataOut = this.setFloatByteArray(fValue);
      }

      int count = DataTypeUtil.getRegisterCount(this.getDataType());
      int code = 6;
      if (device.isPresetMultiple()) {
         code = 16;
      }

      ModbusWriteRequest req = new ModbusWriteRequest(network.getModbusMode(), device, address, code, pointAddress, count, dataOut);
      ModbusResponse resp = (ModbusResponse)device.sendModbusMessage(req);
      if (resp == null) {
         resp = new ModbusResponse(network.getModbusMode(), device);
         resp.exceptionCode = 9;
      }

      if (resp.isError() && resp.exceptionCode != 5) {
         this.writeFail(resp.getExceptionString());
         return false;
      } else {
         if (code == 6 && count > 1) {
            for (int writeRegNum = 2; writeRegNum <= count; writeRegNum++) {
               int startOffset = (writeRegNum - 1) * 2;
               dataOut[0] = dataOut[startOffset];
               dataOut[1] = dataOut[startOffset + 1];
               req = new ModbusWriteRequest(network.getModbusMode(), device, address, code, pointAddress + writeRegNum - 1, 1, dataOut);
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

   protected byte[] setIntegerByteArray(double fValue) {
      return ByteConverterUtil.to2ByteIntArray(fValue, this.isDataTypeSigned());
   }

   protected byte[] setLongByteArray(double fValue) {
      BModbusDevice device = (BModbusDevice)this.getDevice();
      return ByteConverterUtil.to4ByteLongArray(fValue, device.getLongDataByteOrder(), this.isDataTypeSigned());
   }

   protected byte[] setFloatByteArray(double fValue) {
      BModbusDevice device = (BModbusDevice)this.getDevice();
      return ByteConverterUtil.to4ByteFloatArray((float)fValue, device.getFloatDataByteOrder());
   }

   protected byte[] setLong8ByteArray(double value) {
      BModbusDevice device = (BModbusDevice)this.getDevice();
      return ByteConverterUtil.to8ByteLongArray(value, device.getLong64BitDataByteOrder(), this.isDataTypeSigned());
   }

   protected byte[] setDoubleByteArray(double value) {
      BModbusDevice device = (BModbusDevice)this.getDevice();
      return ByteConverterUtil.to8ByteDoubleArray(value, device.getDouble64BitDataByteOrder());
   }
}
