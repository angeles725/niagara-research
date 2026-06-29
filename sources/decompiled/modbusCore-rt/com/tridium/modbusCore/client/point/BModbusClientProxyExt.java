package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.ModbusErrorCodes;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigEntry;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigTable;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import com.tridium.modbusCore.enums.BDataSourceEnum;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.point.BIModbusReadProxyExt;
import com.tridium.modbusCore.point.BModbusProxyExt;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "absoluteAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()",
      flags = 3
   ), @NiagaraProperty(
      name = "dataSource",
      type = "BDataSourceEnum",
      defaultValue = "BDataSourceEnum.pointPoll",
      flags = 3
   )})
public abstract class BModbusClientProxyExt extends BModbusProxyExt implements ModbusErrorCodes, BIModbusReadProxyExt {
   public static final Property absoluteAddress = newProperty(3, new BFlexAddress(), null);
   public static final Property dataSource = newProperty(3, BDataSourceEnum.pointPoll, null);
   public static final Type TYPE = Sys.loadType(BModbusClientProxyExt.class);
   protected Object pollSync = new Object();
   protected boolean subscribed = false;
   protected boolean configFault = false;
   private Object lastPollGroupCode = null;

   @Override
   public BFlexAddress getAbsoluteAddress() {
      return (BFlexAddress)this.get(absoluteAddress);
   }

   @Override
   public void setAbsoluteAddress(BFlexAddress v) {
      this.set(absoluteAddress, v, null);
   }

   @Override
   public BDataSourceEnum getDataSource() {
      return (BDataSourceEnum)this.get(dataSource);
   }

   @Override
   public void setDataSource(BDataSourceEnum v) {
      this.set(dataSource, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public Object getPollGroupCode() {
      synchronized (this.pollSync) {
         if (this.lastPollGroupCode != null) {
            return this.lastPollGroupCode;
         } else {
            BDevicePollConfigEntry entry = null;
            BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
            if (device != null) {
               BRegisterTypesEnum dataType = this.determineRegisterType();
               BDevicePollConfigEntry[] entries = null;
               if (dataType.equals(BRegisterTypesEnum.holdingRegister)) {
                  entries = device.getDevicePollConfig().getActiveHoldingRegisterPollEntries();
               } else if (dataType.equals(BRegisterTypesEnum.inputRegister)) {
                  entries = device.getDevicePollConfig().getActiveInputRegisterPollEntries();
               } else if (dataType.equals(BRegisterTypesEnum.discreteCoil)) {
                  entries = device.getDevicePollConfig().getActiveBinaryCoilPollEntries();
               } else if (dataType.equals(BRegisterTypesEnum.discreteInput)) {
                  entries = device.getDevicePollConfig().getActiveBinaryInputPollEntries();
               }

               int numRegisters = this.determineNumRegisters();
               this.setCurrentAbsoluteAddress();
               int pointAddress = this.getAbsoluteAddress().getDataAddress();
               entry = BDevicePollConfigTable.findPollConfigEntry(entries, pointAddress, numRegisters);
            }

            if (entry != null) {
               this.setDataSource(BDataSourceEnum.devicePoll);
               this.lastPollGroupCode = entry;
            } else {
               this.setDataSource(BDataSourceEnum.pointPoll);
               this.lastPollGroupCode = this;
            }

            return this.lastPollGroupCode;
         }
      }
   }

   public Type getPollGroupType() {
      return BModbusClientPollGroup.TYPE;
   }

   public Type getDeviceExtType() {
      return BModbusClientPointDeviceExt.TYPE;
   }

   public boolean write(Context cx) throws Exception {
      return this.configFault ? false : super.write(cx);
   }

   public void readSubscribed(Context cx) throws Exception {
      this.setCurrentAbsoluteAddress();
      super.readSubscribed(cx);
      synchronized (this.pollSync) {
         this.subscribed = true;
      }
   }

   public void readUnsubscribed(Context cx) throws Exception {
      synchronized (this.pollSync) {
         if (!this.subscribed) {
            return;
         }

         this.subscribed = false;
      }

      super.readUnsubscribed(cx);
   }

   public void adjustPollSubscription() {
      synchronized (this.pollSync) {
         if (this.subscribed) {
            try {
               this.readUnsubscribed(null);
               this.lastPollGroupCode = null;
               this.readSubscribed(null);
            } catch (Exception var4) {
               this.modbusNet().getModbusLog().error(this.getParent().getName() + ">>> adjustPollSubscription error", var4);
            }
         } else {
            this.lastPollGroupCode = null;
         }
      }
   }

   public void started() throws Exception {
      this.setCurrentAbsoluteAddress();
      if (this.getParentPoint().isWritablePoint()) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         if (device != null) {
            if (device.writablePointAlreadyExists(this)) {
               this.modbusNet().getModbusLog().error(this.getParent().getName() + " Duplicate Writable point for register address: " + this.getDataAddress());
               this.setDataAddress(new BFlexAddress(BAddressFormatEnum.hex, "-1"));
            }

            device.addWritableProxy(this);
         }
      }

      super.started();
   }

   public void stopped() throws Exception {
      if (this.getParentPoint().isWritablePoint()) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         if (device != null) {
            device.removeWritableProxy(this);
         }
      }

      super.stopped();
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(dataAddress)) {
            this.setStale(true, null);
            if (!this.isValidAddress(this.getDataAddress()) && Sys.atSteadyState() && this.modbusNet() != null && context != Context.commit) {
               this.modbusNet()
                  .getModbusLog()
                  .error("Illegal Modbus address for point " + this.getParent().getName() + ": Modbus Address does not match Object type.");
            }

            if (this.getDevice() != null) {
               this.adjustPollSubscription();
               this.setCurrentAbsoluteAddress();
               if (!this.configFault) {
                  this.readReset();
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
            }
         }
      }
   }

   public void setCurrentAbsoluteAddress() {
      if (!this.isValidAddress(this.getDataAddress())) {
         this.configFault = true;
         String errMsg = "Illegal Modbus address " + this.getDataAddress() + " for this point's configuration.";
         this.readFail(errMsg);
      } else {
         this.configFault = false;
      }

      int baseAddr = 0;

      try {
         baseAddr = ((BModbusClientDevice)this.getDevice()).getRegisterBaseAddress(this.getRegisterType());
      } catch (NullPointerException var6) {
      } catch (NumberFormatException var7) {
         baseAddr = 0;
      }

      try {
         BFlexAddress absAddr = (BFlexAddress)this.getDataAddress().newCopy();
         int rawAddress;
         if (absAddr.isModbusFormat()) {
            rawAddress = Integer.valueOf(absAddr.getAddress());
         } else {
            rawAddress = absAddr.getDataAddress();
         }

         absAddr.setAddressFromInt(rawAddress + baseAddr);
         this.setAbsoluteAddress(absAddr);
      } catch (NullPointerException var4) {
      } catch (NumberFormatException var5) {
         BFlexAddress absAddr = (BFlexAddress)this.getDataAddress().newCopy();
         absAddr.setAddressFromInt(baseAddr);
         this.setAbsoluteAddress(absAddr);
      }
   }

   @Override
   public void poll() {
      if (!this.configFault && !this.isUnoperational()) {
         if (this.modbusNet().getModbusLog().isTraceOn()) {
            this.modbusNet().getModbusLog().trace(this.getParent().getName() + ">>> BModbusClientProxyExt.poll");
         }

         this.read();
      }
   }

   public boolean isValidAddress(BFlexAddress addr) {
      return addr.isValid();
   }

   public abstract void devicePoll(BDevicePollConfigEntry var1);

   public abstract BRegisterTypesEnum determineRegisterType();

   public abstract BEnum getRegisterType();

   public abstract int determineNumRegisters();
}
