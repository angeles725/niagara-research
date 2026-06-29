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
      name = "remoteStart",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "localStart",
      type = "int",
      defaultValue = "0"
   )})
public class BWriteFileConfig extends BStruct implements BacnetConst {
   public static final Property remoteStart = newProperty(0, 0, null);
   public static final Property localStart = newProperty(0, 0, null);
   public static final Type TYPE = Sys.loadType(BWriteFileConfig.class);

   public int getRemoteStart() {
      return this.getInt(remoteStart);
   }

   public void setRemoteStart(int v) {
      this.setInt(remoteStart, v, null);
   }

   public int getLocalStart() {
      return this.getInt(localStart);
   }

   public void setLocalStart(int v) {
      this.setInt(localStart, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
