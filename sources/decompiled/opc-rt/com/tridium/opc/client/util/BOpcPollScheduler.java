package com.tridium.opc.client.util;

import javax.baja.driver.util.BIPollable;
import javax.baja.driver.util.BPollScheduler;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcPollScheduler extends BPollScheduler {
   public static final Type TYPE = Sys.loadType(BOpcPollScheduler.class);

   public Type getType() {
      return TYPE;
   }

   public void doPoll(BIPollable pollable) {
      ((BIOpcPollable)pollable).poll();
   }
}
