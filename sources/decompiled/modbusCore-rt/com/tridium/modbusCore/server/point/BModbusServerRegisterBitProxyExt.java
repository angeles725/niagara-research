package com.tridium.modbusCore.server.point;

import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.point.BIModbusRegisterBitProxyExt;
import com.tridium.modbusCore.server.BModbusServerDevice;
import com.tridium.modbusCore.server.BModbusServerNetwork;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
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
public class BModbusServerRegisterBitProxyExt extends BModbusServerProxyExt implements BIModbusRegisterBitProxyExt {
   public static final Property regType = newProperty(0, BRegisterTypeEnum.holding, null);
   public static final Property bitNumber = newProperty(0, 0, BFacets.makeInt(null, 0, 15));
   public static final Type TYPE = Sys.loadType(BModbusServerRegisterBitProxyExt.class);

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
   public void read() {
      if (!this.configFault) {
         BModbusServerDevice device = (BModbusServerDevice)this.getDevice();
         int numRegisters = 1;
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
               this.modbusNet().getModbusLog().trace(this.getParent().getName() + ": BModbusServerRegisterBitProxyExt.read() caught exception: ", var6);
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
      BStatusBoolean val = new BStatusBoolean(binaryValue);
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
         if (prop.equals(bitNumber)) {
            this.setStale(true, null);
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
         int numRegs = 1;
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
         int numRegs = 1;
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
         this.modbusNet()
            .getModbusLog()
            .trace(this.getParent().getName() + "- This ModbusServer register bit point is not writable, disregarding write request.");
      }

      super.doWrite(out);
   }

   private boolean updateOutput(BStatusValue out) {
      boolean bValue = ((BStatusBoolean)out).getValue();
      if (!this.isValidAddress(this.getDataAddress())) {
         this.writeFail("Illegal Modbus address");
         return false;
      } else {
         try {
            BModbusServerDevice device = (BModbusServerDevice)this.getDevice();
            BModbusServerNetwork network = (BModbusServerNetwork)this.modbusNet();
            int count = 1;
            int address = device.getDeviceAddress();
            int pointAddress = this.getDataAddress().getDataAddress();
            ModbusResponse rsp = new ModbusResponse(network.getModbusMode(), device);

            try {
               if (this.isHoldingRegisterType()) {
                  rsp.data = device.getHoldingRegisterValues(pointAddress, count);
               } else {
                  rsp.data = device.getInputRegisterValues(pointAddress, count);
               }
            } catch (Exception var12) {
               if (this.modbusNet() != null && this.modbusNet().getModbusLog().isTraceOn()) {
                  this.modbusNet().getModbusLog().trace(this.getParent().getName() + ": BModbusServerRegisterBitProxyExt.read() caught exception: ", var12);
               }

               this.writeFail("Error when writing (" + var12 + ")");
               return false;
            }

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
            if (this.isHoldingRegisterType()) {
               device.setHoldingRegisterValues(pointAddress, dataOut);
            } else {
               device.setInputRegisterValues(pointAddress, dataOut);
            }
         } catch (Exception var13) {
            this.writeFail("Error when writing (" + var13 + ")");
            return false;
         }

         this.writeOk(out);
         return true;
      }
   }
}
