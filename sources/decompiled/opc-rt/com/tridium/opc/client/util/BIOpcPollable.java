package com.tridium.opc.client.util;

import javax.baja.driver.util.BIPollable;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIOpcPollable extends BIPollable {
   Type TYPE = Sys.loadType(BIOpcPollable.class);

   void poll();
}
