package com.tridium.modbusCore.client.point;

import javax.baja.driver.point.BPointFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusClientPointFolder extends BPointFolder {
   public static final Type TYPE = Sys.loadType(BModbusClientPointFolder.class);

   public Type getType() {
      return TYPE;
   }
}
