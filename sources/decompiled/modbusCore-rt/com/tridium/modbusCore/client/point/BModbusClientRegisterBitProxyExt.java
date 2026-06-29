package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigEntry;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.messages.ModbusReadRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import com.tridium.modbusCore.point.BIModbusRegisterBitProxyExt;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
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
      name = "bitNumber",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, 15)")}
   )})
public class BModbusClientRegisterBitProxyExt extends BModbusClientProxyExt implements BIModbusRegisterBitProxyExt {
   public static final Property regType = newProperty(0, BRegisterTypeEnum.holding, null);
   public static final Property bitNumber = newProperty(0, 0, BFacets.makeInt(null, 0, 15));
   public static final Type TYPE = Sys.loadType(BModbusClientRegisterBitProxyExt.class);

   @Override
   public BRegisterTypeEnum getRegType() {
      return (BRegisterTypeEnum)this.get(regType);
   }

   @Override
   public void setRegType(BRegisterTypeEnum v) {
      this.set(regType, v, null);
   }

   @Override
   public int getBitNumber() {
      return this.getInt(bitNumber);
   }

   @Override
   public void setBitNumber(int v) {
      this.setInt(bitNumber, v, null);
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
         int count = 1;
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
         this.setBinaryOutValues(rec);
      } else {
         this.readFail(rec.getExceptionString());
      }
   }

   private void setBinaryOutValues(ModbusResponse rec) {
      int regValue = rec.data[1] & 255 | (rec.data[0] & 255) << 8;
      int mask = 1;
      mask <<= this.getBitNumber();
      boolean binaryValue = (regValue & mask) != 0;
      this.readOk(new BStatusBoolean(binaryValue));
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
      return 1;
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(bitNumber)) {
            this.setStale(true, null);
            if (this.getParentPoint().isWritablePoint()) {
               if (((BModbusClientDevice)this.getDevice()).writablePointAlreadyExists(this)) {
                  this.modbusNet()
                     .getModbusLog()
                     .error(this.getParent().getName() + " Duplicate Writable point for register address: " + this.getDataAddress());
                  this.setDataAddress(new BFlexAddress(BAddressFormatEnum.hex, "-1"));
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

   @Override
   public void devicePoll(BDevicePollConfigEntry entry) {
      if (!this.configFault && !this.isUnoperational()) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         int numRegisters = 1;
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
      } else if (this.modbusNet().getModbusLog().isTraceOn()) {
         this.modbusNet().getModbusLog().trace(this + "- This ModbusClient register bit point is not writable, disregarding write request.");
      }

      super.doWrite(out);
   }

   public boolean updateOutput(BStatusValue out) {
      boolean bValue = ((BStatusBoolean)out).getValue();
      BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
      BModbusClientNetwork network = (BModbusClientNetwork)this.modbusNet();
      int address = device.getDeviceAddress();
      int pointAddress = this.getAbsoluteAddress().getDataAddress();
      int count = 1;
      int code;
      if (this.isHoldingRegisterType()) {
         code = 3;
      } else {
         code = 4;
      }

      ModbusReadRequest req = new ModbusReadRequest(network.getModbusMode(), device, address, code, pointAddress, count);
      ModbusResponse rsp = (ModbusResponse)device.sendModbusMessage(req);
      if (rsp == null) {
         rsp = new ModbusResponse(network.getModbusMode(), device);
         rsp.exceptionCode = 9;
      }

      if (!rsp.isError()) {
         int regValue = rsp.data[1] & 255 | (rsp.data[0] & 255) << 8;
         byte[] dataOut = new byte[2];
         int mask = 1;
         mask <<= this.getBitNumber();
         if (bValue) {
            regValue |= mask;
         } else {
            regValue &= ~mask;
         }

         dataOut[0] = (byte)(regValue >> 8 & 0xFF);
         dataOut[1] = (byte)(regValue & 0xFF);
         int var16 = 6;
         if (device.isPresetMultiple()) {
            var16 = 16;
         }

         ModbusWriteRequest req2 = new ModbusWriteRequest(network.getModbusMode(), device, address, var16, pointAddress, count, dataOut);
         ModbusResponse resp = (ModbusResponse)device.sendModbusMessage(req2);
         if (resp == null) {
            resp = new ModbusResponse(network.getModbusMode(), device);
            resp.exceptionCode = 9;
         }

         if (resp.isError() && resp.exceptionCode != 5) {
            this.writeFail(resp.getExceptionString());
            return false;
         } else {
            this.writeOk(out);
            return true;
         }
      } else {
         this.writeFail(rsp.getExceptionString());
         return false;
      }
   }
}
