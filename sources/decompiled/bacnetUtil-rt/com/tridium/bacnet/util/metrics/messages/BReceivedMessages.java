package com.tridium.bacnet.util.metrics.messages;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.network.wiretap.IncomingWiretap;
import com.tridium.bacnet.stack.network.wiretap.WiretapAware;
import com.tridium.bacnet.util.point.BPeriodicNumericPoint;
import com.tridium.bacnet.util.point.EventsPerSecond;
import java.util.concurrent.atomic.LongAdder;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperty(
   name = "facets",
   type = "BFacets",
   defaultValue = "BFacets.makeNumeric(BUnit.getUnit(\"per second\"), 1)",
   override = true
)
public class BReceivedMessages extends BPeriodicNumericPoint implements IncomingWiretap, EventsPerSecond {
   public static final Property facets = newProperty(0, BFacets.makeNumeric(BUnit.getUnit("per second"), 1), null);
   public static final Type TYPE = Sys.loadType(BReceivedMessages.class);
   private LongAdder received = new LongAdder();
   private long lastUpdate = System.currentTimeMillis();

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof WiretapAware;
   }

   public void onExecute(BStatusValue o, Context cx) {
      long t1 = this.lastUpdate;
      this.lastUpdate = System.currentTimeMillis();
      long messagesReceived = this.received.longValue();
      this.received.reset();
      BStatusNumeric out = (BStatusNumeric)o;
      out.setValue(this.calculateEventsPerSecond(messagesReceived, t1, this.lastUpdate));
   }

   public void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is, boolean isBroadcast) {
      this.received.increment();
   }

   public long getReceivedMessages() {
      return this.received.longValue();
   }
}
