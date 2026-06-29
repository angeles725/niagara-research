package com.tridium.modbusCore.server;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.ModbusErrorCodes;
import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import com.tridium.modbusCore.server.datatypes.BModbusRegisterRangeEntry;
import com.tridium.modbusCore.server.datatypes.BModbusRegisterRangeTable;
import com.tridium.modbusCore.server.datatypes.BModbusServerStringRecord;
import com.tridium.modbusCore.server.point.BModbusServerPointDeviceExt;
import java.util.ArrayList;
import javax.baja.driver.point.BPointDeviceExt;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "validCoilsRange",
      type = "BModbusRegisterRangeTable",
      defaultValue = "new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText(\"device.strings.defaultRange\"))"
   ), @NiagaraProperty(
      name = "validStatusRange",
      type = "BModbusRegisterRangeTable",
      defaultValue = "new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText(\"device.strings.defaultRange\"))"
   ), @NiagaraProperty(
      name = "validHoldingRegistersRange",
      type = "BModbusRegisterRangeTable",
      defaultValue = "new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText(\"device.strings.defaultRange\"))"
   ), @NiagaraProperty(
      name = "validInputRegistersRange",
      type = "BModbusRegisterRangeTable",
      defaultValue = "new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText(\"device.strings.defaultRange\"))"
   ), @NiagaraProperty(
      name = "points",
      type = "BModbusServerPointDeviceExt",
      defaultValue = "new BModbusServerPointDeviceExt()"
   )})
public abstract class BModbusServerDevice extends BModbusDevice implements ModbusMessageConst, ModbusErrorCodes {
   public static Lexicon lex = Lexicon.make("modbusCore");
   public static final Property validCoilsRange = newProperty(
      0, new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText("device.strings.defaultRange")), null
   );
   public static final Property validStatusRange = newProperty(
      0, new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText("device.strings.defaultRange")), null
   );
   public static final Property validHoldingRegistersRange = newProperty(
      0, new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText("device.strings.defaultRange")), null
   );
   public static final Property validInputRegistersRange = newProperty(
      0, new BModbusRegisterRangeTable(new BModbusRegisterRangeEntry(), lex.getText("device.strings.defaultRange")), null
   );
   public static final Property points = newProperty(0, new BModbusServerPointDeviceExt(), null);
   public static final Type TYPE = Sys.loadType(BModbusServerDevice.class);
   private int prevDeviceAddress;
   private BModbusServerNetwork serverNetwork;
   private boolean isDeviceStarted = false;
   private static final BIcon icon = BIcon.std("deviceLocal.png");
   private IntHashMap inputRegisterByteArray = null;
   private IntHashMap holdingRegisterByteArray = null;
   private IntHashMap coilStatusBitSet = null;
   private IntHashMap inputStatusBitSet = null;

   public BModbusRegisterRangeTable getValidCoilsRange() {
      return (BModbusRegisterRangeTable)this.get(validCoilsRange);
   }

   public void setValidCoilsRange(BModbusRegisterRangeTable v) {
      this.set(validCoilsRange, v, null);
   }

   public BModbusRegisterRangeTable getValidStatusRange() {
      return (BModbusRegisterRangeTable)this.get(validStatusRange);
   }

   public void setValidStatusRange(BModbusRegisterRangeTable v) {
      this.set(validStatusRange, v, null);
   }

   public BModbusRegisterRangeTable getValidHoldingRegistersRange() {
      return (BModbusRegisterRangeTable)this.get(validHoldingRegistersRange);
   }

   public void setValidHoldingRegistersRange(BModbusRegisterRangeTable v) {
      this.set(validHoldingRegistersRange, v, null);
   }

   public BModbusRegisterRangeTable getValidInputRegistersRange() {
      return (BModbusRegisterRangeTable)this.get(validInputRegistersRange);
   }

   public void setValidInputRegistersRange(BModbusRegisterRangeTable v) {
      this.set(validInputRegistersRange, v, null);
   }

   public BModbusServerPointDeviceExt getPoints() {
      return (BModbusServerPointDeviceExt)this.get(points);
   }

   public void setPoints(BModbusServerPointDeviceExt v) {
      this.set(points, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.coilStatusBitSet = this.initCoilMap();
      this.inputStatusBitSet = this.initStatusMap();
      this.holdingRegisterByteArray = this.initHoldingRegisterMap();
      this.inputRegisterByteArray = this.initInputRegisterMap();
      this.setStatusForDuplicateAddress();
      this.isDeviceStarted = true;
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(validCoilsRange)) {
            if (this.coilStatusBitSet == null) {
               return;
            }

            Integer[] addresses = this.getValidCoilsRange().getValidAddressArray();
            int hashSize = addresses.length;
            if (hashSize <= 0) {
               hashSize = 1;
            }

            IntHashMap temp = new IntHashMap(hashSize);

            for (int i = 0; i < addresses.length; i++) {
               Object value = this.coilStatusBitSet.get(addresses[i]);
               if (value != null) {
                  temp.put(addresses[i], value);
               } else {
                  temp.put(addresses[i], false);
               }
            }

            this.coilStatusBitSet = temp;
            this.getValidCoilsRange().setPersistedData(this.coilStatusBitSet, false);
         } else if (property.equals(validStatusRange)) {
            if (this.inputStatusBitSet == null) {
               return;
            }

            Integer[] addresses = this.getValidStatusRange().getValidAddressArray();
            int hashSize = addresses.length;
            if (hashSize <= 0) {
               hashSize = 1;
            }

            IntHashMap temp = new IntHashMap(hashSize);

            for (int ix = 0; ix < addresses.length; ix++) {
               Object value = this.inputStatusBitSet.get(addresses[ix]);
               if (value != null) {
                  temp.put(addresses[ix], value);
               } else {
                  temp.put(addresses[ix], false);
               }
            }

            this.inputStatusBitSet = temp;
         } else if (property.equals(validHoldingRegistersRange)) {
            if (this.holdingRegisterByteArray == null) {
               return;
            }

            Integer[] addresses = this.getValidHoldingRegistersRange().getValidAddressArray();
            int hashSize = addresses.length * 2;
            if (hashSize <= 0) {
               hashSize = 1;
            }

            IntHashMap temp = new IntHashMap(hashSize);

            for (int ixx = 0; ixx < addresses.length; ixx++) {
               Object value = this.holdingRegisterByteArray.get(addresses[ixx] * 2);
               if (value != null) {
                  temp.put(addresses[ixx] * 2, value);
               } else {
                  temp.put(addresses[ixx] * 2, (byte)0);
               }

               value = this.holdingRegisterByteArray.get(addresses[ixx] * 2 + 1);
               if (value != null) {
                  temp.put(addresses[ixx] * 2 + 1, value);
               } else {
                  temp.put(addresses[ixx] * 2 + 1, (byte)0);
               }
            }

            this.holdingRegisterByteArray = temp;
            this.getValidHoldingRegistersRange().setPersistedData(this.holdingRegisterByteArray, true);
         } else if (property.equals(validInputRegistersRange)) {
            if (this.inputRegisterByteArray == null) {
               return;
            }

            Integer[] addresses = this.getValidInputRegistersRange().getValidAddressArray();
            int hashSize = addresses.length * 2;
            if (hashSize <= 0) {
               hashSize = 1;
            }

            IntHashMap temp = new IntHashMap(hashSize);

            for (int ixx = 0; ixx < addresses.length; ixx++) {
               Object valuex = this.inputRegisterByteArray.get(addresses[ixx] * 2);
               if (valuex != null) {
                  temp.put(addresses[ixx] * 2, valuex);
               } else {
                  temp.put(addresses[ixx] * 2, (byte)0);
               }

               valuex = this.inputRegisterByteArray.get(addresses[ixx] * 2 + 1);
               if (valuex != null) {
                  temp.put(addresses[ixx] * 2 + 1, valuex);
               } else {
                  temp.put(addresses[ixx] * 2 + 1, (byte)0);
               }
            }

            this.inputRegisterByteArray = temp;
         }

         if (property.equals(deviceAddress) && this.isDeviceStarted) {
            this.serverNetwork.removeDeviceAddress(this.prevDeviceAddress);
            this.setStatusForDuplicateAddress();
         }
      }
   }

   private IntHashMap initCoilMap() {
      Integer[] addresses = this.getValidCoilsRange().getValidAddressArray();
      int hashSize = addresses.length;
      if (hashSize <= 0) {
         hashSize = 1;
      }

      IntHashMap temp = new IntHashMap(hashSize);
      BModbusRegisterRangeEntry[] registerRangeEntries = this.getValidCoilsRange().getEnabledModbusRegisterRangeList();

      for (BModbusRegisterRangeEntry rangeEntry : registerRangeEntries) {
         int rangeSize = rangeEntry.getSize();
         byte[] persistedData = rangeEntry.getPersistedData();
         if (persistedData == null) {
            persistedData = new byte[rangeSize];

            for (int i = 0; i < persistedData.length; i++) {
               persistedData[i] = 0;
            }
         }

         int startingAddress = rangeEntry.getStartingAddressOffset() - 1;

         for (int i = 0; i < rangeEntry.getSize(); i++) {
            temp.put(startingAddress++, persistedData[i] != 0);
         }

         rangeEntry.setPersistedData(persistedData);
      }

      return temp;
   }

   private IntHashMap initStatusMap() {
      Integer[] addresses = this.getValidStatusRange().getValidAddressArray();
      int hashSize = addresses.length;
      if (hashSize <= 0) {
         hashSize = 1;
      }

      IntHashMap temp = new IntHashMap(hashSize);

      for (int i = 0; i < addresses.length; i++) {
         temp.put(addresses[i], false);
      }

      return temp;
   }

   private IntHashMap initHoldingRegisterMap() {
      Integer[] addresses = this.getValidHoldingRegistersRange().getValidAddressArray();
      int hashSize = addresses.length * 2;
      if (hashSize <= 0) {
         hashSize = 1;
      }

      IntHashMap temp = new IntHashMap(hashSize);
      BModbusRegisterRangeEntry[] registerRangeEntries = this.getValidHoldingRegistersRange().getEnabledModbusRegisterRangeList();

      for (BModbusRegisterRangeEntry rangeEntry : registerRangeEntries) {
         int rangeSize = rangeEntry.getSize();
         byte[] persistedData = rangeEntry.getPersistedData();
         if (persistedData == null) {
            persistedData = new byte[rangeSize * 2];

            for (int i = 0; i < persistedData.length; i++) {
               persistedData[i] = 0;
            }
         }

         int startingAddress = rangeEntry.getStartingAddressOffset() - 1;

         for (int i = 0; i < rangeEntry.getSize(); i++) {
            temp.put(startingAddress * 2, persistedData[i * 2]);
            temp.put(startingAddress * 2 + 1, persistedData[i * 2 + 1]);
            startingAddress++;
         }

         rangeEntry.setPersistedData(persistedData);
      }

      return temp;
   }

   private IntHashMap initInputRegisterMap() {
      Integer[] addresses = this.getValidInputRegistersRange().getValidAddressArray();
      int hashSize = addresses.length * 2;
      if (hashSize <= 0) {
         hashSize = 1;
      }

      IntHashMap temp = new IntHashMap(hashSize);

      for (int i = 0; i < addresses.length; i++) {
         temp.put(addresses[i] * 2, (byte)0);
         temp.put(addresses[i] * 2 + 1, (byte)0);
      }

      return temp;
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName()).append("[" + this.getDeviceAddress() + "]");
      return sb.toString();
   }

   public void doPing() {
      if (!this.isDisabled() && !this.isFault()) {
         if (this.modbusNet() == null) {
            this.pingFail("No modbus network found");
         } else {
            if (this.modbusNet().getModbusLog().isTraceOn()) {
               this.modbusNet().getModbusLog().trace("ping(): ModbusServerDevice " + this.getName() + "[" + this.getDeviceAddress() + "] is up");
            }

            this.pingOk();
         }
      }
   }

   public boolean isHoldingRegisterAddressValid(int address, int numRegisters) {
      for (int i = address * 2; i < (address + numRegisters) * 2; i++) {
         if (this.holdingRegisterByteArray.get(i) == null) {
            return false;
         }
      }

      return true;
   }

   public byte[] getHoldingRegisterValues(int address, int numRegisters) throws ModbusException {
      byte[] registerData = new byte[numRegisters * 2];

      for (int i = address * 2; i < (address + numRegisters) * 2; i++) {
         if (this.holdingRegisterByteArray.get(i) == null) {
            throw new ModbusException(103);
         }
      }

      for (int ix = 0; ix < numRegisters * 2; ix++) {
         registerData[ix] = (Byte)this.holdingRegisterByteArray.get(address * 2 + ix);
      }

      return registerData;
   }

   public void setHoldingRegisterValues(int address, byte[] data) throws ModbusException {
      for (int i = 0; i < data.length; i++) {
         if (this.holdingRegisterByteArray.get(address * 2 + i) == null) {
            throw new ModbusException(103);
         }

         this.holdingRegisterByteArray.put(address * 2 + i, data[i]);
         this.getValidHoldingRegistersRange().setPersistedData(this.holdingRegisterByteArray, true);
      }
   }

   public boolean isInputRegisterAddressValid(int address, int numRegisters) {
      for (int i = address * 2; i < (address + numRegisters) * 2; i++) {
         if (this.inputRegisterByteArray.get(i) == null) {
            return false;
         }
      }

      return true;
   }

   public byte[] getInputRegisterValues(int address, int numRegisters) throws ModbusException {
      byte[] registerData = new byte[numRegisters * 2];

      for (int i = address * 2; i < (address + numRegisters) * 2; i++) {
         if (this.inputRegisterByteArray.get(i) == null) {
            throw new ModbusException(103);
         }
      }

      for (int ix = 0; ix < numRegisters * 2; ix++) {
         registerData[ix] = (Byte)this.inputRegisterByteArray.get(address * 2 + ix);
      }

      return registerData;
   }

   public void setInputRegisterValues(int address, byte[] data) throws ModbusException {
      for (int i = 0; i < data.length; i++) {
         if (this.inputRegisterByteArray.get(address * 2 + i) == null) {
            throw new ModbusException(103);
         }

         this.inputRegisterByteArray.put(address * 2 + i, data[i]);
      }
   }

   public boolean isCoilAddressValid(int address, int numRegisters) {
      for (int i = address; i < address + numRegisters; i++) {
         if (this.coilStatusBitSet.get(i) == null) {
            return false;
         }
      }

      return true;
   }

   public byte[] getCoilStatusValues(int address, int numRegisters) throws ModbusException {
      int byteCount = numRegisters / 8;
      if (numRegisters % 8 != 0) {
         byteCount++;
      }

      byte[] registerData = new byte[byteCount];

      for (int i = address; i < address + numRegisters; i++) {
         if (this.coilStatusBitSet.get(i) == null) {
            throw new ModbusException(103);
         }
      }

      int bitIndex = 0;
      int byteIndex = 0;

      for (int ix = 0; ix < numRegisters; ix++) {
         int dataMask = 1 << bitIndex;
         if ((Boolean)this.coilStatusBitSet.get(address + ix)) {
            registerData[byteIndex] = (byte)(registerData[byteIndex] | dataMask);
         } else {
            registerData[byteIndex] = (byte)(registerData[byteIndex] & ~dataMask);
         }

         if (++bitIndex == 8) {
            bitIndex = 0;
            byteIndex++;
         }
      }

      return registerData;
   }

   public void setCoilStatusValue(int address, boolean value) throws ModbusException {
      if (this.coilStatusBitSet.get(address) == null) {
         throw new ModbusException(103);
      } else {
         this.coilStatusBitSet.put(address, value);
         this.getValidCoilsRange().setPersistedData(this.coilStatusBitSet, false);
      }
   }

   public void setCoilStatusValue(int address, int numberCoils, byte[] data) throws ModbusException {
      try {
         int bitIndex = 0;
         int byteIndex = 0;

         for (int i = 0; i < numberCoils; i++) {
            int dataMask = 1 << bitIndex;
            this.setCoilStatusValue(address, (data[byteIndex] & dataMask) != 0);
            address++;
            if (++bitIndex >= 8) {
               bitIndex = 0;
               byteIndex++;
            }
         }

         this.getValidCoilsRange().setPersistedData(this.coilStatusBitSet, false);
      } catch (ModbusException var8) {
         if (this.modbusNet() != null && this.modbusNet().getModbusLog().isTraceOn()) {
            this.modbusNet().getModbusLog().trace(this.getName() + ".setCoilStatusValue() caught Exception: ", var8);
         }

         throw var8;
      } catch (Exception var9) {
         if (this.modbusNet() != null) {
            this.modbusNet().getModbusLog().error(this.getName() + ".setCoilStatusValue() caught Exception: ", var9);
         }
      }
   }

   public boolean isStatusAddressValid(int address, int numRegisters) {
      for (int i = address; i < address + numRegisters; i++) {
         if (this.inputStatusBitSet.get(i) == null) {
            return false;
         }
      }

      return true;
   }

   public byte[] getInputStatusValues(int address, int numRegisters) throws ModbusException {
      int byteCount = numRegisters / 8;
      if (numRegisters % 8 != 0) {
         byteCount++;
      }

      byte[] registerData = new byte[byteCount];

      for (int i = address; i < address + numRegisters; i++) {
         if (this.inputStatusBitSet.get(i) == null) {
            throw new ModbusException(103);
         }
      }

      int bitIndex = 0;
      int byteIndex = 0;

      for (int ix = 0; ix < numRegisters; ix++) {
         int dataMask = 1 << bitIndex;
         if ((Boolean)this.inputStatusBitSet.get(address + ix)) {
            registerData[byteIndex] = (byte)(registerData[byteIndex] | dataMask);
         } else {
            registerData[byteIndex] = (byte)(registerData[byteIndex] & ~dataMask);
         }

         if (++bitIndex == 8) {
            bitIndex = 0;
            byteIndex++;
         }
      }

      return registerData;
   }

   public void setInputStatusValue(int address, boolean value) throws ModbusException {
      if (this.inputStatusBitSet.get(address) == null) {
         throw new ModbusException(103);
      } else {
         this.inputStatusBitSet.put(address, value);
      }
   }

   public byte[] getFileRecordData(int fileNum, int startingRec, int recLength) throws ModbusException {
      BModbusServerStringRecord[] childStringRecords = this.getStringFileRecords();
      if (childStringRecords == null) {
         throw new ModbusException(103);
      } else {
         for (int i = 0; i < childStringRecords.length; i++) {
            if (childStringRecords[i] != null
               && childStringRecords[i].getFileNumber() == fileNum
               && childStringRecords[i].containsRecords(startingRec, recLength)) {
               return childStringRecords[i].getBytes(startingRec, recLength);
            }
         }

         throw new ModbusException(103);
      }
   }

   public byte[] setFileRecordData(int fileNum, int startingRec, int recLength, byte[] data) throws ModbusException {
      BModbusServerStringRecord[] childStringRecords = this.getStringFileRecords();
      if (childStringRecords == null) {
         throw new ModbusException(103);
      } else {
         for (int i = 0; i < childStringRecords.length; i++) {
            if (childStringRecords[i] != null
               && childStringRecords[i].getFileNumber() == fileNum
               && childStringRecords[i].containsRecords(startingRec, recLength)) {
               return childStringRecords[i].setBytes(data, startingRec, recLength);
            }
         }

         throw new ModbusException(103);
      }
   }

   private BModbusServerStringRecord[] getStringFileRecords() {
      ArrayList<BModbusServerStringRecord> list = new ArrayList<>();
      this.getFileRecords(this, BModbusServerStringRecord.TYPE, list);
      return list.toArray(new BModbusServerStringRecord[0]);
   }

   private void getFileRecords(BComponent comp, Type fileType, ArrayList list) {
      SlotCursor<Property> cursor = comp.loadSlots().getProperties();

      while (cursor.nextComponent()) {
         BComponent kid = cursor.get().asComponent();
         if (kid.getType().is(fileType)) {
            list.add(kid);
         }

         this.getFileRecords(kid, fileType, list);
      }
   }

   @Override
   public BPointDeviceExt getPointDeviceExt() {
      return this.getPoints();
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("CoilRegisterMap", 2);
      this.writeIntHashMap(out, this.coilStatusBitSet);
      out.startProps();
      out.trTitle("inputStatusRegisterMap", 2);
      this.writeIntHashMap(out, this.inputStatusBitSet);
      out.startProps();
      out.trTitle("holdingRegisterMap", 2);
      this.writeIntHashMap(out, this.holdingRegisterByteArray);
      out.startProps();
      out.trTitle("inputRegisterMap", 2);
      this.writeIntHashMap(out, this.inputRegisterByteArray);
   }

   private void writeIntHashMap(SpyWriter out, IntHashMap map) {
      Iterator iterator = map.iterator();

      while (iterator.hasNext()) {
         Object value = iterator.next();
         if (value instanceof Byte) {
            int lowByte = ((Byte)value).intValue() & 0xFF;
            int regAddr = iterator.key() / 2;
            int hiByte = ((Byte)iterator.next()).intValue() & 0xFF;
            int intValue = (hiByte << 8) + lowByte;
            String s = String.format("%04x", intValue);
            out.prop(regAddr, "0x" + s);
         } else if (value instanceof Boolean) {
            out.prop(iterator.key(), value);
         }
      }

      out.endProps();
   }

   private void setStatusForDuplicateAddress() {
      this.serverNetwork = (BModbusServerNetwork)this.getNetwork();
      if (this.serverNetwork.isUniqueDeviceAddress(this.getDeviceAddress())) {
         this.serverNetwork.addDeviceAddress(this.getDeviceAddress());
         this.prevDeviceAddress = this.getDeviceAddress();
         if (this.getStatus() == BStatus.fault && this.getFaultCause().contains("Duplicate Device Address")) {
            this.setStatus(BStatus.DEFAULT);
            this.setFaultCause("");
         }
      } else {
         this.setStatus(BStatus.fault);
         this.setFaultCause("Duplicate Device Address");
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
