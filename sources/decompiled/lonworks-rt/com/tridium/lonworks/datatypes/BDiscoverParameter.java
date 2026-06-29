package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.enums.BDiscoverMode;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "mode",
      type = "BDiscoverMode",
      defaultValue = "BDiscoverMode.noOptions"
   ), @NiagaraProperty(
      name = "offset",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "data",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "managedNetwork",
      type = "boolean",
      defaultValue = "false"
   )})
public class BDiscoverParameter extends BStruct {
   public static final Property mode = newProperty(0, BDiscoverMode.noOptions, null);
   public static final Property offset = newProperty(0, 0, null);
   public static final Property data = newProperty(0, "", null);
   public static final Property managedNetwork = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BDiscoverParameter.class);

   public BDiscoverMode getMode() {
      return (BDiscoverMode)this.get(mode);
   }

   public void setMode(BDiscoverMode v) {
      this.set(mode, v, null);
   }

   public int getOffset() {
      return this.getInt(offset);
   }

   public void setOffset(int v) {
      this.setInt(offset, v, null);
   }

   public String getData() {
      return this.getString(data);
   }

   public void setData(String v) {
      this.setString(data, v, null);
   }

   public boolean getManagedNetwork() {
      return this.getBoolean(managedNetwork);
   }

   public void setManagedNetwork(boolean v) {
      this.setBoolean(managedNetwork, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDiscoverParameter() {
   }

   public BDiscoverParameter(BDiscoverMode mode, int offset, String data, boolean managedNetwork) {
      this.setMode(mode);
      this.setOffset(offset);
      this.setData(data);
      this.setManagedNetwork(managedNetwork);
   }
}
