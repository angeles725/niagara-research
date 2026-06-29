package com.tridium.bacnet.job;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BVector;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BDiscoveryPointTable extends BVector {
   public static final Type TYPE = Sys.loadType(BDiscoveryPointTable.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BDiscoveryPoint;
   }
}
