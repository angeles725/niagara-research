package com.tridium.modbusCore.client;

import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.ModbusErrorCodes;
import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.client.datatypes.BCommStatus;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigEntry;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigTable;
import com.tridium.modbusCore.client.datatypes.BModbusClientConfig;
import com.tridium.modbusCore.client.point.BModbusClientBooleanProxyExt;
import com.tridium.modbusCore.client.point.BModbusClientNumericBitsProxyExt;
import com.tridium.modbusCore.client.point.BModbusClientNumericProxyExt;
import com.tridium.modbusCore.client.point.BModbusClientPointDeviceExt;
import com.tridium.modbusCore.client.point.BModbusClientProxyExt;
import com.tridium.modbusCore.client.point.BModbusClientRegisterBitProxyExt;
import com.tridium.modbusCore.client.point.BModbusClientStringProxyExt;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BAddressFormatEnum;
import com.tridium.modbusCore.enums.BDataTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.enums.BStatusTypeEnum;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import com.tridium.modbusCore.messages.ModbusReadRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.util.DataTypeUtil;
import java.util.Arrays;
import java.util.Vector;
import javax.baja.control.BControlPoint;
import javax.baja.driver.point.BPointDeviceExt;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "modbusConfig",
      type = "BModbusConfig",
      defaultValue = "new BModbusClientConfig()",
      override = true
   ), @NiagaraProperty(
      name = "pingAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   ), @NiagaraProperty(
      name = "pingAddressDataType",
      type = "BDataTypeEnum",
      defaultValue = "BDataTypeEnum.integerType"
   ), @NiagaraProperty(
      name = "pingAddressRegType",
      type = "BRegisterTypeEnum",
      defaultValue = "BRegisterTypeEnum.holding"
   ), @NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal"
   ), @NiagaraProperty(
      name = "inputRegisterBaseAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   ), @NiagaraProperty(
      name = "holdingRegisterBaseAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   ), @NiagaraProperty(
      name = "coilStatusBaseAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   ), @NiagaraProperty(
      name = "inputStatusBaseAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   ), @NiagaraProperty(
      name = "devicePollConfig",
      type = "BDevicePollConfigTable",
      defaultValue = "new BDevicePollConfigTable()"
   ), @NiagaraProperty(
      name = "points",
      type = "BModbusClientPointDeviceExt",
      defaultValue = "new BModbusClientPointDeviceExt()"
   )})
public abstract class BModbusClientDevice extends BModbusDevice implements ModbusMessageConst, ModbusErrorCodes {
   public static final Property modbusConfig = newProperty(0, new BModbusClientConfig(), null);
   public static final Property pingAddress = newProperty(0, new BFlexAddress(), null);
   public static final Property pingAddressDataType = newProperty(0, BDataTypeEnum.integerType, null);
   public static final Property pingAddressRegType = newProperty(0, BRegisterTypeEnum.holding, null);
   public static final Property pollFrequency = newProperty(0, BPollFrequency.normal, null);
   public static final Property inputRegisterBaseAddress = newProperty(0, new BFlexAddress(), null);
   public static final Property holdingRegisterBaseAddress = newProperty(0, new BFlexAddress(), null);
   public static final Property coilStatusBaseAddress = newProperty(0, new BFlexAddress(), null);
   public static final Property inputStatusBaseAddress = newProperty(0, new BFlexAddress(), null);
   public static final Property devicePollConfig = newProperty(0, new BDevicePollConfigTable(), null);
   public static final Property points = newProperty(0, new BModbusClientPointDeviceExt(), null);
   public static final Type TYPE = Sys.loadType(BModbusClientDevice.class);
   private Vector<BModbusClientProxyExt> writableProxies = new Vector<>();
   private int pingsFailed = 0;
   private boolean rdCommError = false;
   private boolean pollRegistered = false;

   public BFlexAddress getPingAddress() {
      return (BFlexAddress)this.get(pingAddress);
   }

   public void setPingAddress(BFlexAddress v) {
      this.set(pingAddress, v, null);
   }

   public BDataTypeEnum getPingAddressDataType() {
      return (BDataTypeEnum)this.get(pingAddressDataType);
   }

   public void setPingAddressDataType(BDataTypeEnum v) {
      this.set(pingAddressDataType, v, null);
   }

   public BRegisterTypeEnum getPingAddressRegType() {
      return (BRegisterTypeEnum)this.get(pingAddressRegType);
   }

   public void setPingAddressRegType(BRegisterTypeEnum v) {
      this.set(pingAddressRegType, v, null);
   }

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public BFlexAddress getInputRegisterBaseAddress() {
      return (BFlexAddress)this.get(inputRegisterBaseAddress);
   }

   public void setInputRegisterBaseAddress(BFlexAddress v) {
      this.set(inputRegisterBaseAddress, v, null);
   }

   public BFlexAddress getHoldingRegisterBaseAddress() {
      return (BFlexAddress)this.get(holdingRegisterBaseAddress);
   }

   public void setHoldingRegisterBaseAddress(BFlexAddress v) {
      this.set(holdingRegisterBaseAddress, v, null);
   }

   public BFlexAddress getCoilStatusBaseAddress() {
      return (BFlexAddress)this.get(coilStatusBaseAddress);
   }

   public void setCoilStatusBaseAddress(BFlexAddress v) {
      this.set(coilStatusBaseAddress, v, null);
   }

   public BFlexAddress getInputStatusBaseAddress() {
      return (BFlexAddress)this.get(inputStatusBaseAddress);
   }

   public void setInputStatusBaseAddress(BFlexAddress v) {
      this.set(inputStatusBaseAddress, v, null);
   }

   public BDevicePollConfigTable getDevicePollConfig() {
      return (BDevicePollConfigTable)this.get(devicePollConfig);
   }

   public void setDevicePollConfig(BDevicePollConfigTable v) {
      this.set(devicePollConfig, v, null);
   }

   public BModbusClientPointDeviceExt getPoints() {
      return (BModbusClientPointDeviceExt)this.get(points);
   }

   public void setPoints(BModbusClientPointDeviceExt v) {
      this.set(points, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(inputRegisterBaseAddress)) {
            if (this.getInputRegisterBaseAddress().isModbusFormat() && !this.getInputRegisterBaseAddress().isModbusInputAddress()) {
               this.setInputRegisterBaseAddress((BFlexAddress)inputRegisterBaseAddress.getDefaultValue());
               if (this.modbusNet() != null) {
                  this.modbusNet().getModbusLog().error("Illegal input register base address on " + this.getName() + ".  Must be an Input register address 3x");
               }
            }

            this.updateProxyPointSubscriptions();
         } else if (property.equals(devicePollConfig)) {
            this.updateProxyPointSubscriptions();
         } else if (property.equals(holdingRegisterBaseAddress)) {
            if (this.getHoldingRegisterBaseAddress().isModbusFormat() && !this.getHoldingRegisterBaseAddress().isModbusHoldingAddress()) {
               this.setHoldingRegisterBaseAddress((BFlexAddress)holdingRegisterBaseAddress.getDefaultValue());
               if (this.modbusNet() != null) {
                  this.modbusNet()
                     .getModbusLog()
                     .error("Illegal holding register base address on " + this.getName() + ".  Must be a Holding register address 4x");
               }
            }

            this.updateProxyPointSubscriptions();
         } else if (property.equals(coilStatusBaseAddress)) {
            if (this.getCoilStatusBaseAddress().isModbusFormat() && !this.getCoilStatusBaseAddress().isModbusCoilAddress()) {
               this.setCoilStatusBaseAddress((BFlexAddress)coilStatusBaseAddress.getDefaultValue());
               if (this.modbusNet() != null) {
                  this.modbusNet().getModbusLog().error("Illegal coil status base address on " + this.getName() + ".  Must be a Coil address 0x");
               }
            }

            this.updateProxyPointSubscriptions();
         } else if (property.equals(inputStatusBaseAddress)) {
            if (this.getInputStatusBaseAddress().isModbusFormat() && !this.getInputStatusBaseAddress().isModbusStatusAddress()) {
               this.setInputStatusBaseAddress((BFlexAddress)inputStatusBaseAddress.getDefaultValue());
               if (this.modbusNet() != null) {
                  this.modbusNet().getModbusLog().error("Illegal input status base address on " + this.getName() + ".  Must be a Status address 1x");
               }
            }

            this.updateProxyPointSubscriptions();
         }
      }
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName()).append("[" + this.getDeviceAddress() + "]");
      return sb.toString();
   }

   public abstract Message sendModbusMessage(Message var1);

   private void setCommStatusOutput(BDevicePollConfigEntry entry, int exceptionCode) {
      if (!this.isDisabled()) {
         this.rdCommError = exceptionCode != 0 && exceptionCode != -2;
      }

      if (entry != null) {
         entry.setReadStatus(new BCommStatus(exceptionCode));
      }
   }

   public void doPing() {
      if (!this.isDisabled() && !this.isFault()) {
         if (this.modbusNet() == null) {
            this.pingFail("No modbus network found");
         } else if (this.modbusNet().isCommActive()) {
            ModbusReadRequest req;
            try {
               req = this.getPingRequest();
            } catch (Exception var4) {
               this.modbusNet().getModbusLog().error("Error in " + this.getName() + " pinging.  Check Device Status Monitor Address: ", var4);
               return;
            }

            ModbusResponse rsp = (ModbusResponse)this.sendModbusMessage(req);
            if (this.modbusNet().getModbusLog().isTraceOn()) {
               this.modbusNet().getModbusLog().trace("ping(): " + this.getName() + "[" + this.getDeviceAddress() + "] is " + (rsp == null ? "down" : "up"));
            }

            if (rsp != null && rsp.exceptionCode != 10 && rsp.exceptionCode != 11) {
               this.pingOk();
               this.resetPingsFailed();
            } else if (this.incrementPingsFailed() > ((BModbusClientNetwork)this.modbusNet()).getMaxFailsUntilDeviceDown()) {
               String cause = this.getLexicon().getText("pingFail");
               if (rsp != null) {
                  cause = cause + ": " + rsp.getExceptionString();
               }

               this.pingFail(cause);
            }
         }
      }
   }

   private ModbusReadRequest getPingRequest() {
      int address = this.getDeviceAddress();
      int pointAddress = this.getPointAddress();
      int count = 1;
      int dataMode = this.getModbusMode();
      if (this.isDataTypeLong()) {
         count *= 2;
      } else if (this.isDataTypeFloat()) {
         count *= 2;
      }

      int code;
      if (this.isHoldingRegisterType()) {
         code = 3;
      } else {
         code = 4;
      }

      return new ModbusReadRequest(dataMode, this, address, code, pointAddress, count);
   }

   public boolean isPresetMultiple() {
      return this.getModbusConfig().getOverrideNetwork()
         ? ((BModbusClientConfig)this.getModbusConfig()).getUsePresetMultipleRegister()
         : ((BModbusClientNetwork)this.getNetwork()).getUsePresetMultipleRegister();
   }

   public boolean isForceMultiple() {
      return this.getModbusConfig().getOverrideNetwork()
         ? ((BModbusClientConfig)this.getModbusConfig()).getUseForceMultipleCoil()
         : ((BModbusClientNetwork)this.getNetwork()).getUseForceMultipleCoil();
   }

   public int getPointAddress() {
      return this.getPingAddress().getDataAddress();
   }

   public boolean isHoldingRegisterType() {
      return this.getPingAddressRegType() == BRegisterTypeEnum.holding;
   }

   public boolean isDataTypeInteger() {
      return DataTypeUtil.is16BitInteger(this.getPingAddressDataType());
   }

   public boolean isDataTypeLong() {
      return DataTypeUtil.is32BitLong(this.getPingAddressDataType());
   }

   public boolean isDataTypeFloat() {
      return DataTypeUtil.isFloat(this.getPingAddressDataType());
   }

   public int getRegisterBaseAddress(BEnum registerType) {
      if (registerType.equals(BRegisterTypeEnum.holding)) {
         return this.getHoldingRegisterBaseAddress().getDataAddress();
      } else if (registerType.equals(BRegisterTypeEnum.input)) {
         return this.getInputRegisterBaseAddress().getDataAddress();
      } else {
         return registerType.equals(BStatusTypeEnum.coil)
            ? this.getCoilStatusBaseAddress().getDataAddress()
            : this.getInputStatusBaseAddress().getDataAddress();
      }
   }

   protected int getRegisterBaseAddressFromType(BRegisterTypesEnum registerType) {
      if (registerType.equals(BRegisterTypesEnum.holdingRegister)) {
         return this.getHoldingRegisterBaseAddress().getDataAddress();
      } else if (registerType.equals(BRegisterTypesEnum.inputRegister)) {
         return this.getInputRegisterBaseAddress().getDataAddress();
      } else {
         return registerType.equals(BRegisterTypesEnum.discreteCoil)
            ? this.getCoilStatusBaseAddress().getDataAddress()
            : this.getInputStatusBaseAddress().getDataAddress();
      }
   }

   private void updateProxyPointSubscriptions() {
      BControlPoint[] points = this.getPoints().getPoints();

      for (int i = 0; i < points.length; i++) {
         BComponent proxyExt = points[i].getProxyExt();
         if (proxyExt instanceof BModbusClientProxyExt) {
            ((BModbusClientProxyExt)proxyExt).adjustPollSubscription();
         }
      }
   }

   public BCommStatus getHoldingRegistersReadStatus(int address, int addressNum, BDevicePollConfigEntry entry) throws ModbusException {
      if (entry == null) {
         throw new ModbusException(102);
      } else {
         return entry.getReadStatus();
      }
   }

   public BCommStatus getInputRegistersReadStatus(int address, int addressNum, BDevicePollConfigEntry entry) throws ModbusException {
      if (entry == null) {
         throw new ModbusException(102);
      } else {
         return entry.getReadStatus();
      }
   }

   public BCommStatus getBinaryCoilsReadStatus(int address, int addressNum, BDevicePollConfigEntry entry) throws ModbusException {
      if (entry == null) {
         throw new ModbusException(102);
      } else {
         return entry.getReadStatus();
      }
   }

   public BCommStatus getBinaryInputsReadStatus(int address, int addressNum, BDevicePollConfigEntry entry) throws ModbusException {
      if (entry == null) {
         throw new ModbusException(102);
      } else {
         return entry.getReadStatus();
      }
   }

   public byte[] getHoldingRegisterValues(int address, int numRegisters, BDevicePollConfigEntry entry) throws ModbusException {
      byte[] registerData = new byte[numRegisters * 2];

      int startAddress;
      try {
         startAddress = entry.getStartAddress().getDataAddress();
      } catch (Exception var10) {
         throw new ModbusException(103);
      }

      if (address < startAddress) {
         throw new ModbusException(102);
      } else {
         int registerOffset = address - startAddress;
         if (registerOffset + numRegisters > entry.getConsecutivePointsToPoll()) {
            throw new ModbusException(102);
         } else {
            int registerByteOffset = registerOffset * 2;
            if (entry.getByteArray() == null) {
               throw new ModbusException(103);
            } else if (entry.getByteArray().length < entry.getConsecutivePointsToPoll() * 2) {
               throw new ModbusException(103);
            } else {
               byte[] holdingRegisterByteArray = entry.getByteArray();

               for (int i = 0; i < numRegisters * 2; i++) {
                  registerData[i] = holdingRegisterByteArray[registerByteOffset + i];
               }

               return registerData;
            }
         }
      }
   }

   public byte[] getInputRegisterValues(int address, int numRegisters, BDevicePollConfigEntry entry) throws ModbusException {
      byte[] registerData = new byte[numRegisters * 2];

      int startAddress;
      try {
         startAddress = entry.getStartAddress().getDataAddress();
      } catch (Exception var10) {
         throw new ModbusException(103);
      }

      if (address < startAddress) {
         throw new ModbusException(102);
      } else {
         int registerOffset = address - startAddress;
         if (registerOffset + numRegisters > entry.getConsecutivePointsToPoll()) {
            throw new ModbusException(102);
         } else {
            int registerByteOffset = registerOffset * 2;
            if (entry.getByteArray() == null) {
               throw new ModbusException(103);
            } else if (entry.getByteArray().length < entry.getConsecutivePointsToPoll() * 2) {
               throw new ModbusException(103);
            } else {
               byte[] inputRegisterByteArray = entry.getByteArray();

               for (int i = 0; i < numRegisters * 2; i++) {
                  registerData[i] = inputRegisterByteArray[registerByteOffset + i];
               }

               return registerData;
            }
         }
      }
   }

   public byte[] getCoilStatusValues(int address, int numRegisters, BDevicePollConfigEntry entry) throws ModbusException {
      int byteCount = 1;
      byte[] registerData = new byte[byteCount];

      int startAddress;
      try {
         startAddress = entry.getStartAddress().getDataAddress();
      } catch (Exception var13) {
         throw new ModbusException(103);
      }

      if (address < startAddress) {
         throw new ModbusException(102);
      } else {
         int registerOffset = address - startAddress;
         if (registerOffset + numRegisters > entry.getConsecutivePointsToPoll()) {
            throw new ModbusException(102);
         } else {
            int registerByteOffset = 0;
            int startBit = 0;
            int dataMask = 255 >> 8 - numRegisters;
            if (registerOffset != 0) {
               startBit = registerOffset % 8;
               registerByteOffset = registerOffset / 8;
            }

            if (entry.getByteArray() == null) {
               throw new ModbusException(103);
            } else if (entry.getByteArray().length < byteCount) {
               throw new ModbusException(103);
            } else {
               byte[] coilStatusByteArray = entry.getByteArray();
               if (startBit == 0) {
                  registerData[0] = (byte)(coilStatusByteArray[registerByteOffset] & dataMask);
               } else {
                  int temp = coilStatusByteArray[registerByteOffset] & 255;
                  if (startBit + numRegisters > 8) {
                     temp |= (coilStatusByteArray[registerByteOffset + 1] & 255) << 8;
                  }

                  temp >>= startBit;
                  registerData[0] = (byte)(temp & dataMask);
               }

               return registerData;
            }
         }
      }
   }

   public byte[] getInputStatusValues(int address, int numRegisters, BDevicePollConfigEntry entry) throws ModbusException {
      int byteCount = 1;
      byte[] registerData = new byte[byteCount];

      int startAddress;
      try {
         startAddress = entry.getStartAddress().getDataAddress();
      } catch (Exception var13) {
         throw new ModbusException(103);
      }

      if (address < startAddress) {
         throw new ModbusException(102);
      } else {
         int registerOffset = address - startAddress;
         if (registerOffset + numRegisters > entry.getConsecutivePointsToPoll()) {
            throw new ModbusException(102);
         } else {
            int registerByteOffset = 0;
            int startBit = 0;
            int dataMask = 255 >> 8 - numRegisters;
            if (registerOffset != 0) {
               startBit = registerOffset % 8;
               registerByteOffset = registerOffset / 8;
            }

            byte[] inputStatusByteArray = entry.getByteArray();
            if (inputStatusByteArray == null) {
               throw new ModbusException(103);
            } else if (inputStatusByteArray.length < byteCount) {
               throw new ModbusException(103);
            } else {
               if (startBit == 0) {
                  registerData[0] = (byte)(inputStatusByteArray[registerByteOffset] & dataMask);
               } else {
                  int temp = inputStatusByteArray[registerByteOffset] & 255;
                  if (startBit + numRegisters > 8) {
                     temp |= (inputStatusByteArray[registerByteOffset + 1] & 255) << 8;
                  }

                  temp >>= startBit;
                  registerData[0] = (byte)(temp & dataMask);
               }

               return registerData;
            }
         }
      }
   }

   public byte[] readRegisters(int code, int startAddress, int numRegisters, int minReadSize, BDevicePollConfigEntry entry) throws ModbusException {
      int address = this.getDeviceAddress();
      byte[] data = new byte[(numRegisters / minReadSize + 1) * minReadSize * 2];
      int maxReadSize = 125 - 125 % minReadSize;
      int modbusMode = this.getModbusMode();
      if (modbusMode == 0) {
         maxReadSize /= 2;
      }

      int count = 0;
      int bytesRead = 0;
      if (numRegisters == 0) {
         this.setCommStatusOutput(entry, -2);
         return data;
      } else if (code != 3 && code != 4) {
         throw new ModbusException(100);
      } else {
         do {
            if (numRegisters > maxReadSize) {
               count = maxReadSize;
            } else {
               count = numRegisters + numRegisters % minReadSize;
            }

            ModbusReadRequest req = new ModbusReadRequest(modbusMode, this, address, code, startAddress, count);
            ModbusResponse rsp = (ModbusResponse)this.sendModbusMessage(req);
            if (rsp == null) {
               rsp = new ModbusResponse(modbusMode, this);
               rsp.exceptionCode = 9;
               this.setCommStatusOutput(entry, rsp.exceptionCode);
               throw new ModbusException(101);
            }

            for (int i = 0; i < rsp.byteCount; i++) {
               data[bytesRead] = rsp.data[i];
               bytesRead++;
            }

            this.setCommStatusOutput(entry, rsp.exceptionCode);
            numRegisters -= maxReadSize;
            startAddress += maxReadSize;
         } while (numRegisters > 0);

         return data;
      }
   }

   public byte[] readStatusRegisters(int code, int startAddress, int numRegisters, BDevicePollConfigEntry entry) throws ModbusException {
      int address = this.getDeviceAddress();
      int count = 0;
      int maxReadSize = 2000;
      int modbusMode = this.getModbusMode();
      if (modbusMode == 0) {
         maxReadSize /= 2;
      }

      int byteCount = 0;
      if (numRegisters % 8 > 0) {
         byteCount = numRegisters / 8 + 1;
      } else {
         byteCount = numRegisters / 8;
      }

      byte[] data = new byte[byteCount];
      int bytesRead = 0;
      if (numRegisters == 0) {
         this.setCommStatusOutput(entry, -2);
         return data;
      } else if (code != 2 && code != 1) {
         throw new ModbusException(100);
      } else {
         do {
            if (numRegisters > maxReadSize) {
               count = maxReadSize;
            } else {
               count = numRegisters;
            }

            ModbusReadRequest req = new ModbusReadRequest(modbusMode, this, address, code, startAddress, count);
            ModbusResponse rsp = (ModbusResponse)this.sendModbusMessage(req);
            if (rsp == null) {
               rsp = new ModbusResponse(modbusMode, this);
               rsp.exceptionCode = 9;
               this.setCommStatusOutput(entry, rsp.exceptionCode);
               throw new ModbusException(101);
            }

            for (int i = 0; i < rsp.byteCount; i++) {
               data[bytesRead] = rsp.data[i];
               bytesRead++;
            }

            this.setCommStatusOutput(entry, rsp.exceptionCode);
            numRegisters -= maxReadSize;
            startAddress += maxReadSize;
         } while (numRegisters > 0);

         return data;
      }
   }

   public BDevicePollConfigEntry[] getOptimumDevicePollConfigEntryList() {
      BControlPoint[] points = this.getPoints().getPoints();
      if (points != null && points.length >= 1) {
         BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[points.length];
         int entryCount = 0;
         Vector<BModbusClientProxyExt> holdingRegisterList = new Vector<>();
         Vector<BModbusClientProxyExt> inputRegisterList = new Vector<>();
         Vector<BModbusClientProxyExt> coilStatusList = new Vector<>();
         Vector<BModbusClientProxyExt> inputStatusList = new Vector<>();

         for (int i = 0; i < points.length; i++) {
            BComponent proxyExt = points[i].getProxyExt();
            if (proxyExt instanceof BModbusClientProxyExt) {
               BRegisterTypesEnum dataType = ((BModbusClientProxyExt)proxyExt).determineRegisterType();
               if (dataType.equals(BRegisterTypesEnum.holdingRegister)) {
                  holdingRegisterList.addElement((BModbusClientProxyExt)proxyExt);
               } else if (dataType.equals(BRegisterTypesEnum.inputRegister)) {
                  inputRegisterList.addElement((BModbusClientProxyExt)proxyExt);
               } else if (dataType.equals(BRegisterTypesEnum.discreteCoil)) {
                  coilStatusList.addElement((BModbusClientProxyExt)proxyExt);
               } else if (dataType.equals(BRegisterTypesEnum.discreteInput)) {
                  inputStatusList.addElement((BModbusClientProxyExt)proxyExt);
               }
            }
         }

         if (holdingRegisterList.size() > 0) {
            int[] sortedList = sortByAbsoluteAddress(holdingRegisterList);
            if (sortedList != null && sortedList.length > 0) {
               int currentIndex = 0;

               while (currentIndex < sortedList.length) {
                  int startAddress = sortedList[currentIndex];
                  int consecutiveCount = 1;
                  int startIndex = currentIndex + 1;

                  for (int ix = startIndex; ix < sortedList.length; ix++) {
                     currentIndex = ix;
                     int difference = sortedList[ix] - sortedList[ix - 1];
                     if (difference > 0) {
                        if (difference != 1) {
                           break;
                        }

                        consecutiveCount++;
                     }
                  }

                  if (consecutiveCount > 1) {
                     BFlexAddress newAddr = new BFlexAddress();
                     newAddr.setAddressFormat(BAddressFormatEnum.modbus);
                     newAddr.setAddressFromInt(startAddress + 40001);
                     temp[entryCount] = new BDevicePollConfigEntry(true, BRegisterTypesEnum.holdingRegister, newAddr, consecutiveCount, 1);
                     entryCount++;
                  }

                  if (startIndex >= sortedList.length) {
                     currentIndex++;
                  }
               }
            }
         }

         if (inputRegisterList.size() > 0) {
            int[] sortedList = sortByAbsoluteAddress(inputRegisterList);
            if (sortedList != null && sortedList.length > 0) {
               int currentIndex = 0;

               while (currentIndex < sortedList.length) {
                  int startAddress = sortedList[currentIndex];
                  int consecutiveCount = 1;
                  int startIndex = currentIndex + 1;

                  for (int ixx = startIndex; ixx < sortedList.length; ixx++) {
                     currentIndex = ixx;
                     int difference = sortedList[ixx] - sortedList[ixx - 1];
                     if (difference > 0) {
                        if (difference != 1) {
                           break;
                        }

                        consecutiveCount++;
                     }
                  }

                  if (consecutiveCount > 1) {
                     BFlexAddress newAddr = new BFlexAddress();
                     newAddr.setAddressFormat(BAddressFormatEnum.modbus);
                     newAddr.setAddressFromInt(startAddress + 30001);
                     temp[entryCount] = new BDevicePollConfigEntry(true, BRegisterTypesEnum.inputRegister, newAddr, consecutiveCount, 1);
                     entryCount++;
                  }

                  if (startIndex >= sortedList.length) {
                     currentIndex++;
                  }
               }
            }
         }

         if (coilStatusList.size() > 0) {
            int[] sortedList = sortByAbsoluteAddress(coilStatusList);
            if (sortedList != null && sortedList.length > 0) {
               int currentIndex = 0;

               while (currentIndex < sortedList.length) {
                  int startAddress = sortedList[currentIndex];
                  int consecutiveCount = 1;
                  int startIndex = currentIndex + 1;

                  for (int ixxx = startIndex; ixxx < sortedList.length; ixxx++) {
                     currentIndex = ixxx;
                     int difference = sortedList[ixxx] - sortedList[ixxx - 1];
                     if (difference > 0) {
                        if (difference != 1) {
                           break;
                        }

                        consecutiveCount++;
                     }
                  }

                  if (consecutiveCount > 1) {
                     BFlexAddress newAddr = new BFlexAddress();
                     newAddr.setAddressFormat(BAddressFormatEnum.modbus);
                     newAddr.setAddressFromInt(startAddress + 1);
                     temp[entryCount] = new BDevicePollConfigEntry(true, BRegisterTypesEnum.discreteCoil, newAddr, consecutiveCount, 1);
                     entryCount++;
                  }

                  if (startIndex >= sortedList.length) {
                     currentIndex++;
                  }
               }
            }
         }

         if (inputStatusList.size() > 0) {
            int[] sortedList = sortByAbsoluteAddress(inputStatusList);
            if (sortedList != null && sortedList.length > 0) {
               int currentIndex = 0;

               while (currentIndex < sortedList.length) {
                  int startAddress = sortedList[currentIndex];
                  int consecutiveCount = 1;
                  int startIndex = currentIndex + 1;

                  for (int ixxxx = startIndex; ixxxx < sortedList.length; ixxxx++) {
                     currentIndex = ixxxx;
                     int difference = sortedList[ixxxx] - sortedList[ixxxx - 1];
                     if (difference > 0) {
                        if (difference != 1) {
                           break;
                        }

                        consecutiveCount++;
                     }
                  }

                  if (consecutiveCount > 1) {
                     BFlexAddress newAddr = new BFlexAddress();
                     newAddr.setAddressFormat(BAddressFormatEnum.modbus);
                     newAddr.setAddressFromInt(startAddress + 10001);
                     temp[entryCount] = new BDevicePollConfigEntry(true, BRegisterTypesEnum.discreteInput, newAddr, consecutiveCount, 1);
                     entryCount++;
                  }

                  if (startIndex >= sortedList.length) {
                     currentIndex++;
                  }
               }
            }
         }

         BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[entryCount];
         System.arraycopy(temp, 0, result, 0, entryCount);
         return result;
      } else {
         return null;
      }
   }

   private static int[] sortByAbsoluteAddress(Vector<BModbusClientProxyExt> proxyList) {
      Vector<Integer> addressList = new Vector<>();
      BModbusClientProxyExt[] proxies = proxyList.toArray(new BModbusClientProxyExt[0]);

      for (int i = 0; i < proxies.length; i++) {
         if (proxies[i] instanceof BModbusClientStringProxyExt) {
            addressList.addElement(((BModbusClientStringProxyExt)proxies[i]).getAbsoluteAddress().getDataAddress());

            for (int j = 1; j < ((BModbusClientStringProxyExt)proxies[i]).getNumberRegisters(); j++) {
               addressList.addElement(((BModbusClientStringProxyExt)proxies[i]).getAbsoluteAddress().getDataAddress() + j);
            }
         } else if (proxies[i] instanceof BModbusClientNumericProxyExt) {
            addressList.addElement(((BModbusClientNumericProxyExt)proxies[i]).getAbsoluteAddress().getDataAddress());
            if (((BModbusClientNumericProxyExt)proxies[i]).getDataType().equals(BDataTypeEnum.floatType)
               || ((BModbusClientNumericProxyExt)proxies[i]).getDataType().equals(BDataTypeEnum.longType)) {
               addressList.addElement(((BModbusClientNumericProxyExt)proxies[i]).getAbsoluteAddress().getDataAddress() + 1);
            }
         } else if (proxies[i] instanceof BModbusClientProxyExt) {
            addressList.addElement(proxies[i].getAbsoluteAddress().getDataAddress());
         }
      }

      if (addressList.size() < 1) {
         return null;
      } else {
         Object[] addresses = addressList.toArray();
         int[] sortedList = new int[addresses.length];

         for (int ix = 0; ix < addresses.length; ix++) {
            sortedList[ix] = (Integer)addresses[ix];
         }

         Arrays.sort(sortedList);
         return sortedList;
      }
   }

   @Override
   public BPointDeviceExt getPointDeviceExt() {
      return this.getPoints();
   }

   public int incrementPingsFailed() {
      this.pingsFailed++;
      return this.pingsFailed;
   }

   public void resetPingsFailed() {
      this.pingsFailed = 0;
   }

   public void addWritableProxy(BModbusClientProxyExt proxyExt) {
      if (!this.writableProxies.contains(proxyExt)) {
         this.writableProxies.add(proxyExt);
      }
   }

   public void removeWritableProxy(BModbusClientProxyExt proxyExt) {
      this.writableProxies.remove(proxyExt);
   }

   public boolean writablePointAlreadyExists(BModbusClientProxyExt newExt) {
      if (newExt == null) {
         return false;
      } else if (this.writableProxies.size() <= 0) {
         return false;
      } else {
         int newOffset = 0;
         if (newExt instanceof BModbusClientNumericProxyExt
            && (((BModbusClientNumericProxyExt)newExt).isDataTypeLong() || ((BModbusClientNumericProxyExt)newExt).isDataTypeFloat())) {
            newOffset = 1;
         }

         int newAddress = newExt.getDataAddress().getDataAddress();
         if (newAddress < 0) {
            return false;
         } else {
            for (int i = 0; i < this.writableProxies.size(); i++) {
               BModbusClientProxyExt existingProxy = this.writableProxies.elementAt(i);
               if (!existingProxy.equals(newExt) && existingProxy instanceof BModbusClientProxyExt) {
                  int existAddress = existingProxy.getDataAddress().getDataAddress();
                  if (existingProxy instanceof BModbusClientRegisterBitProxyExt && newExt instanceof BModbusClientRegisterBitProxyExt) {
                     if (existingProxy.determineRegisterType().equals(newExt.determineRegisterType())
                        && existAddress == newAddress
                        && ((BModbusClientRegisterBitProxyExt)existingProxy).getBitNumber() == ((BModbusClientRegisterBitProxyExt)newExt).getBitNumber()) {
                        return true;
                     }
                  } else if (existingProxy instanceof BModbusClientNumericBitsProxyExt && newExt instanceof BModbusClientNumericBitsProxyExt) {
                     if (existingProxy.determineRegisterType().equals(newExt.determineRegisterType())
                        && existAddress == newAddress
                        && (((BModbusClientNumericBitsProxyExt)existingProxy).getRegisterMask() & ((BModbusClientNumericBitsProxyExt)newExt).getRegisterMask())
                           != 0) {
                        return true;
                     }
                  } else if (existingProxy instanceof BModbusClientNumericBitsProxyExt && newExt instanceof BModbusClientRegisterBitProxyExt) {
                     if (existingProxy.determineRegisterType().equals(newExt.determineRegisterType())
                        && existAddress == newAddress
                        && (
                              ((BModbusClientNumericBitsProxyExt)existingProxy).getRegisterMask()
                                 & 1 << ((BModbusClientRegisterBitProxyExt)newExt).getBitNumber()
                           )
                           != 0) {
                        return true;
                     }
                  } else if (existingProxy instanceof BModbusClientRegisterBitProxyExt && newExt instanceof BModbusClientNumericBitsProxyExt) {
                     if (existingProxy.determineRegisterType().equals(newExt.determineRegisterType())
                        && existAddress == newAddress
                        && (
                              ((BModbusClientNumericBitsProxyExt)newExt).getRegisterMask()
                                 & 1 << ((BModbusClientRegisterBitProxyExt)existingProxy).getBitNumber()
                           )
                           != 0) {
                        return true;
                     }
                  } else {
                     int existOffset = 0;
                     if (existingProxy instanceof BModbusClientNumericProxyExt
                        && (((BModbusClientNumericProxyExt)existingProxy).isDataTypeLong() || ((BModbusClientNumericProxyExt)existingProxy).isDataTypeFloat())) {
                        existOffset = 1;
                     }

                     if (existingProxy.determineRegisterType().equals(newExt.determineRegisterType())
                        && (
                           existAddress == newAddress
                              || existAddress == newAddress + newOffset
                              || existAddress + existOffset == newAddress
                              || existAddress + existOffset == newAddress + newOffset
                        )) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }
      }
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);

      try {
         out.startProps();
         out.trTitle("WritablePoint Cache", 1);

         for (int i = 0; i < this.writableProxies.size(); i++) {
            BModbusClientProxyExt proxy = this.writableProxies.elementAt(i);
            BControlPoint point = proxy.getParentPoint();
            String name = "";
            if (proxy.getType().is(BModbusClientBooleanProxyExt.TYPE)) {
               name = "Coil   : 0x";
            } else {
               name = "Holding: 0x";
            }

            name = name + Integer.toHexString(proxy.getDataAddress().getDataAddress()) + ": ";
            out.prop(name, proxy.getParentPoint().getSlotPath());
         }

         out.endProps();
      } catch (Exception var6) {
         out.endProps();
      }
   }
}
