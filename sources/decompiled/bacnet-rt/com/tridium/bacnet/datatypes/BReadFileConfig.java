package com.tridium.bacnet.datatypes;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "start",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "count",
      type = "int",
      defaultValue = "Integer.MAX_VALUE"
   ), @NiagaraProperty(
      name = "appendToLocal",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   )})
public class BReadFileConfig extends BStruct implements BacnetConst {
   public static final Property start = newProperty(0, 0, null);
   public static final Property count = newProperty(0, Integer.MAX_VALUE, null);
   public static final Property appendToLocal = newProperty(5, false, null);
   public static final Type TYPE = Sys.loadType(BReadFileConfig.class);

   public int getStart() {
      return this.getInt(start);
   }

   public void setStart(int v) {
      this.setInt(start, v, null);
   }

   public int getCount() {
      return this.getInt(count);
   }

   public void setCount(int v) {
      this.setInt(count, v, null);
   }

   public boolean getAppendToLocal() {
      return this.getBoolean(appendToLocal);
   }

   public void setAppendToLocal(boolean v) {
      this.setBoolean(appendToLocal, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
