package com.tridium.modbusCore.client.datatypes;

import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.enums.BReplaceExistingEnum;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "learnOptimumDevicePollConfig",
      parameterType = "BReplaceExistingEnum",
      defaultValue = "BReplaceExistingEnum.replaceExistingEntries"
   ), @NiagaraAction(
      name = "clear",
      flags = 128
   )})
public class BDevicePollConfigTable extends BComponent {
   public static final Action learnOptimumDevicePollConfig = newAction(0, BReplaceExistingEnum.replaceExistingEntries, null);
   public static final Action clear = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BDevicePollConfigTable.class);

   public void learnOptimumDevicePollConfig(BReplaceExistingEnum parameter) {
      this.invoke(learnOptimumDevicePollConfig, parameter, null);
   }

   public void clear() {
      this.invoke(clear, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public synchronized BDevicePollConfigEntry[] getDevicePollConfigList() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;

      for (SlotCursor<Property> c = this.getProperties(); c.next(BDevicePollConfigEntry.class); count++) {
         BObject kid = c.get();
         temp[count] = (BDevicePollConfigEntry)kid;
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public BDevicePollConfigEntry[] getActiveInputRegisterPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.inputRegister)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public BDevicePollConfigEntry[] getActiveHoldingRegisterPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.holdingRegister)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public BDevicePollConfigEntry[] getActiveBinaryCoilPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.discreteCoil)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public BDevicePollConfigEntry[] getActiveBinaryInputPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.discreteInput)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public synchronized BDevicePollConfigEntry[] getPossibleInputRegisterPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.inputRegister)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public synchronized BDevicePollConfigEntry[] getPossibleHoldingRegisterPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.holdingRegister)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public synchronized BDevicePollConfigEntry[] getPossibleBinaryCoilPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.discreteCoil)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public synchronized BDevicePollConfigEntry[] getPossibleBinaryInputPollEntries() {
      BDevicePollConfigEntry[] temp = new BDevicePollConfigEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDevicePollConfigEntry.class)) {
         BObject kid = c.get();
         if (((BDevicePollConfigEntry)kid).getEnabled() && ((BDevicePollConfigEntry)kid).getDataType().equals(BRegisterTypesEnum.discreteInput)) {
            temp[count] = (BDevicePollConfigEntry)kid;
            count++;
         }
      }

      BDevicePollConfigEntry[] result = new BDevicePollConfigEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public BDevicePollConfigEntry isRegisterDevicePolled(BRegisterTypesEnum registerType, BFlexAddress registerAddress, int numAddresses) {
      try {
         BDevicePollConfigEntry entry = null;
         if (registerType == BRegisterTypesEnum.holdingRegister) {
            entry = findPollConfigEntry(this.getPossibleHoldingRegisterPollEntries(), registerAddress.getDataAddress(), numAddresses);
            if (entry != null) {
               return entry;
            }
         } else if (registerType == BRegisterTypesEnum.inputRegister) {
            entry = findPollConfigEntry(this.getPossibleInputRegisterPollEntries(), registerAddress.getDataAddress(), numAddresses);
            if (entry != null) {
               return entry;
            }
         } else if (registerType == BRegisterTypesEnum.discreteCoil) {
            entry = findPollConfigEntry(this.getPossibleBinaryCoilPollEntries(), registerAddress.getDataAddress(), numAddresses);
            if (entry != null) {
               return entry;
            }
         } else if (registerType == BRegisterTypesEnum.discreteInput) {
            entry = findPollConfigEntry(this.getPossibleBinaryInputPollEntries(), registerAddress.getDataAddress(), numAddresses);
            if (entry != null) {
               return entry;
            }
         }

         return null;
      } catch (Exception var5) {
         return null;
      }
   }

   public static BDevicePollConfigEntry findPollConfigEntry(BDevicePollConfigEntry[] entries, int registerAddress, int numAddresses) {
      if (entries == null) {
         return null;
      } else {
         int lastRegisterAddress = registerAddress + numAddresses - 1;

         for (int i = 0; i < entries.length; i++) {
            try {
               int startAddress = 0;
               int endAddress = 0;
               boolean isItPolled = false;
               if (entries[i] != null) {
                  startAddress = entries[i].getStartAddress().getDataAddress();
                  endAddress = startAddress + entries[i].getConsecutivePointsToPoll() - 1;
               }

               isItPolled = registerAddress >= startAddress
                  && registerAddress <= endAddress
                  && lastRegisterAddress >= startAddress
                  && lastRegisterAddress <= endAddress;
               if (isItPolled) {
                  return entries[i];
               }
            } catch (Exception var8) {
            }
         }

         return null;
      }
   }

   public void addEntry(BDevicePollConfigEntry entry) {
      this.add(null, entry);
   }

   public void clearEntries() {
      this.removeAll(null);
   }

   public void setDevicePollConfigList(BDevicePollConfigEntry[] dataList) {
      this.clearEntries();
      BComponentSpace space = this.getComponentSpace();
      if (space != null) {
         space.update(this, 0);
      }

      if (dataList != null) {
         for (int i = 0; i < dataList.length; i++) {
            this.addEntry(dataList[i]);
         }
      }

      if (space != null) {
         space.update(this, 0);
      }
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (property.getType() == BDevicePollConfigEntry.TYPE
         && this.getParent() != null
         && this.getPropertyInParent() != null
         && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
      }
   }

   public void removed(Property property, BValue oldValue, Context context) {
      super.removed(property, oldValue, context);
      if (property.getType() == BDevicePollConfigEntry.TYPE
         && this.getParent() != null
         && this.getPropertyInParent() != null
         && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
      }
   }

   public void added(Property property, Context context) {
      super.added(property, context);
      if (property.getType() == BDevicePollConfigEntry.TYPE
         && this.getParent() != null
         && this.getPropertyInParent() != null
         && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
      }
   }

   public void doLearnOptimumDevicePollConfig(BReplaceExistingEnum replaceExistingEntries) {
      BModbusClientDevice device = this.getDevice();
      if (device != null) {
         BDevicePollConfigEntry[] entries = device.getOptimumDevicePollConfigEntryList();
         if (replaceExistingEntries.equals(BReplaceExistingEnum.replaceExistingEntries)) {
            this.clearEntries();
         }

         if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
               this.addEntry(entries[i]);
            }
         }

         if (this.getParent() != null && this.getPropertyInParent() != null && this.getParent() instanceof BComponent) {
            ((BComponent)this.getParent()).changed(this.getPropertyInParent(), null);
         }
      }
   }

   public void doClear() {
      this.clearEntries();
      BComponentSpace space = this.getComponentSpace();
      if (space != null) {
         space.update(this, 0);
      }

      if (this.getParent() != null && this.getPropertyInParent() != null && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), null);
      }
   }

   private BModbusClientDevice getDevice() {
      for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
         if (parent instanceof BModbusClientDevice) {
            return (BModbusClientDevice)parent;
         }
      }

      return null;
   }
}
