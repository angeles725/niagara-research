package com.tridium.opc.client.util;

import javax.baja.driver.point.BTuningPolicy;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "pollFrequency",
   type = "BPollFrequency",
   defaultValue = "BPollFrequency.normal"
)
public class BOpcTuningPolicy extends BTuningPolicy {
   public static final Property pollFrequency = newProperty(0, BPollFrequency.normal, null);
   public static final Type TYPE = Sys.loadType(BOpcTuningPolicy.class);

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcTuningPolicyMap;
   }
}
