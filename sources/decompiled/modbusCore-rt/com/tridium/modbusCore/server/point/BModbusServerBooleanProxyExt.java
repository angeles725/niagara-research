package com.tridium.modbusCore.server.point;

import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.enums.BStatusTypeEnum;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.point.BIModbusBooleanProxyExt;
import com.tridium.modbusCore.server.BModbusServerDevice;
import com.tridium.modbusCore.server.BModbusServerNetwork;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "statusType",
   type = "BStatusTypeEnum",
   defaultValue = "BStatusTypeEnum.coil"
)
public class BModbusServerBooleanProxyExt extends BModbusServerProxyExt implements BIModbusBooleanProxyExt {
   public static final Property statusType = newProperty(0, BStatusTypeEnum.coil, null);
   public static final Type TYPE = Sys.loadType(BModbusServerBooleanProxyExt.class);

   @Override
   public BStatusTypeEnum getStatusType() {
      return (BStatusTypeEnum)this.get(statusType);
   }

   @Override
   public void setStatusType(BStatusTypeEnum v) {
      this.set(statusType, v, null);
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
            if (this.isCoilStatusType()) {
               rsp.data = device.getCoilStatusValues(pointAddress, numRegisters);
            } else {
               rsp.data = device.getInputStatusValues(pointAddress, numRegisters);
            }

            rsp.exceptionCode = 0;
            rsp.byteCount = (byte)rsp.data.length;
            rsp.numberPoints = numRegisters;
         } catch (Exception var6) {
            if (this.modbusNet() != null && this.modbusNet().getModbusLog().isTraceOn()) {
               this.modbusNet().getModbusLog().trace(this.getParent().getName() + ": BModbusServerBooleanProxyExt.read() caught exception: ", var6);
            }

            this.readFail("Exception during read (" + var6 + ")");
            return;
         }

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
      boolean binaryValue;
      try {
         binaryValue = rec.getBinary(0);
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing boolean value (" + var4 + ")");
         return;
      }

      BStatusBoolean val = new BStatusBoolean(binaryValue);
      if (this.getStatus().isValid() && this.lastReadValue != null && this.lastReadValue.equivalent(val)) {
         this.getTuning().readOk();
      } else {
         this.readOk(val);
      }
   }

   @Override
   public BRegisterTypesEnum determineRegisterType() {
      return this.getStatusType().equals(BStatusTypeEnum.coil) ? BRegisterTypesEnum.discreteCoil : BRegisterTypesEnum.discreteInput;
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(statusType) && context != noAddressCheck) {
            this.setStale(true, null);
            if (this.getDataAddress().isModbusFormat()) {
               if (this.getDataAddress().isModbusCoilAddress()) {
                  if (!this.isCoilStatusType()) {
                     this.set(statusType, BStatusTypeEnum.coil, noAddressCheck);
                  }
               } else if (this.isCoilStatusType()) {
                  this.set(statusType, BStatusTypeEnum.input, noAddressCheck);
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
         return this.isCoilStatusType()
            ? device.isCoilAddressValid(addr.getDataAddress(), numRegs)
            : device.isStatusAddressValid(addr.getDataAddress(), numRegs);
      } else if (addr.isModbusDigitalAddress()) {
         BStatusTypeEnum sType = BStatusTypeEnum.coil;
         if (addr.isModbusCoilAddress()) {
            this.set(statusType, BStatusTypeEnum.coil, noAddressCheck);
         } else {
            this.set(statusType, BStatusTypeEnum.input, noAddressCheck);
            sType = BStatusTypeEnum.input;
         }

         BModbusServerDevice device = (BModbusServerDevice)this.getDevice();
         int numRegs = 1;
         return sType.equals(BStatusTypeEnum.coil)
            ? device.isCoilAddressValid(addr.getDataAddress(), numRegs)
            : device.isStatusAddressValid(addr.getDataAddress(), numRegs);
      } else {
         return false;
      }
   }

   private boolean isCoilStatusType() {
      return this.getStatusType().equals(BStatusTypeEnum.coil);
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
         this.modbusNet().getModbusLog().trace(this.getParent().getName() + "- This ModbusServer boolean point is not writable, disregarding write request.");
      }

      super.doWrite(out);
   }

   private boolean updateOutput(BStatusValue out) {
      boolean bVal = ((BStatusBoolean)out).getValue();
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
            if (this.isCoilStatusType()) {
               device.setCoilStatusValue(pointAddress, bVal);
            } else {
               device.setInputStatusValue(pointAddress, bVal);
            }
         } catch (Exception var9) {
            if (this.modbusNet() != null && this.modbusNet().getModbusLog().isTraceOn()) {
               this.modbusNet()
                  .getModbusLog()
                  .error(this.getParent().getName() + ": BModbusServerBooleanWritableProxyExt.updateOutput() caught exception: ", var9);
            }

            this.writeFail("Error when writing (" + var9 + ")");
            return false;
         }

         this.writeOk(out);
         return true;
      }
   }
}
