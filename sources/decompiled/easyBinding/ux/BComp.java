package com.forestrock.genericUi.basic;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BComp extends BComponent {
   public static final Type TYPE = Sys.loadType(BComp.class);

   public Type getType() {
      return TYPE;
   }
}
