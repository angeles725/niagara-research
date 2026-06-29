package com.tridium.modbusCore.server.point;

import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BDataByteOrderEnum;
import com.tridium.modbusCore.enums.BDataTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.point.BIModbusNumericProxyExt;
import com.tridium.modbusCore.server.BModbusServerDevice;
import com.tridium.modbusCore.util.ByteConverterUtil;
import com.tridium.modbusCore.util.DataTypeUtil;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
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
public class BModbusServerNumericProxyExt extends BModbusServerProxyExt implements BIModbusNumericProxyExt {
   public static final Property regType = newProperty(0, BRegisterTypeEnum.holding, null);
   public static final Property dataType = newProperty(0, BDataTypeEnum.integerType, null);
   public static final Type TYPE = Sys.loadType(BModbusServerNumericProxyExt.class);

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
   public void read() {
      if (!this.configFault) {
         BModbusServerDevice device = (BModbusServerDevice)this.getDevice();
         int numRegisters = DataTypeUtil.getRegisterCount(this.getDataType());
         ModbusResponse rsp = new ModbusResponse(this.modbusNet().getModbusMode(), device);
         int pointAddress = this.getDataAddress().getDataAddress();

         try {
            if (this.isHoldingRegisterType()) {
               rsp.data = device.getHoldingRegisterValues(pointAddress, numRegisters);
            } else {
               rsp.data = device.getInputRegisterValues(pointAddress, numRegisters);
            }
         } catch (Exception var6) {
            if (this.modbusNet() != null && this.modbusNet().getModbusLog().isTraceOn()) {
               this.modbusNet().getModbusLog().trace(this.getParent().getName() + ": BModbusServerNumericProxyExt.read() caught exception: ", var6);
            }

            this.readFail("Exception during read (" + var6 + ")");
            return;
         }

         rsp.exceptionCode = 0;
         rsp.byteCount = (byte)rsp.data.length;
         rsp.numberPoints = numRegisters;
         this.setOutValues(rsp);
      }
   }

   private void setOutValues(ModbusResponse rec) {
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
      long regValue;
      try {
         regValue = rec.getRegister(0, dataSize, ((BModbusServerDevice)this.getDevice()).getLongDataByteOrder(), this.isDataTypeSigned());
      } catch (IllegalArgumentException var6) {
         this.readFail("error parsing integer value (" + var6 + ")");
         return;
      }

      BStatusNumeric val = new BStatusNumeric(regValue);
      if (this.getStatus().isValid() && this.lastReadValue != null && this.lastReadValue.equivalent(val)) {
         this.getTuning().readOk();
      } else {
         this.readOk(val);
      }
   }

   private void set64BitLongOutValues(ModbusResponse rec) {
      long regValue;
      try {
         regValue = rec.get64BitLong(0, ((BModbusServerDevice)this.getDevice()).getLong64BitDataByteOrder(), this.isDataTypeSigned());
      } catch (IllegalArgumentException var5) {
         this.readFail("error parsing 64-bit long value (" + var5 + ")");
         return;
      }

      BStatusNumeric val = new BStatusNumeric(regValue);
      if (this.getStatus().isValid() && this.lastReadValue != null && this.lastReadValue.equivalent(val)) {
         this.getTuning().readOk();
      } else {
         this.readOk(val);
      }
   }

   private void setFloatOutValues(ModbusResponse rec, int dataSize) {
      float regValue;
      try {
         regValue = rec.getFloat(0, dataSize, ((BModbusServerDevice)this.getDevice()).getFloatDataByteOrder());
      } catch (IllegalArgumentException var5) {
         this.readFail("error parsing float value (" + var5 + ")");
         return;
      }

      BStatusNumeric val = new BStatusNumeric(regValue);
      if (this.getStatus().isValid() && this.lastReadValue != null && this.lastReadValue.equivalent(val)) {
         this.getTuning().readOk();
      } else {
         this.readOk(val);
      }
   }

   private void setDoubleOutValues(ModbusResponse rec) {
      double regValue;
      try {
         regValue = rec.getDouble(0, ((BModbusServerDevice)this.getDevice()).getDouble64BitDataByteOrder());
      } catch (IllegalArgumentException var5) {
         this.readFail("error parsing float value (" + var5 + ")");
         return;
      }

      BStatusNumeric val = new BStatusNumeric(regValue);
      if (this.getStatus().isValid() && this.lastReadValue != null && this.lastReadValue.equivalent(val)) {
         this.getTuning().readOk();
      } else {
         this.readOk(val);
      }
   }

   @Override
   public BRegisterTypesEnum determineRegisterType() {
      return this.getRegType().equals(BRegisterTypeEnum.holding) ? BRegisterTypesEnum.holdingRegister : BRegisterTypesEnum.inputRegister;
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(dataType)) {
            this.setStale(true, null);
            this.checkConfiguration();
            if (this.getParentPoint().isWritablePoint()) {
               this.getTuning().writeDesired();
            }
         } else if (prop.equals(regType) && context != noAddressCheck) {
            this.setStale(true, null);
            if (this.getDataAddress().isModbusFormat()) {
               if (this.getDataAddress().isModbusHoldingAddress()) {
                  if (!this.isHoldingRegisterType()) {
                     this.set(regType, BRegisterTypeEnum.holding, noAddressCheck);
                  }
               } else if (this.isHoldingRegisterType()) {
                  this.set(regType, BRegisterTypeEnum.input, noAddressCheck);
               }
            }

            this.checkConfiguration();
            if (this.getParentPoint().isWritablePoint()) {
               this.getTuning().writeDesired();
            }
         }
      }
   }

   @Override
   protected boolean isValidAddress(BFlexAddress addr) {
      if (!addr.isModbusFormat()) {
         BModbusServerDevice device = (BModbusServerDevice)this.getDevice();
         int numRegs = DataTypeUtil.getRegisterCount(this.getDataType());
         return this.isHoldingRegisterType()
            ? device.isHoldingRegisterAddressValid(addr.getDataAddress(), numRegs)
            : device.isInputRegisterAddressValid(addr.getDataAddress(), numRegs);
      } else if (addr.isModbusAnalogAddress()) {
         BRegisterTypeEnum rType = BRegisterTypeEnum.holding;
         if (addr.isModbusHoldingAddress()) {
            this.set(regType, BRegisterTypeEnum.holding, noAddressCheck);
         } else {
            this.set(regType, BRegisterTypeEnum.input, noAddressCheck);
            rType = BRegisterTypeEnum.input;
         }

         BModbusServerDevice device = (BModbusServerDevice)this.getDevice();
         int numRegs = DataTypeUtil.getRegisterCount(this.getDataType());
         return rType.equals(BRegisterTypeEnum.holding)
            ? device.isHoldingRegisterAddressValid(addr.getDataAddress(), numRegs)
            : device.isInputRegisterAddressValid(addr.getDataAddress(), numRegs);
      } else {
         return false;
      }
   }

   private boolean isHoldingRegisterType() {
      return this.getRegType().equals(BRegisterTypeEnum.holding);
   }

   private boolean isDataTypeInteger() {
      return this.getDataType().equals(BDataTypeEnum.integerType) ? true : this.getDataType().equals(BDataTypeEnum.signedInteger);
   }

   public boolean isDataTypeLong() {
      return DataTypeUtil.is32BitLong(this.getDataType());
   }

   public boolean isDataTypeSignedLong() {
      return this.getDataType().equals(BDataTypeEnum.longType);
   }

   public boolean isDataTypeUnsignedLong() {
      return this.getDataType().equals(BDataTypeEnum.unsignedLong);
   }

   public boolean isDataTypeFloat() {
      return DataTypeUtil.isFloat(this.getDataType());
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
         this.modbusNet().getModbusLog().trace(this.getParent().getName() + "- This ModbusServer float point is not writable, disregarding write request.");
      }

      super.doWrite(out);
   }

   private boolean updateOutput(BStatusValue out) {
      double fValue = ((BStatusNumeric)out).getValue();
      if (!this.isValidAddress(this.getDataAddress())) {
         this.writeFail("Illegal Modbus address");
         return false;
      } else {
         try {
            BModbusServerDevice device = (BModbusServerDevice)this.getDevice();
            int pointAddress = this.getDataAddress().getDataAddress();
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

            if (this.isHoldingRegisterType()) {
               device.setHoldingRegisterValues(pointAddress, dataOut);
            } else {
               device.setInputRegisterValues(pointAddress, dataOut);
            }
         } catch (Exception var7) {
            if (this.modbusNet() != null && this.modbusNet().getModbusLog().isTraceOn()) {
               this.modbusNet()
                  .getModbusLog()
                  .trace(this.getParent().getName() + ": BModbusServerNumericWritableProxyExt.updateOutput() caught exception: ", var7);
            }

            this.writeFail("Error when writing (" + var7 + ")");
            return false;
         }

         this.writeOk(out);
         return true;
      }
   }

   private boolean isDataType64BitLong() {
      return DataTypeUtil.is64BitLong(this.getDataType());
   }

   private boolean isDataTypeDouble() {
      return DataTypeUtil.isDouble(this.getDataType());
   }

   private boolean isDataTypeSigned() {
      return DataTypeUtil.isSigned(this.getDataType());
   }

   private byte[] setIntegerByteArray(double fValue) {
      return ByteConverterUtil.to2ByteIntArray(fValue, this.isDataTypeSigned());
   }

   private byte[] setLongByteArray(double fValue) {
      BDataByteOrderEnum byteOrder = ((BModbusServerDevice)this.getDevice()).getLongDataByteOrder();
      return ByteConverterUtil.to4ByteLongArray(fValue, byteOrder, this.isDataTypeSigned());
   }

   private byte[] setFloatByteArray(double fValue) {
      BDataByteOrderEnum byteOrder = ((BModbusServerDevice)this.getDevice()).getFloatDataByteOrder();
      return ByteConverterUtil.to4ByteFloatArray((float)fValue, byteOrder);
   }
}
