package com.tridium.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "origEntry",
      type = "BDeviceEntry",
      defaultValue = "new BDeviceEntry()"
   ), @NiagaraProperty(
      name = "newEntry",
      type = "BDeviceEntry",
      defaultValue = "new BDeviceEntry()"
   )})
public class BEditEntryParameter extends BStruct {
   public static final Property origEntry = newProperty(0, new BDeviceEntry(), null);
   public static final Property newEntry = newProperty(0, new BDeviceEntry(), null);
   public static final Type TYPE = Sys.loadType(BEditEntryParameter.class);

   public BDeviceEntry getOrigEntry() {
      return (BDeviceEntry)this.get(origEntry);
   }

   public void setOrigEntry(BDeviceEntry v) {
      this.set(origEntry, v, null);
   }

   public BDeviceEntry getNewEntry() {
      return (BDeviceEntry)this.get(newEntry);
   }

   public void setNewEntry(BDeviceEntry v) {
      this.set(newEntry, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BEditEntryParameter() {
   }

   public BEditEntryParameter(BDeviceEntry origEntry, BDeviceEntry newEntry) {
      this.setOrigEntry(origEntry);
      this.setNewEntry(newEntry);
   }
}
