package com.tridium.modbusCore.server.datatypes;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap;
import javax.baja.sys.BBlob;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
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
      name = "criticalData",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "startingAddressOffset",
      type = "int",
      defaultValue = "1",
      facets = {@Facet("BFacets.makeInt(null, 1, Integer.MAX_VALUE)")}
   ), @NiagaraProperty(
      name = "size",
      type = "int",
      defaultValue = "64",
      facets = {@Facet("BFacets.makeInt(null, 1, Integer.MAX_VALUE)")}
   )})
public class BModbusRegisterRangeEntry extends BComponent {
   public static final Property enabled = newProperty(0, true, null);
   public static final Property criticalData = newProperty(0, false, null);
   public static final Property startingAddressOffset = newProperty(0, 1, BFacets.makeInt(null, 1, Integer.MAX_VALUE));
   public static final Property size = newProperty(0, 64, BFacets.makeInt(null, 1, Integer.MAX_VALUE));
   public static final Type TYPE = Sys.loadType(BModbusRegisterRangeEntry.class);
   private static String PERSISTED_DATA_NAME = "persistedData";
   private static int CRITICAL_FLAGS = 5;
   private static int NON_CRITICAL_FLAGS = 65541;

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public boolean getCriticalData() {
      return this.getBoolean(criticalData);
   }

   public void setCriticalData(boolean v) {
      this.setBoolean(criticalData, v, null);
   }

   public int getStartingAddressOffset() {
      return this.getInt(startingAddressOffset);
   }

   public void setStartingAddressOffset(int v) {
      this.setInt(startingAddressOffset, v, null);
   }

   public int getSize() {
      return this.getInt(size);
   }

   public void setSize(int v) {
      this.setInt(size, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BModbusRegisterRangeEntry() {
   }

   public BModbusRegisterRangeEntry(boolean enabled, int startOffset, int size) {
      this.setEnabled(enabled);
      this.setStartingAddressOffset(startOffset);
      this.setSize(size);
   }

   public BModbusRegisterRangeEntry(BModbusRegisterRangeEntry src) {
      this.copyFrom(src);
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusRegisterRangeTable;
   }

   public String toString(Context context) {
      return (this.getEnabled() ? this.getLexicon().getText("regRange.strings.enabled") : this.getLexicon().getText("regRange.strings.disabled"))
         + ": "
         + this.getStartingAddressOffset()
         + " - "
         + (this.getStartingAddressOffset() + this.getSize() - 1);
   }

   public String toDebugString() {
      return (this.getEnabled() ? "Range Enabled" : "Range Disabled")
         + ": "
         + this.getStartingAddressOffset()
         + " - "
         + (this.getStartingAddressOffset() + this.getSize() - 1);
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (property.equals(criticalData)) {
         Property prop = this.getProperty(PERSISTED_DATA_NAME);
         if (prop != null) {
            int flags = this.getFlags(prop);
            if (this.getCriticalData()) {
               this.setFlags(prop, flags & -65537);
            } else {
               this.setFlags(prop, flags | 65536);
            }
         }
      } else if ((property.equals(enabled) || property.equals(startingAddressOffset) || property.equals(size))
         && this.getParent() != null
         && this.getPropertyInParent() != null
         && this.getParent() instanceof BComponent) {
         ((BComponent)this.getParent()).changed(this.getPropertyInParent(), context);
      }
   }

   public boolean containsAddress(int address) {
      if (!this.getEnabled()) {
         return false;
      } else {
         int startAddress = this.getStartingAddressOffset();
         int endAddress = startAddress + this.getSize() - 1;
         return address >= startAddress && address <= endAddress;
      }
   }

   public byte[] getPersistedData() {
      BValue bValue = this.get(PERSISTED_DATA_NAME);
      return bValue != null && bValue.getType().is(BBlob.TYPE) ? ((BBlob)bValue).copyBytes() : null;
   }

   public void setPersistedData(IntHashMap map, boolean isRegister) {
      long startTicks = Clock.ticks();
      int size = this.getSize();
      int dataSize = size;
      if (isRegister) {
         dataSize = size * 2;
      }

      byte[] data = new byte[dataSize];
      int addrKey = this.getStartingAddressOffset() - 1;
      int dataIndex = 0;

      for (int i = 0; i < size; i++) {
         if (isRegister) {
            Object value0 = map.get(addrKey * 2);
            Object value1 = map.get(addrKey * 2 + 1);
            data[dataIndex++] = (Byte)value0;
            data[dataIndex++] = (Byte)value1;
         } else {
            Object value = map.get(addrKey);
            if (value == null) {
               System.out.println("how'd we get here");
            } else if ((Boolean)value) {
               data[dataIndex++] = -1;
            } else {
               data[dataIndex++] = 0;
            }
         }

         addrKey++;
      }

      this.setPersistedData(data);
   }

   public void setPersistedData(byte[] data) {
      BValue bValue = this.get(PERSISTED_DATA_NAME);
      if (bValue == null) {
         int flags = NON_CRITICAL_FLAGS;
         if (this.getCriticalData()) {
            flags = CRITICAL_FLAGS;
         }

         this.add(PERSISTED_DATA_NAME, BBlob.make(data), flags);
      } else {
         if (bValue instanceof BBlob) {
            this.set(PERSISTED_DATA_NAME, BBlob.make(data));
         }
      }
   }
}
