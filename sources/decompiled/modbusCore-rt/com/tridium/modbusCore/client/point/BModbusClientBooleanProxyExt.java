package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigEntry;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.enums.BStatusTypeEnum;
import com.tridium.modbusCore.messages.ModbusReadRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import com.tridium.modbusCore.point.BIModbusBooleanProxyExt;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BEnum;
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
public class BModbusClientBooleanProxyExt extends BModbusClientProxyExt implements BIModbusBooleanProxyExt {
   public static final Property statusType = newProperty(0, BStatusTypeEnum.coil, null);
   public static final Type TYPE = Sys.loadType(BModbusClientBooleanProxyExt.class);

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
   public void started() throws Exception {
      super.started();
      if (this.getParentPoint().isWritablePoint()) {
         this.setFlags(statusType, 1);
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
         if (this.isCoilStatusType()) {
            code = 1;
         } else {
            code = 2;
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
      boolean binaryValue;
      try {
         binaryValue = rec.getBinary(0);
      } catch (IllegalArgumentException var4) {
         this.readFail(this.getName() + ": error parsing boolean value (" + var4 + ")");
         return;
      }

      this.readOk(new BStatusBoolean(binaryValue));
   }

   @Override
   public BRegisterTypesEnum determineRegisterType() {
      return this.getStatusType().equals(BStatusTypeEnum.coil) ? BRegisterTypesEnum.discreteCoil : BRegisterTypesEnum.discreteInput;
   }

   @Override
   public BEnum getRegisterType() {
      return this.getStatusType();
   }

   @Override
   public int determineNumRegisters() {
      return 1;
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(statusType)) {
            if (context != noAddressCheck) {
               if (this.getParentPoint().isWritablePoint() && !this.isCoilStatusType()) {
                  this.set(statusType, BStatusTypeEnum.coil, noAddressCheck);
               } else if (this.getDataAddress().isModbusFormat()) {
                  this.setStale(true, null);
                  if (this.getDataAddress().isModbusCoilAddress()) {
                     if (!this.isCoilStatusType()) {
                        this.set(statusType, BStatusTypeEnum.coil, noAddressCheck);
                     }
                  } else if (this.isCoilStatusType()) {
                     this.set(statusType, BStatusTypeEnum.input, noAddressCheck);
                  }
               }
            }

            if (this.getDevice() != null) {
               this.adjustPollSubscription();
            }
         }
      }
   }

   @Override
   public boolean isValidAddress(BFlexAddress addr) {
      if (!addr.isModbusFormat()) {
         return addr.isValid();
      } else if (addr.isModbusDigitalAddress()) {
         if (this.getParentPoint().isWritablePoint()) {
            return addr.isModbusCoilAddress();
         } else {
            if (addr.isModbusCoilAddress()) {
               this.set(statusType, BStatusTypeEnum.coil, noAddressCheck);
            } else {
               this.set(statusType, BStatusTypeEnum.input, noAddressCheck);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean isCoilStatusType() {
      return this.getStatusType().equals(BStatusTypeEnum.coil);
   }

   @Override
   public void devicePoll(BDevicePollConfigEntry entry) {
      if (!this.configFault && !this.isUnoperational()) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         int numRegisters = 1;
         ModbusResponse rsp = new ModbusResponse(device.modbusNet().getModbusMode(), device);
         int pointAddress = this.getAbsoluteAddress().getDataAddress();

         try {
            if (this.isCoilStatusType()) {
               rsp.data = device.getCoilStatusValues(pointAddress, numRegisters, entry);
               rsp.exceptionCode = device.getBinaryCoilsReadStatus(pointAddress, numRegisters, entry).getErrorCode();
            } else {
               rsp.data = device.getInputStatusValues(pointAddress, numRegisters, entry);
               rsp.exceptionCode = device.getBinaryInputsReadStatus(pointAddress, numRegisters, entry).getErrorCode();
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
         this.modbusNet().getModbusLog().trace(this.getParent().getName() + "- This ModbusClient boolean point is not writable, disregarding write request.");
      }

      super.doWrite(out);
   }

   private boolean updateOutput(BStatusValue out) {
      boolean bValue = ((BStatusBoolean)out).getValue();
      BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
      BModbusClientNetwork network = (BModbusClientNetwork)this.modbusNet();
      int address = device.getDeviceAddress();
      int pointAddress = this.getAbsoluteAddress().getDataAddress();
      byte[] dataOut;
      int code;
      if (device.isForceMultiple()) {
         dataOut = new byte[]{0};
         code = 15;
         if (bValue) {
            dataOut[0] = 1;
         }
      } else {
         dataOut = new byte[2];
         dataOut[0] = 0;
         dataOut[1] = 0;
         code = 5;
         if (bValue) {
            dataOut[0] = -1;
         }
      }

      int count = 1;
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
         this.writeOk(out);
         return true;
      }
   }
}
