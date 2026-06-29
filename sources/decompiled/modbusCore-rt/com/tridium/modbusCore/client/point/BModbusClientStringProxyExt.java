package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigEntry;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.messages.ModbusReadRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.point.BIModbusStringProxyExt;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusString;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "numberRegisters",
   type = "int",
   defaultValue = "1",
   facets = {@Facet("BFacets.makeInt(null, 1, Integer.MAX_VALUE)")}
)
public class BModbusClientStringProxyExt extends BModbusClientProxyExt implements BIModbusStringProxyExt {
   public static final Property numberRegisters = newProperty(0, 1, BFacets.makeInt(null, 1, Integer.MAX_VALUE));
   public static final Type TYPE = Sys.loadType(BModbusClientStringProxyExt.class);

   @Override
   public int getNumberRegisters() {
      return this.getInt(numberRegisters);
   }

   @Override
   public void setNumberRegisters(int v) {
      this.setInt(numberRegisters, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BReadWriteMode getMode() {
      return BReadWriteMode.readonly;
   }

   @Override
   public void read() {
      if (!this.configFault) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         int address = device.getDeviceAddress();
         int pointAddress = this.getAbsoluteAddress().getDataAddress();
         int size = this.getNumberRegisters();
         int code = 3;
         ModbusReadRequest req = new ModbusReadRequest(device.modbusNet().getModbusMode(), device, address, code, pointAddress, size);
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
         this.setStringOutValues(rec);
      } else {
         this.readFail(rec.getExceptionString());
      }
   }

   private void setStringOutValues(ModbusResponse rec) {
      String stringValue;
      try {
         stringValue = rec.getString(this.getNumberRegisters());
      } catch (IllegalArgumentException var4) {
         this.readFail("error parsing String value (" + var4 + ")");
         return;
      }

      this.readOk(new BStatusString(stringValue));
   }

   @Override
   public BRegisterTypesEnum determineRegisterType() {
      return BRegisterTypesEnum.holdingRegister;
   }

   @Override
   public BEnum getRegisterType() {
      return BRegisterTypeEnum.holding;
   }

   @Override
   public int determineNumRegisters() {
      return this.getNumberRegisters();
   }

   @Override
   public boolean isValidAddress(BFlexAddress addr) {
      return addr.isModbusFormat() && addr.isModbusAnalogAddress() ? addr.isModbusHoldingAddress() : addr.isValid();
   }

   @Override
   public void devicePoll(BDevicePollConfigEntry entry) {
      if (!this.configFault && !this.isUnoperational()) {
         BModbusClientDevice device = (BModbusClientDevice)this.getDevice();
         int numRegisters = this.getNumberRegisters();
         ModbusResponse rsp = new ModbusResponse(device.modbusNet().getModbusMode(), device);
         int pointAddress = this.getAbsoluteAddress().getDataAddress();

         try {
            rsp.data = device.getHoldingRegisterValues(pointAddress, numRegisters, entry);
            rsp.exceptionCode = device.getHoldingRegistersReadStatus(pointAddress, numRegisters, entry).getErrorCode();
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

   @Override
   public void changed(Property prop, Context context) {
      if (!this.isRunning()) {
         super.changed(prop, context);
      } else {
         if (prop.equals(numberRegisters)) {
            this.setStale(true, null);
            if (this.getDevice() != null) {
               this.adjustPollSubscription();
            }
         } else {
            super.changed(prop, context);
         }
      }
   }
}
