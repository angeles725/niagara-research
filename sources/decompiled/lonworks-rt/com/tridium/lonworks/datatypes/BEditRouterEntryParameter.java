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
      type = "BRouterEntry",
      defaultValue = "new BRouterEntry()"
   ), @NiagaraProperty(
      name = "newEntry",
      type = "BRouterEntry",
      defaultValue = "new BRouterEntry()"
   )})
public class BEditRouterEntryParameter extends BStruct {
   public static final Property origEntry = newProperty(0, new BRouterEntry(), null);
   public static final Property newEntry = newProperty(0, new BRouterEntry(), null);
   public static final Type TYPE = Sys.loadType(BEditRouterEntryParameter.class);

   public BRouterEntry getOrigEntry() {
      return (BRouterEntry)this.get(origEntry);
   }

   public void setOrigEntry(BRouterEntry v) {
      this.set(origEntry, v, null);
   }

   public BRouterEntry getNewEntry() {
      return (BRouterEntry)this.get(newEntry);
   }

   public void setNewEntry(BRouterEntry v) {
      this.set(newEntry, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BEditRouterEntryParameter() {
   }

   public BEditRouterEntryParameter(BRouterEntry origEntry, BRouterEntry newEntry) {
      this.setOrigEntry(origEntry);
      this.setNewEntry(newEntry);
   }
}
