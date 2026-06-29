package com.tridium.modbusCore.datatypes;

import com.tridium.modbusCore.enums.BAddressFormatEnum;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconModule;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "addressFormat",
      type = "BAddressFormatEnum",
      defaultValue = "BAddressFormatEnum.hex"
   ), @NiagaraProperty(
      name = "address",
      type = "String",
      defaultValue = "BFlexAddress.DEFAULT_ADDRESS_VALUE"
   )})
public class BFlexAddress extends BStruct {
   public static final Property addressFormat = newProperty(0, BAddressFormatEnum.hex, null);
   public static final Property address = newProperty(0, "0", null);
   public static final Type TYPE = Sys.loadType(BFlexAddress.class);
   private static final String DEFAULT_ADDRESS_VALUE = "0";
   private static final LexiconModule LEXICON_MODULE = LexiconModule.make("modbusCore");

   public BAddressFormatEnum getAddressFormat() {
      return (BAddressFormatEnum)this.get(addressFormat);
   }

   public void setAddressFormat(BAddressFormatEnum v) {
      this.set(addressFormat, v, null);
   }

   public String getAddress() {
      return this.getString(address);
   }

   public void setAddress(String v) {
      this.setString(address, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BFlexAddress() {
   }

   public BFlexAddress(BAddressFormatEnum addressFormat, String address) {
      this.setAddressFormat(addressFormat);
      this.setAddress(address);
   }

   public BFlexAddress(String addressString) {
      this.setAddressFormat((BAddressFormatEnum)BAddressFormatEnum.modbus.getRange().get(addressString.substring(0, addressString.indexOf(58))));
      this.setAddress(addressString.substring(addressString.indexOf(58) + 1, addressString.length()));
   }

   public BFlexAddress(BFlexAddress src) {
      this.copyFrom(src);
   }

   public boolean isHexFormat() {
      return this.getAddressFormat() == BAddressFormatEnum.hex;
   }

   public boolean isDecimalFormat() {
      return this.getAddressFormat() == BAddressFormatEnum.decimal;
   }

   public boolean isModbusFormat() {
      return this.getAddressFormat() == BAddressFormatEnum.modbus;
   }

   public boolean isValid() {
      int temp;
      if (this.isHexFormat()) {
         temp = Integer.valueOf(this.getAddress(), 16);
      } else {
         temp = Integer.valueOf(this.getAddress());
      }

      return !this.isModbusFormat() ? temp >= 0 : temp >= 0 && temp < 50000;
   }

   public boolean isModbusAnalogAddress() {
      if (!this.isModbusFormat()) {
         return false;
      } else {
         int temp = Integer.valueOf(this.getAddress());
         return temp >= 30000 && temp < 50000;
      }
   }

   public boolean isModbusDigitalAddress() {
      if (!this.isModbusFormat()) {
         return false;
      } else {
         int temp = Integer.valueOf(this.getAddress());
         return temp < 30000 && temp >= 0;
      }
   }

   public boolean isModbusHoldingAddress() {
      if (!this.isModbusFormat()) {
         return false;
      } else {
         int temp = Integer.valueOf(this.getAddress());
         return temp > 40000 && temp < 50000;
      }
   }

   public boolean isModbusInputAddress() {
      if (!this.isModbusFormat()) {
         return false;
      } else {
         int temp = Integer.valueOf(this.getAddress());
         return temp > 30000 && temp < 40000;
      }
   }

   public boolean isModbusCoilAddress() {
      if (!this.isModbusFormat()) {
         return false;
      } else {
         int temp = Integer.valueOf(this.getAddress());
         return temp < 10000 && temp >= 0;
      }
   }

   public boolean isModbusStatusAddress() {
      if (!this.isModbusFormat()) {
         return false;
      } else {
         int temp = Integer.valueOf(this.getAddress());
         return temp > 10000 && temp < 20000;
      }
   }

   public String getFlexAddress() {
      return this.getAddressFormat().getTag() + ":" + this.getAddress();
   }

   public int getDataAddress() {
      if (this.isHexFormat()) {
         return Integer.valueOf(this.getAddress(), 16);
      } else if (this.isDecimalFormat()) {
         return Integer.valueOf(this.getAddress());
      } else {
         int temp = Integer.valueOf(this.getAddress());
         if (temp > 40000) {
            return temp - 40001;
         } else if (temp > 30000) {
            return temp - 30001;
         } else if (temp > 20000) {
            return temp - 20001;
         } else {
            return temp > 10000 ? temp - 10001 : temp - 1;
         }
      }
   }

   public int getDataAddressNoModbusAltering() {
      return this.isHexFormat() ? Integer.valueOf(this.getAddress(), 16) : Integer.valueOf(this.getAddress());
   }

   public void setAddressFromInt(int address) {
      if (this.isHexFormat()) {
         this.setAddress(Integer.toString(address, 16));
      } else {
         this.setAddress(Integer.toString(address));
      }
   }

   public void setAddressFromString(String address) {
      int iAddr;
      if (this.isHexFormat()) {
         try {
            iAddr = Integer.valueOf(address, 16);
            address = "0000" + address;
            address = address.substring(address.length() - 4);
         } catch (NumberFormatException var4) {
            throw new RuntimeException("Must enter address in Hex.");
         }
      } else {
         try {
            iAddr = Integer.valueOf(address);
            address = "00000" + address;
            address = address.substring(address.length() - 5);
            if (this.isModbusFormat()
               && (iAddr == 0 || iAddr == 10000 || iAddr == 30000 || iAddr == 40000 || iAddr >= 50000 || iAddr >= 20000 && iAddr < 30000)) {
               throw new RuntimeException("Illegal Modbus address: Must be 0x, 1x, 3x or 4x format.");
            }
         } catch (NumberFormatException var5) {
            throw new RuntimeException("Must enter address in decimal.");
         }
      }

      if (iAddr > 65535) {
         if (this.isHexFormat()) {
            throw new RuntimeException("Address must be less than 10000 hex.");
         } else {
            throw new RuntimeException("Address must be less than 65536.");
         }
      } else {
         this.setAddress(address);
      }
   }

   public String toString(Context context) {
      return LEXICON_MODULE.getText("flexAddress.toString", context, new Object[]{this.getAddressFormat().getDisplayTag(context), this.getAddress()});
   }

   public String toDebugString() {
      return this.getAddressFormat().getTag() + ":" + this.getAddress();
   }
}
