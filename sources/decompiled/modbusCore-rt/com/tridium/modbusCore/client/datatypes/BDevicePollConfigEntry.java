package com.tridium.modbusCore.client.datatypes;

import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "startAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   ), @NiagaraProperty(
      name = "dataType",
      type = "BRegisterTypesEnum",
      defaultValue = "BRegisterTypesEnum.holdingRegister"
   ), @NiagaraProperty(
      name = "consecutivePointsToPoll",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, 9999)")}
   ), @NiagaraProperty(
      name = "readGroupSize",
      type = "int",
      defaultValue = "1",
      facets = {@Facet("BFacets.makeInt(null, 1, 2)")}
   ), @NiagaraProperty(
      name = "readStatus",
      type = "BCommStatus",
      defaultValue = "new BCommStatus(OK_NOT_ACTIVE)",
      flags = 3
   )})
public class BDevicePollConfigEntry extends BComponent implements ModbusMessageConst {
   public static final Property enabled = newProperty(0, true, null);
   public static final Property startAddress = newProperty(0, new BFlexAddress(), null);
   public static final Property dataType = newProperty(0, BRegisterTypesEnum.holdingRegister, null);
   public static final Property consecutivePointsToPoll = newProperty(0, 0, BFacets.makeInt(null, 0, 9999));
   public static final Property readGroupSize = newProperty(0, 1, BFacets.makeInt(null, 1, 2));
   public static final Property readStatus = newProperty(3, new BCommStatus(-2), null);
   public static final Type TYPE = Sys.loadType(BDevicePollConfigEntry.class);
   private byte[] byteArray = null;

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public BFlexAddress getStartAddress() {
      return (BFlexAddress)this.get(startAddress);
   }

   public void setStartAddress(BFlexAddress v) {
      this.set(startAddress, v, null);
   }

   public BRegisterTypesEnum getDataType() {
      return (BRegisterTypesEnum)this.get(dataType);
   }

   public void setDataType(BRegisterTypesEnum v) {
      this.set(dataType, v, null);
   }

   public int getConsecutivePointsToPoll() {
      return this.getInt(consecutivePointsToPoll);
   }

   public void setConsecutivePointsToPoll(int v) {
      this.setInt(consecutivePointsToPoll, v, null);
   }

   public int getReadGroupSize() {
      return this.getInt(readGroupSize);
   }

   public void setReadGroupSize(int v) {
      this.setInt(readGroupSize, v, null);
   }

   public BCommStatus getReadStatus() {
      return (BCommStatus)this.get(readStatus);
   }

   public void setReadStatus(BCommStatus v) {
      this.set(readStatus, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDevicePollConfigEntry() {
   }

   public BDevicePollConfigEntry(boolean enabled, BRegisterTypesEnum dataType, BFlexAddress startAddress, int consecutiveNum, int readSize) {
      this.setEnabled(enabled);
      this.setDataType(dataType);
      this.setStartAddress(startAddress);
      this.setConsecutivePointsToPoll(consecutiveNum);
      this.setReadGroupSize(readSize);
   }

   public BDevicePollConfigEntry(BDevicePollConfigEntry src) {
      this.copyFrom(src);
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BDevicePollConfigTable;
   }

   public String toString(Context context) {
      return this.getDataType().getTag()
         + ": "
         + this.getStartAddress().getDataAddressNoModbusAltering()
         + " - "
         + (this.getStartAddress().getDataAddressNoModbusAltering() + this.getConsecutivePointsToPoll() - 1)
         + ": "
         + (this.getEnabled() ? this.getLexicon().getText("devicePoll.strings.enabled") : this.getLexicon().getText("devicePoll.strings.disabled"));
   }

   public String toDebugString() {
      return this.getDataType().getTag()
         + ": "
         + this.getStartAddress().getDataAddressNoModbusAltering()
         + " - "
         + (this.getStartAddress().getDataAddressNoModbusAltering() + this.getConsecutivePointsToPoll() - 1)
         + ": "
         + (this.getEnabled() ? "enabled" : "disabled");
   }

   public BModbusClientDevice getDevice() {
      for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
         if (parent instanceof BModbusClientDevice) {
            return (BModbusClientDevice)parent;
         }
      }

      return null;
   }

   public void validateAddress() {
      BFlexAddress regAddress = this.getStartAddress();
      if (regAddress.isModbusFormat()) {
         if (regAddress.isModbusHoldingAddress()) {
            this.set(dataType, BRegisterTypesEnum.holdingRegister, null);
         } else if (regAddress.isModbusInputAddress()) {
            this.set(dataType, BRegisterTypesEnum.inputRegister, null);
         } else if (regAddress.isModbusCoilAddress()) {
            this.set(dataType, BRegisterTypesEnum.discreteCoil, null);
         } else if (regAddress.isModbusStatusAddress()) {
            this.set(dataType, BRegisterTypesEnum.discreteInput, null);
         }
      }
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (!property.equals(dataType) && !property.equals(startAddress)) {
            if ((property.equals(enabled) || property.equals(consecutivePointsToPoll))
               && this.getParent() != null
               && this.getPropertyInParent() != null
               && this.getParent() instanceof BComponent) {
               ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
            }
         } else if (context != null) {
            this.validateAddress();
            if (this.getParent() != null && this.getPropertyInParent() != null && this.getParent() instanceof BComponent) {
               ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
            }
         }
      }
   }

   public void setByteArray(byte[] data) {
      this.byteArray = data;
   }

   public byte[] getByteArray() {
      return this.byteArray;
   }
}
