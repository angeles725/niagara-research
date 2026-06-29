package com.tridium.modbusCore.point;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BEnumBitsWritable extends BObject {
   public static final Type TYPE = Sys.loadType(BEnumBitsWritable.class);

   public Type getType() {
      return TYPE;
   }
}
