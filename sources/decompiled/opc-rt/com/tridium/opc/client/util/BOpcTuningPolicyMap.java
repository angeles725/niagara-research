package com.tridium.opc.client.util;

import javax.baja.driver.point.BTuningPolicyMap;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "defaultPolicy",
   type = "BTuningPolicy",
   defaultValue = "new BOpcTuningPolicy()",
   override = true
)
public class BOpcTuningPolicyMap extends BTuningPolicyMap {
   public static final Property defaultPolicy = newProperty(0, new BOpcTuningPolicy(), null);
   public static final Type TYPE = Sys.loadType(BOpcTuningPolicyMap.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BOpcTuningPolicy;
   }
}
