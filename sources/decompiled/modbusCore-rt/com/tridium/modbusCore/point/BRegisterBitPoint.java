package com.tridium.modbusCore.point;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BRegisterBitPoint extends BObject {
   public static final Type TYPE = Sys.loadType(BRegisterBitPoint.class);

   public Type getType() {
      return TYPE;
   }
}
