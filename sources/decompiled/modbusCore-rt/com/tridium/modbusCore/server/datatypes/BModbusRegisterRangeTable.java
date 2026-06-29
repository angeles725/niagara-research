package com.tridium.modbusCore.server.datatypes;

import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.SortUtil;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.Action;
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
      name = "addRange",
      parameterType = "BModbusRegisterRangeEntry",
      defaultValue = "new BModbusRegisterRangeEntry()"
   ), @NiagaraAction(
      name = "clear",
      flags = 128
   )})
public class BModbusRegisterRangeTable extends BComponent {
   public static final Action addRange = newAction(0, new BModbusRegisterRangeEntry(), null);
   public static final Action clear = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BModbusRegisterRangeTable.class);

   public void addRange(BModbusRegisterRangeEntry parameter) {
      this.invoke(addRange, parameter, null);
   }

   public void clear() {
      this.invoke(clear, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BModbusRegisterRangeTable() {
   }

   public BModbusRegisterRangeTable(BModbusRegisterRangeEntry entry, String name) {
      this.addEntry(name, entry);
   }

   public BModbusRegisterRangeTable(BModbusRegisterRangeEntry[] entries) {
      this.setModbusRegisterRangeList(entries);
   }

   public synchronized BModbusRegisterRangeEntry[] getModbusRegisterRangeList() {
      BModbusRegisterRangeEntry[] temp = new BModbusRegisterRangeEntry[this.getSlotCount()];
      int count = 0;

      for (SlotCursor<Property> c = this.getProperties(); c.next(BModbusRegisterRangeEntry.class); count++) {
         BObject kid = c.get();
         temp[count] = (BModbusRegisterRangeEntry)kid;
      }

      BModbusRegisterRangeEntry[] result = new BModbusRegisterRangeEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public synchronized BModbusRegisterRangeEntry[] getEnabledModbusRegisterRangeList() {
      BModbusRegisterRangeEntry[] temp = new BModbusRegisterRangeEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BModbusRegisterRangeEntry.class)) {
         BObject kid = c.get();
         if (((BModbusRegisterRangeEntry)kid).getEnabled()) {
            temp[count] = (BModbusRegisterRangeEntry)kid;
            count++;
         }
      }

      BModbusRegisterRangeEntry[] result = new BModbusRegisterRangeEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public void addEntry(String name, BModbusRegisterRangeEntry entry) {
      if (name != null && !SlotPath.isValidName(name)) {
         name = SlotPath.escape(name);
      }

      this.add(name, entry);
   }

   public void clearEntries() {
      this.removeAll(null);
   }

   public void setModbusRegisterRangeList(BModbusRegisterRangeEntry[] dataList) {
      this.clearEntries();
      BComponentSpace space = this.getComponentSpace();
      if (space != null) {
         space.update(this, 0);
      }

      if (dataList != null) {
         for (int i = 0; i < dataList.length; i++) {
            this.addEntry(null, dataList[i]);
         }
      }

      if (space != null) {
         space.update(this, 0);
      }
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (property.getType() == BModbusRegisterRangeEntry.TYPE
         && this.getParent() != null
         && this.getPropertyInParent() != null
         && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
      }
   }

   public void removed(Property property, BValue oldValue, Context context) {
      super.removed(property, oldValue, context);
      if (property.getType() == BModbusRegisterRangeEntry.TYPE
         && this.getParent() != null
         && this.getPropertyInParent() != null
         && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
      }
   }

   public void added(Property property, Context context) {
      super.added(property, context);
      if (property.getType() == BModbusRegisterRangeEntry.TYPE
         && this.getParent() != null
         && this.getPropertyInParent() != null
         && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
      }
   }

   public void doAddRange(BModbusRegisterRangeEntry param) {
      BComponentSpace space = this.getComponentSpace();
      if (space != null) {
         space.update(this, 0);
      }

      this.addEntry(null, param);
      if (space != null) {
         space.update(this, 0);
      }
   }

   public void doClear() {
      this.clearEntries();
      BComponentSpace space = this.getComponentSpace();
      if (space != null) {
         space.update(this, 0);
      }
   }

   public boolean containsAddress(int address) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BModbusRegisterRangeEntry.class)) {
         BObject kid = c.get();
         if (((BModbusRegisterRangeEntry)kid).getEnabled() && ((BModbusRegisterRangeEntry)kid).containsAddress(address)) {
            return true;
         }
      }

      return false;
   }

   public Integer[] getValidAddressArray() {
      BModbusRegisterRangeEntry[] entries = this.getEnabledModbusRegisterRangeList();
      int totalPossible = 0;

      for (int i = 0; i < entries.length; i++) {
         totalPossible += entries[i].getSize();
      }

      Integer[] temp = new Integer[totalPossible];
      int count = 0;

      for (int i = 0; i < entries.length; i++) {
         int startAddress = entries[i].getStartingAddressOffset();
         int endAddress = startAddress + entries[i].getSize();

         for (int j = startAddress; j < endAddress; j++) {
            if (!addressExists(temp, j)) {
               temp[count] = j - 1;
               count++;
            }
         }
      }

      SortUtil.sort(temp);
      Integer[] result = new Integer[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   private static boolean addressExists(Integer[] addressArray, int address) {
      for (int i = 0; i < addressArray.length; i++) {
         if (addressArray[i] != null && addressArray[i] == address) {
            return true;
         }
      }

      return false;
   }

   public void setPersistedData(IntHashMap map, boolean isRegister) {
      for (BModbusRegisterRangeEntry rangeEntry : this.getModbusRegisterRangeList()) {
         rangeEntry.setPersistedData(map, isRegister);
      }
   }
}
